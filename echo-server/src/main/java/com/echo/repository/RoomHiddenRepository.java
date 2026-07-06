package com.echo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.echo.domain.RoomHidden;
import com.echo.domain.RoomHiddenId;

/**
 * 채팅방 숨김 저장소.
 */
public interface RoomHiddenRepository extends JpaRepository<RoomHidden, RoomHiddenId> {

	boolean existsById_UserIdAndId_RoomId(Long userId, Long roomId);

	void deleteById_UserIdAndId_RoomId(Long userId, Long roomId);

	/**
	 * 채팅방 숨김 상태를 모두 삭제한다.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM RoomHidden rh WHERE rh.room.id = :roomId")
	void deleteAllByRoom_Id(@Param("roomId") Long roomId);

}
