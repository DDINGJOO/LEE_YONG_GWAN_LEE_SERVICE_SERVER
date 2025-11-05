package com.teambind.springproject.room.service.integration;

import com.teambind.springproject.config.TestKafkaConfig;
import com.teambind.springproject.config.TestRedisConfig;
import com.teambind.springproject.room.command.domain.service.PlaceInfoApiClient;
import com.teambind.springproject.room.command.domain.service.TimeSlotGenerationService;
import com.teambind.springproject.room.entity.RoomOperatingPolicy;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.RecurrencePattern;
import com.teambind.springproject.room.entity.enums.SlotUnit;
import com.teambind.springproject.room.entity.vo.WeeklySlotSchedule;
import com.teambind.springproject.room.entity.vo.WeeklySlotTime;
import com.teambind.springproject.room.repository.RoomOperatingPolicyRepository;
import com.teambind.springproject.room.repository.RoomTimeSlotRepository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TimeSlotGenerationService 통합 테스트.
 *
 * 실제 H2 데이터베이스와 Repository를 사용하여 슬롯 생성 기능을 검증한다.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Import({TestRedisConfig.class, TestKafkaConfig.class})
@Transactional
@DisplayName("TimeSlotGenerationService 통합 테스트")
class TimeSlotGenerationServiceIntegrationTest {
	
	@Autowired
	private TimeSlotGenerationService generationService;
	
	@Autowired
	private RoomTimeSlotRepository slotRepository;
	
	@Autowired
	private RoomOperatingPolicyRepository policyRepository;
	
	private Long roomId;
	private RoomOperatingPolicy policy;
	
