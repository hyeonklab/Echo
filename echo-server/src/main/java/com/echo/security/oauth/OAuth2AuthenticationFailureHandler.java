package com.echo.security.oauth;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * OAuth2 로그인 실패 시 프론트엔드 로그인 페이지로 에러와 함께 리다이렉트한다.
 */
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	@Value("${echo.frontend.url}")
	private String frontendUrl;

	@Override
	public void onAuthenticationFailure(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException exception
	) throws IOException {
		String message = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
		String redirectUrl = frontendUrl + "/login?error=" + message;

		getRedirectStrategy().sendRedirect(request, response, redirectUrl);
	}

}
