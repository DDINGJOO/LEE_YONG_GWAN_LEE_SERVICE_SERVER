package com.teambind.springproject.space.service;

import com.teambind.springproject.space.entity.RoomOperatingPolicy;
import com.teambind.springproject.space.entity.enums.RecurrencePattern;
import com.teambind.springproject.space.entity.vo.ClosedDateRange;
import com.teambind.springproject.space.entity.vo.WeeklySlotSchedule;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 룸 운영 정책 관리 서비스.
 *
 * 주요 책임:
 *
 *
 *  정책 등록 및 초기 슬롯 생성
 *   정책 조회 및 수정
 *   휴무일 관리
 *
 */
@Service
public interface RoomOperatingPolicyService {
	
	/**
	 * 새로운 룸 운영 정책을 등록하고 2달치 슬롯을 생성한다.
	 *
	 * <p>처리 플로우:
	 *
	 * <ol>
	 *   <li>RoomOperatingPolicy 저장
	 *   <li>Place Info API 호출 → SlotUnit 조회
	 *   <li>오늘부터 60일치 슬롯 배치 생성
	 * </ol>
	 *
	 * @param roomId         룸 ID
	 * @param weeklySchedule 주간 운영 시간 스케줄
	 * @param recurrence     반복 패턴
	 * @param closedDates    휴무일 목록
	 * @return 생성된 정책
	 */
	RoomOperatingPolicy registerPolicy(
			Long roomId,
			WeeklySlotSchedule weeklySchedule,
			RecurrencePattern recurrence,
			List<ClosedDateRange> closedDates);
	
	/**
	 * Room ID로 운영 정책을 조회한다.
	 *
	 * @param roomId 룸 ID
	 * @return 조회된 정책
	 * @throws IllegalArgumentException 정책이 존재하지 않는 경우
	 */
	RoomOperatingPolicy getPolicyByRoomId(Long roomId);
	
	/**
	 * 주간 운영 시간을 업데이트하고 미래 슬롯을 재생성한다.
	 *
	 * <p>처리 플로우:
	 *
	 * <ol>
	 *   <li>정책의 weeklySchedule 업데이트
	 *   <li>오늘 이후의 기존 슬롯 삭제
	 *   <li>새 정책으로 슬롯 재생성
	 * </ol>
	 *
	 * @param roomId      룸 ID
	 * @param newSchedule 새로운 주간 스케줄
	 */
	void updateWeeklySchedule(Long roomId, WeeklySlotSchedule newSchedule);
	
	/**
	 * 반복 패턴을 업데이트하고 미래 슬롯을 재생성한다.
	 *
	 * @param roomId        룸 ID
	 * @param newRecurrence 새로운 반복 패턴
	 */
	void updateRecurrence(Long roomId, RecurrencePattern newRecurrence);
	
	/**
	 * 휴무일을 추가하고 해당 날짜의 슬롯을 CLOSED 상태로 변경한다.
	 *
	 * @param roomId          룸 ID
	 * @param closedDateRange 추가할 휴무일 범위
	 */
	void addClosedDate(Long roomId, ClosedDateRange closedDateRange);
	
	/**
	 * 휴무일을 제거하고 해당 날짜의 슬롯을 재생성한다.
	 *
	 * @param roomId          룸 ID
	 * @param closedDateRange 제거할 휴무일 범위
	 */
	void removeClosedDate(Long roomId, ClosedDateRange closedDateRange);
	
	/**
	 * 룸 운영 정책을 삭제하고 모든 슬롯을 제거한다.
	 *
	 * @param roomId 룸 ID
	 */
	void deletePolicy(Long roomId);
}
