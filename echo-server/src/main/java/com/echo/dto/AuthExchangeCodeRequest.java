package com.echo.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * OAuth 일회용 교환 코드 요청 DTO.
 */
public record AuthExchangeCodeRequest(
	@NotBlank String code
) {
}
