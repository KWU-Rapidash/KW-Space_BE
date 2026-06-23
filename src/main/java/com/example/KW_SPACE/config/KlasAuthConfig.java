package com.example.KW_SPACE.config;

import com.example.KW_SPACE.auth.klas.FakeKlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KlasAuthConfig {

	@Bean
	KlasAuthClient klasAuthClient() {
		return new FakeKlasAuthClient();
	}
}
