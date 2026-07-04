package com.echo.dto;

import com.echo.domain.AuthProvider;
import com.echo.domain.User;
import com.echo.security.UserPrincipal;

/**
 * 현재 로그인 사용자 응답 DTO.
 */
public record UserResponse(
	Long id,
	String email,
	String displayName,
	AuthProvider provider
) {

	/**
	 * UserPrincipal을 응답 DTO로 변환한다.
	 */
	public static UserResponse fromPrincipal(UserPrincipal principal) {
		return new UserResponse(
			principal.getUserId(),
			principal.getEmail(),
			principal.getDisplayName(),
			principal.getProvider()
		);
	}

	/**
	 * User 엔티티를 응답 DTO로 변환한다.
	 */
	public static UserResponse from(User user) {
		return new UserResponse(
			user.getId(),
			user.getEmail(),
			user.getDisplayName(),
			user.getProvider()
		);
	}

}
