package com.teambind.springproject.room.command.domain.service;

import com.teambind.springproject.room.entity.enums.SlotUnit;
import com.teambind.springproject.room.entity.vo.WeeklySlotSchedule;

/**
 * 운영 시간 업데이트 서비스.
 * <p>
 * 주요 책임:
 * <p>
 * RoomOperatingPolicy 업데이트 (WeeklySchedule, SlotUnit)
 * AVAILABLE 슬롯 삭제 및 새 운영 시간 기준 슬롯 재생성
 * CLOSED, RESERVED, PENDING 슬롯은 유지
 */
public interface OperatingHoursUpdateService {

	/**
	 * 운영 시간 업데이트를 요청한다.
	 * <p>
	 * 동기적으로 RoomOperatingPolicy를 업데이트하고,
	 * 비동기로 슬롯 재생성 작업을 시작한다.
	 *
	 * @param roomId          룸 ID
	 * @param newSchedule     새로운 주간 스케줄
	 * @param newSlotUnit     새로운 슬롯 단위
	 * @return 요청 ID (SlotGenerationRequest의 requestId)
	 */
	String requestOperatingHoursUpdate(Long roomId, WeeklySlotSchedule newSchedule, SlotUnit newSlotUnit);

	/**
	 * 비동기로 슬롯을 재생성한다.
	 * <p>
	 * 처리 과정:
	 * 1. 오늘 이후 AVAILABLE 슬롯 삭제
	 * 2. 새 운영 시간 기준으로 슬롯 생성
	 * 3. 기존 CLOSED/RESERVED/PENDING 시간대는 생성에서 제외
	 *
	 * @param requestId 요청 ID
	 * @param roomId    룸 ID
	 */
	void regenerateSlotsAsync(String requestId, Long roomId);
}
