package com.teambind.springproject.space.event.handler;

import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.space.entity.RoomTimeSlot;
import com.teambind.springproject.space.event.event.SlotConfirmedEvent;
import com.teambind.springproject.space.repository.RoomTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 슬롯 예약 확정 이벤트 핸들러.
 * <p>
 * 해당 예약의 모든 슬롯을 PENDING → RESERVED 상태로 전환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotConfirmedEventHandler implements EventHandler<SlotConfirmedEvent> {

	private final RoomTimeSlotRepository slotRepository;

	@Override
	@Transactional
	public void handle(SlotConfirmedEvent event) {
		log.info("Processing SlotConfirmedEvent: reservationId={}", event.getReservationId());

		// reservationId로 모든 슬롯 조회
		List<RoomTimeSlot> slots = slotRepository.findByReservationId(event.getReservationId());

		if (slots.isEmpty()) {
			log.warn("No slots found for reservationId={}", event.getReservationId());
			return;
		}

		int confirmedCount = 0;

		// 모든 슬롯을 확정 상태로 변경
		for (RoomTimeSlot slot : slots) {
			try {
				slot.confirm();
				slotRepository.save(slot);
				confirmedCount++;

				log.debug("Slot confirmed: slotId={}, slotDate={}, slotTime={}",
						slot.getSlotId(), slot.getSlotDate(), slot.getSlotTime());
			} catch (Exception e) {
				log.error("Failed to confirm slot: slotId={}, reason={}",
						slot.getSlotId(), e.getMessage());
			}
		}

		log.info("SlotConfirmedEvent processed: reservationId={}, confirmed {} of {} slots",
				event.getReservationId(), confirmedCount, slots.size());
	}

	@Override
	public String getSupportedEventType() {
		return "SlotConfirmed";
	}
}
