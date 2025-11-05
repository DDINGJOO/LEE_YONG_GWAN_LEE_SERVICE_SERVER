package com.teambind.springproject.space.entity;

import com.teambind.springproject.space.entity.enums.RecurrencePattern;
import com.teambind.springproject.space.entity.enums.SlotUnit;
import com.teambind.springproject.space.entity.vo.ClosedDateRange;
import com.teambind.springproject.space.entity.vo.WeeklySlotSchedule;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 룸의 운영 시간 정책을 나타내는 Aggregate Root.
 *
 * <p>이 엔티티는 다음 책임을 가진다:
 *
 * <ul>
 *   <li>요일별 운영 시간 슬롯 관리
 *   <li>반복 패턴 관리 (매주, 홀수주, 짝수주)
 *   <li>휴무일 예외 관리
 *   <li>특정 날짜에 슬롯 생성 여부 판단
 *   <li>정책 기반 시간 슬롯 생성
 * </ul>
 */
@Entity
@Table(name = "room_operating_policies")
public class RoomOperatingPolicy {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long policyId;
	
	@Column(name = "room_id", nullable = false, unique = true)
	private Long roomId;
	
	@Embedded
	private WeeklySlotSchedule weeklySchedule;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RecurrencePattern recurrence;
	
	@ElementCollection
	@CollectionTable(name = "policy_closed_dates", joinColumns = @JoinColumn(name = "policy_id"))
	private List<ClosedDateRange> closedDates = new ArrayList<>();
	
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
	
	protected RoomOperatingPolicy() {
		// JPA를 위한 기본 생성자
	}
	
	private RoomOperatingPolicy(
			Long roomId,
			WeeklySlotSchedule weeklySchedule,
			RecurrencePattern recurrence,
			List<ClosedDateRange> closedDates) {
		this.roomId = Objects.requireNonNull(roomId, "roomId must not be null");
		this.weeklySchedule =
				Objects.requireNonNull(weeklySchedule, "weeklySchedule must not be null");
		this.recurrence = Objects.requireNonNull(recurrence, "recurrence must not be null");
		this.closedDates = new ArrayList<>(closedDates);
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}
	
	/**
	 * RoomOperatingPolicy 인스턴스를 생성한다.
	 *
	 * @param roomId         룸 ID
	 * @param weeklySchedule 주간 운영 시간 스케줄
	 * @param recurrence     반복 패턴
	 * @param closedDates    휴무일 목록
	 * @return 생성된 RoomOperatingPolicy
	 */
	public static RoomOperatingPolicy create(
			Long roomId,
			WeeklySlotSchedule weeklySchedule,
			RecurrencePattern recurrence,
			List<ClosedDateRange> closedDates) {
		return new RoomOperatingPolicy(roomId, weeklySchedule, recurrence, closedDates);
	}
	
	/**
	 * 특정 날짜에 슬롯을 생성해야 하는지 판단한다.
	 *
	 * 다음 조건을 모두 만족해야 슬롯을 생성한다:
	 *
	 *
	 *   반복 패턴과 일치
	 *   휴무일이 아님
	 *
	 *
	 * @param date 확인할 날짜
	 * @return 슬롯 생성이 필요하면 true, 아니면 false
	 */
	public boolean shouldGenerateSlotsOn(LocalDate date) {
		if (!recurrence.matches(date)) {
			return false;
		}
		return !isFullDayClosedOn(date);
	}
	
	/**
	 * 특정 날짜와 시각이 휴무인지 확인한다.
	 *
	 * @param date 확인할 날짜
	 * @param time 확인할 시각
	 * @return 휴무이면 true, 아니면 false
	 */
	public boolean isClosedAt(LocalDate date, LocalTime time) {
		return closedDates.stream().anyMatch(range -> range.contains(date, time));
	}
	
	/**
	 * 특정 날짜가 하루 종일 휴무인지 확인한다.
	 *
	 * @param date 확인할 날짜
	 * @return 하루 종일 휴무이면 true, 아니면 false
	 */
	private boolean isFullDayClosedOn(LocalDate date) {
		return closedDates.stream()
				.anyMatch(
						range ->
								range.containsDate(date)
										&& range.getStartTime() == null
										&& range.getEndTime() == null);
	}
	
