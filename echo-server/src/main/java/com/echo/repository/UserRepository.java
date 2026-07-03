package com.echo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.echo.domain.AuthProvider;
import com.echo.domain.User;

/**
 * 사용자 저장소.
 */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

}
