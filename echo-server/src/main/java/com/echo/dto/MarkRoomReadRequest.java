package com.echo.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 채팅방 읽음 처리 요청 DTO.
 */
public record MarkRoomReadRequest(
	@NotNull
	Long messageId
) {
}
