package com.teambind.springproject.room.service.integration;

import com.teambind.springproject.config.TestRedisConfig;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import com.teambind.springproject.room.repository.RoomTimeSlotRepository;
import com.teambind.springproject.room.query.application.TimeSlotQueryService;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TimeSlotQueryService 통합 테스트.
 * 
 * 실제 H2 데이터베이스와 Repository를 사용하여 조회 기능을 검증한다.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
@DisplayName("TimeSlotQueryService 통합 테스트")
class TimeSlotQueryServiceIntegrationTest {
	
	@Autowired
	private TimeSlotQueryService queryService;
	
	@Autowired
	private RoomTimeSlotRepository slotRepository;
	
	private Long roomId;
	private LocalDate testDate;
	
	@BeforeEach
	void setUp() {
		roomId = 100L;
		testDate = LocalDate.of(2025, 11, 5);
		
		// 테스트 데이터 준비: 11월 5일~7일, 각 날짜마다 9시~12시 슬롯
		for (int day = 5; day <= 7; day++) {
			LocalDate date = LocalDate.of(2025, 11, day);
			for (int hour = 9; hour <= 12; hour++) {
				RoomTimeSlot slot = RoomTimeSlot.available(roomId, date, LocalTime.of(hour, 0));
				slotRepository.save(slot);
			}
		}
		
		// 일부 슬롯을 PENDING으로 변경
		RoomTimeSlot pendingSlot = slotRepository.findByRoomIdAndSlotDateAndSlotTime(
				roomId, testDate, LocalTime.of(10, 0)
		).orElseThrow();
		pendingSlot.markAsPending(200L);
		slotRepository.save(pendingSlot);
		
		// 일부 슬롯을 RESERVED로 변경
		RoomTimeSlot reservedSlot = slotRepository.findByRoomIdAndSlotDateAndSlotTime(
				roomId, testDate, LocalTime.of(11, 0)
		).orElseThrow();
		reservedSlot.markAsPending(201L);
		reservedSlot.confirm();
		slotRepository.save(reservedSlot);
	}
	
