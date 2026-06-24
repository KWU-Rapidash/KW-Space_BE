package com.example.KW_SPACE.reservation.service;

import com.example.KW_SPACE.classroom.domain.Classroom;
import com.example.KW_SPACE.classroom.domain.ClassroomRepository;
import com.example.KW_SPACE.reservation.domain.Reservation;
import com.example.KW_SPACE.reservation.domain.ReservationRepository;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.example.KW_SPACE.reservation.domain.TimeSlot;
import com.example.KW_SPACE.reservation.dto.ClassroomAvailabilityResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomAvailabilityService {

	private final ClassroomRepository classroomRepository;
	private final ReservationRepository reservationRepository;
	private final Clock clock;

	public List<ClassroomAvailabilityResponse> findAvailability(int floor, LocalDate date) {
		List<Classroom> classrooms = classroomRepository.findByFloorOrderByRoomNumberAsc(floor);
		if (classrooms.isEmpty()) {
			return List.of();
		}

		List<Long> classroomIds = classrooms.stream().map(Classroom::getId).toList();
		Map<Long, List<Reservation>> reservationsByClassroom = reservationRepository
				.findByClassroomIdInAndDateAndStatus(classroomIds, date, ReservationStatus.RESERVED).stream()
				.collect(Collectors.groupingBy(reservation -> reservation.getClassroom().getId()));

		return classrooms.stream()
				.map(classroom -> ClassroomAvailabilityResponse.of(
						classroom,
						hasAvailableSlot(date, reservationsByClassroom.getOrDefault(classroom.getId(), List.of()))))
				.toList();
	}

	/** 10개 슬롯 중 예약과 겹치지 않는 슬롯이 하나라도 있으면 예약 가능. */
	private boolean hasAvailableSlot(LocalDate date, List<Reservation> reservations) {
		LocalDate today = LocalDate.now(clock);
		if (date.isBefore(today)) {
			return false;
		}

		LocalTime now = LocalTime.now(clock);
		return Arrays.stream(TimeSlot.values())
				.filter(slot -> !date.isEqual(today) || !slot.getStartTime().isBefore(now))
				.anyMatch(slot -> reservations.stream()
						.noneMatch(reservation -> slot.overlaps(reservation.getStartTime(), reservation.getEndTime())));
	}
}
