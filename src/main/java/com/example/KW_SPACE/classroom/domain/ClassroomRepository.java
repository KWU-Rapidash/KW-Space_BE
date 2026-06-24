package com.example.KW_SPACE.classroom.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

	List<Classroom> findByFloorOrderByRoomNumberAsc(int floor);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Classroom c where c.code = :code")
	Optional<Classroom> findByCodeForUpdate(@Param("code") String code);
}
