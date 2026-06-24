package com.example.KW_SPACE.config;

import com.example.KW_SPACE.auth.klas.FakeKlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthProperties;
import com.example.KW_SPACE.auth.klas.RealKlasAuthClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KlasAuthProperties.class)
public class KlasAuthConfig {

	@Bean
	@Profile({"local", "test"})
	KlasAuthClient klasAuthClient() {
		return new FakeKlasAuthClient();
	}

	@Bean
	@Profile("!local & !test")
	KlasAuthClient realKlasAuthClient(KlasAuthProperties properties, ObjectMapper objectMapper, Clock clock) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());

		RestClient restClient = RestClient.builder()
				.baseUrl(properties.baseUrl().toString())
				.requestFactory(requestFactory)
				.build();

		return new RealKlasAuthClient(restClient, objectMapper, properties, clock);
	}
}
