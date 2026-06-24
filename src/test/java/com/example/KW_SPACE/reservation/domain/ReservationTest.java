package com.example.KW_SPACE.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.KW_SPACE.classroom.domain.Classroom;
import com.example.KW_SPACE.user.domain.User;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ReservationTest {

	private static final LocalDate DATE = LocalDate.of(2024, 4, 1);

	private final Classroom classroom = Classroom.create("saebit-401", 4, "401");
	private final User user = User.create("2025404000", "이효원", null, "encoded-password");

	@Test
	void createsReservationWithReservedStatus() {
		Reservation reservation = Reservation.create(classroom, user, DATE, LocalTime.of(10, 0), LocalTime.of(11, 0));

		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(reservation.getStartTime()).isEqualTo(LocalTime.of(10, 0));
		assertThat(reservation.getEndTime()).isEqualTo(LocalTime.of(11, 0));
	}

	@Test
	void rejectsEndTimeEqualToStartTime() {
		assertThatThrownBy(() ->
				Reservation.create(classroom, user, DATE, LocalTime.of(11, 0), LocalTime.of(11, 0)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsEndTimeBeforeStartTime() {
		assertThatThrownBy(() ->
				Reservation.create(classroom, user, DATE, LocalTime.of(12, 0), LocalTime.of(11, 0)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsNullRequiredArguments() {
		LocalTime startTime = LocalTime.of(10, 0);
		LocalTime endTime = LocalTime.of(11, 0);

		assertThatThrownBy(() -> Reservation.create(null, user, DATE, startTime, endTime))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Reservation.create(classroom, null, DATE, startTime, endTime))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Reservation.create(classroom, user, null, startTime, endTime))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Reservation.create(classroom, user, DATE, null, endTime))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Reservation.create(classroom, user, DATE, startTime, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void cancelChangesStatusToCanceled() {
		Reservation reservation = Reservation.create(classroom, user, DATE, LocalTime.of(10, 0), LocalTime.of(11, 0));

		reservation.cancel();

		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
	}
}
