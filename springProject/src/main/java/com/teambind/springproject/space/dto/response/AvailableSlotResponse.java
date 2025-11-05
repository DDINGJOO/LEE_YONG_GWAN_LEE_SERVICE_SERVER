package com.teambind.springproject.space.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 예약 가능한 슬롯 조회 응답 DTO.
 */
public record AvailableSlotResponse(
		Long slotId,
		Long roomId,
		LocalDate slotDate,
		LocalTime slotTime
) {
}
