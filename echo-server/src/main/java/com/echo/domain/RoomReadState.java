package com.echo.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방별 사용자 읽음 상태 엔티티.
 */
@Entity
@Table(name = "room_read_states")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomReadState {

	@EmbeddedId
	private RoomMemberId id = new RoomMemberId();

	@MapsId("roomId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "last_read_message_id")
	private Long lastReadMessageId;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Builder
	public RoomReadState(Room room, User user, Long lastReadMessageId) {
		this.room = room;
		this.user = user;
		this.lastReadMessageId = lastReadMessageId;
	}

	/**
	 * 마지막 읽은 메시지 ID를 갱신한다.
	 */
	public void updateLastReadMessageId(Long messageId) {
		if (messageId == null) {
			return;
		}

		if (lastReadMessageId == null || messageId > lastReadMessageId) {
			lastReadMessageId = messageId;
			updatedAt = Instant.now();
		}
	}

	@PrePersist
	void onCreate() {
		if (updatedAt == null) {
			updatedAt = Instant.now();
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

}
