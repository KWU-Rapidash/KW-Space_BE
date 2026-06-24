package com.example.KW_SPACE.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.KW_SPACE.classroom.exception.ClassroomNotFoundException;
import com.example.KW_SPACE.common.exception.GlobalExceptionHandler;
import com.example.KW_SPACE.config.SecurityConfig;
import com.example.KW_SPACE.reservation.dto.ClassroomAvailabilityResponse;
import com.example.KW_SPACE.reservation.dto.TimeSlotResponse;
import com.example.KW_SPACE.reservation.service.ClassroomAvailabilityService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClassroomAvailabilityController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@WithMockUser
class ClassroomAvailabilityControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ClassroomAvailabilityService classroomAvailabilityService;

	@Test
	void returnsClassroomAvailabilityAsJson() throws Exception {
		when(classroomAvailabilityService.findAvailability(eq(4), any(LocalDate.class))).thenReturn(List.of(
				new ClassroomAvailabilityResponse(1L, 4, "401", true),
				new ClassroomAvailabilityResponse(2L, 4, "402", false)));

		mockMvc.perform(get("/api/v1/classrooms").param("floor", "4").param("date", "2024-04-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].roomNumber").value("401"))
				.andExpect(jsonPath("$[0].available").value(true))
				.andExpect(jsonPath("$[1].available").value(false));
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
		mockMvc.perform(get("/api/v1/classrooms").param("floor", "10").param("date", "2024-04-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsBadRequestWhenFloorIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/classrooms").param("date", "2024-04-01"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsBadRequestWhenDateIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/classrooms").param("floor", "4"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsBadRequestWhenDateIsInvalid() throws Exception {
		mockMvc.perform(get("/api/v1/classrooms").param("floor", "4").param("date", "not-a-date"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returnsClassroomTimesAsJson() throws Exception {
		when(classroomAvailabilityService.findTimes(eq(1L), any(LocalDate.class))).thenReturn(List.of(
				new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(10, 30), false),
				new TimeSlotResponse(LocalTime.of(10, 30), LocalTime.of(12, 0), true)));

		mockMvc.perform(get("/api/v1/classrooms/1/times").param("date", "2024-04-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].startTime").value("09:00"))
				.andExpect(jsonPath("$[0].endTime").value("10:30"))
				.andExpect(jsonPath("$[0].available").value(false))
				.andExpect(jsonPath("$[1].available").value(true));
	}

	@Test
	void returnsNotFoundProblemDetailWhenClassroomDoesNotExist() throws Exception {
		when(classroomAvailabilityService.findTimes(eq(99L), any(LocalDate.class)))
				.thenThrow(new ClassroomNotFoundException(99L));

		mockMvc.perform(get("/api/v1/classrooms/99/times").param("date", "2024-04-01"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.detail").value("강의실을 찾을 수 없습니다. id=99"));
	}

	@Test
	void returnsBadRequestWhenTimesDateIsMissing() throws Exception {
		mockMvc.perform(get("/api/v1/classrooms/1/times"))
				.andExpect(status().isBadRequest());
	}
}
