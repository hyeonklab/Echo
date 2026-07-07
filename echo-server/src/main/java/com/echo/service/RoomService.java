package com.echo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.echo.domain.Message;
import com.echo.domain.Room;
import com.echo.domain.RoomHidden;
import com.echo.domain.RoomMember;
import com.echo.domain.RoomType;
import com.echo.domain.User;
import com.echo.dto.CreateDmRoomRequest;
import com.echo.dto.CreateGroupRoomRequest;
import com.echo.dto.InviteRoomMemberRequest;
import com.echo.dto.LastMessagePreview;
import com.echo.dto.RoomResponse;
import com.echo.dto.UpdateRoomNameRequest;
import com.echo.repository.MessageRepository;
import com.echo.repository.RoomHiddenRepository;
import com.echo.repository.RoomMemberRepository;
import com.echo.repository.RoomReadStateRepository;
import com.echo.repository.RoomRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

/**
 * 채팅방 생성·조회·초대·삭제 처리.
 */
@Service
@RequiredArgsConstructor
public class RoomService {

	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomHiddenRepository roomHiddenRepository;
	private final MessageRepository messageRepository;
	private final RoomReadStateRepository roomReadStateRepository;
	private final RoomReadStateService roomReadStateService;
	private final UserService userService;
	private final RoomBroadcastService roomBroadcastService;
	private final RoomLeaveMessageService roomLeaveMessageService;
	private final FriendDisplayNameService friendDisplayNameService;

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * 사용자가 참여 중인 채팅방 목록을 반환한다.
	 */
	@Transactional(readOnly = true)
	public List<RoomResponse> getRoomsForUser(Long userId) {
		List<Room> rooms = roomRepository.findAllByMemberUserId(userId);

		if (rooms.isEmpty()) {
			return List.of();
		}

		List<Long> roomIds = rooms.stream()
			.map(room -> Objects.requireNonNull(room).getId())
			.toList();
		Map<Long, Message> lastMessagesByRoomId = messageRepository.findLatestVisibleMessagesByRoomIds(roomIds, userId).stream()
			.collect(Collectors.toMap(message -> message.getRoom().getId(), message -> message));
		Map<Long, Integer> unreadCountsByRoomId = roomReadStateService.countUnreadByRoomIds(userId, roomIds);
		Map<Long, String> nicknameMap = friendDisplayNameService.getNicknameMap(userId);

		return rooms.stream()
			.map(room -> toRoomResponse(
				room,
				userId,
				lastMessagesByRoomId.get(room.getId()),
				unreadCountsByRoomId.getOrDefault(room.getId(), 0),
				nicknameMap
			))
			.sorted((left, right) -> compareByLastActivity(right, left))
			.toList();
	}

	/**
	 * 채팅방 상세 정보를 반환한다.
	 */
	@Transactional(readOnly = true)
	public RoomResponse getRoom(Long roomId, Long userId) {
		Room room = getRoomForMember(roomId, userId);

		return toRoomResponse(room, userId);
	}

	/**
	 * 그룹 채팅방을 생성한다.
	 */
	@Transactional
	public RoomResponse createGroupRoom(Long userId, CreateGroupRoomRequest request) {
		User creator = userService.getUser(userId);
		Room newRoom = Room.builder()
			.name(request.name().trim())
			.type(RoomType.GROUP)
			.createdBy(creator)
			.build();
		Room room = roomRepository.save(Objects.requireNonNull(newRoom));

		addMember(room, creator);

		if (request.memberUserIds() != null) {
			for (Long memberUserId : new HashSet<>(request.memberUserIds())) {
				if (memberUserId.equals(userId)) {
					continue;
				}

				User member = userService.getUser(memberUserId);
				addMember(room, member);
				roomBroadcastService.broadcastRoomMembershipUpdated(
					memberUserId,
					toRoomResponse(room, memberUserId)
				);
			}
		}

		return toRoomResponse(room, userId);
	}

	/**
	 * 1:1 DM 채팅방을 조회하거나 생성한다.
	 */
	@Transactional
	public RoomResponse createOrGetDmRoom(Long userId, CreateDmRoomRequest request) {
		if (userId.equals(request.targetUserId())) {
			return createOrGetSelfRoom(userId);
		}

		User currentUser = userService.getUser(userId);
		User targetUser = userService.getUser(request.targetUserId());

		List<Room> existingRooms = roomRepository.findAllDmRoomsBetweenUsers(
			userId,
			request.targetUserId(),
			RoomType.DM
		);

		if (!existingRooms.isEmpty()) {
			Room primaryRoom = selectPrimaryDmRoom(userId, existingRooms);
			removeDuplicateDmRooms(existingRooms, primaryRoom);
			clearRoomHidden(userId, primaryRoom.getId());

			return toRoomResponse(primaryRoom, userId);
		}

		Room newRoom = Room.builder()
			.name("DM")
			.type(RoomType.DM)
			.createdBy(currentUser)
			.build();
		Room room = roomRepository.save(Objects.requireNonNull(newRoom));

		addMember(room, currentUser);
		addMember(room, targetUser);

		RoomResponse targetResponse = toRoomResponse(room, request.targetUserId());
		roomBroadcastService.broadcastRoomMembershipUpdated(request.targetUserId(), targetResponse);

		return toRoomResponse(room, userId);
	}

