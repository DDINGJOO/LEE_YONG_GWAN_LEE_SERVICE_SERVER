package com.teambind.springproject.space.event.event;

import com.teambind.springproject.message.event.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 슬롯 예약 대기 이벤트.
 * <p>
 * 여러 슬롯이 AVAILABLE → PENDING 상태로 전환될 때 발행된다.
 * 하나의 예약(reservationId)에 대해 여러 시간대의 슬롯을 예약할 수 있다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlotReservedEvent extends Event {

	private static final String TOPIC = "reservation-reserved";
	private static final String EVENT_TYPE = "SlotReserved";

	private Long roomId;
	private LocalDate slotDate;
	private List<LocalTime> startTimes;
	private Long reservationId;
	private LocalDateTime occurredAt;

	private SlotReservedEvent(
			Long roomId,
			LocalDate slotDate,
			List<LocalTime> startTimes,
			Long reservationId,
			LocalDateTime occurredAt
	) {
		super(TOPIC, EVENT_TYPE);
		this.roomId = roomId;
		this.slotDate = slotDate;
		this.startTimes = startTimes;
		this.reservationId = reservationId;
		this.occurredAt = occurredAt;
	}

	public static SlotReservedEvent of(
			Long roomId,
			LocalDate slotDate,
			List<LocalTime> startTimes,
			Long reservationId
	) {
		return new SlotReservedEvent(
				roomId,
				slotDate,
				startTimes,
				reservationId,
				LocalDateTime.now()
		);
	}
	
	@Override
	public String getEventTypeName() {
		return EVENT_TYPE;
	}
}
