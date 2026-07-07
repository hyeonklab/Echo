package com.echo.dto;

import java.time.Instant;

import com.echo.domain.Friend;

/**
 * 친구 목록 응답 DTO.
 */
public record FriendResponse(
	Long id,
	String email,
	String displayName,
	String nickname,
	String provider,
	Long avatarFileId,
	Instant addedAt,
	boolean online
) {

	/**
	 * Friend 엔티티를 응답 DTO로 변환한다.
	 */
	public static FriendResponse from(Friend friend, boolean online) {
		return new FriendResponse(
			friend.getFriend().getId(),
			friend.getFriend().getEmail(),
			friend.getFriend().getDisplayName(),
			friend.getNickname(),
			friend.getFriend().getProvider().name(),
			friend.getFriend().getAvatarFile() == null ? null : friend.getFriend().getAvatarFile().getId(),
			friend.getCreatedAt(),
			online
		);
	}

}
