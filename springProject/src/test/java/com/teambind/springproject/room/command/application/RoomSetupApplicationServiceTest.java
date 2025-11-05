package com.teambind.springproject.room.command.application;

import com.teambind.springproject.common.exceptions.domain.RequestNotFoundException;
import com.teambind.springproject.message.publish.EventPublisher;
import com.teambind.springproject.room.command.dto.RoomOperatingPolicySetupRequest;
import com.teambind.springproject.room.command.dto.WeeklySlotDto;
import com.teambind.springproject.room.domain.port.OperatingPolicyPort;
import com.teambind.springproject.room.domain.port.SlotGenerationRequestPort;
import com.teambind.springproject.room.entity.RoomOperatingPolicy;
import com.teambind.springproject.room.entity.SlotGenerationRequest;
import com.teambind.springproject.room.entity.enums.GenerationStatus;
import com.teambind.springproject.room.entity.enums.RecurrencePattern;
import com.teambind.springproject.room.event.event.SlotGenerationRequestedEvent;
import com.teambind.springproject.room.query.dto.RoomSetupResponse;
import com.teambind.springproject.room.query.dto.SlotGenerationStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RoomSetupApplicationService 단위 테스트.
 *
 * ApplicationService 계층의 Use Case 조율 로직을 Port를 Mocking하여 독립적으로 테스트한다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("RoomSetupApplicationService 단위 테스트")
class RoomSetupApplicationServiceTest {

	@Mock
	private OperatingPolicyPort operatingPolicyPort;

	@Mock
	private SlotGenerationRequestPort slotGenerationRequestPort;

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private RoomSetupApplicationService service;

	private Long roomId;
	private RoomOperatingPolicySetupRequest setupRequest;

	@BeforeEach
	void setUp() {
		roomId = 100L;

		// 월요일 09:00, 10:00 슬롯 설정
		WeeklySlotDto slotDto = new WeeklySlotDto(
				DayOfWeek.MONDAY,
				RecurrencePattern.EVERY_WEEK,
				List.of(LocalTime.of(9, 0), LocalTime.of(10, 0))
		);

		setupRequest = new RoomOperatingPolicySetupRequest(roomId, List.of(slotDto));

		log.info("=== 테스트 데이터 초기화 ===");
		log.info("- roomId: {}", roomId);
		log.info("- slotDto: {}", slotDto);
	}

