package com.echo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.echo.config.StorageProperties;
import com.echo.domain.FilePurpose;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * 로컬 디스크 파일 저장소.
 */
@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

	private final StorageProperties storageProperties;

	private Path rootPath;

	@PostConstruct
	void init() throws IOException {
		rootPath = Path.of(storageProperties.getRootPath()).toAbsolutePath().normalize();
		Files.createDirectories(rootPath);
	}

	/**
	 * 파일을 저장하고 storage key를 반환한다.
	 */
	public String store(FilePurpose purpose, Long userId, String originalName, InputStream inputStream) throws IOException {
		String extension = resolveExtension(originalName);
		String storageKey = "%s/%d/%s%s".formatted(
			purpose.name().toLowerCase(Locale.ROOT),
			userId,
			UUID.randomUUID(),
			extension
		);
		Path destination = resolvePath(storageKey);

		Files.createDirectories(destination.getParent());
		Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);

		return storageKey;
	}

	/**
	 * storage key에 해당하는 파일 경로를 반환한다.
	 */
	public Path resolvePath(String storageKey) {
		Path resolved = rootPath.resolve(storageKey).normalize();

		if (!resolved.startsWith(rootPath)) {
			throw new IllegalArgumentException("Invalid storage key");
		}

		return resolved;
	}

	/**
	 * storage key에 해당하는 파일을 삭제한다.
	 */
	public void delete(String storageKey) throws IOException {
		Files.deleteIfExists(resolvePath(storageKey));
	}

	private String resolveExtension(String originalName) {
		if (originalName == null) {
			return "";
		}

		int dotIndex = originalName.lastIndexOf('.');

		if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
			return "";
		}

		return originalName.substring(dotIndex).toLowerCase(Locale.ROOT);
	}

}
