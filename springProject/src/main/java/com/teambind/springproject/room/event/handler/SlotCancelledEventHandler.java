package com.teambind.springproject.room.event.handler;

import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.event.event.SlotCancelledEvent;
import com.teambind.springproject.room.repository.RoomTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 슬롯 취소 이벤트 핸들러.
 * <p>
 * 해당 예약의 모든 슬롯을 PENDING/RESERVED → CANCELLED 상태로 전환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotCancelledEventHandler implements EventHandler<SlotCancelledEvent> {
	
	private final RoomTimeSlotRepository slotRepository;
	
	@Override
	@Transactional
	public void handle(SlotCancelledEvent event) {
		log.info("Processing SlotCancelledEvent: reservationId={}, reason={}",
				event.getReservationId(), event.getCancelReason());
		
		// reservationId로 모든 슬롯 조회
		List<RoomTimeSlot> slots = slotRepository.findByReservationId(event.getReservationId());
		
		if (slots.isEmpty()) {
			log.warn("No slots found for reservationId={}", event.getReservationId());
			return;
		} // irs
		
		int cancelledCount = 0;
		
		// 모든 슬롯을 이용가능 상태로 변경
		for (RoomTimeSlot slot : slots) {
			try {
				slot.cancel();
				slotRepository.save(slot);
				cancelledCount++;
				
				log.debug("Slot cancelled: slotId={}, slotDate={}, slotTime={}",
						slot.getSlotId(), slot.getSlotDate(), slot.getSlotTime());
			} catch (Exception e) {
				log.error("Failed to cancel slot: slotId={}, reason={}",
						slot.getSlotId(), e.getMessage());
			}
		}
		
		log.info("SlotCancelledEvent processed: reservationId={}, cancelled {} of {} slots",
				event.getReservationId(), cancelledCount, slots.size());
	}
	
	@Override
	public String getSupportedEventType() {
		return "SlotCancelled";
	}
}
