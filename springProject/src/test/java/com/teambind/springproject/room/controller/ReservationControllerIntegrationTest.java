package com.teambind.springproject.room.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.springproject.config.TestKafkaConfig;
import com.teambind.springproject.config.TestRedisConfig;
import com.teambind.springproject.room.command.dto.SlotReservationRequest;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ReservationController 통합 테스트.
 *
 * 실제 HTTP 요청을 MockMvc로 시뮬레이션하고 DB 상태를 검증한다.
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestRedisConfig.class, TestKafkaConfig.class})
@Transactional
@DisplayName("ReservationController 통합 테스트")
class ReservationControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RoomTimeSlotRepository slotRepository;

	private Long roomId;
	private LocalDate slotDate;
	private LocalTime slotTime;
	private Long reservationId;

	@BeforeEach
	void setUp() {
		roomId = 100L;
		slotDate = LocalDate.of(2025, 11, 15);
		slotTime = LocalTime.of(10, 0);
		reservationId = 1L;

		log.info("=== 테스트 데이터 초기화 ===");
		log.info("- roomId: {}", roomId);
		log.info("- slotDate: {}", slotDate);
		log.info("- slotTime: {}", slotTime);
		log.info("- reservationId: {}", reservationId);

		// AVAILABLE 슬롯 생성
		RoomTimeSlot slot = RoomTimeSlot.available(roomId, slotDate, slotTime);
		slotRepository.save(slot);
		log.info("- 초기 슬롯 상태: AVAILABLE");
	}

	@Test
	@DisplayName("POST /api/v1/reservations - 정상 요청 시 200 OK 응답")
	void createReservation_Success() throws Exception {
		log.info("=== [POST /api/v1/reservations 정상 요청] 테스트 시작 ===");

		// Given
		log.info("[Given] 예약 요청 생성");
		SlotReservationRequest request = new SlotReservationRequest(
				roomId, slotDate, slotTime, reservationId
		);
		String requestBody = objectMapper.writeValueAsString(request);
		log.info("[Given] - request: {}", request);

		// When
		log.info("[When] POST /api/v1/reservations 호출");
		mockMvc.perform(post("/api/v1/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andDo(print())
				.andExpect(status().isOk());
		log.info("[When] - 호출 완료");

		// Then
		log.info("[Then] 결과 검증 시작");
		log.info("[Then] [검증] DB에서 슬롯 조회 및 상태 확인");
		RoomTimeSlot slot = slotRepository.findByRoomIdAndSlotDateAndSlotTime(
						roomId, slotDate, slotTime)
				.orElseThrow();

		log.info("[Then] - 예상 상태: PENDING");
		log.info("[Then] - 실제 상태: {}", slot.getStatus());
		assertThat(slot.getStatus()).isEqualTo(SlotStatus.PENDING);

		log.info("[Then] - 예상 reservationId: {}", reservationId);
		log.info("[Then] - 실제 reservationId: {}", slot.getReservationId());
		assertThat(slot.getReservationId()).isEqualTo(reservationId);

		log.info("=== [POST /api/v1/reservations 정상 요청] 테스트 성공 ===");
	}

	@Test
	@DisplayName("POST /api/v1/reservations - 존재하지 않는 슬롯 요청 시 4xx 에러")
	void createReservation_SlotNotFound() throws Exception {
		log.info("=== [POST /api/v1/reservations 슬롯 미존재] 테스트 시작 ===");

		// Given
		log.info("[Given] 존재하지 않는 슬롯 예약 요청 생성");
		LocalDate nonExistentDate = LocalDate.of(2099, 12, 31);
		SlotReservationRequest request = new SlotReservationRequest(
				roomId, nonExistentDate, slotTime, reservationId
		);
		String requestBody = objectMapper.writeValueAsString(request);
		log.info("[Given] - request: {}", request);

		// When & Then
		log.info("[When & Then] POST /api/v1/reservations 호출 시 4xx 에러 확인");
		mockMvc.perform(post("/api/v1/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andDo(print())
				.andExpect(status().is4xxClientError());

		log.info("=== [POST /api/v1/reservations 슬롯 미존재] 테스트 성공 ===");
	}

	@Test
	@DisplayName("POST /api/v1/reservations - 이미 PENDING 상태인 슬롯 요청 시 4xx 에러")
	void createReservation_AlreadyPending() throws Exception {
		log.info("=== [POST /api/v1/reservations 이미 PENDING 상태] 테스트 시작 ===");

		// Given
		log.info("[Given] 슬롯을 먼저 PENDING 상태로 변경");
		RoomTimeSlot slot = slotRepository.findByRoomIdAndSlotDateAndSlotTime(
						roomId, slotDate, slotTime)
				.orElseThrow();
		slot.markAsPending(999L);
		slotRepository.save(slot);
		log.info("[Given] - 슬롯 상태: PENDING, reservationId: 999");

		log.info("[Given] 동일 슬롯에 대한 새로운 예약 요청 생성");
		SlotReservationRequest request = new SlotReservationRequest(
				roomId, slotDate, slotTime, reservationId
		);
		String requestBody = objectMapper.writeValueAsString(request);
		log.info("[Given] - request: {}", request);

		// When & Then
		log.info("[When & Then] POST /api/v1/reservations 호출 시 4xx 에러 확인");
		mockMvc.perform(post("/api/v1/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andDo(print())
				.andExpect(status().is4xxClientError());

		log.info("=== [POST /api/v1/reservations 이미 PENDING 상태] 테스트 성공 ===");
	}

	@Test
	@DisplayName("POST /api/v1/reservations - 여러 예약 요청이 순차적으로 처리됨")
	void createReservation_MultipleRequests() throws Exception {
		log.info("=== [POST /api/v1/reservations 다중 요청] 테스트 시작 ===");

		// Given
		log.info("[Given] 3개의 슬롯 생성");
		LocalTime time11 = LocalTime.of(11, 0);
		LocalTime time12 = LocalTime.of(12, 0);
		slotRepository.save(RoomTimeSlot.available(roomId, slotDate, time11));
		slotRepository.save(RoomTimeSlot.available(roomId, slotDate, time12));

		// When & Then
		log.info("[When & Then] 3개의 예약 요청 순차 처리");
		for (int i = 0; i < 3; i++) {
			LocalTime time = LocalTime.of(10 + i, 0);
			Long resId = (long) (i + 1);

			log.info("[When] [요청 {}] POST /api/v1/reservations - time: {}, reservationId: {}",
					i + 1, time, resId);

			SlotReservationRequest request = new SlotReservationRequest(
					roomId, slotDate, time, resId
			);
			String requestBody = objectMapper.writeValueAsString(request);

			mockMvc.perform(post("/api/v1/reservations")
							.contentType(MediaType.APPLICATION_JSON)
							.content(requestBody))
					.andDo(print())
					.andExpect(status().isOk());

			log.info("[Then] [검증 {}] 슬롯 상태 확인", i + 1);
			RoomTimeSlot slot = slotRepository.findByRoomIdAndSlotDateAndSlotTime(
							roomId, slotDate, time)
					.orElseThrow();
			assertThat(slot.getStatus()).isEqualTo(SlotStatus.PENDING);
			assertThat(slot.getReservationId()).isEqualTo(resId);
			log.info("[Then] - ✓ 슬롯 상태 PENDING, reservationId: {}", resId);
		}

		log.info("=== [POST /api/v1/reservations 다중 요청] 테스트 성공 ===");
	}
}
