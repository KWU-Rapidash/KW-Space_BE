package com.example.KW_SPACE.user.presentation.dto;

import com.example.KW_SPACE.user.domain.User;

public record PhoneUpdateResponse(
		String phoneNumber,
		String message
) {

	public static PhoneUpdateResponse from(User user) {
		return new PhoneUpdateResponse(user.getPhoneNumber(), "전화번호 수정에 성공했습니다.");
	}
}
