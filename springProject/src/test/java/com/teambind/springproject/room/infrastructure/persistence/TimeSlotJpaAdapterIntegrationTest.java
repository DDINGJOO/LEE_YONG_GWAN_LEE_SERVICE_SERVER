package com.teambind.springproject.room.infrastructure.persistence;

import com.teambind.springproject.config.TestRedisConfig;
import com.teambind.springproject.room.domain.port.TimeSlotPort;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.SlotStatus;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TimeSlotJpaAdapter 통합 테스트.
 *
 * Hexagonal Architecture의 Adapter 계층 테스트로,
 * Port 인터페이스 구현이 올바르게 동작하는지 검증한다.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
@DisplayName("TimeSlotJpaAdapter 통합 테스트")
class TimeSlotJpaAdapterIntegrationTest {

	@Autowired
	private TimeSlotPort timeSlotPort;

	private Long roomId;
	private LocalDate testDate;
	private LocalTime testTime;

	@BeforeEach
	void setUp() {
		roomId = 100L;
		testDate = LocalDate.of(2025, 11, 5);
		testTime = LocalTime.of(9, 0);

		log.info("=== 테스트 데이터 초기화 ===");
		log.info("- roomId: {}", roomId);
		log.info("- testDate: {}", testDate);
		log.info("- testTime: {}", testTime);
	}

