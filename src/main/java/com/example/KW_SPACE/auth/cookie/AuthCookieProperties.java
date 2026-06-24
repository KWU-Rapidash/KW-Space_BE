package com.example.KW_SPACE.auth.cookie;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kw-space.auth.cookie")
public record AuthCookieProperties(
		String accessTokenName,
		Boolean secure,
		Boolean httpOnly,
		String sameSite,
		String path,
		Duration maxAge
) {

	public AuthCookieProperties {
		if (accessTokenName == null || accessTokenName.isBlank()) {
			accessTokenName = "accessToken";
		}
		if (secure == null) {
			secure = true;
		}
		if (httpOnly == null) {
			httpOnly = true;
		}
		if (sameSite == null || sameSite.isBlank()) {
			sameSite = "Lax";
		}
		if (path == null || path.isBlank()) {
			path = "/";
		}
		if (maxAge == null || maxAge.isZero() || maxAge.isNegative()) {
			maxAge = Duration.ofHours(1);
		}
	}
}
