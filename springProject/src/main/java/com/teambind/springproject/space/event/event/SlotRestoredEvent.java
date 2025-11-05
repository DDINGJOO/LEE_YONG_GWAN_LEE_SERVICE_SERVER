package com.teambind.springproject.space.event.event;

import com.teambind.springproject.message.event.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 슬롯 복구 이벤트.
 * <p>
 * 슬롯이 CANCELLED → AVAILABLE 상태로 전환될 때 발행된다.
 * 만료된 PENDING 슬롯을 자동 복구할 때도 발생한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlotRestoredEvent extends Event {
	
	private static final String TOPIC = "reservation-restored";
	private static final String EVENT_TYPE = "SlotRestored";
	
	private Long slotId;
	private Long roomId;
	private LocalDate slotDate;
	private LocalTime slotTime;
	private String restoreReason;
	private LocalDateTime occurredAt;
	
	private SlotRestoredEvent(
			Long slotId,
			Long roomId,
			LocalDate slotDate,
			LocalTime slotTime,
			String restoreReason,
			LocalDateTime occurredAt
	) {
		super(TOPIC, EVENT_TYPE);
		this.slotId = slotId;
		this.roomId = roomId;
		this.slotDate = slotDate;
		this.slotTime = slotTime;
		this.restoreReason = restoreReason;
		this.occurredAt = occurredAt;
	}

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
	
	@Override
	public String getEventTypeName() {
		return EVENT_TYPE;
	}
}
