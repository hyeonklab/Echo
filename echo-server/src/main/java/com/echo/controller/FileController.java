package com.echo.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.echo.domain.FilePurpose;
import com.echo.domain.StoredFile;
import com.echo.dto.UploadFilesResponse;
import com.echo.security.UserPrincipal;
import com.echo.service.FileService;

import lombok.RequiredArgsConstructor;

/**
 * 파일 업로드 및 다운로드 REST API.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

	private final FileService fileService;

	/**
	 * 이미지 파일을 업로드한다.
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UploadFilesResponse uploadFiles(
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam FilePurpose purpose,
		@RequestParam("files") MultipartFile[] files
	) {
		return executeFileAction(() -> fileService.uploadFiles(
			requireUserId(principal),
			purpose,
			java.util.Arrays.asList(files)
		));
	}

	/**
	 * 파일을 조회하거나 다운로드한다.
	 */
	@GetMapping("/{fileId}")
	public ResponseEntity<org.springframework.core.io.Resource> getFile(
		@AuthenticationPrincipal UserPrincipal principal,
		@PathVariable Long fileId,
		@RequestParam(defaultValue = "false") boolean download
	) {
		Long userId = requireUserId(principal);

		try {
			StoredFile file = fileService.getAccessibleFile(userId, fileId);
			org.springframework.core.io.Resource resource = fileService.loadFileResource(userId, fileId);
			MediaType mediaType = fileService.resolveMediaType(file);
			ContentDisposition disposition = download
				? ContentDisposition.attachment()
					.filename(file.getOriginalName(), StandardCharsets.UTF_8)
					.build()
				: ContentDisposition.inline()
					.filename(file.getOriginalName(), StandardCharsets.UTF_8)
					.build();

			return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
				.contentType(mediaType)
				.contentLength(file.getSizeBytes())
				.body(resource);
		}
		catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
	}

	private Long requireUserId(UserPrincipal principal) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}

		return principal.getUserId();
	}

	private <T> T executeFileAction(IoSupplier<T> action) {
		try {
			return action.get();
		}
		catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
		catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File operation failed", ex);
		}
	}

	@FunctionalInterface
	private interface IoSupplier<T> {
		T get() throws IOException;
	}

}
