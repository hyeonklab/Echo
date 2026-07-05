package com.echo.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.echo.domain.MessageAttachment;
import com.echo.domain.StoredFile;

/**
 * 메시지 첨부 저장소.
 */
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {

	List<MessageAttachment> findByMessage_IdOrderBySortOrderAsc(Long messageId);

	@Query("""
		SELECT ma FROM MessageAttachment ma
		JOIN FETCH ma.file
		WHERE ma.message.id IN :messageIds
		ORDER BY ma.sortOrder ASC
		""")
	List<MessageAttachment> findByMessage_IdInWithFile(@Param("messageIds") Collection<Long> messageIds);

	boolean existsByFile_Id(Long fileId);

	Optional<MessageAttachment> findByFile_Id(Long fileId);

}
