package com.example.KW_SPACE.auth.klas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Objects;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Tag("local-e2e")
class RealKlasAuthClientLocalE2ETest {

	private static final URI KLAS_BASE_URL = URI.create("https://klas.kw.ac.kr");
	private static final ZoneId KLAS_ZONE_ID = ZoneId.of("Asia/Seoul");

	@Test
	void verifiesRealKlasAccountAndStudentInfoFlow() {
		String klasId = requiredCredential("KLAS_E2E_ID");
		String klasPassword = requiredCredential("KLAS_E2E_PASSWORD");
		KlasAuthProperties properties = new KlasAuthProperties(
				KLAS_BASE_URL,
				Duration.ofSeconds(3),
				Duration.ofSeconds(10),
				null
		);
		RestClient restClient = restClient(properties);
		Clock clock = Clock.system(KLAS_ZONE_ID);
		KlasSemesterResolver semesterResolver = new KlasPageSemesterResolver(restClient, properties, clock);
		KlasAuthClient client = new RealKlasAuthClient(
				restClient,
				new ObjectMapper(),
				properties,
				semesterResolver
		);

		KlasAuthResult result = verifyWithoutSensitiveFailureDetails(client, klasId, klasPassword);

		assertTrue(result.authenticated());
		assertTrue(Objects.equals(klasId, result.klasId()));
		assertTrue(result.name() != null && !result.name().isBlank());
	}

	private static KlasAuthResult verifyWithoutSensitiveFailureDetails(
			KlasAuthClient client, String klasId, String klasPassword) {
		try {
			return client.verify(klasId, klasPassword);
		} catch (RuntimeException exception) {
			throw new AssertionError("Real KLAS E2E flow failed");
		}
	}

	private static RestClient restClient(KlasAuthProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());

		return RestClient.builder()
				.baseUrl(properties.baseUrl().toString())
				.requestFactory(requestFactory)
				.build();
	}

	private static String requiredCredential(String environmentVariable) {
		String value = System.getenv(environmentVariable);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("KLAS E2E credentials are not configured");
		}
		return value;
	}
}
