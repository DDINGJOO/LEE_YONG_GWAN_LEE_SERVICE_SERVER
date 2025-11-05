package com.teambind.springproject.room.infrastructure.persistence;

import com.teambind.springproject.config.TestKafkaConfig;
import com.teambind.springproject.config.TestRedisConfig;
import com.teambind.springproject.room.domain.port.SlotGenerationRequestPort;
import com.teambind.springproject.room.entity.SlotGenerationRequest;
import com.teambind.springproject.room.entity.enums.GenerationStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SlotGenerationRequestJpaAdapter 통합 테스트.
 *
 * Hexagonal Architecture의 Adapter 계층 테스트로,
 * Port 인터페이스 구현이 올바르게 동작하는지 검증한다.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Import({TestRedisConfig.class, TestKafkaConfig.class})
@Transactional
@DisplayName("SlotGenerationRequestJpaAdapter 통합 테스트")
class SlotGenerationRequestJpaAdapterIntegrationTest {

	@Autowired
	private SlotGenerationRequestPort requestPort;

	private String requestId;
	private Long roomId;
	private LocalDate startDate;
	private LocalDate endDate;

	@BeforeEach
	void setUp() {
		requestId = UUID.randomUUID().toString();
		roomId = 100L;
		startDate = LocalDate.of(2025, 1, 1);
		endDate = LocalDate.of(2025, 1, 31);

		log.info("=== 테스트 데이터 초기화 ===");
		log.info("- requestId: {}", requestId);
		log.info("- roomId: {}", roomId);
		log.info("- startDate: {}", startDate);
		log.info("- endDate: {}", endDate);
	}

	@Test
	@DisplayName("슬롯 생성 요청을 저장하고 조회한다")
	void save_and_findById() {
		log.info("=== [슬롯 생성 요청 저장 및 조회] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		SlotGenerationRequest request = SlotGenerationRequest.create(
				requestId,
				roomId,
				startDate,
				endDate
		);
		log.info("[Given] - 생성된 요청: requestId={}, roomId={}, status={}",
				requestId, roomId, request.getStatus());

		// When - Save
		log.info("[When] [Step 1] requestPort.save() 호출");
		log.info("[When] - 파라미터: request={}", request);
		SlotGenerationRequest savedRequest = requestPort.save(request);
		log.info("[When] - 저장 완료: requestId={}", savedRequest.getRequestId());

		// Then - Find
		log.info("[Then] [검증1] 저장된 요청 조회");
		log.info("[Then] - requestPort.findById() 호출");
		log.info("[Then] - 파라미터: requestId={}", requestId);
		Optional<SlotGenerationRequest> found = requestPort.findById(requestId);
		log.info("[Then] - 조회 결과: {}", found.isPresent() ? "존재함" : "없음");

		log.info("[Then] [검증2] 요청 존재 확인");
		assertThat(found).isPresent();
		log.info("[Then] - ✓ 요청이 정상적으로 조회됨");

		log.info("[Then] [검증3] 저장된 데이터 검증");
		SlotGenerationRequest foundRequest = found.get();
		log.info("[Then] - 예상 requestId: {}, 실제 requestId: {}", requestId, foundRequest.getRequestId());
		log.info("[Then] - 예상 roomId: {}, 실제 roomId: {}", roomId, foundRequest.getRoomId());
		log.info("[Then] - 예상 startDate: {}, 실제 startDate: {}", startDate, foundRequest.getStartDate());
		log.info("[Then] - 예상 endDate: {}, 실제 endDate: {}", endDate, foundRequest.getEndDate());
		log.info("[Then] - 예상 status: {}, 실제 status: {}",
				GenerationStatus.REQUESTED, foundRequest.getStatus());

		assertThat(foundRequest.getRequestId()).isEqualTo(requestId);
		assertThat(foundRequest.getRoomId()).isEqualTo(roomId);
		assertThat(foundRequest.getStartDate()).isEqualTo(startDate);
		assertThat(foundRequest.getEndDate()).isEqualTo(endDate);
		assertThat(foundRequest.getStatus()).isEqualTo(GenerationStatus.REQUESTED);
		assertThat(foundRequest.getRequestedAt()).isNotNull();
		log.info("[Then] - ✓ 모든 필드가 정확히 일치함");

		log.info("=== [슬롯 생성 요청 저장 및 조회] 테스트 성공 ===");
	}