	/**
	 * 나와의 대화 채팅방을 조회하거나 생성한다.
	 */
	@Transactional
	public RoomResponse createOrGetSelfRoom(Long userId) {
		User user = userService.getUser(userId);
		List<Room> existingRooms = roomRepository.findSelfRoomByUserId(userId, RoomType.SELF);

		if (!existingRooms.isEmpty()) {
			return toRoomResponse(existingRooms.get(0), userId);
		}

		Room newRoom = Room.builder()
			.name("나와의 대화")
			.type(RoomType.SELF)
			.createdBy(user)
			.build();
		Room room = roomRepository.save(Objects.requireNonNull(newRoom));

		addMember(room, user);

		return toRoomResponse(room, userId);
	}

	/**
	 * 채팅방에 멤버를 초대한다.
	 */
	@Transactional
	public RoomResponse inviteMember(Long roomId, Long requesterUserId, InviteRoomMemberRequest request) {
		Room room = getRoomForMember(roomId, requesterUserId);

		if (room.getType() != RoomType.GROUP) {
			throw new IllegalArgumentException("Cannot invite members to a DM room");
		}

		if (roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, request.userId())) {
			throw new IllegalArgumentException("User is already a member of this room");
		}

		User member = userService.getUser(request.userId());
		addMember(room, member);

		RoomResponse inviteeResponse = toRoomResponse(room, request.userId());
		roomBroadcastService.broadcastRoomMembershipUpdated(request.userId(), inviteeResponse);

