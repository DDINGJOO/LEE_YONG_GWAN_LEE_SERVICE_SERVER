package com.teambind.springproject.room.entity.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RecurrencePattern Enum 단위 테스트.
 * <p>
 * 테스트 범위:
 * <p>
 * <p>
 * 매주 패턴 (EVERY_WEEK)
 * 홀수주 패턴 (ODD_WEEK)
 * 짝수주 패턴 (EVEN_WEEK)
 * ISO 8601 주차 계산 기준 검증
 *
 */
@DisplayName("RecurrencePattern Enum 테스트")
class RecurrencePatternTest {
	
	// ============================================================
	// EVERY_WEEK 패턴 테스트
	// ============================================================
	
	@Nested
	@DisplayName("매주 패턴 (EVERY_WEEK)")
	class EveryWeekPatternTests {
		
		@Test
		@DisplayName("[정상] 모든 날짜와 일치한다")
		void everyWeekMatchesAllDates() {
			// Given
			RecurrencePattern pattern = RecurrencePattern.EVERY_WEEK;
			
			// When & Then
			assertThat(pattern.matches(LocalDate.of(2025, 1, 6))).isTrue(); // 1주차
			assertThat(pattern.matches(LocalDate.of(2025, 1, 13))).isTrue(); // 2주차
			assertThat(pattern.matches(LocalDate.of(2025, 1, 20))).isTrue(); // 3주차
			assertThat(pattern.matches(LocalDate.of(2025, 12, 31))).isTrue(); // 53주차
		}
		
		@ParameterizedTest
		@CsvSource({
				"2025-01-01",
				"2025-06-15",
				"2025-12-31",
				"2024-02-29", // 윤년
				"2023-02-28" // 평년
		})
		@DisplayName("[정상] 다양한 날짜에서 항상 true 반환")
		void everyWeekAlwaysReturnsTrue(String dateString) {
			// Given
			RecurrencePattern pattern = RecurrencePattern.EVERY_WEEK;
			LocalDate date = LocalDate.parse(dateString);
			
			// When & Then
			assertThat(pattern.matches(date)).isTrue();
		}
	}
	
	// ============================================================
	// ODD_WEEK 패턴 테스트
	// ============================================================
	
	@Nested
	@DisplayName("홀수주 패턴 (ODD_WEEK)")
	class OddWeekPatternTests {
		
		@Test
		@DisplayName("[정상] 2025년 홀수 주차와 일치한다")
		void oddWeekMatches2025OddWeeks() {
			// Given
			RecurrencePattern pattern = RecurrencePattern.ODD_WEEK;
			
			// When & Then: 2025년 ISO 8601 기준 홀수 주차
			assertThat(pattern.matches(LocalDate.of(2025, 1, 13))).isTrue(); // 3주차 월요일
			assertThat(pattern.matches(LocalDate.of(2025, 1, 27))).isTrue(); // 5주차 월요일
			assertThat(pattern.matches(LocalDate.of(2025, 2, 10))).isTrue(); // 7주차 월요일
		}
		
		@Test
		@DisplayName("[정상] 2025년 짝수 주차와 일치하지 않는다")
		void oddWeekDoesNotMatch2025EvenWeeks() {
			// Given
			RecurrencePattern pattern = RecurrencePattern.ODD_WEEK;
			
			// When & Then: 2025년 ISO 8601 기준 짝수 주차
			assertThat(pattern.matches(LocalDate.of(2025, 1, 6))).isFalse(); // 2주차
			assertThat(pattern.matches(LocalDate.of(2025, 1, 20))).isFalse(); // 4주차
			assertThat(pattern.matches(LocalDate.of(2025, 2, 3))).isFalse(); // 6주차
		}
		
		@ParameterizedTest
		@CsvSource({
				"2025-01-06, false", // 2주차 (짝수)
				"2025-01-13,  true", // 3주차 (홀수)
				"2025-01-20, false", // 4주차 (짝수)
				"2025-01-27,  true", // 5주차 (홀수)
				"2025-12-29,  true" // 1주차 (홀수) - 2026년 기준
		})
		@DisplayName("[정상] 2025년 주차별 패턴 매칭 검증")
		void oddWeekPatternVerification(String dateString, boolean expected) {
			// Given
			RecurrencePattern pattern = RecurrencePattern.ODD_WEEK;
			LocalDate date = LocalDate.parse(dateString);
			
			// When & Then
			assertThat(pattern.matches(date)).isEqualTo(expected);
		}
	}
	
	// ============================================================
	// EVEN_WEEK 패턴 테스트
	// ============================================================
	
	@Nested
	@DisplayName("짝수주 패턴 (EVEN_WEEK)")
	class EvenWeekPatternTests {
		
		@Test
		@DisplayName("[정상] 2025년 짝수 주차와 일치한다")
		void evenWeekMatches2025EvenWeeks() {
			// Given
			RecurrencePattern pattern = RecurrencePattern.EVEN_WEEK;
			
			// When & Then: 2025년 ISO 8601 기준 짝수 주차
			assertThat(pattern.matches(LocalDate.of(2025, 1, 6))).isTrue(); // 2주차
			assertThat(pattern.matches(LocalDate.of(2025, 1, 20))).isTrue(); // 4주차
			assertThat(pattern.matches(LocalDate.of(2025, 2, 3))).isTrue(); // 6주차
		}
		
