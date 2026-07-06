package com.echo.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.echo.domain.Message;
import com.echo.domain.MessageType;
import com.echo.domain.Room;
import com.echo.domain.RoomType;
import com.echo.domain.User;
import com.echo.dto.MessageResponse;
import com.echo.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

/**
 * 채팅방 나가기 등 시스템 메시지를 전송한다.
 */
@Service
@RequiredArgsConstructor
public class RoomLeaveMessageService {

	private final MessageRepository messageRepository;
	private final UserService userService;
	private final MessageBroadcastService messageBroadcastService;

	/**
	 * 참여자 나가기 시스템 메시지를 남긴다.
	 */
	@Transactional
	public void sendMemberLeftMessage(Room room, Long userId) {
		User user = userService.getUser(userId);
		String content = buildLeaveContent(room, user);

		Message message = Message.builder()
			.room(room)
			.sender(user)
			.content(content)
			.messageType(MessageType.ROOM_LEAVE)
			.build();

		Message savedMessage = messageRepository.save(Objects.requireNonNull(message));
		MessageResponse response = MessageResponse.from(savedMessage, List.of());
		messageBroadcastService.broadcastMessage(response);
	}

	private String buildLeaveContent(Room room, User user) {
		if (room.getType() == RoomType.GROUP) {
			return "%s님이 나갔습니다.".formatted(user.getDisplayName());
		}

		return "%s님이 채팅방을 나갔습니다.".formatted(user.getDisplayName());
	}

}
