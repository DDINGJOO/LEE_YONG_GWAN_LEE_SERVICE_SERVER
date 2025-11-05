package com.teambind.springproject.room.service.impl;

import com.teambind.springproject.common.exceptions.application.SlotGenerationFailedException;
import com.teambind.springproject.room.entity.RoomOperatingPolicy;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.RecurrencePattern;
import com.teambind.springproject.room.entity.enums.SlotUnit;
import com.teambind.springproject.room.entity.vo.WeeklySlotSchedule;
import com.teambind.springproject.room.repository.RoomOperatingPolicyRepository;
import com.teambind.springproject.room.repository.RoomTimeSlotRepository;
import com.teambind.springproject.room.service.PlaceInfoApiClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * TimeSlotGenerationServiceImpl 테스트.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("TimeSlotGenerationService 테스트")
class TimeSlotGenerationServiceImplTest {
	
	@Mock
	private RoomTimeSlotRepository slotRepository;
	
	@Mock
	private RoomOperatingPolicyRepository policyRepository;
	
	@Mock
	private PlaceInfoApiClient placeInfoApiClient;
	
	@InjectMocks
	private TimeSlotGenerationServiceImpl generationService;
	
	private Long roomId;
	private RoomOperatingPolicy policy;
	
	@BeforeEach
	void setUp() {
		roomId = 100L;
		
		// 기본 정책 설정: 매주 월~금 9시, 10시, 11시에 시작
		List<com.teambind.springproject.space.entity.vo.WeeklySlotTime> slotTimes = List.of(
				com.teambind.springproject.space.entity.vo.WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
				com.teambind.springproject.space.entity.vo.WeeklySlotTime.of(DayOfWeek.TUESDAY, LocalTime.of(9, 0)),
				com.teambind.springproject.space.entity.vo.WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0)),
				com.teambind.springproject.space.entity.vo.WeeklySlotTime.of(DayOfWeek.THURSDAY, LocalTime.of(9, 0)),
				com.teambind.springproject.space.entity.vo.WeeklySlotTime.of(DayOfWeek.FRIDAY, LocalTime.of(9, 0))
		);
		
		WeeklySlotSchedule schedule = WeeklySlotSchedule.of(slotTimes);
		
		policy = RoomOperatingPolicy.create(
				roomId,
				schedule,
				RecurrencePattern.EVERY_WEEK,
				List.of() // 빈 리스트
		);
	}
	
	@Test
	@DisplayName("특정 날짜에 대해 슬롯을 생성한다")
	void generateSlotsForDate() {
		log.info("=== [특정 날짜에 대해 슬롯을 생성한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate testDate = LocalDate.of(2025, 11, 5); // 수요일
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - testDate: {} (수요일)", testDate);
		log.info("[Given] - 정책: 월~금 9시 슬롯");
		
		List<RoomTimeSlot> generatedSlots = List.of(
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(9, 0)),
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(10, 0)),
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(11, 0))
		);
		log.info("[Given] - Mock 설정: 3개의 슬롯 반환 (9시, 10시, 11시)");
		
		given(policyRepository.findByRoomId(roomId)).willReturn(Optional.of(policy));
		given(placeInfoApiClient.getSlotUnit(roomId)).willReturn(SlotUnit.HOUR);
		given(slotRepository.saveAll(anyList())).willReturn(generatedSlots);
		
		// When
		log.info("[When] generationService.generateSlotsForDate() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}", roomId, testDate);
		int count = generationService.generateSlotsForDate(roomId, testDate);
		log.info("[When] - 반환 값: {} 개의 슬롯 생성됨", count);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 생성된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 3개");
		log.info("[Then] - 실제(Actual): {}개", count);
		assertThat(count).isEqualTo(3);
		log.info("[Then] - ✓ 슬롯 개수 일치");
		
		log.info("[Then] [검증2] 의존성 메소드 호출 확인");
		log.info("[Then] - policyRepository.findByRoomId({}) 호출 확인", roomId);
		verify(policyRepository).findByRoomId(roomId);
		log.info("[Then] - placeInfoApiClient.getSlotUnit({}) 호출 확인", roomId);
		verify(placeInfoApiClient).getSlotUnit(roomId);
		log.info("[Then] - slotRepository.saveAll() 호출 확인");
		verify(slotRepository).saveAll(anyList());
		log.info("[Then] - ✓ 모든 의존성 메소드가 올바르게 호출됨");
		
		log.info("=== [특정 날짜에 대해 슬롯을 생성한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("정책이 없으면 예외가 발생한다")
	void generateSlotsForDate_PolicyNotFound() {
		log.info("=== [정책이 없으면 예외가 발생한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate testDate = LocalDate.of(2025, 11, 5);
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - testDate: {}", testDate);
		log.info("[Given] - Mock 설정: policyRepository가 빈 Optional 반환 (정책 없음)");
		given(policyRepository.findByRoomId(roomId)).willReturn(Optional.empty());
		
		// When & Then
		log.info("[When & Then] 예외 발생 검증");
		log.info("[When] generationService.generateSlotsForDate() 호출");
		log.info("[Then] - 예상(Expected): SlotGenerationFailedException 발생 (메시지에 '2025-11-05' 포함)");
		// PolicyNotFoundException이 catch되어 SlotGenerationFailedException으로 래핑됨
		assertThatThrownBy(() ->
				generationService.generateSlotsForDate(roomId, testDate)
		).isInstanceOf(SlotGenerationFailedException.class)
				.hasMessageContaining("2025-11-05");
		log.info("[Then] - 실제(Actual): SlotGenerationFailedException 발생");
		log.info("[Then] - ✓ 정책이 없을 때 예외 발생 확인");
		
		log.info("[Then] [검증2] 의존성 메소드가 호출되지 않았는지 확인");
		log.info("[Then] - placeInfoApiClient.getSlotUnit()이 호출되지 않아야 함");
		verify(placeInfoApiClient, never()).getSlotUnit(any());
		log.info("[Then] - slotRepository.saveAll()이 호출되지 않아야 함");
		verify(slotRepository, never()).saveAll(anyList());
		log.info("[Then] - ✓ 정책이 없으면 후속 처리가 실행되지 않음");
		
		log.info("=== [정책이 없으면 예외가 발생한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("날짜 범위에 대해 슬롯을 생성한다")
	void generateSlotsForDateRange() {
		log.info("=== [날짜 범위에 대해 슬롯을 생성한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate startDate = LocalDate.of(2025, 11, 5);
		LocalDate endDate = LocalDate.of(2025, 11, 7); // 3일
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - 날짜 범위: {} ~ {} (3일)", startDate, endDate);
		
		List<RoomTimeSlot> dailySlots = List.of(
				RoomTimeSlot.available(roomId, startDate, LocalTime.of(9, 0)),
				RoomTimeSlot.available(roomId, startDate, LocalTime.of(10, 0))
		);
		log.info("[Given] - Mock 설정: 각 날짜마다 2개 슬롯 반환");
		
		given(policyRepository.findByRoomId(roomId)).willReturn(Optional.of(policy));
		given(placeInfoApiClient.getSlotUnit(roomId)).willReturn(SlotUnit.HOUR);
		given(slotRepository.saveAll(anyList())).willReturn(dailySlots);
		
		// When
		log.info("[When] generationService.generateSlotsForDateRange() 호출");
		log.info("[When] - 파라미터: roomId={}, startDate={}, endDate={}", roomId, startDate, endDate);
		int totalCount = generationService.generateSlotsForDateRange(roomId, startDate, endDate);
		log.info("[When] - 반환 값: {} 개의 슬롯 생성됨", totalCount);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 전체 생성된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 6개 (3일 × 2슬롯)");
		log.info("[Then] - 실제(Actual): {}개", totalCount);
		assertThat(totalCount).isEqualTo(6); // 3일 × 2슬롯
		log.info("[Then] - ✓ 전체 슬롯 개수 일치");
		
		log.info("[Then] [검증2] saveAll 호출 횟수");
		log.info("[Then] - 예상(Expected): 3회 (각 날짜마다 1회)");
		verify(slotRepository, times(3)).saveAll(anyList());
		log.info("[Then] - 실제(Actual): 3회 호출됨");
		log.info("[Then] - ✓ 날짜 개수만큼 saveAll 호출됨");
		
		log.info("=== [날짜 범위에 대해 슬롯을 생성한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("모든 룸에 대해 슬롯을 생성한다")
	void generateSlotsForAllRooms() {
		log.info("=== [모든 룸에 대해 슬롯을 생성한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate testDate = LocalDate.of(2025, 11, 5); // 수요일
		log.info("[Given] - testDate: {} (수요일)", testDate);
		
		List<com.teambind.springproject.space.entity.vo.WeeklySlotTime> times1 = List.of(
				com.teambind.springproject.space.entity.vo.WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0))
		);
		
		List<com.teambind.springproject.space.entity.vo.WeeklySlotTime> times2 = List.of(
				com.teambind.springproject.space.entity.vo.WeeklySlotTime.of(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0))
		);
		
		RoomOperatingPolicy policy1 = RoomOperatingPolicy.create(
				100L,
				WeeklySlotSchedule.of(times1),
				RecurrencePattern.EVERY_WEEK,
				List.of() // 빈 리스트
		);
		
		RoomOperatingPolicy policy2 = RoomOperatingPolicy.create(
				200L,
				WeeklySlotSchedule.of(times2),
				RecurrencePattern.EVERY_WEEK,
				List.of() // 빈 리스트
		);
		
		List<RoomOperatingPolicy> policies = List.of(policy1, policy2);
		log.info("[Given] - 정책 개수: {}개 (Room 100L, Room 200L)", policies.size());
		log.info("[Given] - Room 100L: 수요일 9시 슬롯");
		log.info("[Given] - Room 200L: 수요일 10시 슬롯");
		
		List<RoomTimeSlot> slots1 = List.of(
				RoomTimeSlot.available(100L, testDate, LocalTime.of(9, 0))
		);
		
		List<RoomTimeSlot> slots2 = List.of(
				RoomTimeSlot.available(200L, testDate, LocalTime.of(10, 0))
		);
		
		given(policyRepository.findAll()).willReturn(policies);
		given(policyRepository.findByRoomId(100L)).willReturn(Optional.of(policy1));
		given(policyRepository.findByRoomId(200L)).willReturn(Optional.of(policy2));
		given(placeInfoApiClient.getSlotUnit(any())).willReturn(SlotUnit.HOUR);
		given(slotRepository.saveAll(anyList()))
				.willReturn(slots1)
				.willReturn(slots2);
		
		// When
		log.info("[When] generationService.generateSlotsForAllRooms() 호출");
		log.info("[When] - 파라미터: testDate={}", testDate);
		int totalCount = generationService.generateSlotsForAllRooms(testDate);
		log.info("[When] - 반환 값: {} 개의 슬롯 생성됨", totalCount);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 전체 생성된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 2개 (2룸 × 1슬롯)");
		log.info("[Then] - 실제(Actual): {}개", totalCount);
		assertThat(totalCount).isEqualTo(2); // 2룸 × 1슬롯
		log.info("[Then] - ✓ 전체 슬롯 개수 일치");
		
		log.info("[Then] [검증2] 의존성 메소드 호출 확인");
		log.info("[Then] - policyRepository.findAll() 호출 확인");
		verify(policyRepository).findAll();
		log.info("[Then] - saveAll 호출 횟수: 2회 (각 룸마다 1회)");
		verify(slotRepository, times(2)).saveAll(anyList());
		log.info("[Then] - ✓ 모든 룸에 대해 슬롯 생성됨");
		
		log.info("=== [모든 룸에 대해 슬롯을 생성한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("어제 슬롯을 삭제한다")
	void deleteYesterdaySlots() {
		log.info("=== [어제 슬롯을 삭제한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);
		int deletedCount = 10;
		log.info("[Given] - 오늘: {}", today);
		log.info("[Given] - 어제: {}", yesterday);
		log.info("[Given] - Mock 설정: 삭제된 슬롯 개수 = {}", deletedCount);
		
		given(slotRepository.deleteBySlotDateBefore(today)).willReturn(deletedCount);
		
		// When
		log.info("[When] generationService.deleteYesterdaySlots() 호출");
		int result = generationService.deleteYesterdaySlots();
		log.info("[When] - 반환 값: {} 개의 슬롯 삭제됨", result);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 삭제된 슬롯 개수");
		log.info("[Then] - 예상(Expected): {}개", deletedCount);
		log.info("[Then] - 실제(Actual): {}개", result);
		assertThat(result).isEqualTo(10);
		log.info("[Then] - ✓ 삭제된 슬롯 개수 일치");
		
		log.info("[Then] [검증2] deleteBySlotDateBefore 호출 확인");
		log.info("[Then] - 파라미터: today={}", today);
		verify(slotRepository).deleteBySlotDateBefore(today);
		log.info("[Then] - ✓ 올바른 날짜로 삭제 메소드 호출됨");
		
		log.info("=== [어제 슬롯을 삭제한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("특정 날짜 이전의 슬롯을 삭제한다")
	void deleteSlotsBeforeDate() {
		log.info("=== [특정 날짜 이전의 슬롯을 삭제한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate beforeDate = LocalDate.of(2025, 11, 1);
		int deletedCount = 50;
		log.info("[Given] - 기준 날짜: {} (이 날짜 이전 슬롯 삭제)", beforeDate);
		log.info("[Given] - Mock 설정: 삭제된 슬롯 개수 = {}", deletedCount);
		
		given(slotRepository.deleteBySlotDateBefore(beforeDate)).willReturn(deletedCount);
		
		// When
		log.info("[When] generationService.deleteSlotsBeforeDate() 호출");
		log.info("[When] - 파라미터: beforeDate={}", beforeDate);
		int result = generationService.deleteSlotsBeforeDate(beforeDate);
		log.info("[When] - 반환 값: {} 개의 슬롯 삭제됨", result);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 삭제된 슬롯 개수");
		log.info("[Then] - 예상(Expected): {}개", deletedCount);
		log.info("[Then] - 실제(Actual): {}개", result);
		assertThat(result).isEqualTo(50);
		log.info("[Then] - ✓ 삭제된 슬롯 개수 일치");
		
		log.info("[Then] [검증2] deleteBySlotDateBefore 호출 확인");
		log.info("[Then] - 파라미터: beforeDate={}", beforeDate);
		verify(slotRepository).deleteBySlotDateBefore(beforeDate);
		log.info("[Then] - ✓ 올바른 날짜로 삭제 메소드 호출됨");
		
		log.info("=== [특정 날짜 이전의 슬롯을 삭제한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("미래 슬롯을 재생성한다")
	void regenerateFutureSlots() {
		log.info("=== [미래 슬롯을 재생성한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate today = LocalDate.of(2025, 11, 5);
		LocalDate endDate = today.plusDays(60);
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - 재생성 범위: {} ~ {} (60일)", today, endDate);
		
		// 기존 미래 슬롯
		List<RoomTimeSlot> existingFutureSlots = List.of(
				RoomTimeSlot.available(roomId, today, LocalTime.of(9, 0)),
				RoomTimeSlot.available(roomId, today.plusDays(1), LocalTime.of(9, 0))
		);
		log.info("[Given] - 기존 미래 슬롯: {}개", existingFutureSlots.size());
		
		// 새로 생성될 슬롯
		List<RoomTimeSlot> newSlots = List.of(
				RoomTimeSlot.available(roomId, today, LocalTime.of(9, 0)),
				RoomTimeSlot.available(roomId, today, LocalTime.of(10, 0))
		);
		log.info("[Given] - Mock 설정: 새로 생성될 슬롯 {}개", newSlots.size());
		
		given(slotRepository.findAll()).willReturn(existingFutureSlots);
		given(policyRepository.findByRoomId(roomId)).willReturn(Optional.of(policy));
		given(placeInfoApiClient.getSlotUnit(roomId)).willReturn(SlotUnit.HOUR);
		given(slotRepository.saveAll(anyList())).willReturn(newSlots);
		
		// When
		log.info("[When] generationService.regenerateFutureSlots() 호출");
		log.info("[When] - 파라미터: roomId={}", roomId);
		int regeneratedCount = generationService.regenerateFutureSlots(roomId);
		log.info("[When] - 반환 값: {} 개의 슬롯 재생성됨", regeneratedCount);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 재생성된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 0개 초과");
		log.info("[Then] - 실제(Actual): {}개", regeneratedCount);
		assertThat(regeneratedCount).isGreaterThan(0);
		log.info("[Then] - ✓ 슬롯이 재생성됨");
		
		log.info("[Then] [검증2] 기존 슬롯 삭제 확인");
		verify(slotRepository).deleteAll(anyList()); // 기존 슬롯 삭제
		log.info("[Then] - ✓ 기존 미래 슬롯이 삭제됨");
		
		log.info("[Then] [검증3] 새 슬롯 저장 확인");
		verify(slotRepository, atLeastOnce()).saveAll(anyList()); // 새 슬롯 저장
		log.info("[Then] - ✓ 새 슬롯이 생성됨");
		
		log.info("=== [미래 슬롯을 재생성한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("미래 슬롯 재생성 시 예외가 발생하면 SlotGenerationFailedException을 던진다")
	void regenerateFutureSlots_Exception() {
		log.info("=== [미래 슬롯 재생성 시 예외가 발생하면 SlotGenerationFailedException을 던진다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - Mock 설정: slotRepository.findAll()에서 RuntimeException 발생");
		given(slotRepository.findAll()).willThrow(new RuntimeException("Database error"));
		
		// When & Then
		log.info("[When & Then] 예외 발생 검증");
		log.info("[When] generationService.regenerateFutureSlots() 호출");
		log.info("[Then] - 예상(Expected): SlotGenerationFailedException 발생");
		assertThatThrownBy(() ->
				generationService.regenerateFutureSlots(roomId)
		).isInstanceOf(SlotGenerationFailedException.class);
		log.info("[Then] - 실제(Actual): SlotGenerationFailedException 발생");
		log.info("[Then] - ✓ RuntimeException이 SlotGenerationFailedException으로 래핑됨");
		
		log.info("=== [미래 슬롯 재생성 시 예외가 발생하면 SlotGenerationFailedException을 던진다] 테스트 성공 ===");
	}
	
}
