package com.teambind.springproject.room.command.domain.service;

import java.time.LocalDate;

/**
 * 시간 슬롯 생성 서비스.
 * <p>
 * 주요 책임:
 * <p>
 * <p>
 * 정책 기반 슬롯 배치 생성
 * Rolling Window 유지 (매일 배치)
 * SlotUnit 조회 및 슬롯 생성 로직
 *
 */

public interface TimeSlotGenerationService {
	
	/**
	 * 특정 룸의 특정 날짜에 대한 슬롯을 생성한다.
	 * <p>
	 * 처리 플로우:
	 * <p>
	 * <p>
	 * RoomOperatingPolicy 조회
	 * Place Info API에서 SlotUnit 조회
	 * 정책 기반으로 슬롯 생성 (Policy.generateSlotsFor 호출)
	 * DB에 배치 저장
	 *
	 * @param roomId 룸 ID
	 * @param date   슬롯을 생성할 날짜
	 * @return 생성된 슬롯 개수
	 */
	int generateSlotsForDate(Long roomId, LocalDate date);
	
	/**
	 * 특정 룸의 날짜 범위에 대한 슬롯을 생성한다.
	 *
	 * @param roomId    룸 ID
	 * @param startDate 시작 날짜
	 * @param endDate   종료 날짜
	 * @return 생성된 슬롯 개수
	 */
	int generateSlotsForDateRange(Long roomId, LocalDate startDate, LocalDate endDate);
	
	/**
	 * 모든 룸의 특정 날짜에 대한 슬롯을 생성한다.
	 * <p>
	 * 매일 배치 작업에서 호출된다.
	 *
	 * @param date 슬롯을 생성할 날짜
	 * @return 생성된 슬롯 개수
	 */
	int generateSlotsForAllRooms(LocalDate date);
	
	/**
	 * 어제 날짜의 슬롯을 삭제한다.
	 * <p>
	 * Rolling Window 유지를 위해 매일 배치 작업에서 호출된다.
	 *
	 * @return 삭제된 슬롯 개수
	 */
	int deleteYesterdaySlots();
	
	/**
	 * 특정 날짜 이전의 모든 슬롯을 삭제한다.
	 *
	 * @param beforeDate 기준 날짜 (exclusive)
	 * @return 삭제된 슬롯 개수
	 */
	int deleteSlotsBeforeDate(LocalDate beforeDate);
	
	/**
	 * 특정 룸의 미래 슬롯을 재생성한다.
	 * <p>
	 * 정책 변경 시 호출된다.
	 *
	 * @param roomId 룸 ID
	 * @return 재생성된 슬롯 개수
	 */
	int regenerateFutureSlots(Long roomId);
	
	/**
	 * 특정 룸의 오늘부터 30일 이후까지 슬롯을 확인하고, 없는 날짜에만 생성한다.
	 * <p>
	 * 이미 슬롯이 존재하는 날짜는 건너뛰고, 슬롯이 없는 날짜에만 새로 생성한다.
	 * Room이 새로 등록되거나 슬롯이 누락된 경우 보완하는 용도로 사용된다.
	 *
	 * @param roomId 룸 ID
	 * @return 새로 생성된 슬롯 개수
	 */
	int ensureSlotsForNext30Days(Long roomId);
}
