package com.echo.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 설정 값.
 */
@ConfigurationProperties(prefix = "echo.jwt")
@Validated
public record JwtProperties(
	@NotBlank
	@Size(min = 32, message = "JWT_SECRET must be at least 32 bytes")
	String secret,
	long accessTokenExpirationMs,
	long refreshTokenExpirationMs
) {
}
