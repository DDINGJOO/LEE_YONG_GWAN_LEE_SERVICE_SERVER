# ADR-004-1: Hybrid Outbox Pattern (즉시 발행 + Outbox 백업)

**Status**: Proposed
**Date**: 2025-01-19
**Decision Makers**: Teambind_dev_backend Team
**Technical Story**: Outbox 패턴의 발행 지연 최소화 (1초 → 0ms)
**Related**: [ADR-004: Transactional Outbox Pattern](004-transactional-outbox-pattern.md)

---

## Context

ADR-004에서 결정한 Transactional Outbox Pattern은 DB-Kafka 일관성을 보장하지만, 1초의 발행 지연이 발생합니다.

### 현재 방식의 문제점

```
트랜잭션 커밋 → Outbox 저장 → (1초 대기) → Scheduler Polling → Kafka 발행
                                  ↑
                            사용자는 1초간 대기
```

**사용자 경험 영향**:
- 예약 요청 후 1초 뒤 결제 프로세스 시작
- 실시간 알림이 1초 늦게 전송
- API 응답은 빠르지만 후속 처리가 지연

### 요구사항

1. **즉시 발행**: 정상 상황에서는 지연 없이 발행 (< 50ms)
2. **안정성 보장**: Kafka 장애 시에도 이벤트 손실 방지
3. **정확히 한 번**: 중복 발행 방지
4. **복잡도 최소화**: 과도한 엔지니어링 지양

---

## Decision Outcome

**Hybrid Approach: 즉시 발행 + Outbox 백업**

### 핵심 아이디어

```
┌─────────────────────────────────────────────────────────┐
│              @Transactional                             │
│  public void createReservation(...) {                  │
│    // 1. 도메인 로직 실행                               │
│    timeSlotService.markAsPending(...);                 │
│                                                         │
│    // 2. Outbox 저장 (PENDING 상태)                    │
│    OutboxMessage outbox = new OutboxMessage(event);    │
│    outboxRepository.save(outbox);                      │
│  }                                                      │
│  // ← DB 트랜잭션 커밋                                  │
└─────────────────────────────────────────────────────────┘
                    ↓ @TransactionalEventListener
┌─────────────────────────────────────────────────────────┐
│  @TransactionalEventListener(AFTER_COMMIT)              │
│  public void onOutboxSaved(OutboxSavedEvent event) {   │
│    try {                                                │
│      // 3. 즉시 Kafka 발행 시도                        │
│      kafkaTemplate.send(event).get(1, SECONDS);       │
│                                                         │
│      // 4. 성공 시 Outbox 상태 PUBLISHED로 변경       │
│      outboxRepository.updateStatus(                    │
│        event.getId(), PUBLISHED                        │
│      );                                                 │
│    } catch (Exception e) {                             │
│      // 5. 실패 시 Outbox는 PENDING 유지               │
│      // Scheduler가 나중에 재발행                      │
│      log.warn("Immediate publish failed...");          │
│    }                                                    │
│  }                                                      │
└─────────────────────────────────────────────────────────┘
```

**결과**:
- **정상 케이스** (99%): 트랜잭션 커밋 직후 즉시 발행 (10-50ms) ✅
- **장애 케이스** (1%): Outbox에서 재발행 (최대 1초 지연) ✅

---

## Implementation Details

### 1. Outbox 저장 시 Spring Event 발행

```java
// OutboxMessage.java
@Entity
@Table(name = "outbox_messages")
@EntityListeners(OutboxEntityListener.class) // ← Entity Listener 등록
public class OutboxMessage {

    @Id
    private String id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status; // PENDING, PUBLISHED, FAILED

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime publishedAt;

    // ... getters
}
```

```java
// OutboxEntityListener.java
@Component
public class OutboxEntityListener {

    private static ApplicationEventPublisher eventPublisher;

    @Autowired
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        OutboxEntityListener.eventPublisher = publisher;
    }

    @PostPersist
    public void onOutboxSaved(OutboxMessage outbox) {
        // Outbox 저장 직후 Spring Event 발행
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new OutboxSavedEvent(outbox));
        }
    }
}
```

```java
// OutboxSavedEvent.java
public class OutboxSavedEvent {

    private final String outboxId;
    private final String topic;
    private final String payload;

    public OutboxSavedEvent(OutboxMessage outbox) {
        this.outboxId = outbox.getId();
        this.topic = extractTopic(outbox.getPayload()); // JSON에서 topic 추출
        this.payload = outbox.getPayload();
    }

    // getters...
}
```

