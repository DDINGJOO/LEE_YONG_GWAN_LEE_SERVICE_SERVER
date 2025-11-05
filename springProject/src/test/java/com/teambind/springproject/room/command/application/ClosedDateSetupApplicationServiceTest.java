package com.teambind.springproject.room.command.application;

import com.teambind.springproject.common.exceptions.domain.RequestNotFoundException;
import com.teambind.springproject.message.publish.EventPublisher;
import com.teambind.springproject.room.command.dto.ClosedDateDto;
import com.teambind.springproject.room.command.dto.ClosedDateSetupRequest;
import com.teambind.springproject.room.domain.port.ClosedDateUpdateRequestPort;
import com.teambind.springproject.room.domain.port.OperatingPolicyPort;
import com.teambind.springproject.room.entity.ClosedDateUpdateRequest;
import com.teambind.springproject.room.entity.RoomOperatingPolicy;
import com.teambind.springproject.room.entity.enums.GenerationStatus;
import com.teambind.springproject.room.entity.enums.RecurrencePattern;
import com.teambind.springproject.room.entity.vo.WeeklySlotSchedule;
import com.teambind.springproject.room.entity.vo.WeeklySlotTime;
import com.teambind.springproject.room.event.event.ClosedDateUpdateRequestedEvent;
import com.teambind.springproject.room.query.dto.ClosedDateSetupResponse;
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
import static org.mockito.Mockito.*;

