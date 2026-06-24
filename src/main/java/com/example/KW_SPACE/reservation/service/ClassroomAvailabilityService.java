package com.example.KW_SPACE.reservation.service;

import com.example.KW_SPACE.classroom.domain.Classroom;
import com.example.KW_SPACE.classroom.domain.ClassroomRepository;
import com.example.KW_SPACE.classroom.exception.ClassroomNotFoundException;
import com.example.KW_SPACE.reservation.domain.Reservation;
import com.example.KW_SPACE.reservation.domain.ReservationRepository;
import com.example.KW_SPACE.reservation.domain.ReservationStatus;
import com.example.KW_SPACE.reservation.domain.TimeSlot;
import com.example.KW_SPACE.reservation.dto.ClassroomAvailabilityResponse;
import com.example.KW_SPACE.reservation.dto.TimeSlotResponse;
import java.time.LocalDate;
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
						hasAvailableSlot(reservationsByClassroom.getOrDefault(classroom.getId(), List.of()))))
				.toList();
	}

	public List<TimeSlotResponse> findTimes(Long classroomId, LocalDate date) {
		if (!classroomRepository.existsById(classroomId)) {
			throw new ClassroomNotFoundException(classroomId);
		}

		List<Reservation> reservations = reservationRepository
				.findByClassroomIdAndDateAndStatus(classroomId, date, ReservationStatus.RESERVED);

		return Arrays.stream(TimeSlot.values())
				.map(slot -> TimeSlotResponse.of(slot, isAvailable(slot, reservations)))
				.toList();
	}

	/** 10개 슬롯 중 예약과 겹치지 않는 슬롯이 하나라도 있으면 예약 가능. */
	private boolean hasAvailableSlot(List<Reservation> reservations) {
		return Arrays.stream(TimeSlot.values()).anyMatch(slot -> isAvailable(slot, reservations));
	}

	/** 해당 슬롯이 어떤 예약과도 겹치지 않으면 예약 가능. */
	private boolean isAvailable(TimeSlot slot, List<Reservation> reservations) {
		return reservations.stream()
				.noneMatch(reservation -> slot.overlaps(reservation.getStartTime(), reservation.getEndTime()));
	}
}