	@Test
	@DisplayName("존재하지 않는 요청 ID로 조회 시 빈 Optional을 반환한다")
	void findById_notExists() {
		log.info("=== [존재하지 않는 요청 조회] 테스트 시작 ===");

		// Given
		log.info("[Given] 존재하지 않는 requestId 준비");
		String nonExistentRequestId = UUID.randomUUID().toString();
		log.info("[Given] - nonExistentRequestId: {}", nonExistentRequestId);

		// When
		log.info("[When] requestPort.findById() 호출");
		log.info("[When] - 파라미터: requestId={}", nonExistentRequestId);
		Optional<SlotGenerationRequest> found = requestPort.findById(nonExistentRequestId);
		log.info("[When] - 조회 결과: {}", found.isPresent() ? "존재함" : "없음");

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] Optional이 비어있는지 확인");
		log.info("[Then] - 예상: 비어있음");
		log.info("[Then] - 실제: {}", found.isPresent() ? "존재함" : "비어있음");
		assertThat(found).isEmpty();
		log.info("[Then] - ✓ 존재하지 않는 요청은 빈 Optional 반환");

		log.info("=== [존재하지 않는 요청 조회] 테스트 성공 ===");
	}

	@Test
	@DisplayName("요청 상태를 IN_PROGRESS로 업데이트한다")
	void updateStatus_toInProgress() {
		log.info("=== [요청 상태 IN_PROGRESS 업데이트] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		SlotGenerationRequest request = SlotGenerationRequest.create(
				requestId,
				roomId,
				startDate,
				endDate
		);
		SlotGenerationRequest savedRequest = requestPort.save(request);
		log.info("[Given] - 초기 상태: {}", savedRequest.getStatus());

		// When
		log.info("[When] 상태를 IN_PROGRESS로 변경");
		log.info("[When] - [Step 1] markAsInProgress() 호출");
		savedRequest.markAsInProgress();
		log.info("[When] - 변경 완료: status={}", savedRequest.getStatus());

		log.info("[When] - [Step 2] requestPort.save() 호출 (업데이트)");
		SlotGenerationRequest updatedRequest = requestPort.save(savedRequest);
		log.info("[When] - 업데이트 완료");

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 업데이트된 요청 조회");
		Optional<SlotGenerationRequest> found = requestPort.findById(requestId);
		assertThat(found).isPresent();
		log.info("[Then] - ✓ 요청 조회 성공");

		log.info("[Then] [검증2] 상태 변경 확인");
		SlotGenerationRequest foundRequest = found.get();
		log.info("[Then] - 기존 상태: {}", GenerationStatus.REQUESTED);
		log.info("[Then] - 변경 상태: {}", GenerationStatus.IN_PROGRESS);
		log.info("[Then] - 실제 상태: {}", foundRequest.getStatus());
		assertThat(foundRequest.getStatus()).isEqualTo(GenerationStatus.IN_PROGRESS);
		log.info("[Then] - ✓ 상태가 정상적으로 업데이트됨");

		log.info("[Then] [검증3] startedAt 필드 확인");
		log.info("[Then] - startedAt: {}", foundRequest.getStartedAt());
		assertThat(foundRequest.getStartedAt()).isNotNull();
		log.info("[Then] - ✓ startedAt이 설정됨");

		log.info("=== [요청 상태 IN_PROGRESS 업데이트] 테스트 성공 ===");
	}

	@Test
	@DisplayName("요청 상태를 COMPLETED로 업데이트한다")
	void updateStatus_toCompleted() {
		log.info("=== [요청 상태 COMPLETED 업데이트] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		SlotGenerationRequest request = SlotGenerationRequest.create(
				requestId,
				roomId,
				startDate,
				endDate
		);
		SlotGenerationRequest savedRequest = requestPort.save(request);
		savedRequest.markAsInProgress();
		requestPort.save(savedRequest);
		log.info("[Given] - 초기 상태: {}", savedRequest.getStatus());

		// When
		log.info("[When] 상태를 COMPLETED로 변경");
		int totalSlots = 124;
		log.info("[When] - [Step 1] markAsCompleted() 호출");
		log.info("[When]   - 파라미터: totalSlots={}", totalSlots);
		savedRequest.markAsCompleted(totalSlots);
		log.info("[When] - 변경 완료: status={}", savedRequest.getStatus());

		log.info("[When] - [Step 2] requestPort.save() 호출 (업데이트)");
		SlotGenerationRequest updatedRequest = requestPort.save(savedRequest);
		log.info("[When] - 업데이트 완료");

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 업데이트된 요청 조회");
		Optional<SlotGenerationRequest> found = requestPort.findById(requestId);
		assertThat(found).isPresent();
		log.info("[Then] - ✓ 요청 조회 성공");

		log.info("[Then] [검증2] 상태 변경 확인");
		SlotGenerationRequest foundRequest = found.get();
		log.info("[Then] - 기존 상태: {}", GenerationStatus.IN_PROGRESS);
		log.info("[Then] - 변경 상태: {}", GenerationStatus.COMPLETED);
		log.info("[Then] - 실제 상태: {}", foundRequest.getStatus());
		assertThat(foundRequest.getStatus()).isEqualTo(GenerationStatus.COMPLETED);
		log.info("[Then] - ✓ 상태가 정상적으로 업데이트됨");

		log.info("[Then] [검증3] totalSlots 필드 확인");
		log.info("[Then] - 예상 totalSlots: {}", totalSlots);
		log.info("[Then] - 실제 totalSlots: {}", foundRequest.getTotalSlots());
		assertThat(foundRequest.getTotalSlots()).isEqualTo(totalSlots);
		log.info("[Then] - ✓ totalSlots가 정확히 저장됨");

		log.info("[Then] [검증4] completedAt 필드 확인");
		log.info("[Then] - completedAt: {}", foundRequest.getCompletedAt());
		assertThat(foundRequest.getCompletedAt()).isNotNull();
		log.info("[Then] - ✓ completedAt이 설정됨");

		log.info("=== [요청 상태 COMPLETED 업데이트] 테스트 성공 ===");
	}

	@Test
	@DisplayName("요청 상태를 FAILED로 업데이트한다")
	void updateStatus_toFailed() {
		log.info("=== [요청 상태 FAILED 업데이트] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		SlotGenerationRequest request = SlotGenerationRequest.create(
				requestId,
				roomId,
				startDate,
				endDate
		);
		SlotGenerationRequest savedRequest = requestPort.save(request);
		savedRequest.markAsInProgress();
		requestPort.save(savedRequest);
		log.info("[Given] - 초기 상태: {}", savedRequest.getStatus());

		// When
		log.info("[When] 상태를 FAILED로 변경");
		String errorMessage = "Operating policy not found";
		log.info("[When] - [Step 1] markAsFailed() 호출");
		log.info("[When]   - 파라미터: errorMessage={}", errorMessage);
		savedRequest.markAsFailed(errorMessage);
		log.info("[When] - 변경 완료: status={}", savedRequest.getStatus());

		log.info("[When] - [Step 2] requestPort.save() 호출 (업데이트)");
		SlotGenerationRequest updatedRequest = requestPort.save(savedRequest);
		log.info("[When] - 업데이트 완료");

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 업데이트된 요청 조회");
		Optional<SlotGenerationRequest> found = requestPort.findById(requestId);
		assertThat(found).isPresent();
		log.info("[Then] - ✓ 요청 조회 성공");

		log.info("[Then] [검증2] 상태 변경 확인");
		SlotGenerationRequest foundRequest = found.get();
		log.info("[Then] - 기존 상태: {}", GenerationStatus.IN_PROGRESS);
		log.info("[Then] - 변경 상태: {}", GenerationStatus.FAILED);
		log.info("[Then] - 실제 상태: {}", foundRequest.getStatus());
		assertThat(foundRequest.getStatus()).isEqualTo(GenerationStatus.FAILED);
		log.info("[Then] - ✓ 상태가 정상적으로 업데이트됨");

		log.info("[Then] [검증3] errorMessage 필드 확인");
		log.info("[Then] - 예상 errorMessage: {}", errorMessage);
		log.info("[Then] - 실제 errorMessage: {}", foundRequest.getErrorMessage());
		assertThat(foundRequest.getErrorMessage()).isEqualTo(errorMessage);
		log.info("[Then] - ✓ errorMessage가 정확히 저장됨");

		log.info("[Then] [검증4] completedAt 필드 확인");
		log.info("[Then] - completedAt: {}", foundRequest.getCompletedAt());
		assertThat(foundRequest.getCompletedAt()).isNotNull();
		log.info("[Then] - ✓ completedAt이 설정됨");

		log.info("=== [요청 상태 FAILED 업데이트] 테스트 성공 ===");
	}

	@Test
	@DisplayName("여러 요청을 저장하고 각각 독립적으로 조회한다")
	void save_multipleRequests() {
		log.info("=== [여러 요청 저장 및 조회] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		String requestId1 = UUID.randomUUID().toString();
		String requestId2 = UUID.randomUUID().toString();
		String requestId3 = UUID.randomUUID().toString();

		SlotGenerationRequest request1 = SlotGenerationRequest.create(
				requestId1, 101L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)
		);
		SlotGenerationRequest request2 = SlotGenerationRequest.create(
				requestId2, 102L, LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28)
		);
		SlotGenerationRequest request3 = SlotGenerationRequest.create(
				requestId3, 103L, LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31)
		);

		// When
		log.info("[When] 3개의 요청 저장");
		requestPort.save(request1);
		requestPort.save(request2);
		requestPort.save(request3);
		log.info("[When] - Request1 (requestId={}) 저장", requestId1);
		log.info("[When] - Request2 (requestId={}) 저장", requestId2);
		log.info("[When] - Request3 (requestId={}) 저장", requestId3);

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 각 요청이 독립적으로 조회되는지 확인");

		log.info("[Then] - [확인1] Request1 조회");
		Optional<SlotGenerationRequest> found1 = requestPort.findById(requestId1);
		assertThat(found1).isPresent();
		assertThat(found1.get().getRoomId()).isEqualTo(101L);
		log.info("[Then]   - ✓ Request1 조회 성공: roomId={}", found1.get().getRoomId());

		log.info("[Then] - [확인2] Request2 조회");
		Optional<SlotGenerationRequest> found2 = requestPort.findById(requestId2);
		assertThat(found2).isPresent();
		assertThat(found2.get().getRoomId()).isEqualTo(102L);
		log.info("[Then]   - ✓ Request2 조회 성공: roomId={}", found2.get().getRoomId());

		log.info("[Then] - [확인3] Request3 조회");
		Optional<SlotGenerationRequest> found3 = requestPort.findById(requestId3);
		assertThat(found3).isPresent();
		assertThat(found3.get().getRoomId()).isEqualTo(103L);
		log.info("[Then]   - ✓ Request3 조회 성공: roomId={}", found3.get().getRoomId());

		log.info("[Then] - ✓ 모든 요청이 독립적으로 저장 및 조회됨");

		log.info("=== [여러 요청 저장 및 조회] 테스트 성공 ===");
	}
}
