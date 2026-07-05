package com.echo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.echo.domain.AuthProvider;
import com.echo.domain.FilePurpose;
import com.echo.domain.Room;
import com.echo.domain.RoomMember;
import com.echo.domain.RoomType;
import com.echo.domain.User;
import com.echo.dto.MessageResponse;
import com.echo.dto.SendMessageRequest;
import com.echo.repository.MessageAttachmentRepository;
import com.echo.repository.MessageRepository;
import com.echo.repository.RoomMemberRepository;
import com.echo.repository.RoomRepository;
import com.echo.repository.StoredFileRepository;
import com.echo.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MessageServiceDeleteTest {

	@Autowired
	private MessageService messageService;

	@Autowired
	private FileService fileService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoomMemberRepository roomMemberRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private MessageAttachmentRepository messageAttachmentRepository;

	@Autowired
	private StoredFileRepository storedFileRepository;

	private User user;

	private Room room;

	@BeforeEach
	void setUp() throws Exception {
		messageAttachmentRepository.deleteAll();
		storedFileRepository.deleteAll();
		messageRepository.deleteAll();
		roomMemberRepository.deleteAll();
		roomRepository.deleteAll();
		userRepository.deleteAll();

		user = userRepository.save(User.builder()
			.email("delete-test@example.com")
			.displayName("Delete Tester")
			.provider(AuthProvider.LOCAL)
			.providerId("delete-test")
			.passwordHash("hash")
			.build());

		room = roomRepository.save(Room.builder()
			.name("test-room")
			.type(RoomType.SELF)
			.createdBy(user)
			.build());

		roomMemberRepository.save(RoomMember.builder()
			.room(room)
			.user(user)
			.build());
	}

	@Test
	void deleteMessageForEveryone_removesImageMessage() throws Exception {
		MockMultipartFile image = new MockMultipartFile(
			"files",
			"photo.jpg",
			MediaType.IMAGE_JPEG_VALUE,
			new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }
		);
		Long fileId = fileService.uploadFiles(user.getId(), FilePurpose.MESSAGE, List.of(image)).files().get(0).id();

		MessageResponse message = messageService.sendMessage(
			room.getId(),
			user.getId(),
			new SendMessageRequest("", List.of(fileId))
		);

		messageService.deleteMessageForEveryone(room.getId(), user.getId(), message.id());

		assertThat(messageRepository.findById(message.id())).isEmpty();
		assertThat(messageAttachmentRepository.findByMessage_IdOrderBySortOrderAsc(message.id())).isEmpty();
		assertThat(storedFileRepository.findById(fileId)).isEmpty();
	}

}
