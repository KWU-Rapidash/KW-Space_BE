package com.example.KW_SPACE.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.KW_SPACE.classroom.domain.Classroom;
import com.example.KW_SPACE.classroom.domain.ClassroomRepository;
import com.example.KW_SPACE.classroom.exception.ClassroomNotFoundException;
import com.example.KW_SPACE.reservation.domain.Reservation;
import com.example.KW_SPACE.reservation.domain.ReservationRepository;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.example.KW_SPACE.reservation.dto.ClassroomAvailabilityResponse;
import com.example.KW_SPACE.reservation.dto.TimeSlotResponse;
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

	@Test
	void returnsAllTenSlotsWithAvailabilityForClassroom() {
		when(classroomRepository.existsById(1L)).thenReturn(true);
		when(reservationRepository.findByClassroomIdAndDateAndStatus(1L, DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of(reservation(classroom(1L, "401"), LocalTime.of(9, 0), LocalTime.of(10, 30))));

		List<TimeSlotResponse> result = classroomAvailabilityService.findTimes(1L, DATE);

		assertThat(result).hasSize(10);
		assertThat(result.get(0).available()).isFalse(); // 09:00~10:30 예약됨
		assertThat(result.get(1).available()).isTrue();  // 10:30~12:00 가능
	}

	@Test
	void blocksOnlySlotsOverlappingReservation() {
		when(classroomRepository.existsById(1L)).thenReturn(true);
		// 10:30~13:30 예약: slot[1](10:30~12:00), slot[2](12:00~13:30) 두 슬롯만 차단
		when(reservationRepository.findByClassroomIdAndDateAndStatus(1L, DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of(reservation(classroom(1L, "401"), LocalTime.of(10, 30), LocalTime.of(13, 30))));

		List<TimeSlotResponse> result = classroomAvailabilityService.findTimes(1L, DATE);

		assertThat(result.get(0).available()).isTrue();  // 09:00~10:30 경계 인접, 비차단
		assertThat(result.get(1).available()).isFalse(); // 10:30~12:00
		assertThat(result.get(2).available()).isFalse(); // 12:00~13:30
		assertThat(result.get(3).available()).isTrue();  // 13:30~15:00 경계 인접, 비차단
	}

	@Test
	void blocksSlotsAcrossNinetyAndSixtyMinutePolicyBoundary() {
		when(classroomRepository.existsById(1L)).thenReturn(true);
		// 16:30~19:00 예약: slot[5](16:30~18:00, 90분), slot[6](18:00~19:00, 60분) 모두 차단
		when(reservationRepository.findByClassroomIdAndDateAndStatus(1L, DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of(reservation(classroom(1L, "401"), LocalTime.of(16, 30), LocalTime.of(19, 0))));

		List<TimeSlotResponse> result = classroomAvailabilityService.findTimes(1L, DATE);

		assertThat(result.get(5).available()).isFalse(); // 16:30~18:00
		assertThat(result.get(6).available()).isFalse(); // 18:00~19:00
		assertThat(result.get(7).available()).isTrue();  // 19:00~20:00
	}

	@Test
	void throwsWhenClassroomDoesNotExist() {
		when(classroomRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> classroomAvailabilityService.findTimes(99L, DATE))
				.isInstanceOf(ClassroomNotFoundException.class);
	}
}
