package com.example.KW_SPACE.auth.exception;

import java.util.Objects;

public class AuthException extends RuntimeException {

	private final AuthErrorCode errorCode;

	public AuthException(AuthErrorCode errorCode) {
		super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage());
		this.errorCode = errorCode;
	}

	public AuthErrorCode getErrorCode() {
		return errorCode;
	}
}
