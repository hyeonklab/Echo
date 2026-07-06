package com.echo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.echo.domain.AuthProvider;
import com.echo.domain.User;
import com.echo.dto.CreateDmRoomRequest;
import com.echo.dto.MessageHistoryResponse;
import com.echo.dto.RoomResponse;
import com.echo.dto.SendMessageRequest;
import com.echo.repository.MessageRepository;
import com.echo.repository.RoomRepository;
import com.echo.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoomServiceDmDeleteTest {

	@Autowired
	private RoomService roomService;

	@Autowired
	private MessageService messageService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private MessageRepository messageRepository;

	@Test
	void deleteDmRoomForAllSides_removesMessagesWhenConversationRestarts() {
		User userA = createUser("dm-delete-a");
		User userB = createUser("dm-delete-b");

		RoomResponse dmRoom = roomService.createOrGetDmRoom(userA.getId(), new CreateDmRoomRequest(userB.getId()));
		Long roomId = dmRoom.id();

		messageService.sendMessage(roomId, userA.getId(), new SendMessageRequest("이전 메시지", null));
		assertThat(messageRepository.countByRoom_Id(roomId)).isEqualTo(1);

		roomService.deleteRoom(roomId, userA.getId(), "all");

		assertThat(roomRepository.findById(roomId)).isEmpty();
		assertThat(messageRepository.countByRoom_Id(roomId)).isZero();

		RoomResponse reopenedRoom = roomService.createOrGetDmRoom(userA.getId(), new CreateDmRoomRequest(userB.getId()));
		MessageHistoryResponse history = messageService.getMessages(reopenedRoom.id(), userA.getId(), null, 50);

		assertThat(reopenedRoom.id()).isNotEqualTo(roomId);
		assertThat(history.messages()).isEmpty();
	}

	@Test
	void deleteDmRoomForAllSides_isIdempotentWhenRoomAlreadyDeleted() {
		User userA = createUser("dm-delete-idempotent-a");
		User userB = createUser("dm-delete-idempotent-b");

		RoomResponse dmRoom = roomService.createOrGetDmRoom(userA.getId(), new CreateDmRoomRequest(userB.getId()));
		Long roomId = dmRoom.id();

		roomService.deleteRoom(roomId, userA.getId(), "all");
		roomService.deleteRoom(roomId, userB.getId(), "all");

		assertThat(roomRepository.findById(roomId)).isEmpty();
	}

	private User createUser(String prefix) {
		User user = User.builder()
			.email(prefix + "@example.com")
			.displayName(prefix)
			.provider(AuthProvider.LOCAL)
			.providerId(prefix)
			.passwordHash("hash")
			.build();

		return userRepository.save(user);
	}

}
