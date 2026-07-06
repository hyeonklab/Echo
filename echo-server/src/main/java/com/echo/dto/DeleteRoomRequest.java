package com.echo.dto;

import jakarta.validation.constraints.Pattern;

/**
 * 채팅방 삭제/나가기 요청 DTO.
 */
public record DeleteRoomRequest(
	@Pattern(regexp = "me|all") String scope
) {

	/**
	 * 삭제 범위 기본값을 반환한다.
	 */
	public String scopeOrDefault() {
		if ("all".equals(scope)) {
			return "all";
		}

		return "me";
	}

}
