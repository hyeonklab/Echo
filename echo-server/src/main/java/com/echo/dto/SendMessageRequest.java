package com.echo.dto;

import java.util.List;

import jakarta.validation.constraints.Size;

/**
 * 메시지 전송 요청 DTO.
 */
public record SendMessageRequest(
	@Size(max = 4000)
	String content,
	List<Long> attachmentIds
) {

	/**
	 * 메시지 전송 요청 DTO.
	 */
	public SendMessageRequest {
		if (attachmentIds == null) {
			attachmentIds = List.of();
		}
	}

}
