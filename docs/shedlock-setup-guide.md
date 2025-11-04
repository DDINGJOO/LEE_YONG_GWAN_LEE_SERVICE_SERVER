# ShedLock 설정 가이드

## 개요

ShedLock을 사용하여 분산 환경에서 스케줄 작업의 중복 실행을 방지합니다.

### 주요 특징

- **분산 Lock**: Redis 기반으로 여러 인스턴스 중 하나만 작업 실행
- **자동 해제**: `lockAtMostFor` 설정으로 작업 실패 시 자동으로 Lock 해제
- **최소 간격 보장**: `lockAtLeastFor` 설정으로 너무 빠른 재실행 방지

---

## 의존성

`build.gradle`에 다음 의존성이 추가되어 있습니다:

```gradle
// Redis
implementation 'org.springframework.boot:spring-boot-starter-data-redis'

// ShedLock
implementation 'net.javacrumbs.shedlock:shedlock-spring:5.10.0'
implementation 'net.javacrumbs.shedlock:shedlock-provider-redis-spring:5.10.0'
```

---

## Redis 설정

### application-dev.yaml 예시

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      lettuce:
        pool:
          max-active: 10
          max-idle: 5
          min-idle: 2
```

### 환경 변수 (.env)

```env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password
```

---

## 스케줄러 Lock 설정

### 1. maintainRollingWindow (매일 새벽 2시)

```java
@Scheduled(cron = "0 0 2 * * *")
@SchedulerLock(
    name = "maintainRollingWindow",
    lockAtMostFor = "PT5M",   // 최대 5분 (작업이 5분 이상 걸리면 자동 해제)
    lockAtLeastFor = "PT1M"   // 최소 1분 간격 (너무 빠른 재실행 방지)
)
public void maintainRollingWindow() {
    // Rolling Window 작업
}
```

**설정 이유**:

- `lockAtMostFor = PT5M`: 정상적으로 1-2분 내 완료되지만, 장애 시 5분 후 자동 해제
- `lockAtLeastFor = PT1M`: 작업 완료 후 최소 1분은 다른 인스턴스가 실행하지 못하도록 보장

### 2. restoreExpiredPendingSlots (5분마다)

```java
@Scheduled(fixedDelay = 300000) // 5분
@SchedulerLock(
    name = "restoreExpiredPendingSlots",
    lockAtMostFor = "PT2M",   // 최대 2분
    lockAtLeastFor = "PT30S"  // 최소 30초 간격
)
public void restoreExpiredPendingSlots() {
    // 만료된 PENDING 슬롯 복구
}
```

**설정 이유**:

- `lockAtMostFor = PT2M`: 빠른 작업이므로 2분 내 완료 예상
- `lockAtLeastFor = PT30S`: 짧은 간격으로 실행되므로 30초 간격 보장

---

## Redis Key 구조

ShedLock은 다음 형식으로 Redis에 Lock을 저장합니다:

```
Key: "time-service:maintainRollingWindow"
Value: {
  "lockUntil": <timestamp>,
  "lockedAt": <timestamp>,
  "lockedBy": <instance-id>
}
TTL: lockAtMostFor 설정값
```

### 확인 방법

Redis CLI로 Lock 상태 확인:

```bash
# 모든 ShedLock Key 조회
redis-cli KEYS "time-service:*"

# 특정 Lock 정보 조회
redis-cli GET "time-service:maintainRollingWindow"

# TTL 확인
redis-cli TTL "time-service:maintainRollingWindow"
```

---

## 동작 원리

### 정상 시나리오

```
[Instance 1]
02:00:00 - maintainRollingWindow 시작
02:00:00 - Redis Lock 획득 성공
02:00:00 ~ 02:01:30 - 작업 수행
02:01:30 - Lock 해제
02:01:30 - 작업 완료

[Instance 2]
02:00:00 - maintainRollingWindow 시작
02:00:00 - Redis Lock 획득 실패 (Instance 1이 보유)
02:00:00 - 작업 스킵 (중복 실행 방지)
```

### 장애 시나리오

```
[Instance 1]
02:00:00 - maintainRollingWindow 시작
02:00:00 - Redis Lock 획득 성공
02:01:00 - Instance 1 장애 발생 (Lock 미해제)

[Redis]
02:05:00 - lockAtMostFor(5분) 도달, Lock 자동 만료

[Instance 2]
02:05:00 - maintainRollingWindow 재실행
02:05:00 - Redis Lock 획득 성공 (Instance 1의 Lock 만료됨)
02:05:00 ~ 02:06:30 - 작업 수행
```

---

## 모니터링

### 로그 확인

스케줄러 실행 로그:

```
[INFO ] Starting rolling window maintenance
[INFO ] Deleted yesterday's slots: count=1234
[INFO ] Created slots for date 2025-03-05: count=1234
[INFO ] Rolling window maintenance completed: deleted=1234, created=1234
```

Lock 획득 실패 로그 (다른 인스턴스가 이미 실행 중):

```
[DEBUG] Skipping task 'maintainRollingWindow' - lock not acquired
```

### 장애 감지

Lock이 5분 이상 유지되는 경우 알림:

```bash
# Redis TTL 모니터링 스크립트 예시
ttl=$(redis-cli TTL "time-service:maintainRollingWindow")
if [ "$ttl" -gt 240 ]; then
  echo "WARNING: Lock held for too long (${ttl}s)"
fi
```

---

## 트러블슈팅

### 1. Lock이 해제되지 않음

**증상**: 작업이 실행되지 않음

**원인**: `lockAtMostFor` 설정이 너무 김

**해결**:

```bash
# 수동으로 Lock 삭제
redis-cli DEL "time-service:maintainRollingWindow"
```

### 2. 중복 실행 발생

**증상**: 두 인스턴스에서 동시 실행

**원인**: Redis 연결 장애 또는 설정 누락

**해결**:

1. Redis 연결 확인: `redis-cli PING`
2. ShedLockConfig 확인: `@EnableSchedulerLock` 존재 여부
3. LockProvider Bean 확인

### 3. 작업이 너무 자주 실행됨

**증상**: `lockAtLeastFor` 무시됨

**원인**: ShedLock 버전 이슈 또는 설정 오류

**해결**:

- ShedLock 버전 확인: 5.10.0 이상
- ISO-8601 Duration 형식 확인: `PT1M` (1분), `PT30S` (30초)

---

## 성능 고려사항

### Redis 부하

- Lock 획득/해제는 Redis 명령어 2개 (SET, DEL)
- 영향 미미 (ms 단위)

### 네트워크 레이턴시

- Redis가 다른 AZ에 있으면 레이턴시 증가 가능
- 해결: Redis를 애플리케이션과 동일 AZ에 배치

### TTL 정확도

- Redis TTL은 초 단위 정확도
- ms 단위 Lock이 필요하면 Redisson 고려

---

## 참고 자료

- [ShedLock 공식 문서](https://github.com/lukas-krecan/ShedLock)
- [ISO-8601 Duration 형식](https://en.wikipedia.org/wiki/ISO_8601#Durations)
- [Redis SET 명령어](https://redis.io/commands/set/)
