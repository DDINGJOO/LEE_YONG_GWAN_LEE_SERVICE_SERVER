# ADR-004: Transactional Outbox Pattern 적용

**Status**: Proposed
**Date**: 2025-01-19
**Decision Makers**: Teambind_dev_backend Team
**Technical Story**: DB-Kafka 트랜잭션 일관성 보장

---

## Context

현재 예약 생성 프로세스는 DB 트랜잭션과 Kafka 발행을 순차적으로 수행하는 Dual Write 패턴을 사용하고 있습니다.

### 현재 구현 (ReservationApplicationService)

```java
@Transactional
public void createReservation(SlotReservationRequest request) {
    // 1. DB 트랜잭션 커밋
    timeSlotManagementService.markSlotAsPending(
        request.roomId(), request.slotDate(), request.slotTime(), request.reservationId()
    );

    // 2. Kafka 발행 (트랜잭션 외부)
    try {
        eventPublisher.publish(event);
    } catch (Exception e) {
        log.error("Failed to publish SlotReservedEvent...");
        // TODO: 보상 트랜잭션 또는 재시도 메커니즘 구현 필요
    }
}
```

### 문제점

**시나리오 1: Kafka 발행 실패**
```
1. DB 커밋 성공 → 슬롯 상태: PENDING ✅
2. Kafka 발행 실패 (네트워크 장애) ❌
3. Payment 서비스가 이벤트를 받지 못함
4. 결제 프로세스 시작 안 됨
5. 슬롯이 PENDING 상태로 영구 잠김 (만료 스케줄러 실행 전까지)
```

**시나리오 2: Kafka 발행 후 애플리케이션 크래시**
```
1. DB 커밋 성공 ✅
2. Kafka 발행 성공 ✅
3. 애플리케이션 크래시 (로그 누락)
4. 재처리 메커니즘 없음
```

**영향도**:
- 비즈니스 크리티컬 (예약 시스템 핵심 플로우)
- 사용자 경험 저하 (예약했다고 생각했지만 실제론 처리 안 됨)
- 매출 손실 가능성
- PENDING 만료 스케줄러(10분 주기)가 복구하지만 사용자는 이미 이탈

### 요구사항

1. **원자성 보장**: DB 변경과 Kafka 발행이 함께 성공하거나 함께 실패해야 함
2. **정확히 한 번 발행**: 동일 이벤트가 중복 발행되지 않아야 함
3. **장애 복구**: 네트워크 장애나 Kafka 다운 시에도 이벤트가 최종적으로 발행되어야 함
4. **성능**: 기존 동기 발행 대비 큰 성능 저하 없어야 함

---

## Decision Drivers

- MSA 환경에서 서비스 간 데이터 일관성 필수
- Kafka 장애가 예약 생성 프로세스를 블로킹해서는 안 됨
- 분산 트랜잭션(2PC)은 성능 저하 및 복잡도 과다
- 이벤트 발행 보장이 비즈니스 요구사항
- 팀의 Spring Boot + Kafka 경험 활용

---

## Considered Options

### Option 1: Transactional Outbox Pattern (권장)

**개념**:
```
┌─────────────────────────────────────────────┐
│          Application Service                │
│                                             │
│  @Transactional                             │
│  public void createReservation(...) {      │
│    // 1. 슬롯 상태 변경                     │
│    timeSlotService.markAsPending(...);     │
│                                             │
│    // 2. Outbox 테이블에 이벤트 저장       │
│    outboxRepository.save(                  │
│      new OutboxMessage(event)              │
│    );                                       │
│  } // ← 단일 DB 트랜잭션 커밋              │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│      Outbox Message Relay (Scheduler)       │
│                                             │
│  @Scheduled(fixedDelay = 1000)              │
│  public void publishOutboxMessages() {     │
│    List<OutboxMessage> pending =           │
│      outboxRepository.findPending();       │
│                                             │
│    pending.forEach(msg -> {                │
│      kafkaTemplate.send(msg.toEvent());   │
│      msg.markAsPublished();                │
│    });                                      │
│  }                                          │
└─────────────────────────────────────────────┘
```

**구현 예시**:

