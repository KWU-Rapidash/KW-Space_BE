package com.example.KW_SPACE.auth.exception;

public record AuthErrorResponse(
		String code,
		String message
) {

	public static AuthErrorResponse from(AuthErrorCode errorCode) {
		return new AuthErrorResponse(errorCode.name(), errorCode.getMessage());
	}
}