	@Test
	@DisplayName("날짜 범위로 슬롯 목록을 조회한다")
	void getSlotsByDateRange() {
		log.info("=== [날짜 범위로 슬롯 목록을 조회한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate startDate = LocalDate.of(2025, 11, 5);
		LocalDate endDate = LocalDate.of(2025, 11, 7);
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - 조회 기간: {} ~ {} (3일)", startDate, endDate);
		log.info("[Given] - 사전 준비된 데이터: 11월 5일~7일, 각 날짜마다 9시~12시 슬롯 (총 12개)");
		
		// When
		log.info("[When] queryService.getSlotsByDateRange() 호출");
		log.info("[When] - 파라미터: roomId={}, startDate={}, endDate={}", roomId, startDate, endDate);
		List<RoomTimeSlot> slots = queryService.getSlotsByDateRange(roomId, startDate, endDate);
		log.info("[When] - 조회 완료: {} 개의 슬롯 반환됨", slots.size());
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 슬롯 개수 검증");
		log.info("[Then] - 예상(Expected): 12개 (3일 × 4시간)");
		log.info("[Then] - 실제(Actual): {}개", slots.size());
		assertThat(slots).hasSize(12);
		log.info("[Then] - ✓ 슬롯 개수 일치");
		
		log.info("[Then] [검증2] 모든 슬롯의 roomId 검증");
		log.info("[Then] - 예상(Expected): 모든 슬롯의 roomId = {}", roomId);
		long matchingRoomIdCount = slots.stream().filter(slot -> slot.getRoomId().equals(roomId)).count();
		log.info("[Then] - 실제(Actual): {}개 슬롯 중 {}개가 roomId={}", slots.size(), matchingRoomIdCount, roomId);
		assertThat(slots).allMatch(slot -> slot.getRoomId().equals(roomId));
		log.info("[Then] - ✓ 모든 슬롯의 roomId 일치");
		
		log.info("[Then] [검증3] 슬롯 날짜 범위 검증");
		log.info("[Then] - 예상(Expected): 모든 슬롯이 {} ~ {} 범위 내", startDate, endDate);
		LocalDate minDate = slots.stream().map(RoomTimeSlot::getSlotDate).min(LocalDate::compareTo).orElse(null);
		LocalDate maxDate = slots.stream().map(RoomTimeSlot::getSlotDate).max(LocalDate::compareTo).orElse(null);
		log.info("[Then] - 실제(Actual): 최소 날짜={}, 최대 날짜={}", minDate, maxDate);
		assertThat(slots).allMatch(slot ->
				!slot.getSlotDate().isBefore(startDate) && !slot.getSlotDate().isAfter(endDate)
		);
		log.info("[Then] - ✓ 모든 슬롯이 날짜 범위 내에 존재");
		
		log.info("=== [날짜 범위로 슬롯 목록을 조회한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("특정 날짜의 사용 가능한 슬롯만 조회한다")
	void getAvailableSlots() {
		log.info("=== [특정 날짜의 사용 가능한 슬롯만 조회한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 상태 확인");
		log.info("[Given] - roomId: {}, testDate: {}", roomId, testDate);
		log.info("[Given] - 9시 슬롯: AVAILABLE");
		log.info("[Given] - 10시 슬롯: PENDING (reservationId=200)");
		log.info("[Given] - 11시 슬롯: RESERVED (reservationId=201)");
		log.info("[Given] - 12시 슬롯: AVAILABLE");
		
		// When
		log.info("[When] queryService.getAvailableSlots() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}", roomId, testDate);
		List<RoomTimeSlot> availableSlots = queryService.getAvailableSlots(roomId, testDate);
		log.info("[When] - 조회 완료: {} 개의 사용 가능한 슬롯 반환됨", availableSlots.size());
		availableSlots.forEach(slot ->
				log.info("[When] - 슬롯: {}시, 상태={}", slot.getSlotTime().getHour(), slot.getStatus())
		);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 사용 가능한 슬롯 개수 검증");
		log.info("[Then] - 예상(Expected): 2개 (9시, 12시만 AVAILABLE)");
		log.info("[Then] - 실제(Actual): {}개", availableSlots.size());
		assertThat(availableSlots).hasSize(2);
		log.info("[Then] - ✓ 슬롯 개수 일치");
		
		log.info("[Then] [검증2] 모든 슬롯의 상태 검증");
		log.info("[Then] - 예상(Expected): 모든 슬롯이 AVAILABLE 상태");
		long availableCount = availableSlots.stream().filter(slot -> slot.getStatus() == SlotStatus.AVAILABLE).count();
		log.info("[Then] - 실제(Actual): {}개 슬롯 중 {}개가 AVAILABLE", availableSlots.size(), availableCount);
		assertThat(availableSlots).allMatch(slot -> slot.getStatus() == SlotStatus.AVAILABLE);
		log.info("[Then] - ✓ 모든 슬롯이 AVAILABLE 상태");
		
		log.info("[Then] [검증3] 슬롯 시간 검증");
		log.info("[Then] - 예상(Expected): 9시, 12시 슬롯");
		List<LocalTime> actualTimes = availableSlots.stream().map(RoomTimeSlot::getSlotTime).sorted().toList();
		log.info("[Then] - 실제(Actual): {}", actualTimes);
		assertThat(availableSlots).extracting(RoomTimeSlot::getSlotTime)
				.containsExactlyInAnyOrder(LocalTime.of(9, 0), LocalTime.of(12, 0));
		log.info("[Then] - ✓ 슬롯 시간 일치");
		
		log.info("=== [특정 날짜의 사용 가능한 슬롯만 조회한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("슬롯이 사용 가능한지 확인한다 - 사용 가능")
	void isSlotAvailable_Available() {
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 사용 가능] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 상태");
		log.info("[Given] - 확인할 슬롯: roomId={}, date={}, time=9시", roomId, testDate);
		log.info("[Given] - 9시 슬롯 상태: AVAILABLE");
		
		// When
		log.info("[When] queryService.isSlotAvailable() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}, time=09:00", roomId, testDate);
		boolean available = queryService.isSlotAvailable(roomId, testDate, LocalTime.of(9, 0));
		log.info("[When] - 반환 값: {}", available);
		
		// Then
		log.info("[Then] 결과 검증");
		log.info("[Then] - 예상(Expected): true (사용 가능)");
		log.info("[Then] - 실제(Actual): {}", available);
		assertThat(available).isTrue();
		log.info("[Then] - ✓ 슬롯이 사용 가능 상태로 확인됨");
		
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 사용 가능] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("슬롯이 사용 가능한지 확인한다 - 예약 중")
	void isSlotAvailable_Pending() {
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 예약 중] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 상태");
		log.info("[Given] - 확인할 슬롯: roomId={}, date={}, time=10시", roomId, testDate);
		log.info("[Given] - 10시 슬롯 상태: PENDING (reservationId=200)");
		
		// When
		log.info("[When] queryService.isSlotAvailable() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}, time=10:00", roomId, testDate);
		boolean available = queryService.isSlotAvailable(roomId, testDate, LocalTime.of(10, 0));
		log.info("[When] - 반환 값: {}", available);
		
		// Then
		log.info("[Then] 결과 검증");
		log.info("[Then] - 예상(Expected): false (예약 중이므로 사용 불가)");
		log.info("[Then] - 실제(Actual): {}", available);
		assertThat(available).isFalse();
		log.info("[Then] - ✓ PENDING 상태 슬롯이 사용 불가로 확인됨");
		
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 예약 중] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("슬롯이 사용 가능한지 확인한다 - 예약됨")
	void isSlotAvailable_Reserved() {
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 예약됨] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 상태");
		log.info("[Given] - 확인할 슬롯: roomId={}, date={}, time=11시", roomId, testDate);
		log.info("[Given] - 11시 슬롯 상태: RESERVED (reservationId=201, 이미 확정됨)");
		
		// When
		log.info("[When] queryService.isSlotAvailable() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}, time=11:00", roomId, testDate);
		boolean available = queryService.isSlotAvailable(roomId, testDate, LocalTime.of(11, 0));
		log.info("[When] - 반환 값: {}", available);
		
		// Then
		log.info("[Then] 결과 검증");
		log.info("[Then] - 예상(Expected): false (이미 예약 확정되어 사용 불가)");
		log.info("[Then] - 실제(Actual): {}", available);
		assertThat(available).isFalse();
		log.info("[Then] - ✓ RESERVED 상태 슬롯이 사용 불가로 확인됨");
		
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 예약됨] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("슬롯이 사용 가능한지 확인한다 - 존재하지 않음")
	void isSlotAvailable_NotExist() {
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 존재하지 않음] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 상태");
		log.info("[Given] - 확인할 슬롯: roomId={}, date={}, time=15시", roomId, testDate);
		log.info("[Given] - 15시 슬롯: 존재하지 않음 (준비된 슬롯은 9~12시만)");
		
		// When
		log.info("[When] queryService.isSlotAvailable() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}, time=15:00", roomId, testDate);
		boolean available = queryService.isSlotAvailable(roomId, testDate, LocalTime.of(15, 0));
		log.info("[When] - 반환 값: {}", available);
		
		// Then
		log.info("[Then] 결과 검증");
		log.info("[Then] - 예상(Expected): false (슬롯이 존재하지 않으므로 사용 불가)");
		log.info("[Then] - 실제(Actual): {}", available);
		assertThat(available).isFalse();
		log.info("[Then] - ✓ 존재하지 않는 슬롯이 사용 불가로 확인됨");
		
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 존재하지 않음] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("날짜 범위 내 사용 가능한 슬롯 개수를 센다")
	void countAvailableSlots() {
		log.info("=== [날짜 범위 내 사용 가능한 슬롯 개수를 센다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate startDate = LocalDate.of(2025, 11, 5);
		LocalDate endDate = LocalDate.of(2025, 11, 7);
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - 조회 기간: {} ~ {}", startDate, endDate);
		log.info("[Given] - 11/5: 9시(AVAILABLE), 10시(PENDING), 11시(RESERVED), 12시(AVAILABLE) → AVAILABLE 2개");
		log.info("[Given] - 11/6: 9시, 10시, 11시, 12시 모두 AVAILABLE → AVAILABLE 4개");
		log.info("[Given] - 11/7: 9시, 10시, 11시, 12시 모두 AVAILABLE → AVAILABLE 4개");
		log.info("[Given] - 기대값: 총 10개 (2+4+4)");
		
		// When
		log.info("[When] queryService.countAvailableSlots() 호출");
		log.info("[When] - 파라미터: roomId={}, startDate={}, endDate={}", roomId, startDate, endDate);
		long count = queryService.countAvailableSlots(roomId, startDate, endDate);
		log.info("[When] - 반환 값: {} 개", count);
		
		// Then
		log.info("[Then] 결과 검증");
		log.info("[Then] - 예상(Expected): 10개 (11/5: 2개 + 11/6: 4개 + 11/7: 4개)");
		log.info("[Then] - 실제(Actual): {}개", count);
		assertThat(count).isEqualTo(10);
		log.info("[Then] - ✓ 사용 가능한 슬롯 개수 일치");
		
		log.info("=== [날짜 범위 내 사용 가능한 슬롯 개수를 센다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("특정 날짜의 모든 슬롯을 조회한다")
	void getAllSlotsForDate() {
		log.info("=== [특정 날짜의 모든 슬롯을 조회한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 상태");
		log.info("[Given] - 조회 대상: roomId={}, testDate={}", roomId, testDate);
		log.info("[Given] - 9시: AVAILABLE, 10시: PENDING, 11시: RESERVED, 12시: AVAILABLE");
		
		// When
		log.info("[When] queryService.getAllSlotsForDate() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}", roomId, testDate);
		List<RoomTimeSlot> allSlots = queryService.getAllSlotsForDate(roomId, testDate);
		log.info("[When] - 조회 완료: {} 개의 슬롯 반환됨", allSlots.size());
		allSlots.forEach(slot ->
				log.info("[When] - {}시: {}", slot.getSlotTime().getHour(), slot.getStatus())
		);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 전체 슬롯 개수");
		log.info("[Then] - 예상(Expected): 4개 (9시, 10시, 11시, 12시)");
		log.info("[Then] - 실제(Actual): {}개", allSlots.size());
		assertThat(allSlots).hasSize(4);
		log.info("[Then] - ✓ 슬롯 개수 일치");
		
		log.info("[Then] [검증2] 슬롯 시간 확인");
		List<LocalTime> times = allSlots.stream().map(RoomTimeSlot::getSlotTime).sorted().toList();
		log.info("[Then] - 예상(Expected): 09:00, 10:00, 11:00, 12:00");
		log.info("[Then] - 실제(Actual): {}", times);
		assertThat(allSlots).extracting(RoomTimeSlot::getSlotTime)
				.containsExactlyInAnyOrder(
						LocalTime.of(9, 0),
						LocalTime.of(10, 0),
						LocalTime.of(11, 0),
						LocalTime.of(12, 0)
				);
		log.info("[Then] - ✓ 슬롯 시간 일치");
		
		log.info("[Then] [검증3] 슬롯 상태 분포 확인");
		long availableCount = allSlots.stream().filter(s -> s.getStatus() == SlotStatus.AVAILABLE).count();
		long pendingCount = allSlots.stream().filter(s -> s.getStatus() == SlotStatus.PENDING).count();
		long reservedCount = allSlots.stream().filter(s -> s.getStatus() == SlotStatus.RESERVED).count();
		log.info("[Then] - 예상(Expected): AVAILABLE=2개, PENDING=1개, RESERVED=1개");
		log.info("[Then] - 실제(Actual): AVAILABLE={}개, PENDING={}개, RESERVED={}개",
				availableCount, pendingCount, reservedCount);
		assertThat(availableCount).isEqualTo(2);
		assertThat(pendingCount).isEqualTo(1);
		assertThat(reservedCount).isEqualTo(1);
		log.info("[Then] - ✓ 슬롯 상태 분포 일치");
		
		log.info("=== [특정 날짜의 모든 슬롯을 조회한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("특정 상태의 슬롯 목록을 조회한다")
	void getSlotsByStatus() {
		log.info("=== [특정 상태의 슬롯 목록을 조회한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 상태");
		log.info("[Given] - 조회 조건: roomId={}, testDate={}, status=PENDING", roomId, testDate);
		log.info("[Given] - 10시 슬롯만 PENDING 상태 (reservationId=200)");
		
		// When
		log.info("[When] queryService.getSlotsByStatus() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}, status=PENDING", roomId, testDate);
		List<RoomTimeSlot> pendingSlots = queryService.getSlotsByStatus(
				roomId, testDate, SlotStatus.PENDING
		);
		log.info("[When] - 조회 완료: {} 개의 PENDING 슬롯 반환됨", pendingSlots.size());
		pendingSlots.forEach(slot ->
				log.info("[When] - {}시: reservationId={}", slot.getSlotTime().getHour(), slot.getReservationId())
		);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] PENDING 슬롯 개수");
		log.info("[Then] - 예상(Expected): 1개");
		log.info("[Then] - 실제(Actual): {}개", pendingSlots.size());
		assertThat(pendingSlots).hasSize(1);
		log.info("[Then] - ✓ PENDING 슬롯 개수 일치");
		
		log.info("[Then] [검증2] PENDING 슬롯 시간");
		log.info("[Then] - 예상(Expected): 10:00");
		log.info("[Then] - 실제(Actual): {}", pendingSlots.get(0).getSlotTime());
		assertThat(pendingSlots.get(0).getSlotTime()).isEqualTo(LocalTime.of(10, 0));
		log.info("[Then] - ✓ 슬롯 시간 일치");
		
		log.info("[Then] [검증3] 예약 ID");
		log.info("[Then] - 예상(Expected): reservationId=200");
		log.info("[Then] - 실제(Actual): reservationId={}", pendingSlots.get(0).getReservationId());
		assertThat(pendingSlots.get(0).getReservationId()).isEqualTo(200L);
		log.info("[Then] - ✓ 예약 ID 일치");
		
		log.info("=== [특정 상태의 슬롯 목록을 조회한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("다른 룸의 슬롯은 조회되지 않는다")
	void differentRoomNotIncluded() {
		log.info("=== [다른 룸의 슬롯은 조회되지 않는다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		Long otherRoomId = 999L;
		log.info("[Given] - 대상 roomId: {}", roomId);
		log.info("[Given] - 다른 roomId: {}", otherRoomId);
		log.info("[Given] - 다른 룸(999)의 슬롯 추가 중: testDate={}, time=09:00", testDate);
		RoomTimeSlot otherRoomSlot = RoomTimeSlot.available(
				otherRoomId, testDate, LocalTime.of(9, 0)
		);
		slotRepository.save(otherRoomSlot);
		log.info("[Given] - 다른 룸 슬롯 저장 완료");
		
		// When
		log.info("[When] queryService.getAllSlotsForDate() 호출");
		log.info("[When] - 파라미터: roomId={} (다른 룸 999는 제외되어야 함), testDate={}", roomId, testDate);
		List<RoomTimeSlot> slots = queryService.getAllSlotsForDate(roomId, testDate);
		log.info("[When] - 조회 완료: {} 개의 슬롯 반환됨", slots.size());
		slots.forEach(slot ->
				log.info("[When] - roomId={}, {}시", slot.getRoomId(), slot.getSlotTime().getHour())
		);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 슬롯 개수 (다른 룸 제외)");
		log.info("[Then] - 예상(Expected): 4개 (roomId={}의 슬롯만, 다른 룸 제외)", roomId);
		log.info("[Then] - 실제(Actual): {}개", slots.size());
		assertThat(slots).hasSize(4);
		log.info("[Then] - ✓ 다른 룸 슬롯이 제외되고 4개만 조회됨");
		
		log.info("[Then] [검증2] 모든 슬롯이 대상 룸의 것인지 확인");
		long matchingRoomCount = slots.stream().filter(slot -> slot.getRoomId().equals(roomId)).count();
		log.info("[Then] - 예상(Expected): 모든 슬롯의 roomId={}", roomId);
		log.info("[Then] - 실제(Actual): {}개 슬롯 중 {}개가 roomId={}", slots.size(), matchingRoomCount, roomId);
		assertThat(slots).allMatch(slot -> slot.getRoomId().equals(roomId));
		log.info("[Then] - ✓ 모든 슬롯이 올바른 룸의 것");
		
		log.info("=== [다른 룸의 슬롯은 조회되지 않는다] 테스트 성공 ===");
	}
}
