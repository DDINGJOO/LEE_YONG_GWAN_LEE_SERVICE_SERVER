package com.teambind.springproject.room.service.impl;

import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import com.teambind.springproject.room.repository.RoomTimeSlotRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * TimeSlotQueryServiceImpl 테스트.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("TimeSlotQueryService 테스트")
class TimeSlotQueryServiceImplTest {
	
	@Mock
	private RoomTimeSlotRepository slotRepository;
	
	@InjectMocks
	private TimeSlotQueryServiceImpl queryService;
	
	private Long roomId;
	private LocalDate testDate;
	private LocalTime testTime;
	
	@BeforeEach
	void setUp() {
		roomId = 100L;
		testDate = LocalDate.of(2025, 11, 5);
		testTime = LocalTime.of(9, 0);
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
		log.info("[Given] - startDate: {} (시작일)", startDate);
		log.info("[Given] - endDate: {} (종료일)", endDate);
		log.info("[Given] - 기간: {}일", java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1);
		
		List<RoomTimeSlot> expectedSlots = List.of(
				RoomTimeSlot.available(roomId, LocalDate.of(2025, 11, 5), LocalTime.of(9, 0)),
				RoomTimeSlot.available(roomId, LocalDate.of(2025, 11, 6), LocalTime.of(9, 0)),
				RoomTimeSlot.available(roomId, LocalDate.of(2025, 11, 7), LocalTime.of(9, 0))
		);
		log.info("[Given] - Mock 설정: 3개의 슬롯 반환 (11/5, 11/6, 11/7 각 9시)");
		
		given(slotRepository.findByRoomIdAndSlotDateBetween(roomId, startDate, endDate))
				.willReturn(expectedSlots);
		
		// When
		log.info("[When] queryService.getSlotsByDateRange() 호출");
		log.info("[When] - 파라미터: roomId={}, startDate={}, endDate={}", roomId, startDate, endDate);
		List<RoomTimeSlot> result = queryService.getSlotsByDateRange(roomId, startDate, endDate);
		log.info("[When] - 반환 값: {} 개의 슬롯 조회됨", result.size());
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 조회된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 3개");
		log.info("[Then] - 실제(Actual): {}개", result.size());
		assertThat(result).hasSize(3);
		log.info("[Then] - ✓ 슬롯 개수 일치");
		
		log.info("[Then] [검증2] 조회된 슬롯 목록 내용");
		log.info("[Then] - 예상(Expected): expectedSlots와 동일");
		log.info("[Then] - 실제(Actual): result 목록");
		assertThat(result).isEqualTo(expectedSlots);
		log.info("[Then] - ✓ 슬롯 목록 내용 일치");
		
		log.info("[Then] [검증3] Repository 메소드 호출 확인");
		verify(slotRepository).findByRoomIdAndSlotDateBetween(roomId, startDate, endDate);
		log.info("[Then] - ✓ findByRoomIdAndSlotDateBetween 메소드가 올바른 파라미터로 호출됨");
		
		log.info("=== [날짜 범위로 슬롯 목록을 조회한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("특정 날짜의 사용 가능한 슬롯 목록을 조회한다")
	void getAvailableSlots() {
		log.info("=== [특정 날짜의 사용 가능한 슬롯 목록을 조회한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - testDate: {}", testDate);
		log.info("[Given] - 조회할 상태: AVAILABLE");
		
		List<RoomTimeSlot> availableSlots = List.of(
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(9, 0)),
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(10, 0))
		);
		log.info("[Given] - Mock 설정: 2개의 사용 가능한 슬롯 반환 (9시, 10시)");
		
		given(slotRepository.findByRoomIdAndSlotDateAndStatus(roomId, testDate, SlotStatus.AVAILABLE))
				.willReturn(availableSlots);
		
		// When
		log.info("[When] queryService.getAvailableSlots() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}", roomId, testDate);
		List<RoomTimeSlot> result = queryService.getAvailableSlots(roomId, testDate);
		log.info("[When] - 반환 값: {} 개의 사용 가능한 슬롯 조회됨", result.size());
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 조회된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 2개");
		log.info("[Then] - 실제(Actual): {}개", result.size());
		assertThat(result).hasSize(2);
		log.info("[Then] - ✓ 슬롯 개수 일치");
		
		log.info("[Then] [검증2] 첫 번째 슬롯 시간");
		log.info("[Then] - 예상(Expected): 09:00");
		log.info("[Then] - 실제(Actual): {}", result.get(0).getSlotTime());
		assertThat(result.get(0).getSlotTime()).isEqualTo(LocalTime.of(9, 0));
		log.info("[Then] - ✓ 첫 번째 슬롯 시간 일치");
		
		log.info("[Then] [검증3] 두 번째 슬롯 시간");
		log.info("[Then] - 예상(Expected): 10:00");
		log.info("[Then] - 실제(Actual): {}", result.get(1).getSlotTime());
		assertThat(result.get(1).getSlotTime()).isEqualTo(LocalTime.of(10, 0));
		log.info("[Then] - ✓ 두 번째 슬롯 시간 일치");
		
		log.info("[Then] [검증4] Repository 메소드 호출 확인");
		verify(slotRepository).findByRoomIdAndSlotDateAndStatus(roomId, testDate, SlotStatus.AVAILABLE);
		log.info("[Then] - ✓ findByRoomIdAndSlotDateAndStatus 메소드가 올바른 파라미터로 호출됨");
		
		log.info("=== [특정 날짜의 사용 가능한 슬롯 목록을 조회한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("슬롯이 사용 가능한지 확인한다 - 사용 가능한 경우")
	void isSlotAvailable_Available() {
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 사용 가능한 경우] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - testDate: {}", testDate);
		log.info("[Given] - testTime: {}시", testTime.getHour());
		RoomTimeSlot availableSlot = RoomTimeSlot.available(roomId, testDate, testTime);
		log.info("[Given] - 슬롯 상태: AVAILABLE");
		log.info("[Given] - Mock 설정: 사용 가능한 슬롯 반환");
		
		given(slotRepository.findByRoomIdAndSlotDateAndSlotTime(roomId, testDate, testTime))
				.willReturn(Optional.of(availableSlot));
		
		// When
		log.info("[When] queryService.isSlotAvailable() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}, testTime={}시", roomId, testDate, testTime.getHour());
		boolean result = queryService.isSlotAvailable(roomId, testDate, testTime);
		log.info("[When] - 반환 값: {}", result);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 슬롯 사용 가능 여부");
		log.info("[Then] - 예상(Expected): true (사용 가능)");
		log.info("[Then] - 실제(Actual): {}", result);
		assertThat(result).isTrue();
		log.info("[Then] - ✓ 슬롯이 사용 가능함");
		
		log.info("[Then] [검증2] Repository 메소드 호출 확인");
		verify(slotRepository).findByRoomIdAndSlotDateAndSlotTime(roomId, testDate, testTime);
		log.info("[Then] - ✓ findByRoomIdAndSlotDateAndSlotTime 메소드가 올바른 파라미터로 호출됨");
		
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 사용 가능한 경우] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("슬롯이 사용 가능한지 확인한다 - 예약된 경우")
	void isSlotAvailable_Reserved() {
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 예약된 경우] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - testDate: {}", testDate);
		log.info("[Given] - testTime: {}시", testTime.getHour());
		RoomTimeSlot reservedSlot = RoomTimeSlot.available(roomId, testDate, testTime);
		reservedSlot.markAsPending(200L);
		reservedSlot.confirm();
		log.info("[Given] - 슬롯 상태: AVAILABLE -> PENDING -> RESERVED");
		log.info("[Given] - reservationId: 200");
		log.info("[Given] - Mock 설정: 예약된 슬롯 반환");
		
		given(slotRepository.findByRoomIdAndSlotDateAndSlotTime(roomId, testDate, testTime))
				.willReturn(Optional.of(reservedSlot));
		
		// When
		log.info("[When] queryService.isSlotAvailable() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}, testTime={}시", roomId, testDate, testTime.getHour());
		boolean result = queryService.isSlotAvailable(roomId, testDate, testTime);
		log.info("[When] - 반환 값: {}", result);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 슬롯 사용 가능 여부");
		log.info("[Then] - 예상(Expected): false (사용 불가능 - 예약됨)");
		log.info("[Then] - 실제(Actual): {}", result);
		assertThat(result).isFalse();
		log.info("[Then] - ✓ 예약된 슬롯은 사용 불가능함");
		
		log.info("[Then] [검증2] Repository 메소드 호출 확인");
		verify(slotRepository).findByRoomIdAndSlotDateAndSlotTime(roomId, testDate, testTime);
		log.info("[Then] - ✓ findByRoomIdAndSlotDateAndSlotTime 메소드가 올바른 파라미터로 호출됨");
		
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 예약된 경우] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("슬롯이 사용 가능한지 확인한다 - 슬롯이 없는 경우")
	void isSlotAvailable_NotFound() {
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 슬롯이 없는 경우] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - testDate: {}", testDate);
		log.info("[Given] - testTime: {}시", testTime.getHour());
		log.info("[Given] - Mock 설정: 슬롯이 없음 (Optional.empty)");
		
