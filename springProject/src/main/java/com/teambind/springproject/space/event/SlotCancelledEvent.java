package com.teambind.springproject.space.event;

import com.teambind.springproject.message.event.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 슬롯 취소 이벤트.
 * <p>
 * 슬롯이 PENDING/RESERVED → CANCELLED 상태로 전환될 때 발행된다.
 * 결제 실패 또는 사용자 취소 시 발생한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlotCancelledEvent extends Event {
	
	private static final String TOPIC = "reservation-cancelled";
	private static final String EVENT_TYPE = "SlotCancelled";
	
	private Long slotId;
	private Long roomId;
	private LocalDate slotDate;
	private LocalTime slotTime;
	private Long reservationId;
	private String cancelReason;
	private LocalDateTime occurredAt;
	
	private SlotCancelledEvent(
			Long slotId,
			Long roomId,
			LocalDate slotDate,
			LocalTime slotTime,
			Long reservationId,
			String cancelReason,
			LocalDateTime occurredAt
	) {
		super(TOPIC, EVENT_TYPE);
		this.slotId = slotId;
		this.roomId = roomId;
		this.slotDate = slotDate;
		this.slotTime = slotTime;
		this.reservationId = reservationId;
		this.cancelReason = cancelReason;
		this.occurredAt = occurredAt;
	}

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
	
	@Override
	public String getEventTypeName() {
		return EVENT_TYPE;
	}
}
