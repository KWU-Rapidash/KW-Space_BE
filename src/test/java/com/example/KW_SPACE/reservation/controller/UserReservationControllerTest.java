package com.example.KW_SPACE.reservation.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.KW_SPACE.auth.exception.AuthErrorResponseWriter;
import com.example.KW_SPACE.auth.jwt.JwtTokenProvider;
import com.example.KW_SPACE.auth.security.CustomUserDetails;
import com.example.KW_SPACE.auth.security.CustomUserDetailsService;
import com.example.KW_SPACE.config.AuthCookieConfig;
import com.example.KW_SPACE.config.SecurityConfig;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.example.KW_SPACE.reservation.dto.UserReservationResponse;
import com.example.KW_SPACE.reservation.service.ReservationService;
import com.example.KW_SPACE.user.domain.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserReservationController.class)
@Import({SecurityConfig.class, AuthCookieConfig.class, AuthErrorResponseWriter.class})
class UserReservationControllerTest {

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

	@Test
	void returnsMyReservations() throws Exception {
		UserReservationResponse response = new UserReservationResponse(
				100L, LocalDate.of(2024, 4, 1), "saebit-101", "이효원",
				LocalTime.of(9, 0), LocalTime.of(10, 30), ReservationStatus.RESERVED);
		when(reservationService.getUserReservations(eq(USER_ID), eq(null))).thenReturn(List.of(response));

		mockMvc.perform(get("/api/v1/user/reservations").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].reservationId").value(100))
				.andExpect(jsonPath("$[0].date").value("2024-04-01"))
				.andExpect(jsonPath("$[0].classroom").value("saebit-101"))
				.andExpect(jsonPath("$[0].reserverName").value("이효원"))
				.andExpect(jsonPath("$[0].startTime").value("09:00"))
				.andExpect(jsonPath("$[0].endTime").value("10:30"))
				.andExpect(jsonPath("$[0].status").value("RESERVED"));
	}

	@Test
	void filtersByStatus() throws Exception {
		when(reservationService.getUserReservations(eq(USER_ID), eq(ReservationStatus.CANCELED)))
				.thenReturn(List.of());

		mockMvc.perform(get("/api/v1/user/reservations").param("status", "CANCELED").with(user(principal)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	void returnsBadRequestWhenStatusInvalid() throws Exception {
		mockMvc.perform(get("/api/v1/user/reservations").param("status", "INVALID").with(user(principal)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
		mockMvc.perform(get("/api/v1/user/reservations"))
				.andExpect(status().isUnauthorized());
	}
}
