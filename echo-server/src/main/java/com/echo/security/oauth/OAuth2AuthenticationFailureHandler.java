package com.echo.security.oauth;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * OAuth2 로그인 실패 시 프론트엔드 로그인 페이지로 리다이렉트한다.
 */
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);
	private static final String OAUTH_FAILED_ERROR_CODE = "oauth_failed";

	@Value("${echo.frontend.url}")
	private String frontendUrl;

	@Override
	public void onAuthenticationFailure(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException exception
	) throws IOException {
		log.warn("OAuth authentication failed", exception);

		String redirectUrl = frontendUrl + "/login?error=" + OAUTH_FAILED_ERROR_CODE;

		getRedirectStrategy().sendRedirect(request, response, redirectUrl);
	}

}
