package com.example.KW_SPACE.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(
		@NotBlank String currentPassword,
		@NotBlank @Size(min = 8) String newPassword
) {
}