*1. Outbox Entity*:
```java
@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    private String id; // UUID

    @Column(nullable = false)
    private String aggregateType; // "RoomTimeSlot"

    @Column(nullable = false)
    private String aggregateId; // "roomId-slotDate-slotTime"

    @Column(nullable = false)
    private String eventType; // "SlotReserved"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON 직렬화된 이벤트

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status; // PENDING, PUBLISHED, FAILED

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime publishedAt;

    @Column
    private Integer retryCount = 0;

    public static OutboxMessage from(Event event) {
        OutboxMessage message = new OutboxMessage();
        message.id = UUID.randomUUID().toString();
        message.aggregateType = event.getClass().getSimpleName();
        message.eventType = event.getEventTypeName();
        message.payload = JsonUtil.toJson(event);
        message.status = OutboxStatus.PENDING;
        message.createdAt = LocalDateTime.now();
        return message;
    }

    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void incrementRetry() {
        this.retryCount++;
        if (retryCount >= 3) {
            this.status = OutboxStatus.FAILED;
        }
    }
}
```

*2. Application Service 수정*:
```java
@Service
public class ReservationApplicationService {

    private final TimeSlotManagementService timeSlotManagementService;
    private final OutboxMessageRepository outboxRepository;

    @Transactional
    public void createReservation(SlotReservationRequest request) {
        // 1. 도메인 로직 실행
        timeSlotManagementService.markSlotAsPending(
            request.roomId(), request.slotDate(), request.slotTime(), request.reservationId()
        );

        // 2. Outbox에 이벤트 저장 (Kafka 발행 대신)
        SlotReservedEvent event = SlotReservedEvent.of(
            request.roomId().toString(),
            request.slotDate(),
            List.of(request.slotTime()),
            request.reservationId().toString()
        );

        OutboxMessage outboxMessage = OutboxMessage.from(event);
        outboxRepository.save(outboxMessage);

        log.info("SlotReservedEvent saved to outbox: reservationId={}", request.reservationId());

        // DB 트랜잭션 커밋 시 슬롯 + Outbox 함께 커밋 ✅
    }
}
```

*3. Outbox Message Relay (Scheduler)*:
```java
@Component
@RequiredArgsConstructor
public class OutboxMessageRelay {

    private final OutboxMessageRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonUtil jsonUtil;

    @Scheduled(fixedDelay = 1000) // 1초마다
    @SchedulerLock(name = "OutboxMessageRelay", lockAtMostFor = "50s", lockAtLeastFor = "5s")
    public void publishPendingMessages() {
        List<OutboxMessage> pendingMessages = outboxRepository
            .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, 100));

        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("Publishing {} pending outbox messages", pendingMessages.size());

        for (OutboxMessage message : pendingMessages) {
            try {
                // Kafka 발행
                kafkaTemplate.send(message.getTopic(), message.getPayload()).get();

                // 발행 성공 시 상태 업데이트
                message.markAsPublished();
                outboxRepository.save(message);

                log.info("Outbox message published: id={}, eventType={}",
                    message.getId(), message.getEventType());

            } catch (Exception e) {
                log.error("Failed to publish outbox message: id={}, error={}",
                    message.getId(), e.getMessage());

                // 재시도 카운트 증가
                message.incrementRetry();
                outboxRepository.save(message);
            }
        }
    }

    // 오래된 PUBLISHED 메시지 정리 (일주일 이상)
    @Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시
    @SchedulerLock(name = "OutboxCleanup", lockAtMostFor = "10m")
    public void cleanupPublishedMessages() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        int deleted = outboxRepository.deleteByStatusAndPublishedAtBefore(
            OutboxStatus.PUBLISHED, threshold
        );
        log.info("Cleaned up {} published outbox messages", deleted);
    }
}
```

**장점**:
- DB-Kafka 원자성 보장 (단일 DB 트랜잭션)
- 정확히 한 번 발행 보장 (중복 방지)
- Kafka 장애 시에도 애플리케이션 정상 동작
- 재시도 메커니즘 내장
- 발행 실패 이벤트 추적 가능 (FAILED 상태)
- MSA의 표준 패턴 (검증된 솔루션)

**단점**:
- Outbox 테이블 추가 필요
- 스케줄러 구현 필요
- 약간의 이벤트 발행 지연 (최대 1초)
- 저장소 공간 증가 (정리 배치로 완화)

