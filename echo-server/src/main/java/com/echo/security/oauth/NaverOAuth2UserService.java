package com.echo.security.oauth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.echo.util.AttributeUtils;

/**
 * 네이버 OAuth2 사용자 정보 응답을 파싱한다.
 */
@Service
public class NaverOAuth2UserService extends DefaultOAuth2UserService {

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oauth2User = super.loadUser(userRequest);
		@SuppressWarnings("unchecked")
		Map<String, Object> response = oauth2User.getAttribute("response");

		if (response == null) {
			throw new OAuth2AuthenticationException("Naver OAuth response is missing");
		}

		String id = AttributeUtils.stringValue(response.get("id"));
		String email = AttributeUtils.stringValue(response.get("email"));
		String nickname = AttributeUtils.stringValue(response.get("nickname"));

		if (id == null || id.isBlank()) {
			throw new OAuth2AuthenticationException("Naver OAuth id is missing");
		}

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("id", id);
		attributes.put("email", email);
		attributes.put("nickname", nickname);
		attributes.put("name", nickname);

		return new DefaultOAuth2User(
			Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
			attributes,
			"id"
		);
	}

}
