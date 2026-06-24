package com.example.KW_SPACE.reservation.dto;

import com.example.KW_SPACE.classroom.domain.Classroom;
import java.util.List;

public record ClassroomAvailabilityResponse(
		String classroomId, int floor, String classroomNumber, boolean available, List<TimeSlotResponse> times) {

	public static ClassroomAvailabilityResponse of(Classroom classroom, List<TimeSlotResponse> times) {
		boolean available = times.stream().anyMatch(TimeSlotResponse::available);
		return new ClassroomAvailabilityResponse(
				classroom.getCode(), classroom.getFloor(), classroom.getRoomNumber(), available, times);
	}
}
