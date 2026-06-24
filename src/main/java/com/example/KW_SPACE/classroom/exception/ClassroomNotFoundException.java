package com.example.KW_SPACE.classroom.exception;

import com.example.KW_SPACE.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ClassroomNotFoundException extends BusinessException {

	public ClassroomNotFoundException(Long classroomId) {
		super(HttpStatus.NOT_FOUND, "강의실을 찾을 수 없습니다. id=" + classroomId);
	}
}
