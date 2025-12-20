package com.teambind.springproject.room.event.handler;

import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.room.command.domain.service.TimeSlotManagementService;
import com.teambind.springproject.room.event.event.ReservationCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 취소 이벤트 핸들러.
 * <p>
 * 다른 서비스(예약 서비스)에서 예약이 취소되면 해당 예약의 슬롯 락을 해제한다.
 * PENDING/RESERVED 상태의 슬롯을 AVAILABLE 상태로 전환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCancelledEventHandler implements EventHandler<ReservationCancelledEvent> {

	private final TimeSlotManagementService timeSlotManagementService;

	@Override
	@Transactional
	public void handle(ReservationCancelledEvent event) {
		log.info("Processing ReservationCancelledEvent: reservationId={}, reason={}",
				event.getReservationId(), event.getCancelReason());

		try {
			// 슬롯 락 해제 (PENDING/RESERVED → AVAILABLE)
			timeSlotManagementService.cancelSlotsByReservationId(event.getReservationId());

			log.info("ReservationCancelledEvent processed successfully: reservationId={}, slots released",
					event.getReservationId());

		} catch (Exception e) {
			log.error("Failed to process ReservationCancelledEvent: reservationId={}, error={}",
					event.getReservationId(), e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public String getSupportedEventType() {
		return "ReservationCancelled";
	}
}
