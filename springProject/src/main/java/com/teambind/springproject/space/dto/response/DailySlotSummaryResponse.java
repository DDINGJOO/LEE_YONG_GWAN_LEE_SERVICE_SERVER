package com.teambind.springproject.space.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * 일별 슬롯 요약 응답 DTO.
 */
public record DailySlotSummaryResponse(
		Long roomId,
		LocalDate date,
		int totalSlots,
		int availableSlots,
		int reservedSlots,
		List<AvailableSlotResponse> availableSlotDetails
) {
}
