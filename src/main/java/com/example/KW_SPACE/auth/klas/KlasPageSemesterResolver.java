package com.example.KW_SPACE.auth.klas;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class KlasPageSemesterResolver implements KlasSemesterResolver {

	private static final String SEMESTER_PAGE_PATH = "/std/cps/inqire/StandStdPage.do";
	private static final int MIN_YEAR = 2000;
	private static final Duration CACHE_TTL = Duration.ofHours(1);
	private static final Pattern SELECT_YEAR_PATTERN = Pattern.compile(
			"[\\\"']?\\bselectYear\\b[\\\"']?\\s*[:=]\\s*[\\\"']?(\\d{4})[\\\"']?"
	);
	private static final Pattern SELECT_SEMESTER_PATTERN = Pattern.compile(
			"[\\\"']?\\bselect[Hh]akgi\\b[\\\"']?\\s*[:=]\\s*[\\\"']?([12])[\\\"']?"
	);

	private final RestClient restClient;
	private final KlasAuthProperties properties;
	private final Clock clock;
	private volatile CacheEntry cachedSemester;

	public KlasPageSemesterResolver(RestClient restClient, KlasAuthProperties properties, Clock clock) {
		this.restClient = restClient;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public String resolve(String cookieHeader) {
		Instant now = clock.instant();
		CacheEntry current = cachedSemester;
		if (current != null && current.isValidAt(now)) {
			return current.value();
		}

		synchronized (this) {
			current = cachedSemester;
			if (current != null && current.isValidAt(now)) {
				return current.value();
			}

			String resolved = resolveUncached(cookieHeader);
			cachedSemester = new CacheEntry(resolved, now.plus(CACHE_TTL));
			return resolved;
		}
	}

	private String resolveUncached(String cookieHeader) {
		try {
			return resolveFromPage(cookieHeader);
		} catch (KlasAuthServerUnavailableException exception) {
			return configuredFallback()
					.orElseThrow(() -> exception);
		}
	}

	private String resolveFromPage(String cookieHeader) {
		try {
			String html = restClient.get()
					.uri(SEMESTER_PAGE_PATH)
					.accept(MediaType.TEXT_HTML, MediaType.ALL)
					.header(HttpHeaders.COOKIE, cookieHeader)
					.retrieve()
					.body(String.class);

			if (!StringUtils.hasText(html)) {
				throw serverUnavailable();
			}

			return validatedSemester(extract(SELECT_YEAR_PATTERN, html), extract(SELECT_SEMESTER_PATTERN, html));
		} catch (KlasAuthServerUnavailableException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw serverUnavailable(exception);
		}
	}

	private Optional<String> configuredFallback() {
		if (!StringUtils.hasText(properties.selectYearhakgi())) {
			return Optional.empty();
		}

		String[] yearAndSemester = properties.selectYearhakgi().split(",", 2);
		try {
			return Optional.of(validatedSemester(yearAndSemester[0], yearAndSemester[1]));
		} catch (KlasAuthServerUnavailableException exception) {
			return Optional.empty();
		}
	}

	private String extract(Pattern pattern, String html) {
		Matcher matcher = pattern.matcher(html);
		if (!matcher.find()) {
			throw serverUnavailable();
		}
		return matcher.group(1);
	}

	private String validatedSemester(String yearValue, String semesterValue) {
		int year;
		try {
			year = Integer.parseInt(yearValue);
		} catch (NumberFormatException exception) {
			throw serverUnavailable(exception);
		}

		int maximumYear = Year.now(clock).getValue() + 1;
		if (year < MIN_YEAR || year > maximumYear || !("1".equals(semesterValue) || "2".equals(semesterValue))) {
			throw serverUnavailable();
		}

		return "%d,%s".formatted(year, semesterValue);
	}

	private KlasAuthServerUnavailableException serverUnavailable() {
		return new KlasAuthServerUnavailableException("KLAS semester is unavailable");
	}

	private KlasAuthServerUnavailableException serverUnavailable(Throwable cause) {
		return new KlasAuthServerUnavailableException("KLAS semester is unavailable", cause);
	}

	private record CacheEntry(String value, Instant expiresAt) {

		private boolean isValidAt(Instant instant) {
			return instant.isBefore(expiresAt);
		}
	}
}