### 2. 트랜잭션 커밋 직후 즉시 발행

```java
// ImmediateOutboxPublisher.java
@Component
@RequiredArgsConstructor
@Slf4j
public class ImmediateOutboxPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxMessageRepository outboxRepository;

    /**
     * 트랜잭션 커밋 직후 Outbox 메시지를 즉시 발행한다.
     *
     * AFTER_COMMIT: DB 트랜잭션이 성공적으로 커밋된 직후 실행
     * - Outbox는 이미 DB에 저장된 상태
     * - Kafka 발행 실패 시에도 Outbox에 남아있음
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("outboxExecutor") // 비동기 실행 (API 응답 지연 방지)
    public void onOutboxSaved(OutboxSavedEvent event) {
        log.info("Attempting immediate publish: outboxId={}, topic={}",
            event.getOutboxId(), event.getTopic());

        try {
            // Kafka 발행 (최대 1초 대기)
            SendResult<String, String> result = kafkaTemplate
                .send(event.getTopic(), event.getPayload())
                .get(1, TimeUnit.SECONDS);

            log.info("Immediate publish succeeded: outboxId={}, partition={}, offset={}",
                event.getOutboxId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());

            // 발행 성공 시 Outbox 상태 업데이트
            markAsPublished(event.getOutboxId());

        } catch (TimeoutException e) {
            log.warn("Immediate publish timeout (>1s): outboxId={}. Will retry via scheduler.",
                event.getOutboxId());
            // Outbox는 PENDING 상태로 남음 → Scheduler가 재발행

        } catch (Exception e) {
            log.warn("Immediate publish failed: outboxId={}, error={}. Will retry via scheduler.",
                event.getOutboxId(), e.getMessage());
            // Outbox는 PENDING 상태로 남음 → Scheduler가 재발행
        }
    }

    /**
     * Outbox 상태를 PUBLISHED로 변경한다.
     * 별도 트랜잭션으로 실행 (즉시 발행 성공 시에만 호출)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markAsPublished(String outboxId) {
        outboxRepository.findById(outboxId).ifPresent(outbox -> {
            outbox.markAsPublished();
            outboxRepository.save(outbox);
            log.debug("Outbox marked as published: {}", outboxId);
        });
    }
}
```

### 3. 비동기 실행 설정 (API 응답 지연 방지)

```java
// AsyncConfig.java
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Outbox 즉시 발행 전용 Executor.
     * API 응답을 블로킹하지 않도록 비동기 실행.
     */
    @Bean(name = "outboxExecutor")
    public Executor outboxExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("outbox-immediate-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 4. Application Service 수정 (변경 최소화)

```java
// ReservationApplicationService.java
@Service
@RequiredArgsConstructor
public class ReservationApplicationService {

    private final TimeSlotManagementService timeSlotManagementService;
    private final OutboxMessageRepository outboxRepository;

    @Transactional
    public void createReservation(SlotReservationRequest request) {
        log.info("Reservation creation requested: roomId={}, slotDate={}, slotTime={}, reservationId={}",
            request.roomId(), request.slotDate(), request.slotTime(), request.reservationId());

        // 1. 도메인 로직 실행
        timeSlotManagementService.markSlotAsPending(
            request.roomId(), request.slotDate(), request.slotTime(), request.reservationId()
        );

        // 2. Outbox에 이벤트 저장 (기존과 동일)
        SlotReservedEvent event = SlotReservedEvent.of(
            request.roomId().toString(),
            request.slotDate(),
            List.of(request.slotTime()),
            request.reservationId().toString()
        );

        OutboxMessage outboxMessage = OutboxMessage.from(event);
        outboxRepository.save(outboxMessage);

        log.info("SlotReservedEvent saved to outbox: reservationId={}, outboxId={}",
            request.reservationId(), outboxMessage.getId());

        // ← 트랜잭션 커밋 시:
        //   1. Outbox 저장 완료
        //   2. @PostPersist → OutboxSavedEvent 발행
        //   3. @TransactionalEventListener → 즉시 Kafka 발행 시도
    }
}
```

**변경 사항**:
- 기존 `EventPublisher.publish()` 호출 제거
- `OutboxMessage` 저장만 수행
- 나머지는 자동 처리 (Entity Listener + TransactionEventListener)

### 5. Outbox Relay 스케줄러 (백업 발행)

```java
// OutboxMessageRelay.java
@Component
@RequiredArgsConstructor
public class OutboxMessageRelay {

