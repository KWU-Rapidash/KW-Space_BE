package com.example.KW_SPACE.reservation.service;

import com.example.KW_SPACE.classroom.domain.Classroom;
import com.example.KW_SPACE.classroom.domain.ClassroomRepository;
import com.example.KW_SPACE.reservation.domain.Reservation;
import com.example.KW_SPACE.reservation.domain.ReservationRepository;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.example.KW_SPACE.reservation.domain.TimeSlot;
import com.example.KW_SPACE.reservation.dto.ClassroomAvailabilityResponse;
import com.example.KW_SPACE.reservation.dto.TimeSlotResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
		// getClassroom().getId()는 LAZY 프록시의 식별자 접근이라 추가 쿼리를 유발하지 않는다(외래키 값 보유).
		Map<Long, List<Reservation>> reservationsByClassroom = reservationRepository
				.findByClassroomIdInAndDateAndStatus(classroomIds, date, ReservationStatus.RESERVED).stream()
				.collect(Collectors.groupingBy(reservation -> reservation.getClassroom().getId()));

		// today/now는 자정 경계에서 어긋나지 않도록 단일 스냅샷에서 파생한다.
		LocalDateTime nowDateTime = LocalDateTime.now(clock);
		LocalDate today = nowDateTime.toLocalDate();
		LocalTime now = nowDateTime.toLocalTime();

		return classrooms.stream()
				.map(classroom -> ClassroomAvailabilityResponse.of(
						classroom,
						toTimeSlots(date, today, now,
								reservationsByClassroom.getOrDefault(classroom.getId(), List.of()))))
				.toList();
	}

	private List<TimeSlotResponse> toTimeSlots(
			LocalDate date, LocalDate today, LocalTime now, List<Reservation> reservations) {
		return Arrays.stream(TimeSlot.values())
				.map(slot -> TimeSlotResponse.of(slot, isSlotAvailable(slot, date, today, now, reservations)))
				.toList();
	}

	/** 지난 날짜·지난 슬롯이 아니고 예약과 겹치지 않으면 예약 가능. */
	private boolean isSlotAvailable(
			TimeSlot slot, LocalDate date, LocalDate today, LocalTime now, List<Reservation> reservations) {
		if (date.isBefore(today)) {
			return false;
		}
		if (date.isEqual(today) && slot.getStartTime().isBefore(now)) {
			return false;
		}
		return reservations.stream()
				.noneMatch(reservation -> slot.overlaps(reservation.getStartTime(), reservation.getEndTime()));
	}
}
