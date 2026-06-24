package com.example.KW_SPACE.auth.klas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class RealKlasAuthClient implements KlasAuthClient {

	private static final MediaType JSON_UTF8 = MediaType.parseMediaType("application/json;charset=utf-8");
	private static final String LOGIN_SECURITY_PATH = "/usr/cmn/login/LoginSecurity.do";
	private static final String LOGIN_CONFIRM_PATH = "/usr/cmn/login/LoginConfirm.do";
	private static final String STUDENT_INFO_PATH = "/std/cps/inqire/ToeicInfoStd.do";
	private static final String STUDENT_INFO_REFERER = "https://klas.kw.ac.kr/std/cps/inqire/StandStdPage.do";

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final KlasAuthProperties properties;
	private final Clock clock;

	public RealKlasAuthClient(RestClient restClient, ObjectMapper objectMapper, KlasAuthProperties properties,
			Clock clock) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public KlasAuthResult verify(String klasId, String klasPassword) {
		try {
			LoginSecurity loginSecurity = requestLoginSecurity();
			String loginToken = createLoginToken(klasId, klasPassword, loginSecurity.publicKey());
			if (!confirmLogin(loginToken, loginSecurity.cookieHeader())) {
				return KlasAuthResult.failure();
			}

			return requestStudentInfo(klasId, loginSecurity.cookieHeader());
		} catch (KlasAuthServerUnavailableException exception) {
			throw exception;
		} catch (GeneralSecurityException | JsonProcessingException | IllegalArgumentException
				| RestClientException exception) {
			throw serverUnavailable(exception);
		}
	}

	private LoginSecurity requestLoginSecurity() {
		ResponseEntity<LoginSecurityResponse> response = restClient.post()
				.uri(LOGIN_SECURITY_PATH)
				.contentType(JSON_UTF8)
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL)
				.body(Map.of())
				.retrieve()
				.toEntity(LoginSecurityResponse.class);

		LoginSecurityResponse body = response.getBody();
		if (body == null || !StringUtils.hasText(body.publicKey())) {
			throw serverUnavailable();
		}

		String cookieHeader = toCookieHeader(response.getHeaders().get(HttpHeaders.SET_COOKIE));
		if (!StringUtils.hasText(cookieHeader)) {
			throw serverUnavailable();
		}

		return new LoginSecurity(body.publicKey(), cookieHeader);
	}

	private String createLoginToken(String klasId, String klasPassword, String publicKey)
			throws GeneralSecurityException, JsonProcessingException {
		String payload = objectMapper.writeValueAsString(new LoginTokenPayload(klasId, klasPassword, "N"));
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, parsePublicKey(publicKey));
		byte[] encrypted = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(encrypted);
	}

	private PublicKey parsePublicKey(String publicKey) throws GeneralSecurityException {
		String normalized = publicKey
				.replace("-----BEGIN PUBLIC KEY-----", "")
				.replace("-----END PUBLIC KEY-----", "")
				.replaceAll("\\s", "");
		byte[] keyBytes = Base64.getDecoder().decode(normalized);
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
	}

	private boolean confirmLogin(String loginToken, String cookieHeader) {
		LoginConfirmResponse response = restClient.post()
				.uri(LOGIN_CONFIRM_PATH)
				.contentType(JSON_UTF8)
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL)
				.header(HttpHeaders.COOKIE, cookieHeader)
				.body(new LoginConfirmRequest(loginToken, "", ""))
				.retrieve()
				.body(LoginConfirmResponse.class);

		if (response == null) {
			throw serverUnavailable();
		}
		if (response.errorCount() != null && response.errorCount() > 0) {
			return false;
		}
		if (!Boolean.FALSE.equals(response.loginRequired())
				|| !Objects.equals(response.errorCount(), 0)
				|| response.response() == null
				|| !StringUtils.hasText(response.response().userId())) {
			throw serverUnavailable();
		}

		return true;
	}

	private KlasAuthResult requestStudentInfo(String requestedKlasId, String cookieHeader) {
		StudentInfoResponse[] response = restClient.post()
				.uri(STUDENT_INFO_PATH)
				.contentType(JSON_UTF8)
				.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL)
				.header(HttpHeaders.ORIGIN, properties.baseUrl().toString())
				.header(HttpHeaders.REFERER, STUDENT_INFO_REFERER)
				.header(HttpHeaders.COOKIE, cookieHeader)
				.body(new StudentInfoRequest(properties.resolveSelectYearhakgi(clock), "Y"))
				.retrieve()
				.body(StudentInfoResponse[].class);

		if (response == null || response.length == 0) {
			throw serverUnavailable();
		}

		StudentInfoResponse studentInfo = response[0];
		if (!StringUtils.hasText(studentInfo.hakbun())
				|| !StringUtils.hasText(studentInfo.kname())
				|| !Objects.equals(requestedKlasId, studentInfo.hakbun())) {
			throw serverUnavailable();
		}

		return KlasAuthResult.success(studentInfo.hakbun(), studentInfo.kname());
	}

	private String toCookieHeader(List<String> setCookieHeaders) {
		if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
			return "";
		}

		return setCookieHeaders.stream()
				.map(RealKlasAuthClient::toCookiePair)
				.filter(StringUtils::hasText)
				.collect(Collectors.joining("; "));
	}

	private static String toCookiePair(String setCookieHeader) {
		if (!StringUtils.hasText(setCookieHeader)) {
			return "";
		}

		return setCookieHeader.split(";", 2)[0].trim();
	}

	private KlasAuthServerUnavailableException serverUnavailable() {
		return new KlasAuthServerUnavailableException("KLAS authentication server is unavailable");
	}

	private KlasAuthServerUnavailableException serverUnavailable(Throwable cause) {
		return new KlasAuthServerUnavailableException("KLAS authentication server is unavailable", cause);
	}

	private record LoginSecurity(String publicKey, String cookieHeader) {
	}

	private record LoginSecurityResponse(String publicKey) {
	}

	private record LoginTokenPayload(String loginId, String loginPwd, String storeIdYn) {
	}

	private record LoginConfirmRequest(String loginToken, String redirectUrl, String redirectTabUrl) {
	}

	private record LoginConfirmResponse(Boolean loginRequired, Integer errorCount, LoginConfirmUser response) {
	}

	private record LoginConfirmUser(String userId) {
	}

	private record StudentInfoRequest(String selectYearhakgi, String selectChangeYn) {
	}

	private record StudentInfoResponse(String hakbun, String kname) {
	}
}
