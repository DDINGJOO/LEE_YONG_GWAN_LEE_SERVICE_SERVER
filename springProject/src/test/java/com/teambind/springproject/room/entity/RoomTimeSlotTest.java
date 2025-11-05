package com.teambind.springproject.room.entity;

import com.teambind.springproject.common.exceptions.application.InvalidRequestException;
import com.teambind.springproject.common.exceptions.domain.InvalidSlotStateTransitionException;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RoomTimeSlot 엔티티 단위 테스트.
 *
 * 테스트 범위:
 *
 *
 * Factory 메서드를 통한 슬롯 생성
 * 상태 전이 (AVAILABLE → PENDING → RESERVED)
 * 예약 취소 및 복구
 * 예외 케이스 (잘못된 상태 전이)
 *
 */
@DisplayName("RoomTimeSlot 엔티티 테스트")
class RoomTimeSlotTest {
	
	// ============================================================
	// Factory Method 테스트
	// ============================================================
	
	@Nested
	@DisplayName("슬롯 생성 (Factory Methods)")
	class CreateSlotTests {
		
		@Test
		@DisplayName("[정상] 예약 가능한 슬롯을 생성한다")
		void createAvailableSlot() {
			// Given
			Long roomId = 101L;
			LocalDate date = LocalDate.of(2025, 1, 15);
			LocalTime time = LocalTime.of(14, 0);
			
			// When
			RoomTimeSlot slot = RoomTimeSlot.available(roomId, date, time);
			
			// Then
			assertThat(slot.getRoomId()).isEqualTo(roomId);
			assertThat(slot.getSlotDate()).isEqualTo(date);
			assertThat(slot.getSlotTime()).isEqualTo(time);
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
			assertThat(slot.getReservationId()).isNull();
			assertThat(slot.isAvailable()).isTrue();
			assertThat(slot.isReserved()).isFalse();
		}
		
		@Test
		@DisplayName("[정상] 휴무 슬롯을 생성한다")
		void createClosedSlot() {
			// Given
			Long roomId = 101L;
			LocalDate date = LocalDate.of(2025, 1, 1);
			LocalTime time = LocalTime.of(9, 0);
			
			// When
			RoomTimeSlot slot = RoomTimeSlot.closed(roomId, date, time);
			
			// Then
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.CLOSED);
			assertThat(slot.isAvailable()).isFalse();
		}
		
		@Test
		@DisplayName("[오류] roomId가 null이면 예외가 발생한다")
		void createSlotWithNullRoomId() {
			// Given
			Long roomId = null;
			LocalDate date = LocalDate.of(2025, 1, 15);
			LocalTime time = LocalTime.of(14, 0);
			
			// When & Then
			assertThatThrownBy(() -> RoomTimeSlot.available(roomId, date, time))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("roomId must not be null");
		}
		
		@Test
		@DisplayName("[오류] slotDate가 null이면 예외가 발생한다")
		void createSlotWithNullDate() {
			// Given
			Long roomId = 101L;
			LocalDate date = null;
			LocalTime time = LocalTime.of(14, 0);
			
			// When & Then
			assertThatThrownBy(() -> RoomTimeSlot.available(roomId, date, time))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("slotDate must not be null");
		}
		