    private final OutboxMessageRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * PENDING 상태의 Outbox 메시지를 재발행한다.
     *
     * 역할:
     * - 즉시 발행에 실패한 메시지 재발행
     * - 애플리케이션 크래시 등으로 누락된 메시지 발행
     *
     * 주기: 5초마다 실행 (즉시 발행 실패 시 빠른 복구)
     */
    @Scheduled(fixedDelay = 5000) // 5초
    @SchedulerLock(name = "OutboxMessageRelay", lockAtMostFor = "30s", lockAtLeastFor = "3s")
    public void publishPendingMessages() {
        // 생성 후 5초 이상 지난 PENDING 메시지만 재발행
        // (즉시 발행 시도 중인 메시지 제외)
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(5);

        List<OutboxMessage> pendingMessages = outboxRepository
            .findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                threshold,
                PageRequest.of(0, 100)
            );

        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("Retrying {} pending outbox messages", pendingMessages.size());

        for (OutboxMessage message : pendingMessages) {
            try {
                kafkaTemplate.send(message.getTopic(), message.getPayload()).get();

                message.markAsPublished();
                outboxRepository.save(message);

                log.info("Outbox message published (retry): id={}, eventType={}",
                    message.getId(), message.getEventType());

            } catch (Exception e) {
                log.error("Failed to publish outbox message (retry): id={}, error={}",
                    message.getId(), e.getMessage());

                message.incrementRetry();
                outboxRepository.save(message);
            }
        }
    }
}
```

### 6. Repository 메서드 추가

```java
// OutboxMessageRepository.java
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, String> {

    // 기존: 모든 PENDING 메시지 조회
    List<OutboxMessage> findByStatusOrderByCreatedAtAsc(
        OutboxStatus status,
        Pageable pageable
    );

    // 신규: 특정 시간 이전 PENDING 메시지 조회 (즉시 발행 실패 메시지)
    List<OutboxMessage> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
        OutboxStatus status,
        LocalDateTime threshold,
        Pageable pageable
    );

    // 정리 배치용
    int deleteByStatusAndPublishedAtBefore(OutboxStatus status, LocalDateTime threshold);
}
```

---

## 동작 플로우 비교

### Case 1: 정상 케이스 (Kafka 정상)

```
T0    API 요청

T1    @Transactional 시작
      └─ 슬롯 PENDING 변경
      └─ Outbox 저장 (PENDING)

T2    트랜잭션 커밋 ✅
      └─ @PostPersist 트리거
         └─ OutboxSavedEvent 발행

T3    @TransactionalEventListener 실행
      └─ Kafka 즉시 발행 시도

T4    Kafka 발행 성공 ✅
      └─ Outbox 상태 PUBLISHED로 변경

T5    API 응답 (총 소요 시간: 50ms)

결과: 지연 없음 ✅
```

### Case 2: Kafka 일시 장애

```
T0    API 요청

T1    @Transactional 시작
      └─ 슬롯 PENDING 변경
      └─ Outbox 저장 (PENDING)

T2    트랜잭션 커밋 ✅

T3    @TransactionalEventListener 실행
      └─ Kafka 즉시 발행 시도

T4    Kafka 발행 실패 ❌ (timeout)
      └─ Outbox는 PENDING 상태 유지

T5    API 응답 (총 소요 시간: 1050ms)
      ↑ 비동기 실행이므로 실제로는 50ms에 응답 가능

--- 5초 후 ---

T10   Scheduler 실행
      └─ PENDING 메시지 조회
      └─ Kafka 재발행 성공 ✅
      └─ Outbox 상태 PUBLISHED로 변경

결과: 5초 지연으로 복구 ✅
```

### Case 3: 애플리케이션 크래시

```
T0    API 요청

T1    @Transactional 시작
      └─ 슬롯 PENDING 변경
      └─ Outbox 저장 (PENDING)

T2    트랜잭션 커밋 ✅

T3    애플리케이션 크래시 💥
      └─ @TransactionalEventListener 실행 안 됨

--- 재시작 후 ---

T100  Scheduler 실행
      └─ PENDING 메시지 조회
      └─ Kafka 발행 성공 ✅
      └─ Outbox 상태 PUBLISHED로 변경

