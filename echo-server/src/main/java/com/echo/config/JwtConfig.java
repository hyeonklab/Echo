package com.echo.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.echo.security.JwtTokenProvider;

import io.jsonwebtoken.security.Keys;

/**
 * JWT 빈 등록.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	/**
	 * JWT 생성/검증 컴포넌트를 등록한다.
	 */
	@Bean
	public JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
		SecretKey secretKey = Keys.hmacShaKeyFor(
			jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
		);

		return new JwtTokenProvider(
			secretKey,
			jwtProperties.accessTokenExpirationMs(),
			jwtProperties.refreshTokenExpirationMs()
		);
	}

}
