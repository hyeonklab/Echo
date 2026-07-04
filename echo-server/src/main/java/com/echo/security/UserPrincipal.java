package com.echo.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.echo.domain.AuthProvider;
import com.echo.domain.User;

import lombok.Getter;

/**
 * JWT 인증에 사용되는 사용자 principal.
 */
@Getter
public class UserPrincipal implements UserDetails {

	private static final long serialVersionUID = 1L;

	private final Long userId;
	private final String email;
	private final String displayName;
	private final AuthProvider provider;
	private final String passwordHash;

	public UserPrincipal(User user) {
		this.userId = user.getId();
		this.email = user.getEmail();
		this.displayName = user.getDisplayName();
		this.provider = user.getProvider();
		this.passwordHash = user.getPasswordHash();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return String.valueOf(userId);
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
