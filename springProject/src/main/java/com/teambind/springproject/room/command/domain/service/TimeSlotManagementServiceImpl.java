package com.teambind.springproject.room.command.domain.service;

import com.teambind.springproject.common.exceptions.domain.PolicyNotFoundException;
import com.teambind.springproject.common.exceptions.domain.SlotNotAvailableException;
import com.teambind.springproject.common.exceptions.domain.SlotNotFoundException;
import com.teambind.springproject.room.domain.port.ClosedDateUpdateRequestPort;
import com.teambind.springproject.room.domain.port.OperatingPolicyPort;
import com.teambind.springproject.room.domain.port.TimeSlotPort;
import com.teambind.springproject.room.entity.ClosedDateUpdateRequest;
import com.teambind.springproject.room.entity.RoomOperatingPolicy;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import com.teambind.springproject.room.entity.vo.ClosedDateRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
	
	private final TimeSlotPort timeSlotPort;
	private final OperatingPolicyPort operatingPolicyPort;
	private final ClosedDateUpdateRequestPort closedDateUpdateRequestPort;
	private final int pendingExpirationMinutes;
	
	public TimeSlotManagementServiceImpl(
			TimeSlotPort timeSlotPort,
			OperatingPolicyPort operatingPolicyPort,
			ClosedDateUpdateRequestPort closedDateUpdateRequestPort,
			@Value("${room.timeSlot.pending.expiration.minutes}") int pendingExpirationMinutes
	) {
		this.timeSlotPort = timeSlotPort;
		this.operatingPolicyPort = operatingPolicyPort;
		this.closedDateUpdateRequestPort = closedDateUpdateRequestPort;
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
	public void confirmSlotsByReservationId(Long reservationId) {
		// Port를 통해 예약 ID로 슬롯 조회
		List<RoomTimeSlot> slots = timeSlotPort.findByReservationId(reservationId);
		
		if (slots.isEmpty()) {
			log.warn("No slots found for reservationId={}", reservationId);
			return;
		}
		
		for (RoomTimeSlot slot : slots) {
			// PENDING → RESERVED 상태 전환
			slot.confirm();
		}
		
		timeSlotPort.saveAll(slots);
		
		log.info("Confirmed {} slots for reservationId={}", slots.size(), reservationId);
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
			throw new SlotNotFoundException(
					roomId, slotDate.toString(), missingTimes.toString()
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
			throw new SlotNotAvailableException(
					unavailableInfo
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
	
	@Override
	public void restoreSlotsAfterRefund(
			Long roomId,
			LocalDate slotDate,
			List<LocalTime> slotTimes
	) {
		log.info("Attempting to restore slots after refund: roomId={}, slotDate={}, slotTimes={}",
				roomId, slotDate, slotTimes);
		
		// 1. Pessimistic Lock을 사용하여 슬롯 조회 (SELECT ... FOR UPDATE)
		List<RoomTimeSlot> slots = timeSlotPort.findByRoomIdAndSlotDateAndSlotTimeInWithLock(
				roomId, slotDate, slotTimes
		);
		
		// 2. 요청한 슬롯 수와 조회된 슬롯 수 확인 (원자적 처리)
		if (slots.size() != slotTimes.size()) {
			List<LocalTime> foundTimes = slots.stream()
					.map(RoomTimeSlot::getSlotTime)
					.collect(Collectors.toList());
			List<LocalTime> missingTimes = slotTimes.stream()
					.filter(time -> !foundTimes.contains(time))
					.collect(Collectors.toList());
			
			log.error("Some slots not found for refund restoration: roomId={}, slotDate={}, missingTimes={}",
					roomId, slotDate, missingTimes);
			throw new SlotNotFoundException(
					roomId,
					slotDate.toString(),
					String.format("일부 슬롯을 찾을 수 없습니다. missingTimes=%s", missingTimes)
			);
		}
		
		// 3. 도메인 규칙 검증 및 상태 전이
		for (RoomTimeSlot slot : slots) {
			// 예약되지 않은 슬롯에 대한 복구 시도 시 경고 로그
			if (slot.getStatus() != SlotStatus.RESERVED && slot.getStatus() != SlotStatus.PENDING) {
				log.warn("Restoring non-reserved slot: slotId={}, roomId={}, status={}, slotTime={}",
						slot.getSlotId(), roomId, slot.getStatus(), slot.getSlotTime());
			}
			
			// RESERVED/PENDING → AVAILABLE 상태 전이
			slot.markAsAvailable();
		}
		
		// 4. 일괄 저장
		timeSlotPort.saveAll(slots);
		
		log.info("Successfully restored {} slots after refund: roomId={}, slotDate={}",
				slots.size(), roomId, slotDate);
	}
	
	@Override
	public int updateClosedDatesForRoom(Long roomId, String requestId) {
		log.info("Updating closed dates for room: roomId={}, requestId={}", roomId, requestId);
		
		// 1. 요청 조회
		ClosedDateUpdateRequest request = closedDateUpdateRequestPort.findById(requestId)
				.orElseThrow(() -> new IllegalStateException(
						"ClosedDateUpdateRequest not found: " + requestId
				));
		
		try {
			// 2. 처리 시작 상태로 변경
			request.markAsInProgress();
			closedDateUpdateRequestPort.save(request);
			
			log.info("Starting closed date update: requestId={}", requestId);
			
			// 3. RoomOperatingPolicy에서 휴무일 조회
			RoomOperatingPolicy policy = operatingPolicyPort.findByRoomId(roomId)
					.orElseThrow(() -> new PolicyNotFoundException(roomId, true));
			
			List<ClosedDateRange> closedDateRanges = policy.getClosedDates();
			
			log.info("Found {} closed date ranges for roomId={}", closedDateRanges.size(), roomId);
			
			int affectedSlots = 0;
			
			// 4. 각 휴무일 범위에 대해 슬롯 업데이트
			for (ClosedDateRange range : closedDateRanges) {
				List<RoomTimeSlot> slots;
				
				if (range.isPatternBased()) {
					// 패턴 기반 휴무일: 60일치 슬롯 조회
					LocalDate today = LocalDate.now();
					LocalDate futureDate = today.plusMonths(2);
					
					slots = timeSlotPort.findByRoomIdAndSlotDateBetween(roomId, today, futureDate);
					
					log.debug("Pattern-based closed date: dayOfWeek={}, pattern={}, found {} slots in range",
							range.getDayOfWeek(), range.getRecurrencePattern(), slots.size());
				} else {
					// 날짜 기반 휴무일: 해당 날짜 범위의 슬롯만 조회
					LocalDate startDate = range.getStartDate();
					LocalDate endDate = range.getEndDate() != null ? range.getEndDate() : startDate;
					
					slots = timeSlotPort.findByRoomIdAndSlotDateBetween(roomId, startDate, endDate);
					
					log.debug("Date-based closed date: {} to {}, found {} slots",
							startDate, endDate, slots.size());
				}
				
				// 휴무 범위에 해당하는 슬롯만 CLOSED로 변경
				List<RoomTimeSlot> slotsToUpdate = new ArrayList<>();
				for (RoomTimeSlot slot : slots) {
					if (range.contains(slot.getSlotDate(), slot.getSlotTime())) {
						try {
							slot.markAsClosed();
							slotsToUpdate.add(slot);
						} catch (Exception e) {
							// 이미 예약된 슬롯 등 상태 전환이 불가능한 경우 로그만 남기고 계속 진행
							log.warn("Failed to mark slot as closed: slotId={}, status={}, reason={}",
									slot.getSlotId(), slot.getStatus(), e.getMessage());
						}
					}
				}
				
				// 일괄 저장
				if (!slotsToUpdate.isEmpty()) {
					timeSlotPort.saveAll(slotsToUpdate);
					affectedSlots += slotsToUpdate.size();
				}
			}
			
			// 5. 완료 상태로 변경
			request.markAsCompleted(affectedSlots);
			closedDateUpdateRequestPort.save(request);
			
			log.info("Closed date update completed: requestId={}, affectedSlots={}",
					requestId, affectedSlots);
			
			return affectedSlots;
			
		} catch (Exception e) {
			// 6. 실패 상태로 변경
			log.error("Closed date update failed: requestId={}", requestId, e);
			
			request.markAsFailed(e.getMessage());
			closedDateUpdateRequestPort.save(request);
			
			// 예외를 다시 던지지 않음 - DLQ로 이동하지 않고 상태만 업데이트
			return 0;
		}
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
