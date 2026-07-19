package com.example.KW_SPACE.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.KW_SPACE.auth.exception.AuthErrorCode;
import com.example.KW_SPACE.auth.exception.AuthException;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PasswordResetUpdaterTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final PasswordResetUpdater passwordResetUpdater = new PasswordResetUpdater(userRepository);

	@Test
	void updatesPasswordHashAndTokenVersion() {
		User user = User.create("2025404000", "이효원", null, "old-encoded-password");
		given(userRepository.findByKlasId("2025404000")).willReturn(Optional.of(user));

		passwordResetUpdater.update("2025404000", "new-encoded-password");

		assertThat(user.getPasswordHash()).isEqualTo("new-encoded-password");
		assertThat(user.getTokenVersion()).isEqualTo(1);
	}

	@Test
	void rejectsUserRemovedAfterKlasVerification() {
		given(userRepository.findByKlasId("2025404000")).willReturn(Optional.empty());

		assertThatThrownBy(() -> passwordResetUpdater.update("2025404000", "new-encoded-password"))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_USER_NOT_FOUND));
	}
}
