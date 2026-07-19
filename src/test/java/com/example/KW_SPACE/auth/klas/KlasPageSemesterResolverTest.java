package com.example.KW_SPACE.auth.klas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.COOKIE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KlasPageSemesterResolverTest {

	private static final String BASE_URL = "https://klas.example.test";
	private static final String SEMESTER_PAGE_URL = BASE_URL + "/std/cps/inqire/StandStdPage.do";
	private static final String COOKIE_HEADER = "JSESSIONID=<session-cookie>";

	private MutableClock clock;
	private MockRestServiceServer server;
	private RestClient restClient;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(Instant.parse("2026-07-14T00:00:00Z"), ZoneId.of("Asia/Seoul"));
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		restClient = builder.build();
	}

	@Test
	void resolvesSemesterFromKlasPageInitializationData() {
		KlasPageSemesterResolver resolver = resolver(null);
		expectPage("""
				<script>
				  appModule.$data.selectYear = 2026;
				  appModule.$data.selectHakgi = "1";
				</script>
				""");

		String result = resolver.resolve(COOKIE_HEADER);

		assertThat(result).isEqualTo("2026,1");
		server.verify();
	}

	@Test
	void resolvesSemesterFromObjectInitializationData() {
		KlasPageSemesterResolver resolver = resolver(null);
		expectPage("""
				<script>
				  const data = {"selectYear":"2027","selectHakgi":2};
				</script>
				""");

		assertThat(resolver.resolve(COOKIE_HEADER)).isEqualTo("2027,2");
		server.verify();
	}

	@Test
	void usesConfiguredFallbackWhenPageHasNoSemesterInitialization() {
		KlasPageSemesterResolver resolver = resolver("2026,1");
		expectPage("<html><form action='/usr/cmn/login/LoginForm.do'></form></html>");

		assertThat(resolver.resolve(COOKIE_HEADER)).isEqualTo("2026,1");
		server.verify();
	}

	@Test
	void usesConfiguredFallbackWhenSemesterPageRequestFails() {
		KlasPageSemesterResolver resolver = resolver("2026,2");
		server.expect(requestTo(SEMESTER_PAGE_URL))
				.andExpect(method(GET))
				.andRespond(withServerError());

		assertThat(resolver.resolve(COOKIE_HEADER)).isEqualTo("2026,2");
		server.verify();
	}

	@Test
	void rejectsMissingSemesterWithoutConfiguredFallback() {
		KlasPageSemesterResolver resolver = resolver(null);
		expectPage("<html><body>login required</body></html>");

		assertThatThrownBy(() -> resolver.resolve(COOKIE_HEADER))
				.isInstanceOf(KlasAuthServerUnavailableException.class)
				.hasMessage("KLAS semester is unavailable");
		server.verify();
	}

	@Test
	void rejectsSemesterYearOutsideAllowedRange() {
		KlasPageSemesterResolver resolver = resolver(null);
		expectPage("""
				<script>
				  appModule.$data.selectYear = 2028;
				  appModule.$data.selectHakgi = "1";
				</script>
				""");

		assertThatThrownBy(() -> resolver.resolve(COOKIE_HEADER))
				.isInstanceOf(KlasAuthServerUnavailableException.class);
		server.verify();
	}

	@Test
	void rejectsInvalidConfiguredFallbackYear() {
		KlasPageSemesterResolver resolver = resolver("2099,1");
		expectPage("<html><body>semester data unavailable</body></html>");

		assertThatThrownBy(() -> resolver.resolve(COOKIE_HEADER))
				.isInstanceOf(KlasAuthServerUnavailableException.class);
		server.verify();
	}

	@Test
	void cachesOnlyValidatedSemesterUntilTtlExpires() {
		KlasPageSemesterResolver resolver = resolver(null);
		expectPage("""
				<script>const data = {selectYear: 2026, selectHakgi: "1"};</script>
				""");
		expectPage("JSESSIONID=<another-session-cookie>", """
				<script>const data = {selectYear: 2026, selectHakgi: "2"};</script>
				""");

		assertThat(resolver.resolve(COOKIE_HEADER)).isEqualTo("2026,1");
		assertThat(resolver.resolve("JSESSIONID=<another-session-cookie>")).isEqualTo("2026,1");

		clock.advance(Duration.ofHours(1));
		assertThat(resolver.resolve("JSESSIONID=<another-session-cookie>")).isEqualTo("2026,2");
		server.verify();
	}

	private KlasPageSemesterResolver resolver(String configuredSemester) {
		KlasAuthProperties properties = new KlasAuthProperties(
				URI.create(BASE_URL),
				Duration.ofSeconds(1),
				Duration.ofSeconds(1),
				configuredSemester
		);
		return new KlasPageSemesterResolver(restClient, properties, clock);
	}

	private void expectPage(String html) {
		expectPage(COOKIE_HEADER, html);
	}

	private void expectPage(String cookieHeader, String html) {
		server.expect(requestTo(SEMESTER_PAGE_URL))
				.andExpect(method(GET))
				.andExpect(header(COOKIE, cookieHeader))
				.andRespond(withSuccess(html, TEXT_HTML));
	}

	private static final class MutableClock extends Clock {

		private Instant instant;
		private final ZoneId zoneId;

		private MutableClock(Instant instant, ZoneId zoneId) {
			this.instant = instant;
			this.zoneId = zoneId;
		}

		@Override
		public ZoneId getZone() {
			return zoneId;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return new MutableClock(instant, zone);
		}

		@Override
		public Instant instant() {
			return instant;
		}

		private void advance(Duration duration) {
			instant = instant.plus(duration);
		}
	}
}
