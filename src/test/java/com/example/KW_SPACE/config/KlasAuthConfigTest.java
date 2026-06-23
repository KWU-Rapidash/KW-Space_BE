package com.example.KW_SPACE.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.KW_SPACE.auth.klas.FakeKlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class KlasAuthConfigTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(KlasAuthConfig.class);

	@Test
	void registersFakeKlasAuthClientOnlyForTestProfile() {
		contextRunner
				.withPropertyValues("spring.profiles.active=test")
				.run(context -> assertThat(context.getBean(KlasAuthClient.class))
						.isInstanceOf(FakeKlasAuthClient.class));
	}

	@Test
	void failsFastWhenRealKlasClientIsNotConfigured() {
		contextRunner.run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure())
					.hasRootCauseMessage("Real KLAS auth client is not configured");
		});
	}
}
