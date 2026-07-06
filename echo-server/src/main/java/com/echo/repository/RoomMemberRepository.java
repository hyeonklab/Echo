package com.echo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.echo.domain.RoomMember;
import com.echo.domain.RoomMemberId;

/**
 * 채팅방 참여자 저장소.
 */
public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {

	List<RoomMember> findAllByRoom_IdOrderByJoinedAtAsc(Long roomId);

	@Query("SELECT rm.user.id FROM RoomMember rm WHERE rm.room.id = :roomId")
	List<Long> findUserIdsByRoomId(@Param("roomId") Long roomId);

	boolean existsByRoom_IdAndUser_Id(Long roomId, Long userId);

	void deleteByRoom_IdAndUser_Id(Long roomId, Long userId);

	long countByRoom_Id(Long roomId);

	/**
	 * 채팅방 멤버를 모두 삭제한다.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM RoomMember rm WHERE rm.room.id = :roomId")
	void deleteAllByRoom_Id(@Param("roomId") Long roomId);

}
