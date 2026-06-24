package com.example.KW_SPACE.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record UserWithdrawalRequest(
		@NotBlank String password
) {
}
