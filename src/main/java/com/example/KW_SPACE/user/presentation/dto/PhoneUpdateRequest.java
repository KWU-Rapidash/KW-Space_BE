package com.example.KW_SPACE.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneUpdateRequest(
		@NotBlank String phoneNumber
) {
}
