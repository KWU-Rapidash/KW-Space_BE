package com.example.KW_SPACE.user.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.KW_SPACE.auth.exception.AuthErrorResponseWriter;
import com.example.KW_SPACE.auth.jwt.JwtTokenProvider;
import com.example.KW_SPACE.auth.security.CustomUserDetails;
import com.example.KW_SPACE.auth.security.CustomUserDetailsService;
import com.example.KW_SPACE.config.AuthCookieConfig;
import com.example.KW_SPACE.config.SecurityConfig;
import com.example.KW_SPACE.user.application.UserErrorCode;
import com.example.KW_SPACE.user.application.UserException;
import com.example.KW_SPACE.user.application.UserNotFoundException;
import com.example.KW_SPACE.user.application.UserService;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, AuthCookieConfig.class, AuthErrorResponseWriter.class})
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@Test
	@WithMockUser
	void getMyInfoReturnsUserInfo() throws Exception {
		given(userService.getMyInfo("2022202015"))
				.willReturn(new UserInfoResponse("홍길동", "2022202015", "010-****-5678", "내 정보 조회에 성공했습니다."));

		mockMvc.perform(get("/api/v1/user")
						.param("klasId", "2022202015"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.name").value("홍길동"))
				.andExpect(jsonPath("$.username").doesNotExist())
				.andExpect(jsonPath("$.klasId").value("2022202015"))
				.andExpect(jsonPath("$.phoneNumber").value("010-****-5678"))
				.andExpect(jsonPath("$.message").value("내 정보 조회에 성공했습니다."));
	}

	@Test
	@WithMockUser
	void getMyInfoReturnsNotFoundWhenUserDoesNotExist() throws Exception {
		given(userService.getMyInfo("2022202015"))
				.willThrow(new UserNotFoundException("2022202015"));

		mockMvc.perform(get("/api/v1/user")
						.param("klasId", "2022202015"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 일치하지 않습니다."));
	}

	@Test
	@WithMockUser
	void blankKlasIdReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/api/v1/user")
						.param("klasId", " "))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updatePasswordReturnsSuccess() throws Exception {
		CustomUserDetails userDetails = authenticatedUserDetails(1L);

		mockMvc.perform(patch("/api/v1/user/password")
						.with(user(userDetails))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": "current-password",
								  "newPassword": "new-password"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.message").value("비밀번호 수정에 성공했습니다."));

		verify(userService).updatePassword(1L, "current-password", "new-password");
	}

	@Test
	void updatePasswordReturnsBadRequestWhenCurrentPasswordMismatches() throws Exception {
		CustomUserDetails userDetails = authenticatedUserDetails(1L);
		willThrow(new UserException(UserErrorCode.USER_CURRENT_PASSWORD_MISMATCH))
				.given(userService)
				.updatePassword(1L, "wrong-password", "new-password");

		mockMvc.perform(patch("/api/v1/user/password")
						.with(user(userDetails))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": "wrong-password",
								  "newPassword": "new-password"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER_CURRENT_PASSWORD_MISMATCH"))
				.andExpect(jsonPath("$.message").value("현재 비밀번호가 일치하지 않습니다."));
	}

	@Test
	void updatePasswordRequiresAuthentication() throws Exception {
		mockMvc.perform(patch("/api/v1/user/password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": "current-password",
								  "newPassword": "new-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));

		verifyNoInteractions(userService);
	}

	@Test
	void updatePasswordReturnsBadRequestWhenRequiredFieldIsMissing() throws Exception {
		mockMvc.perform(patch("/api/v1/user/password")
						.with(user(authenticatedUserDetails(1L)))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": " ",
								  "newPassword": "new-password"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED_FIELD_MISSING"));

		verifyNoInteractions(userService);
	}

	@Test
	void updatePasswordReturnsUnprocessableContentWhenNewPasswordIsTooShort() throws Exception {
		mockMvc.perform(patch("/api/v1/user/password")
						.with(user(authenticatedUserDetails(1L)))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": "current-password",
								  "newPassword": "short"
								}
								"""))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("AUTH_PASSWORD_POLICY_VIOLATION"));

		verifyNoInteractions(userService);
	}

	private CustomUserDetails authenticatedUserDetails(Long userId) {
		CustomUserDetails userDetails = org.mockito.Mockito.mock(CustomUserDetails.class);
		given(userDetails.getId()).willReturn(userId);
		given(userDetails.getUsername()).willReturn(String.valueOf(userId));
		given(userDetails.getPassword()).willReturn("encoded-password");
		doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();

		return userDetails;
	}
}
