package com.example.KW_SPACE.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import com.example.KW_SPACE.user.presentation.dto.UserInfoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(UserService.class)
class UserServiceIntegrationTest {

	private final UserRepository userRepository;
	private final UserService userService;

	@Autowired
	UserServiceIntegrationTest(UserRepository userRepository, UserService userService) {
		this.userRepository = userRepository;
		this.userService = userService;
	}

	@Test
	void getMyInfoReturnsOnlyUserMatchingRequestedKlasId() {
		userRepository.save(User.create("2022202014", "김철수", "010-1111-2222", "encoded-password-1"));
		userRepository.save(User.create("2022202015", "홍길동", "010-1234-5678", "encoded-password-2"));
		userRepository.saveAndFlush(User.create("2022202016", "이영희", "010-3333-4444", "encoded-password-3"));

		UserInfoResponse response = userService.getMyInfo("2022202015");

		assertThat(response.username()).isEqualTo("홍길동");
		assertThat(response.klasId()).isEqualTo("2022202015");
		assertThat(response.phoneNumber()).isEqualTo("010-****-5678");
	}
}
