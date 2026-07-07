package com.echo.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.echo.domain.Room;
import com.echo.domain.RoomMember;
import com.echo.domain.RoomType;
import com.echo.domain.User;

/**
 * 채팅방 응답 DTO.
 */
public record RoomResponse(
	Long id,
	String name,
	RoomType type,
	Long createdByUserId,
	Instant createdAt,
	List<RoomMemberResponse> members,
	LastMessagePreview lastMessage,
	int unreadCount
) {

	/**
	 * Room 엔티티와 멤버 목록을 요청 사용자 기준 응답 DTO로 변환한다.
	 */
	public static RoomResponse from(
		Room room,
		List<RoomMember> members,
		Long viewerUserId,
		LastMessagePreview lastMessage,
		int unreadCount,
		Map<Long, String> friendNicknameMap
	) {
		List<RoomMemberResponse> memberResponses = members.stream()
			.map(member -> toMemberResponse(member.getUser(), viewerUserId, friendNicknameMap))
			.toList();

		return new RoomResponse(
			room.getId(),
			resolveDisplayName(room, members, viewerUserId, friendNicknameMap),
			room.getType(),
			room.getCreatedBy().getId(),
			room.getCreatedAt(),
			memberResponses,
			lastMessage,
			unreadCount
		);
	}

	private static RoomMemberResponse toMemberResponse(
		User user,
		Long viewerUserId,
		Map<Long, String> friendNicknameMap
	) {
		return new RoomMemberResponse(
			user.getId(),
			resolveUserDisplayName(user, viewerUserId, friendNicknameMap),
			user.getEmail(),
			user.getProvider(),
			user.getAvatarFile() == null ? null : user.getAvatarFile().getId()
		);
	}

	private static String resolveDisplayName(
		Room room,
		List<RoomMember> members,
		Long viewerUserId,
		Map<Long, String> friendNicknameMap
	) {
		if (room.getType() != RoomType.DM) {
			return room.getName();
		}

		return members.stream()
			.filter(member -> !member.getUser().getId().equals(viewerUserId))
			.findFirst()
			.map(member -> resolveUserDisplayName(member.getUser(), viewerUserId, friendNicknameMap))
			.orElse(room.getName());
	}

	private static String resolveUserDisplayName(
		User user,
		Long viewerUserId,
		Map<Long, String> friendNicknameMap
	) {
		if (viewerUserId.equals(user.getId())) {
			return user.getDisplayName();
		}

		String nickname = friendNicknameMap.get(user.getId());

		if (nickname != null && !nickname.isBlank()) {
			return nickname.trim();
		}

		return user.getDisplayName();
	}

}
