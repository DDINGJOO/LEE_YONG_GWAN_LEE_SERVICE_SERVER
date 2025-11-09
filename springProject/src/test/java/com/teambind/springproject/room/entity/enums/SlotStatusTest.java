package com.teambind.springproject.room.entity.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SlotStatus Enum 단위 테스트.
 * <p>
 * 테스트 범위:
 * <p>
 * - Enum 값 존재 여부
 * - valueOf() 변환
 * - 상태별 의미 검증
 */
@DisplayName("SlotStatus Enum 테스트")
class SlotStatusTest {
	
	// ============================================================
	// Enum 값 테스트
	// ============================================================
	
	@Nested
	@DisplayName("Enum 값 검증")
	class EnumValuesTests {
		
		@Test
		@DisplayName("[정상] 모든 슬롯 상태 값이 존재한다")
		void allStatusesExist() {
			// When
			SlotStatus[] statuses = SlotStatus.values();
			
			// Then
			assertThat(statuses)
					.hasSize(5)
					.containsExactly(
							SlotStatus.AVAILABLE,
							SlotStatus.PENDING,
							SlotStatus.RESERVED,
							SlotStatus.AVAILABLE,
							SlotStatus.CLOSED
					);
		}
		
		@Test
		@DisplayName("[정상] valueOf()로 문자열을 Enum으로 변환할 수 있다")
		void valueOfConvertsStringToEnum() {
			// When & Then
			assertThat(SlotStatus.valueOf("AVAILABLE")).isEqualTo(SlotStatus.AVAILABLE);
			assertThat(SlotStatus.valueOf("PENDING")).isEqualTo(SlotStatus.PENDING);
			assertThat(SlotStatus.valueOf("RESERVED")).isEqualTo(SlotStatus.RESERVED);
			assertThat(SlotStatus.valueOf("CANCELLED")).isEqualTo(SlotStatus.AVAILABLE);
			assertThat(SlotStatus.valueOf("CLOSED")).isEqualTo(SlotStatus.CLOSED);
		}
		
		@Test
		@DisplayName("[오류] 존재하지 않는 값으로 valueOf() 호출 시 예외 발생")
		void valueOfThrowsExceptionForInvalidValue() {
			// When & Then
			assertThatThrownBy(() -> SlotStatus.valueOf("INVALID_STATUS"))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}
	
	// ============================================================
	// 상태별 의미 검증
	// ============================================================
	
	@Nested
	@DisplayName("상태별 의미 검증")
	class SemanticTests {
		
		@Test
		@DisplayName("[정상] AVAILABLE은 예약 가능한 상태이다")
		void availableRepresentsBookableState() {
			// Given
			SlotStatus status = SlotStatus.AVAILABLE;
			
			// Then: 예약 가능 상태를 나타냄
			assertThat(status).isNotNull();
			assertThat(status.name()).isEqualTo("AVAILABLE");
		}
		
		@Test
		@DisplayName("[정상] PENDING은 예약 대기 중 상태이다")
		void pendingRepresentsWaitingState() {
			// Given
			SlotStatus status = SlotStatus.PENDING;
			
			// Then: 예약 대기 중 (임시 예약) 상태를 나타냄
			assertThat(status).isNotNull();
			assertThat(status.name()).isEqualTo("PENDING");
		}
		
		@Test
		@DisplayName("[정상] RESERVED는 예약 확정 상태이다")
		void reservedRepresentsConfirmedState() {
			// Given
			SlotStatus status = SlotStatus.RESERVED;
			
			// Then: 예약 확정 상태를 나타냄
			assertThat(status).isNotNull();
			assertThat(status.name()).isEqualTo("RESERVED");
		}
		
		@Test
		@DisplayName("[정상] CANCELLED는 취소된 상태이다")
		void cancelledRepresentsCancelledState() {
			// Given
			SlotStatus status = SlotStatus.AVAILABLE;
			
			// Then: 취소된 상태를 나타냄
			assertThat(status).isNotNull();
			assertThat(status.name()).isEqualTo("CANCELLED");
		}
		
		@Test
		@DisplayName("[정상] CLOSED는 운영하지 않는 상태이다")
		void closedRepresentsNonOperatingState() {
			// Given
			SlotStatus status = SlotStatus.CLOSED;
			
			// Then: 휴무 등으로 운영하지 않는 상태를 나타냄
			assertThat(status).isNotNull();
			assertThat(status.name()).isEqualTo("CLOSED");
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
			SlotStatus status1 = SlotStatus.AVAILABLE;
			SlotStatus status2 = SlotStatus.AVAILABLE;
			
			// When & Then
			assertThat(status1).isEqualTo(status2);
			assertThat(status1 == status2).isTrue(); // Enum은 싱글톤
		}
		
		@Test
		@DisplayName("[정상] 다른 Enum 값은 동등하지 않다")
		void differentEnumValuesAreNotEqual() {
			// Given
			SlotStatus status1 = SlotStatus.AVAILABLE;
			SlotStatus status2 = SlotStatus.RESERVED;
			
			// When & Then
			assertThat(status1).isNotEqualTo(status2);
		}
	}
}