		@Test
		@DisplayName("[오류] slotTime이 null이면 예외가 발생한다")
		void createSlotWithNullTime() {
			// Given
			Long roomId = 101L;
			LocalDate date = LocalDate.of(2025, 1, 15);
			LocalTime time = null;
			
			// When & Then
			assertThatThrownBy(() -> RoomTimeSlot.available(roomId, date, time))
					.isInstanceOf(NullPointerException.class)
					.hasMessageContaining("slotTime must not be null");
		}
	}
	
	// ============================================================
	// 상태 전이: AVAILABLE → PENDING
	// ============================================================
	
	@Nested
	@DisplayName("예약 대기 상태 전환 (AVAILABLE → PENDING)")
	class MarkAsPendingTests {
		
		@Test
		@DisplayName("[정상] AVAILABLE 슬롯을 PENDING 상태로 전환한다")
		void markAvailableSlotAsPending() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			Long reservationId = 1001L;
			
			// When
			slot.markAsPending(reservationId);
			
			// Then
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.PENDING);
			assertThat(slot.getReservationId()).isEqualTo(reservationId);
			assertThat(slot.isAvailable()).isFalse();
		}
		
		@Test
		@DisplayName("[오류] RESERVED 상태의 슬롯은 PENDING으로 전환할 수 없다")
		void cannotMarkReservedSlotAsPending() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			slot.markAsPending(1001L);
			slot.confirm();

			// When & Then
			assertThatThrownBy(() -> slot.markAsPending(1002L))
					.isInstanceOf(InvalidSlotStateTransitionException.class);
		}

		@Test
		@DisplayName("[오류] CLOSED 상태의 슬롯은 PENDING으로 전환할 수 없다")
		void cannotMarkClosedSlotAsPending() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.closed(101L, LocalDate.now(), LocalTime.of(14, 0));

			// When & Then
			assertThatThrownBy(() -> slot.markAsPending(1001L))
					.isInstanceOf(InvalidSlotStateTransitionException.class);
		}
		
		@Test
		@DisplayName("[오류] reservationId가 null이면 예외가 발생한다")
		void markAsPendingWithNullReservationId() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));

			// When & Then
			assertThatThrownBy(() -> slot.markAsPending(null))
					.isInstanceOf(InvalidRequestException.class)
					.hasMessageContaining("reservationId");
		}
	}
	
	// ============================================================
	// 상태 전이: PENDING → RESERVED
	// ============================================================
	
	@Nested
	@DisplayName("예약 확정 (PENDING → RESERVED)")
	class ConfirmTests {
		
		@Test
		@DisplayName("[정상] PENDING 슬롯을 RESERVED 상태로 확정한다")
		void confirmPendingSlot() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			slot.markAsPending(1001L);
			
			// When
			slot.confirm();
			
			// Then
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.RESERVED);
			assertThat(slot.getReservationId()).isEqualTo(1001L);
			assertThat(slot.isReserved()).isTrue();
		}
		
		@Test
		@DisplayName("[오류] AVAILABLE 상태의 슬롯은 직접 RESERVED로 전환할 수 없다")
		void cannotConfirmAvailableSlot() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));

			// When & Then
			assertThatThrownBy(slot::confirm)
					.isInstanceOf(InvalidSlotStateTransitionException.class);
		}

		@Test
		@DisplayName("[오류] 이미 RESERVED 상태인 슬롯은 다시 확정할 수 없다")
		void cannotConfirmReservedSlot() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			slot.markAsPending(1001L);
			slot.confirm();
			
			// When & Then
			assertThatThrownBy(slot::confirm)
					.isInstanceOf(InvalidSlotStateTransitionException.class);
		}
	}
	
	// ============================================================
	// 상태 전이: PENDING/RESERVED → CANCELLED
	// ============================================================
	
	@Nested
	@DisplayName("예약 취소 (PENDING/RESERVED → CANCELLED)")
	class CancelTests {
		
		@Test
		@DisplayName("[정상] PENDING 상태의 슬롯을 취소한다")
		void cancelPendingSlot() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			slot.markAsPending(1001L);
			
			// When
			slot.cancel();
			
			// Then
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.CANCELLED);
			assertThat(slot.getReservationId()).isEqualTo(null); // 예약 ID는 유지 X
		}
		
		@Test
		@DisplayName("[정상] RESERVED 상태의 슬롯을 취소한다")
		void cancelReservedSlot() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			slot.markAsPending(1001L);
			slot.confirm();
			
			// When
			slot.cancel();
			
			// Then
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.CANCELLED);
		}
		
		@Test
		@DisplayName("[오류] AVAILABLE 상태의 슬롯은 취소할 수 없다")
		void cannotCancelAvailableSlot() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			
			// When & Then
			assertThatThrownBy(slot::cancel)
					.isInstanceOf(InvalidSlotStateTransitionException.class);
		}
		
		@Test
		@DisplayName("[오류] CLOSED 상태의 슬롯은 취소할 수 없다")
		void cannotCancelClosedSlot() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.closed(101L, LocalDate.now(), LocalTime.of(14, 0));
			
			// When & Then
			assertThatThrownBy(slot::cancel)
					.isInstanceOf(InvalidSlotStateTransitionException.class);
		}
	}
	
	// ============================================================
	// 상태 전이: CANCELLED → AVAILABLE
	// ============================================================
	
	@Nested
	@DisplayName("슬롯 복구 (CANCELLED → AVAILABLE)")
	class RestoreTests {
		
		@Test
		@DisplayName("[정상] CANCELLED 슬롯을 AVAILABLE 상태로 복구한다")
		void restoreCancelledSlot() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			slot.markAsPending(1001L);
			slot.cancel();
			
			// When
			slot.restore();
			
			// Then
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
			assertThat(slot.getReservationId()).isNull(); // 예약 ID 제거됨
			assertThat(slot.isAvailable()).isTrue();
		}
		
		@Test
		@DisplayName("[오류] AVAILABLE 상태의 슬롯은 복구할 수 없다")
		void cannotRestoreAvailableSlot() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			
			// When & Then
			assertThatThrownBy(slot::restore)
					.isInstanceOf(InvalidSlotStateTransitionException.class);
		}
		
		@Test
		@DisplayName("[오류] RESERVED 상태의 슬롯은 복구할 수 없다")
		void cannotRestoreReservedSlot() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			slot.markAsPending(1001L);
			slot.confirm();
			
			// When & Then
			assertThatThrownBy(slot::restore)
					.isInstanceOf(InvalidSlotStateTransitionException.class);
		}
	}
	
	// ============================================================
	// 엣지 케이스: 상태 전이 시나리오
	// ============================================================
	
	@Nested
	@DisplayName("복잡한 상태 전이 시나리오")
	class ComplexStateTransitionTests {
		
		@Test
		@DisplayName("[정상] 완전한 예약 플로우: AVAILABLE → PENDING → RESERVED")
		void fullReservationFlow() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			
			// When: 예약 시작
			slot.markAsPending(1001L);
			
			// Then: PENDING 상태 확인
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.PENDING);
			assertThat(slot.getReservationId()).isEqualTo(1001L);
			
			// When: 결제 완료
			slot.confirm();
			
			// Then: RESERVED 상태 확인
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.RESERVED);
			assertThat(slot.isReserved()).isTrue();
		}
		
		@Test
		@DisplayName("[정상] 예약 실패 플로우: AVAILABLE → PENDING → CANCELLED → AVAILABLE")
		void reservationFailureFlow() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			
			// When: 예약 시작
			slot.markAsPending(1001L);
			
			// When: 결제 실패로 취소
			slot.cancel();
			
			// Then: CANCELLED 상태 확인
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.CANCELLED);
			
			// When: 슬롯 복구
			slot.restore();
			
			// Then: AVAILABLE 상태로 복구
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
			assertThat(slot.getReservationId()).isNull();
			assertThat(slot.isAvailable()).isTrue();
		}
		
		@Test
		@DisplayName("[정상] 예약 후 취소 플로우: AVAILABLE → PENDING → RESERVED → CANCELLED")
		void reservationCancellationFlow() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			
			// When: 예약 완료
			slot.markAsPending(1001L);
			slot.confirm();
			
			// Then: RESERVED 상태 확인
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.RESERVED);
			
			// When: 사용자가 예약 취소
			slot.cancel();
			
			// Then: CANCELLED 상태
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.CANCELLED);
		}
		
		@Test
		@DisplayName("[오류] PENDING 상태에서 다른 예약 ID로 덮어쓸 수 없다")
		void cannotOverwritePendingReservation() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			slot.markAsPending(1001L);
			
			// When & Then: 다른 예약 ID로 시도
			assertThatThrownBy(() -> slot.markAsPending(1002L))
					.isInstanceOf(InvalidSlotStateTransitionException.class);
		}
	}
	
	// ============================================================
	// equals & hashCode 테스트
	// ============================================================
	
	@Nested
	@DisplayName("동등성 비교 (equals & hashCode)")
	class EqualsAndHashCodeTests {
		
		@Test
		@DisplayName("[정상] slotId가 모두 null인 경우 동등하다")
		void slotsWithNullIdAreEqual() {
			// Given: Factory 메서드로 생성한 슬롯은 slotId가 null
			RoomTimeSlot slot1 = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			RoomTimeSlot slot2 = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			
			// When & Then: Objects.equals(null, null)은 true를 반환
			assertThat(slot1).isEqualTo(slot2);
		}
		
		@Test
		@DisplayName("[정상] 같은 인스턴스는 자기 자신과 동등하다")
		void slotIsEqualToItself() {
			// Given
			RoomTimeSlot slot = RoomTimeSlot.available(101L, LocalDate.now(), LocalTime.of(14, 0));
			
			// When & Then
			assertThat(slot).isEqualTo(slot);
		}
	}
}
