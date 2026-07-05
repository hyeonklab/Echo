package com.echo.dto;

import java.util.List;

/**
 * 파일 업로드 응답 DTO.
 */
public record UploadFilesResponse(
	List<FileResponse> files
) {
}
