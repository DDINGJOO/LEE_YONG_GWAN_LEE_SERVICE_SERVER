package com.teambind.springproject.room.controller;

import com.teambind.springproject.room.command.application.ReservationApplicationService;
import com.teambind.springproject.room.command.dto.SlotReservationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
