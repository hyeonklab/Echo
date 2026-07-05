package com.echo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.multipart.MultipartFile;

import com.echo.config.StorageProperties;
import com.echo.domain.FilePurpose;
import com.echo.domain.StoredFile;
import com.echo.domain.User;
import com.echo.dto.FileResponse;
import com.echo.dto.UploadFilesResponse;
import com.echo.dto.UserResponse;
import com.echo.repository.MessageAttachmentRepository;
import com.echo.repository.StoredFileRepository;

import lombok.RequiredArgsConstructor;

/**
 * 업로드 파일 저장 및 조회.
 */
@Service
@RequiredArgsConstructor
public class FileService {

	private final StorageProperties storageProperties;
	private final StoredFileRepository storedFileRepository;
	private final MessageAttachmentRepository messageAttachmentRepository;
	private final LocalFileStorageService localFileStorageService;
	private final UserService userService;

	/**
	 * 이미지 파일을 업로드한다.
	 */
	@Transactional
	public UploadFilesResponse uploadFiles(Long userId, FilePurpose purpose, List<MultipartFile> files) throws IOException {
		if (files == null || files.isEmpty()) {
			throw new IllegalArgumentException("At least one file is required");
		}

		if (purpose == FilePurpose.MESSAGE && files.size() > storageProperties.getMaxAlbumCount()) {
			throw new IllegalArgumentException("Too many files in one upload");
		}

		if (purpose == FilePurpose.AVATAR && files.size() != 1) {
			throw new IllegalArgumentException("Avatar upload requires exactly one file");
		}

		User owner = userService.getUser(userId);
		Set<String> allowedTypes = resolveAllowedContentTypes();
		long maxBytes = storageProperties.getMaxFileSizeMb() * 1024L * 1024L;
		List<FileResponse> responses = new ArrayList<>();

		for (MultipartFile file : files) {
			validateUploadFile(file, allowedTypes, maxBytes);

			String storageKey;
			try (InputStream inputStream = file.getInputStream()) {
				storageKey = localFileStorageService.store(
					purpose,
					userId,
					file.getOriginalFilename(),
					inputStream
				);
			}

			StoredFile storedFile = StoredFile.builder()
				.owner(owner)
				.originalName(resolveOriginalName(file))
				.contentType(Objects.requireNonNull(file.getContentType()))
				.sizeBytes(file.getSize())
				.storageKey(storageKey)
				.purpose(purpose)
				.build();
			StoredFile saved = storedFileRepository.save(Objects.requireNonNull(storedFile));

			responses.add(FileResponse.from(saved));
		}

		return new UploadFilesResponse(responses);
	}

	/**
	 * 사용자 프로필 사진을 변경한다.
	 */
	@Transactional
	public UserResponse updateUserAvatar(Long userId, MultipartFile file) throws IOException {
		UploadFilesResponse uploaded = uploadFiles(userId, FilePurpose.AVATAR, List.of(file));
		StoredFile newAvatar = storedFileRepository.findById(uploaded.files().get(0).id())
			.orElseThrow(() -> new IllegalArgumentException("Uploaded avatar not found"));
		User user = userService.getUser(userId);
		StoredFile previousAvatar = user.getAvatarFile();

		user.updateAvatarFile(newAvatar);

		if (previousAvatar != null && !previousAvatar.getId().equals(newAvatar.getId())) {
			deleteStoredFile(previousAvatar);
		}

		return UserResponse.from(user);
	}

	/**
	 * 파일 접근 권한을 검사한다.
	 */
	@Transactional(readOnly = true)
	public StoredFile getAccessibleFile(Long userId, Long fileId) {
		StoredFile file = storedFileRepository.findById(Objects.requireNonNull(fileId))
			.orElseThrow(() -> new IllegalArgumentException("File not found"));

		if (!canAccessFile(userId, file)) {
			throw new IllegalArgumentException("File access denied");
		}

		return file;
	}

	/**
	 * 파일 리소스를 반환한다.
	 */
	@Transactional(readOnly = true)
	public Resource loadFileResource(Long userId, Long fileId) {
		StoredFile file = getAccessibleFile(userId, fileId);
		Path path = localFileStorageService.resolvePath(file.getStorageKey());

		if (!Files.exists(path)) {
			throw new IllegalArgumentException("File not found on disk");
		}

		try {
			return new InputStreamResource(Files.newInputStream(path));
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to read file", ex);
		}
	}

	/**
	 * 파일 MIME 타입을 반환한다.
	 */
	@Transactional(readOnly = true)
	public MediaType resolveMediaType(StoredFile file) {
		return MediaType.parseMediaType(file.getContentType());
	}

	/**
	 * 메시지 전송용 첨부 파일을 검증하고 반환한다.
	 */
	@Transactional(readOnly = true)
	public List<StoredFile> getMessageAttachmentsForSend(Long userId, List<Long> attachmentIds) {
		if (attachmentIds.isEmpty()) {
			return List.of();
		}

		List<StoredFile> files = storedFileRepository.findByIdInAndOwner_Id(attachmentIds, userId);

		if (files.size() != attachmentIds.size()) {
			throw new IllegalArgumentException("Invalid attachment file ids");
		}

		for (StoredFile file : files) {
			if (file.getPurpose() != FilePurpose.MESSAGE) {
				throw new IllegalArgumentException("Invalid attachment file purpose");
			}

			if (messageAttachmentRepository.existsByFile_Id(file.getId())) {
				throw new IllegalArgumentException("Attachment file is already used");
			}
		}

		return files.stream()
			.sorted((left, right) -> {
				int leftIndex = attachmentIds.indexOf(left.getId());
				int rightIndex = attachmentIds.indexOf(right.getId());

				return Integer.compare(leftIndex, rightIndex);
			})
			.toList();
	}

	/**
	 * 저장 파일을 삭제한다.
	 */
	@Transactional
	public void deleteStoredFile(StoredFile file) throws IOException {
		localFileStorageService.delete(file.getStorageKey());
		storedFileRepository.delete(file);
	}

	private boolean canAccessFile(Long userId, StoredFile file) {
		if (file.getOwner().getId().equals(userId)) {
			return true;
		}

		if (file.getPurpose() == FilePurpose.AVATAR) {
			return true;
		}

		return storedFileRepository.canAccessMessageFile(file.getId(), userId);
	}

	private void validateUploadFile(MultipartFile file, Set<String> allowedTypes, long maxBytes) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Empty file is not allowed");
		}

		if (file.getSize() > maxBytes) {
			throw new IllegalArgumentException("File is too large");
		}

		String contentType = file.getContentType();

		if (contentType == null || !allowedTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
			throw new IllegalArgumentException("Unsupported file type");
		}
	}

	private Set<String> resolveAllowedContentTypes() {
		return Set.of(storageProperties.getAllowedContentTypes().split(",")).stream()
			.map(value -> value.trim().toLowerCase(Locale.ROOT))
			.filter(value -> !value.isBlank())
			.collect(Collectors.toSet());
	}

	private String resolveOriginalName(MultipartFile file) {
		String originalName = file.getOriginalFilename();

		if (originalName == null || originalName.isBlank()) {
			return "file";
		}

		return originalName;
	}

}
