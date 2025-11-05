package com.teambind.springproject.space.event.event;

import com.teambind.springproject.message.event.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 슬롯 예약 대기 이벤트.
 * <p>
 * 슬롯이 AVAILABLE → PENDING 상태로 전환될 때 발행된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlotReservedEvent extends Event {
	
	private static final String TOPIC = "reservation-reserved";
	private static final String EVENT_TYPE = "SlotReserved";
	
	private Long slotId;
	private Long roomId;
	private LocalDate slotDate;
	private LocalTime slotTime;
	private Long reservationId;
	private LocalDateTime occurredAt;
	
	private SlotReservedEvent(
			Long slotId,
			Long roomId,
			LocalDate slotDate,
			LocalTime slotTime,
			Long reservationId,
			LocalDateTime occurredAt
	) {
		super(TOPIC, EVENT_TYPE);
		this.slotId = slotId;
		this.roomId = roomId;
		this.slotDate = slotDate;
		this.slotTime = slotTime;
		this.reservationId = reservationId;
		this.occurredAt = occurredAt;
	}

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
	
	@Override
	public String getEventTypeName() {
		return EVENT_TYPE;
	}
}
