package com.teambind.springproject.room.entity;

import com.teambind.springproject.common.exceptions.application.InvalidRequestException;
import com.teambind.springproject.room.entity.enums.RecurrencePattern;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import com.teambind.springproject.room.entity.enums.SlotUnit;
import com.teambind.springproject.room.entity.vo.ClosedDateRange;
import com.teambind.springproject.room.entity.vo.WeeklySlotSchedule;
import com.teambind.springproject.room.entity.vo.WeeklySlotTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RoomOperatingPolicy 엔티티 단위 테스트.
 * 테스트 범위:
 * <p>
 * 정책 생성 및 검증
 * 슬롯 생성 여부 판단 (반복 패턴, 휴무일)
 * 슬롯 생성 로직
 * 정책 업데이트 (주간 스케줄, 반복 패턴, 휴무일)
 *
 */
@DisplayName("RoomOperatingPolicy 엔티티 테스트")
class RoomOperatingPolicyTest {
	
	// ============================================================
	// 정책 생성 테스트
	// ============================================================
	
	private WeeklySlotSchedule createWeekdaySchedule() {
		List<WeeklySlotTime> slotTimes = new ArrayList<>();
		// 월-금 09:00, 13:00
		for (DayOfWeek day :
				List.of(
						DayOfWeek.MONDAY,
						DayOfWeek.TUESDAY,
						DayOfWeek.WEDNESDAY,
						DayOfWeek.THURSDAY,
						DayOfWeek.FRIDAY)) {
			slotTimes.add(WeeklySlotTime.of(day, LocalTime.of(9, 0)));
			slotTimes.add(WeeklySlotTime.of(day, LocalTime.of(13, 0)));
		}
		return WeeklySlotSchedule.of(slotTimes);
	}
	
	// ============================================================
	// 슬롯 생성 여부 판단 테스트
	// ============================================================
	
	private WeeklySlotSchedule createWeekendSchedule() {
		List<WeeklySlotTime> slotTimes = new ArrayList<>();
		// 토-일 10:00
		slotTimes.add(WeeklySlotTime.of(DayOfWeek.SATURDAY, LocalTime.of(10, 0)));
		slotTimes.add(WeeklySlotTime.of(DayOfWeek.SUNDAY, LocalTime.of(10, 0)));
		return WeeklySlotSchedule.of(slotTimes);
	}
	
	// ============================================================
	// 휴무 시간 체크 테스트
	// ============================================================
	
	private RoomOperatingPolicy createBasicPolicy(RecurrencePattern recurrence) {
		return RoomOperatingPolicy.create(
				101L, createWeekdaySchedule(), recurrence, SlotUnit.HALF_HOUR, Collections.emptyList());
	}
	
	// ============================================================
	// 슬롯 생성 로직 테스트
	// ============================================================
	
	private RoomOperatingPolicy createPolicyWithClosedDate(
			RecurrencePattern recurrence, ClosedDateRange closedDate) {
		return RoomOperatingPolicy.create(
				101L, createWeekdaySchedule(), recurrence, SlotUnit.HALF_HOUR, List.of(closedDate));
	}
	
	// ============================================================
	// 정책 업데이트 테스트
	// ============================================================
	
	@Nested
	@DisplayName("정책 생성 (Factory Method)")
	class CreatePolicyTests {
		
		@Test
		@DisplayName("[정상] 매주 월-금 운영 정책을 생성한다")
		void createWeekdayPolicy() {
			// Given
			Long roomId = 101L;
			WeeklySlotSchedule schedule = createWeekdaySchedule();
			RecurrencePattern recurrence = RecurrencePattern.EVERY_WEEK;
			List<ClosedDateRange> closedDates = Collections.emptyList();
			
			// When
			RoomOperatingPolicy policy =
					RoomOperatingPolicy.create(roomId, schedule, recurrence, SlotUnit.HOUR, closedDates);
			
			// Then
			assertThat(policy.getRoomId()).isEqualTo(roomId);
			assertThat(policy.getRecurrence()).isEqualTo(RecurrencePattern.EVERY_WEEK);
			assertThat(policy.getWeeklySchedule()).isNotNull();
			assertThat(policy.getClosedDates()).isEmpty();
			assertThat(policy.getCreatedAt()).isNotNull();
			assertThat(policy.getUpdatedAt()).isNotNull();
		}
		