결과: 누락 없이 발행 ✅
```

---

## 장단점 분석

### 장점

1. **즉시 발행**: 정상 케이스에서 지연 없음 (10-50ms)
2. **안정성 보장**: Kafka 장애 시 Outbox에서 재발행
3. **정확히 한 번**: Outbox 상태 관리로 중복 방지
4. **비침투적**: Application Service 변경 최소 (기존 코드와 유사)
5. **점진적 적용**: 기존 Outbox 패턴에서 작은 변경만으로 적용 가능

### 단점

1. **복잡도 증가**: Entity Listener + TransactionEventListener + Scheduler
   - **완화**: 각 컴포넌트 역할이 명확하여 관리 용이

2. **비동기 실행 오버헤드**: ThreadPool 관리 필요
   - **완화**: Pool 크기 최적화 (Core 5, Max 10)

3. **API 응답 시간 미세 증가**: 비동기 작업 시작 시간 포함
   - **완화**: @Async로 즉시 응답 (실제 영향 < 10ms)

### Trade-offs

- **복잡도 vs 실시간성**: 약간의 복잡도 증가를 허용하여 지연 제거
- **리소스 vs 성능**: ThreadPool 추가 사용하여 즉시 발행 확보

---

## Validation

### 1. 즉시 발행 성공 테스트

```java
@SpringBootTest
@EmbeddedKafka
class HybridOutboxPatternTest {

    @Autowired
    private ReservationApplicationService reservationService;

    @Autowired
    private OutboxMessageRepository outboxRepository;

    @Autowired
    private KafkaConsumer<String, String> kafkaConsumer;

    @Test
    void createReservation_ShouldPublishImmediately() throws Exception {
        // Given
        SlotReservationRequest request = new SlotReservationRequest(...);

        // When
        long start = System.currentTimeMillis();
        reservationService.createReservation(request);
        long duration = System.currentTimeMillis() - start;

        // Then: API 응답 빠름 (< 100ms)
        assertThat(duration).isLessThan(100);

        // Then: Kafka 메시지 수신 (즉시 발행 검증)
        Thread.sleep(100); // 비동기 처리 대기
        ConsumerRecord<String, String> record = kafkaConsumer.poll(Duration.ofSeconds(2));
        assertThat(record).isNotNull();
        assertThat(record.value()).contains("SlotReserved");

        // Then: Outbox 상태 PUBLISHED
        Thread.sleep(100); // 상태 업데이트 대기
        List<OutboxMessage> messages = outboxRepository.findByStatusOrderByCreatedAtAsc(
            OutboxStatus.PUBLISHED, PageRequest.of(0, 10)
        );
        assertThat(messages).hasSize(1);
    }
}
```

### 2. Kafka 장애 시 재발행 테스트

```java
@Test
void whenKafkaDown_ShouldRetryViaScheduler() throws Exception {
    // Given: Kafka 다운 시뮬레이션
    kafkaContainer.stop();

    SlotReservationRequest request = new SlotReservationRequest(...);

    // When: 예약 생성
    reservationService.createReservation(request);

    Thread.sleep(2000); // 즉시 발행 실패 대기

    // Then: Outbox는 PENDING 상태
    List<OutboxMessage> pending = outboxRepository.findByStatusOrderByCreatedAtAsc(
        OutboxStatus.PENDING, PageRequest.of(0, 10)
    );
    assertThat(pending).hasSize(1);

    // When: Kafka 복구
    kafkaContainer.start();
    Thread.sleep(6000); // Scheduler 실행 대기 (5초 주기)

    // Then: Scheduler가 재발행
    List<OutboxMessage> published = outboxRepository.findByStatusOrderByCreatedAtAsc(
        OutboxStatus.PUBLISHED, PageRequest.of(0, 10)
    );
    assertThat(published).hasSize(1);
}
```

### 3. 성능 벤치마크

```java
@Test
void publishPerformance_ShouldBeFast() {
    // Given: 100개 예약 생성
    List<SlotReservationRequest> requests = prepareRequests(100);

    // When
    long start = System.currentTimeMillis();
    requests.forEach(reservationService::createReservation);
    long duration = System.currentTimeMillis() - start;

    // Then: 평균 50ms 이내 (즉시 발행 오버헤드 포함)
    assertThat(duration / 100).isLessThan(50);
}
```

---

## 설정 값

```yaml
# application.yaml
outbox:
  immediate:
    enabled: true                      # Hybrid 모드 활성화
    timeout: 1000                      # Kafka 발행 타임아웃 (ms)
    async:
      corePoolSize: 5
      maxPoolSize: 10
      queueCapacity: 100

  relay:
    fixedDelay: 5000                   # 5초마다 재발행 (기존 1초 → 5초)
    retryThreshold: 5                  # 5초 이상 PENDING인 메시지만 재발행
    batchSize: 100

  cleanup:
    retentionDays: 7
    cron: "0 0 2 * * *"
