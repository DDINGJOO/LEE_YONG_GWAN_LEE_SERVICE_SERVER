package com.teambind.springproject.room.event.event;

import com.teambind.springproject.message.event.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 완료 이벤트.
 * <p>
 * Payment 도메인에서 발행한 결제 완료 이벤트를 Room 도메인에서 수신하여
 * 해당 예약의 슬롯을 PENDING → RESERVED 상태로 확정한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentCompletedEvent extends Event {

	private static final String TOPIC = "payment-completed";
	private static final String EVENT_TYPE = "PaymentCompleted";

	private String paymentId;
	private String reservationId;
	private String orderId;
	private String paymentKey;
	private Long amount;
	private String method;
	private String paidAt;

	private PaymentCompletedEvent(
			String paymentId,
			String reservationId,
			String orderId,
			String paymentKey,
			Long amount,
			String method,
			String paidAt
	) {
		super(TOPIC, EVENT_TYPE);
		this.paymentId = paymentId;
		this.reservationId = reservationId;
		this.orderId = orderId;
		this.paymentKey = paymentKey;
		this.amount = amount;
		this.method = method;
		this.paidAt = paidAt;
	}

	public static PaymentCompletedEvent of(
			String paymentId,
			String reservationId,
			String orderId,
			String paymentKey,
			Long amount,
			String method,
			String paidAt
	) {
		return new PaymentCompletedEvent(
				paymentId,
				reservationId,
				orderId,
				paymentKey,
				amount,
				method,
				paidAt
		);
	}

	@Override
	public String getEventTypeName() {
		return EVENT_TYPE;
	}
}