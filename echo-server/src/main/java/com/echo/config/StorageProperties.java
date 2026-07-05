package com.echo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 로컬 파일 저장소 설정.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "echo.storage")
public class StorageProperties {

	private String rootPath = "./data/uploads";

	private long maxFileSizeMb = 10;

	private int maxAlbumCount = 30;

	private String allowedContentTypes = "image/jpeg,image/png,image/gif,image/webp";

}
