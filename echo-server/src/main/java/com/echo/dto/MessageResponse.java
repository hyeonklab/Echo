package com.echo.dto;

import java.time.Instant;

import com.echo.domain.Message;

/**
 * 메시지 응답 DTO.
 */
public record MessageResponse(
	Long id,
	Long roomId,
	Long senderId,
	String senderDisplayName,
	String content,
	Instant createdAt
) {

	/**
	 * Message 엔티티를 응답 DTO로 변환한다.
	 */
	public static MessageResponse from(Message message) {
		return new MessageResponse(
			message.getId(),
			message.getRoom().getId(),
			message.getSender().getId(),
			message.getSender().getDisplayName(),
			message.getContent(),
			message.getCreatedAt()
		);
	}

}
