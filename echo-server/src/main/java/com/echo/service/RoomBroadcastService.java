package com.echo.service;

import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.echo.dto.RoomDeletedResponse;
import com.echo.dto.RoomMetaUpdateResponse;
import com.echo.dto.RoomResponse;
import com.echo.domain.Room;

import lombok.RequiredArgsConstructor;

/**
 * 채팅방 메타 정보 STOMP 브로드캐스트.
 */
@Service
@RequiredArgsConstructor
public class RoomBroadcastService {

	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * 채팅방 구독자에게 메타 정보 변경을 전송한다.
	 */
	public void broadcastRoomMetaUpdate(@NonNull Room room) {
		String destination = "/topic/rooms/" + room.getId() + "/meta";

		messagingTemplate.convertAndSend(destination, RoomMetaUpdateResponse.from(room));
	}

	/**
	 * 사용자에게 채팅방 참여 변경을 전송한다.
	 */
	public void broadcastRoomMembershipUpdated(Long userId, RoomResponse room) {
		String destination = "/topic/users/" + userId + "/rooms";

		messagingTemplate.convertAndSend(destination, room);
	}

	/**
	 * 사용자에게 채팅방 삭제를 전송한다.
	 */
	public void broadcastRoomDeleted(Long userId, Long roomId) {
		String destination = "/topic/users/" + userId + "/rooms/deleted";

		messagingTemplate.convertAndSend(destination, new RoomDeletedResponse(roomId));
	}

}
