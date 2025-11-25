package com.teambind.springproject.room.command.domain.service;

import com.teambind.springproject.common.exceptions.domain.SlotNotFoundException;
import com.teambind.springproject.room.domain.port.TimeSlotPort;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 시간 슬롯 상태 관리 서비스 구현체.
 *
 * Hexagonal Architecture 적용:
 *
 * Infrastructure 계층(JPA)에 직접 의존하지 않고 Port 인터페이스에 의존
 * DIP (Dependency Inversion Principle) 준수
 *
 */
@Service
@Transactional
public class TimeSlotManagementServiceImpl implements TimeSlotManagementService {
	
	private static final Logger log = LoggerFactory.getLogger(TimeSlotManagementServiceImpl.class);

	private final TimeSlotPort timeSlotPort;
	private final int pendingExpirationMinutes;

	public TimeSlotManagementServiceImpl(
			TimeSlotPort timeSlotPort,
			@Value("${room.timeSlot.pending.expiration.minutes}") int pendingExpirationMinutes
	) {
		this.timeSlotPort = timeSlotPort;
		this.pendingExpirationMinutes = pendingExpirationMinutes;
	}
	
	@Override
	public void markSlotAsPending(
			Long roomId,
			LocalDate slotDate,
			LocalTime slotTime,
			Long reservationId
	) {
		// 단일 슬롯도 다중 슬롯 로직으로 통일 (Pessimistic Lock 적용)
		// List.of(slotTime)으로 변환하여 markMultipleSlotsAsPending() 재사용
		log.info("Delegating single slot reservation to multiple slots logic: roomId={}, slotDate={}, slotTime={}, reservationId={}",
				roomId, slotDate, slotTime, reservationId);

		markMultipleSlotsAsPending(roomId, slotDate, List.of(slotTime), reservationId);
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
		List<RoomTimeSlot> expiredSlots = timeSlotPort.findExpiredPendingSlots(pendingExpirationMinutes);

		for (RoomTimeSlot slot : expiredSlots) {
			// PENDING → AVAILABLE 상태로 변경 및 reservationId 제거
			slot.cancel();
		}

		timeSlotPort.saveAll(expiredSlots);

		if (!expiredSlots.isEmpty()) {
			log.info("Restored {} expired pending slots", expiredSlots.size());
		}

		return expiredSlots.size();
	}
	
	@Override
	public int markMultipleSlotsAsPending(
			Long roomId,
			LocalDate slotDate,
			List<LocalTime> slotTimes,
			Long reservationId
	) {
		log.info("Attempting to reserve multiple slots: roomId={}, slotDate={}, slotTimes={}, reservationId={}",
				roomId, slotDate, slotTimes, reservationId);

		// 1. Pessimistic Lock을 사용하여 슬롯 조회 (SELECT ... FOR UPDATE)
		List<RoomTimeSlot> slots = timeSlotPort.findByRoomIdAndSlotDateAndSlotTimeInWithLock(
				roomId, slotDate, slotTimes
		);

		// 2. 요청한 슬롯 수와 조회된 슬롯 수 확인
		if (slots.size() != slotTimes.size()) {
			List<LocalTime> foundTimes = slots.stream()
					.map(RoomTimeSlot::getSlotTime)
					.collect(Collectors.toList());
			List<LocalTime> missingTimes = slotTimes.stream()
					.filter(time -> !foundTimes.contains(time))
					.collect(Collectors.toList());

			log.error("Some slots not found: roomId={}, slotDate={}, missingTimes={}",
					roomId, slotDate, missingTimes);
			throw new IllegalStateException(
					String.format("일부 슬롯을 찾을 수 없습니다. roomId=%d, slotDate=%s, missingTimes=%s",
							roomId, slotDate, missingTimes)
			);
		}

		// 3. 모든 슬롯이 AVAILABLE 상태인지 확인
		List<RoomTimeSlot> unavailableSlots = slots.stream()
				.filter(slot -> slot.getStatus() != SlotStatus.AVAILABLE)
				.collect(Collectors.toList());

		if (!unavailableSlots.isEmpty()) {
			String unavailableInfo = unavailableSlots.stream()
					.map(slot -> String.format("%s(%s)", slot.getSlotTime(), slot.getStatus()))
					.collect(Collectors.joining(", "));

			log.error("Some slots are not available: roomId={}, slotDate={}, unavailable={}",
					roomId, slotDate, unavailableInfo);
			throw new IllegalStateException(
					String.format("일부 슬롯을 예약할 수 없습니다. roomId=%d, slotDate=%s, unavailable=%s",
							roomId, slotDate, unavailableInfo)
			);
		}

		// 4. 모든 슬롯을 PENDING 상태로 변경
		for (RoomTimeSlot slot : slots) {
			slot.markAsPending(reservationId);
		}

		// 5. 일괄 저장
		timeSlotPort.saveAll(slots);

		log.info("Successfully marked {} slots as pending: roomId={}, slotDate={}, reservationId={}",
				slots.size(), roomId, slotDate, reservationId);

		return slots.size();
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