		given(slotRepository.findByRoomIdAndSlotDateAndSlotTime(roomId, testDate, testTime))
				.willReturn(Optional.empty());
		
		// When
		log.info("[When] queryService.isSlotAvailable() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}, testTime={}시", roomId, testDate, testTime.getHour());
		boolean result = queryService.isSlotAvailable(roomId, testDate, testTime);
		log.info("[When] - 반환 값: {}", result);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 슬롯 사용 가능 여부");
		log.info("[Then] - 예상(Expected): false (사용 불가능 - 슬롯 없음)");
		log.info("[Then] - 실제(Actual): {}", result);
		assertThat(result).isFalse();
		log.info("[Then] - ✓ 슬롯이 없으면 사용 불가능함");
		
		log.info("[Then] [검증2] Repository 메소드 호출 확인");
		verify(slotRepository).findByRoomIdAndSlotDateAndSlotTime(roomId, testDate, testTime);
		log.info("[Then] - ✓ findByRoomIdAndSlotDateAndSlotTime 메소드가 올바른 파라미터로 호출됨");
		
		log.info("=== [슬롯이 사용 가능한지 확인한다 - 슬롯이 없는 경우] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("날짜 범위 내 사용 가능한 슬롯 개수를 세어 반환한다")
	void countAvailableSlots() {
		log.info("=== [날짜 범위 내 사용 가능한 슬롯 개수를 세어 반환한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		LocalDate startDate = LocalDate.of(2025, 11, 5);
		LocalDate endDate = LocalDate.of(2025, 11, 10);
		long expectedCount = 30L;
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - startDate: {} (시작일)", startDate);
		log.info("[Given] - endDate: {} (종료일)", endDate);
		log.info("[Given] - 기간: {}일", java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1);
		log.info("[Given] - 조회할 상태: AVAILABLE");
		log.info("[Given] - Mock 설정: 30개의 사용 가능한 슬롯 반환");
		
		given(slotRepository.countByRoomIdAndDateRangeAndStatus(
				roomId, startDate, endDate, SlotStatus.AVAILABLE
		)).willReturn(expectedCount);
		
		// When
		log.info("[When] queryService.countAvailableSlots() 호출");
		log.info("[When] - 파라미터: roomId={}, startDate={}, endDate={}", roomId, startDate, endDate);
		long result = queryService.countAvailableSlots(roomId, startDate, endDate);
		log.info("[When] - 반환 값: {} 개의 사용 가능한 슬롯", result);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 사용 가능한 슬롯 개수");
		log.info("[Then] - 예상(Expected): 30개");
		log.info("[Then] - 실제(Actual): {}개", result);
		assertThat(result).isEqualTo(30L);
		log.info("[Then] - ✓ 슬롯 개수 일치");
		
		log.info("[Then] [검증2] Repository 메소드 호출 확인");
		verify(slotRepository).countByRoomIdAndDateRangeAndStatus(
				roomId, startDate, endDate, SlotStatus.AVAILABLE
		);
		log.info("[Then] - ✓ countByRoomIdAndDateRangeAndStatus 메소드가 올바른 파라미터로 호출됨");
		
		log.info("=== [날짜 범위 내 사용 가능한 슬롯 개수를 세어 반환한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("특정 날짜의 모든 슬롯을 조회한다")
	void getAllSlotsForDate() {
		log.info("=== [특정 날짜의 모든 슬롯을 조회한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - testDate: {}", testDate);
		RoomTimeSlot slot1 = RoomTimeSlot.available(roomId, testDate, LocalTime.of(9, 0));
		RoomTimeSlot slot2 = RoomTimeSlot.available(roomId, testDate, LocalTime.of(10, 0));
		RoomTimeSlot slot3 = RoomTimeSlot.available(roomId, testDate, LocalTime.of(11, 0));
		
		slot2.markAsPending(200L); // 두 번째 슬롯은 예약 중
		log.info("[Given] - 슬롯 구성: 9시(AVAILABLE), 10시(PENDING), 11시(AVAILABLE)");
		log.info("[Given] - 10시 슬롯 reservationId: 200");
		
		List<RoomTimeSlot> allSlots = List.of(slot1, slot2, slot3);
		log.info("[Given] - Mock 설정: 3개의 슬롯 반환 (상태 혼합)");
		
		given(slotRepository.findByRoomIdAndSlotDateBetween(roomId, testDate, testDate))
				.willReturn(allSlots);
		
		// When
		log.info("[When] queryService.getAllSlotsForDate() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}", roomId, testDate);
		List<RoomTimeSlot> result = queryService.getAllSlotsForDate(roomId, testDate);
		log.info("[When] - 반환 값: {} 개의 슬롯 조회됨", result.size());
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 조회된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 3개");
		log.info("[Then] - 실제(Actual): {}개", result.size());
		assertThat(result).hasSize(3);
		log.info("[Then] - ✓ 슬롯 개수 일치");
		
		log.info("[Then] [검증2] 첫 번째 슬롯(9시) 상태");
		log.info("[Then] - 예상(Expected): AVAILABLE");
		log.info("[Then] - 실제(Actual): {}", result.get(0).getStatus());
		assertThat(result.get(0).getStatus()).isEqualTo(SlotStatus.AVAILABLE);
		log.info("[Then] - ✓ 첫 번째 슬롯 상태 일치");
		
		log.info("[Then] [검증3] 두 번째 슬롯(10시) 상태");
		log.info("[Then] - 예상(Expected): PENDING");
		log.info("[Then] - 실제(Actual): {}", result.get(1).getStatus());
		assertThat(result.get(1).getStatus()).isEqualTo(SlotStatus.PENDING);
		log.info("[Then] - ✓ 두 번째 슬롯 상태 일치");
		
		log.info("[Then] [검증4] 세 번째 슬롯(11시) 상태");
		log.info("[Then] - 예상(Expected): AVAILABLE");
		log.info("[Then] - 실제(Actual): {}", result.get(2).getStatus());
		assertThat(result.get(2).getStatus()).isEqualTo(SlotStatus.AVAILABLE);
		log.info("[Then] - ✓ 세 번째 슬롯 상태 일치");
		
		log.info("[Then] [검증5] Repository 메소드 호출 확인");
		verify(slotRepository).findByRoomIdAndSlotDateBetween(roomId, testDate, testDate);
		log.info("[Then] - ✓ findByRoomIdAndSlotDateBetween 메소드가 올바른 파라미터로 호출됨");
		
		log.info("=== [특정 날짜의 모든 슬롯을 조회한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("특정 상태의 슬롯 목록을 조회한다")
	void getSlotsByStatus() {
		log.info("=== [특정 상태의 슬롯 목록을 조회한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		SlotStatus targetStatus = SlotStatus.RESERVED;
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - testDate: {}", testDate);
		log.info("[Given] - targetStatus: {}", targetStatus);
		
		List<RoomTimeSlot> reservedSlots = List.of(
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(9, 0)),
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(10, 0))
		);
		log.info("[Given] - Mock 설정: 2개의 RESERVED 상태 슬롯 반환 (9시, 10시)");
		
		given(slotRepository.findByRoomIdAndSlotDateAndStatus(roomId, testDate, targetStatus))
				.willReturn(reservedSlots);
		
		// When
		log.info("[When] queryService.getSlotsByStatus() 호출");
		log.info("[When] - 파라미터: roomId={}, testDate={}, status={}", roomId, testDate, targetStatus);
		List<RoomTimeSlot> result = queryService.getSlotsByStatus(roomId, testDate, targetStatus);
		log.info("[When] - 반환 값: {} 개의 {} 상태 슬롯 조회됨", result.size(), targetStatus);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 조회된 슬롯 개수");
		log.info("[Then] - 예상(Expected): 2개");
		log.info("[Then] - 실제(Actual): {}개", result.size());
		assertThat(result).hasSize(2);
		log.info("[Then] - ✓ 슬롯 개수 일치");
		
		log.info("[Then] [검증2] Repository 메소드 호출 확인");
		verify(slotRepository).findByRoomIdAndSlotDateAndStatus(roomId, testDate, targetStatus);
		log.info("[Then] - ✓ findByRoomIdAndSlotDateAndStatus 메소드가 올바른 파라미터로 호출됨");
		
		log.info("=== [특정 상태의 슬롯 목록을 조회한다] 테스트 성공 ===");
	}
	
}
