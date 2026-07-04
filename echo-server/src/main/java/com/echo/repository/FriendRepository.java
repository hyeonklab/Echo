package com.echo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.echo.domain.Friend;
import com.echo.domain.FriendId;

/**
 * 친구 관계 저장소.
 */
public interface FriendRepository extends JpaRepository<Friend, FriendId> {

	/**
	 * 내 친구 목록을 추가 시각 역순으로 조회한다.
	 */
	@Query("""
		SELECT f FROM Friend f
		JOIN FETCH f.friend
		WHERE f.owner.id = :ownerUserId
		ORDER BY f.createdAt DESC
		""")
	List<Friend> findByOwnerIdOrderByCreatedAtDesc(@Param("ownerUserId") Long ownerUserId);

	Optional<Friend> findByOwnerIdAndFriendId(Long ownerId, Long friendId);

	boolean existsByOwnerIdAndFriendId(Long ownerId, Long friendId);

}
