package com.example.KW_SPACE.reservation.domain;

import com.example.KW_SPACE.classroom.domain.Classroom;
import com.example.KW_SPACE.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
		name = "reservations",
		indexes = {
			@Index(name = "idx_resv_classroom_date_status", columnList = "classroom_id, date, status"),
			@Index(name = "idx_resv_user", columnList = "user_id")
		}
)
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "classroom_id", nullable = false)
	private Classroom classroom;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private LocalDate date;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReservationStatus status = ReservationStatus.RESERVED;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	private Reservation(Classroom classroom, User user, LocalDate date, LocalTime startTime, LocalTime endTime) {
		this.classroom = classroom;
		this.user = user;
		this.date = date;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public static Reservation create(Classroom classroom, User user, LocalDate date, LocalTime startTime, LocalTime endTime) {
		if (classroom == null || user == null || date == null || startTime == null || endTime == null) {
			throw new IllegalArgumentException("예약 필수 정보는 null일 수 없습니다.");
		}
		if (!endTime.isAfter(startTime)) {
			throw new IllegalArgumentException("예약 종료 시각은 시작 시각보다 늦어야 합니다.");
		}
		return new Reservation(classroom, user, date, startTime, endTime);
	}

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	public void cancel() {
		this.status = ReservationStatus.CANCELED;
	}
}
