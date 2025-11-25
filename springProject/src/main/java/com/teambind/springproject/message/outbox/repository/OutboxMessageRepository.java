package com.teambind.springproject.message.outbox.repository;

import com.teambind.springproject.message.outbox.entity.OutboxMessage;
import com.teambind.springproject.message.outbox.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 메시지 영속성 Repository.
 */
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

	/**
	 * PENDING 상태의 메시지를 생성 시각 순으로 조회합니다.
	 * <p>
	 * Scheduler가 주기적으로 호출하여 미발행 메시지를 발행합니다.
	 *
	 * @return PENDING 상태의 메시지 목록
	 */
	@Query("SELECT o FROM OutboxMessage o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
	List<OutboxMessage> findPendingMessages();

	/**
	 * PENDING 상태이면서 재시도 횟수 제한 내의 메시지를 조회합니다.
	 *
	 * @param maxRetries 최대 재시도 횟수
	 * @return 재시도 가능한 PENDING 메시지 목록
	 */
	@Query("SELECT o FROM OutboxMessage o WHERE o.status = 'PENDING' AND o.retryCount < :maxRetries ORDER BY o.createdAt ASC")
	List<OutboxMessage> findRetryableMessages(@Param("maxRetries") int maxRetries);

	/**
	 * 특정 시각 이전에 발행 완료된 메시지를 조회합니다.
	 * <p>
	 * 정리 작업(cleanup)에 사용됩니다.
	 *
	 * @param beforeDate 기준 시각
	 * @return 삭제 대상 메시지 목록
	 */
	@Query("SELECT o FROM OutboxMessage o WHERE o.status = 'PUBLISHED' AND o.publishedAt < :beforeDate")
	List<OutboxMessage> findPublishedBefore(@Param("beforeDate") LocalDateTime beforeDate);

	/**
	 * 특정 Aggregate의 메시지를 조회합니다.
	 * <p>
	 * 디버깅 및 모니터링 용도입니다.
	 *
	 * @param aggregateType Aggregate 타입
	 * @param aggregateId   Aggregate ID
	 * @return 해당 Aggregate의 메시지 목록
	 */
	List<OutboxMessage> findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
			String aggregateType,
			String aggregateId
	);

	/**
	 * 특정 상태의 메시지 개수를 조회합니다.
	 * <p>
	 * 모니터링 메트릭 수집용입니다.
	 *
	 * @param status 조회할 상태
	 * @return 해당 상태의 메시지 개수
	 */
	long countByStatus(OutboxStatus status);
}