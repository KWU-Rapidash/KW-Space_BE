package com.example.KW_SPACE.user.presentation;

import com.example.KW_SPACE.auth.security.CustomUserDetails;
import com.example.KW_SPACE.user.application.UserService;
import com.example.KW_SPACE.user.presentation.dto.PasswordUpdateRequest;
import com.example.KW_SPACE.user.presentation.dto.PasswordUpdateResponse;
import com.example.KW_SPACE.user.presentation.dto.PhoneUpdateRequest;
import com.example.KW_SPACE.user.presentation.dto.PhoneUpdateResponse;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping({"", "/"})
	public UserInfoResponse getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
		return userService.getMyInfo(userDetails.getId());
	}

	@PatchMapping("/phone")
	public PhoneUpdateResponse updatePhoneNumber(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody PhoneUpdateRequest request
	) {
		return userService.updatePhoneNumber(userDetails.getId(), request.phoneNumber());
	}

	@PatchMapping("/password")
	public PasswordUpdateResponse updatePassword(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody PasswordUpdateRequest request
	) {
		userService.updatePassword(userDetails.getId(), request.currentPassword(), request.newPassword());

		return PasswordUpdateResponse.updated();
	}
}
