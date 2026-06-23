package com.example.KW_SPACE.reservation.dto;

import com.example.KW_SPACE.classroom.domain.Classroom;

public record ClassroomAvailabilityResponse(Long id, int floor, String roomNumber, boolean available) {

	public static ClassroomAvailabilityResponse of(Classroom classroom, boolean available) {
		return new ClassroomAvailabilityResponse(
				classroom.getId(), classroom.getFloor(), classroom.getRoomNumber(), available);
	}
}
