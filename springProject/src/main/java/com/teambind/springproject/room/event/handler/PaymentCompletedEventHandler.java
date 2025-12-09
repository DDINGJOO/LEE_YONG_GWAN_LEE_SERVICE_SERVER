package com.teambind.springproject.room.event.handler;

import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.room.command.domain.service.TimeSlotManagementService;
import com.teambind.springproject.room.event.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 완료 이벤트 핸들러.
 * <p>
 * Hexagonal Architecture 적용:
 * - Infrastructure Layer의 Repository를 직접 참조하지 않음
 * - Domain Service를 통한 간접 참조로 계층 격리 유지
 * <p>
 * 해당 예약의 모든 슬롯을 PENDING → RESERVED 상태로 전환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventHandler implements EventHandler<PaymentCompletedEvent> {
	
	private final TimeSlotManagementService timeSlotManagementService;
	
	@Override
	@Transactional
	public void handle(PaymentCompletedEvent event) {
		log.info("Processing PaymentCompletedEvent: paymentId={}, reservationId={}, orderId={}, amount={}",
				event.getPaymentId(), event.getReservationId(), event.getOrderId(), event.getAmount());
		
		try {
			// String → Long 변환
			Long reservationId = Long.parseLong(event.getReservationId());
			
			// Domain Service를 통한 슬롯 확정 (트랜잭션 원자성 보장)
			timeSlotManagementService.confirmSlotsByReservationId(reservationId);
			
			log.info("PaymentCompletedEvent processed successfully: paymentId={}, reservationId={}, amount={}",
					event.getPaymentId(), reservationId, event.getAmount());
			
		} catch (NumberFormatException e) {
			log.error("Invalid reservationId format: reservationId={}", event.getReservationId(), e);
			throw new IllegalArgumentException("Invalid reservationId format: " + event.getReservationId(), e);
		} catch (Exception e) {
			log.error("Failed to process PaymentCompletedEvent: paymentId={}, reservationId={}, reason={}",
					event.getPaymentId(), event.getReservationId(), e.getMessage(), e);
			throw e; // Re-throw for transaction rollback
		}
	}
	
	@Override
	public String getSupportedEventType() {
		return "PaymentCompleted";
	}
}
