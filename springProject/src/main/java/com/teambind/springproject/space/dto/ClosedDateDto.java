package com.teambind.springproject.space.dto;

import com.teambind.springproject.space.entity.enums.RecurrencePattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 휴무일 정보를 담는 DTO.
 * <p>
 * 날짜 기반 휴무와 패턴 기반 휴무를 모두 지원한다.
 * <ul>
 *   <li>날짜 기반: startDate, endDate 사용</li>
 *   <li>패턴 기반: dayOfWeek, recurrencePattern 사용</li>
 *   <li>startTime/endTime이 null이면 하루 종일 휴무</li>
 * </ul>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClosedDateDto {

	// 날짜 기반 휴무
	/**
	 * 휴무 시작 날짜 (날짜 기반인 경우 필수)
	 */
	private LocalDate startDate;

	/**
	 * 휴무 종료 날짜 (선택 - null이면 단일 날짜)
	 */
	private LocalDate endDate;

	// 패턴 기반 휴무
	/**
	 * 요일 (패턴 기반인 경우 필수)
	 */
	private DayOfWeek dayOfWeek;

	/**
	 * 반복 패턴 (패턴 기반인 경우 필수)
	 */
	private RecurrencePattern recurrencePattern;

	// 시간 범위 (공통)
	/**
	 * 휴무 시작 시각 (선택 - null이면 하루 종일)
	 */
	private LocalTime startTime;

	/**
	 * 휴무 종료 시각 (선택 - null이면 하루 종일)
	 */
	private LocalTime endTime;
}
