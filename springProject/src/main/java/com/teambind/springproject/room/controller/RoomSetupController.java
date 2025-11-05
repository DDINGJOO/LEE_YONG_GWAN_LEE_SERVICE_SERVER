package com.teambind.springproject.room.controller;

import com.teambind.springproject.room.command.dto.ClosedDateSetupRequest;
import com.teambind.springproject.room.query.dto.ClosedDateSetupResponse;
import com.teambind.springproject.room.command.dto.RoomOperatingPolicySetupRequest;
import com.teambind.springproject.room.query.dto.RoomSetupResponse;
import com.teambind.springproject.room.query.dto.SlotGenerationStatusResponse;
import com.teambind.springproject.room.command.application.ClosedDateSetupApplicationService;
import com.teambind.springproject.room.command.application.RoomSetupApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 룸 초기 설정 컨트롤러.
 * <p>
 * 룸의 운영 정책 및 휴무일을 설정하고 슬롯을 관리한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/rooms/setup")
@RequiredArgsConstructor
public class RoomSetupController {

	private final RoomSetupApplicationService setupService;
	private final ClosedDateSetupApplicationService closedDateSetupService;

	/**
	 * 룸 운영 정책을 설정하고 슬롯 생성을 요청한다.
	 * <p>
	 * 운영 정책을 저장한 후, 슬롯 생성이 비동기로 처리되며, 즉시 202 Accepted 응답을 반환한다.
	 *
	 * @param request 운영 정책 설정 요청
	 * @return 설정 응답 (요청 ID 포함)
	 */
	@PostMapping
	public ResponseEntity<RoomSetupResponse> setupRoom(@RequestBody RoomOperatingPolicySetupRequest request) {
		log.info("POST /api/rooms/setup - roomId: {}, slots: {}",
				request.getRoomId(), request.getSlots().size());

		RoomSetupResponse response = setupService.setupRoom(request);

		log.info("Room setup request accepted: requestId={}", response.getRequestId());

		return ResponseEntity
				.status(HttpStatus.ACCEPTED)
				.body(response);
	}

	/**
	 * 슬롯 생성 상태를 조회한다.
	 *
	 * @param requestId 요청 ID
	 * @return 상태 응답
	 */
	@GetMapping("/{requestId}/status")
	public ResponseEntity<SlotGenerationStatusResponse> getStatus(@PathVariable String requestId) {
		log.info("GET /api/rooms/setup/{}/status", requestId);

		SlotGenerationStatusResponse response = setupService.getStatus(requestId);

		log.info("Slot generation status: requestId={}, status={}",
				requestId, response.getStatus());

		return ResponseEntity.ok(response);
	}

	/**
	 * 휴무일을 설정하고 기존 슬롯 업데이트를 요청한다.
	 * <p>
	 * 휴무일이 RoomOperatingPolicy에 추가되며,
	 * 기존 슬롯의 상태가 비동기로 CLOSED로 변경된다.
	 *
	 * @param request 휴무일 설정 요청
	 * @return 설정 응답 (요청 ID 포함)
	 */
	@PostMapping("/closed-dates")
	public ResponseEntity<ClosedDateSetupResponse> setupClosedDates(
			@RequestBody ClosedDateSetupRequest request) {
		log.info("POST /api/rooms/setup/closed-dates - roomId: {}, closedDateCount: {}",
				request.getRoomId(), request.getClosedDates().size());

		ClosedDateSetupResponse response = closedDateSetupService.setupClosedDates(request);

		log.info("Closed date setup request accepted: requestId={}", response.getRequestId());

		return ResponseEntity
				.status(HttpStatus.ACCEPTED)
				.body(response);
	}
}