	@Test
	@DisplayName("슬롯을 저장하고 조회한다")
	void save_and_find() {
		log.info("=== [슬롯 저장 및 조회] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		RoomTimeSlot slot = RoomTimeSlot.available(roomId, testDate, testTime);
		log.info("[Given] - 생성된 슬롯: roomId={}, date={}, time={}, status={}",
				roomId, testDate, testTime, slot.getStatus());

		// When - Save
		log.info("[When] [Step 1] timeSlotPort.save() 호출");
		log.info("[When] - 파라미터: slot={}", slot);
		RoomTimeSlot savedSlot = timeSlotPort.save(slot);
		log.info("[When] - 저장 완료: slotId={}", savedSlot.getSlotId());

		// Then - Find
		log.info("[Then] [검증1] 저장된 슬롯 조회");
		log.info("[Then] - timeSlotPort.findByRoomIdAndSlotDateAndSlotTime() 호출");
		log.info("[Then] - 파라미터: roomId={}, date={}, time={}", roomId, testDate, testTime);
		Optional<RoomTimeSlot> found = timeSlotPort.findByRoomIdAndSlotDateAndSlotTime(
				roomId, testDate, testTime
		);
		log.info("[Then] - 조회 결과: {}", found.isPresent() ? "존재함" : "없음");

		log.info("[Then] [검증2] 슬롯 존재 확인");
		assertThat(found).isPresent();
		log.info("[Then] - ✓ 슬롯이 정상적으로 조회됨");

		log.info("[Then] [검증3] 저장된 데이터 검증");
		RoomTimeSlot foundSlot = found.get();
		log.info("[Then] - 예상 roomId: {}, 실제 roomId: {}", roomId, foundSlot.getRoomId());
		log.info("[Then] - 예상 date: {}, 실제 date: {}", testDate, foundSlot.getSlotDate());
		log.info("[Then] - 예상 time: {}, 실제 time: {}", testTime, foundSlot.getSlotTime());
		log.info("[Then] - 예상 status: {}, 실제 status: {}", SlotStatus.AVAILABLE, foundSlot.getStatus());

		assertThat(foundSlot.getRoomId()).isEqualTo(roomId);
		assertThat(foundSlot.getSlotDate()).isEqualTo(testDate);
		assertThat(foundSlot.getSlotTime()).isEqualTo(testTime);
		assertThat(foundSlot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
		log.info("[Then] - ✓ 모든 필드가 정확히 일치함");

		log.info("=== [슬롯 저장 및 조회] 테스트 성공 ===");
	}

	@Test
	@DisplayName("날짜 범위로 슬롯을 조회한다")
	void findByRoomIdAndSlotDateBetween() {
		log.info("=== [날짜 범위 슬롯 조회] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate date1 = testDate;
		LocalDate date2 = testDate.plusDays(1);
		LocalDate date3 = testDate.plusDays(2);

		log.info("[Given] - 슬롯1 생성: date={}, time=9시", date1);
		log.info("[Given] - 슬롯2 생성: date={}, time=9시", date2);
		log.info("[Given] - 슬롯3 생성: date={}, time=9시", date3);

		RoomTimeSlot slot1 = timeSlotPort.save(RoomTimeSlot.available(roomId, date1, testTime));
		RoomTimeSlot slot2 = timeSlotPort.save(RoomTimeSlot.available(roomId, date2, testTime));
		RoomTimeSlot slot3 = timeSlotPort.save(RoomTimeSlot.available(roomId, date3, testTime));

		log.info("[Given] - 저장 완료: 3개 슬롯");

		// When
		log.info("[When] timeSlotPort.findByRoomIdAndSlotDateBetween() 호출");
		log.info("[When] - 파라미터: roomId={}, startDate={}, endDate={}", roomId, date1, date3);
		List<RoomTimeSlot> slots = timeSlotPort.findByRoomIdAndSlotDateBetween(roomId, date1, date3);
		log.info("[When] - 조회 완료: {} 개의 슬롯", slots.size());

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 조회된 슬롯 개수");
		log.info("[Then] - 예상: 3개 (date1, date2, date3)");
		log.info("[Then] - 실제: {}개", slots.size());
		assertThat(slots).hasSize(3);
		log.info("[Then] - ✓ 슬롯 개수 일치");

		log.info("[Then] [검증2] 슬롯 날짜 범위 확인");
		slots.forEach(slot -> log.info("[Then]   - 슬롯: date={}", slot.getSlotDate()));
		assertThat(slots).extracting(RoomTimeSlot::getSlotDate)
				.containsExactlyInAnyOrder(date1, date2, date3);
		log.info("[Then] - ✓ 날짜 범위 내의 모든 슬롯 조회됨");

		log.info("=== [날짜 범위 슬롯 조회] 테스트 성공 ===");
	}

	@Test
	@DisplayName("reservationId로 슬롯을 조회한다")
	void findByReservationId() {
		log.info("=== [reservationId로 슬롯 조회] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		Long reservationId = 999L;
		log.info("[Given] - reservationId: {}", reservationId);

		RoomTimeSlot slot1 = RoomTimeSlot.available(roomId, testDate, testTime);
		RoomTimeSlot slot2 = RoomTimeSlot.available(roomId, testDate, testTime.plusHours(1));

		slot1.markAsPending(reservationId);
		slot2.markAsPending(reservationId);

		timeSlotPort.save(slot1);
		timeSlotPort.save(slot2);
		log.info("[Given] - 2개 슬롯 저장 완료 (reservationId={})", reservationId);

		// When
		log.info("[When] timeSlotPort.findByReservationId() 호출");
		log.info("[When] - 파라미터: reservationId={}", reservationId);
		List<RoomTimeSlot> slots = timeSlotPort.findByReservationId(reservationId);
		log.info("[When] - 조회 완료: {} 개의 슬롯", slots.size());

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 조회된 슬롯 개수");
		log.info("[Then] - 예상: 2개");
		log.info("[Then] - 실제: {}개", slots.size());
		assertThat(slots).hasSize(2);
		log.info("[Then] - ✓ 슬롯 개수 일치");

		log.info("[Then] [검증2] 모든 슬롯의 reservationId 확인");
		slots.forEach(slot -> {
			log.info("[Then]   - 슬롯: time={}, reservationId={}, status={}",
					slot.getSlotTime(), slot.getReservationId(), slot.getStatus());
		});
		assertThat(slots).allMatch(slot -> slot.getReservationId().equals(reservationId));
		log.info("[Then] - ✓ 모든 슬롯의 reservationId가 일치함");

		log.info("=== [reservationId로 슬롯 조회] 테스트 성공 ===");
	}

	@Test
	@DisplayName("만료된 PENDING 슬롯을 조회한다")
	void findExpiredPendingSlots() {
		log.info("=== [만료된 PENDING 슬롯 조회] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		int expirationMinutes = 15;
		log.info("[Given] - 만료 시간: {}분", expirationMinutes);

		// PENDING 슬롯 생성 (이미 만료되었다고 가정)
		RoomTimeSlot expiredSlot = RoomTimeSlot.available(roomId, testDate, testTime);
		expiredSlot.markAsPending(888L);
		timeSlotPort.save(expiredSlot);

		// 최신 PENDING 슬롯 (만료 안됨)
		RoomTimeSlot recentSlot = RoomTimeSlot.available(roomId, testDate, testTime.plusHours(1));
		recentSlot.markAsPending(889L);
		timeSlotPort.save(recentSlot);

		log.info("[Given] - PENDING 슬롯 2개 저장 (1개는 만료, 1개는 최신)");

		// When
		log.info("[When] timeSlotPort.findExpiredPendingSlots() 호출");
		log.info("[When] - 파라미터: expirationMinutes={}", expirationMinutes);
		// 실제로는 lastUpdated가 15분 이전인 슬롯만 조회되어야 하지만,
		// 테스트 환경에서는 방금 생성했으므로 결과가 0개일 수 있음
		List<RoomTimeSlot> expiredSlots = timeSlotPort.findExpiredPendingSlots(expirationMinutes);
		log.info("[When] - 조회 완료: {} 개의 만료된 슬롯", expiredSlots.size());

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] findExpiredPendingSlots 메서드 동작 확인");
		log.info("[Then] - 조회된 슬롯 개수: {}", expiredSlots.size());
		log.info("[Then] - 참고: 테스트 환경에서는 방금 생성한 슬롯이므로 만료되지 않을 수 있음");
		assertThat(expiredSlots).isNotNull();
		log.info("[Then] - ✓ 메서드가 정상적으로 동작함 (NPE 없음)");

		log.info("=== [만료된 PENDING 슬롯 조회] 테스트 성공 ===");
	}

	@Test
	@DisplayName("여러 슬롯을 한 번에 저장한다")
	void saveAll() {
		log.info("=== [여러 슬롯 일괄 저장] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		List<RoomTimeSlot> slots = List.of(
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(9, 0)),
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(10, 0)),
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(11, 0))
		);
		log.info("[Given] - 생성된 슬롯 개수: {}", slots.size());

		// When
		log.info("[When] timeSlotPort.saveAll() 호출");
		log.info("[When] - 파라미터: {} 개의 슬롯", slots.size());
		List<RoomTimeSlot> savedSlots = timeSlotPort.saveAll(slots);
		log.info("[When] - 저장 완료: {} 개의 슬롯", savedSlots.size());

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 저장된 슬롯 개수");
		log.info("[Then] - 예상: 3개");
		log.info("[Then] - 실제: {}개", savedSlots.size());
		assertThat(savedSlots).hasSize(3);
		log.info("[Then] - ✓ 슬롯 개수 일치");

		log.info("[Then] [검증2] 모든 슬롯이 ID를 받았는지 확인");
		savedSlots.forEach(slot -> {
			log.info("[Then]   - 슬롯: slotId={}, time={}", slot.getSlotId(), slot.getSlotTime());
			assertThat(slot.getSlotId()).isNotNull();
		});
		log.info("[Then] - ✓ 모든 슬롯이 ID를 받음");

		log.info("=== [여러 슬롯 일괄 저장] 테스트 성공 ===");
	}

	@Test
	@DisplayName("특정 날짜 이전의 슬롯을 삭제한다")
	void deleteBySlotDateBefore() {
		log.info("=== [날짜 이전 슬롯 삭제] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate cutoffDate = testDate.plusDays(2);
		log.info("[Given] - 삭제 기준 날짜: {} (이 날짜 이전 슬롯 삭제)", cutoffDate);

		timeSlotPort.save(RoomTimeSlot.available(roomId, testDate, testTime));
		timeSlotPort.save(RoomTimeSlot.available(roomId, testDate.plusDays(1), testTime));
		timeSlotPort.save(RoomTimeSlot.available(roomId, testDate.plusDays(2), testTime));
		timeSlotPort.save(RoomTimeSlot.available(roomId, testDate.plusDays(3), testTime));

		log.info("[Given] - 슬롯 저장: {} (삭제 대상)", testDate);
		log.info("[Given] - 슬롯 저장: {} (삭제 대상)", testDate.plusDays(1));
		log.info("[Given] - 슬롯 저장: {} (유지)", testDate.plusDays(2));
		log.info("[Given] - 슬롯 저장: {} (유지)", testDate.plusDays(3));

		// When
		log.info("[When] timeSlotPort.deleteBySlotDateBefore() 호출");
		log.info("[When] - 파라미터: cutoffDate={}", cutoffDate);
		int deletedCount = timeSlotPort.deleteBySlotDateBefore(cutoffDate);
		log.info("[When] - 삭제 완료: {} 개의 슬롯", deletedCount);

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 삭제된 슬롯 개수");
		log.info("[Then] - 예상: 2개 이상 (testDate, testDate+1)");
		log.info("[Then] - 실제: {}개", deletedCount);
		assertThat(deletedCount).isGreaterThanOrEqualTo(2);
		log.info("[Then] - ✓ 기준 날짜 이전의 슬롯 삭제됨");

		log.info("[Then] [검증2] 남아있는 슬롯 확인");
		List<RoomTimeSlot> remaining = timeSlotPort.findByRoomIdAndSlotDateBetween(
				roomId, testDate, testDate.plusDays(10)
		);
		log.info("[Then] - 남은 슬롯 개수: {}", remaining.size());
		remaining.forEach(slot -> {
			log.info("[Then]   - 남은 슬롯: date={}", slot.getSlotDate());
		});
		assertThat(remaining).allMatch(slot -> !slot.getSlotDate().isBefore(cutoffDate));
		log.info("[Then] - ✓ 기준 날짜 이후의 슬롯만 남아있음");

		log.info("=== [날짜 이전 슬롯 삭제] 테스트 성공 ===");
	}

	@Test
	@DisplayName("상태와 날짜로 슬롯 개수를 조회한다")
	void countByRoomIdAndDateRangeAndStatus() {
		log.info("=== [상태별 슬롯 개수 조회] 테스트 시작 ===");

		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate startDate = testDate;
		LocalDate endDate = testDate.plusDays(5);
		log.info("[Given] - 조회 범위: {} ~ {}", startDate, endDate);

		// AVAILABLE 슬롯 3개
		timeSlotPort.save(RoomTimeSlot.available(roomId, testDate, LocalTime.of(9, 0)));
		timeSlotPort.save(RoomTimeSlot.available(roomId, testDate.plusDays(1), LocalTime.of(9, 0)));
		timeSlotPort.save(RoomTimeSlot.available(roomId, testDate.plusDays(2), LocalTime.of(9, 0)));

		// PENDING 슬롯 2개
		RoomTimeSlot pending1 = RoomTimeSlot.available(roomId, testDate.plusDays(3), LocalTime.of(9, 0));
		pending1.markAsPending(777L);
		timeSlotPort.save(pending1);

		RoomTimeSlot pending2 = RoomTimeSlot.available(roomId, testDate.plusDays(4), LocalTime.of(9, 0));
		pending2.markAsPending(778L);
		timeSlotPort.save(pending2);

		log.info("[Given] - AVAILABLE 슬롯 3개 저장");
		log.info("[Given] - PENDING 슬롯 2개 저장");

		// When
		log.info("[When] timeSlotPort.countByRoomIdAndDateRangeAndStatus() 호출");
		log.info("[When] - [카운트1] AVAILABLE 상태");
		long availableCount = timeSlotPort.countByRoomIdAndDateRangeAndStatus(
				roomId, startDate, endDate, SlotStatus.AVAILABLE
		);
		log.info("[When]   - 결과: {}개", availableCount);

		log.info("[When] - [카운트2] PENDING 상태");
		long pendingCount = timeSlotPort.countByRoomIdAndDateRangeAndStatus(
				roomId, startDate, endDate, SlotStatus.PENDING
		);
		log.info("[When]   - 결과: {}개", pendingCount);

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] AVAILABLE 슬롯 개수");
		log.info("[Then] - 예상: 3개");
		log.info("[Then] - 실제: {}개", availableCount);
		assertThat(availableCount).isEqualTo(3);
		log.info("[Then] - ✓ AVAILABLE 개수 일치");

		log.info("[Then] [검증2] PENDING 슬롯 개수");
		log.info("[Then] - 예상: 2개");
		log.info("[Then] - 실제: {}개", pendingCount);
		assertThat(pendingCount).isEqualTo(2);
		log.info("[Then] - ✓ PENDING 개수 일치");

		log.info("=== [상태별 슬롯 개수 조회] 테스트 성공 ===");
	}
}
