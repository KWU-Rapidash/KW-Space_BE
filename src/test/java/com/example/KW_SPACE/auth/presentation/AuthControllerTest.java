package com.example.KW_SPACE.auth.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.KW_SPACE.auth.application.AuthService;
import com.example.KW_SPACE.auth.application.LoginResult;
import com.example.KW_SPACE.auth.cookie.AuthCookieService;
import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthErrorResponseWriter;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.auth.jwt.JwtTokenProvider;
import com.example.KW_SPACE.auth.presentation.dto.LoginRequest;
import com.example.KW_SPACE.auth.presentation.dto.LoginResponse;
import com.example.KW_SPACE.auth.presentation.dto.SignupRequest;
import com.example.KW_SPACE.auth.presentation.dto.SignupResponse;
import com.example.KW_SPACE.auth.security.CustomUserDetailsService;
import com.example.KW_SPACE.config.AuthCookieConfig;
import com.example.KW_SPACE.config.SecurityConfig;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthCookieConfig.class, AuthCookieService.class, AuthErrorResponseWriter.class})
@TestPropertySource(properties = {
		"kw-space.auth.cookie.secure=false"
})
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@Test
	void signupReturnsCreatedResponse() throws Exception {
		given(authService.signup(any(SignupRequest.class)))
				.willReturn(new SignupResponse("이효원", "2025404000", "회원가입에 성공했습니다."));

		mockMvc.perform(post("/api/v1/auth/signup")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "이효원",
								  "klasId": "2025404000",
								  "klasPassword": "valid-klas-password",
								  "password": "service-password"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.username").value("이효원"))
				.andExpect(jsonPath("$.klasId").value("2025404000"))
				.andExpect(jsonPath("$.message").value("회원가입에 성공했습니다."));
	}

	@Test
	void signupReturnsConflictWhenKlasIdIsDuplicated() throws Exception {
		given(authService.signup(any(SignupRequest.class)))
				.willThrow(new AuthException(AuthErrorCode.AUTH_DUPLICATED_KLAS_ID));

		mockMvc.perform(post("/api/v1/auth/signup")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "이효원",
								  "klasId": "2025404000",
								  "klasPassword": "valid-klas-password",
								  "password": "service-password"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("AUTH_DUPLICATED_KLAS_ID"))
				.andExpect(jsonPath("$.message").value("이미 가입된 학번입니다."));
	}

	@Test
	void signupReturnsUnauthorizedWhenKlasCredentialsAreInvalid() throws Exception {
		given(authService.signup(any(SignupRequest.class)))
				.willThrow(new AuthException(AuthErrorCode.AUTH_INVALID_KLAS_CREDENTIALS));

		mockMvc.perform(post("/api/v1/auth/signup")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "이효원",
								  "klasId": "2025404000",
								  "klasPassword": "wrong-klas-password",
								  "password": "service-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_KLAS_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("KLAS 인증 정보가 일치하지 않습니다."));
	}

	@Test
	void signupReturnsBadRequestWhenRequiredFieldIsMissing() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "klasId": "2025404000",
								  "klasPassword": "valid-klas-password",
								  "password": "service-password"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED_FIELD_MISSING"));
	}

	@Test
	void signupReturnsUnprocessableContentWhenPasswordIsTooShort() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "이효원",
								  "klasId": "2025404000",
								  "klasPassword": "valid-klas-password",
								  "password": "short"
								}
								"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("AUTH_PASSWORD_POLICY_VIOLATION"));
	}

	@Test
	void loginReturnsAccessTokenCookie() throws Exception {
		given(authService.login(any(LoginRequest.class)))
				.willReturn(new LoginResult("access-token", new LoginResponse(true, "로그인에 성공했습니다.")));

		mockMvc.perform(post("/api/v1/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "klasId": "2025404000",
								  "password": "service-password"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
				.andExpect(header().string("Set-Cookie", Matchers.allOf(
						Matchers.containsString("accessToken=access-token"),
						Matchers.containsString("HttpOnly"),
						Matchers.not(Matchers.containsString("Secure")),
						Matchers.containsString("SameSite=Lax"),
						Matchers.containsString("Path=/"),
						Matchers.containsString("Max-Age=3600")
				)));
	}

	@Test
	void loginReturnsUnauthorizedWhenCredentialsAreInvalid() throws Exception {
		given(authService.login(any(LoginRequest.class)))
				.willThrow(new AuthException(AuthErrorCode.AUTH_INVALID_CREDENTIALS));

		mockMvc.perform(post("/api/v1/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "klasId": "2025404000",
								  "password": "wrong-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 일치하지 않습니다."));
	}

	@Test
	void loginReturnsBadRequestWhenRequiredFieldIsMissing() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "password": "service-password"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED_FIELD_MISSING"));
	}

	@Test
	void logoutDeletesAccessTokenCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("로그아웃에 성공했습니다."))
				.andExpect(header().string("Set-Cookie", Matchers.allOf(
						Matchers.containsString("accessToken="),
						Matchers.containsString("HttpOnly"),
						Matchers.not(Matchers.containsString("Secure")),
						Matchers.containsString("SameSite=Lax"),
						Matchers.containsString("Path=/"),
						Matchers.containsString("Max-Age=0")
				)));

		verifyNoInteractions(authService);
	}

	@Test
	void logoutIgnoresInvalidAccessTokenCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout")
						.cookie(new Cookie("accessToken", "invalid-token")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(header().string("Set-Cookie", Matchers.containsString("Max-Age=0")));

		verifyNoInteractions(authService, jwtTokenProvider, customUserDetailsService);
	}
}