	@Test
	@DisplayName("룸 운영 정책을 설정하고 슬롯 생성을 요청한다")
	void setupRoom() {
		log.info("=== [룸 운영 정책 설정 및 슬롯 생성 요청] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		RoomOperatingPolicy savedPolicy = mock(RoomOperatingPolicy.class);
		when(operatingPolicyPort.save(any(RoomOperatingPolicy.class))).thenReturn(savedPolicy);
		log.info("[Given] - operatingPolicyPort.save() -> 정책 저장 성공");

		SlotGenerationRequest savedRequest = SlotGenerationRequest.create(
				"test-request-id",
				roomId,
				LocalDate.now(),
				LocalDate.now().plusMonths(2)
		);
		when(slotGenerationRequestPort.save(any(SlotGenerationRequest.class))).thenReturn(savedRequest);
		log.info("[Given] - slotGenerationRequestPort.save() -> 요청 저장 성공");

		// When
		log.info("[When] setupRoom() 호출");
		log.info("[When] - 파라미터: request={}", setupRequest);
		RoomSetupResponse response = service.setupRoom(setupRequest);
		log.info("[When] - 호출 완료: requestId={}", response.getRequestId());

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 응답 데이터 확인");
		log.info("[Then] - 응답 roomId: {}", response.getRoomId());
		log.info("[Then] - 응답 status: {}", response.getStatus());
		assertThat(response.getRoomId()).isEqualTo(roomId);
		assertThat(response.getRequestId()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(GenerationStatus.REQUESTED);
		assertThat(response.getStartDate()).isNotNull();
		assertThat(response.getEndDate()).isNotNull();
		log.info("[Then] - ✓ 응답 데이터 확인됨");

		log.info("[Then] [검증2] operatingPolicyPort.save()가 1번 호출되었는지 확인");
		ArgumentCaptor<RoomOperatingPolicy> policyCaptor = ArgumentCaptor.forClass(RoomOperatingPolicy.class);
		verify(operatingPolicyPort, times(1)).save(policyCaptor.capture());
		RoomOperatingPolicy capturedPolicy = policyCaptor.getValue();
		log.info("[Then] - 저장된 정책 roomId: {}", capturedPolicy.getRoomId());
		log.info("[Then] - 저장된 정책 recurrence: {}", capturedPolicy.getRecurrence());
		assertThat(capturedPolicy.getRoomId()).isEqualTo(roomId);
		assertThat(capturedPolicy.getRecurrence()).isEqualTo(RecurrencePattern.EVERY_WEEK);
		log.info("[Then] - ✓ 정책 저장 확인됨");

		log.info("[Then] [검증3] slotGenerationRequestPort.save()가 1번 호출되었는지 확인");
		ArgumentCaptor<SlotGenerationRequest> requestCaptor = ArgumentCaptor.forClass(SlotGenerationRequest.class);
		verify(slotGenerationRequestPort, times(1)).save(requestCaptor.capture());
		SlotGenerationRequest capturedRequest = requestCaptor.getValue();
		log.info("[Then] - 저장된 요청 roomId: {}", capturedRequest.getRoomId());
		log.info("[Then] - 저장된 요청 status: {}", capturedRequest.getStatus());
		assertThat(capturedRequest.getRoomId()).isEqualTo(roomId);
		assertThat(capturedRequest.getStatus()).isEqualTo(GenerationStatus.REQUESTED);
		log.info("[Then] - ✓ 요청 저장 확인됨");

		log.info("[Then] [검증4] SlotGenerationRequestedEvent가 발행되었는지 확인");
		ArgumentCaptor<SlotGenerationRequestedEvent> eventCaptor = ArgumentCaptor.forClass(SlotGenerationRequestedEvent.class);
		verify(eventPublisher, times(1)).publish(eventCaptor.capture());
		SlotGenerationRequestedEvent publishedEvent = eventCaptor.getValue();
		log.info("[Then] - 이벤트 requestId: {}", publishedEvent.getRequestId());
		log.info("[Then] - 이벤트 roomId: {}", publishedEvent.getRoomId());
		assertThat(publishedEvent.getRoomId()).isEqualTo(roomId);
		assertThat(publishedEvent.getRequestId()).isNotNull();
		log.info("[Then] - ✓ 이벤트 발행 확인됨");

		log.info("=== [룸 운영 정책 설정 및 슬롯 생성 요청] 테스트 성공 ===");
	}

	@Test
	@DisplayName("빈 슬롯 목록으로 설정 시 기본 recurrence 패턴을 사용한다")
	void setupRoom_emptySlots() {
		log.info("=== [빈 슬롯 목록 설정] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		RoomOperatingPolicySetupRequest emptyRequest = new RoomOperatingPolicySetupRequest(roomId, List.of());
		log.info("[Given] - 빈 슬롯 목록으로 요청 생성");

		RoomOperatingPolicy savedPolicy = mock(RoomOperatingPolicy.class);
		when(operatingPolicyPort.save(any(RoomOperatingPolicy.class))).thenReturn(savedPolicy);

		SlotGenerationRequest savedRequest = SlotGenerationRequest.create(
				"test-request-id",
				roomId,
				LocalDate.now(),
				LocalDate.now().plusMonths(2)
		);
		when(slotGenerationRequestPort.save(any(SlotGenerationRequest.class))).thenReturn(savedRequest);

		// When
		log.info("[When] setupRoom() 호출");
		log.info("[When] - 파라미터: emptyRequest (빈 슬롯 목록)");
		RoomSetupResponse response = service.setupRoom(emptyRequest);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 기본 recurrence 패턴(EVERY_WEEK)이 사용되었는지 확인");
		ArgumentCaptor<RoomOperatingPolicy> policyCaptor = ArgumentCaptor.forClass(RoomOperatingPolicy.class);
		verify(operatingPolicyPort, times(1)).save(policyCaptor.capture());
		RoomOperatingPolicy capturedPolicy = policyCaptor.getValue();
		log.info("[Then] - 예상 recurrence: {}", RecurrencePattern.EVERY_WEEK);
		log.info("[Then] - 실제 recurrence: {}", capturedPolicy.getRecurrence());
		assertThat(capturedPolicy.getRecurrence()).isEqualTo(RecurrencePattern.EVERY_WEEK);
		log.info("[Then] - ✓ 기본 패턴 사용 확인됨");

		log.info("=== [빈 슬롯 목록 설정] 테스트 성공 ===");
	}

	@Test
	@DisplayName("슬롯 생성 상태를 조회한다")
	void getStatus() {
		log.info("=== [슬롯 생성 상태 조회] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		String requestId = "test-request-id";
		SlotGenerationRequest request = SlotGenerationRequest.create(
				requestId,
				roomId,
				LocalDate.now(),
				LocalDate.now().plusMonths(2)
		);
		request.markAsInProgress();
		log.info("[Given] - 슬롯 생성 요청 준비: requestId={}, status={}", requestId, request.getStatus());

		when(slotGenerationRequestPort.findById(requestId)).thenReturn(Optional.of(request));
		log.info("[Given] - slotGenerationRequestPort.findById() -> 요청 반환");

		// When
		log.info("[When] getStatus() 호출");
		log.info("[When] - 파라미터: requestId={}", requestId);
		SlotGenerationStatusResponse response = service.getStatus(requestId);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 응답 데이터 확인");
		log.info("[Then] - 예상 requestId: {}, 실제 requestId: {}", requestId, response.getRequestId());
		log.info("[Then] - 예상 roomId: {}, 실제 roomId: {}", roomId, response.getRoomId());
		log.info("[Then] - 예상 status: {}, 실제 status: {}",
				GenerationStatus.IN_PROGRESS, response.getStatus());
		assertThat(response.getRequestId()).isEqualTo(requestId);
		assertThat(response.getRoomId()).isEqualTo(roomId);
		assertThat(response.getStatus()).isEqualTo(GenerationStatus.IN_PROGRESS);
		log.info("[Then] - ✓ 응답 데이터 확인됨");

		log.info("[Then] [검증2] slotGenerationRequestPort.findById()가 1번 호출되었는지 확인");
		verify(slotGenerationRequestPort, times(1)).findById(requestId);
		log.info("[Then] - ✓ findById() 호출 확인됨");

		log.info("=== [슬롯 생성 상태 조회] 테스트 성공 ===");
	}

	@Test
	@DisplayName("존재하지 않는 요청 ID로 상태 조회 시 예외가 발생한다")
	void getStatus_notFound() {
		log.info("=== [존재하지 않는 요청 상태 조회 예외] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		String nonExistentRequestId = "non-existent-id";
		when(slotGenerationRequestPort.findById(nonExistentRequestId)).thenReturn(Optional.empty());
		log.info("[Given] - slotGenerationRequestPort.findById() -> 빈 Optional 반환");

		// When & Then
		log.info("[When & Then] getStatus() 호출 시 RequestNotFoundException 발생");
		log.info("[When & Then] - 파라미터: requestId={}", nonExistentRequestId);

		assertThatThrownBy(() -> service.getStatus(nonExistentRequestId))
				.isInstanceOf(RequestNotFoundException.class)
				.hasMessageContaining(nonExistentRequestId);

		log.info("[Then] - ✓ RequestNotFoundException 발생 확인됨");

		log.info("=== [존재하지 않는 요청 상태 조회 예외] 테스트 성공 ===");
	}

	@Test
	@DisplayName("여러 슬롯 시간을 가진 설정 요청을 처리한다")
	void setupRoom_multipleSlotTimes() {
		log.info("=== [여러 슬롯 시간 설정] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		WeeklySlotDto mondaySlot = new WeeklySlotDto(
				DayOfWeek.MONDAY,
				RecurrencePattern.EVERY_WEEK,
				List.of(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0))
		);
		WeeklySlotDto tuesdaySlot = new WeeklySlotDto(
				DayOfWeek.TUESDAY,
				RecurrencePattern.EVERY_WEEK,
				List.of(LocalTime.of(14, 0), LocalTime.of(15, 0))
		);

		RoomOperatingPolicySetupRequest multiSlotRequest = new RoomOperatingPolicySetupRequest(
				roomId,
				List.of(mondaySlot, tuesdaySlot)
		);
		log.info("[Given] - 여러 요일/시간 슬롯 요청 생성");
		log.info("[Given]   - Monday: 09:00, 10:00, 11:00");
		log.info("[Given]   - Tuesday: 14:00, 15:00");

		RoomOperatingPolicy savedPolicy = mock(RoomOperatingPolicy.class);
		when(operatingPolicyPort.save(any(RoomOperatingPolicy.class))).thenReturn(savedPolicy);

		SlotGenerationRequest savedRequest = SlotGenerationRequest.create(
				"test-request-id",
				roomId,
				LocalDate.now(),
				LocalDate.now().plusMonths(2)
		);
		when(slotGenerationRequestPort.save(any(SlotGenerationRequest.class))).thenReturn(savedRequest);

		// When
		log.info("[When] setupRoom() 호출");
		log.info("[When] - 파라미터: multiSlotRequest (여러 슬롯)");
		RoomSetupResponse response = service.setupRoom(multiSlotRequest);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 응답 데이터 확인");
		assertThat(response.getRoomId()).isEqualTo(roomId);
		assertThat(response.getRequestId()).isNotNull();
		log.info("[Then] - ✓ 응답 데이터 확인됨");

		log.info("[Then] [검증2] operatingPolicyPort.save()가 1번 호출되었는지 확인");
		ArgumentCaptor<RoomOperatingPolicy> policyCaptor = ArgumentCaptor.forClass(RoomOperatingPolicy.class);
		verify(operatingPolicyPort, times(1)).save(policyCaptor.capture());
		RoomOperatingPolicy capturedPolicy = policyCaptor.getValue();

		log.info("[Then] - 저장된 정책의 주간 스케줄 확인");
		assertThat(capturedPolicy.getWeeklySchedule()).isNotNull();
		assertThat(capturedPolicy.getWeeklySchedule().getSlotTimes()).hasSize(5); // 3 + 2 = 5개 시간대
		log.info("[Then] - ✓ 모든 슬롯 시간이 정책에 포함됨");

		log.info("[Then] [검증3] 이벤트와 요청 저장 확인");
		verify(slotGenerationRequestPort, times(1)).save(any(SlotGenerationRequest.class));
		verify(eventPublisher, times(1)).publish(any(SlotGenerationRequestedEvent.class));
		log.info("[Then] - ✓ 요청 저장 및 이벤트 발행 확인됨");

		log.info("=== [여러 슬롯 시간 설정] 테스트 성공 ===");
	}

	@Test
	@DisplayName("완료된 슬롯 생성 요청의 상태를 조회한다")
	void getStatus_completed() {
		log.info("=== [완료된 슬롯 생성 상태 조회] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		String requestId = "completed-request-id";
		SlotGenerationRequest request = SlotGenerationRequest.create(
				requestId,
				roomId,
				LocalDate.now(),
				LocalDate.now().plusMonths(2)
		);
		request.markAsInProgress();
		request.markAsCompleted(120);
		log.info("[Given] - 완료된 슬롯 생성 요청 준비: requestId={}, totalSlots=120", requestId);

		when(slotGenerationRequestPort.findById(requestId)).thenReturn(Optional.of(request));

		// When
		log.info("[When] getStatus() 호출");
		log.info("[When] - 파라미터: requestId={}", requestId);
		SlotGenerationStatusResponse response = service.getStatus(requestId);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 완료 상태 확인");
		log.info("[Then] - 예상 status: {}, 실제 status: {}",
				GenerationStatus.COMPLETED, response.getStatus());
		assertThat(response.getStatus()).isEqualTo(GenerationStatus.COMPLETED);
		log.info("[Then] - ✓ 완료 상태 확인됨");

		log.info("[Then] [검증2] 생성된 슬롯 개수 확인");
		log.info("[Then] - 예상 totalSlots: 120, 실제 totalSlots: {}", response.getTotalSlots());
		assertThat(response.getTotalSlots()).isEqualTo(120);
		log.info("[Then] - ✓ 슬롯 개수 확인됨");

		log.info("=== [완료된 슬롯 생성 상태 조회] 테스트 성공 ===");
	}
}
