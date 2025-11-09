package com.teambind.springproject.room.command.domain.service;

import com.teambind.springproject.common.exceptions.domain.SlotNotFoundException;
import com.teambind.springproject.room.domain.port.TimeSlotPort;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 시간 슬롯 상태 관리 서비스 구현체.
 * <p>
 * Hexagonal Architecture 적용:
 * <p>
 * Infrastructure 계층(JPA)에 직접 의존하지 않고 Port 인터페이스에 의존
 * DIP (Dependency Inversion Principle) 준수
 *
 */
@Service
@Transactional
public class TimeSlotManagementServiceImpl implements TimeSlotManagementService {
	
	private static final Logger log = LoggerFactory.getLogger(TimeSlotManagementServiceImpl.class);
	private static final int PENDING_EXPIRATION_MINUTES = 15;

	private final TimeSlotPort timeSlotPort;

	public TimeSlotManagementServiceImpl(TimeSlotPort timeSlotPort) {
		this.timeSlotPort = timeSlotPort;
	}
	
	@Override
	public void markSlotAsPending(
			Long roomId,
			LocalDate slotDate,
			LocalTime slotTime,
			Long reservationId
	) {
		RoomTimeSlot slot = findSlot(roomId, slotDate, slotTime);

		// 도메인 로직: 상태 전이
		slot.markAsPending(reservationId);
		timeSlotPort.save(slot);

		log.info("Slot marked as pending: slotId={}, roomId={}, reservationId={}",
				slot.getSlotId(), roomId, reservationId);
	}
	
	@Override
	public void confirmSlot(Long roomId, LocalDate slotDate, LocalTime slotTime) {
		RoomTimeSlot slot = findSlot(roomId, slotDate, slotTime);

		Long reservationId = slot.getReservationId();

		// 도메인 로직: 상태 전이
		slot.confirm();
		timeSlotPort.save(slot);

		log.info("Slot confirmed: slotId={}, roomId={}, reservationId={}",
				slot.getSlotId(), roomId, reservationId);
	}
	
	@Override
	public void cancelSlot(Long roomId, LocalDate slotDate, LocalTime slotTime) {
		RoomTimeSlot slot = findSlot(roomId, slotDate, slotTime);

		Long reservationId = slot.getReservationId();

		// 도메인 로직: 상태 전이
		slot.cancel();
		timeSlotPort.save(slot);

		log.info("Slot cancelled: slotId={}, roomId={}, reservationId={}",
				slot.getSlotId(), roomId, reservationId);
	}
	
	@Override
	public void cancelSlotsByReservationId(Long reservationId) {
		// Port를 통해 예약 ID로 슬롯 조회
		List<RoomTimeSlot> slots = timeSlotPort.findByReservationId(reservationId);

		for (RoomTimeSlot slot : slots) {
			slot.cancel();
		}

		timeSlotPort.saveAll(slots);

		log.info("Cancelled {} slots for reservationId={}", slots.size(), reservationId);
	}
	
	@Override
	public int restoreExpiredPendingSlots() {
		// Port를 통해 만료된 PENDING 슬롯 조회
		List<RoomTimeSlot> expiredSlots = timeSlotPort.findExpiredPendingSlots(PENDING_EXPIRATION_MINUTES);

		for (RoomTimeSlot slot : expiredSlots) {
			// 먼저 취소 처리
			slot.cancel();
			// 그 다음 복구
			slot.restore();
		}

		timeSlotPort.saveAll(expiredSlots);

		if (!expiredSlots.isEmpty()) {
			log.info("Restored {} expired pending slots", expiredSlots.size());
		}

		return expiredSlots.size();
	}
	
	/**
	 * 슬롯을 조회한다. 없으면 예외를 던진다.
	 */
	private RoomTimeSlot findSlot(Long roomId, LocalDate slotDate, LocalTime slotTime) {
		return timeSlotPort
				.findByRoomIdAndSlotDateAndSlotTime(roomId, slotDate, slotTime)
				.orElseThrow(() -> new SlotNotFoundException(roomId, slotDate.toString(), slotTime.toString()));
	}
}
