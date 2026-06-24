package com.example.KW_SPACE.auth.klas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.COOKIE;
import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import javax.crypto.Cipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

class RealKlasAuthClientTest {

	private static final String BASE_URL = "https://klas.example.test";
	private static final String KLAS_ID = "<student-id>";
	private static final String KLAS_PASSWORD = "<student-password>";
	private static final String STUDENT_NAME = "<student-name>";
	private static final String COOKIE_HEADER = "JSESSIONID=<session-cookie>; WMONID=<wmonid>";

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));
	private KeyPair keyPair;
	private MockRestServiceServer server;
	private RealKlasAuthClient client;

	@BeforeEach
	void setUp() throws Exception {
		keyPair = generateKeyPair();
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		client = new RealKlasAuthClient(
				builder.build(),
				objectMapper,
				new KlasAuthProperties(URI.create(BASE_URL), Duration.ofSeconds(1), Duration.ofSeconds(1), null),
				clock
		);
	}

	@Test
	void verifiesKlasAccountAndMapsStudentName() {
		expectLoginSecurity();
		expectLoginConfirmSuccess();
		server.expect(requestTo(BASE_URL + "/std/cps/inqire/ToeicInfoStd.do"))
				.andExpect(method(POST))
				.andExpect(header(COOKIE, COOKIE_HEADER))
				.andExpect(content().json("""
						{"selectYearhakgi":"2026,1","selectChangeYn":"Y"}
						"""))
				.andRespond(withSuccess("""
						[{"hakbun":"<student-id>","kname":"<student-name>"}]
						""", APPLICATION_JSON));

		KlasAuthResult result = client.verify(KLAS_ID, KLAS_PASSWORD);

		assertThat(result.authenticated()).isTrue();
		assertThat(result.klasId()).isEqualTo(KLAS_ID);
		assertThat(result.name()).isEqualTo(STUDENT_NAME);
		server.verify();
	}

	@Test
	void returnsFailureWhenKlasRejectsCredentials() {
		expectLoginSecurity();
		server.expect(requestTo(BASE_URL + "/usr/cmn/login/LoginConfirm.do"))
				.andExpect(method(POST))
				.andExpect(header(COOKIE, COOKIE_HEADER))
				.andExpect(encryptedLoginTokenPayload())
				.andRespond(withSuccess("""
						{"loginRequired":false,"errorCount":1,"fieldErrors":[{"message":"ignored external message"}]}
						""", APPLICATION_JSON));

		KlasAuthResult result = client.verify(KLAS_ID, KLAS_PASSWORD);

		assertThat(result).isEqualTo(KlasAuthResult.failure());
		server.verify();
	}

	@Test
	void throwsServerUnavailableWhenLoginSecurityResponseHasNoSessionCookie() {
		server.expect(requestTo(BASE_URL + "/usr/cmn/login/LoginSecurity.do"))
				.andRespond(withSuccess("""
						{"publicKey":"%s"}
						""".formatted(publicKeyBody()), APPLICATION_JSON));

		assertThatThrownBy(() -> client.verify(KLAS_ID, KLAS_PASSWORD))
				.isInstanceOf(KlasAuthServerUnavailableException.class)
				.hasMessage("KLAS authentication server is unavailable")
				.hasMessageNotContaining(KLAS_PASSWORD);
		server.verify();
	}

	@Test
	void throwsServerUnavailableWhenKlasHttpCallFails() {
		server.expect(requestTo(BASE_URL + "/usr/cmn/login/LoginSecurity.do"))
				.andRespond(withServerError());

		assertThatThrownBy(() -> client.verify(KLAS_ID, KLAS_PASSWORD))
				.isInstanceOf(KlasAuthServerUnavailableException.class)
				.hasMessage("KLAS authentication server is unavailable")
				.hasMessageNotContaining(KLAS_PASSWORD);
		server.verify();
	}

	@Test
	void throwsServerUnavailableWhenStudentInfoDoesNotMatchLoginSession() {
		expectLoginSecurity();
		expectLoginConfirmSuccess();
		server.expect(requestTo(BASE_URL + "/std/cps/inqire/ToeicInfoStd.do"))
				.andRespond(withSuccess("""
						[{"hakbun":"<other-student-id>","kname":"<student-name>"}]
						""", APPLICATION_JSON));

		assertThatThrownBy(() -> client.verify(KLAS_ID, KLAS_PASSWORD))
				.isInstanceOf(KlasAuthServerUnavailableException.class)
				.hasMessage("KLAS authentication server is unavailable")
				.hasMessageNotContaining(KLAS_PASSWORD);
		server.verify();
	}

	private void expectLoginSecurity() {
		server.expect(requestTo(BASE_URL + "/usr/cmn/login/LoginSecurity.do"))
				.andExpect(method(POST))
				.andExpect(content().json("{}"))
				.andRespond(withSuccess("""
						{"publicKey":"%s"}
						""".formatted(publicKeyBody()), APPLICATION_JSON)
						.header(SET_COOKIE, "JSESSIONID=<session-cookie>; Path=/; HttpOnly")
						.header(SET_COOKIE, "WMONID=<wmonid>; Path=/"));
	}

	private void expectLoginConfirmSuccess() {
		server.expect(requestTo(BASE_URL + "/usr/cmn/login/LoginConfirm.do"))
				.andExpect(method(POST))
				.andExpect(header(COOKIE, COOKIE_HEADER))
				.andExpect(encryptedLoginTokenPayload())
				.andRespond(withSuccess("""
						{
						  "loginRequired": false,
						  "errorCount": 0,
						  "response": {"userId": "<klas-user-id>"}
						}
						""", APPLICATION_JSON));
	}

	private RequestMatcher encryptedLoginTokenPayload() {
		return request -> {
			String body = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
			JsonNode json = objectMapper.readTree(body);
			String loginToken = json.get("loginToken").asText();

			assertThat(loginToken).doesNotContain(KLAS_ID);
			assertThat(loginToken).doesNotContain(KLAS_PASSWORD);
			assertThat(json.get("redirectUrl").asText()).isEmpty();
			assertThat(json.get("redirectTabUrl").asText()).isEmpty();

			JsonNode payload = objectMapper.readTree(decryptLoginToken(loginToken));
			assertThat(payload.get("loginId").asText()).isEqualTo(KLAS_ID);
			assertThat(payload.get("loginPwd").asText()).isEqualTo(KLAS_PASSWORD);
			assertThat(payload.get("storeIdYn").asText()).isEqualTo("N");
		};
	}

	private String decryptLoginToken(String loginToken) {
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
			byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(loginToken));
			return new String(decrypted, StandardCharsets.UTF_8);
		} catch (GeneralSecurityException exception) {
			throw new AssertionError("loginToken should be decryptable with the test private key", exception);
		}
	}

	private String publicKeyBody() {
		return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
	}

	private static KeyPair generateKeyPair() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		return generator.generateKeyPair();
	}
}
