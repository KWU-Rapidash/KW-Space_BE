package com.example.KW_SPACE.config;

import com.example.KW_SPACE.auth.klas.FakeKlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthProperties;
import com.example.KW_SPACE.auth.klas.KlasPageSemesterResolver;
import com.example.KW_SPACE.auth.klas.KlasSemesterResolver;
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
	RestClient klasRestClient(KlasAuthProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());

		return RestClient.builder()
				.baseUrl(properties.baseUrl().toString())
				.requestFactory(requestFactory)
				.build();
	}

	@Bean
	@Profile("!local & !test")
	KlasSemesterResolver klasSemesterResolver(RestClient klasRestClient, KlasAuthProperties properties, Clock clock) {
		return new KlasPageSemesterResolver(klasRestClient, properties, clock);
	}

	@Bean
	@Profile("!local & !test")
	KlasAuthClient realKlasAuthClient(RestClient klasRestClient, ObjectMapper objectMapper,
			KlasAuthProperties properties, KlasSemesterResolver semesterResolver) {
		return new RealKlasAuthClient(klasRestClient, objectMapper, properties, semesterResolver);
	}
}
