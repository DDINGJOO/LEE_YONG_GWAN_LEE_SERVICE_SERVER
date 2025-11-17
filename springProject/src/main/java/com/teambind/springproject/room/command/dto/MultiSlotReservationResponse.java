package com.teambind.springproject.room.command.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 다중 슬롯 예약 응답 DTO.
 *
 * 예약 성공 시 생성된 예약 ID와 예약된 슬롯 정보를 반환한다.
 */
public record MultiSlotReservationResponse(
		Long reservationId,
		Long roomId,
		LocalDate slotDate,
		List<LocalTime> reservedSlotTimes
) {
}