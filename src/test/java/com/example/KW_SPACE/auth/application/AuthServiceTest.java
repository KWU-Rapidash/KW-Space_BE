package com.example.KW_SPACE.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.auth.klas.KlasAuthClient;
import com.example.KW_SPACE.auth.klas.KlasAuthResult;
import com.example.KW_SPACE.auth.presentation.dto.SignupRequest;
import com.example.KW_SPACE.auth.presentation.dto.SignupResponse;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final KlasAuthClient klasAuthClient = mock(KlasAuthClient.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final AuthService authService = new AuthService(userRepository, klasAuthClient, passwordEncoder);

	@Test
	void signupCreatesUserWithKlasVerifiedNameAndEncodedPassword() {
		SignupRequest request = new SignupRequest("요청이름", "2025404000", "valid-klas-password", "service-password");
		given(userRepository.existsByKlasId("2025404000")).willReturn(false);
		given(klasAuthClient.verify("2025404000", "valid-klas-password"))
				.willReturn(KlasAuthResult.success("2025404000", "이효원"));
		given(passwordEncoder.encode("service-password")).willReturn("encoded-service-password");
		given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

		SignupResponse response = authService.signup(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User savedUser = userCaptor.getValue();

		assertThat(savedUser.getKlasId()).isEqualTo("2025404000");
		assertThat(savedUser.getName()).isEqualTo("이효원");
		assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-service-password");
		assertThat(response.username()).isEqualTo("이효원");
		assertThat(response.klasId()).isEqualTo("2025404000");
		assertThat(response.message()).isEqualTo("회원가입에 성공했습니다.");
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
}
