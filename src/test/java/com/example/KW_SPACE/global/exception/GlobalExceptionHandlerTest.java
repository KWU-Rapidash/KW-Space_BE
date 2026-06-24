package com.example.KW_SPACE.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.auth.klas.KlasAuthServerUnavailableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders.standaloneSetup(new TestAuthController())
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.build();
	}

	@Test
	void returnsStandardAuthErrorResponse() throws Exception {
		mockMvc.perform(post("/test/auth/invalid-credentials"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 일치하지 않습니다."));
	}

	@Test
	void doesNotExposeAccountExistenceOnInvalidCredentials() throws Exception {
		mockMvc.perform(post("/test/auth/invalid-credentials"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 일치하지 않습니다."))
				.andExpect(jsonPath("$.message").value(Matchers.not(Matchers.containsString("학번"))))
				.andExpect(jsonPath("$.message").value(Matchers.not(Matchers.containsString("존재"))));
	}

	@Test
	void mapsKlasServerUnavailableTo503() throws Exception {
		mockMvc.perform(post("/test/auth/klas-unavailable"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("AUTH_KLAS_SERVER_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("KLAS 서버를 사용할 수 없습니다."));
	}

	@Test
	void mapsMissingRequiredFieldsTo400() throws Exception {
		mockMvc.perform(post("/test/auth/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"password\":\"password123\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED_FIELD_MISSING"))
				.andExpect(jsonPath("$.message").value("필수 입력값이 누락되었습니다."));
	}

	@Test
	void mapsPasswordPolicyViolationTo422() throws Exception {
		mockMvc.perform(post("/test/auth/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"klasId\":\"2025404000\",\"password\":\"short\"}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("AUTH_PASSWORD_POLICY_VIOLATION"))
				.andExpect(jsonPath("$.message").value("비밀번호 정책을 만족하지 않습니다."));
	}

	@Test
	void prioritizesMissingRequiredFieldsWhenValidationErrorsAreMixed() throws Exception {
		mockMvc.perform(post("/test/auth/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"password\":\"short\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED_FIELD_MISSING"))
				.andExpect(jsonPath("$.message").value("필수 입력값이 누락되었습니다."));
	}

	@RestController
	private static class TestAuthController {

		@PostMapping("/test/auth/invalid-credentials")
		void invalidCredentials() {
			throw new AuthException(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
		}

		@PostMapping("/test/auth/klas-unavailable")
		void klasUnavailable() {
			throw new KlasAuthServerUnavailableException("KLAS authentication server is unavailable");
		}

		@PostMapping("/test/auth/validate")
		void validate(@Valid @RequestBody TestAuthRequest request) {
		}
	}

	private record TestAuthRequest(
			@NotBlank String klasId,
			@NotBlank @Size(min = 8) String password
	) {
	}
}
