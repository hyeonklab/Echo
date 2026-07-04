package com.echo.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.echo.dto.RoomReadResponse;

import lombok.RequiredArgsConstructor;

/**
 * 채팅방 읽음 상태 STOMP 브로드캐스트.
 */
@Service
@RequiredArgsConstructor
public class ReadBroadcastService {

	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * 채팅방 구독자에게 읽음 상태를 전송한다.
	 */
	public void broadcastRoomRead(RoomReadResponse readState) {
		String destination = "/topic/rooms/" + readState.roomId() + "/read";

		messagingTemplate.convertAndSend(destination, readState);
	}

}
