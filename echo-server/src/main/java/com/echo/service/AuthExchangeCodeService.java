package com.echo.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.echo.dto.TokenResponse;

/**
 * OAuth 로그인 후 프론트엔드로 JWT를 안전하게 전달하기 위한 일회용 교환 코드 저장소.
 */
@Service
public class AuthExchangeCodeService {

	private static final String INVALID_EXCHANGE_CODE_MESSAGE = "Invalid or expired exchange code";

	private record ExchangeEntry(String accessToken, String refreshToken, Instant expiresAt) {
	}

	private final Map<String, ExchangeEntry> codes = new ConcurrentHashMap<>();
	private final long expirationMs;

	public AuthExchangeCodeService(
		@Value("${echo.auth.exchange-code-expiration-ms:60000}") long expirationMs
	) {
		this.expirationMs = expirationMs;
	}

	/**
	 * access/refresh token을 일회용 교환 코드로 저장한다.
	 */
	public String createExchangeCode(String accessToken, String refreshToken) {
		purgeExpiredCodes();

		String code = UUID.randomUUID().toString();
		Instant expiresAt = Instant.now().plusMillis(expirationMs);

		codes.put(code, new ExchangeEntry(accessToken, refreshToken, expiresAt));

		return code;
	}

	/**
	 * 교환 코드를 검증하고 토큰을 반환한다. 코드는 한 번만 사용할 수 있다.
	 */
	public TokenResponse consumeExchangeCode(String code) {
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException(INVALID_EXCHANGE_CODE_MESSAGE);
		}

		ExchangeEntry entry = codes.remove(code);

		if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
			throw new IllegalArgumentException(INVALID_EXCHANGE_CODE_MESSAGE);
		}

		return TokenResponse.of(entry.accessToken(), entry.refreshToken());
	}

	private void purgeExpiredCodes() {
		Instant now = Instant.now();

		codes.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
	}

}
