package com.example.KW_SPACE.auth.klas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FakeKlasAuthClientTest {

	@Test
	void verifiesRegisteredAccount() {
		FakeKlasAuthClient client = FakeKlasAuthClient.builder()
				.account("2025404000", "correct-password", "이효원")
				.build();

		KlasAuthResult result = client.verify("2025404000", "correct-password");

		assertThat(result.authenticated()).isTrue();
		assertThat(result.klasId()).isEqualTo("2025404000");
		assertThat(result.name()).isEqualTo("이효원");
	}

	@Test
	void rejectsWrongPassword() {
		FakeKlasAuthClient client = FakeKlasAuthClient.builder()
				.account("2025404000", "correct-password", "이효원")
				.build();

		KlasAuthResult result = client.verify("2025404000", "wrong-password");

		assertThat(result).isEqualTo(KlasAuthResult.failure());
	}

	@Test
	void rejectsUnknownKlasId() {
		FakeKlasAuthClient client = FakeKlasAuthClient.builder()
				.account("2025404000", "correct-password", "이효원")
				.build();

		KlasAuthResult result = client.verify("2025404999", "correct-password");

		assertThat(result).isEqualTo(KlasAuthResult.failure());
	}

	@Test
	void throwsServerUnavailableExceptionWhenUnavailable() {
		FakeKlasAuthClient client = FakeKlasAuthClient.unavailable();

		assertThatThrownBy(() -> client.verify("2025404000", "secret-password"))
				.isInstanceOf(KlasAuthServerUnavailableException.class)
				.hasMessage("KLAS authentication server is unavailable")
				.hasMessageNotContaining("secret-password");
	}
}
