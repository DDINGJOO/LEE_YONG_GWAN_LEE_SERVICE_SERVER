package com.teambind.springproject.room.mapper;

import com.teambind.springproject.room.query.dto.AvailableSlotResponse;
import com.teambind.springproject.room.query.dto.SlotAvailabilityResponse;
import com.teambind.springproject.room.query.dto.TimeSlotResponse;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TimeSlotMapper 테스트.
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("TimeSlotMapper 테스트")
class TimeSlotMapperTest {
	
	private TimeSlotMapper mapper;
	
	@BeforeEach
	void setUp() {
		mapper = new TimeSlotMapper();
	}
	
	@Test
	@DisplayName("RoomTimeSlot을 TimeSlotResponse로 변환한다")
	void toTimeSlotResponse() {
		log.info("=== [RoomTimeSlot을 TimeSlotResponse로 변환한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		RoomTimeSlot slot = RoomTimeSlot.available(
				100L,
				LocalDate.of(2025, 11, 5),
				LocalTime.of(9, 0)
		);
		slot.markAsPending(200L);
		log.info("[Given] - roomId: 100");
		log.info("[Given] - slotDate: 2025-11-05");
		log.info("[Given] - slotTime: 09:00");
		log.info("[Given] - status: AVAILABLE -> PENDING");
		log.info("[Given] - reservationId: 200");
		
		// When
		log.info("[When] mapper.toTimeSlotResponse() 호출");
		TimeSlotResponse response = mapper.toTimeSlotResponse(slot);
		log.info("[When] - 반환 값: {}", response);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] slotId (DB 저장 전)");
		log.info("[Then] - 예상(Expected): null");
		log.info("[Then] - 실제(Actual): {}", response.slotId());
		assertThat(response.slotId()).isNull();
		log.info("[Then] - ✓ DB 저장 전이므로 ID는 null");
		
		log.info("[Then] [검증2] roomId");
		log.info("[Then] - 예상(Expected): 100");
		log.info("[Then] - 실제(Actual): {}", response.roomId());
		assertThat(response.roomId()).isEqualTo(100L);
		log.info("[Then] - ✓ roomId 일치");
		
		log.info("[Then] [검증3] slotDate");
		log.info("[Then] - 예상(Expected): 2025-11-05");
		log.info("[Then] - 실제(Actual): {}", response.slotDate());
		assertThat(response.slotDate()).isEqualTo(LocalDate.of(2025, 11, 5));
		log.info("[Then] - ✓ slotDate 일치");
		
		log.info("[Then] [검증4] slotTime");
		log.info("[Then] - 예상(Expected): 09:00");
		log.info("[Then] - 실제(Actual): {}", response.slotTime());
		assertThat(response.slotTime()).isEqualTo(LocalTime.of(9, 0));
		log.info("[Then] - ✓ slotTime 일치");
		
		log.info("[Then] [검증5] status");
		log.info("[Then] - 예상(Expected): PENDING");
		log.info("[Then] - 실제(Actual): {}", response.status());
		assertThat(response.status()).isEqualTo(SlotStatus.PENDING);
		log.info("[Then] - ✓ status 일치");
		
		log.info("[Then] [검증6] reservationId");
		log.info("[Then] - 예상(Expected): 200");
		log.info("[Then] - 실제(Actual): {}", response.reservationId());
		assertThat(response.reservationId()).isEqualTo(200L);
		log.info("[Then] - ✓ reservationId 일치");
		
