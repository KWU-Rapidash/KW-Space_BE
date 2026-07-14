package com.example.KW_SPACE.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.KW_SPACE.auth.jwt.JwtProperties;
import com.example.KW_SPACE.auth.jwt.JwtTokenProvider;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

	private static final String KLAS_ID = "2025404000";
	private static final String KLAS_PASSWORD = "valid-klas-password";
	private static final String SERVICE_PASSWORD = "service-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private JwtProperties jwtProperties;

	@Autowired
	private EntityManager entityManager;

	@AfterEach
	void tearDown() {
		entityManager.clear();
		userRepository.deleteAll();
	}

	@Test
	void signupStoresEncodedPasswordHashWithoutPersistingKlasPassword() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
							.content(signupJson(KLAS_PASSWORD, SERVICE_PASSWORD)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.message").value("회원가입에 성공했습니다."))
					.andExpect(jsonPath("$.username").doesNotExist())
					.andExpect(jsonPath("$.klasId").doesNotExist());

		entityManager.clear();
		User savedUser = findUser(KLAS_ID);

		assertThat(savedUser.getName()).isEqualTo("테스트사용자");
		assertThat(passwordEncoder.matches(SERVICE_PASSWORD, savedUser.getPasswordHash())).isTrue();
		assertThat(passwordEncoder.matches(KLAS_PASSWORD, savedUser.getPasswordHash())).isFalse();
		assertThat(savedUser.getPasswordHash()).isNotEqualTo(SERVICE_PASSWORD);
		assertThat(savedUser.getPasswordHash()).isNotEqualTo(KLAS_PASSWORD);
	}

	@Test
	void signupRejectsInvalidKlasCredentialsWithoutCreatingUser() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupJson("wrong-klas-password", SERVICE_PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_KLAS_CREDENTIALS"));

		assertThat(userRepository.findByKlasId(KLAS_ID)).isEmpty();
	}

	@Test
	void signupRejectsDuplicatedKlasId() throws Exception {
		signupDefaultUser();

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupJson(KLAS_PASSWORD, "another-service-password")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("AUTH_DUPLICATED_KLAS_ID"));

		assertThat(userRepository.count()).isEqualTo(1);
	}

	@Test
	void loginIssuesAccessTokenCookieAndAuthenticatesProtectedRequest() throws Exception {
		signupDefaultUser();

		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson(KLAS_ID, SERVICE_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.allOf(
						Matchers.containsString("accessToken="),
						Matchers.containsString("HttpOnly"),
						Matchers.containsString("SameSite=Lax")
				)))
				.andReturn();

		mockMvc.perform(get("/api/v1/user")
						.cookie(accessTokenCookie(loginResult))
						.param("klasId", KLAS_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("테스트사용자"))
				.andExpect(jsonPath("$.klasId").value(KLAS_ID));
	}

	@Test
	void loginRejectsWrongPassword() throws Exception {
		signupDefaultUser();

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson(KLAS_ID, "wrong-service-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
	}

	@Test
	void loginRejectsUnknownUser() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson(KLAS_ID, SERVICE_PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
	}

	@Test
	void passwordResetUpdatesPasswordHashAndTokenVersion() throws Exception {
		signupDefaultUser();
		String oldPasswordHash = findUser(KLAS_ID).getPasswordHash();

		mockMvc.perform(post("/api/v1/auth/password-reset")
						.contentType(MediaType.APPLICATION_JSON)
						.content(passwordResetJson(KLAS_PASSWORD, "new-service-password")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		entityManager.clear();
		User updatedUser = findUser(KLAS_ID);
		assertThat(updatedUser.getPasswordHash()).isNotEqualTo(oldPasswordHash);
		assertThat(passwordEncoder.matches("new-service-password", updatedUser.getPasswordHash())).isTrue();
		assertThat(passwordEncoder.matches(KLAS_PASSWORD, updatedUser.getPasswordHash())).isFalse();
		assertThat(updatedUser.getTokenVersion()).isEqualTo(1);
	}

	@Test
	void passwordResetRejectsInvalidKlasCredentialsWithoutChangingUser() throws Exception {
		signupDefaultUser();
		User savedUser = findUser(KLAS_ID);
		String oldPasswordHash = savedUser.getPasswordHash();

		mockMvc.perform(post("/api/v1/auth/password-reset")
						.contentType(MediaType.APPLICATION_JSON)
						.content(passwordResetJson("wrong-klas-password", "new-service-password")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_KLAS_CREDENTIALS"));

		entityManager.clear();
		User unchangedUser = findUser(KLAS_ID);
		assertThat(unchangedUser.getPasswordHash()).isEqualTo(oldPasswordHash);
		assertThat(unchangedUser.getTokenVersion()).isZero();
	}

	@Test
	void expiredAccessTokenCookieIsRejected() throws Exception {
		User user = saveUser("encoded-password");
		Instant expiresAt = Instant.now().minus(Duration.ofMinutes(1));
		String expiredToken = createSignedToken(user, expiresAt.minus(Duration.ofHours(1)), expiresAt);

		mockMvc.perform(get("/api/v1/user")
						.cookie(new Cookie("accessToken", expiredToken))
						.param("klasId", KLAS_ID))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_EXPIRED_TOKEN"));
	}

	@Test
	void tamperedAccessTokenCookieIsRejected() throws Exception {
		User user = saveUser("encoded-password");
		String tamperedToken = jwtTokenProvider.createAccessToken(user) + "tampered";

		mockMvc.perform(get("/api/v1/user")
						.cookie(new Cookie("accessToken", tamperedToken))
						.param("klasId", KLAS_ID))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void tokenVersionMismatchRejectsPreviouslyIssuedToken() throws Exception {
		User user = saveUser("encoded-password");
		String oldAccessToken = jwtTokenProvider.createAccessToken(user);
		user.resetPassword("new-encoded-password");
		userRepository.saveAndFlush(user);
		entityManager.clear();

		mockMvc.perform(get("/api/v1/user")
						.cookie(new Cookie("accessToken", oldAccessToken))
						.param("klasId", KLAS_ID))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
	}

	private void signupDefaultUser() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupJson(KLAS_PASSWORD, SERVICE_PASSWORD)))
				.andExpect(status().isCreated());
		entityManager.clear();
	}

	private User saveUser(String passwordHash) {
		return userRepository.saveAndFlush(User.create(KLAS_ID, "테스트사용자", null, passwordHash));
	}

	private User findUser(String klasId) {
		return userRepository.findByKlasId(klasId).orElseThrow();
	}

	private Cookie accessTokenCookie(MvcResult result) {
		String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
		assertThat(setCookie).isNotNull();
		String cookieValue = setCookie.split(";", 2)[0];
		String prefix = "accessToken=";
		assertThat(cookieValue).startsWith(prefix);

		return new Cookie("accessToken", cookieValue.substring(prefix.length()));
	}

	private String createSignedToken(User user, Instant issuedAt, Instant expiresAt) {
		SecretKey secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));

		return Jwts.builder()
				.subject(user.getId().toString())
				.claim("role", user.getRole().name())
				.claim("tokenVersion", user.getTokenVersion())
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiresAt))
				.signWith(secretKey)
				.compact();
	}

	private String signupJson(String klasPassword, String servicePassword) {
		return """
				{
				  "name": "요청이름",
				  "klasId": "%s",
				  "klasPassword": "%s",
				  "password": "%s"
				}
				""".formatted(KLAS_ID, klasPassword, servicePassword);
	}

	private String loginJson(String klasId, String password) {
		return """
				{
				  "klasId": "%s",
				  "password": "%s"
				}
				""".formatted(klasId, password);
	}

	private String passwordResetJson(String klasPassword, String newPassword) {
		return """
				{
				  "klasId": "%s",
				  "klasPassword": "%s",
				  "newPassword": "%s"
				}
				""".formatted(KLAS_ID, klasPassword, newPassword);
	}
}
