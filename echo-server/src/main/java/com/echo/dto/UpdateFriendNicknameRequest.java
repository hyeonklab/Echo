package com.echo.dto;

import jakarta.validation.constraints.Size;

/**
 * 친구 별칭 변경 요청 DTO.
 */
public record UpdateFriendNicknameRequest(
	@Size(max = 255)
	String nickname
) {
}
