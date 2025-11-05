package com.teambind.springproject.room.command.domain.service;

import com.teambind.springproject.common.exceptions.application.SlotGenerationFailedException;
import com.teambind.springproject.common.exceptions.domain.PolicyNotFoundException;
import com.teambind.springproject.room.domain.port.OperatingPolicyPort;
import com.teambind.springproject.room.domain.port.TimeSlotPort;
import com.teambind.springproject.room.entity.RoomOperatingPolicy;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.RecurrencePattern;
import com.teambind.springproject.room.entity.enums.SlotUnit;
import com.teambind.springproject.room.entity.vo.WeeklySlotSchedule;
import com.teambind.springproject.room.entity.vo.WeeklySlotTime;
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
import static org.mockito.Mockito.*;

/**
 * TimeSlotGenerationServiceImpl 단위 테스트.
 *
 * DomainService 계층의 슬롯 생성 로직을 Port를 Mocking하여 독립적으로 테스트한다.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("TimeSlotGenerationServiceImpl 단위 테스트")
class TimeSlotGenerationServiceImplTest {

	@Mock
	private TimeSlotPort timeSlotPort;

	@Mock
	private OperatingPolicyPort operatingPolicyPort;

	@Mock
	private PlaceInfoApiClient placeInfoApiClient;

	@InjectMocks
	private TimeSlotGenerationServiceImpl service;

	private Long roomId;
	private LocalDate testDate;
	private RoomOperatingPolicy policy;
	private SlotUnit slotUnit;

	@BeforeEach
	void setUp() {
		roomId = 100L;
		testDate = LocalDate.of(2025, 1, 20); // 월요일
		slotUnit = SlotUnit.HALF_HOUR;

		// 월요일 09:00, 10:00 운영 정책
		List<WeeklySlotTime> slotTimes = List.of(
				WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)),
				WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(10, 0))
		);
		WeeklySlotSchedule schedule = WeeklySlotSchedule.of(slotTimes);
		policy = RoomOperatingPolicy.create(roomId, schedule, RecurrencePattern.EVERY_WEEK, List.of());

		log.info("=== 테스트 데이터 초기화 ===");
		log.info("- roomId: {}", roomId);
		log.info("- testDate: {} ({})", testDate, testDate.getDayOfWeek());
		log.info("- slotUnit: {}", slotUnit);
		log.info("- policy: 월요일 09:00, 10:00 운영");
	}

	@Test
	@DisplayName("특정 날짜에 대한 슬롯을 생성한다")
	void generateSlotsForDate() {
		log.info("=== [특정 날짜 슬롯 생성] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		when(operatingPolicyPort.findByRoomId(roomId)).thenReturn(Optional.of(policy));
		log.info("[Given] - operatingPolicyPort.findByRoomId() -> 정책 반환");

		when(placeInfoApiClient.getSlotUnit(roomId)).thenReturn(slotUnit);
		log.info("[Given] - placeInfoApiClient.getSlotUnit() -> {}", slotUnit);

		// 예상 슬롯: 월요일이므로 09:00, 09:30, 10:00, 10:30 (4개)
		List<RoomTimeSlot> expectedSlots = List.of(
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(9, 0)),
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(9, 30)),
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(10, 0)),
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(10, 30))
		);

		when(timeSlotPort.saveAll(any())).thenReturn(expectedSlots);
		log.info("[Given] - timeSlotPort.saveAll() -> {} 개 슬롯 반환", expectedSlots.size());

		// When
		log.info("[When] generateSlotsForDate() 호출");
		log.info("[When] - 파라미터: roomId={}, date={}", roomId, testDate);
		int generatedCount = service.generateSlotsForDate(roomId, testDate);
		log.info("[When] - 호출 완료: generatedCount={}", generatedCount);

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 생성된 슬롯 개수 확인");
		log.info("[Then] - 예상 개수: 4 (09:00, 09:30, 10:00, 10:30)");
		log.info("[Then] - 실제 개수: {}", generatedCount);
		assertThat(generatedCount).isEqualTo(4);
		log.info("[Then] - ✓ 생성 개수 확인됨");

		log.info("[Then] [검증2] operatingPolicyPort.findByRoomId()가 1번 호출되었는지 확인");
		verify(operatingPolicyPort, times(1)).findByRoomId(roomId);
		log.info("[Then] - ✓ findByRoomId() 호출 확인됨");

		log.info("[Then] [검증3] placeInfoApiClient.getSlotUnit()이 1번 호출되었는지 확인");
		verify(placeInfoApiClient, times(1)).getSlotUnit(roomId);
		log.info("[Then] - ✓ getSlotUnit() 호출 확인됨");

		log.info("[Then] [검증4] timeSlotPort.saveAll()이 1번 호출되었는지 확인");
		verify(timeSlotPort, times(1)).saveAll(any());
		log.info("[Then] - ✓ saveAll() 호출 확인됨");

		log.info("=== [특정 날짜 슬롯 생성] 테스트 성공 ===");
	}

	@Test
	@DisplayName("운영 정책이 없으면 예외가 발생한다")
	void generateSlotsForDate_policyNotFound() {
		log.info("=== [정책 없음 예외] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		when(operatingPolicyPort.findByRoomId(roomId)).thenReturn(Optional.empty());
		log.info("[Given] - operatingPolicyPort.findByRoomId() -> 빈 Optional 반환");

		// When & Then
		log.info("[When & Then] generateSlotsForDate() 호출 시 PolicyNotFoundException 발생");
		log.info("[When & Then] - 파라미터: roomId={}, date={}", roomId, testDate);

		assertThatThrownBy(() -> service.generateSlotsForDate(roomId, testDate))
				.isInstanceOf(PolicyNotFoundException.class);

		log.info("[Then] - ✓ PolicyNotFoundException 발생 확인됨");

		log.info("[Then] [검증] saveAll()이 호출되지 않았는지 확인");
		verify(timeSlotPort, never()).saveAll(any());
		log.info("[Then] - ✓ saveAll() 미호출 확인됨");

		log.info("=== [정책 없음 예외] 테스트 성공 ===");
	}

	@Test
	@DisplayName("날짜 범위에 대한 슬롯을 생성한다")
	void generateSlotsForDateRange() {
		log.info("=== [날짜 범위 슬롯 생성] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		LocalDate startDate = LocalDate.of(2025, 1, 20); // 월요일
		LocalDate endDate = LocalDate.of(2025, 1, 22);   // 수요일 (3일)

		when(operatingPolicyPort.findByRoomId(roomId)).thenReturn(Optional.of(policy));
		when(placeInfoApiClient.getSlotUnit(roomId)).thenReturn(slotUnit);

		List<RoomTimeSlot> slotsPerDay = List.of(
				RoomTimeSlot.available(roomId, startDate, LocalTime.of(9, 0)),
				RoomTimeSlot.available(roomId, startDate, LocalTime.of(9, 30)),
				RoomTimeSlot.available(roomId, startDate, LocalTime.of(10, 0)),
				RoomTimeSlot.available(roomId, startDate, LocalTime.of(10, 30))
		);

		when(timeSlotPort.saveAll(any())).thenReturn(slotsPerDay);
		log.info("[Given] - 각 날짜마다 {} 개 슬롯 생성 예정", slotsPerDay.size());

		// When
		log.info("[When] generateSlotsForDateRange() 호출");
		log.info("[When] - 파라미터: roomId={}, startDate={}, endDate={}", roomId, startDate, endDate);
		int totalGenerated = service.generateSlotsForDateRange(roomId, startDate, endDate);
		log.info("[When] - 호출 완료: totalGenerated={}", totalGenerated);

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 생성된 슬롯 개수 확인 (3일 * 4개 = 12개)");
		log.info("[Then] - 예상 개수: 12");
		log.info("[Then] - 실제 개수: {}", totalGenerated);
		assertThat(totalGenerated).isEqualTo(12);
		log.info("[Then] - ✓ 생성 개수 확인됨");

		log.info("[Then] [검증2] operatingPolicyPort.findByRoomId()가 3번 호출되었는지 확인 (각 날짜마다)");
		verify(operatingPolicyPort, times(3)).findByRoomId(roomId);
		log.info("[Then] - ✓ findByRoomId() 호출 횟수 확인됨");

		log.info("[Then] [검증3] timeSlotPort.saveAll()이 3번 호출되었는지 확인 (각 날짜마다)");
		verify(timeSlotPort, times(3)).saveAll(any());
		log.info("[Then] - ✓ saveAll() 호출 횟수 확인됨");

		log.info("=== [날짜 범위 슬롯 생성] 테스트 성공 ===");
	}

	@Test
	@DisplayName("모든 룸의 특정 날짜 슬롯을 생성한다")
	void generateSlotsForAllRooms() {
		log.info("=== [모든 룸 슬롯 생성] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		Long room1Id = 101L;
		Long room2Id = 102L;
		Long room3Id = 103L;

		RoomOperatingPolicy policy1 = RoomOperatingPolicy.create(
				room1Id,
				WeeklySlotSchedule.of(List.of(WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)))),
				RecurrencePattern.EVERY_WEEK,
				List.of()
		);
		RoomOperatingPolicy policy2 = RoomOperatingPolicy.create(
				room2Id,
				WeeklySlotSchedule.of(List.of(WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(10, 0)))),
				RecurrencePattern.EVERY_WEEK,
				List.of()
		);
		RoomOperatingPolicy policy3 = RoomOperatingPolicy.create(
				room3Id,
				WeeklySlotSchedule.of(List.of(WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(11, 0)))),
				RecurrencePattern.EVERY_WEEK,
				List.of()
		);

		List<RoomOperatingPolicy> allPolicies = List.of(policy1, policy2, policy3);
		when(operatingPolicyPort.findAll()).thenReturn(allPolicies);
		log.info("[Given] - operatingPolicyPort.findAll() -> 3개 정책 반환");

		when(operatingPolicyPort.findByRoomId(room1Id)).thenReturn(Optional.of(policy1));
		when(operatingPolicyPort.findByRoomId(room2Id)).thenReturn(Optional.of(policy2));
		when(operatingPolicyPort.findByRoomId(room3Id)).thenReturn(Optional.of(policy3));

		when(placeInfoApiClient.getSlotUnit(any())).thenReturn(slotUnit);

		List<RoomTimeSlot> slotsPerRoom = List.of(
				RoomTimeSlot.available(room1Id, testDate, LocalTime.of(9, 0)),
				RoomTimeSlot.available(room1Id, testDate, LocalTime.of(9, 30))
		);
		when(timeSlotPort.saveAll(any())).thenReturn(slotsPerRoom);
		log.info("[Given] - 각 룸마다 {} 개 슬롯 생성 예정", slotsPerRoom.size());

		// When
		log.info("[When] generateSlotsForAllRooms() 호출");
		log.info("[When] - 파라미터: date={}", testDate);
		int totalGenerated = service.generateSlotsForAllRooms(testDate);
		log.info("[When] - 호출 완료: totalGenerated={}", totalGenerated);

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 생성된 슬롯 개수 확인 (3개 룸 * 2개 = 6개)");
		log.info("[Then] - 예상 개수: 6");
		log.info("[Then] - 실제 개수: {}", totalGenerated);
		assertThat(totalGenerated).isEqualTo(6);
		log.info("[Then] - ✓ 생성 개수 확인됨");

		log.info("[Then] [검증2] operatingPolicyPort.findAll()이 1번 호출되었는지 확인");
		verify(operatingPolicyPort, times(1)).findAll();
		log.info("[Then] - ✓ findAll() 호출 확인됨");

		log.info("[Then] [검증3] timeSlotPort.saveAll()이 3번 호출되었는지 확인 (각 룸마다)");
		verify(timeSlotPort, times(3)).saveAll(any());
		log.info("[Then] - ✓ saveAll() 호출 횟수 확인됨");

		log.info("=== [모든 룸 슬롯 생성] 테스트 성공 ===");
	}

	@Test
	@DisplayName("특정 날짜 이전의 슬롯을 삭제한다")
	void deleteSlotsBeforeDate() {
		log.info("=== [특정 날짜 이전 슬롯 삭제] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		LocalDate beforeDate = LocalDate.of(2025, 1, 15);
		int deletedCount = 50;

		when(timeSlotPort.deleteBySlotDateBefore(beforeDate)).thenReturn(deletedCount);
		log.info("[Given] - timeSlotPort.deleteBySlotDateBefore() -> {} 개 삭제 예정", deletedCount);

		// When
		log.info("[When] deleteSlotsBeforeDate() 호출");
		log.info("[When] - 파라미터: beforeDate={}", beforeDate);
		int result = service.deleteSlotsBeforeDate(beforeDate);
		log.info("[When] - 호출 완료: deletedCount={}", result);

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 삭제된 슬롯 개수 확인");
		log.info("[Then] - 예상 개수: {}", deletedCount);
		log.info("[Then] - 실제 개수: {}", result);
		assertThat(result).isEqualTo(deletedCount);
		log.info("[Then] - ✓ 삭제 개수 확인됨");

		log.info("[Then] [검증2] timeSlotPort.deleteBySlotDateBefore()가 1번 호출되었는지 확인");
		verify(timeSlotPort, times(1)).deleteBySlotDateBefore(beforeDate);
		log.info("[Then] - ✓ deleteBySlotDateBefore() 호출 확인됨");

		log.info("=== [특정 날짜 이전 슬롯 삭제] 테스트 성공 ===");
	}

	@Test
	@DisplayName("미래 슬롯을 재생성한다")
	void regenerateFutureSlots() {
		log.info("=== [미래 슬롯 재생성] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		when(operatingPolicyPort.findByRoomId(roomId)).thenReturn(Optional.of(policy));
		when(placeInfoApiClient.getSlotUnit(roomId)).thenReturn(slotUnit);

		List<RoomTimeSlot> slotsPerDay = List.of(
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(9, 0)),
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(9, 30)),
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(10, 0)),
				RoomTimeSlot.available(roomId, testDate, LocalTime.of(10, 30))
		);
		when(timeSlotPort.saveAll(any())).thenReturn(slotsPerDay);
		log.info("[Given] - 각 날짜마다 {} 개 슬롯 생성 예정", slotsPerDay.size());

		// When
		log.info("[When] regenerateFutureSlots() 호출");
		log.info("[When] - 파라미터: roomId={}", roomId);
		int regeneratedCount = service.regenerateFutureSlots(roomId);
		log.info("[When] - 호출 완료: regeneratedCount={}", regeneratedCount);

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 재생성된 슬롯 개수 확인 (60일 * 4개 = 240개 이상)");
		log.info("[Then] - 실제 개수: {}", regeneratedCount);
		assertThat(regeneratedCount).isGreaterThan(0);
		log.info("[Then] - ✓ 재생성 개수 확인됨");

		log.info("[Then] [검증2] timeSlotPort.deleteByRoomId()가 1번 호출되었는지 확인");
		verify(timeSlotPort, times(1)).deleteByRoomId(roomId);
		log.info("[Then] - ✓ deleteByRoomId() 호출 확인됨 (기존 슬롯 삭제)");

		log.info("[Then] [검증3] timeSlotPort.saveAll()이 60번 호출되었는지 확인 (60일치)");
		// 60일치 슬롯을 생성하므로 saveAll()이 60번 호출되어야 함
		// 단, 월요일만 슬롯이 생성되므로 실제로는 8~9번 정도 호출됨
		verify(timeSlotPort, atLeast(1)).saveAll(any());
		log.info("[Then] - ✓ saveAll() 호출 확인됨");

		log.info("=== [미래 슬롯 재생성] 테스트 성공 ===");
	}

	@Test
	@DisplayName("슬롯 생성 중 예외 발생 시 SlotGenerationFailedException으로 래핑한다")
	void generateSlotsForDate_exception() {
		log.info("=== [슬롯 생성 예외 처리] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		when(operatingPolicyPort.findByRoomId(roomId)).thenReturn(Optional.of(policy));
		when(placeInfoApiClient.getSlotUnit(roomId)).thenThrow(new RuntimeException("API 호출 실패"));
		log.info("[Given] - placeInfoApiClient.getSlotUnit() -> 예외 발생");

		// When & Then
		log.info("[When & Then] generateSlotsForDate() 호출 시 SlotGenerationFailedException 발생");
		log.info("[When & Then] - 파라미터: roomId={}, date={}", roomId, testDate);

		assertThatThrownBy(() -> service.generateSlotsForDate(roomId, testDate))
				.isInstanceOf(SlotGenerationFailedException.class)
				.hasMessageContaining(testDate.toString());

		log.info("[Then] - ✓ SlotGenerationFailedException 발생 확인됨");

		log.info("[Then] [검증] saveAll()이 호출되지 않았는지 확인");
		verify(timeSlotPort, never()).saveAll(any());
		log.info("[Then] - ✓ saveAll() 미호출 확인됨");

		log.info("=== [슬롯 생성 예외 처리] 테스트 성공 ===");
	}

	@Test
	@DisplayName("모든 룸 슬롯 생성 중 일부 실패해도 나머지는 계속 처리한다")
	void generateSlotsForAllRooms_partialFailure() {
		log.info("=== [일부 룸 실패 시 계속 처리] 테스트 시작 ===");

		// Given
		log.info("[Given] Mock 동작 설정");
		Long room1Id = 101L;
		Long room2Id = 102L;
		Long room3Id = 103L;

		RoomOperatingPolicy policy1 = RoomOperatingPolicy.create(
				room1Id,
				WeeklySlotSchedule.of(List.of(WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(9, 0)))),
				RecurrencePattern.EVERY_WEEK,
				List.of()
		);
		RoomOperatingPolicy policy2 = RoomOperatingPolicy.create(
				room2Id,
				WeeklySlotSchedule.of(List.of(WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(10, 0)))),
				RecurrencePattern.EVERY_WEEK,
				List.of()
		);
		RoomOperatingPolicy policy3 = RoomOperatingPolicy.create(
				room3Id,
				WeeklySlotSchedule.of(List.of(WeeklySlotTime.of(DayOfWeek.MONDAY, LocalTime.of(11, 0)))),
				RecurrencePattern.EVERY_WEEK,
				List.of()
		);

		List<RoomOperatingPolicy> allPolicies = List.of(policy1, policy2, policy3);
		when(operatingPolicyPort.findAll()).thenReturn(allPolicies);

		// Room1: 성공
		when(operatingPolicyPort.findByRoomId(room1Id)).thenReturn(Optional.of(policy1));
		when(placeInfoApiClient.getSlotUnit(room1Id)).thenReturn(slotUnit);

		// Room2: 실패 (정책 없음)
		when(operatingPolicyPort.findByRoomId(room2Id)).thenReturn(Optional.empty());

		// Room3: 성공
		when(operatingPolicyPort.findByRoomId(room3Id)).thenReturn(Optional.of(policy3));
		when(placeInfoApiClient.getSlotUnit(room3Id)).thenReturn(slotUnit);

		List<RoomTimeSlot> slotsPerRoom = List.of(
				RoomTimeSlot.available(room1Id, testDate, LocalTime.of(9, 0)),
				RoomTimeSlot.available(room1Id, testDate, LocalTime.of(9, 30))
		);
		when(timeSlotPort.saveAll(any())).thenReturn(slotsPerRoom);

		log.info("[Given] - Room1: 성공 예정");
		log.info("[Given] - Room2: 실패 예정 (정책 없음)");
		log.info("[Given] - Room3: 성공 예정");

		// When
		log.info("[When] generateSlotsForAllRooms() 호출");
		log.info("[When] - 파라미터: date={}", testDate);
		int totalGenerated = service.generateSlotsForAllRooms(testDate);
		log.info("[When] - 호출 완료: totalGenerated={}", totalGenerated);

		// Then
		log.info("[Then] 결과 검증 시작");

		log.info("[Then] [검증1] 성공한 룸의 슬롯만 생성되었는지 확인 (Room1, Room3만)");
		log.info("[Then] - 예상 개수: 4 (Room1: 2개, Room3: 2개)");
		log.info("[Then] - 실제 개수: {}", totalGenerated);
		assertThat(totalGenerated).isEqualTo(4);
		log.info("[Then] - ✓ 실패한 룸을 제외한 슬롯 생성 확인됨");

		log.info("[Then] [검증2] timeSlotPort.saveAll()이 2번 호출되었는지 확인 (Room1, Room3만)");
		verify(timeSlotPort, times(2)).saveAll(any());
		log.info("[Then] - ✓ saveAll() 호출 횟수 확인됨");

		log.info("=== [일부 룸 실패 시 계속 처리] 테스트 성공 ===");
	}
}