**Trade-offs**:
- 즉시 발행 vs 원자성: 1초 지연을 허용하여 데이터 일관성 보장
- 단순성 vs 안정성: 코드 복잡도 증가하지만 운영 안정성 확보

---

### Option 2: Change Data Capture (CDC) with Debezium

**개념**:
```
MySQL Binlog → Debezium Connector → Kafka Connect → Kafka Topic
```

**구현 예시**:
```yaml
# Debezium MySQL Connector
name: room-slot-connector
connector.class: io.debezium.connector.mysql.MySqlConnector
database.hostname: mysql
database.port: 3306
database.user: debezium
database.password: ${DEBEZIUM_PASSWORD}
database.server.id: 1
database.server.name: room-service
table.include.list: room_service.outbox_messages
transforms: outbox
transforms.outbox.type: io.debezium.transforms.outbox.EventRouter
transforms.outbox.table.field.event.type: event_type
transforms.outbox.table.field.event.payload: payload
```

**장점**:
- Application 코드 변경 최소화
- 매우 낮은 레이턴시 (실시간 CDC)
- 높은 처리량
- Kafka Connect 생태계 활용

**단점**:
- 인프라 복잡도 대폭 증가 (Kafka Connect, Debezium)
- MySQL Binlog 설정 필요 (`binlog_format=ROW`)
- 운영 난이도 상승 (Connector 장애 처리)
- 팀의 Debezium 경험 부족
- 초기 설정 비용 높음

**Trade-offs**:
- 성능 vs 복잡도: 최고 성능이지만 운영 부담 큼

---

### Option 3: Kafka Transactional Producer + 2PC

**개념**:
```java
@Transactional
public void createReservation(...) {
    // DB 트랜잭션 시작
    timeSlotService.markAsPending(...);

    // Kafka 트랜잭션 시작
    kafkaTemplate.executeInTransaction(kt -> {
        kt.send(event);
        return null;
    });

    // 2단계 커밋 (DB + Kafka)
}
```

**장점**:
- 강력한 ACID 보장

**단점**:
- 심각한 성능 저하 (2PC는 blocking)
- Kafka는 완전한 2PC를 지원하지 않음 (제한적)
- 구현 복잡도 매우 높음
- 네트워크 파티션 시 데드락 가능성
- 실무에서 거의 사용되지 않음

**Trade-offs**:
- 일관성 vs 가용성: CAP 이론의 C를 극대화하지만 A와 P 희생

---

### Option 4: 현재 방식 유지 + 보상 트랜잭션

**개념**:
```java
@Transactional
public void createReservation(...) {
    timeSlotService.markAsPending(...);

    try {
        eventPublisher.publish(event);
    } catch (Exception e) {
        // 보상 트랜잭션: 슬롯 상태 롤백
        timeSlotService.cancelSlot(...);
        throw e;
    }
}
```

**장점**:
- 구현 단순
- 인프라 변경 없음

**단점**:
- 보상 트랜잭션도 실패할 수 있음 (네트워크 장애)
- Eventually Consistent가 아님 (실패 시 불일치 지속)
- 부분 실패 시나리오 처리 복잡

**Trade-offs**:
- 단순성 vs 안정성: 간단하지만 데이터 불일치 리스크

---

## Decision Outcome

**선택: Option 1 - Transactional Outbox Pattern**

### 선택 이유

1. **검증된 MSA 패턴**
   - Martin Fowler, Chris Richardson 등이 권장하는 표준 패턴
   - 많은 기업에서 프로덕션 검증 완료

2. **적절한 Trade-off**
   - Option 2(CDC)보다 인프라 복잡도 낮음
   - Option 3(2PC)보다 성능 우수
   - Option 4(현재)보다 안정성 확보

3. **팀 역량 적합**
   - Spring Boot + JPA + Kafka 기술 스택 활용
   - ShedLock 스케줄러 경험 있음 (Rolling Window에서 이미 사용)
   - 학습 곡선 낮음

