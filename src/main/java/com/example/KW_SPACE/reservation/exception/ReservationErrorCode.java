package com.example.KW_SPACE.reservation.exception;

import org.springframework.http.HttpStatus;

public enum ReservationErrorCode {
	CLASSROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "강의실을 찾을 수 없습니다."),
	RESERVATION_CONFLICT(HttpStatus.CONFLICT, "이미 예약된 시간대입니다."),
	INVALID_SLOT(HttpStatus.BAD_REQUEST, "예약 가능한 시간 슬롯이 아닙니다."),
	PAST_TIME(HttpStatus.BAD_REQUEST, "지난 시간은 예약할 수 없습니다.");

	private final HttpStatus status;
	private final String message;

	ReservationErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}
}
