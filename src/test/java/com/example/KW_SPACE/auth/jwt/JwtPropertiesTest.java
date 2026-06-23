package com.example.KW_SPACE.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

	@Test
	void createsJwtProperties() {
		JwtProperties properties = new JwtProperties("secret-value", Duration.ofHours(1));

		assertThat(properties.secret()).isEqualTo("secret-value");
		assertThat(properties.accessTokenTtl()).isEqualTo(Duration.ofHours(1));
	}

	@Test
	void rejectsBlankSecret() {
		assertThatThrownBy(() -> new JwtProperties(" ", Duration.ofHours(1)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("JWT secret must not be blank");
	}

	@Test
	void rejectsNonPositiveTtl() {
		assertThatThrownBy(() -> new JwtProperties("secret-value", Duration.ZERO))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("JWT access token ttl must be positive");
	}
}
