package com.example.KW_SPACE.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void openApiDocsArePublicAndExposeServiceInfo() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("application/json"))
			.andExpect(jsonPath("$.info.title").value("KW-Space API"))
			.andExpect(jsonPath("$.info.description").value("새빛관 대여 시스템 Backend API"))
			.andExpect(jsonPath("$.info.version").value("0.0.1-SNAPSHOT"))
			.andExpect(jsonPath("$.components.securitySchemes.accessTokenCookie.in").value("cookie"))
			.andExpect(jsonPath("$.components.securitySchemes.accessTokenCookie.name").value("accessToken"))
			.andExpect(jsonPath("$.paths['/api/health'].get.summary").value("헬스체크"))
			.andExpect(jsonPath("$.paths['/api/auth/login'].post.summary").value("로그인"))
			.andExpect(jsonPath("$.paths['/api/auth/signup'].post.summary").value("회원가입"))
			.andExpect(jsonPath("$.paths['/api/auth/password-reset'].post.summary").value("비밀번호 재설정"))
			.andExpect(jsonPath("$.paths['/api/auth/login'].post.security").doesNotExist());
	}

	@Test
	void swaggerUiIsPublic() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/html"));
	}
}
