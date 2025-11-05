package com.teambind.springproject.space.service.impl;

import com.teambind.springproject.common.exceptions.application.SlotGenerationFailedException;
import com.teambind.springproject.common.exceptions.domain.PolicyNotFoundException;
import com.teambind.springproject.space.entity.RoomOperatingPolicy;
import com.teambind.springproject.space.entity.RoomTimeSlot;
import com.teambind.springproject.space.entity.enums.SlotUnit;
import com.teambind.springproject.space.repository.RoomOperatingPolicyRepository;
import com.teambind.springproject.space.repository.RoomTimeSlotRepository;
import com.teambind.springproject.space.service.PlaceInfoApiClient;
import com.teambind.springproject.space.service.TimeSlotGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 시간 슬롯 생성 서비스 구현체.
 */
@Service
public class TimeSlotGenerationServiceImpl implements TimeSlotGenerationService {
	
	private static final Logger log = LoggerFactory.getLogger(TimeSlotGenerationServiceImpl.class);
	
	private final RoomTimeSlotRepository slotRepository;
	private final RoomOperatingPolicyRepository policyRepository;
	private final PlaceInfoApiClient placeInfoApiClient;
	
	public TimeSlotGenerationServiceImpl(
			RoomTimeSlotRepository slotRepository,
			RoomOperatingPolicyRepository policyRepository,
			PlaceInfoApiClient placeInfoApiClient
	) {
		this.slotRepository = slotRepository;
		this.policyRepository = policyRepository;
		this.placeInfoApiClient = placeInfoApiClient;
	}
	
	@Override
	@Transactional
	public int generateSlotsForDate(Long roomId, LocalDate date) {
		try {
			// 1. 운영 정책 조회
			RoomOperatingPolicy policy = policyRepository
					.findByRoomId(roomId)
					.orElseThrow(() -> new PolicyNotFoundException(roomId, true));

			// 2. SlotUnit 조회 (Place Info Service)
			SlotUnit slotUnit = placeInfoApiClient.getSlotUnit(roomId);

			// 3. 정책 기반 슬롯 생성
			List<RoomTimeSlot> slots = policy.generateSlotsFor(date, slotUnit);

			// 4. DB 저장 (배치 처리)
			List<RoomTimeSlot> savedSlots = slotRepository.saveAll(slots);

			log.debug("Generated {} slots for roomId={}, date={}",
					savedSlots.size(), roomId, date);

			return savedSlots.size();

		} catch (Exception e) {
			log.error("Failed to generate slots for roomId={}, date={}", roomId, date, e);
			throw SlotGenerationFailedException.forDate(date.toString(), e);
		}
	}
	
	@Override
	public int generateSlotsForDateRange(Long roomId, LocalDate startDate, LocalDate endDate) {
		int totalGenerated = 0;

		// 트랜잭션 분할: 각 날짜마다 별도 트랜잭션으로 처리
		// 60일치 데이터를 한 트랜잭션에서 처리하면 타임아웃 위험
		LocalDate currentDate = startDate;
		while (!currentDate.isAfter(endDate)) {
			totalGenerated += generateSlotsForDate(roomId, currentDate);
			currentDate = currentDate.plusDays(1);
		}

		log.info("Generated {} slots for roomId={}, dateRange=[{} to {}]",
				totalGenerated, roomId, startDate, endDate);

		return totalGenerated;
	}
	
	@Override
	public int generateSlotsForAllRooms(LocalDate date) {
		// 모든 정책 조회
		List<RoomOperatingPolicy> policies = policyRepository.findAll();
		
		int totalGenerated = 0;
		
		for (RoomOperatingPolicy policy : policies) {
			try {
				int generated = generateSlotsForDate(policy.getRoomId(), date);
				totalGenerated += generated;
			} catch (Exception e) {
				log.error("Failed to generate slots for roomId={}, date={}",
						policy.getRoomId(), date, e);
				// 한 룸 실패해도 다른 룸은 계속 처리
			}
		}
		
		log.info("Generated {} slots for all rooms on date={}", totalGenerated, date);
		
		return totalGenerated;
	}
	
	@Override
	public int deleteYesterdaySlots() {
		LocalDate yesterday = LocalDate.now().minusDays(1);
		return deleteSlotsBeforeDate(yesterday.plusDays(1)); // yesterday 포함해서 삭제
	}
	
	@Override
	@Transactional
	public int deleteSlotsBeforeDate(LocalDate beforeDate) {
		int deletedCount = slotRepository.deleteBySlotDateBefore(beforeDate);

		log.info("Deleted {} slots before date={}", deletedCount, beforeDate);

		return deletedCount;
	}
	
	@Override
	public int regenerateFutureSlots(Long roomId) {
		try {
			// 1. 미래 슬롯 삭제 (오늘 이후)
			LocalDate today = LocalDate.now();
			List<RoomTimeSlot> futureSlots = slotRepository
					.findAll()
					.stream()
					.filter(slot -> slot.getRoomId().equals(roomId))
					.filter(slot -> !slot.getSlotDate().isBefore(today))
					.toList();
			
			slotRepository.deleteAll(futureSlots);
			
			log.info("Deleted {} future slots for roomId={}", futureSlots.size(), roomId);
			
			// 2. 60일치 슬롯 재생성
			LocalDate endDate = today.plusDays(60);
			int regenerated = generateSlotsForDateRange(roomId, today, endDate);
			
			log.info("Regenerated {} slots for roomId={}", regenerated, roomId);
			
			return regenerated;
			
		} catch (Exception e) {
			log.error("Failed to regenerate future slots for roomId={}", roomId, e);
			throw SlotGenerationFailedException.forRoom(roomId, e);
		}
	}
}
