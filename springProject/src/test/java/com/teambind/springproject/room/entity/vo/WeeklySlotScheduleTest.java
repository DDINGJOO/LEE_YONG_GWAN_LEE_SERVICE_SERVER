package com.teambind.springproject.room.entity.vo;

import com.teambind.springproject.common.exceptions.application.InvalidRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WeeklySlotSchedule Value Object 단위 테스트.
 * <p>
 * 테스트 범위:
 * <p>
 * - 주간 슬롯 스케줄 생성
 * - 요일별 시작 시각 조회 (getStartTimesFor)
 * - 빈 스케줄 처리
 * - null 값 검증
 * - 동등성 비교
 */
@DisplayName("WeeklySlotSchedule Value Object 테스트")
class WeeklySlotScheduleTest {
	
	// ============================================================
	// 생성 테스트
	// ============================================================
	
	@Nested
	@DisplayName("WeeklySlotSchedule 생성")
	class CreateWeeklySlotScheduleTests {
		
		@Test
		@DisplayName("[정상] 주간 슬롯 리스트로 생성된다")
		void createWithWeeklySlotTimes() {
			// Given
			List<WeeklySlotTime> slots = Arrays.asList(
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(14, 0)),
					WeeklySlotTime.of(DayOfWeek.FRIDAY, LocalTime.of(10, 0))
			);
			
			// When
			WeeklySlotSchedule schedule = WeeklySlotSchedule.of(slots);
			
			// Then
			assertThat(schedule.getStartTimesFor(DayOfWeek.MONDAY))
					.containsExactlyInAnyOrder(LocalTime.of(9, 0), LocalTime.of(14, 0));
			assertThat(schedule.getStartTimesFor(DayOfWeek.FRIDAY))
					.containsExactly(LocalTime.of(10, 0));
		}
		
		@Test
		@DisplayName("[정상] 빈 리스트로 생성 가능하다")
		void createWithEmptyList() {
			// Given
			List<WeeklySlotTime> emptySlots = Collections.emptyList();
			
			// When
			WeeklySlotSchedule schedule = WeeklySlotSchedule.of(emptySlots);
			
			// Then
			assertThat(schedule.getStartTimesFor(DayOfWeek.MONDAY)).isEmpty();
			assertThat(schedule.getStartTimesFor(DayOfWeek.SUNDAY)).isEmpty();
		}
		
