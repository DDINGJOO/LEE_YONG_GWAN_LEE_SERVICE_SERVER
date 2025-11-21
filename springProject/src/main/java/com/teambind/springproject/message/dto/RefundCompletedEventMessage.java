package com.teambind.springproject.message.dto;

import com.teambind.springproject.room.event.event.SlotRestoredEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 환불 완료 이벤트 메시지 DTO.
 * <p>
 * Kafka 메시지로 수신되며, SlotRestoredEvent로 변환되어 슬롯 복구 처리된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefundCompletedEventMessage {

	private String topic;
	private String eventType;
	private String reservationId;
	private String occurredAt;

	/**
	 * 메시지 DTO를 SlotRestoredEvent로 변환한다.
	 * 환불 완료 시 해당 예약의 슬롯을 복구한다.
	 */
	public SlotRestoredEvent toEvent() {
		return SlotRestoredEvent.of(
				reservationId,
				"REFUND"
		);
	}
}