/**
 * ClosedDateSetupApplicationService 단위 테스트.
 *
 * ApplicationService 계층의 휴무일 설정 로직을 Port를 Mocking하여 독립적으로 테스트한다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("ClosedDateSetupApplicationService 단위 테스트")
class ClosedDateSetupApplicationServiceTest {

	@Mock
	private OperatingPolicyPort operatingPolicyPort;

	@Mock
	private ClosedDateUpdateRequestPort updateRequestPort;

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private ClosedDateSetupApplicationService service;

	private Long roomId;
	private RoomOperatingPolicy policy;
	private ClosedDateSetupRequest setupRequest;

	@BeforeEach
	void setUp() {
		roomId = 100L;

		// 월요일 09:00, 10:00 운영 정책
		List<WeeklySlotTime> slotTimes = List.of(
				WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
				WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(10, 0))
		);
		WeeklySlotSchedule schedule = WeeklySlotSchedule.of(slotTimes);
		policy = RoomOperatingPolicy.create(roomId, schedule, RecurrencePattern.EVERY_WEEK, List.of());

		// 휴무일 설정 요청: 2025-01-15 하루 종일 휴무
		ClosedDateDto closedDateDto = new ClosedDateDto(
				LocalDate.of(2025, 1, 15), null, null, null, null, null
		);
		setupRequest = new ClosedDateSetupRequest(roomId, List.of(closedDateDto));

		log.info("=== 테스트 데이터 초기화 ===");
		log.info("- roomId: {}", roomId);
		log.info("- closedDate: 2025-01-15 (하루 종일)");
	}

	@Test
	@DisplayName("휴무일을 설정하고 슬롯 업데이트를 요청한다")
	void setupClosedDates() {
		log.info("=== [휴무일 설정 및 슬롯 업데이트 요청] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		when(operatingPolicyPort.findByRoomId(roomId)).thenReturn(Optional.of(policy));
		log.info("[Given] - operatingPolicyPort.findByRoomId() -> 정책 반환");

		when(operatingPolicyPort.save(any(RoomOperatingPolicy.class))).thenReturn(policy);
		log.info("[Given] - operatingPolicyPort.save() -> 정책 저장 성공");

		ClosedDateUpdateRequest savedRequest = ClosedDateUpdateRequest.create(
				"test-request-id",
				roomId,
				1
		);
		when(updateRequestPort.save(any(ClosedDateUpdateRequest.class))).thenReturn(savedRequest);
		log.info("[Given] - updateRequestPort.save() -> 요청 저장 성공");

		// When
		log.info("[When] setupClosedDates() 호출");
		log.info("[When] - 파라미터: request={}", setupRequest);
		ClosedDateSetupResponse response = service.setupClosedDates(setupRequest);
		log.info("[When] - 호출 완료: requestId={}", response.getRequestId());

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 응답 데이터 확인");
		log.info("[Then] - 응답 roomId: {}", response.getRoomId());
		log.info("[Then] - 응답 closedDateCount: {}", response.getClosedDateCount());
		log.info("[Then] - 응답 status: {}", response.getStatus());
		assertThat(response.getRoomId()).isEqualTo(roomId);
		assertThat(response.getRequestId()).isNotNull();
		assertThat(response.getClosedDateCount()).isEqualTo(1);
		assertThat(response.getStatus()).isEqualTo(GenerationStatus.REQUESTED);
		log.info("[Then] - ✓ 응답 데이터 확인됨");

		log.info("[Then] [검증2] operatingPolicyPort.findByRoomId()가 1번 호출되었는지 확인");
		verify(operatingPolicyPort, times(1)).findByRoomId(roomId);
		log.info("[Then] - ✓ findByRoomId() 호출 확인됨");

		log.info("[Then] [검증3] operatingPolicyPort.save()가 1번 호출되었는지 확인");
		ArgumentCaptor<RoomOperatingPolicy> policyCaptor = ArgumentCaptor.forClass(RoomOperatingPolicy.class);
		verify(operatingPolicyPort, times(1)).save(policyCaptor.capture());
		RoomOperatingPolicy capturedPolicy = policyCaptor.getValue();
		log.info("[Then] - 저장된 정책 roomId: {}", capturedPolicy.getRoomId());
		log.info("[Then] - 저장된 정책의 휴무일 개수: {}", capturedPolicy.getClosedDates().size());
		assertThat(capturedPolicy.getRoomId()).isEqualTo(roomId);
		assertThat(capturedPolicy.getClosedDates()).hasSize(1);
		log.info("[Then] - ✓ 정책에 휴무일 추가 확인됨");

		log.info("[Then] [검증4] updateRequestPort.save()가 1번 호출되었는지 확인");
		ArgumentCaptor<ClosedDateUpdateRequest> requestCaptor = ArgumentCaptor.forClass(ClosedDateUpdateRequest.class);
		verify(updateRequestPort, times(1)).save(requestCaptor.capture());
		ClosedDateUpdateRequest capturedRequest = requestCaptor.getValue();
		log.info("[Then] - 저장된 요청 roomId: {}", capturedRequest.getRoomId());
		log.info("[Then] - 저장된 요청 closedDateCount: {}", capturedRequest.getClosedDateCount());
		assertThat(capturedRequest.getRoomId()).isEqualTo(roomId);
		assertThat(capturedRequest.getClosedDateCount()).isEqualTo(1);
		log.info("[Then] - ✓ 요청 저장 확인됨");

		log.info("[Then] [검증5] ClosedDateUpdateRequestedEvent가 발행되었는지 확인");
		ArgumentCaptor<ClosedDateUpdateRequestedEvent> eventCaptor = ArgumentCaptor.forClass(ClosedDateUpdateRequestedEvent.class);
		verify(eventPublisher, times(1)).publish(eventCaptor.capture());
		ClosedDateUpdateRequestedEvent publishedEvent = eventCaptor.getValue();
		log.info("[Then] - 이벤트 requestId: {}", publishedEvent.getRequestId());
		log.info("[Then] - 이벤트 roomId: {}", publishedEvent.getRoomId());
		assertThat(publishedEvent.getRoomId()).isEqualTo(roomId);
		assertThat(publishedEvent.getRequestId()).isNotNull();
		log.info("[Then] - ✓ 이벤트 발행 확인됨");

		log.info("=== [휴무일 설정 및 슬롯 업데이트 요청] 테스트 성공 ===");
	}

	@Test
	@DisplayName("정책이 없으면 예외가 발생한다")
	void setupClosedDates_policyNotFound() {
		log.info("=== [정책 없음 예외] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		when(operatingPolicyPort.findByRoomId(roomId)).thenReturn(Optional.empty());
		log.info("[Given] - operatingPolicyPort.findByRoomId() -> 빈 Optional 반환");

		// When & Then
		log.info("[When & Then] setupClosedDates() 호출 시 RequestNotFoundException 발생");
		log.info("[When & Then] - 파라미터: request={}", setupRequest);

		assertThatThrownBy(() -> service.setupClosedDates(setupRequest))
				.isInstanceOf(RequestNotFoundException.class)
				.hasMessageContaining(roomId.toString());

		log.info("[Then] - ✓ RequestNotFoundException 발생 확인됨");

		log.info("[Then] [검증] save()와 이벤트 발행이 호출되지 않았는지 확인");
		verify(operatingPolicyPort, never()).save(any());
		verify(updateRequestPort, never()).save(any());
		verify(eventPublisher, never()).publish(any());
		log.info("[Then] - ✓ save()와 publish() 미호출 확인됨");

		log.info("=== [정책 없음 예외] 테스트 성공 ===");
	}

	@Test
	@DisplayName("여러 휴무일을 한 번에 설정한다")
	void setupClosedDates_multipleDates() {
		log.info("=== [여러 휴무일 설정] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		ClosedDateDto date1 = new ClosedDateDto(LocalDate.of(2025, 1, 15), null, null, null, null, null);
		ClosedDateDto date2 = new ClosedDateDto(LocalDate.of(2025, 1, 22), null, null, null, null, null);
		ClosedDateDto date3 = new ClosedDateDto(LocalDate.of(2025, 1, 29), null, null, null, null, null);

		ClosedDateSetupRequest multiDateRequest = new ClosedDateSetupRequest(
				roomId,
				List.of(date1, date2, date3)
		);
		log.info("[Given] - 3개의 휴무일 요청 생성");
		log.info("[Given]   - 2025-01-15, 2025-01-22, 2025-01-29");

		when(operatingPolicyPort.findByRoomId(roomId)).thenReturn(Optional.of(policy));
		when(operatingPolicyPort.save(any(RoomOperatingPolicy.class))).thenReturn(policy);

		ClosedDateUpdateRequest savedRequest = ClosedDateUpdateRequest.create(
				"test-request-id",
				roomId,
				3
		);
		when(updateRequestPort.save(any(ClosedDateUpdateRequest.class))).thenReturn(savedRequest);

		// When
		log.info("[When] setupClosedDates() 호출");
		log.info("[When] - 파라미터: multiDateRequest (3개 휴무일)");
		ClosedDateSetupResponse response = service.setupClosedDates(multiDateRequest);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 응답의 휴무일 개수 확인");
		log.info("[Then] - 예상 개수: 3");
		log.info("[Then] - 실제 개수: {}", response.getClosedDateCount());
		assertThat(response.getClosedDateCount()).isEqualTo(3);
		log.info("[Then] - ✓ 휴무일 개수 확인됨");

		log.info("[Then] [검증2] 정책에 모든 휴무일이 추가되었는지 확인");
		ArgumentCaptor<RoomOperatingPolicy> policyCaptor = ArgumentCaptor.forClass(RoomOperatingPolicy.class);
		verify(operatingPolicyPort, times(1)).save(policyCaptor.capture());
		RoomOperatingPolicy capturedPolicy = policyCaptor.getValue();
		log.info("[Then] - 정책의 휴무일 개수: {}", capturedPolicy.getClosedDates().size());
		assertThat(capturedPolicy.getClosedDates()).hasSize(3);
		log.info("[Then] - ✓ 모든 휴무일 추가 확인됨");

		log.info("[Then] [검증3] 요청에 휴무일 개수가 정확히 기록되었는지 확인");
		ArgumentCaptor<ClosedDateUpdateRequest> requestCaptor = ArgumentCaptor.forClass(ClosedDateUpdateRequest.class);
		verify(updateRequestPort, times(1)).save(requestCaptor.capture());
		ClosedDateUpdateRequest capturedRequest = requestCaptor.getValue();
		assertThat(capturedRequest.getClosedDateCount()).isEqualTo(3);
		log.info("[Then] - ✓ 요청의 휴무일 개수 확인됨");

		log.info("=== [여러 휴무일 설정] 테스트 성공 ===");
	}

	@Test
	@DisplayName("날짜 범위 휴무일을 설정한다")
	void setupClosedDates_dateRange() {
		log.info("=== [날짜 범위 휴무일 설정] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		LocalDate startDate = LocalDate.of(2025, 1, 15);
		LocalDate endDate = LocalDate.of(2025, 1, 20);

		ClosedDateDto dateRangeDto = new ClosedDateDto(startDate, endDate, null, null, null, null);
		ClosedDateSetupRequest dateRangeRequest = new ClosedDateSetupRequest(
				roomId,
				List.of(dateRangeDto)
		);
		log.info("[Given] - 날짜 범위 휴무일 요청 생성: {} ~ {}", startDate, endDate);

		when(operatingPolicyPort.findByRoomId(roomId)).thenReturn(Optional.of(policy));
		when(operatingPolicyPort.save(any(RoomOperatingPolicy.class))).thenReturn(policy);

		ClosedDateUpdateRequest savedRequest = ClosedDateUpdateRequest.create(
				"test-request-id",
				roomId,
				1
		);
		when(updateRequestPort.save(any(ClosedDateUpdateRequest.class))).thenReturn(savedRequest);

		// When
		log.info("[When] setupClosedDates() 호출");
		log.info("[When] - 파라미터: dateRangeRequest (날짜 범위)");
		ClosedDateSetupResponse response = service.setupClosedDates(dateRangeRequest);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 응답 데이터 확인");
		assertThat(response.getRoomId()).isEqualTo(roomId);
		assertThat(response.getClosedDateCount()).isEqualTo(1);
		log.info("[Then] - ✓ 응답 데이터 확인됨");

		log.info("[Then] [검증2] 정책에 휴무일 범위가 추가되었는지 확인");
		ArgumentCaptor<RoomOperatingPolicy> policyCaptor = ArgumentCaptor.forClass(RoomOperatingPolicy.class);
		verify(operatingPolicyPort, times(1)).save(policyCaptor.capture());
		RoomOperatingPolicy capturedPolicy = policyCaptor.getValue();
		assertThat(capturedPolicy.getClosedDates()).hasSize(1);
		log.info("[Then] - ✓ 휴무일 범위 추가 확인됨");

		log.info("=== [날짜 범위 휴무일 설정] 테스트 성공 ===");
	}

	@Test
	@DisplayName("시간 범위 휴무일을 설정한다")
	void setupClosedDates_timeRange() {
		log.info("=== [시간 범위 휴무일 설정] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		LocalDate targetDate = LocalDate.of(2025, 1, 15);
		LocalTime startTime = LocalTime.of(9, 0);
		LocalTime endTime = LocalTime.of(12, 0);

		ClosedDateDto timeRangeDto = new ClosedDateDto(targetDate, null, null, null, startTime, endTime);
		ClosedDateSetupRequest timeRangeRequest = new ClosedDateSetupRequest(
				roomId,
				List.of(timeRangeDto)
		);
		log.info("[Given] - 시간 범위 휴무일 요청 생성: {} {} ~ {}", targetDate, startTime, endTime);

		when(operatingPolicyPort.findByRoomId(roomId)).thenReturn(Optional.of(policy));
		when(operatingPolicyPort.save(any(RoomOperatingPolicy.class))).thenReturn(policy);

		ClosedDateUpdateRequest savedRequest = ClosedDateUpdateRequest.create(
				"test-request-id",
				roomId,
				1
		);
		when(updateRequestPort.save(any(ClosedDateUpdateRequest.class))).thenReturn(savedRequest);

		// When
		log.info("[When] setupClosedDates() 호출");
		log.info("[When] - 파라미터: timeRangeRequest (시간 범위)");
		ClosedDateSetupResponse response = service.setupClosedDates(timeRangeRequest);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 응답 데이터 확인");
		assertThat(response.getRoomId()).isEqualTo(roomId);
		assertThat(response.getClosedDateCount()).isEqualTo(1);
		log.info("[Then] - ✓ 응답 데이터 확인됨");

		log.info("[Then] [검증2] 정책에 시간 범위 휴무일이 추가되었는지 확인");
		ArgumentCaptor<RoomOperatingPolicy> policyCaptor = ArgumentCaptor.forClass(RoomOperatingPolicy.class);
		verify(operatingPolicyPort, times(1)).save(policyCaptor.capture());
		RoomOperatingPolicy capturedPolicy = policyCaptor.getValue();
		assertThat(capturedPolicy.getClosedDates()).hasSize(1);
		log.info("[Then] - ✓ 시간 범위 휴무일 추가 확인됨");

		log.info("=== [시간 범위 휴무일 설정] 테스트 성공 ===");
	}

	@Test
	@DisplayName("패턴 기반 휴무일을 설정한다")
	void setupClosedDates_patternBased() {
		log.info("=== [패턴 기반 휴무일 설정] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		// 매주 월요일 종일 휴무
		ClosedDateDto patternDto = new ClosedDateDto(
				null, null, DayOfWeek.MONDAY, RecurrencePattern.EVERY_WEEK, null, null
		);
		ClosedDateSetupRequest patternRequest = new ClosedDateSetupRequest(
				roomId,
				List.of(patternDto)
		);
		log.info("[Given] - 패턴 기반 휴무일 요청 생성: 매주 월요일 종일");

		when(operatingPolicyPort.findByRoomId(roomId)).thenReturn(Optional.of(policy));
		when(operatingPolicyPort.save(any(RoomOperatingPolicy.class))).thenReturn(policy);

		ClosedDateUpdateRequest savedRequest = ClosedDateUpdateRequest.create(
				"test-request-id",
				roomId,
				1
		);
		when(updateRequestPort.save(any(ClosedDateUpdateRequest.class))).thenReturn(savedRequest);

		// When
		log.info("[When] setupClosedDates() 호출");
		log.info("[When] - 파라미터: patternRequest (패턴 기반)");
		ClosedDateSetupResponse response = service.setupClosedDates(patternRequest);
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 응답 데이터 확인");
		assertThat(response.getRoomId()).isEqualTo(roomId);
		assertThat(response.getClosedDateCount()).isEqualTo(1);
		log.info("[Then] - ✓ 응답 데이터 확인됨");

		log.info("[Then] [검증2] 정책에 패턴 기반 휴무일이 추가되었는지 확인");
		ArgumentCaptor<RoomOperatingPolicy> policyCaptor = ArgumentCaptor.forClass(RoomOperatingPolicy.class);
		verify(operatingPolicyPort, times(1)).save(policyCaptor.capture());
		RoomOperatingPolicy capturedPolicy = policyCaptor.getValue();
		assertThat(capturedPolicy.getClosedDates()).hasSize(1);
		log.info("[Then] - ✓ 패턴 기반 휴무일 추가 확인됨");

		log.info("[Then] [검증3] 이벤트와 요청 저장 확인");
		verify(updateRequestPort, times(1)).save(any(ClosedDateUpdateRequest.class));
		verify(eventPublisher, times(1)).publish(any(ClosedDateUpdateRequestedEvent.class));
		log.info("[Then] - ✓ 요청 저장 및 이벤트 발행 확인됨");

		log.info("=== [패턴 기반 휴무일 설정] 테스트 성공 ===");
	}
}