		@Test
		@DisplayName("[오류] null 리스트로 생성 시 예외 발생")
		void createWithNullList() {
			// When & Then: ArrayList 생성자가 NullPointerException을 던짐
			assertThatThrownBy(() -> WeeklySlotSchedule.of(null))
					.isInstanceOf(NullPointerException.class);
		}
	}
	
	// ============================================================
	// 요일별 시작 시각 조회 테스트
	// ============================================================
	
	@Nested
	@DisplayName("요일별 시작 시각 조회 (getStartTimesFor)")
	class GetStartTimesForTests {
		
		@Test
		@DisplayName("[정상] 특정 요일의 모든 시작 시각을 반환한다")
		void returnAllStartTimesForSpecificDay() {
			// Given
			List<WeeklySlotTime> slots = Arrays.asList(
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(11, 0)),
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(14, 0)),
					WeeklySlotTime.of(DayOfWeek.TUESDAY, LocalTime.of(10, 0))
			);
			WeeklySlotSchedule schedule = WeeklySlotSchedule.of(slots);
			
			// When
			List<LocalTime> mondayTimes = schedule.getStartTimesFor(DayOfWeek.MONDAY);
			
			// Then
			assertThat(mondayTimes)
					.hasSize(3)
					.containsExactlyInAnyOrder(
							LocalTime.of(9, 0),
							LocalTime.of(11, 0),
							LocalTime.of(14, 0)
					);
		}
		
		@Test
		@DisplayName("[정상] 스케줄이 없는 요일은 빈 리스트를 반환한다")
		void returnEmptyListForDayWithoutSchedule() {
			// Given
			List<WeeklySlotTime> slots = Arrays.asList(
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0))
			);
			WeeklySlotSchedule schedule = WeeklySlotSchedule.of(slots);
			
			// When
			List<LocalTime> sundayTimes = schedule.getStartTimesFor(DayOfWeek.SUNDAY);
			
			// Then
			assertThat(sundayTimes).isEmpty();
		}
		
		@Test
		@DisplayName("[정상] 반환된 리스트는 시간 순으로 정렬되어 있다")
		void returnedListIsSortedByTime() {
			// Given
			List<WeeklySlotTime> slots = Arrays.asList(
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(14, 0)),
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(11, 0))
			);
			WeeklySlotSchedule schedule = WeeklySlotSchedule.of(slots);
			
			// When
			List<LocalTime> mondayTimes = schedule.getStartTimesFor(DayOfWeek.MONDAY);
			
			// Then
			assertThat(mondayTimes)
					.containsExactly(
							LocalTime.of(9, 0),
							LocalTime.of(11, 0),
							LocalTime.of(14, 0)
					);
		}
		
		@Test
		@DisplayName("[오류] null 요일로 조회 시 예외 발생")
		void throwExceptionWhenQueryingWithNullDay() {
			// Given
			WeeklySlotSchedule schedule = WeeklySlotSchedule.of(Collections.emptyList());
			
			// When & Then
			assertThatThrownBy(() -> schedule.getStartTimesFor(null))
					.isInstanceOf(InvalidRequestException.class)
					.hasMessageContaining("dayOfWeek");
		}
	}
	
	// ============================================================
	// 불변성 검증 테스트
	// ============================================================
	
	@Nested
	@DisplayName("불변성 검증")
	class ImmutabilityTests {
		
		@Test
		@DisplayName("[정상] 반환된 리스트를 수정해도 원본에 영향을 주지 않는다")
		void modifyingReturnedListDoesNotAffectOriginal() {
			// Given
			List<WeeklySlotTime> slots = Arrays.asList(
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0))
			);
			WeeklySlotSchedule schedule = WeeklySlotSchedule.of(slots);
			
			// When
			List<LocalTime> mondayTimes = schedule.getStartTimesFor(DayOfWeek.MONDAY);
			
			// Then: 수정 시도 시 예외 발생 (불변 리스트)
			assertThatThrownBy(() -> mondayTimes.add(LocalTime.of(10, 0)))
					.isInstanceOf(UnsupportedOperationException.class);
			
			// 원본은 여전히 1개의 시각만 유지
			assertThat(schedule.getStartTimesFor(DayOfWeek.MONDAY)).hasSize(1);
		}
	}
	
	// ============================================================
	// 동등성 비교 테스트
	// ============================================================
	
	@Nested
	@DisplayName("동등성 비교 (equals & hashCode)")
	class EqualsAndHashCodeTests {
		
		@Test
		@DisplayName("[정상] 같은 슬롯 리스트는 동등하다")
		void sameSlotsAreEqual() {
			// Given
			List<WeeklySlotTime> slots1 = Arrays.asList(
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.FRIDAY, LocalTime.of(14, 0))
			);
			List<WeeklySlotTime> slots2 = Arrays.asList(
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.FRIDAY, LocalTime.of(14, 0))
			);
			
			WeeklySlotSchedule schedule1 = WeeklySlotSchedule.of(slots1);
			WeeklySlotSchedule schedule2 = WeeklySlotSchedule.of(slots2);
			
			// When & Then
			assertThat(schedule1).isEqualTo(schedule2);
			assertThat(schedule1.hashCode()).isEqualTo(schedule2.hashCode());
		}
		
		@Test
		@DisplayName("[정상] 다른 슬롯 리스트는 동등하지 않다")
		void differentSlotsAreNotEqual() {
			// Given
			List<WeeklySlotTime> slots1 = Arrays.asList(
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0))
			);
			List<WeeklySlotTime> slots2 = Arrays.asList(
					WeeklySlotTime.of(DayOfWeek.TUESDAY, LocalTime.of(9, 0))
			);
			
			WeeklySlotSchedule schedule1 = WeeklySlotSchedule.of(slots1);
			WeeklySlotSchedule schedule2 = WeeklySlotSchedule.of(slots2);
			
			// When & Then
			assertThat(schedule1).isNotEqualTo(schedule2);
		}
		
		@Test
		@DisplayName("[정상] 같은 인스턴스는 자기 자신과 동등하다")
		void sameInstanceIsEqual() {
			// Given
			WeeklySlotSchedule schedule = WeeklySlotSchedule.of(Collections.emptyList());
			
			// When & Then
			assertThat(schedule).isEqualTo(schedule);
		}
	}
	
	// ============================================================
	// 엣지 케이스 테스트
	// ============================================================
	
	@Nested
	@DisplayName("엣지 케이스")
	class EdgeCaseTests {
		
		@Test
		@DisplayName("[엣지] 모든 요일에 슬롯이 있는 경우")
		void scheduleWithSlotsForAllDays() {
			// Given
			List<WeeklySlotTime> slots = Arrays.asList(
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.TUESDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.THURSDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.FRIDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.SATURDAY, LocalTime.of(10, 0)),
					WeeklySlotTime.of(DayOfWeek.SUNDAY, LocalTime.of(10, 0))
			);
			WeeklySlotSchedule schedule = WeeklySlotSchedule.of(slots);
			
			// When & Then
			for (DayOfWeek day : DayOfWeek.values()) {
				assertThat(schedule.getStartTimesFor(day)).isNotEmpty();
			}
		}
		
		@Test
		@DisplayName("[엣지] 하나의 요일에 여러 시각이 있는 경우")
		void multipleSlotsForSingleDay() {
			// Given
			List<WeeklySlotTime> slots = Arrays.asList(
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(10, 0)),
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(11, 0)),
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(13, 0)),
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(14, 0)),
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(15, 0))
			);
			WeeklySlotSchedule schedule = WeeklySlotSchedule.of(slots);
			
			// When
			List<LocalTime> mondayTimes = schedule.getStartTimesFor(DayOfWeek.MONDAY);
			
			// Then
			assertThat(mondayTimes).hasSize(6);
		}
		
		@Test
		@DisplayName("[엣지] 중복된 시각이 있는 경우 중복 제거된다")
		void duplicateTimesAreRemoved() {
			// Given
			List<WeeklySlotTime> slots = Arrays.asList(
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
					WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0))
			);
			WeeklySlotSchedule schedule = WeeklySlotSchedule.of(slots);
			
			// When
			List<LocalTime> mondayTimes = schedule.getStartTimesFor(DayOfWeek.MONDAY);
			
			// Then
			assertThat(mondayTimes).hasSize(1).containsExactly(LocalTime.of(9, 0));
		}
	}
}
