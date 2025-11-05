package com.teambind.springproject.space.event.event;

import com.teambind.springproject.message.event.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 슬롯 예약 확정 이벤트.
 * <p>
 * 해당 예약의 모든 슬롯이 PENDING → RESERVED 상태로 전환될 때 발행된다.
 * 결제 완료 후 발생한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlotConfirmedEvent extends Event {

	private static final String TOPIC = "reservation-confirmed";
	private static final String EVENT_TYPE = "SlotConfirmed";

	private Long reservationId;
	private LocalDateTime occurredAt;

	private SlotConfirmedEvent(
			Long reservationId,
			LocalDateTime occurredAt
	) {
		super(TOPIC, EVENT_TYPE);
		this.reservationId = reservationId;
		this.occurredAt = occurredAt;
	}

	public static SlotConfirmedEvent of(Long reservationId) {
		return new SlotConfirmedEvent(
				reservationId,
				LocalDateTime.now()
		);
	}
	
	@Override
	public String getEventTypeName() {
		return EVENT_TYPE;
	}
}
