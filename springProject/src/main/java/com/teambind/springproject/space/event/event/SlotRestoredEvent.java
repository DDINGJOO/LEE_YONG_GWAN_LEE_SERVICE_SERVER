package com.teambind.springproject.space.event.event;

import com.teambind.springproject.message.event.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 슬롯 복구 이벤트.
 * <p>
 * 해당 예약의 모든 슬롯이 CANCELLED → AVAILABLE 상태로 전환될 때 발행된다.
 * 만료된 PENDING 슬롯을 자동 복구할 때도 발생한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlotRestoredEvent extends Event {

	private static final String TOPIC = "reservation-restored";
	private static final String EVENT_TYPE = "SlotRestored";

	private Long reservationId;
	private String restoreReason;
	private LocalDateTime occurredAt;

	private SlotRestoredEvent(
			Long reservationId,
			String restoreReason,
			LocalDateTime occurredAt
	) {
		super(TOPIC, EVENT_TYPE);
		this.reservationId = reservationId;
		this.restoreReason = restoreReason;
		this.occurredAt = occurredAt;
	}

	public static SlotRestoredEvent of(
			Long reservationId,
			String restoreReason
	) {
		return new SlotRestoredEvent(
				reservationId,
				restoreReason,
				LocalDateTime.now()
		);
	}
	
	@Override
	public String getEventTypeName() {
		return EVENT_TYPE;
	}
}
