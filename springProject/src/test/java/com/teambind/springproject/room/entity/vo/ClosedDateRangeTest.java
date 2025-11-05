package com.teambind.springproject.room.entity.vo;

import com.teambind.springproject.common.exceptions.application.InvalidTimeRangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ClosedDateRange Value Object 단위 테스트.
 * <p>
 * 테스트 범위:
 * <p>
 * <p>
 * Factory 메서드를 통한 생성 (하루 종일, 날짜 범위, 시간 범위)
 * 날짜/시간 포함 여부 체크
 * 예외 케이스 (잘못된 날짜 범위)
 *
 */
@DisplayName("ClosedDateRange Value Object 테스트")
class ClosedDateRangeTest {
	
	// ============================================================
	// Factory Method 테스트
	// ============================================================
	
	@Nested
	@DisplayName("휴무일 생성 (Factory Methods)")
	class CreateClosedDateRangeTests {
		
		@Test
		@DisplayName("[정상] 하루 종일 휴무 생성")
		void createFullDayClosed() {
			// Given
			LocalDate date = LocalDate.of(2025, 1, 1);
			
			// When
			ClosedDateRange range = ClosedDateRange.ofFullDay(date);
			
			// Then
			assertThat(range.getStartDate()).isEqualTo(date);
			assertThat(range.getEndDate()).isNull();
			assertThat(range.getStartTime()).isNull();
			assertThat(range.getEndTime()).isNull();
		}
		
		@Test
		@DisplayName("[정상] 날짜 범위 휴무 생성")
		void createDateRangeClosed() {
			// Given
			LocalDate start = LocalDate.of(2025, 1, 27);
			LocalDate end = LocalDate.of(2025, 1, 29);
			
			// When
			ClosedDateRange range = ClosedDateRange.ofDateRange(start, end);
			
			// Then
			assertThat(range.getStartDate()).isEqualTo(start);
			assertThat(range.getEndDate()).isEqualTo(end);
			assertThat(range.getStartTime()).isNull();
			assertThat(range.getEndTime()).isNull();
		}
		
		@Test
		@DisplayName("[정상] 시간 범위 휴무 생성")
		void createTimeRangeClosed() {
			// Given
			LocalDate date = LocalDate.of(2025, 1, 15);
			LocalTime start = LocalTime.of(12, 0);
			LocalTime end = LocalTime.of(13, 0);
			
			// When
			ClosedDateRange range = ClosedDateRange.ofTimeRange(date, start, end);
			
			// Then
			assertThat(range.getStartDate()).isEqualTo(date);
			assertThat(range.getEndDate()).isNull();
			assertThat(range.getStartTime()).isEqualTo(start);
			assertThat(range.getEndTime()).isEqualTo(end);
		}
		
		@Test
		@DisplayName("[오류] startDate가 null이면 예외 발생")
		void createWithNullStartDate() {
			// When & Then
			assertThatThrownBy(() -> ClosedDateRange.ofFullDay(null))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("startDate must not be null");
		}
		
		@Test
		@DisplayName("[오류] endDate가 startDate보다 이전이면 예외 발생")
		void createWithInvalidDateRange() {
			// Given
			LocalDate start = LocalDate.of(2025, 1, 29);
			LocalDate end = LocalDate.of(2025, 1, 27); // start보다 이전

			// When & Then
			assertThatThrownBy(() -> ClosedDateRange.ofDateRange(start, end))
					.isInstanceOf(InvalidTimeRangeException.class)
					.hasMessageContaining("2025-01-29")
					.hasMessageContaining("2025-01-27");
		}
		
		@Test
		@DisplayName("[오류] endTime이 startTime보다 이전이면 예외 발생")
		void createWithInvalidTimeRange() {
			// Given
			LocalDate date = LocalDate.of(2025, 1, 15);
			LocalTime start = LocalTime.of(13, 0);
			LocalTime end = LocalTime.of(12, 0); // start보다 이전
			
			// When & Then
			assertThatThrownBy(() -> ClosedDateRange.ofTimeRange(date, start, end))
					.isInstanceOf(InvalidTimeRangeException.class)
					.hasMessageContaining("13:00")
					.hasMessageContaining("12:00");
		}
	}
	
