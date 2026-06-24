package com.example.KW_SPACE.reservation.dto;

import com.example.KW_SPACE.reservation.domain.Reservation;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

public record UserReservationResponse(
		Long reservationId,
		@JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
		String classroom,
		String reserverName,
		@JsonFormat(pattern = "HH:mm") LocalTime startTime,
		@JsonFormat(pattern = "HH:mm") LocalTime endTime,
		ReservationStatus status) {

	public static UserReservationResponse of(Reservation reservation) {
		return new UserReservationResponse(
				reservation.getId(),
				reservation.getDate(),
				reservation.getClassroom().getCode(),
				reservation.getUser().getName(),
				reservation.getStartTime(),
				reservation.getEndTime(),
				reservation.getStatus());
	}
}
