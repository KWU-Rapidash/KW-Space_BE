package com.example.KW_SPACE.auth.exception;

import org.springframework.http.HttpStatus;

public enum AuthErrorCode {
	AUTH_REQUIRED_FIELD_MISSING(HttpStatus.BAD_REQUEST, "필수 입력값이 누락되었습니다."),
	AUTH_INVALID_KLAS_CREDENTIALS(HttpStatus.UNAUTHORIZED, "KLAS 인증 정보가 일치하지 않습니다."),
	AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
	AUTH_DUPLICATED_KLAS_ID(HttpStatus.CONFLICT, "이미 가입된 학번입니다."),
	AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증 토큰입니다."),
	AUTH_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다."),
	AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	AUTH_PASSWORD_POLICY_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT, "비밀번호 정책을 만족하지 않습니다."),
	AUTH_KLAS_SERVER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "KLAS 서버를 사용할 수 없습니다.");

	private final HttpStatus status;
	private final String message;

	AuthErrorCode(HttpStatus status, String message) {
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