	// ============================================================
	// 날짜 포함 여부 체크
	// ============================================================
	
	@Nested
	@DisplayName("날짜 포함 여부 체크 (containsDate)")
	class ContainsDateTests {
		
		@Test
		@DisplayName("[정상] 단일 날짜 휴무는 해당 날짜만 포함한다")
		void singleDateContainsOnlyThatDate() {
			// Given
			LocalDate holiday = LocalDate.of(2025, 1, 1);
			ClosedDateRange range = ClosedDateRange.ofFullDay(holiday);
			
			// When & Then
			assertThat(range.containsDate(LocalDate.of(2024, 12, 31))).isFalse();
			assertThat(range.containsDate(holiday)).isTrue();
			assertThat(range.containsDate(LocalDate.of(2025, 1, 2))).isFalse();
		}
		
		@Test
		@DisplayName("[정상] 날짜 범위 휴무는 범위 내 모든 날짜를 포함한다")
		void dateRangeContainsAllDatesInRange() {
			// Given
			LocalDate start = LocalDate.of(2025, 1, 27);
			LocalDate end = LocalDate.of(2025, 1, 29);
			ClosedDateRange range = ClosedDateRange.ofDateRange(start, end);
			
			// When & Then
			assertThat(range.containsDate(LocalDate.of(2025, 1, 26))).isFalse(); // 이전
			assertThat(range.containsDate(LocalDate.of(2025, 1, 27))).isTrue(); // 시작
			assertThat(range.containsDate(LocalDate.of(2025, 1, 28))).isTrue(); // 중간
			assertThat(range.containsDate(LocalDate.of(2025, 1, 29))).isTrue(); // 끝
			assertThat(range.containsDate(LocalDate.of(2025, 1, 30))).isFalse(); // 이후
		}
	}
	
	// ============================================================
	// 시간 포함 여부 체크
	// ============================================================
	
	@Nested
	@DisplayName("시간 포함 여부 체크 (containsTime)")
	class ContainsTimeTests {
		
		@Test
		@DisplayName("[정상] 하루 종일 휴무는 모든 시간을 포함한다")
		void fullDayClosedContainsAllTimes() {
			// Given
			ClosedDateRange range = ClosedDateRange.ofFullDay(LocalDate.of(2025, 1, 1));
			
			// When & Then
			assertThat(range.containsTime(LocalTime.of(0, 0))).isTrue();
			assertThat(range.containsTime(LocalTime.of(12, 0))).isTrue();
			assertThat(range.containsTime(LocalTime.of(23, 59))).isTrue();
		}
		
		@Test
		@DisplayName("[정상] 시간 범위 휴무는 범위 내 시간만 포함한다")
		void timeRangeContainsOnlyTimesInRange() {
			// Given
			LocalDate date = LocalDate.of(2025, 1, 15);
			LocalTime start = LocalTime.of(12, 0);
			LocalTime end = LocalTime.of(13, 0);
			ClosedDateRange range = ClosedDateRange.ofTimeRange(date, start, end);
			
			// When & Then
			assertThat(range.containsTime(LocalTime.of(11, 59))).isFalse(); // 이전
			assertThat(range.containsTime(LocalTime.of(12, 0))).isTrue(); // 시작
			assertThat(range.containsTime(LocalTime.of(12, 30))).isTrue(); // 중간
			assertThat(range.containsTime(LocalTime.of(13, 0))).isTrue(); // 끝
			assertThat(range.containsTime(LocalTime.of(13, 1))).isFalse(); // 이후
		}
	}
	
	// ============================================================
	// 날짜와 시간 동시 체크
	// ============================================================
	
	@Nested
	@DisplayName("날짜와 시간 동시 체크 (contains)")
	class ContainsDateAndTimeTests {
		
