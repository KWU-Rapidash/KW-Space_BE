package com.example.KW_SPACE.auth.klas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
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
	void resolvesYearSemesterFromConfiguredValueOrClock() {
		Clock firstSemester = Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		Clock secondSemester = Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		KlasAuthProperties derived = new KlasAuthProperties(
				URI.create("https://klas.example.test"),
				Duration.ofSeconds(1),
				Duration.ofSeconds(1),
				""
		);
		KlasAuthProperties configured = new KlasAuthProperties(
				URI.create("https://klas.example.test"),
				Duration.ofSeconds(1),
				Duration.ofSeconds(1),
				"2025,2"
		);

		assertThat(derived.resolveSelectYearhakgi(firstSemester)).isEqualTo("2026,1");
		assertThat(derived.resolveSelectYearhakgi(secondSemester)).isEqualTo("2026,2");
		assertThat(configured.resolveSelectYearhakgi(firstSemester)).isEqualTo("2025,2");
	}
}
