package com.teambind.springproject.room.command.dto;

import com.teambind.springproject.room.entity.enums.RecurrencePattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * 요일별 슬롯 시작 시각 목록을 담는 DTO.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySlotDto {

	/**
	 * 요일
	 */
	private DayOfWeek dayOfWeek;

	/**
	 * 반복 패턴 (EVERY_WEEK, ODD_WEEK, EVEN_WEEK)
	 */
	private RecurrencePattern recurrencePattern;

	/**
	 * 해당 요일의 시작 시각 목록
	 */
	private List<LocalTime> startTimes;
}
