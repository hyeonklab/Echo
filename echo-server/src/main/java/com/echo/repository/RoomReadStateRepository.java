package com.echo.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.echo.domain.RoomMemberId;
import com.echo.domain.RoomReadState;

/**
 * 채팅방 읽음 상태 저장소.
 */
public interface RoomReadStateRepository extends JpaRepository<RoomReadState, RoomMemberId> {

	Optional<RoomReadState> findById_RoomIdAndId_UserId(Long roomId, Long userId);

	List<RoomReadState> findById_UserIdAndId_RoomIdIn(Long userId, Collection<Long> roomIds);

}
