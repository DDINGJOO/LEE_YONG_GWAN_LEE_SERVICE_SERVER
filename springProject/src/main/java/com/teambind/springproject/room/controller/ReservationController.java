package com.teambind.springproject.room.controller;

import com.teambind.springproject.room.command.application.ReservationApplicationService;
import com.teambind.springproject.room.command.dto.MultiSlotReservationRequest;
import com.teambind.springproject.room.command.dto.MultiSlotReservationResponse;
import com.teambind.springproject.room.command.dto.SlotReservationRequest;
import com.teambind.springproject.room.mapper.TimeSlotMapper;
import com.teambind.springproject.room.query.application.TimeSlotQueryService;
import com.teambind.springproject.room.query.dto.AvailableSlotResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 예약 관리 컨트롤러.
 *
 * 예약 생성 요청을 처리하고 슬롯을 PENDING 상태로 변경한 후 Kafka 이벤트를 발행한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationApplicationService reservationService;
	private final TimeSlotQueryService queryService;
	private final TimeSlotMapper mapper;

	/**
	 * 특정 룸의 특정 날짜에 예약 가능한 슬롯 목록을 조회한다.
	 *
	 * @param roomId 룸 ID
	 * @param date   조회할 날짜
	 * @return 예약 가능한 슬롯 목록 (AVAILABLE 상태만)
	 */
	@GetMapping("/available-slots")
	public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
			@RequestParam Long roomId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		log.info("GET /api/v1/reservations/available-slots - roomId: {}, date: {}", roomId, date);

		List<AvailableSlotResponse> availableSlots = mapper.toAvailableSlotResponseList(
				queryService.getAvailableSlots(roomId, date)
		);

		log.info("Found {} available slots for roomId={}, date={}", availableSlots.size(), roomId, date);

		return ResponseEntity.ok(availableSlots);
	}

	/**
	 * 예약 생성 요청을 처리한다.
	 *
	 * 예약 가능한 슬롯을 PENDING 상태로 변경하고 Kafka로 이벤트를 발행한다.
	 *
	 * @param request 예약 요청 (roomId, slotDate, slotTime, reservationId)
	 * @return 성공 응답 (200 OK)
	 */
	@PostMapping
	public ResponseEntity<Void> createReservation(@RequestBody SlotReservationRequest request) {
		log.info("POST /api/v1/reservations - roomId: {}, slotDate: {}, slotTime: {}, reservationId: {}",
				request.roomId(), request.slotDate(), request.slotTime(), request.reservationId());

		reservationService.createReservation(request);

		log.info("Reservation created successfully: reservationId={}", request.reservationId());

		return ResponseEntity.ok().build();
	}

	/**
	 * 다중 슬롯 예약 생성 요청을 처리한다.
	 *
	 * 특정 날짜의 여러 시간 슬롯을 한 번에 예약 대기 상태로 변경한다.
	 * 예약 ID는 자동으로 생성되며, Pessimistic Lock을 통해 동시성 문제를 해결한다.
	 *
	 * @param request 다중 슬롯 예약 요청 (roomId, slotDate, slotTimes)
	 * @return 예약 응답 (reservationId, roomId, slotDate, reservedSlotTimes)
	 */
	@PostMapping("/multi")
	public ResponseEntity<MultiSlotReservationResponse> createMultiSlotReservation(
			@RequestBody MultiSlotReservationRequest request) {
		log.info("POST /api/v1/reservations/multi - roomId: {}, slotDate: {}, slotTimes: {}",
				request.roomId(), request.slotDate(), request.slotTimes());

		MultiSlotReservationResponse response = reservationService.createMultiSlotReservation(request);

		log.info("Multi-slot reservation created successfully: reservationId={}, reservedCount={}",
				response.reservationId(), response.reservedSlotTimes().size());

		return ResponseEntity.ok(response);
	}
}
