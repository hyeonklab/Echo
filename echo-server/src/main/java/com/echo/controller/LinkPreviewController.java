package com.echo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.echo.dto.LinkPreviewResponse;
import com.echo.security.UserPrincipal;
import com.echo.service.LinkPreviewService;

import lombok.RequiredArgsConstructor;

/**
 * 링크 미리보기 REST API.
 */
@RestController
@RequestMapping("/api/link-preview")
@RequiredArgsConstructor
public class LinkPreviewController {

	private final LinkPreviewService linkPreviewService;

	/**
	 * URL의 Open Graph 메타데이터를 반환한다.
	 */
	@GetMapping
	public LinkPreviewResponse getLinkPreview(
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam String url
	) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}

		try {
			return linkPreviewService.fetchPreview(url);
		}
		catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
	}

}
