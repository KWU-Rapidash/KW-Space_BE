package com.example.KW_SPACE.user.application;

import java.util.Objects;

public class UserException extends RuntimeException {

	private final UserErrorCode errorCode;

	public UserException(UserErrorCode errorCode) {
		super(Objects.requireNonNull(errorCode).getMessage());
		this.errorCode = errorCode;
	}

	public UserErrorCode getErrorCode() {
		return errorCode;
	}
}
