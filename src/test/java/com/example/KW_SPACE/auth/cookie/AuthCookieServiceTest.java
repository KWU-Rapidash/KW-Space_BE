package com.example.KW_SPACE.auth.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class AuthCookieServiceTest {

	@Test
	void createAccessTokenCookieUsesConfiguredCommonAttributes() {
		AuthCookieService authCookieService = new AuthCookieService(new AuthCookieProperties(
				"accessToken",
				true,
				true,
				"Lax",
				"/",
				Duration.ofHours(1)
		));

		ResponseCookie cookie = authCookieService.createAccessTokenCookie("jwt-token");

		assertThat(cookie.toString()).contains(
				"accessToken=jwt-token",
				"Path=/",
				"Max-Age=3600",
				"Secure",
				"HttpOnly",
				"SameSite=Lax"
		);
	}

	@Test
	void createAccessTokenCookieCanDisableSecureForLocalProfile() {
		AuthCookieService authCookieService = new AuthCookieService(new AuthCookieProperties(
				"accessToken",
				false,
				true,
				"Lax",
				"/",
				Duration.ofHours(1)
		));

		ResponseCookie cookie = authCookieService.createAccessTokenCookie("jwt-token");

		assertThat(cookie.toString()).doesNotContain("Secure");
		assertThat(cookie.toString()).contains("HttpOnly", "SameSite=Lax", "Max-Age=3600");
	}

	@Test
	void deleteAccessTokenCookieUsesSameAttributesWithZeroMaxAge() {
		AuthCookieService authCookieService = new AuthCookieService(new AuthCookieProperties(
				"accessToken",
				true,
				true,
				"Lax",
				"/",
				Duration.ofHours(1)
		));

		ResponseCookie cookie = authCookieService.deleteAccessTokenCookie();

		assertThat(cookie.toString()).contains(
				"accessToken=",
				"Path=/",
				"Max-Age=0",
				"Secure",
				"HttpOnly",
				"SameSite=Lax"
		);
	}
}
