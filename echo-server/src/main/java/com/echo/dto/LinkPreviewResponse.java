package com.echo.dto;

/**
 * 링크 미리보기 응답 DTO.
 */
public record LinkPreviewResponse(
	String url,
	String title,
	String description,
	String imageUrl,
	String siteName
) {

}
