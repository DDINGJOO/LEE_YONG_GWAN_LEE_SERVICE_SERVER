package com.teambind.springproject.space.handler;

import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.space.event.SlotRestoredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 슬롯 복구 이벤트 핸들러
 * <p>
 * 슬롯이 사용 가능한 상태로 복구되었을 때의 비즈니스 로직을 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotRestoredEventHandler implements EventHandler<SlotRestoredEvent> {
	
	// TODO: 필요한 서비스 의존성 주입
	// private final SlotAvailabilityService availabilityService;
	// private final CacheService cacheService;
	
	@Override
	public void handle(SlotRestoredEvent event) {
		log.info("Processing SlotRestoredEvent: slotId={}, reason={}",
				event.getSlotId(), event.getRestoreReason());
		
		try {
			// TODO: 실제 비즈니스 로직 구현
			// 예시:
			// 1. 슬롯 가용성 캐시 업데이트
			// cacheService.updateSlotAvailability(event.getSlotId(), true);
			
			// 2. 예약 가능한 슬롯 목록 갱신
			// availabilityService.refreshAvailableSlots(
			//     event.getRoomId(),
			//     event.getSlotDate()
			// );
			
			// 3. 대기 중인 사용자에게 슬롯 복구 알림
			// if (event.getRestoreReason().contains("CANCELLED")) {
			//     notificationService.notifyWaitingUsers(event.getSlotId());
			// }
			
			// 4. 복구 로그 기록
			logRestoration(event);
			
			log.info("SlotRestoredEvent processed successfully: slotId={}", event.getSlotId());
			
		} catch (Exception e) {
			log.error("Failed to process SlotRestoredEvent: slotId={}", event.getSlotId(), e);
			throw e;
		}
	}
	
	@Override
	public String getSupportedEventType() {
		return "SlotRestored";
	}
	
	private void logRestoration(SlotRestoredEvent event) {
		log.info("Slot restored - Room: {}, Date: {}, Time: {}, Reason: {}",
				event.getRoomId(), event.getSlotDate(), event.getSlotTime(), event.getRestoreReason());
	}
}
