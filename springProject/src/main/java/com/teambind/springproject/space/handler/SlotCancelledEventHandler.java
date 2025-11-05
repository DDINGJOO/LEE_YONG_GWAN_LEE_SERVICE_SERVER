package com.teambind.springproject.space.handler;

import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.space.event.SlotCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 슬롯 취소 이벤트 핸들러
 * <p>
 * 슬롯이 취소되었을 때의 비즈니스 로직을 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotCancelledEventHandler implements EventHandler<SlotCancelledEvent> {
	
	// TODO: 필요한 서비스 의존성 주입
	// private final NotificationService notificationService;
	// private final RefundService refundService;
	
	@Override
	public void handle(SlotCancelledEvent event) {
		log.info("Processing SlotCancelledEvent: slotId={}, reservationId={}, reason={}",
				event.getSlotId(), event.getReservationId(), event.getCancelReason());
		
		try {
			// TODO: 실제 비즈니스 로직 구현
			// 예시:
			// 1. 환불 처리
			// if (event.getCancelReason().contains("PAYMENT_FAILED")) {
			//     // 결제 실패는 환불 불필요
			// } else {
			//     refundService.processRefund(event.getReservationId());
			// }
			
			// 2. 사용자에게 취소 알림 전송
			// notificationService.sendCancellationNotification(
			//     event.getReservationId(),
			//     event.getCancelReason()
			// );
			
			// 3. 슬롯을 다시 사용 가능한 상태로 복구
			// slotService.restoreSlot(event.getSlotId());
			
			// 4. 취소 로그 기록
			logCancellation(event);
			
			log.info("SlotCancelledEvent processed successfully: slotId={}", event.getSlotId());
			
		} catch (Exception e) {
			log.error("Failed to process SlotCancelledEvent: slotId={}", event.getSlotId(), e);
			throw e;
		}
	}
	
	@Override
	public String getSupportedEventType() {
		return "SlotCancelled";
	}
	
	private void logCancellation(SlotCancelledEvent event) {
		log.info("Reservation cancelled - Room: {}, Date: {}, Time: {}, ReservationId: {}, Reason: {}",
				event.getRoomId(), event.getSlotDate(), event.getSlotTime(),
				event.getReservationId(), event.getCancelReason());
	}
}
