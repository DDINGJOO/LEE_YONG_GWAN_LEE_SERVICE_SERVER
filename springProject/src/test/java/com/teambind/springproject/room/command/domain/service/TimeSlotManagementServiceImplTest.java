package com.teambind.springproject.room.command.domain.service;

import com.teambind.springproject.common.exceptions.domain.SlotNotFoundException;
import com.teambind.springproject.room.domain.port.TimeSlotPort;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import com.teambind.springproject.room.event.event.SlotCancelledEvent;
import com.teambind.springproject.room.event.event.SlotConfirmedEvent;
import com.teambind.springproject.room.event.event.SlotReservedEvent;
import com.teambind.springproject.room.event.event.SlotRestoredEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TimeSlotManagementServiceImpl 단위 테스트.
 *
 * DomainService 계층의 비즈니스 로직을 Port를 Mocking하여 독립적으로 테스트한다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("TimeSlotManagementServiceImpl 단위 테스트")
class TimeSlotManagementServiceImplTest {

	@Mock
	private TimeSlotPort timeSlotPort;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private TimeSlotManagementServiceImpl service;

	private Long roomId;
	private LocalDate slotDate;
	private LocalTime slotTime;
	private Long reservationId;
	private RoomTimeSlot availableSlot;

	@BeforeEach
	void setUp() {
		roomId = 100L;
		slotDate = LocalDate.of(2025, 1, 15);
		slotTime = LocalTime.of(10, 0);
		reservationId = 1L;
		availableSlot = RoomTimeSlot.available(roomId, slotDate, slotTime);

		log.info("=== 테스트 데이터 초기화 ===");
		log.info("- roomId: {}", roomId);
		log.info("- slotDate: {}", slotDate);
		log.info("- slotTime: {}", slotTime);
		log.info("- reservationId: {}", reservationId);
	}

