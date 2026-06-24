package com.example.KW_SPACE.classroom.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class ClassroomRepositoryTest {

	private final ClassroomRepository classroomRepository;

	@Autowired
	ClassroomRepositoryTest(ClassroomRepository classroomRepository) {
		this.classroomRepository = classroomRepository;
	}

	@Test
	void savesAndFindsClassroomsByFloor() {
		classroomRepository.save(Classroom.create("saebit-402", 4, "402"));
		classroomRepository.save(Classroom.create("saebit-401", 4, "401"));
		classroomRepository.save(Classroom.create("saebit-501", 5, "501"));

		assertThat(classroomRepository.findByFloorOrderByRoomNumberAsc(4))
				.hasSize(2)
				.extracting(Classroom::getRoomNumber)
				.containsExactly("401", "402");
	}

	@Test
	void rejectsDuplicateFloorAndRoomNumber() {
		classroomRepository.saveAndFlush(Classroom.create("saebit-401", 4, "401"));

		// 코드는 다르지만 floor+room이 같아 uk_classrooms_floor_room 위반을 확인한다.
		Classroom duplicate = Classroom.create("saebit-401-dup", 4, "401");

		assertThatThrownBy(() -> classroomRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsDuplicateCode() {
		classroomRepository.saveAndFlush(Classroom.create("saebit-401", 4, "401"));

		// floor+room은 다르지만 code가 같아 uk_classrooms_code 위반을 확인한다.
		Classroom duplicate = Classroom.create("saebit-401", 5, "501");

		assertThatThrownBy(() -> classroomRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findsByCodeForUpdate() {
		classroomRepository.saveAndFlush(Classroom.create("saebit-401", 4, "401"));

		assertThat(classroomRepository.findByCodeForUpdate("saebit-401"))
				.isPresent()
				.get()
				.extracting(Classroom::getRoomNumber)
				.isEqualTo("401");
	}
}
