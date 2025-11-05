package com.teambind.springproject.space.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 슬롯 예약 대기 이벤트.
 * <p>
 * 슬롯이 AVAILABLE → PENDING 상태로 전환될 때 발행된다.
 */
public record SlotReservedEvent(
		Long slotId,
		Long roomId,
		LocalDate slotDate,
		LocalTime slotTime,
		Long reservationId,
		LocalDateTime occurredAt
) {
	public static SlotReservedEvent of(
			Long slotId,
			Long roomId,
			LocalDate slotDate,
			LocalTime slotTime,
			Long reservationId
	) {
		return new SlotReservedEvent(
				slotId,
				roomId,
				slotDate,
				slotTime,
				reservationId,
				LocalDateTime.now()
		);
	}
}
