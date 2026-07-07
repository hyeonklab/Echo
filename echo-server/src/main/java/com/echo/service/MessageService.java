package com.echo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.echo.domain.Message;
import com.echo.domain.MessageAttachment;
import com.echo.domain.MessageHidden;
import com.echo.domain.MessageType;
import com.echo.domain.Room;
import com.echo.domain.StoredFile;
import com.echo.domain.User;
import com.echo.dto.FileResponse;
import com.echo.dto.MessageDeletedResponse;
import com.echo.dto.MessageHistoryResponse;
import com.echo.dto.MessageResponse;
import com.echo.dto.MemberReadStateResponse;
import com.echo.dto.SendMessageRequest;
import com.echo.repository.MessageAttachmentRepository;
import com.echo.repository.MessageHiddenRepository;
import com.echo.repository.MessageRepository;
import com.echo.repository.RoomMemberRepository;
import com.echo.repository.RoomRepository;

import lombok.RequiredArgsConstructor;

/**
 * 메시지 전송 및 히스토리 조회.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

	private static final int DEFAULT_LIMIT = 50;
	private static final int MAX_LIMIT = 100;

	private final MessageRepository messageRepository;
	private final MessageAttachmentRepository messageAttachmentRepository;
	private final MessageHiddenRepository messageHiddenRepository;
	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final UserService userService;
	private final FileService fileService;
	private final MessageBroadcastService messageBroadcastService;
	private final RoomReadStateService roomReadStateService;
	private final RoomService roomService;
	private final FriendDisplayNameService friendDisplayNameService;

	/**
	 * 채팅방 메시지 히스토리를 반환한다.
	 */
	@Transactional(readOnly = true)
	public MessageHistoryResponse getMessages(Long roomId, Long userId, Long beforeId, Integer limit) {
		verifyRoomMember(roomId, userId);

		int pageSize = normalizeLimit(limit);
		Pageable pageable = PageRequest.of(0, pageSize + 1);
		List<Message> messages = beforeId == null
			? messageRepository.findVisibleByRoom_IdOrderByCreatedAtDesc(roomId, userId, pageable)
			: messageRepository.findVisibleByRoom_IdAndIdLessThanOrderByCreatedAtDesc(roomId, beforeId, userId, pageable);

		boolean hasMore = messages.size() > pageSize;

		if (hasMore) {
			messages = new ArrayList<>(messages.subList(0, pageSize));
		}

		List<Message> ascendingMessages = new ArrayList<>(messages);
		Collections.reverse(ascendingMessages);

		Map<Long, String> nicknameMap = friendDisplayNameService.getNicknameMap(userId);
		List<MessageResponse> responses = toMessageResponses(ascendingMessages, userId, nicknameMap);

		Long peerLastReadMessageId = roomReadStateService.getPeerLastReadMessageId(roomId, userId);
		List<MemberReadStateResponse> memberReadStates = roomReadStateService.getMemberReadStates(roomId, userId);

		return new MessageHistoryResponse(responses, hasMore, peerLastReadMessageId, memberReadStates);
	}

	/**
	 * 채팅방에 메시지를 전송한다.
	 */
	@Transactional
	public MessageResponse sendMessage(Long roomId, Long userId, SendMessageRequest request) {
		Room room = verifyRoomMember(roomId, userId);
		User sender = userService.getUser(userId);
		String content = request.content() == null ? "" : request.content().trim();
		List<Long> attachmentIds = request.attachmentIds() == null ? List.of() : request.attachmentIds();

		if (content.isBlank() && attachmentIds.isEmpty()) {
			throw new IllegalArgumentException("Message content or attachments required");
		}

		MessageType messageType = attachmentIds.isEmpty() ? MessageType.TEXT : MessageType.IMAGE_ALBUM;
		List<StoredFile> attachmentFiles = fileService.getMessageAttachmentsForSend(userId, attachmentIds);

		Message newMessage = Message.builder()
			.room(room)
			.sender(sender)
			.content(content)
			.messageType(messageType)
			.build();
		Message message = messageRepository.save(Objects.requireNonNull(newMessage));

		for (int index = 0; index < attachmentFiles.size(); index += 1) {
			StoredFile file = attachmentFiles.get(index);
			MessageAttachment attachment = MessageAttachment.builder()
				.message(message)
				.file(file)
				.sortOrder(index)
				.build();

			messageAttachmentRepository.save(Objects.requireNonNull(attachment));
		}

		MessageResponse response = toMessageResponse(message, attachmentFiles, userId);

		roomReadStateService.markAsRead(roomId, userId, message.getId());
		messageBroadcastService.broadcastMessage(response);
		roomService.restoreHiddenDmRoomForRecipients(room, userId, message);

		return response;
	}

	/**
	 * 메시지를 현재 사용자 화면에서만 숨긴다.
	 */
	@Transactional
	public void hideMessageForUser(Long roomId, Long userId, Long messageId) {
		verifyRoomMember(roomId, userId);

		Message message = getMessageInRoom(roomId, messageId);

		if (messageHiddenRepository.existsById_UserIdAndId_MessageId(userId, messageId)) {
			return;
		}

		User user = userService.getUser(userId);
		MessageHidden hidden = MessageHidden.builder()
			.user(user)
			.message(message)
			.build();

		messageHiddenRepository.save(Objects.requireNonNull(hidden));
	}

	/**
	 * 메시지를 모든 참여자에게서 삭제한다.
	 */
	@Transactional
	public void deleteMessageForEveryone(Long roomId, Long userId, Long messageId) {
		verifyRoomMember(roomId, userId);

		Message message = getMessageInRoom(roomId, messageId);

		if (message.getMessageType() == MessageType.ROOM_LEAVE) {
			throw new IllegalArgumentException("Cannot delete a system message");
		}

		if (!message.getSender().getId().equals(userId)) {
			throw new IllegalArgumentException("Only the sender can delete a message for everyone");
		}

		List<Long> attachmentFileIds = messageAttachmentRepository.findFileIdsByMessageId(messageId);

		messageAttachmentRepository.deleteByMessageId(messageId);
		messageRepository.delete(message);

		for (Long fileId : attachmentFileIds) {
			StoredFile file = fileService.getStoredFileIfExists(fileId);

			if (file == null) {
				continue;
			}

			try {
				fileService.deleteStoredFile(file);
			}
			catch (Exception ex) {
				// 첨부 파일 정리 실패 시에도 메시지 삭제는 유지한다.
			}
		}

		messageBroadcastService.broadcastMessageDeleted(new MessageDeletedResponse(roomId, messageId));
	}

	private List<MessageResponse> toMessageResponses(
		List<Message> messages,
		Long viewerUserId,
		Map<Long, String> nicknameMap
	) {
		if (messages.isEmpty()) {
			return List.of();
		}

		List<Long> messageIds = messages.stream()
			.map(Message::getId)
			.toList();
		Map<Long, List<FileResponse>> attachmentsByMessageId = messageAttachmentRepository
			.findByMessage_IdInWithFile(messageIds).stream()
			.collect(Collectors.groupingBy(
				attachment -> attachment.getMessage().getId(),
				Collectors.mapping(attachment -> FileResponse.from(attachment.getFile()), Collectors.toList())
			));

		return messages.stream()
			.map(message -> MessageResponse.from(
				message,
				attachmentsByMessageId.getOrDefault(message.getId(), List.of()),
				viewerUserId,
				nicknameMap
			))
			.toList();
	}

	private MessageResponse toMessageResponse(Message message, List<StoredFile> attachmentFiles, Long viewerUserId) {
		List<FileResponse> attachments = attachmentFiles.stream()
			.map(FileResponse::from)
			.toList();
		Map<Long, String> nicknameMap = friendDisplayNameService.getNicknameMap(viewerUserId);

		return MessageResponse.from(message, attachments, viewerUserId, nicknameMap);
	}

	private Message getMessageInRoom(Long roomId, Long messageId) {
		Message message = messageRepository.findById(Objects.requireNonNull(messageId))
			.orElseThrow(() -> new IllegalArgumentException("Message not found"));

		if (!message.getRoom().getId().equals(roomId)) {
			throw new IllegalArgumentException("Message does not belong to this room");
		}

		return message;
	}

	private Room verifyRoomMember(Long roomId, Long userId) {
		if (!roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, userId)) {
			throw new IllegalArgumentException("Room not found or access denied");
		}

		return roomRepository.findById(Objects.requireNonNull(roomId))
			.orElseThrow(() -> new IllegalArgumentException("Room not found or access denied"));
	}

	private int normalizeLimit(Integer limit) {
		if (limit == null || limit <= 0) {
			return DEFAULT_LIMIT;
		}

		return Math.min(limit, MAX_LIMIT);
	}

}
