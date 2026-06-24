package com.example.KW_SPACE.reservation.domain;

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
		classroomRepository.save(Classroom.create(4, "401"));
		classroomRepository.save(Classroom.create(4, "402"));
		classroomRepository.save(Classroom.create(5, "501"));

		assertThat(classroomRepository.findByFloor(4))
				.hasSize(2)
				.extracting(Classroom::getRoomNumber)
				.containsExactlyInAnyOrder("401", "402");
	}

	@Test
	void rejectsDuplicateFloorAndRoomNumber() {
		classroomRepository.saveAndFlush(Classroom.create(4, "401"));

		Classroom duplicate = Classroom.create(4, "401");

		assertThatThrownBy(() -> classroomRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findsByIdForUpdate() {
		Classroom classroom = classroomRepository.saveAndFlush(Classroom.create(4, "401"));

		assertThat(classroomRepository.findByIdForUpdate(classroom.getId()))
				.isPresent()
				.get()
				.extracting(Classroom::getRoomNumber)
				.isEqualTo("401");
	}
}
