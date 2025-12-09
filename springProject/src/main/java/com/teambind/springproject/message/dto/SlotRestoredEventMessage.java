package com.teambind.springproject.message.dto;

import com.teambind.springproject.room.event.event.SlotRestoredEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 슬롯 복구 이벤트 메시지 DTO.
 * <p>
 * Kafka 메시지로 전송될 때 사용되며, 모든 ID 필드는 String으로 직렬화된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SlotRestoredEventMessage {
	
	private String topic;
	private String eventType;
	private String reservationId;
	private String restoreReason;
	private LocalDateTime occurredAt;
	
	/**
	 * SlotRestoredEvent로부터 메시지 DTO를 생성한다.
	 */
	public static SlotRestoredEventMessage from(SlotRestoredEvent event) {
		return new SlotRestoredEventMessage(
				event.getTopic(),
				event.getEventType(),
				event.getReservationId(),
				event.getRestoreReason(),
				event.getOccurredAt()
		);
	}
	
	/**
	 * 메시지 DTO를 SlotRestoredEvent로 변환한다.
	 */
	public SlotRestoredEvent toEvent() {
		return SlotRestoredEvent.of(
				reservationId,
				restoreReason
		);
	}
}
