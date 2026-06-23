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
			.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.summary").value("로그인"))
			.andExpect(jsonPath("$.paths['/api/v1/auth/signup'].post.summary").value("회원가입"))
			.andExpect(jsonPath("$.paths['/api/v1/auth/signup'].post.requestBody.content['application/json'].schema.properties.klasId")
				.exists())
			.andExpect(jsonPath("$.paths['/api/v1/auth/signup'].post.requestBody.content['application/json'].schema.properties.klasPassword")
				.exists())
			.andExpect(jsonPath("$.paths['/api/v1/auth/signup'].post.requestBody.content['application/json'].schema.properties.password")
				.exists())
			.andExpect(jsonPath("$.paths['/api/v1/auth/signup'].post.requestBody.content['application/json'].schema.properties.phoneNumber")
				.exists())
			.andExpect(jsonPath("$.paths['/api/v1/auth/password-reset'].post.summary").value("비밀번호 재설정"))
			.andExpect(jsonPath("$.paths['/api/v1/reservations'].post.summary").value("강의실 예약"))
			.andExpect(jsonPath("$.paths['/api/v1/classrooms/{classroomId}/times'].get.summary")
				.value("강의실 예약 가능 시간 조회"))
			.andExpect(jsonPath("$.paths['/api/v1/classrooms'].get.summary").value("특정 날짜/층의 전체 강의실 조회"))
			.andExpect(jsonPath("$.paths['/api/v1/user/reservations'].get.summary").value("사용자별 예약 정보"))
			.andExpect(jsonPath("$.paths['/api/v1/user/reservations'].get.parameters[0].schema.enum[0]").value("RESERVED"))
			.andExpect(jsonPath("$.paths['/api/v1/user/reservations'].get.parameters[0].schema.enum[1]").value("CANCELLED"))
			.andExpect(jsonPath("$.paths['/api/v1/reservations/{reservationId}'].delete.summary").value("예약 취소"))
			.andExpect(jsonPath("$.paths['/api/v1/reservations'].post.responses['200'].content['application/json'].schema.properties.status.enum[0]")
				.value("RESERVED"))
			.andExpect(jsonPath("$.paths['/api/v1/reservations'].post.responses['200'].content['application/json'].schema.properties.status.enum[1]")
				.value("CANCELLED"))
			.andExpect(jsonPath("$.paths['/api/v1/user'].get.summary").value("내 정보 조회"))
			.andExpect(jsonPath("$.paths['/api/v1/user'].delete.summary").value("회원 탈퇴"))
			.andExpect(jsonPath("$.paths['/api/v1/user/password'].patch.summary").value("내 비밀번호 수정"))
			.andExpect(jsonPath("$.paths['/api/v1/user/phone'].patch.summary").value("내 전화번호 수정"))
			.andExpect(jsonPath("$.paths['/api/v1/user'].get.security[0].accessTokenCookie[0]").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/user/']").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/auth/login']").doesNotExist());
	}

	@Test
	void swaggerUiIsPublic() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/html"));
	}
}
