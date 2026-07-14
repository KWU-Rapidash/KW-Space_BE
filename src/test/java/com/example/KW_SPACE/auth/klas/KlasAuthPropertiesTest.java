package com.example.KW_SPACE.auth.klas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class KlasAuthPropertiesTest {

	@Test
	void appliesDefaults() {
		KlasAuthProperties properties = new KlasAuthProperties(null, null, null, null);

		assertThat(properties.baseUrl()).isEqualTo(URI.create("https://klas.kw.ac.kr"));
		assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
		assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(5));
		assertThat(properties.selectYearhakgi()).isNull();
	}

	@Test
	void rejectsNonPositiveTimeouts() {
		assertThatThrownBy(() -> new KlasAuthProperties(
				URI.create("https://klas.example.test"),
				Duration.ZERO,
				Duration.ofSeconds(5),
				null
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("KLAS connect timeout must be positive");

		assertThatThrownBy(() -> new KlasAuthProperties(
				URI.create("https://klas.example.test"),
				Duration.ofSeconds(3),
				Duration.ofSeconds(-1),
				null
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("KLAS read timeout must be positive");
	}

	@Test
	void normalizesConfiguredSemester() {
		KlasAuthProperties blank = new KlasAuthProperties(
				URI.create("https://klas.example.test"),
				Duration.ofSeconds(1),
				Duration.ofSeconds(1),
				" "
		);
		KlasAuthProperties configured = new KlasAuthProperties(
				URI.create("https://klas.example.test"),
				Duration.ofSeconds(1),
				Duration.ofSeconds(1),
				" 2026,1 "
		);

		assertThat(blank.selectYearhakgi()).isNull();
		assertThat(configured.selectYearhakgi()).isEqualTo("2026,1");
	}

	@Test
	void rejectsMalformedYearSemester() {
		assertThatThrownBy(() -> new KlasAuthProperties(
				URI.create("https://klas.example.test"),
				Duration.ofSeconds(1),
				Duration.ofSeconds(1),
				"2026,3"
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("selectYearhakgi must be in format yyyy,1 or yyyy,2");

		assertThatThrownBy(() -> new KlasAuthProperties(
				URI.create("https://klas.example.test"),
				Duration.ofSeconds(1),
				Duration.ofSeconds(1),
				"current"
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("selectYearhakgi must be in format yyyy,1 or yyyy,2");
	}
}
