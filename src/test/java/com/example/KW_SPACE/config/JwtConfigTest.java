package com.example.KW_SPACE.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.KW_SPACE.auth.jwt.JwtProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class JwtConfigTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(JwtConfig.class);

	@Test
	void registersJwtProperties() {
		contextRunner
				.withPropertyValues(
						"kw-space.auth.jwt.secret=0123456789abcdef0123456789abcdef",
						"kw-space.auth.jwt.access-token-ttl=1h"
				)
				.run(context -> {
					assertThat(context).hasNotFailed();
					JwtProperties jwtProperties = context.getBean(JwtProperties.class);

					assertThat(jwtProperties.secret()).isEqualTo("0123456789abcdef0123456789abcdef");
					assertThat(jwtProperties.accessTokenTtl()).isEqualTo(Duration.ofHours(1));
				});
	}

	@Test
	void failsFastWhenJwtSecretIsBlank() {
		contextRunner
				.withPropertyValues(
						"kw-space.auth.jwt.secret=",
						"kw-space.auth.jwt.access-token-ttl=1h"
				)
				.run(context -> assertThat(context).hasFailed());
	}
}
