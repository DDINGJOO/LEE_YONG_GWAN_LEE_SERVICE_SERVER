package com.teambind.springproject.space.entity.vo;

import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 휴무일 범위를 나타내는 Value Object.
 *
 * 특정 날짜 또는 날짜 범위의 휴무를 표현하며, 선택적으로 시간 범위도 지정할 수 있다.
 *
 *
 *   endDate가 null이면 단일 날짜 휴무
 *   startTime/endTime이 null이면 하루 종일 휴무
 *   startTime/endTime이 있으면 해당 시간 범위만 휴무
 *
 */
@Embeddable
public class ClosedDateRange {
	
	private LocalDate startDate;
	
	private LocalDate endDate;
	
	private LocalTime startTime;
	
	private LocalTime endTime;
	
	protected ClosedDateRange() {
		// JPA를 위한 기본 생성자
	}
	
	private ClosedDateRange(
			LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime) {
		this.startDate = Objects.requireNonNull(startDate, "startDate must not be null");
		this.endDate = endDate; // nullable
		this.startTime = startTime; // nullable
		this.endTime = endTime; // nullable
		
		if (endDate != null && endDate.isBefore(startDate)) {
			throw new IllegalArgumentException("endDate must not be before startDate");
		}
		
		if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
			throw new IllegalArgumentException("endTime must not be before startTime");
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
	 * 주어진 날짜가 이 휴무 범위에 포함되는지 확인한다.
	 *
	 * @param date 확인할 날짜
	 * @return 휴무 범위에 포함되면 true, 아니면 false
	 */
	public boolean containsDate(LocalDate date) {
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
	
	public LocalDate getStartDate() {
		return startDate;
	}
	
	public LocalDate getEndDate() {
		return endDate;
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
				&& Objects.equals(startTime, that.startTime)
				&& Objects.equals(endTime, that.endTime);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(startDate, endDate, startTime, endTime);
	}
}
