package com.example.KW_SPACE.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record KlasVerifyRequest(
		@NotBlank String klasId,
		@NotBlank String klasPassword
) {
}
