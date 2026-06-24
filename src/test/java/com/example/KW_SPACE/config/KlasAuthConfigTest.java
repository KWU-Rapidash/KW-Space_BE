package com.example.KW_SPACE.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.KW_SPACE.auth.klas.FakeKlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthServerUnavailableException;
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
	void registersUnavailableKlasAuthClientWhenRealClientIsNotConfigured() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			KlasAuthClient klasAuthClient = context.getBean(KlasAuthClient.class);

			assertThatThrownBy(() -> klasAuthClient.verify("2025404000", "klas-password"))
					.isInstanceOf(KlasAuthServerUnavailableException.class)
					.hasMessage("Real KLAS auth client is not configured");
		});
	}
}
