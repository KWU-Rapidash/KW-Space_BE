package com.example.KW_SPACE.auth.application;

import com.example.KW_SPACE.auth.presentation.dto.LoginResponse;

public record LoginResult(
		String accessToken,
		LoginResponse response
) {
}
