package com.example.KW_SPACE.reservation.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

	List<Classroom> findByFloor(int floor);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Classroom c where c.id = :id")
	Optional<Classroom> findByIdForUpdate(@Param("id") Long id);
}
