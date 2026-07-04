package com.echo.service;

import org.springframework.stereotype.Service;

import com.echo.domain.User;
import com.echo.dto.TokenResponse;
import com.echo.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

/**
 * JWT 갱신 처리.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserService userService;
	private final AuthExchangeCodeService authExchangeCodeService;

	/**
	 * OAuth 로그인 후 발급된 일회용 교환 코드를 JWT로 교환한다.
	 */
	public TokenResponse exchangeAuthCode(String code) {
		return authExchangeCodeService.consumeExchangeCode(code);
	}

	/**
	 * refresh token으로 새 access/refresh 토큰을 발급한다.
	 */
	public TokenResponse refreshTokens(String refreshToken) {
		if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
			throw new IllegalArgumentException("Invalid refresh token");
		}

		Long userId = jwtTokenProvider.getUserId(refreshToken);
		User user = userService.getUser(userId);

		return TokenResponse.of(
			jwtTokenProvider.createAccessToken(user),
			jwtTokenProvider.createRefreshToken(user)
		);
	}

}