4. **비즈니스 요구사항 충족**
   - 원자성 보장: DB 트랜잭션 내에서 Outbox 저장
   - 정확히 한 번 발행: PENDING → PUBLISHED 상태 전이
   - 장애 복구: 재시도 메커니즘 내장
   - 성능: 1초 지연은 예약 프로세스에 허용 가능
     - **참고**: 실시간성이 중요한 경우 [ADR-004-1: Hybrid Outbox Pattern](004-1-hybrid-outbox-pattern.md) 참조

5. **확장성**
   - 향후 다른 이벤트(SlotConfirmed, SlotCancelled 등)에도 동일 패턴 적용
   - Outbox 테이블 파티셔닝으로 대용량 처리 가능

---

## Implementation Details

### 1. 데이터베이스 스키마

```sql
CREATE TABLE outbox_messages (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL,
    retry_count INT NOT NULL DEFAULT 0,

    INDEX idx_status_created (status, created_at),
    INDEX idx_cleanup (status, published_at)
) ENGINE=InnoDB;
```

### 2. 패키지 구조

```
com.teambind.springproject/
├── message/
│   ├── outbox/
│   │   ├── entity/
│   │   │   └── OutboxMessage.java
│   │   ├── repository/
│   │   │   └── OutboxMessageRepository.java
│   │   ├── relay/
│   │   │   └── OutboxMessageRelay.java (Scheduler)
│   │   └── enums/
│   │       └── OutboxStatus.java
```

### 3. 마이그레이션 전략

**Phase 1: Outbox 인프라 구축**
- Outbox 테이블 생성
- OutboxMessage Entity 작성
- OutboxMessageRelay 스케줄러 구현

**Phase 2: 점진적 적용**
- 신규 기능부터 Outbox 패턴 적용
- 기존 이벤트는 점진적으로 마이그레이션

**Phase 3: 모니터링 강화**
- FAILED 상태 메시지 알림
- 발행 지연 시간 모니터링
- Outbox 테이블 크기 추적

### 4. 설정 값

```yaml
# application.yaml
outbox:
  relay:
    fixedDelay: 1000          # 1초마다 폴링
    batchSize: 100             # 한 번에 처리할 메시지 수
    maxRetryCount: 3           # 최대 재시도 횟수
  cleanup:
    retentionDays: 7           # PUBLISHED 메시지 보관 기간
    cron: "0 0 2 * * *"        # 정리 배치 실행 시간
```

---

## Consequences

### Positive

- **데이터 일관성 보장**: DB와 Kafka가 항상 동기화됨
- **장애 격리**: Kafka 다운타임이 예약 생성을 블로킹하지 않음
- **재시도 메커니즘**: 네트워크 일시 장애 자동 복구
- **추적성**: FAILED 상태 메시지로 문제 파악 용이
- **확장성**: 다른 도메인 이벤트에도 동일 패턴 적용 가능

### Negative

- **저장소 공간 증가**: Outbox 테이블 관리 필요
  - **완화 방안**: 일주일 후 PUBLISHED 메시지 자동 삭제

- **이벤트 발행 지연**: 최대 1초 (스케줄러 주기)
  - **완화 방안 1**: 예약 프로세스는 1초 지연 허용 가능
  - **완화 방안 2**: 실시간성이 중요한 경우 [Hybrid Outbox Pattern](004-1-hybrid-outbox-pattern.md) 적용 (즉시 발행 + Outbox 백업)

- **코드 복잡도 증가**: Outbox Entity + Relay 추가
  - **완화 방안**: 공통 컴포넌트로 구현하여 재사용

### Risks

- **Outbox 테이블 과다 증가**: 정리 배치 실패 시
  - **완화 방안**: 모니터링 알림 설정, 수동 정리 스크립트 준비

- **스케줄러 장애**: Relay가 멈추면 이벤트 발행 중단
  - **완화 방안**: ShedLock으로 HA 구성, 헬스체크 엔드포인트 추가

---

## Validation

### 1. 기능 테스트