	@Test
	@DisplayName("슬롯을 PENDING 상태로 변경한다")
	void markSlotAsPending() {
		log.info("=== [슬롯 PENDING 상태 변경] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		log.info("[Given] - timeSlotPort.findByRoomIdAndSlotDateAndSlotTime() -> 슬롯 반환");
		when(timeSlotPort.findByRoomIdAndSlotDateAndSlotTime(roomId, slotDate, slotTime))
				.thenReturn(Optional.of(availableSlot));
		log.info("[Given] - 초기 슬롯 상태: {}", availableSlot.getStatus());

		// When
		log.info("[When] markSlotAsPending() 호출");
		log.info("[When] - 파라미터: roomId={}, slotDate={}, slotTime={}, reservationId={}",
				roomId, slotDate, slotTime, reservationId);
		service.markSlotAsPending(roomId, slotDate, slotTime, reservationId);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 슬롯 상태가 PENDING으로 변경되었는지 확인");
		log.info("[Then] - 기존 상태: {}", SlotStatus.AVAILABLE);
		log.info("[Then] - 예상 상태: {}", SlotStatus.PENDING);
		log.info("[Then] - 실제 상태: {}", availableSlot.getStatus());
		assertThat(availableSlot.getStatus()).isEqualTo(SlotStatus.PENDING);
		log.info("[Then] - ✓ 상태 변경 확인됨");

		log.info("[Then] [검증2] reservationId가 설정되었는지 확인");
		log.info("[Then] - 예상 reservationId: {}", reservationId);
		log.info("[Then] - 실제 reservationId: {}", availableSlot.getReservationId());
		assertThat(availableSlot.getReservationId()).isEqualTo(reservationId);
		log.info("[Then] - ✓ reservationId 설정 확인됨");

		log.info("[Then] [검증3] timeSlotPort.save()가 1번 호출되었는지 확인");
		verify(timeSlotPort, times(1)).save(availableSlot);
		log.info("[Then] - ✓ save() 호출 확인됨");

		log.info("[Then] [검증4] SlotReservedEvent가 발행되었는지 확인");
		ArgumentCaptor<SlotReservedEvent> eventCaptor = ArgumentCaptor.forClass(SlotReservedEvent.class);
		verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
		SlotReservedEvent publishedEvent = eventCaptor.getValue();
		log.info("[Then] - 예상 roomId: {}, 실제 roomId: {}", roomId, publishedEvent.getRoomId());
		log.info("[Then] - 예상 reservationId: {}, 실제 reservationId: {}",
				reservationId, publishedEvent.getReservationId());
		assertThat(publishedEvent.getRoomId()).isEqualTo(roomId);
		assertThat(publishedEvent.getReservationId()).isEqualTo(reservationId);
		log.info("[Then] - ✓ 이벤트 발행 확인됨");

		log.info("=== [슬롯 PENDING 상태 변경] 테스트 성공 ===");
	}

	@Test
	@DisplayName("슬롯을 RESERVED 상태로 확정한다")
	void confirmSlot() {
		log.info("=== [슬롯 RESERVED 상태 확정] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		availableSlot.markAsPending(reservationId);
		log.info("[Given] - 슬롯을 PENDING 상태로 설정: reservationId={}", reservationId);
		when(timeSlotPort.findByRoomIdAndSlotDateAndSlotTime(roomId, slotDate, slotTime))
				.thenReturn(Optional.of(availableSlot));
		log.info("[Given] - 초기 슬롯 상태: {}", availableSlot.getStatus());

		// When
		log.info("[When] confirmSlot() 호출");
		log.info("[When] - 파라미터: roomId={}, slotDate={}, slotTime={}",
				roomId, slotDate, slotTime);
		service.confirmSlot(roomId, slotDate, slotTime);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 슬롯 상태가 RESERVED로 변경되었는지 확인");
		log.info("[Then] - 기존 상태: {}", SlotStatus.PENDING);
		log.info("[Then] - 예상 상태: {}", SlotStatus.RESERVED);
		log.info("[Then] - 실제 상태: {}", availableSlot.getStatus());
		assertThat(availableSlot.getStatus()).isEqualTo(SlotStatus.RESERVED);
		log.info("[Then] - ✓ 상태 변경 확인됨");

		log.info("[Then] [검증2] timeSlotPort.save()가 1번 호출되었는지 확인");
		verify(timeSlotPort, times(1)).save(availableSlot);
		log.info("[Then] - ✓ save() 호출 확인됨");

		log.info("[Then] [검증3] SlotConfirmedEvent가 발행되었는지 확인");
		ArgumentCaptor<SlotConfirmedEvent> eventCaptor = ArgumentCaptor.forClass(SlotConfirmedEvent.class);
		verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
		SlotConfirmedEvent publishedEvent = eventCaptor.getValue();
		log.info("[Then] - 예상 reservationId: {}, 실제 reservationId: {}",
				reservationId, publishedEvent.getReservationId());
		assertThat(publishedEvent.getReservationId()).isEqualTo(reservationId);
		log.info("[Then] - ✓ 이벤트 발행 확인됨");

		log.info("=== [슬롯 RESERVED 상태 확정] 테스트 성공 ===");
	}

	@Test
	@DisplayName("슬롯을 취소하고 AVAILABLE 상태로 복구한다")
	void cancelSlot() {
		log.info("=== [슬롯 취소 및 복구] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		availableSlot.markAsPending(reservationId);
		log.info("[Given] - 슬롯을 PENDING 상태로 설정: reservationId={}", reservationId);
		when(timeSlotPort.findByRoomIdAndSlotDateAndSlotTime(roomId, slotDate, slotTime))
				.thenReturn(Optional.of(availableSlot));
		log.info("[Given] - 초기 슬롯 상태: {}", availableSlot.getStatus());

		// When
		log.info("[When] cancelSlot() 호출");
		log.info("[When] - 파라미터: roomId={}, slotDate={}, slotTime={}",
				roomId, slotDate, slotTime);
		service.cancelSlot(roomId, slotDate, slotTime);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 슬롯 상태가 CANCELLED로 변경되었는지 확인");
		log.info("[Then] - 기존 상태: {}", SlotStatus.PENDING);
		log.info("[Then] - 예상 상태: {}", SlotStatus.CANCELLED);
		log.info("[Then] - 실제 상태: {}", availableSlot.getStatus());
		assertThat(availableSlot.getStatus()).isEqualTo(SlotStatus.CANCELLED);
		log.info("[Then] - ✓ 취소 상태 확인됨");

		log.info("[Then] [검증2] reservationId가 null로 초기화되었는지 확인");
		log.info("[Then] - 실제 reservationId: {}", availableSlot.getReservationId());
		assertThat(availableSlot.getReservationId()).isNull();
		log.info("[Then] - ✓ reservationId 초기화 확인됨");

		log.info("[Then] [검증3] timeSlotPort.save()가 1번 호출되었는지 확인");
		verify(timeSlotPort, times(1)).save(availableSlot);
		log.info("[Then] - ✓ save() 호출 확인됨");

		log.info("[Then] [검증4] SlotCancelledEvent가 발행되었는지 확인");
		ArgumentCaptor<SlotCancelledEvent> eventCaptor = ArgumentCaptor.forClass(SlotCancelledEvent.class);
		verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
		SlotCancelledEvent publishedEvent = eventCaptor.getValue();
		log.info("[Then] - 예상 reservationId: {}, 실제 reservationId: {}",
				reservationId, publishedEvent.getReservationId());
		assertThat(publishedEvent.getReservationId()).isEqualTo(reservationId);
		log.info("[Then] - ✓ 이벤트 발행 확인됨");

		log.info("=== [슬롯 취소 및 복구] 테스트 성공 ===");
	}

	@Test
	@DisplayName("예약 ID로 연관된 모든 슬롯을 취소한다")
	void cancelSlotsByReservationId() {
		log.info("=== [예약 ID로 슬롯 일괄 취소] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		RoomTimeSlot slot1 = RoomTimeSlot.available(roomId, slotDate, LocalTime.of(10, 0));
		RoomTimeSlot slot2 = RoomTimeSlot.available(roomId, slotDate, LocalTime.of(11, 0));
		RoomTimeSlot slot3 = RoomTimeSlot.available(roomId, slotDate, LocalTime.of(12, 0));

		slot1.markAsPending(reservationId);
		slot2.markAsPending(reservationId);
		slot3.markAsPending(reservationId);

		List<RoomTimeSlot> slots = List.of(slot1, slot2, slot3);
		log.info("[Given] - 3개의 PENDING 슬롯 준비: reservationId={}", reservationId);

		when(timeSlotPort.findByReservationId(reservationId)).thenReturn(slots);
		log.info("[Given] - timeSlotPort.findByReservationId() -> 3개 슬롯 반환");

		// When
		log.info("[When] cancelSlotsByReservationId() 호출");
		log.info("[When] - 파라미터: reservationId={}", reservationId);
		service.cancelSlotsByReservationId(reservationId);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 모든 슬롯이 CANCELLED 상태로 변경되었는지 확인");
		for (int i = 0; i < slots.size(); i++) {
			RoomTimeSlot slot = slots.get(i);
			log.info("[Then] - Slot{} 상태: 예상={}, 실제={}",
					i + 1, SlotStatus.CANCELLED, slot.getStatus());
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.CANCELLED);
		}
		log.info("[Then] - ✓ 모든 슬롯 취소 확인됨");

		log.info("[Then] [검증2] timeSlotPort.saveAll()이 1번 호출되었는지 확인");
		verify(timeSlotPort, times(1)).saveAll(slots);
		log.info("[Then] - ✓ saveAll() 호출 확인됨");

		log.info("[Then] [검증3] SlotCancelledEvent가 1번 발행되었는지 확인");
		verify(eventPublisher, times(1)).publishEvent(any(SlotCancelledEvent.class));
		log.info("[Then] - ✓ 이벤트 발행 확인됨 (일괄 처리)");

		log.info("=== [예약 ID로 슬롯 일괄 취소] 테스트 성공 ===");
	}

	@Test
	@DisplayName("만료된 PENDING 슬롯을 AVAILABLE 상태로 복구한다")
	void restoreExpiredPendingSlots() {
		log.info("=== [만료된 PENDING 슬롯 복구] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		RoomTimeSlot expiredSlot1 = RoomTimeSlot.available(roomId, slotDate, LocalTime.of(10, 0));
		RoomTimeSlot expiredSlot2 = RoomTimeSlot.available(roomId, slotDate, LocalTime.of(11, 0));

		expiredSlot1.markAsPending(1L);
		expiredSlot2.markAsPending(2L);

		List<RoomTimeSlot> expiredSlots = List.of(expiredSlot1, expiredSlot2);
		log.info("[Given] - 2개의 만료된 PENDING 슬롯 준비");

		when(timeSlotPort.findExpiredPendingSlots(15)).thenReturn(expiredSlots);
		log.info("[Given] - timeSlotPort.findExpiredPendingSlots(15) -> 2개 슬롯 반환");

		// When
		log.info("[When] restoreExpiredPendingSlots() 호출");
		int restoredCount = service.restoreExpiredPendingSlots();
		log.info("[When] - 호출 완료: restoredCount={}", restoredCount);

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 복구된 슬롯 개수 확인");
		log.info("[Then] - 예상 개수: 2");
		log.info("[Then] - 실제 개수: {}", restoredCount);
		assertThat(restoredCount).isEqualTo(2);
		log.info("[Then] - ✓ 복구 개수 확인됨");

		log.info("[Then] [검증2] 모든 슬롯이 AVAILABLE 상태로 복구되었는지 확인");
		for (int i = 0; i < expiredSlots.size(); i++) {
			RoomTimeSlot slot = expiredSlots.get(i);
			log.info("[Then] - Slot{} 상태: 예상={}, 실제={}",
					i + 1, SlotStatus.AVAILABLE, slot.getStatus());
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
		}
		log.info("[Then] - ✓ 모든 슬롯 상태 복구 확인됨");

		log.info("[Then] [검증3] timeSlotPort.saveAll()이 1번 호출되었는지 확인");
		verify(timeSlotPort, times(1)).saveAll(expiredSlots);
		log.info("[Then] - ✓ saveAll() 호출 확인됨");

		log.info("[Then] [검증4] SlotRestoredEvent가 2번 발행되었는지 확인 (각 슬롯마다)");
		verify(eventPublisher, times(2)).publishEvent(any(SlotRestoredEvent.class));
		log.info("[Then] - ✓ 이벤트 발행 확인됨");

		log.info("=== [만료된 PENDING 슬롯 복구] 테스트 성공 ===");
	}

	@Test
	@DisplayName("존재하지 않는 슬롯을 PENDING으로 변경하면 예외가 발생한다")
	void markSlotAsPending_notFound() {
		log.info("=== [존재하지 않는 슬롯 PENDING 변경 예외] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		log.info("[Given] - timeSlotPort.findByRoomIdAndSlotDateAndSlotTime() -> 빈 Optional 반환");
		when(timeSlotPort.findByRoomIdAndSlotDateAndSlotTime(roomId, slotDate, slotTime))
				.thenReturn(Optional.empty());

		// When & Then
		log.info("[When & Then] markSlotAsPending() 호출 시 SlotNotFoundException 발생");
		log.info("[When & Then] - 파라미터: roomId={}, slotDate={}, slotTime={}",
				roomId, slotDate, slotTime);

		assertThatThrownBy(() -> service.markSlotAsPending(roomId, slotDate, slotTime, reservationId))
				.isInstanceOf(SlotNotFoundException.class);

		log.info("[Then] - ✓ SlotNotFoundException 발생 확인됨");

		log.info("[Then] [검증] save()와 이벤트 발행이 호출되지 않았는지 확인");
		verify(timeSlotPort, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
		log.info("[Then] - ✓ save()와 publishEvent() 미호출 확인됨");

		log.info("=== [존재하지 않는 슬롯 PENDING 변경 예외] 테스트 성공 ===");
	}

	@Test
	@DisplayName("만료된 슬롯이 없으면 0을 반환한다")
	void restoreExpiredPendingSlots_noExpiredSlots() {
		log.info("=== [만료된 슬롯 없음] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		log.info("[Given] - timeSlotPort.findExpiredPendingSlots(15) -> 빈 리스트 반환");
		when(timeSlotPort.findExpiredPendingSlots(15)).thenReturn(List.of());

		// When
		log.info("[When] restoreExpiredPendingSlots() 호출");
		int restoredCount = service.restoreExpiredPendingSlots();
		log.info("[When] - 호출 완료: restoredCount={}", restoredCount);

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 복구된 슬롯 개수가 0인지 확인");
		log.info("[Then] - 예상 개수: 0");
		log.info("[Then] - 실제 개수: {}", restoredCount);
		assertThat(restoredCount).isEqualTo(0);
		log.info("[Then] - ✓ 복구 개수 확인됨");

		log.info("[Then] [검증2] saveAll()이 1번 호출되었는지 확인 (빈 리스트로)");
		verify(timeSlotPort, times(1)).saveAll(List.of());
		log.info("[Then] - ✓ saveAll() 호출 확인됨");

		log.info("[Then] [검증3] 이벤트가 발행되지 않았는지 확인");
		verify(eventPublisher, never()).publishEvent(any());
		log.info("[Then] - ✓ 이벤트 미발행 확인됨");

		log.info("=== [만료된 슬롯 없음] 테스트 성공 ===");
	}
}
