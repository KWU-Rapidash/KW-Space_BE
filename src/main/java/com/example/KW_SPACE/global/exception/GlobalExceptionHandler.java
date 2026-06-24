package com.example.KW_SPACE.global.exception;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthErrorResponse;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.auth.klas.KlasAuthServerUnavailableException;
import com.example.KW_SPACE.user.application.UserErrorCode;
import com.example.KW_SPACE.user.application.UserException;
import com.example.KW_SPACE.user.application.UserNotFoundException;
import java.util.List;
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

	@ExceptionHandler(UserException.class)
	public ResponseEntity<AuthErrorResponse> handleUserException(UserException exception) {
		return toResponse(exception.getErrorCode());
	}

	@ExceptionHandler(KlasAuthServerUnavailableException.class)
	public ResponseEntity<AuthErrorResponse> handleKlasServerUnavailable() {
		return toResponse(AuthErrorCode.AUTH_KLAS_SERVER_UNAVAILABLE);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<AuthErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
		List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
		AuthErrorCode errorCode = resolveValidationErrorCode(fieldErrors);

		return toResponse(errorCode);
	}

	private AuthErrorCode resolveValidationErrorCode(List<FieldError> fieldErrors) {
		if (fieldErrors.stream().anyMatch(this::isRequiredFieldMissing)) {
			return AuthErrorCode.AUTH_REQUIRED_FIELD_MISSING;
		}

		if (fieldErrors.stream().anyMatch(this::isPasswordPolicyViolation)) {
			return AuthErrorCode.AUTH_PASSWORD_POLICY_VIOLATION;
		}

		return AuthErrorCode.AUTH_REQUIRED_FIELD_MISSING;
	}

	private boolean isRequiredFieldMissing(FieldError fieldError) {
		String code = fieldError.getCode();

		return "NotBlank".equals(code) || "NotEmpty".equals(code) || "NotNull".equals(code);
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

	private ResponseEntity<AuthErrorResponse> toResponse(UserErrorCode errorCode) {
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(new AuthErrorResponse(errorCode.name(), errorCode.getMessage()));
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<AuthErrorResponse> handleUserNotFound() {
		return toResponse(AuthErrorCode.AUTH_INVALID_CREDENTIALS);
	}
}