		@Test
		@DisplayName("[정상] 하루 종일 휴무는 해당 날짜의 모든 시간을 포함한다")
		void fullDayClosedContainsDateAndAllTimes() {
			// Given
			LocalDate holiday = LocalDate.of(2025, 1, 1);
			ClosedDateRange range = ClosedDateRange.ofFullDay(holiday);
			
			// When & Then
			assertThat(range.contains(holiday, LocalTime.of(9, 0))).isTrue();
			assertThat(range.contains(holiday, LocalTime.of(14, 0))).isTrue();
			assertThat(range.contains(LocalDate.of(2025, 1, 2), LocalTime.of(9, 0))).isFalse();
		}
		
		@Test
		@DisplayName("[정상] 시간 범위 휴무는 특정 날짜의 특정 시간만 포함한다")
		void timeRangeContainsOnlySpecificDateAndTime() {
			// Given
			LocalDate date = LocalDate.of(2025, 1, 15);
			LocalTime start = LocalTime.of(12, 0);
			LocalTime end = LocalTime.of(13, 0);
			ClosedDateRange range = ClosedDateRange.ofTimeRange(date, start, end);
			
			// When & Then
			assertThat(range.contains(date, LocalTime.of(12, 30))).isTrue(); // 해당 날짜, 범위 내
			assertThat(range.contains(date, LocalTime.of(9, 0))).isFalse(); // 해당 날짜, 범위 밖
			assertThat(range.contains(LocalDate.of(2025, 1, 16), LocalTime.of(12, 30)))
					.isFalse(); // 다른 날짜
		}
		
		@Test
		@DisplayName("[엣지] 날짜 범위 휴무는 범위 내 모든 날짜의 모든 시간을 포함한다")
		void dateRangeContainsAllDatesAndAllTimes() {
			// Given
			LocalDate start = LocalDate.of(2025, 1, 27);
			LocalDate end = LocalDate.of(2025, 1, 29);
			ClosedDateRange range = ClosedDateRange.ofDateRange(start, end);
			
			// When & Then
			assertThat(range.contains(LocalDate.of(2025, 1, 27), LocalTime.of(9, 0))).isTrue();
			assertThat(range.contains(LocalDate.of(2025, 1, 28), LocalTime.of(14, 0))).isTrue();
			assertThat(range.contains(LocalDate.of(2025, 1, 29), LocalTime.of(23, 59))).isTrue();
			assertThat(range.contains(LocalDate.of(2025, 1, 30), LocalTime.of(9, 0))).isFalse();
		}
	}
	
	// ============================================================
	// equals & hashCode 테스트
	// ============================================================
	
	@Nested
	@DisplayName("동등성 비교 (equals & hashCode)")
	class EqualsAndHashCodeTests {
		
		@Test
		@DisplayName("[정상] 모든 필드가 같으면 동등하다")
		void sameFieldsAreEqual() {
			// Given
			LocalDate date = LocalDate.of(2025, 1, 1);
			ClosedDateRange range1 = ClosedDateRange.ofFullDay(date);
			ClosedDateRange range2 = ClosedDateRange.ofFullDay(date);
			
			// When & Then
			assertThat(range1).isEqualTo(range2);
			assertThat(range1.hashCode()).isEqualTo(range2.hashCode());
		}
		
		@Test
		@DisplayName("[정상] 필드가 다르면 동등하지 않다")
		void differentFieldsAreNotEqual() {
			// Given
			ClosedDateRange range1 = ClosedDateRange.ofFullDay(LocalDate.of(2025, 1, 1));
			ClosedDateRange range2 = ClosedDateRange.ofFullDay(LocalDate.of(2025, 1, 2));
			
			// When & Then
			assertThat(range1).isNotEqualTo(range2);
		}
		
		@Test
		@DisplayName("[정상] 같은 인스턴스는 자기 자신과 동등하다")
		void sameInstanceIsEqual() {
			// Given
			ClosedDateRange range = ClosedDateRange.ofFullDay(LocalDate.of(2025, 1, 1));
			
			// When & Then
			assertThat(range).isEqualTo(range);
		}
	}
}
