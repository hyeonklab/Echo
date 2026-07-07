package com.echo.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.echo.domain.Message;
import com.echo.domain.MessageType;
import com.echo.domain.User;

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
		return from(message, attachments, null, Map.of());
	}

	/**
	 * Message 엔티티를 시청자 기준 별칭이 반영된 응답 DTO로 변환한다.
	 */
	public static MessageResponse from(
		Message message,
		List<FileResponse> attachments,
		Long viewerUserId,
		Map<Long, String> friendNicknameMap
	) {
		return new MessageResponse(
			message.getId(),
			message.getRoom().getId(),
			message.getSender().getId(),
			resolveSenderDisplayName(message.getSender(), viewerUserId, friendNicknameMap),
			message.getContent() == null ? "" : message.getContent(),
			message.getMessageType(),
			attachments,
			message.getCreatedAt()
		);
	}

	private static String resolveSenderDisplayName(
		User sender,
		Long viewerUserId,
		Map<Long, String> friendNicknameMap
	) {
		if (viewerUserId == null || viewerUserId.equals(sender.getId())) {
			return sender.getDisplayName();
		}

		String nickname = friendNicknameMap.get(sender.getId());

		if (nickname != null && !nickname.isBlank()) {
			return nickname.trim();
		}

		return sender.getDisplayName();
	}

}
