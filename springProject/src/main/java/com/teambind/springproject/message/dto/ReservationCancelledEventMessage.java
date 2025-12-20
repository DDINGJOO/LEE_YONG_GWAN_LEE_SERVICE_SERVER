package com.teambind.springproject.message.dto;

import com.teambind.springproject.room.event.event.ReservationCancelledEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 예약 취소 이벤트 메시지 DTO.
 * <p>
 * 다른 서비스(예약 서비스)에서 발행한 예약 취소 이벤트를 수신할 때 사용된다.
 * 이 이벤트를 받으면 해당 예약의 슬롯 락을 해제한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReservationCancelledEventMessage {

	private String eventType;
	private String reservationId;
	private String cancelReason;
	private LocalDateTime occurredAt;

	/**
	 * 메시지 DTO를 ReservationCancelledEvent로 변환한다.
	 * String ID → Long ID 변환
	 */
	public ReservationCancelledEvent toEvent() {
		return ReservationCancelledEvent.of(
				reservationId != null ? Long.parseLong(reservationId) : null,
				cancelReason
		);
	}
}
