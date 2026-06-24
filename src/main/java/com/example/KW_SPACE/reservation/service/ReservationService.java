package com.example.KW_SPACE.reservation.service;

import com.example.KW_SPACE.classroom.domain.Classroom;
import com.example.KW_SPACE.classroom.domain.ClassroomRepository;
import com.example.KW_SPACE.reservation.domain.Reservation;
import com.example.KW_SPACE.reservation.domain.ReservationRepository;
import com.example.KW_SPACE.reservation.domain.TimeSlot;
import com.example.KW_SPACE.reservation.dto.ReservationCreateRequest;
import com.example.KW_SPACE.reservation.dto.ReservationCreateResponse;
import com.example.KW_SPACE.reservation.exception.ReservationErrorCode;
import com.example.KW_SPACE.reservation.exception.ReservationException;
import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

	private final ClassroomRepository classroomRepository;
	private final ReservationRepository reservationRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	public ReservationCreateResponse create(Long userId, ReservationCreateRequest request) {
		validateSlot(request);
		validateNotPast(request);

		// 강의실 행을 비관적 쓰기 락으로 잡아 동일 강의실 동시 예약을 직렬화한다.
		Classroom classroom = classroomRepository.findByCodeForUpdate(request.classroomId())
				.orElseThrow(() -> new ReservationException(ReservationErrorCode.CLASSROOM_NOT_FOUND));

		if (reservationRepository.existsOverlappingReservation(
				classroom, request.date(), request.startTime(), request.endTime())) {
			throw new ReservationException(ReservationErrorCode.RESERVATION_CONFLICT);
		}

		User user = userRepository.getReferenceById(userId);
		Reservation reservation = reservationRepository.save(
				Reservation.create(classroom, user, request.date(), request.startTime(), request.endTime()));

		return ReservationCreateResponse.of(reservation);
	}

	private void validateSlot(ReservationCreateRequest request) {
		if (!TimeSlot.isValidSlot(request.startTime(), request.endTime())) {
			throw new ReservationException(ReservationErrorCode.INVALID_SLOT);
		}
	}

	private void validateNotPast(ReservationCreateRequest request) {
		LocalDateTime now = LocalDateTime.now(clock);
		boolean past = request.date().isBefore(now.toLocalDate())
				|| (request.date().isEqual(now.toLocalDate()) && request.startTime().isBefore(now.toLocalTime()));
		if (past) {
			throw new ReservationException(ReservationErrorCode.PAST_TIME);
		}
	}
}