		return toRoomResponse(room, requesterUserId);
	}

	/**
	 * 채팅방 이름을 변경한다.
	 */
	@Transactional
	public RoomResponse updateRoomName(Long roomId, Long userId, UpdateRoomNameRequest request) {
		Room room = getRoomForMember(roomId, userId);

		if (room.getType() == RoomType.DM) {
			throw new IllegalArgumentException("Cannot rename a DM room");
		}

		room.updateName(request.name().trim());

		RoomResponse response = toRoomResponse(room, userId);
		roomBroadcastService.broadcastRoomMetaUpdate(room);

		return response;
	}

	/**
	 * DM 새 메시지 수신 시 숨김 처리된 상대방 목록에 채팅방을 복원한다.
	 */
	@Transactional
	public void restoreHiddenDmRoomForRecipients(Room room, Long senderUserId, Message lastMessage) {
		if (room.getType() != RoomType.DM) {
			return;
		}

		List<RoomMember> members = roomMemberRepository.findAllByRoom_IdOrderByJoinedAtAsc(room.getId());

		for (RoomMember member : members) {
			Long memberUserId = member.getUser().getId();

			if (memberUserId.equals(senderUserId)) {
				continue;
			}

			if (!roomHiddenRepository.existsById_UserIdAndId_RoomId(memberUserId, room.getId())) {
				continue;
			}

			clearRoomHidden(memberUserId, room.getId());

			int unreadCount = roomReadStateService.countUnread(room.getId(), memberUserId);
			RoomResponse response = toRoomResponse(room, memberUserId, lastMessage, unreadCount);
			roomBroadcastService.broadcastRoomMembershipUpdated(memberUserId, response);
		}
	}

	/**
	 * 채팅방을 삭제하거나 참여를 종료한다.
	 */
	@Transactional
	public void deleteRoom(Long roomId, Long userId, String scope) {
		if (!roomRepository.existsById(roomId)) {
			return;
		}

		if (!roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, userId)) {
			return;
		}

		Room room = roomRepository.findById(Objects.requireNonNull(roomId))
			.orElseThrow(() -> new IllegalArgumentException("Room not found or access denied"));
		String normalizedScope = normalizeDeleteScope(scope);

		if (room.getType() == RoomType.SELF) {
			purgeRoom(roomId);
			return;
		}

		if (room.getType() == RoomType.DM) {
			if ("all".equals(normalizedScope)) {
				deleteAllDmRoomsBetweenMembers(userId, roomId);
				return;
			}

			roomLeaveMessageService.sendMemberLeftMessage(room, userId);
			hideRoomForUser(room, userId);
			return;
		}

		if (room.getCreatedBy().getId().equals(userId)) {
			notifyRoomDeletedToOthers(room, userId);
			purgeRoom(roomId);
			return;
		}

		roomLeaveMessageService.sendMemberLeftMessage(room, userId);
		roomMemberRepository.deleteByRoom_IdAndUser_Id(roomId, userId);

		if (roomMemberRepository.countByRoom_Id(roomId) == 0) {
			purgeRoom(roomId);
			return;
		}

		notifyRoomMembershipUpdatedToRemaining(room, userId);
	}

	private Room getRoomForMember(Long roomId, Long userId) {
		Room room = requireRoomMember(roomId, userId);

		if (roomHiddenRepository.existsById_UserIdAndId_RoomId(userId, roomId)) {
			throw new IllegalArgumentException("Room not found or access denied");
		}

		return room;
	}

	private Room requireRoomMember(Long roomId, Long userId) {
		if (!roomMemberRepository.existsByRoom_IdAndUser_Id(roomId, userId)) {
			throw new IllegalArgumentException("Room not found or access denied");
		}

		return roomRepository.findById(Objects.requireNonNull(roomId))
			.orElseThrow(() -> new IllegalArgumentException("Room not found or access denied"));
	}

	private void hideRoomForUser(Room room, Long userId) {
		if (roomHiddenRepository.existsById_UserIdAndId_RoomId(userId, room.getId())) {
			return;
		}

		User user = userService.getUser(userId);
		RoomHidden hidden = RoomHidden.builder()
			.user(user)
			.room(room)
			.build();

		roomHiddenRepository.save(Objects.requireNonNull(hidden));
	}

	private void clearRoomHidden(Long userId, Long roomId) {
		roomHiddenRepository.deleteById_UserIdAndId_RoomId(userId, roomId);
	}

	private void deleteAllDmRoomsBetweenMembers(Long userId, Long roomId) {
		Long otherUserId = findOtherMemberUserId(roomId, userId);
		List<Room> dmRooms = otherUserId == null
			? roomRepository.findById(roomId).map(List::of).orElse(List.of())
			: roomRepository.findAllDmRoomsBetweenUsers(userId, otherUserId, RoomType.DM);

		if (dmRooms.isEmpty()) {
			Room room = roomRepository.findById(roomId).orElse(null);

			if (room != null) {
				dmRooms = List.of(room);
			}
		}

		notifyRoomsDeletedToOthers(userId, dmRooms);

		for (Room dmRoom : dmRooms) {
			purgeRoom(dmRoom.getId());
		}
	}

	private Long findOtherMemberUserId(Long roomId, Long userId) {
		return roomMemberRepository.findUserIdsByRoomId(roomId).stream()
			.filter(memberUserId -> !memberUserId.equals(userId))
			.findFirst()
			.orElse(null);
	}

	private Room selectPrimaryDmRoom(Long viewerUserId, List<Room> rooms) {
		Room primaryRoom = rooms.get(0);

		for (Room room : rooms) {
			if (compareRoomActivity(room, primaryRoom, viewerUserId) > 0) {
				primaryRoom = room;
			}
		}

		return primaryRoom;
	}

	private void removeDuplicateDmRooms(List<Room> rooms, Room primaryRoom) {
		for (Room room : rooms) {
			if (room.getId().equals(primaryRoom.getId())) {
				continue;
			}

			purgeRoom(room.getId());
		}
	}

	private void purgeRoom(Long roomId) {
		if (roomId == null || !roomRepository.existsById(roomId)) {
			return;
		}

		messageRepository.deleteAllByRoom_Id(roomId);
		roomHiddenRepository.deleteAllByRoom_Id(roomId);
		roomReadStateRepository.deleteAllByRoom_Id(roomId);
		roomMemberRepository.deleteAllByRoom_Id(roomId);
		roomRepository.deleteById(roomId);
		entityManager.flush();
		entityManager.clear();
	}

	private int compareRoomActivity(Room left, Room right, Long viewerUserId) {
		Message leftLastMessage = findLastMessage(left.getId(), viewerUserId);
		Message rightLastMessage = findLastMessage(right.getId(), viewerUserId);

		if (leftLastMessage == null && rightLastMessage == null) {
			return left.getCreatedAt().compareTo(right.getCreatedAt());
		}

		if (leftLastMessage == null) {
			return -1;
		}

		if (rightLastMessage == null) {
			return 1;
		}

		return leftLastMessage.getCreatedAt().compareTo(rightLastMessage.getCreatedAt());
	}

	private void notifyRoomDeletedToOthers(Room room, Long actorUserId) {
		notifyRoomsDeletedToOthers(actorUserId, List.of(room));
	}

	private void notifyRoomsDeletedToOthers(Long actorUserId, List<Room> rooms) {
		Map<Long, List<Long>> roomIdsByMemberUserId = new HashMap<>();

		for (Room room : rooms) {
			Long roomId = room.getId();

			for (Long memberUserId : roomMemberRepository.findUserIdsByRoomId(roomId)) {
				if (memberUserId.equals(actorUserId)) {
					continue;
				}

				roomIdsByMemberUserId
					.computeIfAbsent(memberUserId, ignored -> new ArrayList<>())
					.add(roomId);
			}
		}

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			broadcastRoomDeletedToMembers(roomIdsByMemberUserId);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				broadcastRoomDeletedToMembers(roomIdsByMemberUserId);
			}
		});
	}

	private void broadcastRoomDeletedToMembers(Map<Long, List<Long>> roomIdsByMemberUserId) {
		for (Map.Entry<Long, List<Long>> entry : roomIdsByMemberUserId.entrySet()) {
			for (Long roomId : entry.getValue()) {
				roomBroadcastService.broadcastRoomDeleted(entry.getKey(), roomId);
			}
		}
	}

	/**
	 * 멤버 나가기 후 남은 참여자에게 갱신된 채팅방 정보를 전송한다.
	 */
	private void notifyRoomMembershipUpdatedToRemaining(Room room, Long excludedUserId) {
		List<Long> memberUserIds = roomMemberRepository.findUserIdsByRoomId(room.getId()).stream()
			.filter(memberUserId -> !memberUserId.equals(excludedUserId))
			.toList();

		if (memberUserIds.isEmpty()) {
			return;
		}

		Runnable broadcast = () -> {
			for (Long memberUserId : memberUserIds) {
				RoomResponse response = toRoomResponse(room, memberUserId);
				roomBroadcastService.broadcastRoomMembershipUpdated(memberUserId, response);
			}
		};

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			broadcast.run();
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				broadcast.run();
			}
		});
	}

	private String normalizeDeleteScope(String scope) {
		if ("all".equals(scope)) {
			return "all";
		}

		return "me";
	}

	private void addMember(Room room, User user) {
		if (roomMemberRepository.existsByRoom_IdAndUser_Id(room.getId(), user.getId())) {
			return;
		}

		RoomMember newMember = RoomMember.builder()
			.room(room)
			.user(user)
			.build();
		roomMemberRepository.save(Objects.requireNonNull(newMember));
	}

	private RoomResponse toRoomResponse(Room room, Long viewerUserId) {
		return toRoomResponse(
			room,
			viewerUserId,
			findLastMessage(room.getId(), viewerUserId),
			roomReadStateService.countUnread(room.getId(), viewerUserId),
			friendDisplayNameService.getNicknameMap(viewerUserId)
		);
	}

	private RoomResponse toRoomResponse(Room room, Long viewerUserId, Message lastMessage, int unreadCount) {
		return toRoomResponse(
			room,
			viewerUserId,
			lastMessage,
			unreadCount,
			friendDisplayNameService.getNicknameMap(viewerUserId)
		);
	}

	private RoomResponse toRoomResponse(
		Room room,
		Long viewerUserId,
		Message lastMessage,
		int unreadCount,
		Map<Long, String> nicknameMap
	) {
		List<RoomMember> members = roomMemberRepository.findAllByRoom_IdOrderByJoinedAtAsc(room.getId());
		LastMessagePreview lastMessagePreview = lastMessage == null
			? null
			: LastMessagePreview.from(lastMessage, viewerUserId, nicknameMap);

		return RoomResponse.from(room, members, viewerUserId, lastMessagePreview, unreadCount, nicknameMap);
	}

	private Message findLastMessage(Long roomId, Long viewerUserId) {
		List<Message> messages = messageRepository.findVisibleByRoom_IdAndUser_IdOrderByCreatedAtDesc(
			roomId,
			viewerUserId,
			PageRequest.of(0, 1)
		);

		if (messages.isEmpty()) {
			return null;
		}

		return messages.get(0);
	}

	private int compareByLastActivity(RoomResponse left, RoomResponse right) {
		if (left.lastMessage() == null && right.lastMessage() == null) {
			return left.createdAt().compareTo(right.createdAt());
		}

		if (left.lastMessage() == null) {
			return -1;
		}

		if (right.lastMessage() == null) {
			return 1;
		}

		return left.lastMessage().createdAt().compareTo(right.lastMessage().createdAt());
	}

}
