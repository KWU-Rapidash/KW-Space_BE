package com.example.KW_SPACE.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.KW_SPACE.classroom.domain.Classroom;
import com.example.KW_SPACE.classroom.domain.ClassroomRepository;
import com.example.KW_SPACE.reservation.domain.Reservation;
import com.example.KW_SPACE.reservation.domain.ReservationRepository;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.example.KW_SPACE.reservation.dto.ReservationCreateRequest;
import com.example.KW_SPACE.reservation.dto.ReservationCreateResponse;
import com.example.KW_SPACE.reservation.exception.ReservationErrorCode;
import com.example.KW_SPACE.reservation.exception.ReservationException;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	private static final LocalDate DATE = LocalDate.of(2024, 4, 1);
	private static final LocalTime SLOT_START = LocalTime.of(9, 0);
	private static final LocalTime SLOT_END = LocalTime.of(10, 30);
	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	private static final long USER_ID = 1L;
	private static final long RESERVATION_ID = 100L;
	private static final String CLASSROOM_CODE = "saebit-101";

	@Mock
	private ClassroomRepository classroomRepository;

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private Clock clock;

	@InjectMocks
	private ReservationService reservationService;

	@BeforeEach
	void setUp() {
		setNow(LocalDate.of(2024, 3, 31), LocalTime.of(8, 0));
	}

	private void setNow(LocalDate date, LocalTime time) {
		Instant instant = date.atTime(time).atZone(ZONE).toInstant();
		lenient().when(clock.instant()).thenReturn(instant);
		lenient().when(clock.getZone()).thenReturn(ZONE);
	}

	private Classroom classroom() {
		Classroom classroom = Classroom.create(CLASSROOM_CODE, 1, "101");
		ReflectionTestUtils.setField(classroom, "id", 10L);
		return classroom;
	}

	private ReservationCreateRequest request(LocalDate date, LocalTime start, LocalTime end) {
		return new ReservationCreateRequest(CLASSROOM_CODE, date, start, end);
	}

	@Test
	void createsReservationWhenSlotIsFree() {
		Classroom classroom = classroom();
		when(classroomRepository.findByCodeForUpdate(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
		when(reservationRepository.existsOverlappingReservation(classroom, DATE, SLOT_START, SLOT_END))
				.thenReturn(false);
		User user = User.create("2025404000", "이효원", null, "encoded-password");
		when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
		when(reservationRepository.save(any(Reservation.class)))
				.thenAnswer(invocation -> {
					Reservation reservation = invocation.getArgument(0);
					ReflectionTestUtils.setField(reservation, "id", 100L);
					return reservation;
				});

		ReservationCreateResponse response = reservationService.create(USER_ID, request(DATE, SLOT_START, SLOT_END));

		assertThat(response.reservationId()).isEqualTo(100L);
		assertThat(response.classroomId()).isEqualTo(CLASSROOM_CODE);
		assertThat(response.classroomNumber()).isEqualTo("101");
		assertThat(response.startTime()).isEqualTo(SLOT_START);
		assertThat(response.endTime()).isEqualTo(SLOT_END);
	}

	@Test
	void rejectsWhenTimeIsNotAValidSlot() {
		assertThatThrownBy(() ->
				reservationService.create(USER_ID, request(DATE, LocalTime.of(9, 0), LocalTime.of(10, 0))))
				.isInstanceOf(ReservationException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.INVALID_SLOT);

		verify(reservationRepository, never()).save(any());
	}

	@Test
	void rejectsWhenDateIsPast() {
		setNow(LocalDate.of(2024, 4, 2), LocalTime.of(8, 0));

		assertThatThrownBy(() -> reservationService.create(USER_ID, request(DATE, SLOT_START, SLOT_END)))
				.isInstanceOf(ReservationException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.PAST_TIME);

		verify(reservationRepository, never()).save(any());
	}

	@Test
	void rejectsWhenSlotAlreadyStartedToday() {
		setNow(DATE, LocalTime.of(9, 1));

		assertThatThrownBy(() -> reservationService.create(USER_ID, request(DATE, SLOT_START, SLOT_END)))
				.isInstanceOf(ReservationException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.PAST_TIME);
	}

	@Test
	void rejectsWhenClassroomNotFound() {
		when(classroomRepository.findByCodeForUpdate(CLASSROOM_CODE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reservationService.create(USER_ID, request(DATE, SLOT_START, SLOT_END)))
				.isInstanceOf(ReservationException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.CLASSROOM_NOT_FOUND);

		verify(reservationRepository, never()).save(any());
	}

	@Test
	void rejectsWhenOverlappingReservationExists() {
		Classroom classroom = classroom();
		when(classroomRepository.findByCodeForUpdate(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
		when(reservationRepository.existsOverlappingReservation(eq(classroom), eq(DATE), eq(SLOT_START), eq(SLOT_END)))
				.thenReturn(true);

		assertThatThrownBy(() -> reservationService.create(USER_ID, request(DATE, SLOT_START, SLOT_END)))
				.isInstanceOf(ReservationException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_CONFLICT);

		verify(reservationRepository, never()).save(any());
	}

	private Reservation reservationOf(long ownerId, ReservationStatus status) {
		User owner = User.create("2025404000", "이효원", null, "encoded-password");
		ReflectionTestUtils.setField(owner, "id", ownerId);
		Reservation reservation = Reservation.create(classroom(), owner, DATE, SLOT_START, SLOT_END);
		ReflectionTestUtils.setField(reservation, "id", RESERVATION_ID);
		if (status == ReservationStatus.CANCELED) {
			reservation.cancel();
		}
		return reservation;
	}

	@Test
	void cancelsOwnReservation() {
		Reservation reservation = reservationOf(USER_ID, ReservationStatus.RESERVED);
		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

		reservationService.cancel(USER_ID, RESERVATION_ID);

		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
	}

	@Test
	void rejectsCancelWhenReservationNotFound() {
		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reservationService.cancel(USER_ID, RESERVATION_ID))
				.isInstanceOf(ReservationException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);
	}

	@Test
	void rejectsCancelWhenNotOwner() {
		Reservation reservation = reservationOf(2L, ReservationStatus.RESERVED);
		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

		assertThatThrownBy(() -> reservationService.cancel(USER_ID, RESERVATION_ID))
				.isInstanceOf(ReservationException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);

		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
	}

	@Test
	void rejectsCancelWhenAlreadyCanceled() {
		Reservation reservation = reservationOf(USER_ID, ReservationStatus.CANCELED);
		when(reservationRepository.findById(RESERVATION_ID)).thenReturn(Optional.of(reservation));

		assertThatThrownBy(() -> reservationService.cancel(USER_ID, RESERVATION_ID))
				.isInstanceOf(ReservationException.class)
				.extracting("errorCode").isEqualTo(ReservationErrorCode.ALREADY_CANCELED);
	}
}
