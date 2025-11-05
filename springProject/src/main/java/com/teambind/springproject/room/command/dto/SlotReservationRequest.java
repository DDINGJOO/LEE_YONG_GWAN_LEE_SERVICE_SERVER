package com.teambind.springproject.room.command.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 슬롯 예약 요청 DTO.
 */
public record SlotReservationRequest(
		Long roomId,
		LocalDate slotDate,
		LocalTime slotTime,
		Long reservationId
) {
}