		log.info("=== [RoomTimeSlot을 TimeSlotResponse로 변환한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("RoomTimeSlot을 AvailableSlotResponse로 변환한다")
	void toAvailableSlotResponse() {
		log.info("=== [RoomTimeSlot을 AvailableSlotResponse로 변환한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		RoomTimeSlot slot = RoomTimeSlot.available(
				100L,
				LocalDate.of(2025, 11, 5),
				LocalTime.of(10, 0)
		);
		log.info("[Given] - roomId: 100");
		log.info("[Given] - slotDate: 2025-11-05");
		log.info("[Given] - slotTime: 10:00");
		log.info("[Given] - status: AVAILABLE");
		
		// When
		log.info("[When] mapper.toAvailableSlotResponse() 호출");
		AvailableSlotResponse response = mapper.toAvailableSlotResponse(slot);
		log.info("[When] - 반환 값: {}", response);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] slotId (DB 저장 전)");
		log.info("[Then] - 예상(Expected): null");
		log.info("[Then] - 실제(Actual): {}", response.slotId());
		assertThat(response.slotId()).isNull();
		log.info("[Then] - ✓ DB 저장 전이므로 ID는 null");
		
		log.info("[Then] [검증2] roomId");
		log.info("[Then] - 예상(Expected): 100");
		log.info("[Then] - 실제(Actual): {}", response.roomId());
		assertThat(response.roomId()).isEqualTo(100L);
		log.info("[Then] - ✓ roomId 일치");
		
		log.info("[Then] [검증3] slotDate");
		log.info("[Then] - 예상(Expected): 2025-11-05");
		log.info("[Then] - 실제(Actual): {}", response.slotDate());
		assertThat(response.slotDate()).isEqualTo(LocalDate.of(2025, 11, 5));
		log.info("[Then] - ✓ slotDate 일치");
		
		log.info("[Then] [검증4] slotTime");
		log.info("[Then] - 예상(Expected): 10:00");
		log.info("[Then] - 실제(Actual): {}", response.slotTime());
		assertThat(response.slotTime()).isEqualTo(LocalTime.of(10, 0));
		log.info("[Then] - ✓ slotTime 일치");
		
		log.info("=== [RoomTimeSlot을 AvailableSlotResponse로 변환한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("RoomTimeSlot 목록을 TimeSlotResponse 목록으로 변환한다")
	void toTimeSlotResponseList() {
		log.info("=== [RoomTimeSlot 목록을 TimeSlotResponse 목록으로 변환한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		List<RoomTimeSlot> slots = List.of(
				RoomTimeSlot.available(100L, LocalDate.of(2025, 11, 5), LocalTime.of(9, 0)),
				RoomTimeSlot.available(100L, LocalDate.of(2025, 11, 5), LocalTime.of(10, 0)),
				RoomTimeSlot.available(100L, LocalDate.of(2025, 11, 5), LocalTime.of(11, 0))
		);
		log.info("[Given] - roomId: 100");
		log.info("[Given] - slotDate: 2025-11-05");
		log.info("[Given] - 슬롯 개수: 3개 (9시, 10시, 11시)");
		
		// When
		log.info("[When] mapper.toTimeSlotResponseList() 호출");
		List<TimeSlotResponse> responses = mapper.toTimeSlotResponseList(slots);
		log.info("[When] - 반환된 응답 개수: {}", responses.size());
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 변환된 목록 크기");
		log.info("[Then] - 예상(Expected): 3개");
		log.info("[Then] - 실제(Actual): {}개", responses.size());
		assertThat(responses).hasSize(3);
		log.info("[Then] - ✓ 목록 크기 일치");
		
		log.info("[Then] [검증2] 첫 번째 슬롯 시간");
		log.info("[Then] - 예상(Expected): 09:00");
		log.info("[Then] - 실제(Actual): {}", responses.get(0).slotTime());
		assertThat(responses.get(0).slotTime()).isEqualTo(LocalTime.of(9, 0));
		log.info("[Then] - ✓ 첫 번째 슬롯 시간 일치");
		
		log.info("[Then] [검증3] 두 번째 슬롯 시간");
		log.info("[Then] - 예상(Expected): 10:00");
		log.info("[Then] - 실제(Actual): {}", responses.get(1).slotTime());
		assertThat(responses.get(1).slotTime()).isEqualTo(LocalTime.of(10, 0));
		log.info("[Then] - ✓ 두 번째 슬롯 시간 일치");
		
		log.info("[Then] [검증4] 세 번째 슬롯 시간");
		log.info("[Then] - 예상(Expected): 11:00");
		log.info("[Then] - 실제(Actual): {}", responses.get(2).slotTime());
		assertThat(responses.get(2).slotTime()).isEqualTo(LocalTime.of(11, 0));
		log.info("[Then] - ✓ 세 번째 슬롯 시간 일치");
		
		log.info("=== [RoomTimeSlot 목록을 TimeSlotResponse 목록으로 변환한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("RoomTimeSlot 목록을 AvailableSlotResponse 목록으로 변환한다")
	void toAvailableSlotResponseList() {
		log.info("=== [RoomTimeSlot 목록을 AvailableSlotResponse 목록으로 변환한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		List<RoomTimeSlot> slots = List.of(
				RoomTimeSlot.available(100L, LocalDate.of(2025, 11, 5), LocalTime.of(9, 0)),
				RoomTimeSlot.available(100L, LocalDate.of(2025, 11, 5), LocalTime.of(10, 0))
		);
		log.info("[Given] - roomId: 100");
		log.info("[Given] - slotDate: 2025-11-05");
		log.info("[Given] - 슬롯 개수: 2개 (9시, 10시)");
		
		// When
		log.info("[When] mapper.toAvailableSlotResponseList() 호출");
		List<AvailableSlotResponse> responses = mapper.toAvailableSlotResponseList(slots);
		log.info("[When] - 반환된 응답 개수: {}", responses.size());
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] 변환된 목록 크기");
		log.info("[Then] - 예상(Expected): 2개");
		log.info("[Then] - 실제(Actual): {}개", responses.size());
		assertThat(responses).hasSize(2);
		log.info("[Then] - ✓ 목록 크기 일치");
		
		log.info("[Then] [검증2] 첫 번째 슬롯 시간");
		log.info("[Then] - 예상(Expected): 09:00");
		log.info("[Then] - 실제(Actual): {}", responses.get(0).slotTime());
		assertThat(responses.get(0).slotTime()).isEqualTo(LocalTime.of(9, 0));
		log.info("[Then] - ✓ 첫 번째 슬롯 시간 일치");
		
		log.info("[Then] [검증3] 두 번째 슬롯 시간");
		log.info("[Then] - 예상(Expected): 10:00");
		log.info("[Then] - 실제(Actual): {}", responses.get(1).slotTime());
		assertThat(responses.get(1).slotTime()).isEqualTo(LocalTime.of(10, 0));
		log.info("[Then] - ✓ 두 번째 슬롯 시간 일치");
		
		log.info("=== [RoomTimeSlot 목록을 AvailableSlotResponse 목록으로 변환한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("슬롯 가용성 정보를 SlotAvailabilityResponse로 변환한다")
	void toSlotAvailabilityResponse() {
		log.info("=== [슬롯 가용성 정보를 SlotAvailabilityResponse로 변환한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		Long roomId = 100L;
		LocalDate slotDate = LocalDate.of(2025, 11, 5);
		LocalTime slotTime = LocalTime.of(9, 0);
		boolean available = true;
		log.info("[Given] - roomId: {}", roomId);
		log.info("[Given] - slotDate: {}", slotDate);
		log.info("[Given] - slotTime: {}", slotTime);
		log.info("[Given] - available: {}", available);
		
		// When
		log.info("[When] mapper.toSlotAvailabilityResponse() 호출");
		log.info("[When] - 파라미터: roomId={}, slotDate={}, slotTime={}, available={}", roomId, slotDate, slotTime, available);
		SlotAvailabilityResponse response = mapper.toSlotAvailabilityResponse(
				roomId, slotDate, slotTime, available
		);
		log.info("[When] - 반환 값: {}", response);
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] roomId");
		log.info("[Then] - 예상(Expected): 100");
		log.info("[Then] - 실제(Actual): {}", response.roomId());
		assertThat(response.roomId()).isEqualTo(100L);
		log.info("[Then] - ✓ roomId 일치");
		
		log.info("[Then] [검증2] slotDate");
		log.info("[Then] - 예상(Expected): 2025-11-05");
		log.info("[Then] - 실제(Actual): {}", response.slotDate());
		assertThat(response.slotDate()).isEqualTo(LocalDate.of(2025, 11, 5));
		log.info("[Then] - ✓ slotDate 일치");
		
		log.info("[Then] [검증3] slotTime");
		log.info("[Then] - 예상(Expected): 09:00");
		log.info("[Then] - 실제(Actual): {}", response.slotTime());
		assertThat(response.slotTime()).isEqualTo(LocalTime.of(9, 0));
		log.info("[Then] - ✓ slotTime 일치");
		
		log.info("[Then] [검증4] available");
		log.info("[Then] - 예상(Expected): true");
		log.info("[Then] - 실제(Actual): {}", response.available());
		assertThat(response.available()).isTrue();
		log.info("[Then] - ✓ available 일치");
		
		log.info("=== [슬롯 가용성 정보를 SlotAvailabilityResponse로 변환한다] 테스트 성공 ===");
	}
	
	
	@Test
	@DisplayName("빈 목록을 변환하면 빈 목록을 반환한다")
	void emptyListConversion() {
		log.info("=== [빈 목록을 변환하면 빈 목록을 반환한다] 테스트 시작 ===");
		
		// Given
		log.info("[Given] 테스트 데이터 준비");
		List<RoomTimeSlot> emptySlots = List.of();
		log.info("[Given] - emptySlots 크기: {}", emptySlots.size());
		
		// When
		log.info("[When] mapper.toTimeSlotResponseList() 및 toAvailableSlotResponseList() 호출");
		List<TimeSlotResponse> timeSlotResponses = mapper.toTimeSlotResponseList(emptySlots);
		List<AvailableSlotResponse> availableResponses = mapper.toAvailableSlotResponseList(emptySlots);
		log.info("[When] - timeSlotResponses 크기: {}", timeSlotResponses.size());
		log.info("[When] - availableResponses 크기: {}", availableResponses.size());
		
		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증1] TimeSlotResponse 목록이 비어있음");
		log.info("[Then] - 예상(Expected): 빈 목록 (0개)");
		log.info("[Then] - 실제(Actual): {} 개", timeSlotResponses.size());
		assertThat(timeSlotResponses).isEmpty();
		log.info("[Then] - ✓ TimeSlotResponse 목록이 비어있음");
		
		log.info("[Then] [검증2] AvailableSlotResponse 목록이 비어있음");
		log.info("[Then] - 예상(Expected): 빈 목록 (0개)");
		log.info("[Then] - 실제(Actual): {} 개", availableResponses.size());
		assertThat(availableResponses).isEmpty();
		log.info("[Then] - ✓ AvailableSlotResponse 목록이 비어있음");
		
		log.info("=== [빈 목록을 변환하면 빈 목록을 반환한다] 테스트 성공 ===");
	}
	
}
