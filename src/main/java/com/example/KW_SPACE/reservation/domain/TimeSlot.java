package com.example.KW_SPACE.reservation.domain;

import java.time.LocalTime;
import java.util.Arrays;
import lombok.Getter;

/**
 * 예약 슬롯 정책. 운영시간 09:00~22:00.
 * 09:00~18:00 은 90분(학교 시간표) 단위, 18:00~22:00 은 60분 단위로 끊는다.
 */
@Getter
public enum TimeSlot {

	SLOT_09_00(LocalTime.of(9, 0), LocalTime.of(10, 30)),
	SLOT_10_30(LocalTime.of(10, 30), LocalTime.of(12, 0)),
	SLOT_12_00(LocalTime.of(12, 0), LocalTime.of(13, 30)),
	SLOT_13_30(LocalTime.of(13, 30), LocalTime.of(15, 0)),
	SLOT_15_00(LocalTime.of(15, 0), LocalTime.of(16, 30)),
	SLOT_16_30(LocalTime.of(16, 30), LocalTime.of(18, 0)),
	SLOT_18_00(LocalTime.of(18, 0), LocalTime.of(19, 0)),
	SLOT_19_00(LocalTime.of(19, 0), LocalTime.of(20, 0)),
	SLOT_20_00(LocalTime.of(20, 0), LocalTime.of(21, 0)),
	SLOT_21_00(LocalTime.of(21, 0), LocalTime.of(22, 0));

	private final LocalTime startTime;
	private final LocalTime endTime;

	TimeSlot(LocalTime startTime, LocalTime endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public boolean overlaps(LocalTime start, LocalTime end) {
		return start.isBefore(this.endTime) && end.isAfter(this.startTime);
	}

	/** start~end 가 정의된 슬롯 하나의 경계와 정확히 일치하면 true. */
	public static boolean isValidSlot(LocalTime start, LocalTime end) {
		return Arrays.stream(values())
				.anyMatch(slot -> slot.startTime.equals(start) && slot.endTime.equals(end));
	}
}
