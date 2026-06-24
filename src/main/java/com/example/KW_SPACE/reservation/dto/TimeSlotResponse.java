package com.example.KW_SPACE.reservation.dto;

import com.example.KW_SPACE.reservation.domain.TimeSlot;
import java.time.format.DateTimeFormatter;

public record TimeSlotResponse(String time, boolean available) {

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	public static TimeSlotResponse of(TimeSlot slot, boolean available) {
		String time = slot.getStartTime().format(TIME_FORMAT) + "~" + slot.getEndTime().format(TIME_FORMAT);
		return new TimeSlotResponse(time, available);
	}
}
