package com.teambind.springproject.space.event.handler;

import com.teambind.springproject.message.handler.EventHandler;
import com.teambind.springproject.space.entity.RoomTimeSlot;
import com.teambind.springproject.space.event.event.SlotReservedEvent;
import com.teambind.springproject.space.repository.RoomTimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Optional;

/**
 * 슬롯 예약 대기 이벤트 핸들러.
 * <p>
 * 여러 슬롯을 AVAILABLE → PENDING 상태로 전환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotReservedEventHandler implements EventHandler<SlotReservedEvent> {

	private final RoomTimeSlotRepository slotRepository;

	@Override
	@Transactional
	public void handle(SlotReservedEvent event) {
		log.info("Processing SlotReservedEvent: roomId={}, slotDate={}, startTimes={}, reservationId={}",
				event.getRoomId(), event.getSlotDate(), event.getStartTimes(), event.getReservationId());

		int reservedCount = 0;

		// 각 시작 시각에 대해 슬롯을 예약 대기 상태로 변경
		for (LocalTime startTime : event.getStartTimes()) {
			Optional<RoomTimeSlot> slotOptional = slotRepository.findByRoomIdAndSlotDateAndSlotTime(
					event.getRoomId(),
					event.getSlotDate(),
					startTime
			);

			if (slotOptional.isPresent()) {
				RoomTimeSlot slot = slotOptional.get();
				try {
					slot.markAsPending(event.getReservationId());
					slotRepository.save(slot);
					reservedCount++;

					log.debug("Slot marked as PENDING: slotId={}, startTime={}",
							slot.getSlotId(), startTime);
				} catch (Exception e) {
					log.error("Failed to mark slot as PENDING: roomId={}, slotDate={}, startTime={}, reason={}",
							event.getRoomId(), event.getSlotDate(), startTime, e.getMessage());
				}
			} else {
				log.warn("Slot not found: roomId={}, slotDate={}, startTime={}",
						event.getRoomId(), event.getSlotDate(), startTime);
			}
		}

		log.info("SlotReservedEvent processed: reservationId={}, reserved {} of {} slots",
				event.getReservationId(), reservedCount, event.getStartTimes().size());
	}

	@Override
	public String getSupportedEventType() {
		return "SlotReserved";
	}
}
