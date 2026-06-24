package com.example.KW_SPACE.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class TimeSlotTest {

	@Test
	void definesTenSlotsFromNineToTwentyTwo() {
		assertThat(TimeSlot.values()).hasSize(10);
		assertThat(TimeSlot.SLOT_09_00.getStartTime()).isEqualTo(LocalTime.of(9, 0));
		assertThat(TimeSlot.SLOT_21_00.getEndTime()).isEqualTo(LocalTime.of(22, 0));
	}

	@Test
	void adjacentReservationDoesNotOverlapSlot() {
		// 09:00~10:30 슬롯: 10:30 시작 예약은 경계 접촉이라 겹치지 않음
		assertThat(TimeSlot.SLOT_09_00.overlaps(LocalTime.of(10, 30), LocalTime.of(12, 0))).isFalse();
		// 18:00~19:00 슬롯: 17:30~18:00 예약은 90분/60분 전환 경계라 겹치지 않음
		assertThat(TimeSlot.SLOT_18_00.overlaps(LocalTime.of(17, 30), LocalTime.of(18, 0))).isFalse();
	}

	@Test
	void reservationCrossingBoundaryOverlapsBothSlots() {
		// 09:30~11:00 예약은 09:00~10:30, 10:30~12:00 두 슬롯과 모두 겹침
		assertThat(TimeSlot.SLOT_09_00.overlaps(LocalTime.of(9, 30), LocalTime.of(11, 0))).isTrue();
		assertThat(TimeSlot.SLOT_10_30.overlaps(LocalTime.of(9, 30), LocalTime.of(11, 0))).isTrue();
	}

	@Test
	void reservationInsideSlotOverlaps() {
		assertThat(TimeSlot.SLOT_09_00.overlaps(LocalTime.of(9, 15), LocalTime.of(9, 45))).isTrue();
	}
}
