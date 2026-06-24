package com.example.KW_SPACE.reservation.dto;

import com.example.KW_SPACE.classroom.domain.Classroom;
import java.util.List;

public record ClassroomAvailabilityResponse(Long id, int floor, String roomNumber, List<TimeSlotResponse> times) {

	public static ClassroomAvailabilityResponse of(Classroom classroom, List<TimeSlotResponse> times) {
		return new ClassroomAvailabilityResponse(
				classroom.getId(), classroom.getFloor(), classroom.getRoomNumber(), times);
	}
}
