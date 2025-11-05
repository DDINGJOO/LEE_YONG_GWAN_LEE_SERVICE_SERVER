package com.teambind.springproject.space.service.impl;

import com.teambind.springproject.common.exceptions.domain.SlotNotFoundException;
import com.teambind.springproject.space.entity.RoomTimeSlot;
import com.teambind.springproject.space.entity.enums.SlotStatus;
import com.teambind.springproject.space.event.event.SlotCancelledEvent;
import com.teambind.springproject.space.event.event.SlotConfirmedEvent;
import com.teambind.springproject.space.event.event.SlotReservedEvent;
import com.teambind.springproject.space.event.event.SlotRestoredEvent;
import com.teambind.springproject.space.repository.RoomTimeSlotRepository;
import com.teambind.springproject.space.service.TimeSlotManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 시간 슬롯 상태 관리 서비스 구현체.
 */
@Service
@Transactional
public class TimeSlotManagementServiceImpl implements TimeSlotManagementService {
	
	private static final Logger log = LoggerFactory.getLogger(TimeSlotManagementServiceImpl.class);
	private static final int PENDING_EXPIRATION_MINUTES = 15;
	
	private final RoomTimeSlotRepository slotRepository;
	private final ApplicationEventPublisher eventPublisher;
	
	public TimeSlotManagementServiceImpl(
			RoomTimeSlotRepository slotRepository,
			ApplicationEventPublisher eventPublisher
	) {
		this.slotRepository = slotRepository;
		this.eventPublisher = eventPublisher;
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
		slotRepository.save(slot);
		
		// 이벤트 발행
		SlotReservedEvent event = SlotReservedEvent.of(
				roomId,
				slotDate,
				List.of(slotTime),
				reservationId
		);
		eventPublisher.publishEvent(event);
		
		log.info("Slot marked as pending: slotId={}, roomId={}, reservationId={}",
				slot.getSlotId(), roomId, reservationId);
	}
	
	@Override
	public void confirmSlot(Long roomId, LocalDate slotDate, LocalTime slotTime) {
		RoomTimeSlot slot = findSlot(roomId, slotDate, slotTime);
		
		Long reservationId = slot.getReservationId();
		
		// 도메인 로직: 상태 전이
		slot.confirm();
		slotRepository.save(slot);
		
		// 이벤트 발행
		SlotConfirmedEvent event = SlotConfirmedEvent.of(reservationId);
		eventPublisher.publishEvent(event);
		
		log.info("Slot confirmed: slotId={}, roomId={}, reservationId={}",
				slot.getSlotId(), roomId, reservationId);
	}
	
	@Override
	public void cancelSlot(Long roomId, LocalDate slotDate, LocalTime slotTime) {
		RoomTimeSlot slot = findSlot(roomId, slotDate, slotTime);
		
		Long reservationId = slot.getReservationId();
		
		// 도메인 로직: 상태 전이
		slot.cancel();
		slotRepository.save(slot);
		
		// 이벤트 발행
		SlotCancelledEvent event = SlotCancelledEvent.of(
				reservationId,
				"User cancelled"
		);
		eventPublisher.publishEvent(event);
		
		log.info("Slot cancelled: slotId={}, roomId={}, reservationId={}",
				slot.getSlotId(), roomId, reservationId);
	}
	
	@Override
	public void cancelSlotsByReservationId(Long reservationId) {
		// 예약 ID로 모든 슬롯 조회 (JPQL 사용)
		List<RoomTimeSlot> slots = slotRepository
				.findAll()
				.stream()
				.filter(slot -> reservationId.equals(slot.getReservationId()))
				.toList();
		
		for (RoomTimeSlot slot : slots) {
			slot.cancel();
		}

		slotRepository.saveAll(slots);

		// 이벤트 발행 (한 번만)
		SlotCancelledEvent event = SlotCancelledEvent.of(
				reservationId,
				"Reservation cancelled"
		);
		eventPublisher.publishEvent(event);
		
		log.info("Cancelled {} slots for reservationId={}", slots.size(), reservationId);
	}
	
	@Override
	public int restoreExpiredPendingSlots() {
		LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(PENDING_EXPIRATION_MINUTES);
		
		// PENDING 상태이고 만료된 슬롯 조회
		List<RoomTimeSlot> expiredSlots = slotRepository
				.findAll()
				.stream()
				.filter(slot -> slot.getStatus() == SlotStatus.PENDING)
				.filter(slot -> slot.getLastUpdated().isBefore(expirationTime))
				.toList();
		
		for (RoomTimeSlot slot : expiredSlots) {
			// 먼저 취소 처리
			slot.cancel();
			// 그 다음 복구
			slot.restore();

			// 이벤트 발행 (각 슬롯마다 별도의 reservationId가 있을 수 있음)
			SlotRestoredEvent event = SlotRestoredEvent.of(
					slot.getReservationId(),
					"Expired PENDING slot"
			);
			eventPublisher.publishEvent(event);
		}

		slotRepository.saveAll(expiredSlots);
		
		if (!expiredSlots.isEmpty()) {
			log.info("Restored {} expired pending slots", expiredSlots.size());
		}
		
		return expiredSlots.size();
	}
	
	/**
	 * 슬롯을 조회한다. 없으면 예외를 던진다.
	 */
	private RoomTimeSlot findSlot(Long roomId, LocalDate slotDate, LocalTime slotTime) {
		return slotRepository
				.findByRoomIdAndSlotDateAndSlotTime(roomId, slotDate, slotTime)
				.orElseThrow(() -> new SlotNotFoundException(roomId, slotDate.toString(), slotTime.toString()));
	}
}
