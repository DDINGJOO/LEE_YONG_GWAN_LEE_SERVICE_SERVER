package com.teambind.springproject.room.command.application;

import com.teambind.springproject.common.exceptions.domain.SlotNotFoundException;
import com.teambind.springproject.common.exceptions.domain.InvalidSlotStateTransitionException;
import com.teambind.springproject.message.publish.EventPublisher;
import com.teambind.springproject.room.command.domain.service.TimeSlotManagementService;
import com.teambind.springproject.room.command.dto.SlotReservationRequest;
import com.teambind.springproject.room.event.event.SlotReservedEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ReservationApplicationService 단위 테스트.
 *
 * ApplicationService 계층의 Use Case 조율 로직을 Mocking하여 독립적으로 테스트한다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationApplicationService 단위 테스트")
class ReservationApplicationServiceTest {

	@Mock
	private TimeSlotManagementService timeSlotManagementService;

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private ReservationApplicationService service;

	private Long roomId;
	private LocalDate slotDate;
	private LocalTime slotTime;
	private Long reservationId;
	private SlotReservationRequest request;

	@BeforeEach
	void setUp() {
		roomId = 100L;
		slotDate = LocalDate.of(2025, 1, 15);
		slotTime = LocalTime.of(10, 0);
		reservationId = 1L;

		request = new SlotReservationRequest(roomId, slotDate, slotTime, reservationId);

		log.info("=== 테스트 데이터 초기화 ===");
		log.info("- roomId: {}", roomId);
		log.info("- slotDate: {}", slotDate);
		log.info("- slotTime: {}", slotTime);
		log.info("- reservationId: {}", reservationId);
	}

	@Test
	@DisplayName("정상적인 예약 생성 시나리오")
	void createReservation_Success() {
		log.info("=== [정상적인 예약 생성] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		doNothing().when(timeSlotManagementService)
				.markSlotAsPending(roomId, slotDate, slotTime, reservationId);
		doNothing().when(eventPublisher).publish(any(SlotReservedEvent.class));

		// When
		log.info("[When] createReservation() 호출");
		log.info("[When] - request: {}", request);
		service.createReservation(request);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] TimeSlotManagementService.markSlotAsPending() 호출 확인");
		verify(timeSlotManagementService, times(1))
				.markSlotAsPending(roomId, slotDate, slotTime, reservationId);
		log.info("[Then] - ✓ 도메인 서비스 호출 확인됨");

		log.info("[Then] [검증2] EventPublisher.publish() 호출 확인");
		ArgumentCaptor<SlotReservedEvent> eventCaptor = ArgumentCaptor.forClass(SlotReservedEvent.class);
		verify(eventPublisher, times(1)).publish(eventCaptor.capture());
		log.info("[Then] - ✓ 이벤트 발행 확인됨");

		log.info("[Then] [검증3] 발행된 이벤트 내용 검증");
		SlotReservedEvent publishedEvent = eventCaptor.getValue();
		log.info("[Then] - 예상 roomId: {}, 실제 roomId: {}", roomId, publishedEvent.getRoomId());
		log.info("[Then] - 예상 slotDate: {}, 실제 slotDate: {}", slotDate, publishedEvent.getSlotDate());
		log.info("[Then] - 예상 reservationId: {}, 실제 reservationId: {}",
				reservationId, publishedEvent.getReservationId());

		assertThat(publishedEvent.getRoomId()).isEqualTo(roomId);
		assertThat(publishedEvent.getSlotDate()).isEqualTo(slotDate);
		assertThat(publishedEvent.getStartTimes()).containsExactly(slotTime);
		assertThat(publishedEvent.getReservationId()).isEqualTo(reservationId);
		log.info("[Then] - ✓ 이벤트 내용 검증 완료");

		log.info("=== [정상적인 예약 생성] 테스트 성공 ===");
	}

	@Test
	@DisplayName("슬롯이 존재하지 않는 경우 SlotNotFoundException 발생")
	void createReservation_SlotNotFound() {
		log.info("=== [슬롯 미존재 예외] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		doThrow(new SlotNotFoundException(roomId, slotDate.toString(), slotTime.toString()))
				.when(timeSlotManagementService)
				.markSlotAsPending(roomId, slotDate, slotTime, reservationId);

		// When & Then
		log.info("[When & Then] createReservation() 호출 시 SlotNotFoundException 발생 확인");
		assertThatThrownBy(() -> service.createReservation(request))
				.isInstanceOf(SlotNotFoundException.class);
		log.info("[Then] - ✓ SlotNotFoundException 발생 확인됨");

		log.info("[Then] [검증] EventPublisher.publish() 호출되지 않았는지 확인");
		verify(eventPublisher, never()).publish(any());
		log.info("[Then] - ✓ 이벤트 미발행 확인됨");

		log.info("=== [슬롯 미존재 예외] 테스트 성공 ===");
	}

	@Test
	@DisplayName("슬롯이 이미 PENDING 상태인 경우 InvalidSlotStateTransitionException 발생")
	void createReservation_AlreadyPending() {
		log.info("=== [슬롯 상태 전이 예외] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		doThrow(new InvalidSlotStateTransitionException("PENDING", "PENDING"))
				.when(timeSlotManagementService)
				.markSlotAsPending(roomId, slotDate, slotTime, reservationId);

		// When & Then
		log.info("[When & Then] createReservation() 호출 시 InvalidSlotStateTransitionException 발생 확인");
		assertThatThrownBy(() -> service.createReservation(request))
				.isInstanceOf(InvalidSlotStateTransitionException.class);
		log.info("[Then] - ✓ InvalidSlotStateTransitionException 발생 확인됨");

		log.info("[Then] [검증] EventPublisher.publish() 호출되지 않았는지 확인");
		verify(eventPublisher, never()).publish(any());
		log.info("[Then] - ✓ 이벤트 미발행 확인됨");

		log.info("=== [슬롯 상태 전이 예외] 테스트 성공 ===");
	}

	@Test
	@DisplayName("Kafka 발행 실패 시에도 예외가 전파되지 않음")
	void createReservation_KafkaPublishFailure() {
		log.info("=== [Kafka 발행 실패] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		doNothing().when(timeSlotManagementService)
				.markSlotAsPending(roomId, slotDate, slotTime, reservationId);
		doThrow(new RuntimeException("Kafka connection failed"))
				.when(eventPublisher).publish(any(SlotReservedEvent.class));

		// When
		log.info("[When] createReservation() 호출 (Kafka 실패해도 예외 전파되지 않아야 함)");
		service.createReservation(request);
		log.info("[When] - 호출 완료 (예외 발생하지 않음)");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] TimeSlotManagementService.markSlotAsPending() 호출 확인");
		verify(timeSlotManagementService, times(1))
				.markSlotAsPending(roomId, slotDate, slotTime, reservationId);
		log.info("[Then] - ✓ 도메인 서비스 호출 확인됨 (DB 트랜잭션은 성공)");

		log.info("[Then] [검증2] EventPublisher.publish() 호출 시도 확인");
		verify(eventPublisher, times(1)).publish(any(SlotReservedEvent.class));
		log.info("[Then] - ✓ 이벤트 발행 시도 확인됨 (실패했지만 예외는 catch됨)");

		log.info("=== [Kafka 발행 실패] 테스트 성공 ===");
	}
}
