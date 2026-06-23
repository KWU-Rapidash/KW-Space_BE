package com.example.KW_SPACE.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ReservationRepositoryTest {

	private static final LocalDate DATE = LocalDate.of(2024, 4, 1);

	private final ReservationRepository reservationRepository;
	private final ClassroomRepository classroomRepository;
	private final UserRepository userRepository;

	private Classroom classroom;
	private User user;

	@Autowired
	ReservationRepositoryTest(ReservationRepository reservationRepository,
			ClassroomRepository classroomRepository, UserRepository userRepository) {
		this.reservationRepository = reservationRepository;
		this.classroomRepository = classroomRepository;
		this.userRepository = userRepository;
	}

	@BeforeEach
	void setUp() {
		classroom = classroomRepository.save(Classroom.create(4, "401"));
		user = userRepository.save(User.create("2025404000", "이효원", "010-1234-5678", "encoded-password"));
	}

	private void reserve(LocalTime start, LocalTime end) {
		reservationRepository.saveAndFlush(Reservation.create(classroom, user, DATE, start, end));
	}

	private void reserveCanceled(LocalTime start, LocalTime end) {
		Reservation reservation = Reservation.create(classroom, user, DATE, start, end);
		reservation.cancel();
		reservationRepository.saveAndFlush(reservation);
	}

	private boolean overlaps(LocalTime start, LocalTime end) {
		return reservationRepository.existsOverlappingReservation(classroom, DATE, start, end);
	}

	@Test
	void savesReservationWithDefaultStatusAndTimestamp() {
		Reservation saved = reservationRepository.save(
				Reservation.create(classroom, user, DATE, LocalTime.of(10, 0), LocalTime.of(11, 0)));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getStatus()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getClassroom().getId()).isEqualTo(classroom.getId());
		assertThat(saved.getUser().getId()).isEqualTo(user.getId());
	}

	@Test
	void returnsTrueWhenTimeOverlaps() {
		reserve(LocalTime.of(10, 30), LocalTime.of(11, 30));

		assertThat(overlaps(LocalTime.of(10, 0), LocalTime.of(11, 0))).isTrue();   // 앞부분 겹침
		assertThat(overlaps(LocalTime.of(11, 0), LocalTime.of(12, 0))).isTrue();   // 뒷부분 겹침
		assertThat(overlaps(LocalTime.of(10, 45), LocalTime.of(11, 15))).isTrue(); // 내부 포함
		assertThat(overlaps(LocalTime.of(10, 0), LocalTime.of(12, 0))).isTrue();   // 전체 포함
	}

	@Test
	void returnsFalseWhenAdjacentOrSeparate() {
		reserve(LocalTime.of(10, 30), LocalTime.of(11, 30));

		assertThat(overlaps(LocalTime.of(9, 0), LocalTime.of(10, 30))).isFalse();  // 끝=기존시작 (인접)
		assertThat(overlaps(LocalTime.of(11, 30), LocalTime.of(12, 30))).isFalse(); // 시작=기존끝 (인접)
		assertThat(overlaps(LocalTime.of(8, 0), LocalTime.of(9, 0))).isFalse();    // 완전 분리
	}

	@Test
	void ignoresCanceledReservations() {
		reserveCanceled(LocalTime.of(10, 30), LocalTime.of(11, 30));

		assertThat(overlaps(LocalTime.of(10, 0), LocalTime.of(11, 0))).isFalse();
	}

	@Test
	void ignoresOtherDate() {
		reserve(LocalTime.of(10, 30), LocalTime.of(11, 30));

		assertThat(reservationRepository.existsOverlappingReservation(
				classroom, LocalDate.of(2024, 4, 2), LocalTime.of(10, 0), LocalTime.of(11, 0))).isFalse();
	}

	@Test
	void ignoresOtherClassroom() {
		reserve(LocalTime.of(10, 30), LocalTime.of(11, 30));
		Classroom otherClassroom = classroomRepository.save(Classroom.create(4, "402"));

		assertThat(reservationRepository.existsOverlappingReservation(
				otherClassroom, DATE, LocalTime.of(10, 0), LocalTime.of(11, 0))).isFalse();
	}

	@Test
	void findsReservationsByUserAndStatus() {
		reserve(LocalTime.of(10, 0), LocalTime.of(11, 0));
		reserveCanceled(LocalTime.of(13, 0), LocalTime.of(14, 0));

		assertThat(reservationRepository.findByUserId(user.getId())).hasSize(2);
		assertThat(reservationRepository.findByUserIdAndStatus(user.getId(), ReservationStatus.RESERVED)).hasSize(1);
		assertThat(reservationRepository.findByClassroomIdAndDateAndStatus(
				classroom.getId(), DATE, ReservationStatus.RESERVED)).hasSize(1);
	}
}
