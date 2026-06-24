package com.example.KW_SPACE.config;

import java.util.List;

public final class AuthPublicEndpoints {

	public static final List<String> PUBLIC_GET_PATHS = List.of(
			"/api/health",
			"/api/health/",
			"/api/v1/classrooms",
			"/api/v1/classrooms/",
			"/api/v1/classrooms/*/times",
			"/api/v1/classrooms/*/times/"
	);
	public static final List<String> PUBLIC_HEAD_PATHS = List.of("/api/health", "/api/health/");
	public static final List<String> PUBLIC_POST_PATHS = List.of(
			"/api/v1/auth/signup",
			"/api/v1/auth/login",
			"/api/v1/auth/password-reset",
			"/api/v1/auth/logout"
	);

	private AuthPublicEndpoints() {
	}
}
