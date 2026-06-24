package com.example.KW_SPACE.auth.cookie;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {

	private final AuthCookieProperties authCookieProperties;

	public AuthCookieService(AuthCookieProperties authCookieProperties) {
		this.authCookieProperties = authCookieProperties;
	}

	public ResponseCookie createAccessTokenCookie(String accessToken) {
		return baseAccessTokenCookie(accessToken)
				.maxAge(authCookieProperties.maxAge())
				.build();
	}

	public ResponseCookie deleteAccessTokenCookie() {
		return baseAccessTokenCookie("")
				.maxAge(Duration.ZERO)
				.build();
	}

	private ResponseCookie.ResponseCookieBuilder baseAccessTokenCookie(String value) {
		return ResponseCookie.from(authCookieProperties.accessTokenName(), value)
				.httpOnly(authCookieProperties.httpOnly())
				.secure(authCookieProperties.secure())
				.sameSite(authCookieProperties.sameSite())
				.path(authCookieProperties.path());
	}
}
