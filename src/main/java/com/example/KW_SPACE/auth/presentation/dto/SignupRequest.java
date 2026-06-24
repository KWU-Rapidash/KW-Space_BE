package com.example.KW_SPACE.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotBlank String name,
		@NotBlank String klasId,
		@NotBlank String klasPassword,
		@NotBlank @Size(min = 8) String password
) {
}
