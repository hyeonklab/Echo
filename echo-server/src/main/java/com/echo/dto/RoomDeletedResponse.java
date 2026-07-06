package com.echo.dto;

/**
 * 채팅방 삭제 브로드캐스트 DTO.
 */
public record RoomDeletedResponse(
	Long roomId
) {

}
