package com.example.KW_SPACE.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.KW_SPACE.auth.exception.AuthErrorResponseWriter;
import com.example.KW_SPACE.auth.jwt.JwtTokenProvider;
import com.example.KW_SPACE.auth.security.CustomUserDetailsService;
import com.example.KW_SPACE.config.AuthCookieConfig;
import com.example.KW_SPACE.config.SecurityConfig;
import com.example.KW_SPACE.reservation.dto.ClassroomAvailabilityResponse;
import com.example.KW_SPACE.reservation.dto.TimeSlotResponse;
import com.example.KW_SPACE.reservation.service.ClassroomAvailabilityService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClassroomAvailabilityController.class)
@Import({SecurityConfig.class, AuthCookieConfig.class, AuthErrorResponseWriter.class})
@WithMockUser
class ClassroomAvailabilityControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ClassroomAvailabilityService classroomAvailabilityService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@Test
	void returnsClassroomAvailabilityAsJson() throws Exception {
		LocalDate date = LocalDate.parse("2024-04-01");
		when(classroomAvailabilityService.findAvailability(eq(2), eq(date))).thenReturn(List.of(
				new ClassroomAvailabilityResponse("saebit-201", 2, "201", true, List.of(
						new TimeSlotResponse("09:00~10:30", true),
						new TimeSlotResponse("10:30~12:00", false)))));

		mockMvc.perform(get("/api/v1/classrooms").param("floor", "2").param("date", "2024-04-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].classroomId").value("saebit-201"))
				.andExpect(jsonPath("$[0].classroomNumber").value("201"))
				.andExpect(jsonPath("$[0].available").value(true))
				.andExpect(jsonPath("$[0].times[0].time").value("09:00~10:30"))
				.andExpect(jsonPath("$[0].times[0].available").value(true))
				.andExpect(jsonPath("$[0].times[1].available").value(false));

		verify(classroomAvailabilityService).findAvailability(2, date);
	}

	@Test
	void returnsOkWhenFloorIsAtMaxBoundary() throws Exception {
		when(classroomAvailabilityService.findAvailability(eq(2), any(LocalDate.class))).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/classrooms").param("floor", "2").param("date", "2024-04-01"))
				.andExpect(status().isOk());
	}

	@Test
	void returnsBadRequestWhenFloorIsNotNumber() throws Exception {
		mockMvc.perform(get("/api/v1/classrooms").param("floor", "abc").param("date", "2024-04-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsBadRequestWhenFloorIsNotPositive() throws Exception {
		mockMvc.perform(get("/api/v1/classrooms").param("floor", "0").param("date", "2024-04-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsBadRequestWhenFloorExceedsMax() throws Exception {
		mockMvc.perform(get("/api/v1/classrooms").param("floor", "3").param("date", "2024-04-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsBadRequestWhenFloorIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/classrooms").param("date", "2024-04-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsBadRequestWhenDateIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/classrooms").param("floor", "2"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsBadRequestWhenDateIsInvalid() throws Exception {
		mockMvc.perform(get("/api/v1/classrooms").param("floor", "2").param("date", "not-a-date"))
				.andExpect(status().isBadRequest());
	}
}
