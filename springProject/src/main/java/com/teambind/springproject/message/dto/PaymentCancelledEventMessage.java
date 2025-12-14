package com.teambind.springproject.message.dto;

import com.teambind.springproject.room.event.event.SlotCancelledEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 취소 이벤트 메시지 DTO.
 * <p>
 * 결제 서버에서 발행하는 payment-cancelled 토픽 메시지를 수신한다.
 * SlotCancelledEvent로 변환되어 슬롯 취소 처리된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentCancelledEventMessage {

	private String eventType;
	private String paymentId;
	private String reservationId;
	private String orderId;
	private Long amount;
	private String cancelledAt;

	/**
	 * 메시지 DTO를 SlotCancelledEvent로 변환한다.
	 * 결제 취소 시 해당 예약의 슬롯을 취소한다.
	 */
	public SlotCancelledEvent toEvent() {
		return SlotCancelledEvent.of(
				reservationId != null ? Long.parseLong(reservationId) : null,
				"PAYMENT_CANCELLED"
		);
	}
}
