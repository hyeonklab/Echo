package com.echo.dto;

import java.time.Instant;
import java.util.Map;

import com.echo.domain.Message;
import com.echo.domain.User;

/**
 * 채팅방 목록용 마지막 메시지 미리보기 DTO.
 */
public record LastMessagePreview(
	Long id,
	Long senderId,
	String senderDisplayName,
	String content,
	Instant createdAt,
	com.echo.domain.MessageType messageType
) {

	/**
	 * Message 엔티티를 미리보기 DTO로 변환한다.
	 */
	public static LastMessagePreview from(Message message) {
		return new LastMessagePreview(
			message.getId(),
			message.getSender().getId(),
			message.getSender().getDisplayName(),
			message.getContent(),
			message.getCreatedAt(),
			message.getMessageType()
		);
	}

	/**
	 * Message 엔티티를 시청자 기준 별칭이 반영된 미리보기 DTO로 변환한다.
	 */
	public static LastMessagePreview from(Message message, Long viewerUserId, Map<Long, String> friendNicknameMap) {
		return new LastMessagePreview(
			message.getId(),
			message.getSender().getId(),
			resolveSenderDisplayName(message.getSender(), viewerUserId, friendNicknameMap),
			message.getContent(),
			message.getCreatedAt(),
			message.getMessageType()
		);
	}

	private static String resolveSenderDisplayName(
		User sender,
		Long viewerUserId,
		Map<Long, String> friendNicknameMap
	) {
		if (viewerUserId.equals(sender.getId())) {
			return sender.getDisplayName();
		}

		String nickname = friendNicknameMap.get(sender.getId());

		if (nickname != null && !nickname.isBlank()) {
			return nickname.trim();
		}

		return sender.getDisplayName();
	}

}
