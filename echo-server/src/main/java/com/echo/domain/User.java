package com.echo.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 계정 엔티티.
 */
@Entity
@Table(
	name = "users",
	uniqueConstraints = @UniqueConstraint(name = "uk_users_provider_provider_id", columnNames = { "provider", "provider_id" })
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String email;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuthProvider provider;

	@Column(name = "provider_id", nullable = false)
	private String providerId;

	@Column(name = "password_hash")
	private String passwordHash;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "avatar_file_id")
	private StoredFile avatarFile;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Builder
	public User(String email, String displayName, AuthProvider provider, String providerId, String passwordHash) {
		this.email = email;
		this.displayName = displayName;
		this.provider = provider;
		this.providerId = providerId;
		this.passwordHash = passwordHash;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	/**
	 * OAuth 로그인 시 이메일 정보를 갱신한다.
	 */
	public void updateOAuthProfile(String email) {
		if (email != null && !email.isBlank()) {
			this.email = email;
		}
	}

	/**
	 * 표시 이름을 변경한다.
	 */
	public void updateDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * 프로필 사진을 변경한다.
	 */
	public void updateAvatarFile(StoredFile avatarFile) {
		this.avatarFile = avatarFile;
	}

	/**
	 * 프로필 사진을 제거한다.
	 */
	public void clearAvatarFile() {
		this.avatarFile = null;
	}

}
