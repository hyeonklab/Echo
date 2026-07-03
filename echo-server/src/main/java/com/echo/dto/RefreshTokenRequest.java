package com.echo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * refresh token 요청 DTO.
 */
public record RefreshTokenRequest(
	@NotBlank String refreshToken
) {
}
