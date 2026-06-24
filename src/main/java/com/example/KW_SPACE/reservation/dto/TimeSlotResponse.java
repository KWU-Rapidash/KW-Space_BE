package com.example.KW_SPACE.reservation.dto;

import com.example.KW_SPACE.reservation.domain.TimeSlot;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

public record TimeSlotResponse(
		@JsonFormat(pattern = "HH:mm") LocalTime startTime,
		@JsonFormat(pattern = "HH:mm") LocalTime endTime,
		boolean available) {

	public static TimeSlotResponse of(TimeSlot slot, boolean available) {
		return new TimeSlotResponse(slot.getStartTime(), slot.getEndTime(), available);
	}
}
