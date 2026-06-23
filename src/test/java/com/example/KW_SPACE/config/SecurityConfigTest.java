package com.example.KW_SPACE.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
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
	void userEndpointRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/user")
						.param("klasId", "2025404000"))
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
				.andExpect(jsonPath("$.username").value("이효원"))
				.andExpect(jsonPath("$.klasId").value("2025404000"))
				.andExpect(jsonPath("$.message").value("내 정보 조회에 성공했습니다."));
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
