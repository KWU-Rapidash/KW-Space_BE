package com.example.KW_SPACE.user.application;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String klasId) {
        super("사용자를 찾을 수 없습니다.");
    }
}
