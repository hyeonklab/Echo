package com.echo.dto;

import java.util.List;

/**
 * 메시지 히스토리 응답 DTO.
 */
public record MessageHistoryResponse(
	List<MessageResponse> messages,
	boolean hasMore,
	Long peerLastReadMessageId,
	List<MemberReadStateResponse> memberReadStates
) {
}