		@Test
		@DisplayName("[오류] roomId가 null이면 예외가 발생한다")
		void createPolicyWithNullRoomId() {
			// Given
			WeeklySlotSchedule schedule = createWeekdaySchedule();
			
			// When & Then
			assertThatThrownBy(
					() ->
							RoomOperatingPolicy.create(
									null, schedule, RecurrencePattern.EVERY_WEEK, SlotUnit.HOUR, Collections.emptyList()))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("roomId must not be null");
		}
		
		@Test
		@DisplayName("[오류] weeklySchedule이 null이면 예외가 발생한다")
		void createPolicyWithNullSchedule() {
			// Given
			Long roomId = 101L;
			
			// When & Then
			assertThatThrownBy(
					() ->
							RoomOperatingPolicy.create(
									roomId, null, RecurrencePattern.EVERY_WEEK, SlotUnit.HOUR, Collections.emptyList()))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("weeklySchedule must not be null");
		}
		
		@Test
		@DisplayName("[오류] recurrence가 null이면 예외가 발생한다")
		void createPolicyWithNullRecurrence() {
			// Given
			Long roomId = 101L;
			WeeklySlotSchedule schedule = createWeekdaySchedule();
			
			// When & Then
			assertThatThrownBy(
					() -> RoomOperatingPolicy.create(roomId, schedule, null, SlotUnit.HOUR, Collections.emptyList()))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("recurrence must not be null");
		}
	}
	
	// ============================================================
	// 헬퍼 메서드
	// ============================================================
	
	@Nested
	@DisplayName("슬롯 생성 여부 판단 (shouldGenerateSlotsOn)")
	class ShouldGenerateSlotsTests {
		
		@Test
		@DisplayName("[정상] 매주 패턴: 모든 날짜에 슬롯을 생성한다")
		void everyWeekPatternGeneratesSlots() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);
			LocalDate monday = LocalDate.of(2025, 1, 6); // 2025년 1월 6일 (월요일, 1주차)
			LocalDate friday = LocalDate.of(2025, 1, 10); // 2025년 1월 10일 (금요일, 1주차)
			
