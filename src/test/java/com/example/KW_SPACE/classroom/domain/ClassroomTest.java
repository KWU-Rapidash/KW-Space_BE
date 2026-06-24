package com.example.KW_SPACE.classroom.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ClassroomTest {

	@Test
	void createsClassroomWithValidRoomNumber() {
		Classroom classroom = Classroom.create("saebit-101", 4, "12345678901234567890");

		assertThat(classroom.getCode()).isEqualTo("saebit-101");
		assertThat(classroom.getRoomNumber()).isEqualTo("12345678901234567890");
	}

	@Test
	void rejectsNullCode() {
		assertThatThrownBy(() -> Classroom.create(null, 4, "401"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsBlankCode() {
		assertThatThrownBy(() -> Classroom.create("   ", 4, "401"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsNullRoomNumber() {
		assertThatThrownBy(() -> Classroom.create("saebit-401", 4, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsBlankRoomNumber() {
		assertThatThrownBy(() -> Classroom.create("saebit-401", 4, "   "))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsRoomNumberLongerThanTwentyCharacters() {
		assertThatThrownBy(() -> Classroom.create("saebit-401", 4, "123456789012345678901"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
