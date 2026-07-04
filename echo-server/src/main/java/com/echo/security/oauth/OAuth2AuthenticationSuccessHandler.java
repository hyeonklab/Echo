package com.echo.security.oauth;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.echo.domain.User;
import com.echo.security.JwtTokenProvider;
import com.echo.service.AuthExchangeCodeService;
import com.echo.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * OAuth2 로그인 성공 시 사용자 upsert 후 JWT를 발급하고 프론트엔드로 리다이렉트한다.
 *
 * JWT는 URL에 직접 노출하지 않고 일회용 교환 코드로 전달한다: /auth/callback?code={exchangeCode}
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final UserService userService;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthExchangeCodeService authExchangeCodeService;

	@Value("${echo.frontend.url}")
	private String frontendUrl;

	@Override
	public void onAuthenticationSuccess(
		HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication
	) throws IOException {
		OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
		OAuth2User oauth2User = oauthToken.getPrincipal();
		String registrationId = oauthToken.getAuthorizedClientRegistrationId();

		User user = userService.upsertOAuthUser(registrationId, oauth2User);
		String accessToken = jwtTokenProvider.createAccessToken(user);
		String refreshToken = jwtTokenProvider.createRefreshToken(user);
		String exchangeCode = authExchangeCodeService.createExchangeCode(accessToken, refreshToken);

		String redirectUrl = UriComponentsBuilder
			.fromUriString(frontendUrl + "/auth/callback")
			.queryParam("code", exchangeCode)
			.build(true)
			.toUriString();

		getRedirectStrategy().sendRedirect(request, response, redirectUrl);
	}

}
