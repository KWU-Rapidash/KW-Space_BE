package com.example.KW_SPACE.auth.presentation;

import com.example.KW_SPACE.auth.application.AuthService;
import com.example.KW_SPACE.auth.application.LoginResult;
import com.example.KW_SPACE.auth.cookie.AuthCookieService;
import com.example.KW_SPACE.auth.presentation.dto.LoginRequest;
import com.example.KW_SPACE.auth.presentation.dto.LoginResponse;
import com.example.KW_SPACE.auth.presentation.dto.LogoutResponse;
import com.example.KW_SPACE.auth.presentation.dto.PasswordResetRequest;
import com.example.KW_SPACE.auth.presentation.dto.PasswordResetResponse;
import com.example.KW_SPACE.auth.presentation.dto.SignupRequest;
import com.example.KW_SPACE.auth.presentation.dto.SignupResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final AuthCookieService authCookieService;

	public AuthController(AuthService authService, AuthCookieService authCookieService) {
		this.authService = authService;
		this.authCookieService = authCookieService;
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
		return authService.signup(request);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		LoginResult loginResult = authService.login(request);

		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE,
						authCookieService.createAccessTokenCookie(loginResult.accessToken()).toString())
				.body(loginResult.response());
	}

	@PostMapping("/logout")
	public ResponseEntity<LogoutResponse> logout() {
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, authCookieService.deleteAccessTokenCookie().toString())
				.body(new LogoutResponse(true, "로그아웃에 성공했습니다."));
	}

	@PostMapping("/password-reset")
	public PasswordResetResponse resetPassword(@Valid @RequestBody PasswordResetRequest request) {
		authService.resetPassword(request);

		return new PasswordResetResponse(true, "비밀번호 재설정에 성공했습니다.");
	}
}
