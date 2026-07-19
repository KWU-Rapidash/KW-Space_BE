package com.example.KW_SPACE.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.KW_SPACE.auth.klas.FakeKlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasPageSemesterResolver;
import com.example.KW_SPACE.auth.klas.KlasSemesterResolver;
import com.example.KW_SPACE.auth.klas.RealKlasAuthClient;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class KlasAuthConfigTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(KlasAuthConfig.class)
			.withBean(Clock.class, Clock::systemUTC);

	@Test
	void registersFakeKlasAuthClientOnlyForTestProfile() {
		contextRunner
				.withPropertyValues("spring.profiles.active=test")
				.run(context -> assertThat(context.getBean(KlasAuthClient.class))
						.isInstanceOf(FakeKlasAuthClient.class));
	}

	@Test
	void registersRealKlasAuthClientWhenNotLocalOrTestProfile() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBean(KlasAuthClient.class))
					.isInstanceOf(RealKlasAuthClient.class);
			assertThat(context.getBean(KlasSemesterResolver.class))
					.isInstanceOf(KlasPageSemesterResolver.class);
		});
	}
}
