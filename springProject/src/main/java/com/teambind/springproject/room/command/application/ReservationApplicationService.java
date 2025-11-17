package com.teambind.springproject.room.command.application;

import com.teambind.springproject.common.util.generator.PrimaryKeyGenerator;
import com.teambind.springproject.message.publish.EventPublisher;
import com.teambind.springproject.room.command.domain.service.TimeSlotManagementService;
import com.teambind.springproject.room.command.dto.MultiSlotReservationRequest;
import com.teambind.springproject.room.command.dto.MultiSlotReservationResponse;
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
	private final PrimaryKeyGenerator primaryKeyGenerator;

	public ReservationApplicationService(
			TimeSlotManagementService timeSlotManagementService,
			EventPublisher eventPublisher,
			PrimaryKeyGenerator primaryKeyGenerator
	) {
		this.timeSlotManagementService = timeSlotManagementService;
		this.eventPublisher = eventPublisher;
		this.primaryKeyGenerator = primaryKeyGenerator;
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
	public void  createReservation(SlotReservationRequest request) {
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

	/**
	 * 다중 슬롯 예약 생성 요청을 처리한다.
	 *
	 * 플로우:
	 * 1. 예약 ID 자동 생성 (Snowflake ID Generator)
	 * 2. Pessimistic Lock을 사용하여 여러 슬롯을 PENDING 상태로 변경
	 * 3. Kafka로 SlotReservedEvent 발행
	 *
	 * 동시성 제어:
	 * - SELECT ... FOR UPDATE로 슬롯을 잠금
	 * - 모든 슬롯이 AVAILABLE인지 검증 후 일괄 변경
	 * - 하나라도 예약 불가능하면 전체 롤백
	 *
	 * 트랜잭션 경계:
	 * - DB 트랜잭션 커밋 후 Kafka 발행
	 * - Kafka 발행 실패 시 로깅만 수행 (보상 트랜잭션은 향후 구현 예정)
	 *
	 * @param request 다중 슬롯 예약 요청 (roomId, slotDate, slotTimes)
	 * @return 예약 응답 (reservationId, roomId, slotDate, reservedSlotTimes)
	 */
	@Transactional
	public MultiSlotReservationResponse createMultiSlotReservation(MultiSlotReservationRequest request) {
		log.info("Multi-slot reservation requested: roomId={}, slotDate={}, slotTimes={}",
				request.roomId(), request.slotDate(), request.slotTimes());

		// 1. 예약 ID 생성 (Snowflake ID Generator)
		Long reservationId = primaryKeyGenerator.generateLongKey();
		log.info("Generated reservationId: {}", reservationId);

		// 2. 도메인 로직 실행: 여러 슬롯을 PENDING 상태로 변경 (Pessimistic Lock)
		int reservedCount = timeSlotManagementService.markMultipleSlotsAsPending(
				request.roomId(),
				request.slotDate(),
				request.slotTimes(),
				reservationId
		);

		log.info("Marked {} slots as PENDING: roomId={}, slotDate={}, reservationId={}",
				reservedCount, request.roomId(), request.slotDate(), reservationId);

		// 3. Kafka 이벤트 발행
		SlotReservedEvent event = SlotReservedEvent.of(
				request.roomId(),
				request.slotDate(),
				request.slotTimes(),
				reservationId
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

			log.info("SlotReservedEvent published successfully: reservationId={}", reservationId);
		} catch (Exception e) {
			log.error("Failed to publish SlotReservedEvent: reservationId={}, error={}",
					reservationId, e.getMessage(), e);
			// TODO: 보상 트랜잭션 또는 재시도 메커니즘 구현 필요
		}

		// 4. 응답 생성
		return new MultiSlotReservationResponse(
				reservationId,
				request.roomId(),
				request.slotDate(),
				request.slotTimes()
		);
	}
}
