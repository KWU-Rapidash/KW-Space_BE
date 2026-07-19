package com.example.KW_SPACE.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.auth.jwt.JwtTokenProvider;
import com.example.KW_SPACE.auth.klas.KlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthResult;
import com.example.KW_SPACE.auth.presentation.dto.LoginRequest;
import com.example.KW_SPACE.auth.presentation.dto.PasswordResetRequest;
import com.example.KW_SPACE.auth.presentation.dto.SignupRequest;
import com.example.KW_SPACE.auth.presentation.dto.SignupResponse;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final KlasAuthClient klasAuthClient = mock(KlasAuthClient.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
	private final PasswordResetUpdater passwordResetUpdater = mock(PasswordResetUpdater.class);
	private final AuthService authService = new AuthService(
			userRepository,
			klasAuthClient,
			passwordEncoder,
			jwtTokenProvider,
			passwordResetUpdater
	);

	@Test
	void signupCreatesUserWithKlasVerifiedNameAndEncodedPassword() {
		SignupRequest request = new SignupRequest("요청이름", "2025404000", "valid-klas-password", "service-password");
		given(userRepository.existsByKlasId("2025404000")).willReturn(false);
		given(klasAuthClient.verify("2025404000", "valid-klas-password"))
				.willReturn(KlasAuthResult.success("2025404000", "이효원"));
		given(passwordEncoder.encode("service-password")).willReturn("encoded-service-password");
		given(userRepository.saveAndFlush(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

		SignupResponse response = authService.signup(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).saveAndFlush(userCaptor.capture());
		User savedUser = userCaptor.getValue();

		assertThat(savedUser.getKlasId()).isEqualTo("2025404000");
		assertThat(savedUser.getName()).isEqualTo("이효원");
		assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-service-password");
		assertThat(response.success()).isTrue();
		assertThat(response.message()).isEqualTo("회원가입에 성공했습니다.");
	}

	@Test
	void signupFallsBackToRequestNameWhenKlasNameIsBlank() {
		SignupRequest request = new SignupRequest("요청이름", "2025404000", "valid-klas-password", "service-password");
		given(userRepository.existsByKlasId("2025404000")).willReturn(false);
		given(klasAuthClient.verify("2025404000", "valid-klas-password"))
				.willReturn(KlasAuthResult.success("2025404000", " "));
		given(passwordEncoder.encode("service-password")).willReturn("encoded-service-password");
		given(userRepository.saveAndFlush(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

		SignupResponse response = authService.signup(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).saveAndFlush(userCaptor.capture());
		assertThat(userCaptor.getValue().getName()).isEqualTo("요청이름");
		assertThat(response.success()).isTrue();
	}

	@Test
	void signupRejectsDuplicatedKlasIdBeforeKlasVerification() {
		SignupRequest request = new SignupRequest("이효원", "2025404000", "valid-klas-password", "service-password");
		given(userRepository.existsByKlasId("2025404000")).willReturn(true);

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_DUPLICATED_KLAS_ID));

		verifyNoInteractions(klasAuthClient, passwordEncoder);
	}

	@Test
	void signupRejectsInvalidKlasCredentials() {
		SignupRequest request = new SignupRequest("이효원", "2025404000", "wrong-klas-password", "service-password");
		given(userRepository.existsByKlasId("2025404000")).willReturn(false);
		given(klasAuthClient.verify("2025404000", "wrong-klas-password"))
				.willReturn(KlasAuthResult.failure());

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_KLAS_CREDENTIALS));

		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void signupMapsDuplicateConstraintViolationToDuplicatedKlasId() {
		SignupRequest request = new SignupRequest("요청이름", "2025404000", "valid-klas-password", "service-password");
		given(userRepository.existsByKlasId("2025404000")).willReturn(false);
		given(klasAuthClient.verify("2025404000", "valid-klas-password"))
				.willReturn(KlasAuthResult.success("2025404000", "이효원"));
		given(passwordEncoder.encode("service-password")).willReturn("encoded-service-password");
		given(userRepository.saveAndFlush(any(User.class)))
				.willThrow(new DataIntegrityViolationException("constraint [uk_users_klas_id]"));

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_DUPLICATED_KLAS_ID));
	}

	@Test
	void signupMapsNestedDuplicateConstraintViolationToDuplicatedKlasId() {
		SignupRequest request = new SignupRequest("요청이름", "2025404000", "valid-klas-password", "service-password");
		given(userRepository.existsByKlasId("2025404000")).willReturn(false);
		given(klasAuthClient.verify("2025404000", "valid-klas-password"))
				.willReturn(KlasAuthResult.success("2025404000", "이효원"));
		given(passwordEncoder.encode("service-password")).willReturn("encoded-service-password");
		given(userRepository.saveAndFlush(any(User.class)))
				.willThrow(new DataIntegrityViolationException(
						"could not execute statement",
						new RuntimeException("duplicate key violates constraint [uk_users_klas_id]")
				));

		assertThatThrownBy(() -> authService.signup(request))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_DUPLICATED_KLAS_ID));
	}

	@Test
	void signupRethrowsOtherConstraintViolation() {
		SignupRequest request = new SignupRequest("요청이름", "2025404000", "valid-klas-password", "service-password");
		DataIntegrityViolationException dataIntegrityViolationException =
				new DataIntegrityViolationException("constraint [uk_users_phone_number]");
		given(userRepository.existsByKlasId("2025404000")).willReturn(false);
		given(klasAuthClient.verify("2025404000", "valid-klas-password"))
				.willReturn(KlasAuthResult.success("2025404000", "이효원"));
		given(passwordEncoder.encode("service-password")).willReturn("encoded-service-password");
		given(userRepository.saveAndFlush(any(User.class))).willThrow(dataIntegrityViolationException);

		assertThatThrownBy(() -> authService.signup(request))
				.isSameAs(dataIntegrityViolationException);
	}

	@Test
	void loginReturnsAccessToken() {
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		setUserId(user, 1L);
		given(userRepository.findByKlasId("2025404000")).willReturn(Optional.of(user));
		given(passwordEncoder.matches("service-password", "encoded-password")).willReturn(true);
		given(jwtTokenProvider.createAccessToken(user)).willReturn("access-token");

		LoginResult result = authService.login(new LoginRequest("2025404000", "service-password"));

		assertThat(result.accessToken()).isEqualTo("access-token");
		assertThat(result.response().success()).isTrue();
		assertThat(result.response().message()).isEqualTo("로그인에 성공했습니다.");
		verifyNoInteractions(klasAuthClient);
	}

	@Test
	void loginRejectsUnknownKlasId() {
		given(userRepository.findByKlasId("2025404000")).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("2025404000", "service-password")))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_CREDENTIALS));

		verifyNoInteractions(klasAuthClient, passwordEncoder, jwtTokenProvider);
	}

	@Test
	void loginRejectsWrongPassword() {
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		given(userRepository.findByKlasId("2025404000")).willReturn(Optional.of(user));
		given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginRequest("2025404000", "wrong-password")))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_CREDENTIALS));

		verify(passwordEncoder).matches("wrong-password", "encoded-password");
		verifyNoInteractions(klasAuthClient, jwtTokenProvider);
		verifyNoMoreInteractions(passwordEncoder);
	}

	@Test
	void resetPasswordVerifiesKlasAndDelegatesPasswordUpdate() {
		User user = User.create("2025404000", "이효원", null, "old-encoded-password");
		given(userRepository.findByKlasId("2025404000")).willReturn(Optional.of(user));
		given(klasAuthClient.verify("2025404000", "valid-klas-password"))
				.willReturn(KlasAuthResult.success("2025404000", "이효원"));
		given(passwordEncoder.encode("new-service-password")).willReturn("new-encoded-password");

		authService.resetPassword(new PasswordResetRequest(
				"2025404000",
				"valid-klas-password",
				"new-service-password"
		));

		verify(passwordResetUpdater).update("2025404000", "new-encoded-password");
		verifyNoMoreInteractions(jwtTokenProvider);
	}

	@Test
	void resetPasswordRejectsUnknownKlasIdBeforeKlasVerification() {
		given(userRepository.findByKlasId("2025404000")).willReturn(Optional.empty());

		assertThatThrownBy(() -> authService.resetPassword(new PasswordResetRequest(
				"2025404000",
				"valid-klas-password",
				"new-service-password"
		)))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_USER_NOT_FOUND));

		verifyNoInteractions(klasAuthClient, passwordEncoder, jwtTokenProvider, passwordResetUpdater);
	}

	@Test
	void resetPasswordRejectsInvalidKlasCredentialsWithoutUpdatingPassword() {
		User user = User.create("2025404000", "이효원", null, "old-encoded-password");
		given(userRepository.findByKlasId("2025404000")).willReturn(Optional.of(user));
		given(klasAuthClient.verify("2025404000", "wrong-klas-password"))
				.willReturn(KlasAuthResult.failure());

		assertThatThrownBy(() -> authService.resetPassword(new PasswordResetRequest(
				"2025404000",
				"wrong-klas-password",
				"new-service-password"
		)))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_INVALID_KLAS_CREDENTIALS));

		verifyNoInteractions(passwordEncoder, jwtTokenProvider, passwordResetUpdater);
	}

	private void setUserId(User user, Long id) {
		try {
			var field = User.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(user, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
