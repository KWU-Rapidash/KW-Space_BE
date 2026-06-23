package com.example.KW_SPACE.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.KW_SPACE.classroom.domain.Classroom;
import com.example.KW_SPACE.classroom.domain.ClassroomRepository;
import com.example.KW_SPACE.reservation.domain.Reservation;
import com.example.KW_SPACE.reservation.domain.ReservationRepository;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.example.KW_SPACE.reservation.dto.ClassroomAvailabilityResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClassroomAvailabilityServiceTest {

	private static final LocalDate DATE = LocalDate.of(2024, 4, 1);

	@Mock
	private ClassroomRepository classroomRepository;

	@Mock
	private ReservationRepository reservationRepository;

	@InjectMocks
	private ClassroomAvailabilityService classroomAvailabilityService;

	private Classroom classroom(long id, String roomNumber) {
		Classroom classroom = Classroom.create(4, roomNumber);
		ReflectionTestUtils.setField(classroom, "id", id);
		return classroom;
	}

	private Reservation reservation(Classroom classroom, LocalTime startTime, LocalTime endTime) {
		return Reservation.create(classroom, null, DATE, startTime, endTime);
	}

	@Test
	void marksClassroomAvailableWhenNoReservation() {
		when(classroomRepository.findByFloorOrderByRoomNumberAsc(4))
				.thenReturn(List.of(classroom(1L, "401")));
		when(reservationRepository.findByClassroomIdInAndDateAndStatus(List.of(1L), DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of());

		List<ClassroomAvailabilityResponse> result = classroomAvailabilityService.findAvailability(4, DATE);

		assertThat(result).singleElement()
				.extracting(ClassroomAvailabilityResponse::available)
				.isEqualTo(true);
	}

	@Test
	void marksClassroomUnavailableWhenFullyBooked() {
		Classroom classroom = classroom(1L, "401");
		when(classroomRepository.findByFloorOrderByRoomNumberAsc(4)).thenReturn(List.of(classroom));
		when(reservationRepository.findByClassroomIdInAndDateAndStatus(List.of(1L), DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of(reservation(classroom, LocalTime.of(9, 0), LocalTime.of(22, 0))));

		List<ClassroomAvailabilityResponse> result = classroomAvailabilityService.findAvailability(4, DATE);

		assertThat(result).singleElement()
				.extracting(ClassroomAvailabilityResponse::available)
				.isEqualTo(false);
	}

	@Test
	void marksClassroomAvailableWhenOnlySomeSlotsBooked() {
		Classroom classroom = classroom(1L, "401");
		when(classroomRepository.findByFloorOrderByRoomNumberAsc(4)).thenReturn(List.of(classroom));
		when(reservationRepository.findByClassroomIdInAndDateAndStatus(List.of(1L), DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of(reservation(classroom, LocalTime.of(9, 0), LocalTime.of(10, 30))));

		List<ClassroomAvailabilityResponse> result = classroomAvailabilityService.findAvailability(4, DATE);

		assertThat(result).singleElement()
				.extracting(ClassroomAvailabilityResponse::available)
				.isEqualTo(true);
	}

	@Test
	void computesAvailabilityPerClassroomInOrder() {
		Classroom available = classroom(1L, "401");
		Classroom fullyBooked = classroom(2L, "402");
		when(classroomRepository.findByFloorOrderByRoomNumberAsc(4))
				.thenReturn(List.of(available, fullyBooked));
		when(reservationRepository.findByClassroomIdInAndDateAndStatus(List.of(1L, 2L), DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of(reservation(fullyBooked, LocalTime.of(9, 0), LocalTime.of(22, 0))));

		List<ClassroomAvailabilityResponse> result = classroomAvailabilityService.findAvailability(4, DATE);

		assertThat(result).extracting(ClassroomAvailabilityResponse::roomNumber).containsExactly("401", "402");
		assertThat(result).extracting(ClassroomAvailabilityResponse::available).containsExactly(true, false);
	}
}
