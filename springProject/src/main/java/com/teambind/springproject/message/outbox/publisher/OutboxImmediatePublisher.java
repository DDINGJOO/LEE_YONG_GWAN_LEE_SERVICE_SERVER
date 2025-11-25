package com.teambind.springproject.message.outbox.publisher;

import com.teambind.springproject.message.outbox.entity.OutboxMessage;
import com.teambind.springproject.message.outbox.event.OutboxSavedEvent;
import com.teambind.springproject.message.outbox.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.TimeUnit;

/**
 * Outbox 메시지를 즉시 Kafka로 발행하는 Publisher.
 * <p>
 * Hybrid Outbox Pattern의 핵심 컴포넌트로, OutboxSavedEvent를 수신하여
 * 트랜잭션 커밋 직후 즉시 Kafka 발행을 시도합니다.
 * <p>
 * 목표: 99%의 메시지를 0ms 지연으로 발행
 * <p>
 * 처리 흐름:
 * 1. OutboxMessage가 DB에 저장됨 (@PostPersist)
 * 2. OutboxSavedEvent 발행
 * 3. DB 트랜잭션 커밋
 * 4. @TransactionalEventListener(AFTER_COMMIT)가 이벤트 수신 (이 클래스)
 * 5. @Async로 비동기 Kafka 발행 시도 (타임아웃: 1초)
 * 6. 성공 시 PUBLISHED 마킹, 실패 시 Scheduler가 5초 내 재시도
 * <p>
 * 실패 처리:
 * - 즉시 발행 실패는 심각한 문제가 아님 (Scheduler가 백업)
 * - WARN 레벨 로그만 남기고 계속 진행
 * - 메시지 손실 없음 (Outbox에 이미 저장됨)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxImmediatePublisher {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final OutboxMessageRepository outboxRepository;

	/**
	 * OutboxSavedEvent를 수신하여 즉시 Kafka 발행을 시도합니다.
	 * <p>
	 * TransactionPhase.AFTER_COMMIT:
	 * - DB 트랜잭션 커밋 후 실행
	 * - Outbox 메시지가 확실히 DB에 저장된 상태
	 * <p>
	 * @Async("outboxExecutor"):
	 * - 비동기 실행으로 메인 트랜잭션 스레드를 블록하지 않음
	 * - 전용 스레드풀(outboxExecutor) 사용
	 * - 여러 메시지 동시 발행 가능
	 * <p>
	 * 타임아웃: 1초
	 * - ImmediatePublisher는 빠른 시도만 담당
	 * - 타임아웃 시 Scheduler가 2초 타임아웃으로 재시도
	 *
	 * @param event OutboxSavedEvent
	 */
	@Async("outboxExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOutboxSaved(OutboxSavedEvent event) {
		try {
			// Kafka 발행 시도 (타임아웃: 1초)
			kafkaTemplate.send(
					event.getTopic(),
					event.getKey(),          // 파티셔닝 키
					event.getPayload()
			).get(1, TimeUnit.SECONDS);

			// 성공 시 PUBLISHED 마킹 (새 트랜잭션)
			markAsPublished(event.getOutboxId());

			log.debug("Outbox message immediately published: id={}, topic={}",
					event.getOutboxId(), event.getTopic());

		} catch (Exception e) {
			// 실패 시 경고 로그만 남김 (Scheduler가 재시도할 것임)
			log.warn("Immediate publish failed for outbox id={}, will be retried by scheduler: {}",
					event.getOutboxId(), e.getMessage());
		}
	}

	/**
	 * Outbox 메시지를 PUBLISHED 상태로 마킹합니다.
	 * <p>
	 * REQUIRES_NEW: 새로운 트랜잭션에서 실행
	 * - 발행 성공/실패와 무관하게 독립적으로 커밋
	 * - 메인 비즈니스 트랜잭션에 영향 없음
	 *
	 * @param outboxId Outbox 메시지 ID
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	protected void markAsPublished(Long outboxId) {
		OutboxMessage message = outboxRepository.findById(outboxId)
				.orElseThrow(() -> new IllegalStateException(
						"Outbox message not found: " + outboxId));

		message.markAsPublished();
		outboxRepository.save(message);
	}
}