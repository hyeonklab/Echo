package com.echo.service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import com.echo.dto.LinkPreviewResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * URL Open Graph 메타데이터 조회.
 */
@Service
@RequiredArgsConstructor
public class LinkPreviewService {

	private static final int READ_TIMEOUT_MS = 5_000;
	private static final int MAX_BODY_BYTES = 512_000;
	private static final int MAX_REDIRECTS = 3;
	private static final String USER_AGENT = "Echo-LinkPreview/1.0";
	private static final String YOUTUBE_OEMBED_URL = "https://www.youtube.com/oembed";

	private final ObjectMapper objectMapper;

	/**
	 * URL의 링크 미리보기 메타데이터를 조회한다.
	 */
	public LinkPreviewResponse fetchPreview(String url) {
		URI uri = validateUrl(url);

		if (isYoutubeUrl(uri)) {
			return fetchYoutubePreview(uri);
		}

		String html = fetchHtml(uri);

		return parsePreview(uri, html);
	}

	private boolean isYoutubeUrl(URI uri) {
		String host = uri.getHost().toLowerCase(Locale.ROOT);

		return host.equals("youtu.be")
			|| host.equals("youtube.com")
			|| host.equals("www.youtube.com")
			|| host.equals("m.youtube.com")
			|| host.endsWith(".youtube.com");
	}

	private LinkPreviewResponse fetchYoutubePreview(URI uri) {
		try {
			String json = Jsoup.connect(YOUTUBE_OEMBED_URL)
				.userAgent(USER_AGENT)
				.timeout(READ_TIMEOUT_MS)
				.ignoreContentType(true)
				.data("url", uri.toString())
				.data("format", "json")
				.execute()
				.body();

			JsonNode node = objectMapper.readTree(json);
			String title = readTextField(node, "title");
			String thumbnailUrl = readTextField(node, "thumbnail_url");
			String authorName = readTextField(node, "author_name");

			return new LinkPreviewResponse(
				uri.toString(),
				title,
				authorName,
				thumbnailUrl,
				"YouTube"
			);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to fetch YouTube preview", ex);
		}
	}

	private String readTextField(JsonNode node, String fieldName) {
		JsonNode field = node.get(fieldName);

		if (field == null || field.isNull()) {
			return null;
		}

		String value = field.asText().trim();

		if (value.isBlank()) {
			return null;
		}

		return value;
	}

	private URI validateUrl(String url) {
		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException("URL is required");
		}

		URI uri;

		try {
			uri = new URI(url.trim());
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException("Invalid URL", ex);
		}

		if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
			throw new IllegalArgumentException("Unsupported URL scheme");
		}

		if (uri.getUserInfo() != null) {
			throw new IllegalArgumentException("URL credentials are not allowed");
		}

		String host = uri.getHost();

		if (host == null || host.isBlank()) {
			throw new IllegalArgumentException("Invalid URL host");
		}

		if (isBlockedHost(host)) {
			throw new IllegalArgumentException("URL host is not allowed");
		}

		return uri;
	}

	private boolean isBlockedHost(String host) {
		String normalized = host.toLowerCase(Locale.ROOT);

		if (normalized.equals("localhost")
			|| normalized.endsWith(".local")
			|| normalized.equals("0.0.0.0")) {
			return true;
		}

		try {
			InetAddress address = InetAddress.getByName(host);

			return address.isAnyLocalAddress()
				|| address.isLoopbackAddress()
				|| address.isLinkLocalAddress()
				|| address.isSiteLocalAddress()
				|| address.isMulticastAddress();
		}
		catch (UnknownHostException ex) {
			throw new IllegalArgumentException("Unknown URL host", ex);
		}
	}

	private String fetchHtml(URI uri) {
		URI current = uri;

		for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
			validateUrl(current.toString());

			try {
				var connection = Jsoup.connect(current.toString())
					.userAgent(USER_AGENT)
					.timeout(READ_TIMEOUT_MS)
					.maxBodySize(MAX_BODY_BYTES)
					.followRedirects(false)
					.ignoreHttpErrors(true);

				var response = connection.execute();
				int statusCode = response.statusCode();

				if (statusCode >= 300 && statusCode < 400) {
					String location = response.header("Location");

					if (location == null || location.isBlank()) {
						throw new IllegalArgumentException("Redirect location is missing");
					}

					current = current.resolve(location.trim());
					continue;
				}

				if (statusCode < 200 || statusCode >= 300) {
					throw new IllegalArgumentException("Failed to fetch URL: HTTP " + statusCode);
				}

				String contentType = response.contentType();

				if (contentType != null && !contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
					throw new IllegalArgumentException("Unsupported content type");
				}

				return response.body();
			}
			catch (IOException ex) {
				throw new IllegalArgumentException("Failed to fetch URL", ex);
			}
		}

		throw new IllegalArgumentException("Too many redirects");
	}

	private LinkPreviewResponse parsePreview(URI uri, String html) {
		Document document = Jsoup.parse(html, uri.toString());
		String title = firstNonBlank(
			metaContent(document, "og:title"),
			metaContent(document, "twitter:title"),
			document.title()
		);
		String description = firstNonBlank(
			metaContent(document, "og:description"),
			metaContent(document, "description"),
			metaContent(document, "twitter:description")
		);
		String imageUrl = resolveAbsoluteUrl(uri, firstNonBlank(
			metaContent(document, "og:image"),
			metaContent(document, "twitter:image")
		));
		String siteName = firstNonBlank(
			metaContent(document, "og:site_name"),
			uri.getHost()
		);

		return new LinkPreviewResponse(
			uri.toString(),
			title,
			description,
			imageUrl,
			siteName
		);
	}

	private String metaContent(Document document, String key) {
		Element propertyMeta = document.selectFirst("meta[property=" + cssEscape(key) + "]");

		if (propertyMeta != null) {
			return normalizeMetaValue(propertyMeta.attr("content"));
		}

		Element nameMeta = document.selectFirst("meta[name=" + cssEscape(key) + "]");

		if (nameMeta != null) {
			return normalizeMetaValue(nameMeta.attr("content"));
		}

		return null;
	}

	private String cssEscape(String value) {
		return value.replace("\"", "\\\"");
	}

	private String normalizeMetaValue(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim();
	}

	private String resolveAbsoluteUrl(URI baseUri, String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		try {
			return baseUri.resolve(value.trim()).toString();
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.trim();
			}
		}

		return null;
	}

}
