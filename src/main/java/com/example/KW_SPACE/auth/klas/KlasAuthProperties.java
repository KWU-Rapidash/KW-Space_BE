package com.example.KW_SPACE.auth.klas;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kw-space.auth.klas")
public record KlasAuthProperties(
		URI baseUrl,
		Duration connectTimeout,
		Duration readTimeout,
		String selectYearhakgi
) {

	private static final URI DEFAULT_BASE_URL = URI.create("https://klas.kw.ac.kr");
	private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);

	public KlasAuthProperties {
		if (baseUrl == null) {
			baseUrl = DEFAULT_BASE_URL;
		}
		if (connectTimeout == null) {
			connectTimeout = DEFAULT_CONNECT_TIMEOUT;
		}
		if (readTimeout == null) {
			readTimeout = DEFAULT_READ_TIMEOUT;
		}
		if (connectTimeout.isZero() || connectTimeout.isNegative()) {
			throw new IllegalArgumentException("KLAS connect timeout must be positive");
		}
		if (readTimeout.isZero() || readTimeout.isNegative()) {
			throw new IllegalArgumentException("KLAS read timeout must be positive");
		}
		if (selectYearhakgi != null && selectYearhakgi.isBlank()) {
			selectYearhakgi = null;
		}
	}

	public String resolveSelectYearhakgi(Clock clock) {
		if (selectYearhakgi != null) {
			return selectYearhakgi;
		}

		LocalDate now = LocalDate.now(clock);
		int semester = now.getMonthValue() <= 6 ? 1 : 2;
		return "%d,%d".formatted(now.getYear(), semester);
	}
}
