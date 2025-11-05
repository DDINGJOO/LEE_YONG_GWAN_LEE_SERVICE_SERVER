package com.teambind.springproject.space.service;

import com.teambind.springproject.space.entity.RoomTimeSlot;
import com.teambind.springproject.space.entity.enums.SlotStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 시간 슬롯 조회 서비스.
 * 주요 책임:
 *   날짜/시간별 슬롯 조회
 *   예약 가능한 슬롯 필터링
 *   슬롯 가용성 체크
 *
 */
@Service
public interface TimeSlotQueryService {
	
	/**
	 * Room ID와 날짜 범위로 슬롯 목록을 조회한다.
	 *
	 * @param roomId    룸 ID
	 * @param startDate 시작 날짜
	 * @param endDate   종료 날짜
	 * @return 조회된 슬롯 목록
	 */
	List<RoomTimeSlot> getSlotsByDateRange(Long roomId, LocalDate startDate, LocalDate endDate);
	
	/**
	 * Room ID와 특정 날짜의 예약 가능한 슬롯 목록을 조회한다.
	 *
	 * @param roomId 룸 ID
	 * @param date   조회 날짜
	 * @return AVAILABLE 상태의 슬롯 목록
	 */
	List<RoomTimeSlot> getAvailableSlots(Long roomId, LocalDate date);
	
	/**
	 * 특정 슬롯이 예약 가능한지 확인한다.
	 *
	 * @param roomId   룸 ID
	 * @param slotDate 슬롯 날짜
	 * @param slotTime 슬롯 시간
	 * @return 예약 가능하면 true, 아니면 false
	 */
	boolean isSlotAvailable(Long roomId, LocalDate slotDate, LocalTime slotTime);
	
	/**
	 * Room ID와 날짜 범위의 예약 가능한 슬롯 개수를 조회한다.
	 *
	 * @param roomId    룸 ID
	 * @param startDate 시작 날짜
	 * @param endDate   종료 날짜
	 * @return 예약 가능한 슬롯 개수
	 */
	long countAvailableSlots(Long roomId, LocalDate startDate, LocalDate endDate);
	
	/**
	 * Room ID와 날짜로 모든 상태의 슬롯을 조회한다.
	 *
	 * @param roomId 룸 ID
	 * @param date   조회 날짜
	 * @return 슬롯 목록 (모든 상태)
	 */
	List<RoomTimeSlot> getAllSlotsForDate(Long roomId, LocalDate date);
	
	/**
	 * Room ID와 날짜, 상태로 슬롯 목록을 조회한다.
	 *
	 * @param roomId 룸 ID
	 * @param date   조회 날짜
	 * @param status 슬롯 상태
	 * @return 조회된 슬롯 목록
	 */
	List<RoomTimeSlot> getSlotsByStatus(Long roomId, LocalDate date, SlotStatus status);
}
