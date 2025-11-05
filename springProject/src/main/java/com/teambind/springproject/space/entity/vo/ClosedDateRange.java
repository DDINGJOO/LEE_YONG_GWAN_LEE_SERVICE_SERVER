package com.teambind.springproject.space.entity.vo;

import com.teambind.springproject.common.exceptions.application.InvalidTimeRangeException;
import com.teambind.springproject.space.entity.enums.RecurrencePattern;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 휴무일 범위를 나타내는 Value Object.
 *
 * <p>특정 날짜 또는 날짜 범위의 휴무를 표현하며, 선택적으로 시간 범위도 지정할 수 있다.
 * <p>또한 반복 패턴 기반 휴무일도 지원한다 (예: 매주 월요일 09:00~10:00 휴무).
 *
 * <ul>
 *   <li>날짜 기반 휴무: startDate, endDate 사용</li>
 *   <li>패턴 기반 휴무: dayOfWeek, recurrencePattern 사용</li>
 *   <li>startTime/endTime이 null이면 하루 종일 휴무</li>
 *   <li>startTime/endTime이 있으면 해당 시간 범위만 휴무</li>
 * </ul>
 */
@Embeddable
public class ClosedDateRange {

	// 날짜 기반 휴무
	private LocalDate startDate;

	private LocalDate endDate;

	// 패턴 기반 휴무
	@Enumerated(EnumType.STRING)
	private DayOfWeek dayOfWeek;

	@Enumerated(EnumType.STRING)
	private RecurrencePattern recurrencePattern;

	// 시간 범위 (공통)
	private LocalTime startTime;

	private LocalTime endTime;
	
	protected ClosedDateRange() {
		// JPA를 위한 기본 생성자
	}

