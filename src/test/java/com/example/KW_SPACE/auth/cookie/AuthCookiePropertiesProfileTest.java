package com.example.KW_SPACE.auth.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.KW_SPACE.config.AuthCookieConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuthCookiePropertiesProfileTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withInitializer(new ConfigDataApplicationContextInitializer())
			.withUserConfiguration(AuthCookieConfig.class);

	@Test
	void localProfileUsesInsecureCookie() {
		contextRunner
				.withPropertyValues("spring.profiles.active=local")
				.run(context -> assertThat(context.getBean(AuthCookieProperties.class).secure()).isFalse());
	}

	@Test
	void testProfileUsesInsecureCookie() {
		contextRunner
				.withPropertyValues("spring.profiles.active=test")
				.run(context -> assertThat(context.getBean(AuthCookieProperties.class).secure()).isFalse());
	}

	@Test
	void prodProfileUsesSecureCookie() {
		contextRunner
				.withPropertyValues("spring.profiles.active=prod")
				.run(context -> assertThat(context.getBean(AuthCookieProperties.class).secure()).isTrue());
	}

	@Test
	void defaultPropertiesUseCommonCookieAttributes() {
		contextRunner
				.withPropertyValues("spring.profiles.active=prod")
				.run(context -> {
					AuthCookieProperties properties = context.getBean(AuthCookieProperties.class);

					assertThat(properties.accessTokenName()).isEqualTo("accessToken");
					assertThat(properties.httpOnly()).isTrue();
					assertThat(properties.sameSite()).isEqualTo("Lax");
					assertThat(properties.path()).isEqualTo("/");
					assertThat(properties.maxAge()).isEqualTo(Duration.ofHours(1));
				});
	}
}
