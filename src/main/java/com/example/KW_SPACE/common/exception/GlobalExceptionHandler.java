package com.example.KW_SPACE.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ProblemDetail handleBusiness(BusinessException exception) {
		return ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "요청 파라미터가 유효하지 않습니다.");
	}
}
