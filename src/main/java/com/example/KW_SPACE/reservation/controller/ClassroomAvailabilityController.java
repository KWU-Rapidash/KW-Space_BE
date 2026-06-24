package com.example.KW_SPACE.reservation.controller;

import com.example.KW_SPACE.reservation.dto.ClassroomAvailabilityResponse;
import com.example.KW_SPACE.reservation.dto.TimeSlotResponse;
import com.example.KW_SPACE.reservation.service.ClassroomAvailabilityService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/classrooms")
public class ClassroomAvailabilityController {

	private final ClassroomAvailabilityService classroomAvailabilityService;

	@GetMapping
	public List<ClassroomAvailabilityResponse> getClassrooms(
			@RequestParam @Min(1) @Max(9) int floor,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return classroomAvailabilityService.findAvailability(floor, date);
	}

	@GetMapping("/{classroomId}/times")
	public List<TimeSlotResponse> getClassroomTimes(
			@PathVariable Long classroomId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return classroomAvailabilityService.findTimes(classroomId, date);
	}
}
