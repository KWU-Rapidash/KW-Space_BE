package com.example.KW_SPACE.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

	@Test
	void createsJwtProperties() {
		String secret = "0123456789abcdef0123456789abcdef";

		JwtProperties properties = new JwtProperties(secret, Duration.ofHours(1));

		assertThat(properties.secret()).isEqualTo(secret);
		assertThat(properties.accessTokenTtl()).isEqualTo(Duration.ofHours(1));
	}

	@Test
	void rejectsBlankSecret() {
		assertThatThrownBy(() -> new JwtProperties(" ", Duration.ofHours(1)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("JWT secret must not be blank");
	}

	@Test
	void rejectsShortSecret() {
		assertThatThrownBy(() -> new JwtProperties("short-secret", Duration.ofHours(1)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("JWT secret must be at least 32 bytes");
	}

	@Test
	void rejectsNonPositiveTtl() {
		assertThatThrownBy(() -> new JwtProperties("0123456789abcdef0123456789abcdef", Duration.ZERO))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("JWT access token ttl must be positive");
	}
}
