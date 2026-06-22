package com.example.KW_SPACE.user.presentation.dto;

import com.example.KW_SPACE.user.domain.User;

public record UserInfoResponse(
        String username,
        String studentNumber,
        String phoneNumber,
        String message
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getName(),
                user.getStudentNumber(),
                user.getPhoneNumber(),
                "내 정보 조회에 성공했습니다."
        );
    }
}
