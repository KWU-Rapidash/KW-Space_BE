package com.example.KW_SPACE.auth.exception;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class AuthErrorResponseWriter {

	public void write(HttpServletResponse response, AuthErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("""
				{"code":"%s","message":"%s"}""".formatted(errorCode.name(), errorCode.getMessage()));
	}
}
