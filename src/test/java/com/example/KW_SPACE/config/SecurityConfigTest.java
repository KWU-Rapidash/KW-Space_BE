package com.example.KW_SPACE.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.KW_SPACE.auth.jwt.JwtTokenProvider;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@AfterEach
	void tearDown() {
		userRepository.deleteAll();
	}

	@Test
	void healthEndpointIsPublic() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void healthHeadEndpointIsPublic() throws Exception {
		mockMvc.perform(head("/api/health"))
				.andExpect(status().isOk());
	}

	@Test
	void apiPreflightEndpointIsPublic() throws Exception {
		mockMvc.perform(options("/api/v1/user"))
				.andExpect(status().isOk());
	}

	@Test
	void classroomListEndpointIsPublic() throws Exception {
		mockMvc.perform(get("/api/v1/classrooms")
						.param("floor", "1")
						.param("date", "2024-04-01"))
				.andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
	}

	@Test
	void jsonLoginWithoutCsrfTokenReachesAuthController() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "klasId": "2025404000",
								  "password": "service-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
	}

	@Test
	void klasVerifyWithoutCsrfTokenIsPublic() throws Exception {
		mockMvc.perform(post("/api/v1/auth/klas/verify")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "klasId": "2025404000",
								  "klasPassword": "valid-klas-password"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.klasId").value("2025404000"))
				.andExpect(jsonPath("$.name").value("테스트사용자"))
				.andExpect(jsonPath("$.message").value("KLAS 인증에 성공했습니다."));
	}

	@Test
	void protectedPostWithoutCsrfTokenStillRequiresJwtAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/user"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void reservationCreationEndpointRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/reservations"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void userReservationsEndpointRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/user/reservations"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void reservationCancelEndpointRequiresAuthentication() throws Exception {
		mockMvc.perform(delete("/api/v1/reservations/1"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void userEndpointRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/user")
						.param("klasId", "2025404000"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void userPhoneUpdateEndpointRequiresAuthentication() throws Exception {
		mockMvc.perform(patch("/api/v1/user/phone")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "phoneNumber": "010-1234-5678"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
	}

	@Test
	void validAccessTokenCookieAuthenticatesProtectedRequest() throws Exception {
		User user = userRepository.saveAndFlush(User.create("2025404000", "이효원", null, "encoded-password"));
		String accessToken = jwtTokenProvider.createAccessToken(user);

		mockMvc.perform(get("/api/v1/user")
						.cookie(new Cookie("accessToken", accessToken))
						.param("klasId", "2025404000"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.name").value("이효원"))
				.andExpect(jsonPath("$.username").doesNotExist())
				.andExpect(jsonPath("$.klasId").value("2025404000"))
				.andExpect(jsonPath("$.message").value("내 정보 조회에 성공했습니다."));
	}

	@Test
	void validAccessTokenCookieAllowsPhoneNumberUpdate() throws Exception {
		User user = userRepository.saveAndFlush(User.create("2025404000", "이효원", null, "encoded-password"));
		String accessToken = jwtTokenProvider.createAccessToken(user);

		mockMvc.perform(patch("/api/v1/user/phone")
						.cookie(new Cookie("accessToken", accessToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "phoneNumber": "010-1234-5678"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.phoneNumber").value("010-1234-5678"))
				.andExpect(jsonPath("$.message").value("전화번호 수정에 성공했습니다."));
	}

	@Test
	void invalidAccessTokenCookieReturnsStandardUnauthorizedResponse() throws Exception {
		mockMvc.perform(get("/api/v1/user")
						.cookie(new Cookie("accessToken", "invalid-token"))
						.param("klasId", "2025404000"))
				.andExpect(status().isUnauthorized())
				.andExpect(cookie().doesNotExist("JSESSIONID"))
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));
	}
}
