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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 채팅방 숨김 상태.
 */
@Entity
@Table(name = "room_hidden")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomHidden {

	@EmbeddedId
	private RoomHiddenId id = new RoomHiddenId();

	@MapsId("userId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@MapsId("roomId")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@Column(name = "hidden_at", nullable = false, updatable = false)
	private Instant hiddenAt;

	@Builder
	public RoomHidden(User user, Room room) {
		this.user = user;
		this.room = room;
	}

	@PrePersist
	void onCreate() {
		if (hiddenAt == null) {
			hiddenAt = Instant.now();
		}
	}

}