	// 날짜 기반 휴무 생성자
	private ClosedDateRange(
			LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime) {
		this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
		this.endDate = endDate; // nullable
		this.startTime = startTime; // nullable
		this.endTime = endTime; // nullable
		this.dayOfWeek = null;
		this.recurrencePattern = null;

		if (endDate != null && endDate.isBefore(startDate)) {
			throw InvalidTimeRangeException.endDateBeforeStartDate(
					startDate.toString(), endDate.toString());
		}

		if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
			throw InvalidTimeRangeException.endBeforeStart(
					startTime.toString(), endTime.toString());
		}
	}

	// 패턴 기반 휴무 생성자
	private ClosedDateRange(
			DayOfWeek dayOfWeek,
			RecurrencePattern recurrencePattern,
			LocalTime startTime,
			LocalTime endTime) {
		this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
		this.recurrencePattern = Objects.requireNonNull(recurrencePattern, "recurrencePattern must not be null");
		this.startTime = startTime; // nullable
		this.endTime = endTime; // nullable
		this.startDate = null;
		this.endDate = null;

		if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
			throw InvalidTimeRangeException.endBeforeStart(
					startTime.toString(), endTime.toString());
		}
	}
	
	/**
	 * 하루 종일 휴무인 단일 날짜를 생성한다.
	 *
	 * @param date 휴무 날짜
	 * @return 생성된 ClosedDateRange
	 */
	public static ClosedDateRange ofFullDay(LocalDate date) {
		return new ClosedDateRange(date, null, null, null);
	}
	
	/**
	 * 하루 종일 휴무인 날짜 범위를 생성한다.
	 *
	 * @param startDate 시작 날짜
	 * @param endDate   종료 날짜
	 * @return 생성된 ClosedDateRange
	 */
	public static ClosedDateRange ofDateRange(LocalDate startDate, LocalDate endDate) {
		return new ClosedDateRange(startDate, endDate, null, null);
	}
	
	/**
	 * 특정 시간 범위가 휴무인 날짜를 생성한다.
	 *
	 * @param date      휴무 날짜
	 * @param startTime 휴무 시작 시각
	 * @param endTime   휴무 종료 시각
	 * @return 생성된 ClosedDateRange
	 */
	public static ClosedDateRange ofTimeRange(
			LocalDate date, LocalTime startTime, LocalTime endTime) {
		return new ClosedDateRange(date, null, startTime, endTime);
	}

	/**
	 * 패턴 기반 하루 종일 휴무를 생성한다.
	 * (예: 매주 월요일 종일 휴무)
	 *
	 * @param dayOfWeek         요일
	 * @param recurrencePattern 반복 패턴
	 * @return 생성된 ClosedDateRange
	 */
	public static ClosedDateRange ofPatternFullDay(
			DayOfWeek dayOfWeek,
			RecurrencePattern recurrencePattern) {
		return new ClosedDateRange(dayOfWeek, recurrencePattern, null, null);
	}

	/**
	 * 패턴 기반 특정 시간 범위 휴무를 생성한다.
	 * (예: 매주 월요일 09:00~10:00 휴무)
	 *
	 * @param dayOfWeek         요일
	 * @param recurrencePattern 반복 패턴
	 * @param startTime         휴무 시작 시각
	 * @param endTime           휴무 종료 시각
	 * @return 생성된 ClosedDateRange
	 */
	public static ClosedDateRange ofPatternTimeRange(
			DayOfWeek dayOfWeek,
			RecurrencePattern recurrencePattern,
			LocalTime startTime,
			LocalTime endTime) {
		return new ClosedDateRange(dayOfWeek, recurrencePattern, startTime, endTime);
	}
	
	/**
	 * 주어진 날짜가 이 휴무 범위에 포함되는지 확인한다.
	 * <p>
	 * 패턴 기반인 경우 요일과 반복 패턴을 체크하고,
	 * 날짜 기반인 경우 날짜 범위를 체크한다.
	 *
	 * @param date 확인할 날짜
	 * @return 휴무 범위에 포함되면 true, 아니면 false
	 */
	public boolean containsDate(LocalDate date) {
		// 패턴 기반 휴무일 체크
		if (isPatternBased()) {
			return date.getDayOfWeek() == dayOfWeek
					&& recurrencePattern.matches(date);
		}

		// 날짜 기반 휴무일 체크
		LocalDate effectiveEndDate = endDate != null ? endDate : startDate;
		return !date.isBefore(startDate) && !date.isAfter(effectiveEndDate);
	}
	
	/**
	 * 주어진 시각이 이 휴무 시간 범위에 포함되는지 확인한다.
	 *
	 * startTime/endTime이 null이면 하루 종일 휴무로 간주하여 항상 true를 반환한다.
	 *
	 * @param time 확인할 시각
	 * @return 휴무 시간 범위에 포함되면 true, 아니면 false
	 */
	public boolean containsTime(LocalTime time) {
		if (startTime == null || endTime == null) {
			return true; // 하루 종일 휴무
		}
		return !time.isBefore(startTime) && !time.isAfter(endTime);
	}
	
	/**
	 * 주어진 날짜와 시각이 이 휴무 범위에 포함되는지 확인한다.
	 *
	 * @param date 확인할 날짜
	 * @param time 확인할 시각
	 * @return 휴무 범위에 포함되면 true, 아니면 false
	 */
	public boolean contains(LocalDate date, LocalTime time) {
		return containsDate(date) && containsTime(time);
	}

	/**
	 * 패턴 기반 휴무일인지 확인한다.
	 *
	 * @return 패턴 기반이면 true, 날짜 기반이면 false
	 */
	public boolean isPatternBased() {
		return dayOfWeek != null && recurrencePattern != null;
	}

	// Getters
	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}

	public RecurrencePattern getRecurrencePattern() {
		return recurrencePattern;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ClosedDateRange)) {
			return false;
		}
		ClosedDateRange that = (ClosedDateRange) o;
		return Objects.equals(startDate, that.startDate)
				&& Objects.equals(endDate, that.endDate)
				&& dayOfWeek == that.dayOfWeek
				&& recurrencePattern == that.recurrencePattern
				&& Objects.equals(startTime, that.startTime)
				&& Objects.equals(endTime, that.endTime);
	}

	@Override
	public int hashCode() {
		return Objects.hash(startDate, endDate, dayOfWeek, recurrencePattern, startTime, endTime);
	}
}
