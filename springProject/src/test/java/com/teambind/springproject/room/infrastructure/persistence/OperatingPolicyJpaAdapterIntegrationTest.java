package com.teambind.springproject.room.infrastructure.persistence;

import com.teambind.springproject.config.TestKafkaConfig;
import com.teambind.springproject.config.TestRedisConfig;
import com.teambind.springproject.room.domain.port.OperatingPolicyPort;
import com.teambind.springproject.room.entity.RoomOperatingPolicy;
import com.teambind.springproject.room.entity.enums.RecurrencePattern;
import com.teambind.springproject.room.entity.vo.WeeklySlotSchedule;
import com.teambind.springproject.room.entity.vo.WeeklySlotTime;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OperatingPolicyJpaAdapter 통합 테스트.
 *
 * Hexagonal Architecture의 Adapter 계층 테스트로,
 * Port 인터페이스 구현이 올바르게 동작하는지 검증한다.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Import({TestRedisConfig.class, TestKafkaConfig.class})
@Transactional
@DisplayName("OperatingPolicyJpaAdapter 통합 테스트")
class OperatingPolicyJpaAdapterIntegrationTest {

	@Autowired
	private OperatingPolicyPort operatingPolicyPort;

	private Long roomId;
	private WeeklySlotSchedule schedule;

	@BeforeEach
	void setUp() {
		roomId = 200L;

		List<WeeklySlotTime> slotTimes = List.of(
				WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
				WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(10, 0))
		);
		schedule = WeeklySlotSchedule.of(slotTimes);

