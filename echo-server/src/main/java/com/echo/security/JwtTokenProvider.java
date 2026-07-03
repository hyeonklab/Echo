package com.echo.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.echo.domain.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT access/refresh 토큰 생성 및 검증.
 */
@Component
public class JwtTokenProvider {

	private static final String TOKEN_TYPE_CLAIM = "type";
	private static final String ACCESS_TOKEN_TYPE = "access";
	private static final String REFRESH_TOKEN_TYPE = "refresh";

	private final SecretKey secretKey;
	private final long accessTokenExpirationMs;
	private final long refreshTokenExpirationMs;

	public JwtTokenProvider(
		@Value("${echo.jwt.secret}") String secret,
		@Value("${echo.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
		@Value("${echo.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
	) {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
		}

		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpirationMs = accessTokenExpirationMs;
		this.refreshTokenExpirationMs = refreshTokenExpirationMs;
	}

	/**
	 * access token을 생성한다.
	 */
	public String createAccessToken(User user) {
		return createToken(user, accessTokenExpirationMs, ACCESS_TOKEN_TYPE);
	}

	/**
	 * refresh token을 생성한다.
	 */
	public String createRefreshToken(User user) {
		return createToken(user, refreshTokenExpirationMs, REFRESH_TOKEN_TYPE);
	}

	/**
	 * access token을 검증한다.
	 */
	public boolean validateAccessToken(String token) {
		return validateToken(token, ACCESS_TOKEN_TYPE);
	}

	/**
	 * refresh token을 검증한다.
	 */
	public boolean validateRefreshToken(String token) {
		return validateToken(token, REFRESH_TOKEN_TYPE);
	}

	/**
	 * 토큰에서 사용자 ID를 추출한다.
	 */
	public Long getUserId(String token) {
		return Long.parseLong(parseClaims(token).getSubject());
	}

	private String createToken(User user, long expirationMs, String tokenType) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
			.subject(String.valueOf(user.getId()))
			.claim(TOKEN_TYPE_CLAIM, tokenType)
			.claim("email", user.getEmail())
			.claim("displayName", user.getDisplayName())
			.issuedAt(now)
			.expiration(expiry)
			.signWith(secretKey)
			.compact();
	}

	private boolean validateToken(String token, String expectedType) {
		try {
			Claims claims = parseClaims(token);

			return expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
		}
		catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

}