		@Test
		@DisplayName("[정상] 2025년 홀수 주차와 일치하지 않는다")
		void evenWeekDoesNotMatch2025OddWeeks() {
			// Given
			RecurrencePattern pattern = RecurrencePattern.EVEN_WEEK;
			
			// When & Then: 2025년 ISO 8601 기준 홀수 주차
			assertThat(pattern.matches(LocalDate.of(2025, 1, 13))).isFalse(); // 3주차
			assertThat(pattern.matches(LocalDate.of(2025, 1, 27))).isFalse(); // 5주차
			assertThat(pattern.matches(LocalDate.of(2025, 2, 10))).isFalse(); // 7주차
		}
		
		@ParameterizedTest
		@CsvSource({
				"2025-01-06,  true", // 2주차 (짝수)
				"2025-01-13, false", // 3주차 (홀수)
				"2025-01-20,  true", // 4주차 (짝수)
				"2025-01-27, false", // 5주차 (홀수)
				"2025-12-29, false" // 1주차 (홀수) - 2026년 기준
		})
		@DisplayName("[정상] 2025년 주차별 패턴 매칭 검증")
		void evenWeekPatternVerification(String dateString, boolean expected) {
			// Given
			RecurrencePattern pattern = RecurrencePattern.EVEN_WEEK;
			LocalDate date = LocalDate.parse(dateString);
			
			// When & Then
			assertThat(pattern.matches(date)).isEqualTo(expected);
		}
	}
	
	// ============================================================
	// 엣지 케이스: 연말/연초, 윤년
	// ============================================================
	
	@Nested
	@DisplayName("엣지 케이스 (연말/연초, 윤년)")
	class EdgeCaseTests {
		
		@Test
		@DisplayName("[엣지] 2024년 12월 31일은 2025년 1주차로 계산된다 (ISO 8601)")
		void yearEndDateBelongsToNextYear() {
			// Given: 2024년 12월 31일 (화요일)
			// ISO 8601 기준: 2025년 1주차에 속함
			LocalDate date = LocalDate.of(2024, 12, 31);
			
			// When & Then: 1주차는 홀수주
			assertThat(RecurrencePattern.ODD_WEEK.matches(date)).isTrue();
			assertThat(RecurrencePattern.EVEN_WEEK.matches(date)).isFalse();
		}
		
		@Test
		@DisplayName("[엣지] 2025년 1월 1일은 2025년 1주차이다")
		void yearStartDate() {
			// Given: 2025년 1월 1일 (수요일)
			LocalDate date = LocalDate.of(2025, 1, 1);
			
			// When & Then: 1주차는 홀수주
			assertThat(RecurrencePattern.ODD_WEEK.matches(date)).isTrue();
			assertThat(RecurrencePattern.EVEN_WEEK.matches(date)).isFalse();
		}
		
		@Test
		@DisplayName("[엣지] 윤년 2월 29일 (2024)")
		void leapYearFebruary29() {
			// Given: 2024년 2월 29일 (목요일, 9주차)
			LocalDate date = LocalDate.of(2024, 2, 29);
			
			// When & Then: 9주차는 홀수주
			assertThat(RecurrencePattern.ODD_WEEK.matches(date)).isTrue();
			assertThat(RecurrencePattern.EVEN_WEEK.matches(date)).isFalse();
		}
		
		@Test
		@DisplayName("[엣지] 2025년 마지막 날 (12월 31일, 수요일)")
		void yearEnd2025() {
			// Given: 2025년 12월 31일 (수요일, 2026년 1주차)
			LocalDate date = LocalDate.of(2025, 12, 31);
			
			// When & Then: ISO 8601 기준 2026년 1주차 (홀수)
			assertThat(RecurrencePattern.ODD_WEEK.matches(date)).isTrue();
			assertThat(RecurrencePattern.EVEN_WEEK.matches(date)).isFalse();
		}
	}
	
	// ============================================================
	// 상호 배타성 테스트
	// ============================================================
	
	@Nested
	@DisplayName("홀수주와 짝수주의 상호 배타성")
	class MutualExclusivityTests {
		
		@ParameterizedTest
		@CsvSource({
				"2025-01-06", // 1주차
				"2025-01-13", // 2주차
				"2025-06-15", // 24주차
				"2025-12-29" // 53주차
		})
		@DisplayName("[정상] 특정 날짜는 홀수주 또는 짝수주 중 하나에만 속한다")
		void dateMatchesEitherOddOrEvenWeek(String dateString) {
			// Given
			LocalDate date = LocalDate.parse(dateString);
			
			// When
			boolean matchesOdd = RecurrencePattern.ODD_WEEK.matches(date);
			boolean matchesEven = RecurrencePattern.EVEN_WEEK.matches(date);
			
			// Then: XOR 검증 (하나만 true)
			assertThat(matchesOdd ^ matchesEven).isTrue();
		}
	}
}