		log.info("=== 테스트 데이터 초기화 ===");
		log.info("- roomId: {}", roomId);
		log.info("- schedule: 월요일 9시, 10시");
	}

	@Test
	@DisplayName("운영 정책을 저장하고 조회한다")
	void save_and_findByRoomId() {
		log.info("=== [운영 정책 저장 및 조회] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		RoomOperatingPolicy policy = RoomOperatingPolicy.create(
				roomId,
				schedule,
				RecurrencePattern.EVERY_WEEK,
				List.of()
		);
		log.info("[Given] - 생성된 정책: roomId={}, recurrence={}",
				roomId, policy.getRecurrence());

		// When - Save
		log.info("[When] [Step 1] operatingPolicyPort.save() 호출");
		log.info("[When] - 파라미터: policy={}", policy);
		RoomOperatingPolicy savedPolicy = operatingPolicyPort.save(policy);
		log.info("[When] - 저장 완료: policyId={}", savedPolicy.getPolicyId());

		// Then - Find
		log.info("[Then] [검증1] 저장된 정책 조회");
		log.info("[Then] - operatingPolicyPort.findByRoomId() 호출");
		log.info("[Then] - 파라미터: roomId={}", roomId);
		Optional<RoomOperatingPolicy> found = operatingPolicyPort.findByRoomId(roomId);
		log.info("[Then] - 조회 결과: {}", found.isPresent() ? "존재함" : "없음");

		log.info("[Then] [검증2] 정책 존재 확인");
		assertThat(found).isPresent();
		log.info("[Then] - ✓ 정책이 정상적으로 조회됨");

		log.info("[Then] [검증3] 저장된 데이터 검증");
		RoomOperatingPolicy foundPolicy = found.get();
		log.info("[Then] - 예상 roomId: {}, 실제 roomId: {}", roomId, foundPolicy.getRoomId());
		log.info("[Then] - 예상 recurrence: {}, 실제 recurrence: {}",
				RecurrencePattern.EVERY_WEEK, foundPolicy.getRecurrence());

		assertThat(foundPolicy.getRoomId()).isEqualTo(roomId);
		assertThat(foundPolicy.getRecurrence()).isEqualTo(RecurrencePattern.EVERY_WEEK);
		assertThat(foundPolicy.getWeeklySchedule()).isNotNull();
		log.info("[Then] - ✓ 모든 필드가 정확히 일치함");

		log.info("=== [운영 정책 저장 및 조회] 테스트 성공 ===");
	}

	@Test
	@DisplayName("roomId로 정책 존재 여부를 확인한다")
	void existsByRoomId() {
		log.info("=== [정책 존재 여부 확인] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		RoomOperatingPolicy policy = RoomOperatingPolicy.create(
				roomId,
				schedule,
				RecurrencePattern.EVERY_WEEK,
				List.of()
		);
		operatingPolicyPort.save(policy);
		log.info("[Given] - roomId={} 정책 저장 완료", roomId);

		// When
		log.info("[When] existsByRoomId() 호출");
		log.info("[When] - [확인1] 저장된 roomId로 확인");
		log.info("[When]   - 파라미터: roomId={}", roomId);
		boolean exists = operatingPolicyPort.existsByRoomId(roomId);
		log.info("[When]   - 결과: {}", exists);

		log.info("[When] - [확인2] 존재하지 않는 roomId로 확인");
		Long nonExistentRoomId = 999L;
		log.info("[When]   - 파라미터: roomId={}", nonExistentRoomId);
		boolean notExists = operatingPolicyPort.existsByRoomId(nonExistentRoomId);
		log.info("[When]   - 결과: {}", notExists);

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 저장된 roomId는 존재해야 함");
		log.info("[Then] - 예상: true");
		log.info("[Then] - 실제: {}", exists);
		assertThat(exists).isTrue();
		log.info("[Then] - ✓ 정책 존재 확인됨");

		log.info("[Then] [검증2] 존재하지 않는 roomId는 없어야 함");
		log.info("[Then] - 예상: false");
		log.info("[Then] - 실제: {}", notExists);
		assertThat(notExists).isFalse();
		log.info("[Then] - ✓ 정책 미존재 확인됨");

		log.info("=== [정책 존재 여부 확인] 테스트 성공 ===");
	}

	@Test
	@DisplayName("모든 운영 정책을 조회한다")
	void findAll() {
		log.info("=== [모든 운영 정책 조회] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		Long room1Id = 201L;
		Long room2Id = 202L;
		Long room3Id = 203L;

		// 각 정책에 대해 별도의 schedule 인스턴스 생성 (JPA 공유 참조 문제 방지)
		List<WeeklySlotTime> slotTimes1 = List.of(
				WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
				WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(10, 0))
		);
		WeeklySlotSchedule schedule1 = WeeklySlotSchedule.of(slotTimes1);

		List<WeeklySlotTime> slotTimes2 = List.of(
				WeeklySlotTime.of(DayOfWeek.TUESDAY, LocalTime.of(9, 0)),
				WeeklySlotTime.of(DayOfWeek.TUESDAY, LocalTime.of(10, 0))
		);
		WeeklySlotSchedule schedule2 = WeeklySlotSchedule.of(slotTimes2);

		List<WeeklySlotTime> slotTimes3 = List.of(
				WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0)),
				WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0))
		);
		WeeklySlotSchedule schedule3 = WeeklySlotSchedule.of(slotTimes3);

		RoomOperatingPolicy policy1 = RoomOperatingPolicy.create(
				room1Id, schedule1, RecurrencePattern.EVERY_WEEK, List.of()
		);
		RoomOperatingPolicy policy2 = RoomOperatingPolicy.create(
				room2Id, schedule2, RecurrencePattern.ODD_WEEK, List.of()
		);
		RoomOperatingPolicy policy3 = RoomOperatingPolicy.create(
				room3Id, schedule3, RecurrencePattern.EVEN_WEEK, List.of()
		);

		operatingPolicyPort.save(policy1);
		operatingPolicyPort.save(policy2);
		operatingPolicyPort.save(policy3);

		log.info("[Given] - Room1 (roomId={}) 정책 저장: EVERY_WEEK", room1Id);
		log.info("[Given] - Room2 (roomId={}) 정책 저장: ODD_WEEKS", room2Id);
		log.info("[Given] - Room3 (roomId={}) 정책 저장: EVEN_WEEKS", room3Id);

		// When
		log.info("[When] operatingPolicyPort.findAll() 호출");
		List<RoomOperatingPolicy> allPolicies = operatingPolicyPort.findAll();
		log.info("[When] - 조회 완료: {} 개의 정책", allPolicies.size());

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 조회된 정책 개수");
		log.info("[Then] - 예상: 3개 이상 (방금 저장한 정책 포함)");
		log.info("[Then] - 실제: {}개", allPolicies.size());
		assertThat(allPolicies).hasSizeGreaterThanOrEqualTo(3);
		log.info("[Then] - ✓ 모든 정책이 조회됨");

		log.info("[Then] [검증2] 저장한 정책들이 포함되어 있는지 확인");
		List<Long> roomIds = allPolicies.stream()
				.map(RoomOperatingPolicy::getRoomId)
				.toList();
		log.info("[Then] - 조회된 roomId 목록: {}", roomIds);
		assertThat(roomIds).contains(room1Id, room2Id, room3Id);
		log.info("[Then] - ✓ 저장한 정책들이 모두 포함됨");

		log.info("=== [모든 운영 정책 조회] 테스트 성공 ===");
	}

	@Test
	@DisplayName("roomId로 정책을 삭제한다")
	void deleteByRoomId() {
		log.info("=== [정책 삭제] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		RoomOperatingPolicy policy = RoomOperatingPolicy.create(
				roomId,
				schedule,
				RecurrencePattern.EVERY_WEEK,
				List.of()
		);
		operatingPolicyPort.save(policy);
		log.info("[Given] - roomId={} 정책 저장 완료", roomId);

		log.info("[Given] - 삭제 전 정책 존재 확인");
		boolean existsBefore = operatingPolicyPort.existsByRoomId(roomId);
		log.info("[Given] - 존재 여부: {}", existsBefore);

		// When
		log.info("[When] operatingPolicyPort.deleteByRoomId() 호출");
		log.info("[When] - 파라미터: roomId={}", roomId);
		operatingPolicyPort.deleteByRoomId(roomId);
		log.info("[When] - 삭제 완료");

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 삭제 후 정책 존재 여부 확인");
		boolean existsAfter = operatingPolicyPort.existsByRoomId(roomId);
		log.info("[Then] - 삭제 전: {}", existsBefore);
		log.info("[Then] - 삭제 후: {}", existsAfter);
		log.info("[Then] - 예상: false (삭제됨)");
		log.info("[Then] - 실제: {}", existsAfter);
		assertThat(existsAfter).isFalse();
		log.info("[Then] - ✓ 정책이 정상적으로 삭제됨");

		log.info("[Then] [검증2] 조회 시 결과 없음 확인");
		Optional<RoomOperatingPolicy> found = operatingPolicyPort.findByRoomId(roomId);
		log.info("[Then] - 예상: 비어있음");
		log.info("[Then] - 실제: {}", found.isPresent() ? "존재함" : "비어있음");
		assertThat(found).isEmpty();
		log.info("[Then] - ✓ 삭제된 정책은 조회되지 않음");

		log.info("=== [정책 삭제] 테스트 성공 ===");
	}

	@Test
	@DisplayName("정책을 업데이트한다")
	void update() {
		log.info("=== [정책 업데이트] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		RoomOperatingPolicy policy = RoomOperatingPolicy.create(
				roomId,
				schedule,
				RecurrencePattern.EVERY_WEEK,
				List.of()
		);
		RoomOperatingPolicy savedPolicy = operatingPolicyPort.save(policy);
		log.info("[Given] - 초기 정책 저장: recurrence={}", savedPolicy.getRecurrence());

		// When
		log.info("[When] 정책 업데이트");
		log.info("[When] - [Step 1] recurrence를 ODD_WEEK으로 변경");
		savedPolicy.updateRecurrence(RecurrencePattern.ODD_WEEK);
		log.info("[When] - 변경 완료: recurrence={}", savedPolicy.getRecurrence());

		log.info("[When] - [Step 2] operatingPolicyPort.save() 호출 (업데이트)");
		RoomOperatingPolicy updatedPolicy = operatingPolicyPort.save(savedPolicy);
		log.info("[When] - 업데이트 완료");

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 업데이트된 정책 조회");
		Optional<RoomOperatingPolicy> found = operatingPolicyPort.findByRoomId(roomId);
		assertThat(found).isPresent();
		log.info("[Then] - ✓ 정책 조회 성공");

		log.info("[Then] [검증2] recurrence 변경 확인");
		RoomOperatingPolicy foundPolicy = found.get();
		log.info("[Then] - 기존 값: {}", RecurrencePattern.EVERY_WEEK);
		log.info("[Then] - 변경 값: {}", RecurrencePattern.ODD_WEEK);
		log.info("[Then] - 실제 값: {}", foundPolicy.getRecurrence());
		assertThat(foundPolicy.getRecurrence()).isEqualTo(RecurrencePattern.ODD_WEEK);
		log.info("[Then] - ✓ recurrence가 정상적으로 업데이트됨");

		log.info("[Then] [검증3] updatedAt 변경 확인");
		log.info("[Then] - updatedAt: {}", foundPolicy.getUpdatedAt());
		assertThat(foundPolicy.getUpdatedAt()).isNotNull();
		log.info("[Then] - ✓ updatedAt이 갱신됨");

		log.info("=== [정책 업데이트] 테스트 성공 ===");
	}
}
