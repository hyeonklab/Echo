package com.echo.dto;

import java.time.Instant;
import java.util.List;

import com.echo.domain.Message;
import com.echo.domain.MessageType;

/**
 * 메시지 응답 DTO.
 */
public record MessageResponse(
	Long id,
	Long roomId,
	Long senderId,
	String senderDisplayName,
	String content,
	MessageType messageType,
	List<FileResponse> attachments,
	Instant createdAt
) {

	/**
	 * Message 엔티티를 응답 DTO로 변환한다.
	 */
	public static MessageResponse from(Message message, List<FileResponse> attachments) {
		return new MessageResponse(
			message.getId(),
			message.getRoom().getId(),
			message.getSender().getId(),
			message.getSender().getDisplayName(),
			message.getContent() == null ? "" : message.getContent(),
			message.getMessageType(),
			attachments,
			message.getCreatedAt()
		);
	}

}
