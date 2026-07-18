package com.example.KW_SPACE.reservation.domain;

import com.example.KW_SPACE.classroom.domain.Classroom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	@Query("""
			select case when count(r) > 0 then true else false end
			from Reservation r
			where r.classroom = :classroom
			  and r.date = :date
			  and r.status = com.example.KW_SPACE.reservation.domain.ReservationStatus.RESERVED
			  and r.startTime < :endTime
			  and r.endTime > :startTime
			""")
	boolean existsOverlappingReservation(
			@Param("classroom") Classroom classroom,
			@Param("date") LocalDate date,
			@Param("startTime") LocalTime startTime,
			@Param("endTime") LocalTime endTime);

	List<Reservation> findByClassroomIdAndDateAndStatus(Long classroomId, LocalDate date, ReservationStatus status);

	List<Reservation> findByClassroomIdInAndDateAndStatus(
			Collection<Long> classroomIds, LocalDate date, ReservationStatus status);

	List<Reservation> findByUserId(Long userId);

	List<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status);

	@Query("""
			select r from Reservation r
			join fetch r.classroom
			join fetch r.user
			where r.user.id = :userId
			  and (:status is null or r.status = :status)
			order by r.date desc, r.startTime desc
			""")
	List<Reservation> findUserReservations(
			@Param("userId") Long userId,
			@Param("status") ReservationStatus status);
}
