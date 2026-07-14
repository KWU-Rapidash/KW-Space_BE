package com.example.KW_SPACE.auth.klas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.security.interfaces.RSAKey;
import java.time.Duration;
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
	private static final String CONFIRMED_COOKIE_HEADER =
			"JSESSIONID=<session-cookie>; WMONID=<confirmed-wmonid>; KLASSESSION=<confirmed-session>";

	private final ObjectMapper objectMapper = new ObjectMapper();
	private KeyPair keyPair;
	private MockRestServiceServer server;
	private KlasSemesterResolver semesterResolver;
	private RealKlasAuthClient client;

	@BeforeEach
	void setUp() throws Exception {
		keyPair = generateKeyPair();
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		semesterResolver = mock(KlasSemesterResolver.class);
		given(semesterResolver.resolve(CONFIRMED_COOKIE_HEADER)).willReturn("2026,1");
		client = new RealKlasAuthClient(
				builder.build(),
				objectMapper,
				new KlasAuthProperties(URI.create(BASE_URL), Duration.ofSeconds(1), Duration.ofSeconds(1), null),
				semesterResolver
		);
	}

	@Test
	void verifiesKlasAccountAndMapsStudentName() {
		expectLoginSecurity();
		expectLoginConfirmSuccess();
		expectStudentInfoSuccess();

		KlasAuthResult result = client.verify(KLAS_ID, KLAS_PASSWORD);

		assertThat(result.authenticated()).isTrue();
		assertThat(result.klasId()).isEqualTo(KLAS_ID);
		assertThat(result.name()).isEqualTo(STUDENT_NAME);
		verify(semesterResolver).resolve(CONFIRMED_COOKIE_HEADER);
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
		verifyNoInteractions(semesterResolver);
		server.verify();
	}

	@Test
	void acceptsLoginPayloadAtPkcs1SizeLimit() throws Exception {
		String boundaryPassword = passwordForPayloadSize(maxPkcs1PayloadBytes());
		expectLoginSecurity();
		expectLoginConfirmSuccess(boundaryPassword);
		expectStudentInfoSuccess();

		KlasAuthResult result = client.verify(KLAS_ID, boundaryPassword);

		assertThat(result.authenticated()).isTrue();
		server.verify();
	}

	@Test
	void rejectsLoginPayloadOverPkcs1SizeLimitWithoutLoginConfirm() throws Exception {
		String oversizedPassword = passwordForPayloadSize(maxPkcs1PayloadBytes() + 1);
		expectLoginSecurity();

		KlasAuthResult result = client.verify(KLAS_ID, oversizedPassword);

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

	@Test
	void throwsServerUnavailableWhenStudentInfoFirstRowIsNull() {
		expectLoginSecurity();
		expectLoginConfirmSuccess();
		server.expect(requestTo(BASE_URL + "/std/cps/inqire/ToeicInfoStd.do"))
				.andRespond(withSuccess("""
						[null]
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
		expectLoginConfirmSuccess(KLAS_PASSWORD);
	}

	private void expectLoginConfirmSuccess(String klasPassword) {
		server.expect(requestTo(BASE_URL + "/usr/cmn/login/LoginConfirm.do"))
				.andExpect(method(POST))
				.andExpect(header(COOKIE, COOKIE_HEADER))
				.andExpect(encryptedLoginTokenPayload(klasPassword))
				.andRespond(withSuccess("""
						{
						  "loginRequired": false,
						  "errorCount": 0,
						  "response": {"userId": "<klas-user-id>"}
						}
						""", APPLICATION_JSON)
						.header(SET_COOKIE, "WMONID=<confirmed-wmonid>; Path=/")
						.header(SET_COOKIE, "KLASSESSION=<confirmed-session>; Path=/; HttpOnly"));
	}

	private void expectStudentInfoSuccess() {
		server.expect(requestTo(BASE_URL + "/std/cps/inqire/ToeicInfoStd.do"))
				.andExpect(method(POST))
				.andExpect(header(COOKIE, CONFIRMED_COOKIE_HEADER))
				.andExpect(content().json("""
						{"selectYearhakgi":"2026,1","selectChangeYn":"Y"}
						"""))
				.andRespond(withSuccess("""
						[{"hakbun":"<student-id>","kname":"<student-name>"}]
						""", APPLICATION_JSON));
	}

	private RequestMatcher encryptedLoginTokenPayload() {
		return encryptedLoginTokenPayload(KLAS_PASSWORD);
	}

	private RequestMatcher encryptedLoginTokenPayload(String klasPassword) {
		return request -> {
			String body = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
			JsonNode json = objectMapper.readTree(body);
			String loginToken = json.get("loginToken").asText();

			assertThat(loginToken).doesNotContain(KLAS_ID);
			assertThat(loginToken).doesNotContain(klasPassword);
			assertThat(json.get("redirectUrl").asText()).isEmpty();
			assertThat(json.get("redirectTabUrl").asText()).isEmpty();

			JsonNode payload = objectMapper.readTree(decryptLoginToken(loginToken));
			assertThat(payload.get("loginId").asText()).isEqualTo(KLAS_ID);
			assertThat(payload.get("loginPwd").asText()).isEqualTo(klasPassword);
			assertThat(payload.get("storeIdYn").asText()).isEqualTo("N");
		};
	}

	private int maxPkcs1PayloadBytes() {
		RSAKey rsaKey = (RSAKey) keyPair.getPublic();
		return (rsaKey.getModulus().bitLength() + 7) / 8 - 11;
	}

	private String passwordForPayloadSize(int targetPayloadBytes) throws Exception {
		JsonNode emptyPasswordPayload = objectMapper.createObjectNode()
				.put("loginId", KLAS_ID)
				.put("loginPwd", "")
				.put("storeIdYn", "N");
		int fixedPayloadBytes = objectMapper.writeValueAsBytes(emptyPasswordPayload).length;
		return "a".repeat(targetPayloadBytes - fixedPayloadBytes);
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
