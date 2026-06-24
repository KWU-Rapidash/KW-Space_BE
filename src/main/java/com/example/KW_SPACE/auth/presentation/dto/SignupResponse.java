package com.example.KW_SPACE.auth.presentation.dto;

import com.example.KW_SPACE.user.domain.User;

public record SignupResponse(
		String username,
		String klasId,
		String message
) {

	public static SignupResponse from(User user) {
		return new SignupResponse(user.getName(), user.getKlasId(), "회원가입에 성공했습니다.");
	}
}
