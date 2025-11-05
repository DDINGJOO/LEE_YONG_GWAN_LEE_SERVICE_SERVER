package com.teambind.springproject.space.handler;

import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.space.event.SlotReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 슬롯 예약 대기 이벤트 핸들러
 * <p>
 * 슬롯이 예약 대기 상태로 전환되었을 때의 비즈니스 로직을 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotReservedEventHandler implements EventHandler<SlotReservedEvent> {
	
	// TODO: 필요한 서비스 의존성 주입
	// private final NotificationService notificationService;
	// private final ReservationService reservationService;
	
	@Override
	public void handle(SlotReservedEvent event) {
		log.info("Processing SlotReservedEvent: slotId={}, reservationId={}",
				event.getSlotId(), event.getReservationId());
		
		try {
			// TODO: 실제 비즈니스 로직 구현
			// 예시:
			// 1. 사용자에게 예약 대기 알림 전송
			// notificationService.sendReservationPendingNotification(event.getReservationId());
			
			// 2. 예약 만료 타이머 설정 (일정 시간 내 결제 미완료시 자동 취소)
			// reservationService.scheduleExpirationCheck(event.getReservationId());
			
			// 3. 예약 상태 로그 기록
			logReservationStatus(event);
			
			log.info("SlotReservedEvent processed successfully: slotId={}", event.getSlotId());
			
		} catch (Exception e) {
			log.error("Failed to process SlotReservedEvent: slotId={}", event.getSlotId(), e);
			throw e;
		}
	}
	
	@Override
	public String getSupportedEventType() {
		return "SlotReserved";
	}
	
	private void logReservationStatus(SlotReservedEvent event) {
		log.info("Reservation status - Room: {}, Date: {}, Time: {}, ReservationId: {}",
				event.getRoomId(), event.getSlotDate(), event.getSlotTime(), event.getReservationId());
	}
}
