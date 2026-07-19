package com.example.KW_SPACE.auth.presentation.dto;

public record SignupResponse(
		boolean success,
		String message
) {

	public static SignupResponse created() {
		return new SignupResponse(true, "회원가입에 성공했습니다.");
	}
}
