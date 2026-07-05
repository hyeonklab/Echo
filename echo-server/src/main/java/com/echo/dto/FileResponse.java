package com.echo.dto;

import com.echo.domain.StoredFile;

/**
 * 업로드 파일 응답 DTO.
 */
public record FileResponse(
	Long id,
	String originalName,
	String contentType,
	long sizeBytes
) {

	/**
	 * StoredFile 엔티티를 응답 DTO로 변환한다.
	 */
	public static FileResponse from(StoredFile file) {
		return new FileResponse(
			file.getId(),
			file.getOriginalName(),
			file.getContentType(),
			file.getSizeBytes()
		);
	}

}
