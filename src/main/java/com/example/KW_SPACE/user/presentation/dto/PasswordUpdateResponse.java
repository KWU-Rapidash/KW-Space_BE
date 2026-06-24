package com.example.KW_SPACE.user.presentation.dto;

public record PasswordUpdateResponse(
		boolean success,
		String message
) {

	public static PasswordUpdateResponse updated() {
		return new PasswordUpdateResponse(true, "비밀번호 수정에 성공했습니다.");
	}
}
