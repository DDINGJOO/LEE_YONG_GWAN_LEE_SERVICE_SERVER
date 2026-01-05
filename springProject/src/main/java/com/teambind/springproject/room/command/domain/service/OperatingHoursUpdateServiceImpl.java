package com.teambind.springproject.room.command.domain.service;

import com.teambind.springproject.common.exceptions.domain.PolicyNotFoundException;
import com.teambind.springproject.room.domain.port.OperatingPolicyPort;
import com.teambind.springproject.room.domain.port.TimeSlotPort;
import com.teambind.springproject.room.entity.RoomOperatingPolicy;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.SlotGenerationRequest;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import com.teambind.springproject.room.entity.enums.SlotUnit;
import com.teambind.springproject.room.entity.vo.WeeklySlotSchedule;
import com.teambind.springproject.room.repository.SlotGenerationRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 운영 시간 업데이트 서비스 구현체.
 */
@Service
public class OperatingHoursUpdateServiceImpl implements OperatingHoursUpdateService {

	private static final Logger log = LoggerFactory.getLogger(OperatingHoursUpdateServiceImpl.class);

	private final TimeSlotPort timeSlotPort;
	private final OperatingPolicyPort operatingPolicyPort;
	private final SlotGenerationRequestRepository requestRepository;

	@Value("${room.timeSlot.rollingWindow.days:30}")
	private int rollingWindowDays;

	public OperatingHoursUpdateServiceImpl(
			TimeSlotPort timeSlotPort,
			OperatingPolicyPort operatingPolicyPort,
			SlotGenerationRequestRepository requestRepository
	) {
		this.timeSlotPort = timeSlotPort;
		this.operatingPolicyPort = operatingPolicyPort;
		this.requestRepository = requestRepository;
	}

	@Override
	@Transactional
	public String requestOperatingHoursUpdate(Long roomId, WeeklySlotSchedule newSchedule, SlotUnit newSlotUnit) {
		// 1. 정책 조회 및 업데이트
		RoomOperatingPolicy policy = operatingPolicyPort
				.findByRoomId(roomId)
				.orElseThrow(() -> new PolicyNotFoundException(roomId, true));

		policy.updateOperatingHours(newSchedule, newSlotUnit);
		operatingPolicyPort.save(policy);

		// 2. 요청 생성
		LocalDate today = LocalDate.now();
		LocalDate endDate = today.plusDays(rollingWindowDays);
		String requestId = UUID.randomUUID().toString();

		SlotGenerationRequest request = SlotGenerationRequest.create(
				requestId,
				roomId,
				today,
				endDate
		);
		requestRepository.save(request);

		log.info("Operating hours update requested: roomId={}, requestId={}", roomId, requestId);

		// 3. 비동기 작업 시작
		regenerateSlotsAsync(requestId, roomId);

		return requestId;
	}

	@Override
	@Async
	@Transactional
	public void regenerateSlotsAsync(String requestId, Long roomId) {
		SlotGenerationRequest request = requestRepository.findById(requestId)
				.orElseThrow(() -> new IllegalStateException("Request not found: " + requestId));

		try {
			request.markAsInProgress();
			requestRepository.save(request);

			LocalDate today = LocalDate.now();
			LocalDate endDate = today.plusDays(rollingWindowDays);

			// 1. 기존 슬롯 중 CLOSED, RESERVED, PENDING 시간대 수집
			List<RoomTimeSlot> existingSlots = timeSlotPort.findByRoomIdAndSlotDateBetween(roomId, today, endDate);
			Set<String> preservedSlotKeys = existingSlots.stream()
					.filter(slot -> slot.getStatus() != SlotStatus.AVAILABLE)
					.map(slot -> slotKey(slot.getSlotDate(), slot.getSlotTime()))
					.collect(Collectors.toSet());

			// 2. AVAILABLE 슬롯만 삭제
			int deletedCount = timeSlotPort.deleteAvailableSlotsByRoomIdAndDateRange(roomId, today, endDate);
			log.info("Deleted {} AVAILABLE slots for roomId={}", deletedCount, roomId);

			// 3. 새 운영 시간 기준으로 슬롯 생성 (보존된 시간대 제외)
			RoomOperatingPolicy policy = operatingPolicyPort.findByRoomId(roomId)
					.orElseThrow(() -> new PolicyNotFoundException(roomId, true));

			int createdCount = 0;
			LocalDate currentDate = today;
			while (!currentDate.isAfter(endDate)) {
				List<RoomTimeSlot> newSlots = policy.generateSlotsFor(currentDate, policy.getSlotUnit());

				// 보존된 시간대와 겹치는 슬롯은 제외
				LocalDate finalCurrentDate = currentDate;
				List<RoomTimeSlot> filteredSlots = newSlots.stream()
						.filter(slot -> !preservedSlotKeys.contains(slotKey(finalCurrentDate, slot.getSlotTime())))
						.collect(Collectors.toList());

				if (!filteredSlots.isEmpty()) {
					timeSlotPort.saveAll(filteredSlots);
					createdCount += filteredSlots.size();
				}

				currentDate = currentDate.plusDays(1);
			}

			log.info("Operating hours update completed: roomId={}, deleted={}, created={}",
					roomId, deletedCount, createdCount);

			// 완료 후 요청 삭제
			requestRepository.delete(request);

		} catch (Exception e) {
			log.error("Failed to regenerate slots for roomId={}", roomId, e);
			// 실패 시에도 요청 삭제
			requestRepository.delete(request);
			throw e;
		}
	}

	private String slotKey(LocalDate date, LocalTime time) {
		return date.toString() + "_" + time.toString();
	}
}