```java
@SpringBootTest
@Transactional
class OutboxPatternIntegrationTest {

    @Autowired
    private ReservationApplicationService reservationService;

    @Autowired
    private OutboxMessageRepository outboxRepository;

    @Test
    void createReservation_ShouldSaveToOutbox() {
        // Given
        SlotReservationRequest request = new SlotReservationRequest(...);

        // When
        reservationService.createReservation(request);

        // Then
        List<OutboxMessage> messages = outboxRepository.findByStatus(OutboxStatus.PENDING);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getEventType()).isEqualTo("SlotReserved");
    }

    @Test
    void outboxRelay_ShouldPublishToKafka() throws Exception {
        // Given: Outbox에 PENDING 메시지 저장
        OutboxMessage message = OutboxMessage.from(event);
        outboxRepository.save(message);

        // When: Relay 실행
        outboxMessageRelay.publishPendingMessages();

        // Then: Kafka 수신 확인
        ConsumerRecord<String, String> record = kafkaConsumer.poll(Duration.ofSeconds(5));
        assertThat(record.value()).contains("SlotReserved");

        // Then: Outbox 상태 업데이트 확인
        OutboxMessage updated = outboxRepository.findById(message.getId()).get();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }
}
```

### 2. 장애 시나리오 테스트

```java
@Test
void outboxRelay_WhenKafkaDown_ShouldRetry() {
    // Given: Kafka 다운 시뮬레이션
    kafkaContainer.stop();

    OutboxMessage message = outboxRepository.save(OutboxMessage.from(event));

    // When: Relay 실행 (실패 예상)
    outboxMessageRelay.publishPendingMessages();

    // Then: 재시도 카운트 증가
    OutboxMessage updated = outboxRepository.findById(message.getId()).get();
    assertThat(updated.getRetryCount()).isEqualTo(1);
    assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PENDING);

    // When: Kafka 복구 후 재시도
    kafkaContainer.start();
    outboxMessageRelay.publishPendingMessages();

    // Then: 발행 성공
    updated = outboxRepository.findById(message.getId()).get();
    assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
}
```

### 3. 성능 테스트

```java
@Test
void outboxRelay_Performance_ShouldProcess100MessagesInOneSecond() {
    // Given: 100개 메시지 생성
    List<OutboxMessage> messages = IntStream.range(0, 100)
        .mapToObj(i -> OutboxMessage.from(createEvent(i)))
        .collect(Collectors.toList());
    outboxRepository.saveAll(messages);

    // When
    long start = System.currentTimeMillis();
    outboxMessageRelay.publishPendingMessages();
    long duration = System.currentTimeMillis() - start;

    // Then: 1초 이내 처리
    assertThat(duration).isLessThan(1000);

    long publishedCount = outboxRepository.countByStatus(OutboxStatus.PUBLISHED);
    assertThat(publishedCount).isEqualTo(100);
}
```

---

## References

- [Pattern: Transactional outbox](https://microservices.io/patterns/data/transactional-outbox.html) - Chris Richardson
- [Event-Driven Data Management for Microservices](https://www.nginx.com/blog/event-driven-data-management-microservices/) - NGINX
- [Implementing the Outbox Pattern](https://debezium.io/blog/2019/02/19/reliable-microservices-data-exchange-with-the-outbox-pattern/) - Debezium Blog
- [Transactional Outbox Pattern with Spring Boot](https://medium.com/@sonus21/transactional-outbox-pattern-in-spring-boot-eb2b81a79c3a)

---

## Future Considerations

**Phase 1 (현재)**:
- 기본 Outbox 패턴 구현
- 단일 테이블, 단순 스케줄러

**Phase 2 (실시간성 개선)**:
- **[Hybrid Outbox Pattern](004-1-hybrid-outbox-pattern.md) 도입** (우선순위 높음)
  - 트랜잭션 커밋 직후 즉시 Kafka 발행 시도
  - 실패 시에만 Outbox에서 재발행
  - 발행 지연 1초 → 0ms 개선

**Phase 3 (확장성)**:
- Outbox 테이블 파티셔닝 (월별)
- 재시도 전략 고도화 (Exponential Backoff)

**Phase 4 (고급)**:
- CDC(Debezium) 도입 검토 (처리량이 크게 증가할 경우)
- Event Sourcing과 통합

**Phase 5 (MSA 성숙도)**:
- Outbox를 별도 서비스로 분리 (Event Publisher Microservice)

---

**Maintained by**: Teambind_dev_backend Team
**Lead Developer**: DDINGJOO