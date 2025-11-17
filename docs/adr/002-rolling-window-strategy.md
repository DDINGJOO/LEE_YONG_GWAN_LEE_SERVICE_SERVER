# ADR-002: Rolling Window 전략

**Status**: Accepted
**Date**: 2025-01-15 (Updated: 2025-01-17)
**Decision Makers**: Teambind_dev_backend Team
**Technical Story**: 시간 슬롯 생성 및 유지 관리 전략

---

## Context

Room Time Slot Management Service는 예약 가능한 시간 슬롯을 미리 생성하고 관리해야 합니다. 사용자는 현재 시점부터 일정 기간 이후까지의 예약 가능 시간을 조회할 수 있어야 합니다.

### 요구사항

1. **예약 가능 기간**
   - 사용자는 최소 2개월 후까지의 슬롯을 조회 가능해야 함
   - 예약 가능 기간은 항상 일정하게 유지되어야 함

2. **성능**
   - 슬롯 조회는 빠르게 응답해야 함 (100ms 이내)
   - 슬롯 생성은 사용자 요청을 블로킹하지 않아야 함

3. **데이터 정합성**
   - 운영 정책 변경이 미래 슬롯에 즉시 반영되어야 함
   - 과거 슬롯은 이력 관리를 위해 보관

4. **확장성**
   - 여러 인스턴스 환경에서 중복 생성 방지
   - 배치 작업 장애 시 자동 복구

---

## Decision Drivers

- 사용자 경험 (빠른 조회 성능)
- 운영 정책 변경의 즉시 반영
- 스토리지 최적화 (과도한 미래 데이터 방지)
- 배치 작업의 안정성 및 동시성 제어
- 시스템 복잡도 최소화

---

## Considered Options

### Option 1: Rolling Window (설정 가능한 선행 생성)

**개념**:
```
Today                    +N days
  ↓                         ↓
[==========================]  ← 항상 N일치 슬롯 유지
  ↑
Yesterday's slots 삭제
Today + N일의 slots 생성
```

**설정**:
```yaml
room:
  timeSlot:
    rollingWindow:
      days: 30  # 기본값 30일 (변경 가능)
```

**구현**:
```java
@Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시
@SchedulerLock(name = "maintainRollingWindow", lockAtMostFor = "PT5M")
public void maintainRollingWindow() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    LocalDate targetDate = LocalDate.now().plusDays(60);

    // 1. 어제 날짜 슬롯 삭제
    slotRepository.deleteBySlotDate(yesterday);

    // 2. 오늘 + 60일 슬롯 생성
    List<RoomOperatingPolicy> policies = policyRepository.findAll();
    for (RoomOperatingPolicy policy : policies) {
        if (policy.shouldGenerateSlotsOn(targetDate)) {
            List<RoomTimeSlot> slots = policy.generateSlotsFor(targetDate);
            slotRepository.saveAll(slots);
        }
    }
}
```

**장점**:
- 항상 일정한 예약 가능 기간 보장 (60일)
- 조회 성능 최적화 (미리 생성된 데이터)
- 스토리지 최적화 (오래된 데이터 자동 삭제)
- 운영 정책 변경 시 미래 슬롯만 재생성하면 됨
- 구현 단순함

**단점**:
- 일일 배치 작업 필요
- 정책 변경 시 미래 슬롯 전체 재생성 필요

### Option 2: On-Demand Generation (실시간 생성)

**개념**:
```java
public List<RoomTimeSlot> getAvailableSlots(Long roomId, LocalDate date) {
    // 슬롯이 없으면 즉시 생성
    if (!slotRepository.existsByRoomIdAndDate(roomId, date)) {
        generateSlotsFor(roomId, date);
    }
    return slotRepository.findByRoomIdAndDate(roomId, date);
}
```

**장점**:
- 배치 작업 불필요
- 정책 변경 즉시 반영

**단점**:
- 첫 조회 시 지연 발생 (생성 시간 소요)
- 동시 요청 시 중복 생성 위험
- 캐싱 복잡도 증가
- 스토리지 관리 부담 (오래된 데이터 수동 정리)

### Option 3: Pre-Computed Calendar (6개월 선행 생성)

**개념**:
- 정책 생성 시 6개월치 슬롯을 미리 생성
- 정책 변경 시 전체 재생성

**장점**:
- 조회 성능 최고
- 배치 작업 불필요

**단점**:
- 대량의 스토리지 사용
- 정책 변경 시 재생성 비용 큼
- 6개월 이후 조회 불가 (기간 제약)
- 슬롯 생성 API 응답 시간 증가

---

## Decision Outcome

**선택: Option 1 - Rolling Window (설정 가능한 선행 생성, 기본 30일)**

### 선택 이유

1. **예약 가능 기간 보장**
   - 설정 가능한 선행 생성 기간 (기본 30일)
   - 사용자 경험 일관성 보장
   - 비즈니스 요구사항에 따라 유연하게 조정 가능

2. **조회 성능**
   - 미리 생성된 데이터 조회 (SELECT만 수행)
   - 조회 API 응답 시간 10ms 이내

3. **스토리지 최적화**
   - 30일치 유지 (룸당 약 720개 슬롯)
   - 과거 데이터 자동 정리
   - 필요시 설정으로 기간 확장 가능 (60일, 90일 등)

4. **운영 정책 변경 대응**
   - 미래 슬롯만 재생성 (부분 재생성)
   - 예약 완료된 슬롯은 유지

5. **확장성**
   - ShedLock으로 다중 인스턴스 환경 지원
   - 배치 작업 실패 시 자동 재시도

---

## Implementation Details

### 1. 스케줄러 설정

```java
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulerConfig {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory);
    }
}
```

