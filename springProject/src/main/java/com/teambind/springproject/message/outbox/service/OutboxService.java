package com.teambind.springproject.message.outbox.service;

import com.teambind.springproject.common.util.json.JsonUtil;
import com.teambind.springproject.message.event.Event;
import com.teambind.springproject.message.outbox.entity.OutboxMessage;
import com.teambind.springproject.message.outbox.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional Outbox Pattern을 위한 서비스.
 * <p>
 * 도메인 이벤트를 Outbox 테이블에 저장하여 DB 트랜잭션과 메시지 발행의 원자성을 보장합니다.
 * <p>
 * 처리 흐름:
 * 1. 도메인 이벤트를 JSON으로 직렬화
 * 2. OutboxMessage로 변환하여 DB에 저장
 * 3. @PostPersist에서 OutboxSavedEvent 발행 (향후 구현)
 * 4. ImmediatePublisher가 즉시 Kafka 발행 시도 (향후 구현)
 * 5. 실패 시 Scheduler가 재시도 (향후 구현)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

	private final OutboxMessageRepository outboxRepository;
	private final JsonUtil jsonUtil;

	/**
	 * 도메인 이벤트를 Outbox에 저장합니다.
	 * <p>
	 * 호출한 트랜잭션과 같은 트랜잭션 내에서 실행되어 원자성을 보장합니다.
	 *
	 * @param event          도메인 이벤트
	 * @param messageDto     Kafka 메시지 DTO
	 * @param aggregateType  Aggregate 타입
	 * @param aggregateId    Aggregate ID
	 */
	@Transactional
	public void saveToOutbox(Event event, Object messageDto, String aggregateType, String aggregateId) {
		// 1. Message DTO를 JSON으로 직렬화
		String payload = jsonUtil.toJson(messageDto);

		// 2. OutboxMessage 생성
		OutboxMessage outbox = OutboxMessage.create(
				aggregateType,
				aggregateId,
				event.getTopic(),
				event.getEventTypeName(),
				payload
		);

		// 3. DB에 저장 (현재 트랜잭션 내에서)
		outboxRepository.save(outbox);

		log.debug("Event saved to outbox: aggregateType={}, aggregateId={}, eventType={}",
				aggregateType, aggregateId, event.getEventTypeName());
	}

	/**
	 * Aggregate ID를 추출하는 헬퍼 메서드.
	 * <p>
	 * 이벤트 타입에 따라 적절한 ID를 추출합니다.
	 *
	 * @param event 도메인 이벤트
	 * @return Aggregate ID
	 */
	public static String extractAggregateId(Event event) {
		// 이벤트 타입에 따라 적절한 ID 추출
		// 예: SlotReservedEvent -> roomId, SlotCancelledEvent -> reservationId
		// 현재는 단순화를 위해 eventType을 반환
		// 실제 구현 시 각 이벤트별로 적절한 ID를 추출하도록 수정 필요
		return event.getEventTypeName();
	}
}