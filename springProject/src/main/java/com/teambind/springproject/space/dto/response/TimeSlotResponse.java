package com.teambind.springproject.space.dto.response;

import com.teambind.springproject.space.entity.enums.SlotStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 타임 슬롯 조회 응답 DTO.
 */
public record TimeSlotResponse(
		Long slotId,
		Long roomId,
		LocalDate slotDate,
		LocalTime slotTime,
		SlotStatus status,
		Long reservationId
) {
}
