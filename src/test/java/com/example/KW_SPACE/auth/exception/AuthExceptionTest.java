package com.example.KW_SPACE.auth.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class AuthExceptionTest {

	@Test
	void storesAuthErrorCode() {
		AuthException exception = new AuthException(AuthErrorCode.AUTH_INVALID_CREDENTIALS);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
		assertThat(exception).hasMessage("아이디 또는 비밀번호가 일치하지 않습니다.");
	}

	@Test
	void rejectsNullErrorCode() {
		assertThatNullPointerException()
				.isThrownBy(() -> new AuthException(null))
				.withMessage("errorCode must not be null");
	}
}
