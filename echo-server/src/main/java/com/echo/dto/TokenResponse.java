package com.echo.dto;

/**
 * JWT 토큰 응답 DTO.
 */
public record TokenResponse(
	String accessToken,
	String refreshToken
) {

	/**
	 * access/refresh 토큰 쌍을 생성한다.
	 */
	public static TokenResponse of(String accessToken, String refreshToken) {
		return new TokenResponse(accessToken, refreshToken);
	}

}
