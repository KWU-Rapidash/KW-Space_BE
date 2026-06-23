package com.example.KW_SPACE.global.exception;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthErrorResponse;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.auth.klas.KlasAuthServerUnavailableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<AuthErrorResponse> handleAuthException(AuthException exception) {
		return toResponse(exception.getErrorCode());
	}

	@ExceptionHandler(KlasAuthServerUnavailableException.class)
	public ResponseEntity<AuthErrorResponse> handleKlasServerUnavailable() {
		return toResponse(AuthErrorCode.AUTH_KLAS_SERVER_UNAVAILABLE);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<AuthErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
		AuthErrorCode errorCode = exception.getBindingResult().getFieldErrors().stream()
				.anyMatch(this::isPasswordPolicyViolation)
				? AuthErrorCode.AUTH_PASSWORD_POLICY_VIOLATION
				: AuthErrorCode.AUTH_REQUIRED_FIELD_MISSING;

		return toResponse(errorCode);
	}

	private boolean isPasswordPolicyViolation(FieldError fieldError) {
		String field = fieldError.getField();
		String code = fieldError.getCode();

		return field != null
				&& field.toLowerCase().contains("password")
				&& ("Size".equals(code) || "Pattern".equals(code));
	}

	private ResponseEntity<AuthErrorResponse> toResponse(AuthErrorCode errorCode) {
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(AuthErrorResponse.from(errorCode));
	}
}
