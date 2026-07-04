package com.echo.dto;

/**
 * 채팅방 멤버 읽음 상태 DTO.
 */
public record MemberReadStateResponse(
	Long userId,
	Long lastReadMessageId
) {
}
