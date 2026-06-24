package com.example.KW_SPACE.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.KW_SPACE.auth.klas.KlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthServerUnavailableException;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthKlasExceptionFlowIntegrationTest {

	private static final String KLAS_ID = "2025404000";
	private static final String KLAS_PASSWORD = "valid-klas-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private KlasAuthClient klasAuthClient;

	@AfterEach
	void tearDown() {
		entityManager.clear();
		userRepository.deleteAll();
	}

	@Test
	void signupReturnsServiceUnavailableWhenKlasClientThrowsException() throws Exception {
		given(klasAuthClient.verify(KLAS_ID, KLAS_PASSWORD))
				.willThrow(new KlasAuthServerUnavailableException("KLAS request timed out"));

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupJson()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("AUTH_KLAS_SERVER_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("KLAS 서버를 사용할 수 없습니다."));

		assertThat(userRepository.findByKlasId(KLAS_ID)).isEmpty();
	}

	@Test
	void passwordResetReturnsServiceUnavailableWithoutChangingUserWhenKlasClientThrowsException() throws Exception {
		User user = userRepository.saveAndFlush(User.create(KLAS_ID, "테스트사용자", null, "old-password-hash"));
		given(klasAuthClient.verify(KLAS_ID, KLAS_PASSWORD))
				.willThrow(new KlasAuthServerUnavailableException("KLAS network error"));

		mockMvc.perform(post("/api/v1/auth/password-reset")
						.contentType(MediaType.APPLICATION_JSON)
						.content(passwordResetJson()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("AUTH_KLAS_SERVER_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("KLAS 서버를 사용할 수 없습니다."));

		entityManager.clear();
		User unchangedUser = userRepository.findById(user.getId()).orElseThrow();
		assertThat(unchangedUser.getPasswordHash()).isEqualTo("old-password-hash");
		assertThat(unchangedUser.getTokenVersion()).isZero();
	}

	private String signupJson() {
		return """
				{
				  "name": "요청이름",
				  "klasId": "%s",
				  "klasPassword": "%s",
				  "password": "service-password"
				}
				""".formatted(KLAS_ID, KLAS_PASSWORD);
	}

	private String passwordResetJson() {
		return """
				{
				  "klasId": "%s",
				  "klasPassword": "%s",
				  "newPassword": "new-service-password"
				}
				""".formatted(KLAS_ID, KLAS_PASSWORD);
	}
}
