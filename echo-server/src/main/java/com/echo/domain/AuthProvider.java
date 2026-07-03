package com.echo.domain;

/**
 * OAuth 및 로컬 인증 제공자.
 */
public enum AuthProvider {

	LOCAL,
	GOOGLE,
	NAVER;

	/**
	 * Spring Security OAuth2 registration ID를 AuthProvider로 변환한다.
	 */
	public static AuthProvider fromRegistrationId(String registrationId) {
		return switch (registrationId.toLowerCase()) {
			case "google" -> GOOGLE;
			case "naver" -> NAVER;
			default -> throw new IllegalArgumentException("Unknown OAuth provider: " + registrationId);
		};
	}

}
