package com.echo.controller;

import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.echo.dto.AuthExchangeCodeRequest;
import com.echo.dto.RefreshTokenRequest;
import com.echo.dto.TokenResponse;
import com.echo.dto.UserResponse;
import com.echo.security.UserPrincipal;
import com.echo.service.AuthService;
import com.echo.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 인증 관련 REST API.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserService userService;

	/**
	 * JWT로 인증된 현재 사용자 정보를 반환한다.
	 */
	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}

		return userService.getUserResponse(principal.getUserId());
	}

	/**
	 * OAuth 일회용 교환 코드를 JWT로 교환한다.
	 */
	@PostMapping("/exchange")
	public TokenResponse exchange(@Valid @RequestBody AuthExchangeCodeRequest request) {
		return executeAuthAction(() -> authService.exchangeAuthCode(request.code()));
	}

	/**
	 * refresh token으로 access token을 재발급한다.
	 */
	@PostMapping("/refresh")
	public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return executeAuthAction(() -> authService.refreshTokens(request.refreshToken()));
	}

	/**
	 * OAuth 세션 및 SecurityContext를 정리한다.
	 */
	@PostMapping("/logout")
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		var authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null) {
			return;
		}

		new SecurityContextLogoutHandler().logout(request, response, authentication);
	}

	private TokenResponse executeAuthAction(Supplier<TokenResponse> action) {
		try {
			return action.get();
		}
		catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage(), ex);
		}
	}

}