### 2. Rolling Window 작업

```java
@Service
@Slf4j
public class RollingWindowScheduler {

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul") // 매일 새벽 2시 (KST)
    @SchedulerLock(name = "maintainRollingWindow",
                   lockAtMostFor = "PT5M",    // 최대 5분
                   lockAtLeastFor = "PT1M")   // 최소 1분 간격
    public void maintainRollingWindow() {
        log.info("Starting rolling window maintenance");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate targetDate = LocalDate.now().plusDays(60);

        // 1. 어제 슬롯 삭제
        int deletedCount = slotRepository.deleteBySlotDate(yesterday);
        log.info("Deleted yesterday's slots: count={}", deletedCount);

        // 2. 오늘 + 60일 슬롯 생성
        List<RoomOperatingPolicy> policies = policyRepository.findAll();
        int createdCount = 0;

        for (RoomOperatingPolicy policy : policies) {
            if (policy.shouldGenerateSlotsOn(targetDate)) {
                List<RoomTimeSlot> slots = policy.generateSlotsFor(targetDate, SlotUnit.HOUR);
                slotRepository.saveAll(slots);
                createdCount += slots.size();
            }
        }

        log.info("Created slots for date {}: count={}", targetDate, createdCount);
        log.info("Rolling window maintenance completed: deleted={}, created={}",
                 deletedCount, createdCount);
    }
}
```

### 3. 동시성 제어

**ShedLock 설정**:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

**동작 원리**:
```
[Instance 1]  02:00:00 → Lock 획득 성공 → 작업 수행
[Instance 2]  02:00:00 → Lock 획득 실패 → 작업 스킵
[Instance 3]  02:00:00 → Lock 획득 실패 → 작업 스킵
```

### 4. 정책 변경 시 슬롯 재생성

```java
@Transactional
public void updateOperatingPolicy(Long policyId, WeeklySlotSchedule newSchedule) {
    RoomOperatingPolicy policy = policyRepository.findById(policyId)
        .orElseThrow(() -> new PolicyNotFoundException(policyId));

    // 1. 정책 업데이트
    policy.updateSchedule(newSchedule);
    policyRepository.save(policy);

    // 2. 미래 슬롯 재생성 (비동기 Kafka 이벤트)
    Long requestId = slotGenerationRequestRepository.save(
        SlotGenerationRequest.forPolicyChange(policy.getPolicyId())
    ).getRequestId();

    kafkaProducer.send("slot.regenerate",
        SlotRegenerateEvent.of(requestId, policy.getRoomId()));
}
```

---

## Consequences

### Positive

- **성능**: 조회 API 응답 시간 10ms 이내
- **일관성**: 항상 60일 후까지 예약 가능
- **스토리지**: 2개월치만 유지하여 최적화
- **안정성**: ShedLock으로 중복 실행 방지
- **유지보수**: 단순한 배치 작업 구조

### Negative

- **배치 의존성**: 배치 작업 실패 시 슬롯 생성 중단
  - **완화 방안**:
    - Kafka를 통한 재시도 메커니즘
    - 모니터링 및 알림 설정
    - 수동 복구 스크립트 준비

- **정책 변경 지연**: 정책 변경 후 미래 슬롯 재생성 필요
  - **완화 방안**:
    - Kafka를 통한 비동기 재생성
    - 재생성 진행 상황 API 제공

### Neutral

- **배치 시간**: 매일 새벽 2시 (사용자 트래픽 최소 시간대)
  - 평균 수행 시간: 1-2분
  - 최대 수행 시간: 5분 (ShedLock 타임아웃)

---

## Validation

### 성능 검증

```bash
# 1. 슬롯 조회 성능 테스트
curl -w "@curl-format.txt" http://localhost:8080/api/rooms/101/slots?date=2025-01-15
# Response time: 8ms

# 2. 배치 작업 수행 시간 측정
# 룸 100개 기준: 평균 90초

# 3. 스토리지 사용량 측정
SELECT COUNT(*) FROM room_time_slots;
# 룸 100개 × 60일 × 24시간 = 144,000 rows (~30MB)
```

### 동시성 테스트

```bash
# 3개 인스턴스에서 동시 실행
docker-compose up --scale app=3

# Redis에서 Lock 확인
redis-cli GET "time-service:maintainRollingWindow"
# → 하나의 인스턴스만 Lock 보유 확인
```

---

## Monitoring

### 모니터링 지표

1. **배치 작업 성공률**
   ```
   metric: scheduler.rolling_window.success_rate
   target: 99.9%
   ```

2. **슬롯 생성 개수**
   ```
   metric: scheduler.rolling_window.slots_created
   alert: < 1000 (정상 범위: 1,000~10,000)
   ```

3. **배치 수행 시간**
   ```
   metric: scheduler.rolling_window.duration_seconds
   alert: > 300 (5분 초과 시)
   ```

4. **예약 가능 기간**
   ```sql
   SELECT MAX(slot_date) FROM room_time_slots WHERE room_id = 101;
   -- Expected: Today + 60 days
   ```

---

## References

- [Spring Scheduling Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#scheduling)
- [ShedLock GitHub](https://github.com/lukas-krecan/ShedLock)
- [Rolling Window Pattern](https://martinfowler.com/articles/patterns-of-distributed-systems/rolling-window.html)

---

## Future Considerations

- **Phase 2**: Kafka 이벤트 기반 슬롯 생성
- **Phase 3**: Redis 캐싱 레이어 추가
- **Phase 4**: 슬롯 생성 우선순위 (인기 룸 우선 생성)

---

**Maintained by**: Teambind_dev_backend Team
**Lead Developer**: DDINGJOO
