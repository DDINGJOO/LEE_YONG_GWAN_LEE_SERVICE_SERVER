package com.teambind.springproject.space.event.handler;

import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.space.entity.RoomTimeSlot;
import com.teambind.springproject.space.event.event.SlotRestoredEvent;
import com.teambind.springproject.space.repository.RoomTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 슬롯 복구 이벤트 핸들러.
 * <p>
 * 해당 예약의 모든 슬롯을 CANCELLED → AVAILABLE 상태로 전환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotRestoredEventHandler implements EventHandler<SlotRestoredEvent> {

	private final RoomTimeSlotRepository slotRepository;

	@Override
	@Transactional
	public void handle(SlotRestoredEvent event) {
		log.info("Processing SlotRestoredEvent: reservationId={}, reason={}",
				event.getReservationId(), event.getRestoreReason());

		// reservationId로 모든 슬롯 조회
		List<RoomTimeSlot> slots = slotRepository.findByReservationId(event.getReservationId());

		if (slots.isEmpty()) {
			log.warn("No slots found for reservationId={}", event.getReservationId());
			return;
		}

		int restoredCount = 0;

		// 모든 슬롯을 복구 상태로 변경
		for (RoomTimeSlot slot : slots) {
			try {
				slot.restore();
				slotRepository.save(slot);
				restoredCount++;

				log.debug("Slot restored: slotId={}, slotDate={}, slotTime={}",
						slot.getSlotId(), slot.getSlotDate(), slot.getSlotTime());
			} catch (Exception e) {
				log.error("Failed to restore slot: slotId={}, reason={}",
						slot.getSlotId(), e.getMessage());
			}
		}

		log.info("SlotRestoredEvent processed: reservationId={}, restored {} of {} slots",
				event.getReservationId(), restoredCount, slots.size());
	}

	@Override
	public String getSupportedEventType() {
		return "SlotRestored";
	}
}
