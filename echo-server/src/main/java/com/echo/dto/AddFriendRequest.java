package com.echo.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 친구 추가 요청 DTO.
 */
public record AddFriendRequest(
	@NotNull
	Long targetUserId
) {
}
