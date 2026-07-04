package com.echo.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.echo.domain.Message;

/**
 * 메시지 저장소.
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

	List<Message> findByRoom_IdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

	List<Message> findByRoom_IdAndIdLessThanOrderByCreatedAtDesc(Long roomId, Long id, Pageable pageable);

}
