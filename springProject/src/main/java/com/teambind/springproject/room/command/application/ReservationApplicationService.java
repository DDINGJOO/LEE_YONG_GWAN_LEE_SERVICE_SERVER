package com.teambind.springproject.room.command.application;

import com.teambind.springproject.message.publish.EventPublisher;
import com.teambind.springproject.room.command.domain.service.TimeSlotManagementService;
import com.teambind.springproject.room.command.dto.SlotReservationRequest;
import com.teambind.springproject.room.event.event.SlotReservedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 예약 생성 Application Service.
 *
 * 예약 생성 요청을 처리하고 슬롯을 PENDING 상태로 변경한 후 Kafka 이벤트를 발행한다.
 *
 * Hexagonal Architecture 적용:
 * Use Case 조율만 담당 (비즈니스 로직은 Domain Service에 위임)
 * Domain Service와 Infrastructure(Kafka)를 조율하여 트랜잭션 경계 관리
 * DIP (Dependency Inversion Principle) 준수
 */
@Slf4j
@Service
public class ReservationApplicationService {

	private final TimeSlotManagementService timeSlotManagementService;
	private final EventPublisher eventPublisher;

	public ReservationApplicationService(
			TimeSlotManagementService timeSlotManagementService,
			EventPublisher eventPublisher
	) {
		this.timeSlotManagementService = timeSlotManagementService;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * 예약 생성 요청을 처리한다.
	 *
	 * 플로우:
	 * 1. 도메인 서비스를 통해 슬롯을 PENDING 상태로 변경
	 * 2. Kafka로 SlotReservedEvent 발행
	 *
	 * 트랜잭션 경계:
	 * - DB 트랜잭션 커밋 후 Kafka 발행
	 * - Kafka 발행 실패 시 로깅만 수행 (보상 트랜잭션은 향후 구현 예정)
	 *
	 * @param request 예약 요청 (roomId, slotDate, slotTime, reservationId)
	 */
	@Transactional
	public void createReservation(SlotReservationRequest request) {
		log.info("Reservation creation requested: roomId={}, slotDate={}, slotTime={}, reservationId={}",
				request.roomId(), request.slotDate(), request.slotTime(), request.reservationId());

		// 1. 도메인 로직 실행: 슬롯을 PENDING 상태로 변경
		timeSlotManagementService.markSlotAsPending(
				request.roomId(),
				request.slotDate(),
				request.slotTime(),
				request.reservationId()
		);

		log.info("Slot marked as PENDING: roomId={}, slotDate={}, slotTime={}, reservationId={}",
				request.roomId(), request.slotDate(), request.slotTime(), request.reservationId());

		// 2. Kafka 이벤트 발행
		SlotReservedEvent event = SlotReservedEvent.of(
				request.roomId(),
				request.slotDate(),
				List.of(request.slotTime()),
				request.reservationId()
		);

		try {
			log.info("Publishing SlotReservedEvent to Kafka - topic: {}, eventType: {}, payload: {{roomId: {}, slotDate: {}, startTimes: {}, reservationId: {}, occurredAt: {}}}",
					event.getTopic(),
					event.getEventTypeName(),
					event.getRoomId(),
					event.getSlotDate(),
					event.getStartTimes(),
					event.getReservationId(),
					event.getOccurredAt());

			eventPublisher.publish(event);

			log.info("SlotReservedEvent published successfully: reservationId={}", request.reservationId());
		} catch (Exception e) {
			log.error("Failed to publish SlotReservedEvent: reservationId={}, error={}",
					request.reservationId(), e.getMessage(), e);
			// TODO: 보상 트랜잭션 또는 재시도 메커니즘 구현 필요
		}
	}
}