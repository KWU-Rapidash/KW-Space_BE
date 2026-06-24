package com.example.KW_SPACE.classroom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
		name = "classrooms",
		uniqueConstraints = {
			@UniqueConstraint(name = "uk_classrooms_floor_room", columnNames = {"floor", "room_number"})
		}
)
public class Classroom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private int floor;

	@Column(name = "room_number", nullable = false, length = 20)
	private String roomNumber;

	private Classroom(int floor, String roomNumber) {
		this.floor = floor;
		this.roomNumber = roomNumber;
	}

	public static Classroom create(int floor, String roomNumber) {
		if (roomNumber == null || roomNumber.isBlank() || roomNumber.length() > 20) {
			throw new IllegalArgumentException("강의실 호수는 1자 이상 20자 이하여야 합니다.");
		}
		return new Classroom(floor, roomNumber);
	}
}
