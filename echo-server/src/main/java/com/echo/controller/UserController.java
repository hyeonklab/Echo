package com.echo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.echo.dto.UpdateDisplayNameRequest;
import com.echo.dto.UserResponse;
import com.echo.security.UserPrincipal;
import com.echo.service.FileService;
import com.echo.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 검색 REST API.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final FileService fileService;

	/**
	 * 표시 이름을 변경한다.
	 */
	@PatchMapping("/me")
	public UserResponse updateDisplayName(
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody UpdateDisplayNameRequest request
	) {
		return executeUserAction(() -> userService.updateDisplayName(requireUserId(principal), request.displayName()));
	}

	/**
	 * 프로필 사진을 변경한다.
	 */
	@PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UserResponse updateAvatar(
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam("file") MultipartFile file
	) {
		return executeUserAction(() -> fileService.updateUserAvatar(requireUserId(principal), file));
	}

	/**
	 * 프로필 사진을 기본(초기) 상태로 되돌린다.
	 */
	@DeleteMapping("/me/avatar")
	public UserResponse removeAvatar(@AuthenticationPrincipal UserPrincipal principal) {
		return executeUserAction(() -> fileService.removeUserAvatar(requireUserId(principal)));
	}

	/**
	 * 이름 또는 이메일로 사용자를 검색한다.
	 */
	@GetMapping("/search")
	public List<UserResponse> searchUsers(
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(name = "q") String keyword
	) {
		return userService.searchUsers(keyword, requireUserId(principal));
	}

	private Long requireUserId(UserPrincipal principal) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}

		return principal.getUserId();
	}

	private <T> T executeUserAction(IoSupplier<T> action) {
		try {
			return action.get();
		}
		catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
		catch (java.io.IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File operation failed", ex);
		}
	}

	@FunctionalInterface
	private interface IoSupplier<T> {
		T get() throws java.io.IOException;
	}

}
