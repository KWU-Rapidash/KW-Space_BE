package com.example.KW_SPACE.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	void createsUserWithDefaultRoleAndTokenVersion() {
		User user = User.create("2025404000", "이효원", null, "encoded-password");

		assertThat(user.getKlasId()).isEqualTo("2025404000");
		assertThat(user.getName()).isEqualTo("이효원");
		assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
		assertThat(user.getRole()).isEqualTo(UserRole.USER);
		assertThat(user.getTokenVersion()).isZero();
	}

	@Test
	void doesNotDeclareRawPasswordOrKlasPasswordFields() {
		assertThat(Arrays.stream(User.class.getDeclaredFields()).map(Field::getName))
				.doesNotContain("password", "rawPassword", "klasPassword");
	}
}