	/**
	 * 특정 날짜에 대한 시간 슬롯을 생성한다.
	 *
	 *
	 * 생성 로직:
	 *
	 *   해당 날짜의 요일을 확인
	 *   요일별 시작 시각 목록 조회
	 *   각 시작 시각에서 SlotUnit 간격으로 슬롯 생성
	 *   휴무 시간은 CLOSED 상태로 생성
	 *
	 *
	 * @param date     슬롯을 생성할 날짜
	 * @param slotUnit 슬롯 단위 (HOUR 또는 HALF_HOUR)
	 * @return 생성된 슬롯 목록
	 */
	public List<RoomTimeSlot> generateSlotsFor(LocalDate date, SlotUnit slotUnit) {
		if (!shouldGenerateSlotsOn(date)) {
			return Collections.emptyList();
		}
		
		DayOfWeek dayOfWeek = date.getDayOfWeek();
		List<LocalTime> startTimes = weeklySchedule.getStartTimesFor(dayOfWeek);
		
		if (startTimes.isEmpty()) {
			return Collections.emptyList();
		}
		
		return startTimes.stream()
				.map(
						startTime -> {
							if (isClosedAt(date, startTime)) {
								return RoomTimeSlot.closed(roomId, date, startTime);
							} else {
								return RoomTimeSlot.available(roomId, date, startTime);
							}
						})
				.collect(Collectors.toList());
	}
	
	/**
	 * 주간 스케줄을 업데이트한다.
	 *
	 * @param newSchedule 새로운 주간 스케줄
	 */
	public void updateWeeklySchedule(WeeklySlotSchedule newSchedule) {
		this.weeklySchedule =
				Objects.requireNonNull(newSchedule, "weeklySchedule must not be null");
		this.updatedAt = LocalDateTime.now();
	}
	
	/**
	 * 반복 패턴을 업데이트한다.
	 *
	 * @param newRecurrence 새로운 반복 패턴
	 */
	public void updateRecurrence(RecurrencePattern newRecurrence) {
		this.recurrence = Objects.requireNonNull(newRecurrence, "recurrence must not be null");
		this.updatedAt = LocalDateTime.now();
	}
	
	/**
	 * 휴무일을 추가한다.
	 *
	 * @param closedDateRange 추가할 휴무일 범위
	 */
	public void addClosedDate(ClosedDateRange closedDateRange) {
		Objects.requireNonNull(closedDateRange, "closedDateRange must not be null");
		this.closedDates.add(closedDateRange);
		this.updatedAt = LocalDateTime.now();
	}
	
	/**
	 * 휴무일을 제거한다.
	 *
	 * @param closedDateRange 제거할 휴무일 범위
	 */
	public void removeClosedDate(ClosedDateRange closedDateRange) {
		this.closedDates.remove(closedDateRange);
		this.updatedAt = LocalDateTime.now();
	}
	
	/**
	 * 모든 휴무일을 새로운 목록으로 교체한다.
	 *
	 * @param newClosedDates 새로운 휴무일 목록
	 */
	public void updateClosedDates(List<ClosedDateRange> newClosedDates) {
		Objects.requireNonNull(newClosedDates, "closedDates must not be null");
		this.closedDates = new ArrayList<>(newClosedDates);
		this.updatedAt = LocalDateTime.now();
	}
	
	// Getters
	public Long getPolicyId() {
		return policyId;
	}
	
	public Long getRoomId() {
		return roomId;
	}
	
	public WeeklySlotSchedule getWeeklySchedule() {
		return weeklySchedule;
	}
	
	public RecurrencePattern getRecurrence() {
		return recurrence;
	}
	
	public List<ClosedDateRange> getClosedDates() {
		return Collections.unmodifiableList(closedDates);
	}
	
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RoomOperatingPolicy)) {
			return false;
		}
		RoomOperatingPolicy that = (RoomOperatingPolicy) o;
		return Objects.equals(policyId, that.policyId);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(policyId);
	}
	
	@Override
	public String toString() {
		return String.format(
				"RoomOperatingPolicy{policyId=%d, roomId=%d, recurrence=%s}",
				policyId, roomId, recurrence);
	}
}
