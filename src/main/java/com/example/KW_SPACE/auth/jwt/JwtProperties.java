package com.example.KW_SPACE.auth.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kw-space.auth.jwt")
public record JwtProperties(
		String secret,
		Duration accessTokenTtl
) {

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("JWT secret must not be blank");
		}
		if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
			throw new IllegalArgumentException("JWT access token ttl must be positive");
		}
	}
}
