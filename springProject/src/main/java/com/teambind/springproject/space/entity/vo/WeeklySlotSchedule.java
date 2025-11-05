package com.teambind.springproject.space.entity.vo;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 주간 운영 시간 스케줄을 나타내는 Value Object.
 *
 *  요일별 슬롯 시작 시각 목록을 관리하며, 특정 요일의 시작 시각을 조회하는 기능을 제공한다.
 */
@Embeddable
public class WeeklySlotSchedule {
	
	@ElementCollection
	@CollectionTable(name = "weekly_slot_times", joinColumns = @JoinColumn(name = "policy_id"))
	private List<WeeklySlotTime> slotTimes = new ArrayList<>();
	
	protected WeeklySlotSchedule() {
		// JPA를 위한 기본 생성자
	}
	
	private WeeklySlotSchedule(List<WeeklySlotTime> slotTimes) {
		Objects.requireNonNull(slotTimes, "slotTimes must not be null");
		this.slotTimes = new ArrayList<>(slotTimes);
	}
	
	/**
	 * WeeklySlotSchedule 인스턴스를 생성한다.
	 *
	 * @param slotTimes 요일별 슬롯 시작 시각 목록
	 * @return 생성된 WeeklySlotSchedule
	 */
	public static WeeklySlotSchedule of(List<WeeklySlotTime> slotTimes) {
		return new WeeklySlotSchedule(slotTimes);
	}
	
	/**
	 * 특정 요일의 슬롯 시작 시각 목록을 반환한다.
	 *
	 * <p>반환된 목록은 시간순으로 정렬되며, 중복이 제거되고, 수정 불가능한 리스트이다.
	 *
	 * @param dayOfWeek 조회할 요일
	 * @return 해당 요일의 시작 시각 목록 (정렬됨, 중복 제거됨)
	 * @throws NullPointerException dayOfWeek가 null인 경우
	 */
	public List<LocalTime> getStartTimesFor(DayOfWeek dayOfWeek) {
		Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
		return slotTimes.stream()
				.filter(slot -> slot.getDayOfWeek() == dayOfWeek)
				.map(WeeklySlotTime::getStartTime)
				.distinct() // 중복 제거
				.sorted()
				.collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
	}
	
	/**
	 * 모든 슬롯 시간 정보를 반환한다.
	 *
	 * @return 수정 불가능한 슬롯 시간 목록
	 */
	public List<WeeklySlotTime> getSlotTimes() {
		return Collections.unmodifiableList(slotTimes);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof WeeklySlotSchedule)) {
			return false;
		}
		WeeklySlotSchedule that = (WeeklySlotSchedule) o;
		return Objects.equals(slotTimes, that.slotTimes);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(slotTimes);
	}
}
