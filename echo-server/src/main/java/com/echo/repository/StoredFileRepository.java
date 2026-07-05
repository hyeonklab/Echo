package com.echo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.echo.domain.StoredFile;

/**
 * 업로드 파일 저장소.
 */
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

	@Query("""
		SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
		FROM RoomMember m
		JOIN MessageAttachment ma ON ma.file.id = :fileId
		JOIN ma.message msg
		WHERE msg.room.id = m.room.id AND m.user.id = :userId
		""")
	boolean canAccessMessageFile(@Param("fileId") Long fileId, @Param("userId") Long userId);

	@Query("""
		SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
		FROM User u
		WHERE u.avatarFile.id = :fileId
		""")
	boolean isAvatarFile(@Param("fileId") Long fileId);

	List<StoredFile> findByIdInAndOwner_Id(List<Long> ids, Long ownerId);

	Optional<StoredFile> findByIdAndOwner_Id(Long id, Long ownerId);

}
