package com.teambind.springproject.room.entity.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WeeklySlotTime Value Object 단위 테스트
 * 테스트 범위:
 * - 요일과 시작 시각 조합 생성
 * - null 값 검증
 * - 동등성 비교 (equals & hashCode)
 * - 불변성 검증
 */
@DisplayName("WeeklySlotTime Value Object 테스트")
class WeeklySlotTimeTest {
	
	// ============================================================
	// 생성 테스트
	// ============================================================
	
	@Nested
	@DisplayName("WeeklySlotTime 생성")
	class CreateWeeklySlotTimeTests {
		
		@Test
		@DisplayName("[정상] 요일과 시작 시각으로 생성된다")
		void createWithDayOfWeekAndStartTime() {
			// Given
			DayOfWeek day = DayOfWeek.MONDAY;
			LocalTime time = LocalTime.of(9, 0);
			
			// When
			WeeklySlotTime slotTime = WeeklySlotTime.of(day, time);
			
			// Then
			assertThat(slotTime.getDayOfWeek()).isEqualTo(day);
			assertThat(slotTime.getStartTime()).isEqualTo(time);
		}
		
		@Test
		@DisplayName("[오류] dayOfWeek가 null이면 예외 발생")
		void createWithNullDayOfWeek() {
			// Given
			LocalTime time = LocalTime.of(9, 0);
			
			// When & Then
			assertThatThrownBy(() -> WeeklySlotTime.of(null, time))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("dayOfWeek must not be null");
		}
		
		@Test
		@DisplayName("[오류] startTime이 null이면 예외 발생")
		void createWithNullStartTime() {
			// Given
			DayOfWeek day = DayOfWeek.MONDAY;
			
			// When & Then
			assertThatThrownBy(() -> WeeklySlotTime.of(day, null))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("startTime must not be null");
		}
	}
	
	// ============================================================
	// 동등성 비교 테스트
	// ============================================================
	
	@Nested
	@DisplayName("동등성 비교 (equals & hashCode)")
	class EqualsAndHashCodeTests {
		
		@Test
		@DisplayName("[정상] 같은 요일과 시각은 동등하다")
		void sameDayAndTimeAreEqual() {
			// Given
			WeeklySlotTime slot1 = WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0));
			WeeklySlotTime slot2 = WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0));
			
			// When & Then
			assertThat(slot1).isEqualTo(slot2);
			assertThat(slot1.hashCode()).isEqualTo(slot2.hashCode());
		}
		
		@Test
		@DisplayName("[정상] 다른 요일은 동등하지 않다")
		void differentDaysAreNotEqual() {
			// Given
			WeeklySlotTime slot1 = WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0));
			WeeklySlotTime slot2 = WeeklySlotTime.of(DayOfWeek.TUESDAY, LocalTime.of(9, 0));
			
			// When & Then
			assertThat(slot1).isNotEqualTo(slot2);
		}
		
		@Test
		@DisplayName("[정상] 다른 시각은 동등하지 않다")
		void differentTimesAreNotEqual() {
			// Given
			WeeklySlotTime slot1 = WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0));
			WeeklySlotTime slot2 = WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(10, 0));
			
			// When & Then
			assertThat(slot1).isNotEqualTo(slot2);
		}
		
		@Test
		@DisplayName("[정상] 같은 인스턴스는 자기 자신과 동등하다")
		void sameInstanceIsEqual() {
			// Given
			WeeklySlotTime slot = WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0));
			
			// When & Then
			assertThat(slot).isEqualTo(slot);
		}
		
		@Test
		@DisplayName("[정상] null과는 동등하지 않다")
		void notEqualToNull() {
			// Given
			WeeklySlotTime slot = WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0));
			
			// When & Then
			assertThat(slot).isNotEqualTo(null);
		}
	}
	
	// ============================================================
	// 불변성 검증 테스트
	// ============================================================
	
	@Nested
	@DisplayName("불변성 검증")
	class ImmutabilityTests {
		
		@Test
		@DisplayName("[정상] 생성 후 요일과 시각은 변경되지 않는다")
		void fieldsAreImmutableAfterCreation() {
			// Given
			DayOfWeek originalDay = DayOfWeek.MONDAY;
			LocalTime originalTime = LocalTime.of(9, 0);
			WeeklySlotTime slot = WeeklySlotTime.of(originalDay, originalTime);
			
			// When
			DayOfWeek retrievedDay = slot.getDayOfWeek();
			LocalTime retrievedTime = slot.getStartTime();
			
			// Then
			assertThat(retrievedDay).isEqualTo(originalDay);
			assertThat(retrievedTime).isEqualTo(originalTime);
			
			// Value Object 특성: 필드가 final이므로 setter가 없어야 함
			// (컴파일 타임 보장, 런타임 검증 불필요)
		}
	}
	
	// ============================================================
	// 엣지 케이스 테스트
	// ============================================================
	
	@Nested
	@DisplayName("엣지 케이스")
	class EdgeCaseTests {
		
		@Test
		@DisplayName("[엣지] 자정(00:00) 시각도 생성 가능하다")
		void createWithMidnightTime() {
			// Given
			DayOfWeek day = DayOfWeek.MONDAY;
			LocalTime midnight = LocalTime.of(0, 0);
			
			// When
			WeeklySlotTime slot = WeeklySlotTime.of(day, midnight);
			
			// Then
			assertThat(slot.getStartTime()).isEqualTo(midnight);
		}
		
		@Test
		@DisplayName("[엣지] 23:59 시각도 생성 가능하다")
		void createWithLastMinuteTime() {
			// Given
			DayOfWeek day = DayOfWeek.SUNDAY;
			LocalTime lastMinute = LocalTime.of(23, 59);
			
			// When
			WeeklySlotTime slot = WeeklySlotTime.of(day, lastMinute);
			
			// Then
			assertThat(slot.getStartTime()).isEqualTo(lastMinute);
		}
		
		@Test
		@DisplayName("[엣지] 모든 요일에 대해 생성 가능하다")
		void createForAllDaysOfWeek() {
			// Given
			LocalTime time = LocalTime.of(9, 0);
			
			// When & Then
			for (DayOfWeek day : DayOfWeek.values()) {
				WeeklySlotTime slot = WeeklySlotTime.of(day, time);
				assertThat(slot.getDayOfWeek()).isEqualTo(day);
			}
		}
	}
}
