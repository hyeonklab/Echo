package com.echo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 메시지 전송 요청 DTO.
 */
public record SendMessageRequest(
	@NotBlank
	@Size(max = 4000)
	String content
) {
}
