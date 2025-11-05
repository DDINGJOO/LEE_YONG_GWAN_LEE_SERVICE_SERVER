package com.teambind.springproject.space.service.impl;

import com.teambind.springproject.space.entity.RoomTimeSlot;
import com.teambind.springproject.space.entity.enums.SlotStatus;
import com.teambind.springproject.space.repository.RoomTimeSlotRepository;
import com.teambind.springproject.space.service.TimeSlotQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 시간 슬롯 조회 서비스 구현체.
 */
@Service
@Transactional(readOnly = true)
public class TimeSlotQueryServiceImpl implements TimeSlotQueryService {
	
	private final RoomTimeSlotRepository slotRepository;
	
	public TimeSlotQueryServiceImpl(RoomTimeSlotRepository slotRepository) {
		this.slotRepository = slotRepository;
	}
	
	@Override
	public List<RoomTimeSlot> getSlotsByDateRange(
			Long roomId,
			LocalDate startDate,
			LocalDate endDate
	) {
		return slotRepository.findByRoomIdAndSlotDateBetween(roomId, startDate, endDate);
	}
	
	@Override
	public List<RoomTimeSlot> getAvailableSlots(Long roomId, LocalDate date) {
		return slotRepository.findByRoomIdAndSlotDateAndStatus(
				roomId,
				date,
				SlotStatus.AVAILABLE
		);
	}
	
	@Override
	public boolean isSlotAvailable(Long roomId, LocalDate slotDate, LocalTime slotTime) {
		return slotRepository
				.findByRoomIdAndSlotDateAndSlotTime(roomId, slotDate, slotTime)
				.map(RoomTimeSlot::isAvailable)
				.orElse(false);
	}
	
	@Override
	public long countAvailableSlots(Long roomId, LocalDate startDate, LocalDate endDate) {
		return slotRepository.countByRoomIdAndDateRangeAndStatus(
				roomId,
				startDate,
				endDate,
				SlotStatus.AVAILABLE
		);
	}
	
	@Override
	public List<RoomTimeSlot> getAllSlotsForDate(Long roomId, LocalDate date) {
		return slotRepository.findByRoomIdAndSlotDateBetween(roomId, date, date);
	}
	
	@Override
	public List<RoomTimeSlot> getSlotsByStatus(
			Long roomId,
			LocalDate date,
			SlotStatus status
	) {
		return slotRepository.findByRoomIdAndSlotDateAndStatus(roomId, date, status);
	}
}
