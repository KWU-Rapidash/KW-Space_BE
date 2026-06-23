package com.example.KW_SPACE.auth.klas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KlasAuthResultTest {

	@Test
	void createsSuccessResultWithKlasIdAndName() {
		KlasAuthResult result = KlasAuthResult.success("2025404000", "이효원");

		assertThat(result.authenticated()).isTrue();
		assertThat(result.klasId()).isEqualTo("2025404000");
		assertThat(result.name()).isEqualTo("이효원");
	}

	@Test
	void createsFailureResultWithoutKlasIdentity() {
		KlasAuthResult result = KlasAuthResult.failure();

		assertThat(result.authenticated()).isFalse();
		assertThat(result.klasId()).isNull();
		assertThat(result.name()).isNull();
	}
}
