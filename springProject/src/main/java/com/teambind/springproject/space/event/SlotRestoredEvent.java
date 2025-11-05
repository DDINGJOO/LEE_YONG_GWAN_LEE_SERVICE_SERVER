package com.teambind.springproject.space.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 슬롯 복구 이벤트.
 * <p>
 * 슬롯이 CANCELLED → AVAILABLE 상태로 전환될 때 발행된다.
 * 만료된 PENDING 슬롯을 자동 복구할 때도 발생한다.
 */
public record SlotRestoredEvent(
		Long slotId,
		Long roomId,
		LocalDate slotDate,
		LocalTime slotTime,
		String restoreReason,
		LocalDateTime occurredAt
) {
	public static SlotRestoredEvent of(
			Long slotId,
			Long roomId,
			LocalDate slotDate,
			LocalTime slotTime,
			String restoreReason
	) {
		return new SlotRestoredEvent(
				slotId,
				roomId,
				slotDate,
				slotTime,
				restoreReason,
				LocalDateTime.now()
		);
	}
}
