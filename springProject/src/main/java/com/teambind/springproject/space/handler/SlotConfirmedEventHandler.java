package com.teambind.springproject.space.handler;

import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.space.event.SlotConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 슬롯 예약 확정 이벤트 핸들러
 * <p>
 * 결제 완료 후 슬롯이 확정되었을 때의 비즈니스 로직을 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotConfirmedEventHandler implements EventHandler<SlotConfirmedEvent> {
	
	// TODO: 필요한 서비스 의존성 주입
	// private final NotificationService notificationService;
	// private final ReservationService reservationService;
	
	@Override
	public void handle(SlotConfirmedEvent event) {
		log.info("Processing SlotConfirmedEvent: slotId={}, reservationId={}",
				event.getSlotId(), event.getReservationId());
		
		try {
			// TODO: 실제 비즈니스 로직 구현
			// 예시:
			// 1. 사용자에게 예약 확정 알림 전송
			// notificationService.sendReservationConfirmedNotification(event.getReservationId());
			
			// 2. QR 코드 생성 및 발송
			// reservationService.generateAndSendQRCode(event.getReservationId());
			
			// 3. 예약 확정 이메일 발송
			// emailService.sendConfirmationEmail(event.getReservationId());
			
			// 4. 예약 상태 로그 기록
			logReservationConfirmation(event);
			
			log.info("SlotConfirmedEvent processed successfully: slotId={}", event.getSlotId());
			
		} catch (Exception e) {
			log.error("Failed to process SlotConfirmedEvent: slotId={}", event.getSlotId(), e);
			throw e;
		}
	}
	
	@Override
	public String getSupportedEventType() {
		return "SlotConfirmed";
	}
	
	private void logReservationConfirmation(SlotConfirmedEvent event) {
		log.info("Reservation confirmed - Room: {}, Date: {}, Time: {}, ReservationId: {}",
				event.getRoomId(), event.getSlotDate(), event.getSlotTime(), event.getReservationId());
	}
}
