package com.teambind.springproject.space.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 슬롯 가용성 조회 응답 DTO.
 */
public record SlotAvailabilityResponse(
		Long roomId,
		LocalDate slotDate,
		LocalTime slotTime,
		boolean available
) {
}
