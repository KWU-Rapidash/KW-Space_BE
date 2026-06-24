package com.example.KW_SPACE.user.presentation.dto;

import com.example.KW_SPACE.user.domain.User;

public record UserInfoResponse(
        String name,
        String klasId,
        String phoneNumber,
        String message
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getName(),
                user.getKlasId(),
                maskPhoneNumber(user.getPhoneNumber()),
                "내 정보 조회에 성공했습니다."
        );
    }

    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return phoneNumber;
        }

        StringBuilder maskedPhoneNumber = new StringBuilder(phoneNumber);
        int digitCount = 0;
        for (int i = phoneNumber.length() - 1; i >= 0; i--) {
            if (!Character.isDigit(phoneNumber.charAt(i))) {
                continue;
            }

            digitCount++;
            if (digitCount > 4 && digitCount <= 8) {
                maskedPhoneNumber.setCharAt(i, '*');
            }
        }

        return maskedPhoneNumber.toString();
    }
}
