package com.echo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 표시 이름 변경 요청 DTO.
 */
public record UpdateDisplayNameRequest(
	@NotBlank
	@Size(max = 255)
	String displayName
) {
}
