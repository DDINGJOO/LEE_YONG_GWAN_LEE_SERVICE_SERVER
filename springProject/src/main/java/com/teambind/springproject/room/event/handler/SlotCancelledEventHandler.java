package com.teambind.springproject.room.event.handler;

import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.room.command.domain.service.TimeSlotManagementService;
import com.teambind.springproject.room.event.event.SlotCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 슬롯 취소 이벤트 핸들러.
 * <p>
 * Hexagonal Architecture 적용:
 * - Infrastructure Layer의 Repository를 직접 참조하지 않음
 * - Domain Service를 통한 간접 참조로 계층 격리 유지
 * <p>
 * 해당 예약의 모든 슬롯을 PENDING/RESERVED → AVAILABLE 상태로 전환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotCancelledEventHandler implements EventHandler<SlotCancelledEvent> {
	
	private final TimeSlotManagementService timeSlotManagementService;
	
	@Override
	@Transactional
	public void handle(SlotCancelledEvent event) {
		log.info("Processing SlotCancelledEvent: reservationId={}, reason={}",
				event.getReservationId(), event.getCancelReason());
		
		try {
			// Domain Service를 통한 슬롯 취소 (트랜잭션 원자성 보장)
			timeSlotManagementService.cancelSlotsByReservationId(event.getReservationId());
			
			log.info("SlotCancelledEvent processed successfully: reservationId={}, reason={}",
					event.getReservationId(), event.getCancelReason());
			
		} catch (Exception e) {
			log.error("Failed to process SlotCancelledEvent: reservationId={}, reason={}",
					event.getReservationId(), e.getMessage(), e);
			throw e; // Re-throw for transaction rollback
		}
	}
	
	@Override
	public String getSupportedEventType() {
		return "SlotCancelled";
	}
}
