package com.teambind.springproject.message.dto;

import com.teambind.springproject.room.event.event.PaymentCompletedEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 완료 이벤트 메시지 DTO.
 * <p>
 * Kafka 메시지로 수신되며, PaymentCompletedEvent로 변환되어 슬롯 확정 처리된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentCompletedEventMessage {

	private String topic;
	private String eventType;
	private String paymentId;
	private String reservationId;
	private String orderId;
	private String paymentKey;
	private Long amount;
	private String method;
	private String paidAt;

	/**
	 * 메시지 DTO를 PaymentCompletedEvent로 변환한다.
	 * 결제 완료 시 해당 예약의 슬롯을 확정한다.
	 */
	public PaymentCompletedEvent toEvent() {
		return PaymentCompletedEvent.of(
				paymentId,
				reservationId,
				orderId,
				paymentKey,
				amount,
				method,
				paidAt
		);
	}
}