package com.example.KW_SPACE.auth.presentation;

import com.example.KW_SPACE.auth.application.AuthService;
import com.example.KW_SPACE.auth.application.LoginResult;
import com.example.KW_SPACE.auth.jwt.JwtProperties;
import com.example.KW_SPACE.auth.presentation.dto.LoginRequest;
import com.example.KW_SPACE.auth.presentation.dto.LoginResponse;
import com.example.KW_SPACE.auth.presentation.dto.SignupRequest;
import com.example.KW_SPACE.auth.presentation.dto.SignupResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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
	private final JwtProperties jwtProperties;

	public AuthController(AuthService authService, JwtProperties jwtProperties) {
		this.authService = authService;
		this.jwtProperties = jwtProperties;
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
				.header(HttpHeaders.SET_COOKIE, accessTokenCookie(loginResult.accessToken()).toString())
				.body(loginResult.response());
	}

	private ResponseCookie accessTokenCookie(String accessToken) {
		return ResponseCookie.from("accessToken", accessToken)
				.httpOnly(true)
				.secure(true)
				.sameSite("Lax")
				.path("/")
				.maxAge(jwtProperties.accessTokenTtl())
				.build();
	}
}
