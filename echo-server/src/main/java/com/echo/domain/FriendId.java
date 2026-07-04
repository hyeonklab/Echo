package com.echo.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * friends 복합 키.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name = "owner_user_id")
	private Long ownerUserId;

	@Column(name = "friend_user_id")
	private Long friendUserId;

	public FriendId(Long ownerUserId, Long friendUserId) {
		this.ownerUserId = ownerUserId;
		this.friendUserId = friendUserId;
	}

}
