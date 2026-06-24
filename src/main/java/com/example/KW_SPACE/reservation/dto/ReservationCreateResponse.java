package com.example.KW_SPACE.reservation.dto;

import com.example.KW_SPACE.reservation.domain.Reservation;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationCreateResponse(
		Long reservationId,
		String classroomId,
		String classroomNumber,
		@JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
		@JsonFormat(pattern = "HH:mm") LocalTime startTime,
		@JsonFormat(pattern = "HH:mm") LocalTime endTime,
		ReservationStatus status) {

	public static ReservationCreateResponse of(Reservation reservation) {
		return new ReservationCreateResponse(
				reservation.getId(),
				reservation.getClassroom().getCode(),
				reservation.getClassroom().getRoomNumber(),
				reservation.getDate(),
				reservation.getStartTime(),
				reservation.getEndTime(),
				reservation.getStatus());
	}
}
