package com.teambind.springproject.room.entity.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HourStatus Enum 단위 테스트.
 * <p>
 * 테스트 범위:
 * <p>
 * - Enum 값 존재 여부
 * - SlotUnit과의 매핑
 * - getMinutes() 메서드 검증
 */
@DisplayName("HourStatus (SlotUnit) Enum 테스트")
class HourStatusTest {
	
	// ============================================================
	// Enum 값 테스트
	// ============================================================
	
	@Nested
	@DisplayName("Enum 값 검증")
	class EnumValuesTests {
		
		@Test
		@DisplayName("[정상] 모든 슬롯 단위 값이 존재한다")
		void allSlotUnitsExist() {
			// When
			SlotUnit[] units = SlotUnit.values();
			
			// Then
			assertThat(units)
					.hasSize(2)
					.containsExactly(
							SlotUnit.HOUR,
							SlotUnit.HALF_HOUR
					);
		}
		
		@Test
		@DisplayName("[정상] valueOf()로 문자열을 Enum으로 변환할 수 있다")
		void valueOfConvertsStringToEnum() {
			// When & Then
			assertThat(SlotUnit.valueOf("HOUR")).isEqualTo(SlotUnit.HOUR);
			assertThat(SlotUnit.valueOf("HALF_HOUR")).isEqualTo(SlotUnit.HALF_HOUR);
		}
		
		@Test
		@DisplayName("[오류] 존재하지 않는 값으로 valueOf() 호출 시 예외 발생")
		void valueOfThrowsExceptionForInvalidValue() {
			// When & Then
			assertThatThrownBy(() -> SlotUnit.valueOf("INVALID_UNIT"))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}
	
	// ============================================================
	// getMinutes() 메서드 테스트
	// ============================================================
	
	@Nested
	@DisplayName("getMinutes() 메서드 검증")
	class GetMinutesTests {
		
		@Test
		@DisplayName("[정상] HOUR는 60분을 반환한다")
		void hourReturns60Minutes() {
			// Given
			SlotUnit unit = SlotUnit.HOUR;
			
			// When
			int minutes = unit.getMinutes();
			
			// Then
			assertThat(minutes).isEqualTo(60);
		}
		
		@Test
		@DisplayName("[정상] HALF_HOUR는 30분을 반환한다")
		void halfHourReturns30Minutes() {
			// Given
			SlotUnit unit = SlotUnit.HALF_HOUR;
			
			// When
			int minutes = unit.getMinutes();
			
			// Then
			assertThat(minutes).isEqualTo(30);
		}
	}
	
	// ============================================================
	// 동등성 비교 테스트
	// ============================================================
	
	@Nested
	@DisplayName("동등성 비교")
	class EqualityTests {
		
		@Test
		@DisplayName("[정상] 같은 Enum 값은 동등하다")
		void sameEnumValuesAreEqual() {
			// Given
			SlotUnit unit1 = SlotUnit.HOUR;
			SlotUnit unit2 = SlotUnit.HOUR;
			
			// When & Then
			assertThat(unit1).isEqualTo(unit2);
			assertThat(unit1 == unit2).isTrue(); // Enum은 싱글톤
		}
		
		@Test
		@DisplayName("[정상] 다른 Enum 값은 동등하지 않다")
		void differentEnumValuesAreNotEqual() {
			// Given
			SlotUnit unit1 = SlotUnit.HOUR;
			SlotUnit unit2 = SlotUnit.HALF_HOUR;
			
			// When & Then
			assertThat(unit1).isNotEqualTo(unit2);
		}
	}
	
	// ============================================================
	// 엣지 케이스 테스트
	// ============================================================
	
	@Nested
	@DisplayName("엣지 케이스")
	class EdgeCaseTests {
		
		@Test
		@DisplayName("[엣지] 모든 슬롯 단위는 양수 분을 반환한다")
		void allUnitsReturnPositiveMinutes() {
			// When & Then
			for (SlotUnit unit : SlotUnit.values()) {
				assertThat(unit.getMinutes()).isPositive();
			}
		}
		
		@Test
		@DisplayName("[엣지] HALF_HOUR는 HOUR의 절반이다")
		void halfHourIsHalfOfHour() {
			// When & Then
			assertThat(SlotUnit.HALF_HOUR.getMinutes() * 2)
					.isEqualTo(SlotUnit.HOUR.getMinutes());
		}
	}
}
