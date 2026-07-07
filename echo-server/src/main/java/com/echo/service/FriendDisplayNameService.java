package com.echo.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.echo.domain.User;
import com.echo.repository.FriendRepository;

import lombok.RequiredArgsConstructor;

/**
 * 친구 별칭 기반 표시 이름 해석.
 */
@Service
@RequiredArgsConstructor
public class FriendDisplayNameService {

	private final FriendRepository friendRepository;

	/**
	 * 소유자가 지정한 친구 별칭 맵을 반환한다.
	 */
	@Transactional(readOnly = true)
	public Map<Long, String> getNicknameMap(Long ownerUserId) {
		return friendRepository.findByOwnerIdOrderByCreatedAtDesc(ownerUserId).stream()
			.filter(friend -> friend.getNickname() != null && !friend.getNickname().isBlank())
			.collect(Collectors.toMap(
				friend -> friend.getFriend().getId(),
				friend -> friend.getNickname().trim(),
				(left, right) -> left
			));
	}

	/**
	 * 시청자 기준 사용자 표시 이름을 반환한다.
	 */
	public String resolve(Long viewerUserId, User user, Map<Long, String> nicknameMap) {
		if (viewerUserId.equals(user.getId())) {
			return user.getDisplayName();
		}

		String nickname = nicknameMap.get(user.getId());

		if (nickname != null && !nickname.isBlank()) {
			return nickname.trim();
		}

		return user.getDisplayName();
	}

}
