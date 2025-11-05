package com.teambind.springproject.space.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 슬롯 취소 이벤트.
 * <p>
 * 슬롯이 PENDING/RESERVED → CANCELLED 상태로 전환될 때 발행된다.
 * 결제 실패 또는 사용자 취소 시 발생한다.
 */
public record SlotCancelledEvent(
		Long slotId,
		Long roomId,
		LocalDate slotDate,
		LocalTime slotTime,
		Long reservationId,
		String cancelReason,
		LocalDateTime occurredAt
) {
	public static SlotCancelledEvent of(
			Long slotId,
			Long roomId,
			LocalDate slotDate,
			LocalTime slotTime,
			Long reservationId,
			String cancelReason
	) {
		return new SlotCancelledEvent(
				slotId,
				roomId,
				slotDate,
				slotTime,
				reservationId,
				cancelReason,
				LocalDateTime.now()
		);
	}
}
