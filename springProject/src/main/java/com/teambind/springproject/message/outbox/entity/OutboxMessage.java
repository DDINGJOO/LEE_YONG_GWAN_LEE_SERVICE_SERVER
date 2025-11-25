package com.teambind.springproject.message.outbox.entity;

import com.teambind.springproject.common.event.DomainEventPublisher;
import com.teambind.springproject.message.outbox.enums.OutboxStatus;
import com.teambind.springproject.message.outbox.event.OutboxSavedEvent;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Transactional Outbox Pattern을 위한 메시지 저장소.
 * <p>
 * 도메인 이벤트를 Kafka로 발행하기 전에 DB에 먼저 저장하여
 * DB 트랜잭션과 메시지 발행의 원자성을 보장합니다.
 * <p>
 * Hybrid Outbox Pattern 적용:
 * - @PostPersist에서 OutboxSavedEvent 발행
 * - ImmediatePublisher가 즉시 Kafka 발행 시도
 * - 실패 시 Scheduler가 재시도
 */
@Entity
@Table(
		name = "outbox_messages",
		indexes = {
				@Index(name = "idx_status_created", columnList = "status,created_at"),
				@Index(name = "idx_aggregate", columnList = "aggregate_type,aggregate_id")
		}
)
public class OutboxMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Aggregate 타입 (예: RoomTimeSlot, Reservation).
	 */
	@Column(name = "aggregate_type", nullable = false, length = 100)
	private String aggregateType;

	/**
	 * Aggregate ID (Kafka 파티셔닝 키로 사용).
	 */
	@Column(name = "aggregate_id", nullable = false, length = 100)
	private String aggregateId;

	/**
	 * Kafka 토픽명.
	 */
	@Column(name = "topic", nullable = false, length = 100)
	private String topic;

	/**
	 * 이벤트 타입 (예: SlotReserved, SlotCancelled).
	 */
	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	/**
	 * JSON 형식의 메시지 페이로드.
	 */
	@Column(name = "payload", nullable = false, columnDefinition = "TEXT")
	private String payload;

	/**
	 * 발행 상태.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OutboxStatus status;

	/**
	 * 생성 시각 (Outbox에 저장된 시각).
	 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * 발행 완료 시각.
	 */
	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	/**
	 * 재시도 횟수.
	 */
	@Column(name = "retry_count", nullable = false)
	private Integer retryCount;

	/**
	 * 마지막 에러 메시지.
	 */
	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	protected OutboxMessage() {
		// JPA를 위한 기본 생성자
	}

	private OutboxMessage(
			String aggregateType,
			String aggregateId,
			String topic,
			String eventType,
			String payload
	) {
		this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType must not be null");
		this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
		this.topic = Objects.requireNonNull(topic, "topic must not be null");
		this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
		this.payload = Objects.requireNonNull(payload, "payload must not be null");
		this.status = OutboxStatus.PENDING;
		this.createdAt = LocalDateTime.now();
		this.retryCount = 0;
	}

	/**
	 * 새로운 Outbox 메시지를 생성합니다.
	 *
	 * @param aggregateType Aggregate 타입
	 * @param aggregateId   Aggregate ID
	 * @param topic         Kafka 토픽
	 * @param eventType     이벤트 타입
	 * @param payload       JSON 페이로드
	 * @return 생성된 OutboxMessage
	 */
	public static OutboxMessage create(
			String aggregateType,
			String aggregateId,
			String topic,
			String eventType,
			String payload
	) {
		return new OutboxMessage(aggregateType, aggregateId, topic, eventType, payload);
	}

	/**
	 * Outbox 메시지가 DB에 저장된 직후 OutboxSavedEvent를 발행합니다.
	 * <p>
	 * Hybrid Outbox Pattern의 핵심 메커니즘:
	 * 1. DB 트랜잭션 커밋
	 * 2. @PostPersist 콜백 실행 (이 메서드)
	 * 3. OutboxSavedEvent 발행
	 * 4. @TransactionalEventListener(AFTER_COMMIT)가 이벤트 수신
	 * 5. ImmediatePublisher가 즉시 Kafka 발행 시도
	 * 6. 실패 시 Scheduler가 5초 내 재시도
	 */
	@PostPersist
	private void publishSavedEvent() {
		DomainEventPublisher.publish(
				new OutboxSavedEvent(this.id, this.topic, this.aggregateId, this.payload)
		);
	}

	/**
	 * 발행 완료 상태로 변경합니다.
	 */
	public void markAsPublished() {
		this.status = OutboxStatus.PUBLISHED;
		this.publishedAt = LocalDateTime.now();
	}

	/**
	 * 발행 실패 상태로 변경합니다.
	 *
	 * @param errorMessage 에러 메시지
	 */
	public void markAsFailed(String errorMessage) {
		this.status = OutboxStatus.FAILED;
		this.errorMessage = errorMessage;
	}

	/**
	 * 재시도 횟수를 증가시킵니다.
	 */
	public void incrementRetryCount() {
		this.retryCount++;
	}

	/**
	 * 최대 재시도 횟수를 초과했는지 확인합니다.
	 *
	 * @param maxRetries 최대 재시도 횟수
	 * @return 초과 여부
	 */
	public boolean exceedsMaxRetries(int maxRetries) {
		return this.retryCount >= maxRetries;
	}

	// Getters
	public Long getId() {
		return id;
	}

	public String getAggregateType() {
		return aggregateType;
	}

	public String getAggregateId() {
		return aggregateId;
	}

	public String getTopic() {
		return topic;
	}

	public String getEventType() {
		return eventType;
	}

	public String getPayload() {
		return payload;
	}

	public OutboxStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getPublishedAt() {
		return publishedAt;
	}

	public Integer getRetryCount() {
		return retryCount;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof OutboxMessage that)) return false;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}