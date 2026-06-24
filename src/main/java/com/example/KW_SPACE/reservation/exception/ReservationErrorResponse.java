package com.example.KW_SPACE.reservation.exception;

public record ReservationErrorResponse(
		String code,
		String message
) {

	public static ReservationErrorResponse from(ReservationErrorCode errorCode) {
		return new ReservationErrorResponse(errorCode.name(), errorCode.getMessage());
	}
}
