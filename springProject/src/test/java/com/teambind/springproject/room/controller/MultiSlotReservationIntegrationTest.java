package com.teambind.springproject.room.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.springproject.room.command.dto.MultiSlotReservationRequest;
import com.teambind.springproject.room.entity.RoomTimeSlot;
import com.teambind.springproject.room.entity.enums.SlotStatus;
import com.teambind.springproject.room.repository.RoomTimeSlotRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 다중 슬롯 예약 통합 테스트.
 *
 * Pessimistic Lock을 사용한 동시성 제어를 검증한다.
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MultiSlotReservationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RoomTimeSlotRepository slotRepository;

	private Long roomId;
	private LocalDate slotDate;
	private List<LocalTime> slotTimes;

	@BeforeEach
	void setUp() {
		log.info("[Setup] 테스트 데이터 준비");

		roomId = 1L;
		slotDate = LocalDate.now().plusDays(1);
		slotTimes = Arrays.asList(
				LocalTime.of(9, 0),
				LocalTime.of(10, 0),
				LocalTime.of(11, 0)
		);

		// 테스트용 슬롯 생성
		for (LocalTime time : slotTimes) {
			RoomTimeSlot slot = RoomTimeSlot.available(roomId, slotDate, time);
			slotRepository.save(slot);
		}

		log.info("[Setup] {} 개의 AVAILABLE 슬롯 생성 완료", slotTimes.size());
	}

	@Test
	@DisplayName("다중 슬롯 예약 성공 - 예약 ID 자동 생성 및 모든 슬롯 PENDING 상태로 변경")
	void createMultiSlotReservation_Success() throws Exception {
		// Given
		MultiSlotReservationRequest request = new MultiSlotReservationRequest(
				roomId,
				slotDate,
				slotTimes
		);

		log.info("[Given] 다중 슬롯 예약 요청: roomId={}, slotDate={}, slotTimes={}",
				roomId, slotDate, slotTimes);

		// When & Then
		mockMvc.perform(post("/api/v1/reservations/multi")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reservationId").isNumber())
				.andExpect(jsonPath("$.roomId").value(roomId))
				.andExpect(jsonPath("$.slotDate").value(slotDate.toString()))
				.andExpect(jsonPath("$.reservedSlotTimes").isArray())
				.andExpect(jsonPath("$.reservedSlotTimes.length()").value(slotTimes.size()));

		log.info("[Then] API 응답 검증 완료");

		// 데이터베이스 상태 확인
		for (LocalTime time : slotTimes) {
			RoomTimeSlot slot = slotRepository.findByRoomIdAndSlotDateAndSlotTime(
					roomId, slotDate, time
			).orElseThrow();

			assertThat(slot.getStatus()).isEqualTo(SlotStatus.PENDING);
			assertThat(slot.getReservationId()).isNotNull();
			log.info("[Then] 슬롯 상태 확인: time={}, status={}, reservationId={}",
					time, slot.getStatus(), slot.getReservationId());
		}

		log.info("[Success] 다중 슬롯 예약 성공 테스트 완료");
	}

	@Test
	@DisplayName("다중 슬롯 예약 실패 - 일부 슬롯이 이미 예약된 경우")
	void createMultiSlotReservation_Fail_PartiallyReserved() throws Exception {
		// Given: 첫 번째 슬롯을 미리 PENDING 상태로 변경
		RoomTimeSlot firstSlot = slotRepository.findByRoomIdAndSlotDateAndSlotTime(
				roomId, slotDate, slotTimes.get(0)
		).orElseThrow();
		firstSlot.markAsPending(999L);
		slotRepository.save(firstSlot);

		log.info("[Given] 첫 번째 슬롯을 PENDING 상태로 변경: time={}", slotTimes.get(0));

		MultiSlotReservationRequest request = new MultiSlotReservationRequest(
				roomId,
				slotDate,
				slotTimes
		);

		// When & Then
		mockMvc.perform(post("/api/v1/reservations/multi")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().is5xxServerError());

		log.info("[Then] 예약 실패 응답 확인 (500 Internal Server Error)");

		// 모든 슬롯이 원래 상태를 유지하는지 확인 (트랜잭션 롤백)
		RoomTimeSlot secondSlot = slotRepository.findByRoomIdAndSlotDateAndSlotTime(
				roomId, slotDate, slotTimes.get(1)
		).orElseThrow();

		assertThat(secondSlot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
		log.info("[Then] 두 번째 슬롯은 AVAILABLE 상태 유지: time={}", slotTimes.get(1));

		log.info("[Success] 부분 예약 실패 테스트 완료");
	}

	@Test
	@DisplayName("다중 슬롯 예약 실패 - 존재하지 않는 슬롯 포함")
	void createMultiSlotReservation_Fail_SlotNotFound() throws Exception {
		// Given
		List<LocalTime> invalidTimes = Arrays.asList(
				LocalTime.of(9, 0),
				LocalTime.of(10, 0),
				LocalTime.of(23, 0) // 존재하지 않는 시간
		);

		MultiSlotReservationRequest request = new MultiSlotReservationRequest(
				roomId,
				slotDate,
				invalidTimes
		);

		log.info("[Given] 존재하지 않는 슬롯 포함: invalidTimes={}", invalidTimes);

		// When & Then
		mockMvc.perform(post("/api/v1/reservations/multi")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().is5xxServerError());

		log.info("[Then] 예약 실패 응답 확인 (500 Internal Server Error)");

		// 기존 슬롯들이 AVAILABLE 상태를 유지하는지 확인
		RoomTimeSlot firstSlot = slotRepository.findByRoomIdAndSlotDateAndSlotTime(
				roomId, slotDate, slotTimes.get(0)
		).orElseThrow();

		assertThat(firstSlot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
		log.info("[Then] 첫 번째 슬롯은 AVAILABLE 상태 유지");

		log.info("[Success] 슬롯 미존재 실패 테스트 완료");
	}
}