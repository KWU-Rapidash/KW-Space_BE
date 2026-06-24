package com.example.KW_SPACE.user.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.KW_SPACE.auth.exception.AuthErrorResponseWriter;
import com.example.KW_SPACE.auth.jwt.JwtTokenProvider;
import com.example.KW_SPACE.auth.security.CustomUserDetailsService;
import com.example.KW_SPACE.config.AuthCookieConfig;
import com.example.KW_SPACE.config.SecurityConfig;
import com.example.KW_SPACE.user.application.UserNotFoundException;
import com.example.KW_SPACE.user.application.UserService;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
}
