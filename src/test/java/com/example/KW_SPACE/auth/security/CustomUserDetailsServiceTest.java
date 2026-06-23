package com.example.KW_SPACE.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class CustomUserDetailsServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final CustomUserDetailsService customUserDetailsService = new CustomUserDetailsService(userRepository);

	@Test
	void loadsUserById() {
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		setUserId(user, 1L);
		given(userRepository.findById(1L)).willReturn(Optional.of(user));

		CustomUserDetails userDetails = customUserDetailsService.loadUserById(1L);

		assertThat(userDetails.getId()).isEqualTo(1L);
		assertThat(userDetails.getUsername()).isEqualTo("1");
		assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
		assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
	}

	@Test
	void throwsWhenUserDoesNotExist() {
		given(userRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> customUserDetailsService.loadUserById(1L))
				.isInstanceOf(UsernameNotFoundException.class);
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
