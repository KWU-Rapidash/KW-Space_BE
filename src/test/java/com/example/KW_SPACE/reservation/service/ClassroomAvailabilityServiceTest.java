package com.example.KW_SPACE.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.KW_SPACE.classroom.domain.Classroom;
import com.example.KW_SPACE.classroom.domain.ClassroomRepository;
import com.example.KW_SPACE.reservation.domain.Reservation;
import com.example.KW_SPACE.reservation.domain.ReservationRepository;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.example.KW_SPACE.reservation.dto.ClassroomAvailabilityResponse;
import com.example.KW_SPACE.reservation.dto.TimeSlotResponse;
import com.example.KW_SPACE.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClassroomAvailabilityServiceTest {

	private static final LocalDate DATE = LocalDate.of(2024, 4, 1);
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	@Mock
	private ClassroomRepository classroomRepository;

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private Clock clock;

	@InjectMocks
	private ClassroomAvailabilityService classroomAvailabilityService;

	@BeforeEach
	void setUp() {
		setNow(LocalDate.of(2024, 3, 31), LocalTime.of(8, 0));
	}

	private Classroom classroom(long id, String roomNumber) {
		Classroom classroom = Classroom.create(4, roomNumber);
		ReflectionTestUtils.setField(classroom, "id", id);
		return classroom;
	}

	private Reservation reservation(Classroom classroom, LocalTime startTime, LocalTime endTime) {
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		return Reservation.create(classroom, user, DATE, startTime, endTime);
	}

	private void setNow(LocalDate date, LocalTime time) {
		Instant instant = date.atTime(time).atZone(ZONE).toInstant();
		when(clock.instant()).thenReturn(instant);
		when(clock.getZone()).thenReturn(ZONE);
	}

	private List<TimeSlotResponse> times(ClassroomAvailabilityResponse response) {
		return response.times();
	}

	@Test
	void returnsAllTenSlotsAvailableWhenNoReservation() {
		when(classroomRepository.findByFloorOrderByRoomNumberAsc(4))
				.thenReturn(List.of(classroom(1L, "401")));
		when(reservationRepository.findByClassroomIdInAndDateAndStatus(List.of(1L), DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of());

		List<ClassroomAvailabilityResponse> result = classroomAvailabilityService.findAvailability(4, DATE);

		assertThat(times(result.get(0))).hasSize(10).allMatch(TimeSlotResponse::available);
	}

	@Test
	void marksAllSlotsUnavailableWhenFullyBooked() {
		Classroom classroom = classroom(1L, "401");
		when(classroomRepository.findByFloorOrderByRoomNumberAsc(4)).thenReturn(List.of(classroom));
		when(reservationRepository.findByClassroomIdInAndDateAndStatus(List.of(1L), DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of(reservation(classroom, LocalTime.of(9, 0), LocalTime.of(22, 0))));

		List<ClassroomAvailabilityResponse> result = classroomAvailabilityService.findAvailability(4, DATE);

		assertThat(times(result.get(0))).noneMatch(TimeSlotResponse::available);
	}

	@Test
	void marksOnlyOverlappingSlotsUnavailable() {
		Classroom classroom = classroom(1L, "401");
		when(classroomRepository.findByFloorOrderByRoomNumberAsc(4)).thenReturn(List.of(classroom));
		// 10:30~13:30 예약: slot[1], slot[2]만 차단, 인접 슬롯은 가능
		when(reservationRepository.findByClassroomIdInAndDateAndStatus(List.of(1L), DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of(reservation(classroom, LocalTime.of(10, 30), LocalTime.of(13, 30))));

		List<TimeSlotResponse> slots = times(classroomAvailabilityService.findAvailability(4, DATE).get(0));

		assertThat(slots.get(0).available()).isTrue();  // 09:00~10:30 인접, 가능
		assertThat(slots.get(1).available()).isFalse(); // 10:30~12:00
		assertThat(slots.get(2).available()).isFalse(); // 12:00~13:30
		assertThat(slots.get(3).available()).isTrue();  // 13:30~15:00 인접, 가능
	}

	@Test
	void marksAllSlotsUnavailableForPastDate() {
		setNow(LocalDate.of(2024, 4, 2), LocalTime.of(8, 0));
		Classroom classroom = classroom(1L, "401");
		when(classroomRepository.findByFloorOrderByRoomNumberAsc(4)).thenReturn(List.of(classroom));
		when(reservationRepository.findByClassroomIdInAndDateAndStatus(List.of(1L), DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of());

		List<ClassroomAvailabilityResponse> result = classroomAvailabilityService.findAvailability(4, DATE);

		assertThat(times(result.get(0))).noneMatch(TimeSlotResponse::available);
	}

	@Test
	void marksAlreadyStartedSlotsUnavailableForToday() {
		setNow(DATE, LocalTime.of(10, 31));
		Classroom classroom = classroom(1L, "401");
		when(classroomRepository.findByFloorOrderByRoomNumberAsc(4)).thenReturn(List.of(classroom));
		when(reservationRepository.findByClassroomIdInAndDateAndStatus(List.of(1L), DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of());

		List<TimeSlotResponse> slots = times(classroomAvailabilityService.findAvailability(4, DATE).get(0));

		assertThat(slots.get(0).available()).isFalse(); // 09:00 이미 지남
		assertThat(slots.get(1).available()).isFalse(); // 10:30 이미 지남 (10:30 < 10:31)
		assertThat(slots.get(2).available()).isTrue();  // 12:00 아직 시작 전
	}

	@Test
	void computesTimesPerClassroomInOrder() {
		Classroom available = classroom(1L, "401");
		Classroom fullyBooked = classroom(2L, "402");
		when(classroomRepository.findByFloorOrderByRoomNumberAsc(4))
				.thenReturn(List.of(available, fullyBooked));
		when(reservationRepository.findByClassroomIdInAndDateAndStatus(List.of(1L, 2L), DATE, ReservationStatus.RESERVED))
				.thenReturn(List.of(reservation(fullyBooked, LocalTime.of(9, 0), LocalTime.of(22, 0))));

		List<ClassroomAvailabilityResponse> result = classroomAvailabilityService.findAvailability(4, DATE);

		assertThat(result).extracting(ClassroomAvailabilityResponse::roomNumber).containsExactly("401", "402");
		assertThat(times(result.get(0))).anyMatch(TimeSlotResponse::available);  // 401 가용 슬롯 있음
		assertThat(times(result.get(1))).noneMatch(TimeSlotResponse::available); // 402 풀부킹
	}
}
