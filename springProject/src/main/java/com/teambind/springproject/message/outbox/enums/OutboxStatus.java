package com.teambind.springproject.message.outbox.enums;

/**
 * Outbox 메시지 상태.
 * <p>
 * Transactional Outbox Pattern에서 메시지의 발행 상태를 추적합니다.
 */
public enum OutboxStatus {
	/**
	 * 발행 대기 중.
	 * <p>
	 * DB 트랜잭션 커밋 후 Kafka 발행을 기다리는 상태입니다.
	 * Scheduler 또는 ImmediatePublisher가 이 상태의 메시지를 발행합니다.
	 */
	PENDING,

	/**
	 * 발행 완료.
	 * <p>
	 * Kafka로 성공적으로 발행된 상태입니다.
	 * 일정 기간 후 삭제 대상이 됩니다.
	 */
	PUBLISHED,

	/**
	 * 발행 실패.
	 * <p>
	 * 여러 번 재시도 후에도 실패한 상태입니다.
	 * 수동 조치가 필요할 수 있습니다.
	 */
	FAILED
}