			// When & Then
			assertThat(policy.shouldGenerateSlotsOn(monday)).isTrue();
			assertThat(policy.shouldGenerateSlotsOn(friday)).isTrue();
		}
		
		@Test
		@DisplayName("[정상] 홀수주 패턴: 홀수 주에만 슬롯을 생성한다")
		void oddWeekPatternGeneratesOnlyOnOddWeeks() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.ODD_WEEK);
			LocalDate oddWeek = LocalDate.of(2025, 1, 13); // 3주차 (홀수)
			LocalDate evenWeek = LocalDate.of(2025, 1, 6); // 2주차 (짝수)
			
			// When & Then
			assertThat(policy.shouldGenerateSlotsOn(oddWeek)).isTrue();
			assertThat(policy.shouldGenerateSlotsOn(evenWeek)).isFalse();
		}
		
		@Test
		@DisplayName("[정상] 짝수주 패턴: 짝수 주에만 슬롯을 생성한다")
		void evenWeekPatternGeneratesOnlyOnEvenWeeks() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVEN_WEEK);
			LocalDate oddWeek = LocalDate.of(2025, 1, 13); // 3주차 (홀수)
			LocalDate evenWeek = LocalDate.of(2025, 1, 6); // 2주차 (짝수)
			
			// When & Then
			assertThat(policy.shouldGenerateSlotsOn(oddWeek)).isFalse();
			assertThat(policy.shouldGenerateSlotsOn(evenWeek)).isTrue();
		}
		
		@Test
		@DisplayName("[정상] 하루 종일 휴무인 날짜는 슬롯을 생성하지 않는다")
		void closedDateDoesNotGenerateSlots() {
			// Given
			LocalDate holiday = LocalDate.of(2025, 1, 1);
			RoomOperatingPolicy policy =
					createPolicyWithClosedDate(
							RecurrencePattern.EVERY_WEEK, ClosedDateRange.ofFullDay(holiday));
			
			// When & Then
			assertThat(policy.shouldGenerateSlotsOn(holiday)).isFalse();
		}
		
		@Test
		@DisplayName("[정상] 부분 휴무인 날짜는 슬롯을 생성한다 (시간 필터링은 별도)")
		void partialClosedDateGeneratesSlots() {
			// Given
			LocalDate date = LocalDate.of(2025, 1, 15);
			ClosedDateRange partialClosed =
					ClosedDateRange.ofTimeRange(date, LocalTime.of(12, 0), LocalTime.of(13, 0));
			RoomOperatingPolicy policy =
					createPolicyWithClosedDate(RecurrencePattern.EVERY_WEEK, partialClosed);
			
			// When & Then
			assertThat(policy.shouldGenerateSlotsOn(date)).isTrue();
		}
	}
	
	@Nested
	@DisplayName("휴무 시간 체크 (isClosedAt)")
	class IsClosedAtTests {
		
		@Test
		@DisplayName("[정상] 하루 종일 휴무인 경우 모든 시간이 휴무다")
		void fullDayClosedIsClosedAtAnyTime() {
			// Given
			LocalDate holiday = LocalDate.of(2025, 1, 1);
			RoomOperatingPolicy policy =
					createPolicyWithClosedDate(
							RecurrencePattern.EVERY_WEEK, ClosedDateRange.ofFullDay(holiday));
			
			// When & Then
			assertThat(policy.isClosedAt(holiday, LocalTime.of(9, 0))).isTrue();
			assertThat(policy.isClosedAt(holiday, LocalTime.of(14, 0))).isTrue();
			assertThat(policy.isClosedAt(holiday, LocalTime.of(23, 59))).isTrue();
		}
		
		@Test
		@DisplayName("[정상] 부분 휴무 시간대는 해당 시간만 휴무다")
		void partialClosedIsClosedOnlyInRange() {
			// Given
			LocalDate date = LocalDate.of(2025, 1, 15);
			ClosedDateRange lunchBreak =
					ClosedDateRange.ofTimeRange(date, LocalTime.of(12, 0), LocalTime.of(13, 0));
			RoomOperatingPolicy policy =
					createPolicyWithClosedDate(RecurrencePattern.EVERY_WEEK, lunchBreak);
			
			// When & Then
			assertThat(policy.isClosedAt(date, LocalTime.of(9, 0))).isFalse(); // 점심 전
			assertThat(policy.isClosedAt(date, LocalTime.of(12, 0))).isTrue(); // 점심 시간
			assertThat(policy.isClosedAt(date, LocalTime.of(12, 30))).isTrue(); // 점심 시간
			assertThat(policy.isClosedAt(date, LocalTime.of(13, 0))).isTrue(); // 점심 시간 끝
			assertThat(policy.isClosedAt(date, LocalTime.of(14, 0))).isFalse(); // 점심 후
		}
		
		@Test
		@DisplayName("[정상] 휴무일이 아닌 날짜는 모든 시간이 운영 중이다")
		void nonClosedDateIsNotClosedAtAnyTime() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);
			LocalDate normalDay = LocalDate.of(2025, 1, 15);
			
			// When & Then
			assertThat(policy.isClosedAt(normalDay, LocalTime.of(9, 0))).isFalse();
			assertThat(policy.isClosedAt(normalDay, LocalTime.of(14, 0))).isFalse();
		}
	}
	
	@Nested
	@DisplayName("슬롯 생성 로직 (generateSlotsFor)")
	class GenerateSlotsForTests {
		
		@Test
		@DisplayName("[정상] 월요일 슬롯을 생성한다 (09:00, 13:00)")
		void generateMondaySlots() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);
			LocalDate monday = LocalDate.of(2025, 1, 6);
			SlotUnit slotUnit = SlotUnit.HOUR;
			
			// When
			List<RoomTimeSlot> slots = policy.generateSlotsFor(monday, slotUnit);
			
			// Then
			assertThat(slots).hasSize(2);
			assertThat(slots.get(0).getSlotTime()).isEqualTo(LocalTime.of(9, 0));
			assertThat(slots.get(0).getStatus()).isEqualTo(SlotStatus.AVAILABLE);
			assertThat(slots.get(1).getSlotTime()).isEqualTo(LocalTime.of(13, 0));
			assertThat(slots.get(1).getStatus()).isEqualTo(SlotStatus.AVAILABLE);
		}
		
		@Test
		@DisplayName("[정상] 휴무 시간대는 CLOSED 상태로 생성된다")
		void generateClosedSlotForClosedTime() {
			// Given
			LocalDate date = LocalDate.of(2025, 1, 15);
			ClosedDateRange lunchBreak =
					ClosedDateRange.ofTimeRange(date, LocalTime.of(12, 0), LocalTime.of(13, 0));
			
			List<WeeklySlotTime> slotTimes = new ArrayList<>();
			slotTimes.add(WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0)));
			slotTimes.add(WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(12, 0))); // 점심시간
			slotTimes.add(WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0)));
			
			RoomOperatingPolicy policy =
					RoomOperatingPolicy.create(
							101L,
							WeeklySlotSchedule.of(slotTimes),
							RecurrencePattern.EVERY_WEEK, SlotUnit.HOUR,
							List.of(lunchBreak));
			
			// When
			List<RoomTimeSlot> slots = policy.generateSlotsFor(date, SlotUnit.HOUR);
			
			// Then
			assertThat(slots).hasSize(3);
			assertThat(slots.get(0).getStatus()).isEqualTo(SlotStatus.AVAILABLE); // 09:00
			assertThat(slots.get(1).getStatus()).isEqualTo(SlotStatus.CLOSED); // 12:00 (점심시간)
			assertThat(slots.get(2).getStatus()).isEqualTo(SlotStatus.AVAILABLE); // 14:00
		}
		
		@Test
		@DisplayName("[정상] 하루 종일 휴무인 경우 빈 리스트를 반환한다")
		void generateNoSlotsForFullDayClosed() {
			// Given
			LocalDate holiday = LocalDate.of(2025, 1, 1);
			RoomOperatingPolicy policy =
					createPolicyWithClosedDate(
							RecurrencePattern.EVERY_WEEK, ClosedDateRange.ofFullDay(holiday));
			
			// When
			List<RoomTimeSlot> slots = policy.generateSlotsFor(holiday, SlotUnit.HOUR);
			
			// Then
			assertThat(slots).isEmpty();
		}
		
		@Test
		@DisplayName("[정상] 반복 패턴과 맞지 않는 날짜는 빈 리스트를 반환한다")
		void generateNoSlotsForNonMatchingRecurrence() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.ODD_WEEK);
			LocalDate evenWeek = LocalDate.of(2025, 1, 6); // 2주차 (짝수)
			
			// When
			List<RoomTimeSlot> slots = policy.generateSlotsFor(evenWeek, SlotUnit.HOUR);
			
			// Then
			assertThat(slots).isEmpty();
		}
		
		@Test
		@DisplayName("[정상] 해당 요일에 슬롯이 없으면 빈 리스트를 반환한다")
		void generateNoSlotsForDayWithoutSchedule() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);
			LocalDate saturday = LocalDate.of(2025, 1, 11); // 토요일 (스케줄 없음)
			
			// When
			List<RoomTimeSlot> slots = policy.generateSlotsFor(saturday, SlotUnit.HOUR);
			
			// Then
			assertThat(slots).isEmpty();
		}
	}
	
	@Nested
	@DisplayName("정책 업데이트")
	class UpdatePolicyTests {
		
		@Test
		@DisplayName("[정상] 주간 스케줄을 업데이트한다")
		void updateWeeklySchedule() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);
			WeeklySlotSchedule newSchedule = createWeekendSchedule();
			
			// When
			policy.updateWeeklySchedule(newSchedule);
			
			// Then
			assertThat(policy.getWeeklySchedule()).isEqualTo(newSchedule);
			assertThat(policy.getUpdatedAt()).isNotNull();
		}
		
		@Test
		@DisplayName("[정상] 반복 패턴을 업데이트한다")
		void updateRecurrence() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);
			
			// When
			policy.updateRecurrence(RecurrencePattern.ODD_WEEK);
			
			// Then
			assertThat(policy.getRecurrence()).isEqualTo(RecurrencePattern.ODD_WEEK);
		}
		
		@Test
		@DisplayName("[정상] 휴무일을 추가한다")
		void addClosedDate() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);
			ClosedDateRange holiday = ClosedDateRange.ofFullDay(LocalDate.of(2025, 1, 1));
			
			// When
			policy.addClosedDate(holiday);
			
			// Then
			assertThat(policy.getClosedDates()).hasSize(1);
			assertThat(policy.getClosedDates().get(0)).isEqualTo(holiday);
		}
		
		@Test
		@DisplayName("[정상] 휴무일을 제거한다")
		void removeClosedDate() {
			// Given
			ClosedDateRange holiday = ClosedDateRange.ofFullDay(LocalDate.of(2025, 1, 1));
			RoomOperatingPolicy policy =
					createPolicyWithClosedDate(RecurrencePattern.EVERY_WEEK, holiday);
			
			// When
			policy.removeClosedDate(holiday);
			
			// Then
			assertThat(policy.getClosedDates()).isEmpty();
		}
		
		@Test
		@DisplayName("[정상] 모든 휴무일을 새 목록으로 교체한다")
		void updateClosedDates() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);
			List<ClosedDateRange> newClosedDates =
					List.of(
							ClosedDateRange.ofFullDay(LocalDate.of(2025, 1, 1)),
							ClosedDateRange.ofFullDay(LocalDate.of(2025, 12, 25)));
			
			// When
			policy.updateClosedDates(newClosedDates);
			
			// Then
			assertThat(policy.getClosedDates()).hasSize(2);
		}
		
		@Test
		@DisplayName("[오류] null 스케줄로 업데이트할 수 없다")
		void cannotUpdateWithNullSchedule() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);
			
			// When & Then
			assertThatThrownBy(() -> policy.updateWeeklySchedule(null))
					.isInstanceOf(InvalidRequestException.class)
					.hasMessageContaining("weeklySchedule");
		}
		
		@Test
		@DisplayName("[오류] null 반복 패턴으로 업데이트할 수 없다")
		void cannotUpdateWithNullRecurrence() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);

			// When & Then
			assertThatThrownBy(() -> policy.updateRecurrence(null))
					.isInstanceOf(InvalidRequestException.class)
					.hasMessageContaining("recurrence");
		}

		@Test
		@DisplayName("[정상] 슬롯 단위를 업데이트한다")
		void updateSlotUnit() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);
			assertThat(policy.getSlotUnit()).isEqualTo(SlotUnit.HALF_HOUR);

			// When
			policy.updateSlotUnit(SlotUnit.HOUR);

			// Then
			assertThat(policy.getSlotUnit()).isEqualTo(SlotUnit.HOUR);
			assertThat(policy.getUpdatedAt()).isNotNull();
		}

		@Test
		@DisplayName("[오류] null 슬롯 단위로 업데이트할 수 없다")
		void cannotUpdateWithNullSlotUnit() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);

			// When & Then
			assertThatThrownBy(() -> policy.updateSlotUnit(null))
					.isInstanceOf(InvalidRequestException.class)
					.hasMessageContaining("slotUnit");
		}

		@Test
		@DisplayName("[정상] 운영 시간(스케줄+슬롯단위)을 한 번에 업데이트한다")
		void updateOperatingHours() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);
			WeeklySlotSchedule newSchedule = createWeekendSchedule();

			// When
			policy.updateOperatingHours(newSchedule, SlotUnit.HOUR);

			// Then
			assertThat(policy.getWeeklySchedule()).isEqualTo(newSchedule);
			assertThat(policy.getSlotUnit()).isEqualTo(SlotUnit.HOUR);
			assertThat(policy.getUpdatedAt()).isNotNull();
		}

		@Test
		@DisplayName("[오류] null 스케줄로 운영 시간을 업데이트할 수 없다")
		void cannotUpdateOperatingHoursWithNullSchedule() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);

			// When & Then
			assertThatThrownBy(() -> policy.updateOperatingHours(null, SlotUnit.HOUR))
					.isInstanceOf(InvalidRequestException.class)
					.hasMessageContaining("weeklySchedule");
		}

		@Test
		@DisplayName("[오류] null 슬롯 단위로 운영 시간을 업데이트할 수 없다")
		void cannotUpdateOperatingHoursWithNullSlotUnit() {
			// Given
			RoomOperatingPolicy policy = createBasicPolicy(RecurrencePattern.EVERY_WEEK);
			WeeklySlotSchedule newSchedule = createWeekendSchedule();

			// When & Then
			assertThatThrownBy(() -> policy.updateOperatingHours(newSchedule, null))
					.isInstanceOf(InvalidRequestException.class)
					.hasMessageContaining("slotUnit");
		}
	}
}
