package com.example.KW_SPACE.user.application;

import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import com.example.KW_SPACE.user.presentation.dto.PhoneUpdateResponse;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final UserService userService = new UserService(userRepository);

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
	void updatePhoneNumberChangesCurrentUsersPhoneNumber() {
		User user = User.create("2022202015", "홍길동", "010-1234-5678", "encoded-password");
		given(userRepository.findById(1L)).willReturn(Optional.of(user));
		given(userRepository.existsByPhoneNumber("010-0000-1111")).willReturn(false);

		PhoneUpdateResponse response = userService.updatePhoneNumber(1L, "010-0000-1111");

		assertThat(user.getPhoneNumber()).isEqualTo("010-0000-1111");
		assertThat(response.phoneNumber()).isEqualTo("010-0000-1111");
		assertThat(response.message()).isEqualTo("전화번호 수정에 성공했습니다.");
		verify(userRepository).existsByPhoneNumber("010-0000-1111");
	}

	@Test
	void updatePhoneNumberAllowsKeepingSamePhoneNumber() {
		User user = User.create("2022202015", "홍길동", "010-1234-5678", "encoded-password");
		given(userRepository.findById(1L)).willReturn(Optional.of(user));

		PhoneUpdateResponse response = userService.updatePhoneNumber(1L, "010-1234-5678");

		assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");
	}

	@Test
	void updatePhoneNumberThrowsExceptionWhenFormatIsInvalid() {
		assertThatThrownBy(() -> userService.updatePhoneNumber(1L, "01012345678"))
				.isInstanceOf(UserException.class)
				.extracting("errorCode")
				.isEqualTo(UserErrorCode.USER_INVALID_PHONE_NUMBER);
		verifyNoInteractions(userRepository);
	}

	@Test
	void updatePhoneNumberThrowsExceptionWhenPhoneNumberIsDuplicated() {
		User user = User.create("2022202015", "홍길동", "010-1234-5678", "encoded-password");
		given(userRepository.findById(1L)).willReturn(Optional.of(user));
		given(userRepository.existsByPhoneNumber("010-0000-1111")).willReturn(true);

		assertThatThrownBy(() -> userService.updatePhoneNumber(1L, "010-0000-1111"))
				.isInstanceOf(UserException.class)
				.extracting("errorCode")
				.isEqualTo(UserErrorCode.USER_DUPLICATED_PHONE_NUMBER);
	}
}
