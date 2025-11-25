package com.teambind.springproject.message.outbox.scheduler;

import com.teambind.springproject.message.outbox.entity.OutboxMessage;
import com.teambind.springproject.message.outbox.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Outbox 메시지를 주기적으로 발행하는 Scheduler.
 * <p>
 * Transactional Outbox Pattern의 핵심 컴포넌트로, DB에 저장된 PENDING 메시지를
 * Kafka로 발행하여 최종 일관성을 보장합니다.
 * <p>
 * Hybrid Pattern에서의 역할:
 * - ImmediatePublisher가 실패한 메시지의 백업 발행
 * - 시스템 재시작 후 미발행 메시지 복구
 * - 네트워크 장애 등으로 실패한 메시지 재시도
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

	private final OutboxMessageRepository outboxRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;

	@Value("${outbox.scheduler.max-retries:5}")
	private int maxRetries;

	@Value("${outbox.scheduler.batch-size:100}")
	private int batchSize;

	/**
	 * PENDING 상태의 메시지를 주기적으로 발행합니다.
	 * <p>
	 * 실행 주기: 5초마다 (fixedDelay)
	 * - ImmediatePublisher 실패 후 빠른 재시도 (Hybrid Pattern 목표: 5초 이내)
	 * - 시스템 부하를 고려한 적절한 주기
	 * <p>
	 * 처리 흐름:
	 * 1. PENDING 상태이면서 재시도 제한 내의 메시지 조회
	 * 2. Kafka 발행 시도 (타임아웃: 2초)
	 * 3. 성공 시 PUBLISHED로 마킹
	 * 4. 실패 시 재시도 횟수 증가, 최대 초과 시 FAILED로 마킹
	 */
	@Scheduled(fixedDelay = 5000) // 5초마다 실행
	@Transactional
	public void publishPendingMessages() {
		try {
			// 1. 재시도 가능한 PENDING 메시지 조회 (배치 처리)
			List<OutboxMessage> pendingMessages = outboxRepository.findRetryableMessages(maxRetries);

			if (pendingMessages.isEmpty()) {
				return;
			}

			log.info("Publishing {} pending outbox messages", pendingMessages.size());

			int successCount = 0;
			int failedCount = 0;

			// 2. 각 메시지 발행 시도
			for (OutboxMessage message : pendingMessages) {
				try {
					// Kafka 발행 (타임아웃: 2초)
					kafkaTemplate.send(
							message.getTopic(),
							message.getAggregateId(), // 파티셔닝 키
							message.getPayload()
					).get(2, TimeUnit.SECONDS);

					// 성공 시 PUBLISHED로 마킹
					message.markAsPublished();
					outboxRepository.save(message);

					successCount++;

					log.debug("Outbox message published: id={}, eventType={}",
							message.getId(), message.getEventType());

				} catch (Exception e) {
					// 실패 시 재시도 횟수 증가
					message.incrementRetryCount();

					// 최대 재시도 초과 시 FAILED로 마킹
					if (message.exceedsMaxRetries(maxRetries)) {
						message.markAsFailed(
								String.format("Max retries exceeded: %s", e.getMessage())
						);
						failedCount++;

						log.error("Outbox message permanently failed: id={}, eventType={}, retries={}",
								message.getId(), message.getEventType(), message.getRetryCount(), e);
					} else {
						log.warn("Outbox message publish failed, will retry: id={}, eventType={}, retries={}",
								message.getId(), message.getEventType(), message.getRetryCount());
					}

					outboxRepository.save(message);
				}
			}

			log.info("Outbox scheduler completed: success={}, failed={}, total={}",
					successCount, failedCount, pendingMessages.size());

		} catch (Exception e) {
			log.error("Outbox scheduler encountered an error", e);
		}
	}

	/**
	 * 오래된 PUBLISHED 메시지를 정리합니다.
	 * <p>
	 * 실행 주기: 1시간마다
	 * - 7일 이상 지난 PUBLISHED 메시지 삭제
	 * - DB 공간 절약 및 성능 유지
	 */
	@Scheduled(fixedRate = 3600000) // 1시간마다 실행
	@Transactional
	public void cleanupPublishedMessages() {
		try {
			java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7);
			List<OutboxMessage> oldMessages = outboxRepository.findPublishedBefore(sevenDaysAgo);

			if (!oldMessages.isEmpty()) {
				outboxRepository.deleteAll(oldMessages);
				log.info("Cleaned up {} old outbox messages", oldMessages.size());
			}

		} catch (Exception e) {
			log.error("Outbox cleanup encountered an error", e);
		}
	}
}