package com.example.KW_SPACE.auth.jwt;

import com.example.KW_SPACE.user.domain.UserRole;

public record JwtAuthenticationClaims(
		Long userId,
		UserRole role,
		int tokenVersion
) {
}
