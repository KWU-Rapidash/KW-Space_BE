package com.example.KW_SPACE.user.application;

import org.springframework.http.HttpStatus;

public enum UserErrorCode {
	USER_INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "전화번호 형식이 올바르지 않습니다."),
	USER_DUPLICATED_PHONE_NUMBER(HttpStatus.CONFLICT, "이미 사용 중인 전화번호입니다."),
	USER_CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.");

	private final HttpStatus status;
	private final String message;

	UserErrorCode(HttpStatus status, String message) {
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
