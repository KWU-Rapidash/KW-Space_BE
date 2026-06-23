package com.example.KW_SPACE.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class UserRepositoryTest {

	private final UserRepository userRepository;

	@Autowired
	UserRepositoryTest(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Test
	void findsUserByKlasId() {
		User user = userRepository.save(User.create("2025404000", "이효원", "010-1234-5678", "encoded-password"));

		assertThat(userRepository.findByKlasId(user.getKlasId()))
				.isPresent()
				.get()
				.extracting(User::getName)
				.isEqualTo("이효원");
	}

	@Test
	void rejectsDuplicateKlasId() {
		userRepository.saveAndFlush(User.create("2025404000", "이효원", null, "encoded-password"));

		User duplicate = User.create("2025404000", "홍길동", null, "another-encoded-password");

		assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void checksPhoneNumberExistence() {
		userRepository.saveAndFlush(User.create("2025404000", "이효원", "010-1234-5678", "encoded-password"));

		assertThat(userRepository.existsByPhoneNumber("010-1234-5678")).isTrue();
		assertThat(userRepository.existsByPhoneNumber("010-0000-0000")).isFalse();
	}

	@Test
	void setsTimestampsOnPersistAndUpdate() {
		User user = userRepository.saveAndFlush(User.create("2025404000", "이효원", null, "encoded-password"));

		assertThat(user.getCreatedAt()).isNotNull();
		assertThat(user.getUpdatedAt()).isNotNull();

		user.changePhoneNumber("010-1234-5678");
		User updatedUser = userRepository.saveAndFlush(user);

		assertThat(updatedUser.getUpdatedAt()).isNotNull();
	}
}
