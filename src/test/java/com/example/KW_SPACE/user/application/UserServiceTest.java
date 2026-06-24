package com.example.KW_SPACE.user.application;

import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class UserServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final UserService userService = new UserService(userRepository, passwordEncoder);

	@Test
	void getMyInfoReturnsUserInfoByKlasId() {
		User user = User.create("2022202015", "홍길동", "010-1234-5678", "encoded-password");
		given(userRepository.findByKlasId("2022202015")).willReturn(Optional.of(user));

		UserInfoResponse response = userService.getMyInfo("2022202015");

		assertThat(response.name()).isEqualTo("홍길동");
		assertThat(response.klasId()).isEqualTo("2022202015");
		assertThat(response.phoneNumber()).isEqualTo("010-****-5678");
	}

	@Test
	void getMyInfoThrowsExceptionWhenKlasIdDoesNotExist() {
		given(userRepository.findByKlasId("2022202015")).willReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getMyInfo("2022202015"))
				.isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void updatePasswordStoresEncodedNewPassword() {
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		given(userRepository.findById(1L)).willReturn(Optional.of(user));
		given(passwordEncoder.matches("current-password", "encoded-password")).willReturn(true);
		given(passwordEncoder.encode("new-password")).willReturn("new-encoded-password");

		userService.updatePassword(1L, "current-password", "new-password");

		assertThat(user.getPasswordHash()).isEqualTo("new-encoded-password");
		verify(passwordEncoder).matches("current-password", "encoded-password");
		verify(passwordEncoder).encode("new-password");
	}

	@Test
	void updatePasswordRejectsMismatchedCurrentPassword() {
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		given(userRepository.findById(1L)).willReturn(Optional.of(user));
		given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

		assertThatThrownBy(() -> userService.updatePassword(1L, "wrong-password", "new-password"))
				.isInstanceOfSatisfying(UserException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_CURRENT_PASSWORD_MISMATCH));

		assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
		verify(passwordEncoder).matches("wrong-password", "encoded-password");
		verifyNoMoreInteractions(passwordEncoder);
	}

	@Test
	void updatePasswordThrowsExceptionWhenUserDoesNotExist() {
		given(userRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> userService.updatePassword(1L, "current-password", "new-password"))
				.isInstanceOf(UserNotFoundException.class);
	}
}
