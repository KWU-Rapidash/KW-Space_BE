package com.example.KW_SPACE.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스/도메인 규칙 위반 예외의 베이스. 전역 핸들러가 status/message로 변환한다.
 */
@Getter
public abstract class BusinessException extends RuntimeException {

	private final HttpStatus status;

	protected BusinessException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}
}
