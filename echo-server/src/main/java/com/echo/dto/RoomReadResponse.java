package com.echo.dto;

/**
 * 채팅방 읽음 상태 브로드캐스트 DTO.
 */
public record RoomReadResponse(
	Long roomId,
	Long userId,
	Long lastReadMessageId
) {
}