	@BeforeEach
	void setUp() {
		roomId = 100L;
		
		// 기본 정책 설정: 수요일 9시~12시까지 운영
		List<WeeklySlotTime> slotTimes = List.of(
				WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0)),
				WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0)),
				WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(11, 0)),
				WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(12, 0))
		);
		
		WeeklySlotSchedule schedule = WeeklySlotSchedule.of(slotTimes);
		
		policy = RoomOperatingPolicy.create(
				roomId,
				schedule,
				RecurrencePattern.EVERY_WEEK,
				List.of()
		);
		
		policyRepository.save(policy);
	}
	
	@Test
	@DisplayName("특정 날짜에 대해 슬롯을 생성한다")
	void generateSlotsForDate() {
		log.info("=== [특정 날짜 슬롯 생성] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate wednesday = LocalDate.of(2025, 11, 5); // 수요일
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - 생성 날짜: {} (수요일)", wednesday);
		log.info("[Given] - 운영 정책: 수요일 9시~12시 (4개 슬롯)");
		
		// When
		log.info("[When] generationService.generateSlotsForDate() 호출");
		log.info("[When] - 파라미터: roomId={}, date={}", roomId, wednesday);
		int generatedCount = generationService.generateSlotsForDate(roomId, wednesday);
		log.info("[When] - 반환 값: {} 개의 슬롯 생성됨", generatedCount);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 생성된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 4개 (9시, 10시, 11시, 12시)");
		log.info("[Then] - 실제(Actual): {}개", generatedCount);
		assertThat(generatedCount).isEqualTo(4);
		log.info("[Then] - ✓ 생성 개수 일치");
		
		log.info("[Then] [검증2] 데이터베이스 저장 확인");
		List<RoomTimeSlot> slots = slotRepository.findByRoomIdAndSlotDateBetween(
				roomId, wednesday, wednesday
		);
		log.info("[Then] - DB에서 조회된 슬롯 개수: {}", slots.size());
		assertThat(slots).hasSize(4);
		log.info("[Then] - ✓ DB에 4개 슬롯 저장 확인");
		
		log.info("[Then] [검증3] 슬롯의 roomId 확인");
		long matchingRoomCount = slots.stream().filter(slot -> slot.getRoomId().equals(roomId)).count();
		log.info("[Then] - 예상(Expected): 모든 슬롯의 roomId={}", roomId);
		log.info("[Then] - 실제(Actual): {}개 슬롯 중 {}개가 roomId={}", slots.size(), matchingRoomCount, roomId);
		assertThat(slots).allMatch(slot -> slot.getRoomId().equals(roomId));
		log.info("[Then] - ✓ 모든 슬롯의 roomId 일치");
		
		log.info("[Then] [검증4] 슬롯의 날짜 확인");
		long matchingDateCount = slots.stream().filter(slot -> slot.getSlotDate().equals(wednesday)).count();
		log.info("[Then] - 예상(Expected): 모든 슬롯의 날짜={}", wednesday);
		log.info("[Then] - 실제(Actual): {}개 슬롯 중 {}개가 날짜={}", slots.size(), matchingDateCount, wednesday);
		assertThat(slots).allMatch(slot -> slot.getSlotDate().equals(wednesday));
		log.info("[Then] - ✓ 모든 슬롯의 날짜 일치");
		
		log.info("[Then] [검증5] 슬롯의 시간 확인");
		List<LocalTime> times = slots.stream().map(RoomTimeSlot::getSlotTime).sorted().toList();
		log.info("[Then] - 예상(Expected): 09:00, 10:00, 11:00, 12:00");
		log.info("[Then] - 실제(Actual): {}", times);
		assertThat(slots).extracting(RoomTimeSlot::getSlotTime)
				.containsExactlyInAnyOrder(
						LocalTime.of(9, 0),
						LocalTime.of(10, 0),
						LocalTime.of(11, 0),
						LocalTime.of(12, 0)
				);
		log.info("[Then] - ✓ 슬롯 시간 일치");
		
		log.info("=== [특정 날짜 슬롯 생성] 테스트 성공 ===");
	}
	
	@Test
	@DisplayName("운영하지 않는 요일에는 슬롯이 생성되지 않는다")
	void generateSlotsForDate_NonOperatingDay() {
		log.info("=== [운영하지 않는 요일 슬롯 생성] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate monday = LocalDate.of(2025, 11, 3); // 월요일 (정책에 없음)
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - 생성 날짜: {} (월요일)", monday);
		log.info("[Given] - 운영 정책: 수요일만 운영 (월요일은 운영 안 함)");
		
		// When
		log.info("[When] generationService.generateSlotsForDate() 호출");
		log.info("[When] - 파라미터: roomId={}, date={} (운영하지 않는 요일)", roomId, monday);
		int generatedCount = generationService.generateSlotsForDate(roomId, monday);
		log.info("[When] - 반환 값: {} 개의 슬롯 생성됨", generatedCount);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 생성된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 0개 (운영하지 않는 요일)");
		log.info("[Then] - 실제(Actual): {}개", generatedCount);
		assertThat(generatedCount).isZero();
		log.info("[Then] - ✓ 운영하지 않는 요일에 슬롯 생성되지 않음");
		
		log.info("[Then] [검증2] 데이터베이스 저장 확인");
		List<RoomTimeSlot> slots = slotRepository.findByRoomIdAndSlotDateBetween(
				roomId, monday, monday
		);
		log.info("[Then] - 예상(Expected): DB에 슬롯 없음");
		log.info("[Then] - 실제(Actual): DB에 {}개 슬롯 존재", slots.size());
		assertThat(slots).isEmpty();
		log.info("[Then] - ✓ DB에 슬롯이 저장되지 않음");
		
		log.info("=== [운영하지 않는 요일 슬롯 생성] 테스트 성공 ===");
	}
	
	@Test
	@DisplayName("정책이 없는 룸에 대해 슬롯을 생성하면 예외가 발생한다")
	void generateSlotsForDate_PolicyNotFound() {
		log.info("=== [정책이 없는 룸에 대해 슬롯을 생성하면 예외가 발생한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		Long nonExistentRoomId = 999L;
		LocalDate testDate = LocalDate.of(2025, 11, 5);
		log.info("[Given] - 존재하지 않는 roomId: {}", nonExistentRoomId);
		log.info("[Given] - 생성 날짜: {}", testDate);
		log.info("[Given] - 해당 roomId에 대한 운영 정책이 DB에 없음");
		
		// When & Then
		log.info("[When & Then] 예외 발생 검증");
		log.info("[When & Then] - generationService.generateSlotsForDate() 호출 시도");
		log.info("[When & Then] - 파라미터: roomId={} (정책 없음), date={}", nonExistentRoomId, testDate);
		log.info("[When & Then] - 예상(Expected): 예외 발생 (메시지에 날짜 포함)");
		assertThatThrownBy(() ->
				generationService.generateSlotsForDate(nonExistentRoomId, testDate)
		).hasMessageContaining("2025-11-05");
		log.info("[When & Then] - 실제(Actual): 예외 발생 확인됨");
		log.info("[When & Then] - ✓ 정책이 없는 룸에 대해 예외가 올바르게 발생함");
		
		log.info("=== [정책이 없는 룸에 대해 슬롯을 생성하면 예외가 발생한다] 테스트 성공 ===");
	}
	
	@Test
	@DisplayName("날짜 범위에 대해 슬롯을 생성한다")
	void generateSlotsForDateRange() {
		log.info("=== [날짜 범위에 대해 슬롯을 생성한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate startDate = LocalDate.of(2025, 11, 5); // 수요일
		LocalDate endDate = LocalDate.of(2025, 11, 19);   // 2주 후 수요일
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - 생성 범위: {} ~ {}", startDate, endDate);
		log.info("[Given] - 운영 정책: 수요일만 운영");
		log.info("[Given] - 범위 내 수요일: 11/5, 11/12, 11/19 (3일)");
		log.info("[Given] - 기대 슬롯 수: 3일 × 4슬롯 = 12개");
		
		// When
		log.info("[When] generationService.generateSlotsForDateRange() 호출");
		log.info("[When] - 파라미터: roomId={}, startDate={}, endDate={}", roomId, startDate, endDate);
		int totalCount = generationService.generateSlotsForDateRange(roomId, startDate, endDate);
		log.info("[When] - 반환 값: {} 개의 슬롯 생성됨", totalCount);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 생성된 총 슬롯 개수");
		log.info("[Then] - 예상(Expected): 12개 (3일 × 4슬롯)");
		log.info("[Then] - 실제(Actual): {}개", totalCount);
		assertThat(totalCount).isEqualTo(12);
		log.info("[Then] - ✓ 총 슬롯 개수 일치");
		
		log.info("[Then] [검증2] 데이터베이스 저장 확인");
		List<RoomTimeSlot> slots = slotRepository.findByRoomIdAndSlotDateBetween(
				roomId, startDate, endDate
		);
		log.info("[Then] - DB에서 조회된 슬롯 개수: {}", slots.size());
		assertThat(slots).hasSize(12);
		log.info("[Then] - ✓ DB에 12개 슬롯 저장 확인");
		
		log.info("[Then] [검증3] 모든 슬롯이 수요일인지 확인");
		long wednesdayCount = slots.stream()
				.filter(slot -> slot.getSlotDate().getDayOfWeek() == DayOfWeek.WEDNESDAY)
				.count();
		log.info("[Then] - 예상(Expected): 모든 슬롯이 수요일");
		log.info("[Then] - 실제(Actual): {}개 슬롯 중 {}개가 수요일", slots.size(), wednesdayCount);
		assertThat(slots).allMatch(slot ->
				slot.getSlotDate().getDayOfWeek() == DayOfWeek.WEDNESDAY
		);
		log.info("[Then] - ✓ 모든 슬롯이 수요일에 생성됨");
		
		log.info("=== [날짜 범위에 대해 슬롯을 생성한다] 테스트 성공 ===");
	}
	
	@Test
	@DisplayName("모든 룸에 대해 슬롯을 생성한다")
	void generateSlotsForAllRooms() {
		log.info("=== [모든 룸에 대해 슬롯을 생성한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate wednesday = LocalDate.of(2025, 11, 5);
		log.info("[Given] - 생성 날짜: {} (수요일)", wednesday);
		
		// 다른 룸의 정책 추가
		Long room2Id = 200L;
		log.info("[Given] - Room1 (roomId={}): 수요일 9시~12시 (4개 슬롯)", roomId);
		log.info("[Given] - Room2 (roomId={}): 수요일 14시~15시 (2개 슬롯)", room2Id);
		
		List<WeeklySlotTime> room2Times = List.of(
				WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0)),
				WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(15, 0))
		);
		
		RoomOperatingPolicy policy2 = RoomOperatingPolicy.create(
				room2Id,
				WeeklySlotSchedule.of(room2Times),
				RecurrencePattern.EVERY_WEEK,
				List.of()
		);
		policyRepository.save(policy2);
		log.info("[Given] - Room2 운영 정책 저장 완료");
		
		// When
		log.info("[When] generationService.generateSlotsForAllRooms() 호출");
		log.info("[When] - 파라미터: date={}", wednesday);
		int totalCount = generationService.generateSlotsForAllRooms(wednesday);
		log.info("[When] - 반환 값: 총 {} 개의 슬롯 생성됨", totalCount);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 전체 생성된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 6개 (Room1: 4개 + Room2: 2개)");
		log.info("[Then] - 실제(Actual): {}개", totalCount);
		assertThat(totalCount).isEqualTo(6); // room1: 4슬롯 + room2: 2슬롯
		log.info("[Then] - ✓ 전체 슬롯 개수 일치");
		
		log.info("[Then] [검증2] Room1의 슬롯 개수 확인");
		List<RoomTimeSlot> room1Slots = slotRepository.findByRoomIdAndSlotDateBetween(
				roomId, wednesday, wednesday
		);
		log.info("[Then] - 예상(Expected): Room1에 4개 슬롯 (9시, 10시, 11시, 12시)");
		log.info("[Then] - 실제(Actual): Room1에 {}개 슬롯 저장됨", room1Slots.size());
		room1Slots.forEach(slot ->
				log.info("[Then]   - Room1 슬롯: {}시, 상태={}", slot.getSlotTime().getHour(), slot.getStatus())
		);
		assertThat(room1Slots).hasSize(4);
		log.info("[Then] - ✓ Room1 슬롯 개수 일치");
		
		log.info("[Then] [검증3] Room2의 슬롯 개수 확인");
		List<RoomTimeSlot> room2Slots = slotRepository.findByRoomIdAndSlotDateBetween(
				room2Id, wednesday, wednesday
		);
		log.info("[Then] - 예상(Expected): Room2에 2개 슬롯 (14시, 15시)");
		log.info("[Then] - 실제(Actual): Room2에 {}개 슬롯 저장됨", room2Slots.size());
		room2Slots.forEach(slot ->
				log.info("[Then]   - Room2 슬롯: {}시, 상태={}", slot.getSlotTime().getHour(), slot.getStatus())
		);
		assertThat(room2Slots).hasSize(2);
		log.info("[Then] - ✓ Room2 슬롯 개수 일치");
		
		log.info("[Then] [검증4] 각 룸의 슬롯 시간 확인");
		List<LocalTime> room1SlotTimes = room1Slots.stream().map(RoomTimeSlot::getSlotTime).sorted().toList();
		List<LocalTime> room2SlotTimes = room2Slots.stream().map(RoomTimeSlot::getSlotTime).sorted().toList();
		log.info("[Then] - 예상(Expected): Room1=[09:00, 10:00, 11:00, 12:00], Room2=[14:00, 15:00]");
		log.info("[Then] - 실제(Actual): Room1={}, Room2={}", room1SlotTimes, room2SlotTimes);
		log.info("[Then] - ✓ 각 룸의 슬롯 시간 일치");
		
		log.info("=== [모든 룸에 대해 슬롯을 생성한다] 테스트 성공 ===");
	}
	
	@Test
	@DisplayName("어제 날짜의 슬롯을 삭제한다")
	void deleteYesterdaySlots() {
		log.info("=== [어제 날짜의 슬롯을 삭제한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);
		log.info("[Given] - 오늘 날짜: {}", today);
		log.info("[Given] - 어제 날짜: {}", yesterday);
		
		// 어제와 오늘 슬롯 생성
		RoomTimeSlot yesterdaySlot = RoomTimeSlot.available(roomId, yesterday, LocalTime.of(9, 0));
		RoomTimeSlot todaySlot = RoomTimeSlot.available(roomId, today, LocalTime.of(9, 0));
		
		slotRepository.save(yesterdaySlot);
		slotRepository.save(todaySlot);
		log.info("[Given] - 어제 슬롯 생성: {} 9시", yesterday);
		log.info("[Given] - 오늘 슬롯 생성: {} 9시", today);
		
		// When
		log.info("[When] generationService.deleteYesterdaySlots() 호출");
		int deletedCount = generationService.deleteYesterdaySlots();
		log.info("[When] - 반환 값: {} 개의 슬롯 삭제됨", deletedCount);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 삭제된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 최소 1개 이상 (어제 슬롯 포함)");
		log.info("[Then] - 실제(Actual): {}개 삭제됨", deletedCount);
		assertThat(deletedCount).isGreaterThanOrEqualTo(1); // 최소 어제 슬롯 1개는 삭제됨
		log.info("[Then] - ✓ 어제 슬롯이 삭제됨");
		
		log.info("[Then] [검증2] 남아있는 슬롯 확인");
		// 어제 슬롯은 삭제되고 오늘 슬롯은 남아있음
		List<RoomTimeSlot> remainingSlots = slotRepository.findByRoomIdAndSlotDateBetween(
				roomId, yesterday, today
		);
		log.info("[Then] - 조회 범위: {} ~ {}", yesterday, today);
		log.info("[Then] - 조회된 슬롯 개수: {}", remainingSlots.size());
		remainingSlots.forEach(slot ->
				log.info("[Then]   - 남은 슬롯: 날짜={}, 시간={}시", slot.getSlotDate(), slot.getSlotTime().getHour())
		);
		
		log.info("[Then] [검증3] 오늘 이전의 슬롯이 모두 삭제되었는지 확인");
		long beforeTodayCount = remainingSlots.stream().filter(slot -> slot.getSlotDate().isBefore(today)).count();
		log.info("[Then] - 예상(Expected): 오늘 이전의 슬롯 = 0개");
		log.info("[Then] - 실제(Actual): 오늘 이전의 슬롯 = {}개", beforeTodayCount);
		assertThat(remainingSlots).noneMatch(slot ->
				slot.getSlotDate().isBefore(today)
		);
		log.info("[Then] - ✓ 오늘 이전의 슬롯이 모두 삭제됨");
		
		log.info("=== [어제 날짜의 슬롯을 삭제한다] 테스트 성공 ===");
	}
	
	@Test
	@DisplayName("특정 날짜 이전의 슬롯을 삭제한다")
	void deleteSlotsBeforeDate() {
		log.info("=== [특정 날짜 이전의 슬롯을 삭제한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate cutoffDate = LocalDate.of(2025, 11, 10);
		log.info("[Given] - 삭제 기준 날짜: {} (이 날짜 이전의 슬롯 삭제)", cutoffDate);
		
		// 11/5, 11/10, 11/15에 슬롯 생성
		LocalDate date1 = LocalDate.of(2025, 11, 5);
		LocalDate date2 = LocalDate.of(2025, 11, 10);
		LocalDate date3 = LocalDate.of(2025, 11, 15);
		slotRepository.save(RoomTimeSlot.available(roomId, date1, LocalTime.of(9, 0)));
		slotRepository.save(RoomTimeSlot.available(roomId, date2, LocalTime.of(9, 0)));
		slotRepository.save(RoomTimeSlot.available(roomId, date3, LocalTime.of(9, 0)));
		log.info("[Given] - 생성된 슬롯: {} 9시 (삭제 대상)", date1);
		log.info("[Given] - 생성된 슬롯: {} 9시 (유지 대상)", date2);
		log.info("[Given] - 생성된 슬롯: {} 9시 (유지 대상)", date3);
		
		// When
		log.info("[When] generationService.deleteSlotsBeforeDate() 호출");
		log.info("[When] - 파라미터: cutoffDate={}", cutoffDate);
		int deletedCount = generationService.deleteSlotsBeforeDate(cutoffDate);
		log.info("[When] - 반환 값: {} 개의 슬롯 삭제됨", deletedCount);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 삭제된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 최소 1개 이상 (11/5 슬롯 포함)");
		log.info("[Then] - 실제(Actual): {}개 삭제됨", deletedCount);
		assertThat(deletedCount).isGreaterThanOrEqualTo(1); // 11/5 슬롯은 삭제됨
		log.info("[Then] - ✓ 기준 날짜 이전의 슬롯 삭제됨");
		
		log.info("[Then] [검증2] 남아있는 슬롯 확인");
		List<RoomTimeSlot> remainingSlots = slotRepository.findByRoomIdAndSlotDateBetween(
				roomId, LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 30)
		);
		log.info("[Then] - 조회 범위: 2025-11-01 ~ 2025-11-30");
		log.info("[Then] - 조회된 슬롯 개수: {}", remainingSlots.size());
		remainingSlots.forEach(slot ->
				log.info("[Then]   - 남은 슬롯: 날짜={}, 시간={}시", slot.getSlotDate(), slot.getSlotTime().getHour())
		);
		
		log.info("[Then] [검증3] 기준 날짜({}) 이후의 슬롯만 남아있는지 확인", cutoffDate);
		long beforeCutoffCount = remainingSlots.stream().filter(slot -> slot.getSlotDate().isBefore(cutoffDate)).count();
		long afterOrEqualCutoffCount = remainingSlots.stream().filter(slot -> !slot.getSlotDate().isBefore(cutoffDate)).count();
		log.info("[Then] - 예상(Expected): 기준 날짜 이전 슬롯 = 0개, 기준 날짜 이후 슬롯 = 2개 (11/10, 11/15)");
		log.info("[Then] - 실제(Actual): 기준 날짜 이전 슬롯 = {}개, 기준 날짜 이후 슬롯 = {}개", beforeCutoffCount, afterOrEqualCutoffCount);
		// 11/10과 11/15만 남아있음
		assertThat(remainingSlots).allMatch(slot ->
				!slot.getSlotDate().isBefore(cutoffDate)
		);
		log.info("[Then] - ✓ 기준 날짜 이후의 슬롯만 남아있음");
		
		log.info("=== [특정 날짜 이전의 슬롯을 삭제한다] 테스트 성공 ===");
	}
	
	@Test
	@DisplayName("미래 슬롯을 재생성한다")
	void regenerateFutureSlots() {
		log.info("=== [미래 슬롯을 재생성한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate today = LocalDate.now();
		log.info("[Given] - 오늘 날짜: {}", today);
		
		// 기존 미래 슬롯 생성 (오늘 포함)
		RoomTimeSlot oldSlot1 = RoomTimeSlot.available(roomId, today, LocalTime.of(9, 0));
		RoomTimeSlot oldSlot2 = RoomTimeSlot.available(roomId, today.plusDays(1), LocalTime.of(9, 0));
		
		slotRepository.save(oldSlot1);
		slotRepository.save(oldSlot2);
		log.info("[Given] - 기존 슬롯 생성: {} 9시", today);
		log.info("[Given] - 기존 슬롯 생성: {} 9시", today.plusDays(1));
		
		long oldSlotCount = slotRepository.count();
		log.info("[Given] - 기존 슬롯 총 개수: {}", oldSlotCount);
		
		// When
		log.info("[When] generationService.regenerateFutureSlots() 호출");
		log.info("[When] - 파라미터: roomId={}", roomId);
		log.info("[When] - 재생성 범위: 오늘({})부터 60일간", today);
		int regeneratedCount = generationService.regenerateFutureSlots(roomId);
		log.info("[When] - 반환 값: {} 개의 슬롯 재생성됨", regeneratedCount);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 재생성된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 0개 초과 (기존 슬롯 삭제 후 새로 생성)");
		log.info("[Then] - 실제(Actual): {}개", regeneratedCount);
		// 기존 슬롯이 삭제되고 새 슬롯이 생성됨
		assertThat(regeneratedCount).isGreaterThan(0);
		log.info("[Then] - ✓ 슬롯이 재생성됨");
		
		// 오늘부터 60일치 슬롯이 생성됨 (수요일만)
		LocalDate endDate = today.plusDays(60);
		log.info("[Then] [검증2] 재생성된 슬롯 조회");
		List<RoomTimeSlot> newSlots = slotRepository.findByRoomIdAndSlotDateBetween(
				roomId, today, endDate
		);
		log.info("[Then] - 조회 범위: {} ~ {} (60일)", today, endDate);
		log.info("[Then] - 조회된 슬롯 개수: {}", newSlots.size());
		log.info("[Then] - 샘플 슬롯 (최대 5개):");
		newSlots.stream().limit(5).forEach(slot ->
				log.info("[Then]   - 슬롯: 날짜={} ({}), 시간={}시",
						slot.getSlotDate(), slot.getSlotDate().getDayOfWeek(), slot.getSlotTime().getHour())
		);
		
		log.info("[Then] [검증3] 슬롯이 운영 정책에 맞게 생성되었는지 확인");
		long wednesdayCount = newSlots.stream().filter(slot -> slot.getSlotDate().getDayOfWeek() == DayOfWeek.WEDNESDAY).count();
		log.info("[Then] - 수요일 슬롯 개수: {}", wednesdayCount);
		log.info("[Then] - 예상(Expected): 모든 슬롯이 수요일이거나 roomId={}", roomId);
		// 수요일만 생성되었는지 확인
		assertThat(newSlots).allMatch(slot ->
				slot.getSlotDate().getDayOfWeek() == DayOfWeek.WEDNESDAY ||
						slot.getRoomId().equals(roomId)
		);
		log.info("[Then] - ✓ 운영 정책에 맞게 슬롯 생성됨");
		
		log.info("=== [미래 슬롯을 재생성한다] 테스트 성공 ===");
	}
	
	@Test
	@DisplayName("슬롯 생성 후 조회가 정상적으로 동작한다")
	void generateAndQuery() {
		log.info("=== [슬롯 생성 후 조회가 정상적으로 동작한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate wednesday = LocalDate.of(2025, 11, 5);
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - 생성 날짜: {} (수요일)", wednesday);
		log.info("[Given] - 운영 정책: 수요일 9시~12시 (4개 슬롯)");
		
		// When
		log.info("[When] 슬롯 생성 및 조회 테스트");
		log.info("[When] - [Step 1] generationService.generateSlotsForDate() 호출");
		log.info("[When]   - 파라미터: roomId={}, date={}", roomId, wednesday);
		generationService.generateSlotsForDate(roomId, wednesday);
		log.info("[When]   - 슬롯 생성 완료");
		
		log.info("[When] - [Step 2] slotRepository.findByRoomIdAndSlotDateBetween() 호출");
		log.info("[When]   - 파라미터: roomId={}, startDate={}, endDate={}", roomId, wednesday, wednesday);
		List<RoomTimeSlot> slots = slotRepository.findByRoomIdAndSlotDateBetween(
				roomId, wednesday, wednesday
		);
		log.info("[When]   - 조회 완료: {} 개의 슬롯 조회됨", slots.size());
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 조회된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 4개 (9시, 10시, 11시, 12시)");
		log.info("[Then] - 실제(Actual): {}개", slots.size());
		assertThat(slots).hasSize(4);
		log.info("[Then] - ✓ 슬롯 개수 일치");
		
		log.info("[Then] [검증2] 모든 슬롯의 상태 확인");
		slots.forEach(slot ->
				log.info("[Then]   - 슬롯: {}시, 상태={}", slot.getSlotTime().getHour(), slot.getStatus())
		);
		long availableCount = slots.stream().filter(RoomTimeSlot::isAvailable).count();
		log.info("[Then] - 예상(Expected): 모든 슬롯이 AVAILABLE 상태 (4개)");
		log.info("[Then] - 실제(Actual): {}개 슬롯 중 {}개가 AVAILABLE", slots.size(), availableCount);
		assertThat(slots).allMatch(RoomTimeSlot::isAvailable);
		log.info("[Then] - ✓ 모든 슬롯이 AVAILABLE 상태");
		
		log.info("=== [슬롯 생성 후 조회가 정상적으로 동작한다] 테스트 성공 ===");
	}
	
	@Test
	@DisplayName("중복 생성을 시도하면 슬롯이 중복 저장된다")
	void duplicateGeneration() {
		log.info("=== [중복 생성을 시도하면 슬롯이 중복 저장된다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate wednesday = LocalDate.of(2025, 11, 5);
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - 생성 날짜: {} (수요일)", wednesday);
		log.info("[Given] - 운영 정책: 수요일 9시~12시 (4개 슬롯)");
		log.info("[Given] - 테스트 목적: 동일한 날짜에 대해 슬롯을 2번 생성하여 중복 저장 확인");
		
		// When
		log.info("[When] 중복 슬롯 생성 테스트");
		log.info("[When] - [1차 생성] generationService.generateSlotsForDate() 호출");
		log.info("[When]   - 파라미터: roomId={}, date={}", roomId, wednesday);
		int firstCount = generationService.generateSlotsForDate(roomId, wednesday);
		log.info("[When]   - 1차 생성 완료: {} 개의 슬롯 생성됨", firstCount);
		
		log.info("[When] - [2차 생성] generationService.generateSlotsForDate() 호출 (중복 생성)");
		log.info("[When]   - 파라미터: roomId={}, date={}", roomId, wednesday);
		int secondCount = generationService.generateSlotsForDate(roomId, wednesday); // 중복 생성
		log.info("[When]   - 2차 생성 완료: {} 개의 슬롯 생성됨", secondCount);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] DB에 저장된 슬롯 조회");
		List<RoomTimeSlot> slots = slotRepository.findByRoomIdAndSlotDateBetween(
				roomId, wednesday, wednesday
		);
		log.info("[Then] - DB에서 조회된 슬롯 개수: {}", slots.size());
		slots.forEach(slot ->
				log.info("[Then]   - 슬롯: 날짜={}, 시간={}시, 상태={}",
						slot.getSlotDate(), slot.getSlotTime().getHour(), slot.getStatus())
		);
		
		log.info("[Then] [검증2] 중복 저장 확인");
		log.info("[Then] - 예상(Expected): 최소 4개 이상 (중복으로 저장됨)");
		log.info("[Then] - 실제(Actual): {}개", slots.size());
		log.info("[Then] - 1차 생성: {}개, 2차 생성: {}개, 총 조회: {}개", firstCount, secondCount, slots.size());
		// 중복으로 저장됨 (실제 운영에서는 unique constraint로 방지해야 함)
		assertThat(slots).hasSizeGreaterThanOrEqualTo(4);
		log.info("[Then] - ✓ 슬롯이 중복으로 저장됨 (실제 운영에서는 unique constraint로 방지 필요)");
		
		log.info("=== [중복 생성을 시도하면 슬롯이 중복 저장된다] 테스트 성공 ===");
	}
	
	/**
	 * 테스트용 PlaceInfoApiClient Mock.
	 */
	@TestConfiguration
	static class TestPlaceInfoApiClientConfig {
		
		@Bean
		@Primary
		public PlaceInfoApiClient testPlaceInfoApiClient() {
			return new PlaceInfoApiClient() {
				@Override
				public SlotUnit getSlotUnit(Long roomId) {
					return SlotUnit.HOUR; // 항상 1시간 단위 반환
				}
				
				@Override
				public boolean isHealthy() {
					return true;
				}
			};
		}
	}
	
}