```

---

## Migration Plan

### Phase 1: Hybrid 모드 도입 (1주)

1. `OutboxEntityListener` 구현
2. `ImmediateOutboxPublisher` 구현
3. `AsyncConfig` 설정
4. 단위/통합 테스트 작성

### Phase 2: 점진적 배포 (2주)

1. **Week 1**: 개발 환경 배포 및 모니터링
   - 즉시 발행 성공률 측정
   - 재발행 빈도 측정

2. **Week 2**: 스테이징 환경 배포
   - 부하 테스트 수행
   - 장애 시나리오 테스트

### Phase 3: 프로덕션 적용 (1주)

1. **카나리 배포**: 10% 트래픽에만 적용
2. **모니터링**: 24시간 동안 메트릭 확인
3. **전체 배포**: 문제 없으면 100% 적용

---

## Monitoring

### 핵심 메트릭

```java
@Component
@RequiredArgsConstructor
public class OutboxMetrics {

    private final MeterRegistry meterRegistry;

    public void recordImmediatePublishSuccess() {
        meterRegistry.counter("outbox.immediate.success").increment();
    }

    public void recordImmediatePublishFailure() {
        meterRegistry.counter("outbox.immediate.failure").increment();
    }

    public void recordSchedulerRetry() {
        meterRegistry.counter("outbox.scheduler.retry").increment();
    }

    public void recordPublishLatency(long millis) {
        meterRegistry.timer("outbox.publish.latency").record(millis, TimeUnit.MILLISECONDS);
    }
}
```

### Grafana Dashboard

```
즉시 발행 성공률:
  outbox.immediate.success / (outbox.immediate.success + outbox.immediate.failure)
  → 목표: 99% 이상

평균 발행 지연:
  avg(outbox.publish.latency)
  → 목표: 50ms 이하

Scheduler 재발행 빈도:
  rate(outbox.scheduler.retry[5m])
  → 목표: < 1/min (정상 상황)
```

---

## Consequences

### Positive

- **실시간성 확보**: 99% 케이스에서 지연 없음 (0ms)
- **안정성 유지**: Outbox 백업으로 이벤트 손실 방지
- **사용자 경험 개선**: 결제/알림 지연 제거
- **점진적 적용 가능**: 기존 코드 최소 변경

### Negative

- **복잡도 증가**: 3개 컴포넌트 추가 (Entity Listener, TransactionEventListener, Scheduler)
- **리소스 사용 증가**: ThreadPool 추가

### Risks & Mitigation

**Risk 1: 비동기 작업으로 인한 메모리 부족**
- **완화**: ThreadPool 크기 제한 (Max 10)
- **완화**: RejectedExecutionHandler로 폴백 처리

**Risk 2: Outbox 상태 업데이트 실패 (PUBLISHED 변경 실패)**
- **완화**: 별도 트랜잭션 (REQUIRES_NEW)으로 독립 실행
- **완화**: Scheduler가 재발행 시도하므로 중복 발행 가능 (Kafka Consumer에서 멱등성 보장 필요)

---

## References

- [Spring @TransactionalEventListener](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/event/TransactionalEventListener.html)
- [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Spring @Async](https://spring.io/guides/gs/async-method/)
- ADR-004: Transactional Outbox Pattern

---

## Future Considerations

**Phase 1 (현재)**:
- Hybrid 모드 구현 및 검증

**Phase 2**:
- 즉시 발행 성공률 모니터링
- 성능 튜닝 (Timeout, Pool Size)

**Phase 3**:
- Kafka Consumer 멱등성 강화 (중복 발행 대비)
- Circuit Breaker 추가 (Kafka 반복 장애 시 자동 우회)

**Phase 4 (장기)**:
- CDC(Debezium) 전환 검토 (트래픽 급증 시)

---

**Maintained by**: Teambind_dev_backend Team
**Lead Developer**: DDINGJOO