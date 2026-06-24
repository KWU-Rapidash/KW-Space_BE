package com.example.KW_SPACE.config;

import com.example.KW_SPACE.auth.klas.FakeKlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthServerUnavailableException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class KlasAuthConfig {

	@Bean
	@Profile({"local", "test"})
	KlasAuthClient klasAuthClient() {
		return new FakeKlasAuthClient();
	}

	@Bean
	@Profile("!local & !test")
	KlasAuthClient unconfiguredKlasAuthClient() {
		return (klasId, klasPassword) -> {
			throw new KlasAuthServerUnavailableException("Real KLAS auth client is not configured");
		};
	}
}
