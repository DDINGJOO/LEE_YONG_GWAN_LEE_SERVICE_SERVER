package com.teambind.springproject.space.entity.vo;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 특정 요일의 슬롯 시작 시각을 나타내는 Value Object.
 *
 *  불변 객체로 설계되어 thread-safe하며, equals/hashCode는 모든 필드 기반으로 판단한다.
 */
@Embeddable
public class WeeklySlotTime {
	
	@Enumerated(EnumType.STRING)
	private DayOfWeek dayOfWeek;
	
	private LocalTime startTime;
	
	protected WeeklySlotTime() {
		// JPA를 위한 기본 생성자
	}
	
	private WeeklySlotTime(DayOfWeek dayOfWeek, LocalTime startTime) {
		this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
		this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
	}
	
	/**
	 * WeeklySlotTime 인스턴스를 생성한다.
	 *
	 * @param dayOfWeek 요일
	 * @param startTime 시작 시각
	 * @return 생성된 WeeklySlotTime
	 * @throws NullPointerException dayOfWeek 또는 startTime이 null인 경우
	 */
	public static WeeklySlotTime of(DayOfWeek dayOfWeek, LocalTime startTime) {
		return new WeeklySlotTime(dayOfWeek, startTime);
	}
	
	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}
	
	public LocalTime getStartTime() {
		return startTime;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof WeeklySlotTime)) {
			return false;
		}
		WeeklySlotTime that = (WeeklySlotTime) o;
		return dayOfWeek == that.dayOfWeek && Objects.equals(startTime, that.startTime);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(dayOfWeek, startTime);
	}
	
	@Override
	public String toString() {
		return String.format("%s %s", dayOfWeek, startTime);
	}
}
