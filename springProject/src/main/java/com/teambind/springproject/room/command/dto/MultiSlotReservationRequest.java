package com.teambind.springproject.room.command.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 다중 슬롯 예약 요청 DTO.
 *
 * 특정 날짜의 여러 시간 슬롯을 한 번에 예약 대기 상태로 변경한다.
 * Pessimistic Lock을 통해 동시성 문제를 해결하고, 예약 ID를 자동 생성한다.
 */
public record MultiSlotReservationRequest(
		Long roomId,
		LocalDate slotDate,
		List<LocalTime> slotTimes
) {
	public MultiSlotReservationRequest {
		if (roomId == null) {
			throw new IllegalArgumentException("roomId must not be null");
		}
		if (slotDate == null) {
			throw new IllegalArgumentException("slotDate must not be null");
		}
		if (slotTimes == null || slotTimes.isEmpty()) {
			throw new IllegalArgumentException("slotTimes must not be null or empty");
		}
	}
}