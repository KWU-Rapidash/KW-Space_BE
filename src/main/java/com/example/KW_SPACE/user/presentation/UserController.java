package com.example.KW_SPACE.user.presentation;

import com.example.KW_SPACE.auth.cookie.AuthCookieService;
import com.example.KW_SPACE.auth.security.CustomUserDetails;
import com.example.KW_SPACE.user.application.UserService;
import com.example.KW_SPACE.user.presentation.dto.PasswordUpdateRequest;
import com.example.KW_SPACE.user.presentation.dto.PasswordUpdateResponse;
import com.example.KW_SPACE.user.presentation.dto.PhoneUpdateRequest;
import com.example.KW_SPACE.user.presentation.dto.PhoneUpdateResponse;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

	private final UserService userService;
	private final AuthCookieService authCookieService;

	public UserController(UserService userService, AuthCookieService authCookieService) {
		this.userService = userService;
		this.authCookieService = authCookieService;
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

	@DeleteMapping({"", "/"})
	public ResponseEntity<Void> withdraw(
			@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		userService.withdraw(userDetails.getId());

		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, authCookieService.deleteAccessTokenCookie().toString())
				.build();
	}
}
