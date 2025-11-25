package com.teambind.springproject.message.outbox.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Outbox 메시지가 DB에 저장되었을 때 발행되는 이벤트.
 * <p>
 * Hybrid Outbox Pattern의 핵심 컴포넌트로, @PostPersist 시점에 발행되어
 * OutboxImmediatePublisher가 즉시 Kafka 발행을 시도하도록 합니다.
 * <p>
 * 처리 흐름:
 * 1. OutboxMessage 저장 (DB 트랜잭션)
 * 2. @PostPersist에서 OutboxSavedEvent 발행
 * 3. 트랜잭션 커밋
 * 4. @TransactionalEventListener(AFTER_COMMIT)에서 즉시 Kafka 발행 시도
 * 5. 실패 시 Scheduler가 5초 내 재시도
 */
@Getter
@RequiredArgsConstructor
public class OutboxSavedEvent {

	/**
	 * Outbox 메시지 ID (발행 성공 시 PUBLISHED 마킹용)
	 */
	private final Long outboxId;

	/**
	 * Kafka Topic
	 */
	private final String topic;

	/**
	 * Kafka 파티셔닝 키 (Aggregate ID)
	 */
	private final String key;

	/**
	 * 메시지 Payload (JSON)
	 */
	private final String payload;
}