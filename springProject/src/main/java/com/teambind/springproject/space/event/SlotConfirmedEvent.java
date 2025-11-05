package com.teambind.springproject.space.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 슬롯 예약 확정 이벤트.
 * <p>
 * 슬롯이 PENDING → RESERVED 상태로 전환될 때 발행된다.
 * 결제 완료 후 발생한다.
 */
public record SlotConfirmedEvent(
		Long slotId,
		Long roomId,
		LocalDate slotDate,
		LocalTime slotTime,
		Long reservationId,
		LocalDateTime occurredAt
) {
	public static SlotConfirmedEvent of(
			Long slotId,
			Long roomId,
			LocalDate slotDate,
			LocalTime slotTime,
			Long reservationId
	) {
		return new SlotConfirmedEvent(
				slotId,
				roomId,
				slotDate,
				slotTime,
				reservationId,
				LocalDateTime.now()
		);
	}
}
