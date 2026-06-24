package com.example.KW_SPACE.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.KW_SPACE.auth.exception.AuthErrorResponseWriter;
import com.example.KW_SPACE.auth.jwt.JwtTokenProvider;
import com.example.KW_SPACE.auth.security.CustomUserDetails;
import com.example.KW_SPACE.auth.security.CustomUserDetailsService;
import com.example.KW_SPACE.config.AuthCookieConfig;
import com.example.KW_SPACE.config.SecurityConfig;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.example.KW_SPACE.reservation.dto.ReservationCreateRequest;
import com.example.KW_SPACE.reservation.dto.ReservationCreateResponse;
import com.example.KW_SPACE.reservation.exception.ReservationErrorCode;
import com.example.KW_SPACE.reservation.exception.ReservationException;
import com.example.KW_SPACE.reservation.service.ReservationService;
import com.example.KW_SPACE.user.domain.User;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservationController.class)
@Import({SecurityConfig.class, AuthCookieConfig.class, AuthErrorResponseWriter.class})
class ReservationControllerTest {

	private static final long USER_ID = 1L;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReservationService reservationService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	private CustomUserDetails principal;

	@BeforeEach
	void setUp() {
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		ReflectionTestUtils.setField(user, "id", USER_ID);
		principal = CustomUserDetails.from(user);
	}

	private String requestBody(String classroomId, String date, String startTime, String endTime) {
		return """
				{"classroomId":"%s","date":"%s","startTime":"%s","endTime":"%s"}
				""".formatted(classroomId, date, startTime, endTime);
	}

	@Test
	void createsReservationAndReturnsCreated() throws Exception {
		ReservationCreateResponse response = new ReservationCreateResponse(
				100L, "saebit-101", "101", LocalDate.of(2024, 4, 1), LocalTime.of(9, 0), LocalTime.of(10, 30),
				ReservationStatus.RESERVED);
		when(reservationService.create(eq(USER_ID), any(ReservationCreateRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/v1/reservations").with(user(principal))
						.contentType("application/json")
						.content(requestBody("saebit-101", "2024-04-01", "09:00", "10:30")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reservationId").value(100))
				.andExpect(jsonPath("$.classroomId").value("saebit-101"))
				.andExpect(jsonPath("$.classroomNumber").value("101"))
				.andExpect(jsonPath("$.startTime").value("09:00"))
				.andExpect(jsonPath("$.endTime").value("10:30"))
				.andExpect(jsonPath("$.status").value("RESERVED"));
	}

	@Test
	void returnsBadRequestWhenFieldMissing() throws Exception {
		mockMvc.perform(post("/api/v1/reservations").with(user(principal))
						.contentType("application/json")
						.content("""
								{"date":"2024-04-01","startTime":"09:00","endTime":"10:30"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsConflictWhenReservationOverlaps() throws Exception {
		when(reservationService.create(eq(USER_ID), any(ReservationCreateRequest.class)))
				.thenThrow(new ReservationException(ReservationErrorCode.RESERVATION_CONFLICT));

		mockMvc.perform(post("/api/v1/reservations").with(user(principal))
						.contentType("application/json")
						.content(requestBody("saebit-101", "2024-04-01", "09:00", "10:30")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RESERVATION_CONFLICT"));
	}

	@Test
	void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
		mockMvc.perform(post("/api/v1/reservations")
						.contentType("application/json")
						.content(requestBody("saebit-101", "2024-04-01", "09:00", "10:30")))
				.andExpect(status().isUnauthorized());
	}
}
