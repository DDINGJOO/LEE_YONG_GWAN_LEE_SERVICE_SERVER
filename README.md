# Room Time Slot Management Service

**Version**: 1.1.0
**Team**: Teambind_dev_backend Team
**Maintainer**: DDINGJOO
**Type**: Spring Boot REST API Microservice
**Java**: 17
**Build Tool**: Gradle
**Last Updated**: 2025-01-17

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [핵심 기능](#핵심-기능)
- [아키텍처](#아키텍처)
- [디자인 패턴](#디자인-패턴)
- [데이터베이스 스키마](#데이터베이스-스키마)
- [API 엔드포인트](#api-엔드포인트)
- [API 명세서](#api-명세서)
- [기술 스택](#기술-스택)
- [테스트](#테스트)
- [설정 및 실행](#설정-및-실행)

---

## 프로젝트 개요

Room Time Slot Management Service는 **음악 스튜디오, 공연장, 연습실 등의 룸 예약 시스템**을 위한 시간 슬롯 관리 마이크로서비스입니다.

MSA(Microservices Architecture) 환경에서 동작하며, **DDD(Domain-Driven Design)**, **Hexagonal Architecture**, **CQRS** 패턴을 적용하여 복잡한 도메인 로직을 체계적으로 관리합니다.

### 핵심 목표

- **유연한 운영 정책 관리**: 요일별, 시간별, 반복 패턴(매주/홀수주/짝수주) 기반 운영 시간 설정
- **자동화된 슬롯 생성**: Rolling Window 방식으로 항상 30일치 예약 가능 슬롯 유지 (설정 변경 가능)
- **비동기 대용량 처리**: Kafka 기반 이벤트 드리븐 아키텍처로 수천 개 슬롯 생성 성능 보장
- **분산 환경 안정성**: ShedLock을 활용한 분산 스케줄러 동시성 제어
- **예약 상태 관리**: 슬롯 상태 전이(AVAILABLE → PENDING → RESERVED) 도메인 모델 구현
- **동시성 제어**: Pessimistic Lock 기반 다중 슬롯 예약 동시성 보장

---

## 핵심 기능

### 1. 운영 정책 관리

룸별로 독립적인 운영 정책을 설정하고 관리합니다.

#### 1-1. 주간 스케줄 설정
- **요일별 운영 시간**: 월요일 09:00/10:00/11:00, 화요일 14:00/15:00 등 개별 설정
- **유연한 시간 조합**: 하나의 요일에 불연속적인 여러 시간대 설정 가능
- **Value Object 기반 불변성**: `WeeklySlotSchedule`, `WeeklySlotTime`으로 안전한 스케줄 관리

#### 1-2. 반복 패턴 설정
```java
public enum RecurrencePattern {
    EVERY_WEEK,    // 매주 운영
    ODD_WEEK,      // 홀수 주만 운영 (ISO 8601 기준)
    EVEN_WEEK      // 짝수 주만 운영
}
```

#### 1-3. 휴무일 관리
- **날짜 기반 휴무**: 특정 날짜 또는 날짜 범위 설정
  - 예: 2025-01-01 종일 휴무
  - 예: 2025-07-01 ~ 2025-07-07 하절기 휴무
- **시간 기반 휴무**: 특정 날짜의 특정 시간만 휴무
  - 예: 2025-03-15 09:00~12:00 점검 휴무
- **패턴 기반 휴무**: 반복 패턴 + 요일 조합
  - 예: 매주 월요일 09:00~10:00 정기 점검
  - 예: 홀수 주 금요일 종일 휴무

### 2. 시간 슬롯 자동 생성

#### 2-1. Rolling Window 전략
- **30일 선행 생성**: 항상 현재일 기준 30일 후까지 슬롯 유지 (설정 변경 가능: `room.timeSlot.rollingWindow.days`)
- **스케줄러 기반 자동화**: 매일 자정 익일 슬롯 자동 생성
- **과거 데이터 정리**: 과거 데이터 자동 삭제 (보관 정책 적용 시 아카이빙 가능)

#### 2-2. 정책 기반 지능형 생성
```java
// RoomOperatingPolicy Aggregate Root에서 슬롯 생성 로직 캡슐화
public List<RoomTimeSlot> generateSlotsFor(LocalDate date, SlotUnit slotUnit) {
    // 1. 반복 패턴 검증
    if (!recurrence.matches(date)) return Collections.emptyList();

    // 2. 휴무일 검증
    if (isFullDayClosedOn(date)) return Collections.emptyList();

    // 3. 요일별 시작 시간 조회 및 슬롯 생성
    // 4. 부분 휴무 시간은 CLOSED 상태로 생성
}
```

#### 2-3. 비동기 대용량 처리
- **Kafka 이벤트 기반**: 슬롯 생성 요청을 Kafka 토픽으로 발행
- **컨슈머 측 배치 처리**: 수천 개 슬롯을 배치로 생성하여 성능 최적화
- **상태 추적**: `SlotGenerationRequest` 엔티티로 요청 상태 관리 (REQUESTED → IN_PROGRESS → COMPLETED)
- **멱등성 보장**: 동일 요청 중복 처리 방지

### 3. 예약 상태 관리

#### 3-1. 상태 전이 모델
```
AVAILABLE → PENDING → RESERVED
           ↓
       AVAILABLE (취소 시)

CLOSED ↔ AVAILABLE (휴무일 설정/해제)
```

#### 3-2. 도메인 이벤트 기반 상태 변경
- **markAsPending(reservationId)**: 예약 시도 시 호출 (결제 진행 중)
- **confirm()**: 결제 완료 시 예약 확정
- **cancel()**: 예약 취소 시 슬롯 복구
- **markAsClosed() / markAsAvailable()**: 휴무일 설정/해제

#### 3-3. 불변 조건 검증
```java
public void markAsPending(Long reservationId) {
    if (status != SlotStatus.AVAILABLE) {
        throw new InvalidSlotStateTransitionException(
            status.name(), SlotStatus.PENDING.name());
    }
    // 상태 전이 수행
}
```

### 4. 분산 스케줄러 동시성 제어

#### 4-1. ShedLock 기반 분산 락
- **Redis를 Lock Storage로 사용**: 다중 인스턴스 환경에서 단일 스케줄러 실행 보장
- **Graceful 실패 처리**: 락 획득 실패 시 다음 실행 대기
- **자동 락 해제**: 프로세스 장애 시에도 TTL로 자동 해제

```java
@Scheduled(cron = "0 0 0 * * *")  // 매일 자정
@SchedulerLock(name = "generateDailySlotsTask",
               lockAtMostFor = "10m",
               lockAtLeastFor = "5m")
public void generateDailySlots() {
    // 슬롯 생성 로직
}
```

### 5. 외부 시스템 연동

#### 5-1. Place Info Service 연동
- **Port/Adapter 패턴**: `PlaceInfoApiClient` 인터페이스로 추상화
- **슬롯 단위 조회**: Room별 슬롯 단위(HOUR/HALF_HOUR) 조회
- **회로 차단기 패턴 대비**: Resilience4j 적용 가능한 구조

---

## 아키텍처

### 계층 구조

```
[Presentation Layer]
    └── Controller (REST API Endpoints)
         └── RoomSetupController

[Application Layer]
    └── ApplicationService (UseCase 조율)
         ├── RoomSetupApplicationService (Command)
         ├── ClosedDateSetupApplicationService (Command)
         └── TimeSlotQueryService (Query)

[Domain Layer]
    ├── Entity (Aggregate Root)
    │    ├── RoomOperatingPolicy
    │    ├── RoomTimeSlot
    │    ├── SlotGenerationRequest
    │    └── ClosedDateUpdateRequest
    ├── Value Object
    │    ├── WeeklySlotSchedule
    │    ├── WeeklySlotTime
    │    └── ClosedDateRange
    ├── Enum (Strategy)
    │    ├── RecurrencePattern
    │    ├── SlotStatus
    │    └── SlotUnit
    └── Domain Service
         ├── TimeSlotGenerationService
         └── TimeSlotManagementService

[Infrastructure Layer]
    ├── Persistence (Adapter)
    │    ├── OperatingPolicyJpaAdapter
    │    ├── TimeSlotJpaAdapter
    │    ├── SlotGenerationRequestJpaAdapter
    │    └── ClosedDateUpdateRequestJpaAdapter
    ├── External API (Adapter)
    │    └── PlaceInfoApiClientImpl
    └── Messaging (Kafka Producer/Consumer)
```

### CQRS 패턴 적용

**Command (쓰기 작업)**
- `RoomSetupApplicationService`: 운영 정책 설정 + 슬롯 생성 요청
- `ClosedDateSetupApplicationService`: 휴무일 설정 + 슬롯 업데이트 요청

**Query (읽기 작업)**
- `TimeSlotQueryService`: 슬롯 조회, 가용 시간 조회
- DTO 최적화로 읽기 성능 향상

### Hexagonal Architecture (Ports & Adapters)

**Domain Layer가 외부 의존성으로부터 완전히 격리**되어 있습니다.

#### Port (인터페이스)
```java
// domain/port/OperatingPolicyPort.java
public interface OperatingPolicyPort {
    RoomOperatingPolicy save(RoomOperatingPolicy policy);
    Optional<RoomOperatingPolicy> findByRoomId(Long roomId);
}

// domain/port/TimeSlotPort.java
public interface TimeSlotPort {
    List<RoomTimeSlot> saveAll(List<RoomTimeSlot> slots);
    List<RoomTimeSlot> findByRoomIdAndDateRange(Long roomId, LocalDate start, LocalDate end);
}
```

#### Adapter (구현체)
```java
// infrastructure/persistence/OperatingPolicyJpaAdapter.java
@Component
public class OperatingPolicyJpaAdapter implements OperatingPolicyPort {
    private final RoomOperatingPolicyRepository repository;

    @Override
    public RoomOperatingPolicy save(RoomOperatingPolicy policy) {
        return repository.save(policy);
    }
}
```

**장점**:
- 도메인 로직과 인프라 기술의 완전한 분리
- 테스트 시 쉬운 Mock 객체 대체
- 기술 스택 변경 시 도메인 코드 무변경

---

## 디자인 패턴

### 1. Factory Pattern (정적 팩토리 메서드)

모든 엔티티와 Value Object는 **생성자를 private로 감추고** 명확한 의도를 드러내는 정적 팩토리 메서드를 제공합니다.

#### 1-1. Entity Factory
```java
public class RoomTimeSlot {
    private RoomTimeSlot(Long roomId, LocalDate slotDate, LocalTime slotTime,
                         SlotStatus status, Long reservationId) {
        // 생성자는 private
    }

    // 의도가 명확한 팩토리 메서드
    public static RoomTimeSlot available(Long roomId, LocalDate slotDate, LocalTime slotTime) {
        return new RoomTimeSlot(roomId, slotDate, slotTime, SlotStatus.AVAILABLE, null);
    }

    public static RoomTimeSlot closed(Long roomId, LocalDate slotDate, LocalTime slotTime) {
        return new RoomTimeSlot(roomId, slotDate, slotTime, SlotStatus.CLOSED, null);
    }
}

// 사용 예시
RoomTimeSlot slot = RoomTimeSlot.available(1L, LocalDate.now(), LocalTime.of(9, 0));
```

#### 1-2. Value Object Factory
```java
public class ClosedDateRange {
    // 다양한 생성 시나리오에 맞는 팩토리 메서드
    public static ClosedDateRange ofFullDay(LocalDate date) { ... }
    public static ClosedDateRange ofDateRange(LocalDate start, LocalDate end) { ... }
    public static ClosedDateRange ofTimeRange(LocalDate date, LocalTime start, LocalTime end) { ... }
    public static ClosedDateRange ofPatternFullDay(DayOfWeek day, RecurrencePattern pattern) { ... }
    public static ClosedDateRange ofPatternTimeRange(DayOfWeek day, RecurrencePattern pattern,
                                                      LocalTime start, LocalTime end) { ... }
}

// 사용 예시 - 명확한 의도 표현
ClosedDateRange newYear = ClosedDateRange.ofFullDay(LocalDate.of(2025, 1, 1));
ClosedDateRange summer = ClosedDateRange.ofDateRange(
    LocalDate.of(2025, 7, 1),
    LocalDate.of(2025, 7, 7)
);
ClosedDateRange maintenance = ClosedDateRange.ofPatternTimeRange(
    DayOfWeek.MONDAY,
    RecurrencePattern.EVERY_WEEK,
    LocalTime.of(9, 0),
    LocalTime.of(10, 0)
);
```

### 2. Strategy Pattern (Enum 기반 전략)

#### 2-1. RecurrencePattern Strategy
```java
public enum RecurrencePattern {
    EVERY_WEEK {
        @Override
        public boolean matches(LocalDate date) {
            return true;
        }
    },

    ODD_WEEK {
        @Override
        public boolean matches(LocalDate date) {
            int weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            return weekOfYear % 2 == 1;
        }
    },

    EVEN_WEEK {
        @Override
        public boolean matches(LocalDate date) {
            int weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            return weekOfYear % 2 == 0;
        }
    };

    // Template Method
    public abstract boolean matches(LocalDate date);
}
```

**장점**:
- 새로운 반복 패턴 추가 시 기존 코드 수정 불필요 (Open-Closed Principle)
- 조건문(if-else) 없이 다형성으로 해결
- 타입 안전성 보장

### 3. State Pattern (상태 기반 행위 제어)

#### 3-1. SlotStatus 상태 전이
```java
public class RoomTimeSlot {
    private SlotStatus status;

    // 상태 전이 메서드 - 현재 상태 검증 후 전이
    public void markAsPending(Long reservationId) {
        if (status != SlotStatus.AVAILABLE) {
            throw new InvalidSlotStateTransitionException(
                status.name(), SlotStatus.PENDING.name());
        }
        this.status = SlotStatus.PENDING;
        this.reservationId = reservationId;
        this.lastUpdated = LocalDateTime.now();
    }

    public void confirm() {
        if (status != SlotStatus.PENDING) {
            throw new InvalidSlotStateTransitionException(
                status.name(), SlotStatus.RESERVED.name());
        }
        this.status = SlotStatus.RESERVED;
        this.lastUpdated = LocalDateTime.now();
    }

    public void cancel() {
        if (status != SlotStatus.PENDING && status != SlotStatus.RESERVED) {
            throw new InvalidSlotStateTransitionException(
                "슬롯을 취소할 수 없습니다. 현재 상태: " + status.name());
        }
        this.status = SlotStatus.AVAILABLE;
        this.reservationId = null;
        this.lastUpdated = LocalDateTime.now();
    }
}
```

**불변 조건(Invariant) 보호**:
- 도메인 규칙 위반 시 컴파일 타임이 아닌 런타임에 명확한 예외 발생
- 상태 전이 로직을 엔티티 내부에 캡슐화하여 외부에서 직접 상태 변경 불가

### 4. Value Object Pattern

#### 4-1. 불변 Value Object
```java
@Embeddable
public class WeeklySlotSchedule {
    @ElementCollection
    private List<WeeklySlotTime> slotTimes = new ArrayList<>();

    // 생성 후 내부 상태 변경 불가
    private WeeklySlotSchedule(List<WeeklySlotTime> slotTimes) {
        this.slotTimes = new ArrayList<>(slotTimes);  // 방어적 복사
    }

    public static WeeklySlotSchedule of(List<WeeklySlotTime> slotTimes) {
        return new WeeklySlotSchedule(slotTimes);
    }

    // Getter는 수정 불가능한 컬렉션 반환
    public List<LocalTime> getStartTimesFor(DayOfWeek dayOfWeek) {
        return slotTimes.stream()
            .filter(slot -> slot.getDayOfWeek() == dayOfWeek)
            .map(WeeklySlotTime::getStartTime)
            .distinct()
            .sorted()
            .collect(Collectors.collectingAndThen(
                Collectors.toList(),
                Collections::unmodifiableList
            ));
    }
}
```

**Value Object의 특징**:
- **불변성(Immutability)**: 생성 후 상태 변경 불가
- **자기 검증(Self-Validation)**: 생성 시점에 유효성 검증
- **동등성 비교**: equals/hashCode를 모든 필드 기반으로 구현
- **Side-Effect Free**: 메서드 호출이 객체 상태를 변경하지 않음

### 5. Aggregate Root Pattern (DDD)

#### 5-1. RoomOperatingPolicy Aggregate
```java
@Entity
public class RoomOperatingPolicy {  // Aggregate Root
    @Id
    private Long policyId;

    @Embedded
    private WeeklySlotSchedule weeklySchedule;  // Value Object

    @ElementCollection
    private List<ClosedDateRange> closedDates;  // Value Object Collection

    // Aggregate 경계 내 일관성 보장
    public void addClosedDate(ClosedDateRange closedDateRange) {
        if (closedDateRange == null) {
            throw InvalidRequestException.requiredFieldMissing("closedDateRange");
        }
        this.closedDates.add(closedDateRange);
        this.updatedAt = LocalDateTime.now();
    }

    // 도메인 로직 캡슐화
    public List<RoomTimeSlot> generateSlotsFor(LocalDate date, SlotUnit slotUnit) {
        // 정책 기반 슬롯 생성 로직
    }
}
```

**Aggregate 설계 원칙**:
- **일관성 경계**: Aggregate 내부는 강한 일관성, Aggregate 간은 최종 일관성
- **트랜잭션 경계**: 하나의 트랜잭션은 하나의 Aggregate만 수정
- **식별자로 참조**: 다른 Aggregate는 ID로만 참조 (roomId)

### 6. Domain Service Pattern

복잡한 비즈니스 로직이 특정 엔티티에 속하지 않을 때 Domain Service로 분리합니다.

```java
public interface TimeSlotGenerationService {
    /**
     * 특정 기간 동안의 슬롯을 생성한다.
     *
     * 여러 날짜에 걸친 슬롯 생성 로직은 단일 엔티티 책임을 넘어서므로
     * Domain Service로 구현
     */
    List<RoomTimeSlot> generateSlots(
        RoomOperatingPolicy policy,
        LocalDate startDate,
        LocalDate endDate,
        SlotUnit slotUnit
    );
}
```

---

## 데이터베이스 스키마

### ERD 구조

```
room_operating_policies (운영 정책)
    ├── PK: policy_id (BIGINT AUTO_INCREMENT)
    ├── UK: room_id (BIGINT)
    ├── recurrence (VARCHAR)
    ├── created_at, updated_at (DATETIME)
    │
    ├─[1:N]─> weekly_slot_times (주간 스케줄)
    │         ├── FK: policy_id
    │         ├── day_of_week (VARCHAR)
    │         └── start_time (TIME)
    │
    └─[1:N]─> policy_closed_dates (휴무일)
              ├── FK: policy_id
              ├── start_date, end_date (DATE)
              ├── day_of_week (VARCHAR)
              ├── recurrence_pattern (VARCHAR)
              └── start_time, end_time (TIME)

room_time_slots (시간 슬롯)
    ├── PK: slot_id (BIGINT AUTO_INCREMENT)
    ├── room_id (BIGINT)
    ├── slot_date (DATE)
    ├── slot_time (TIME)
    ├── status (VARCHAR)
    ├── reservation_id (BIGINT, nullable)
    ├── last_updated (DATETIME)
    └── Indexes:
        ├── idx_room_date_time (room_id, slot_date, slot_time)
        ├── idx_date_status (slot_date, status)
        └── idx_cleanup (slot_date)

slot_generation_requests (슬롯 생성 요청)
    ├── PK: request_id (VARCHAR(36) UUID)
    ├── room_id (BIGINT)
    ├── start_date, end_date (DATE)
    ├── status (VARCHAR)
    ├── total_slots (INT)
    ├── requested_at, started_at, completed_at (DATETIME)
    └── error_message (VARCHAR(1000))

closed_date_update_requests (휴무일 업데이트 요청)
    ├── PK: request_id (VARCHAR(36) UUID)
    ├── room_id (BIGINT)
    ├── closed_date_count (INT)
    ├── status (VARCHAR)
    ├── affected_slots (INT)
    ├── requested_at, started_at, completed_at (DATETIME)
    └── error_message (VARCHAR(1000))
```

### 인덱스 전략

#### 복합 인덱스 (room_time_slots)
```sql
INDEX idx_room_date_time (room_id, slot_date, slot_time)
```
**용도**: 특정 룸의 특정 날짜/시간 슬롯 조회 (가장 빈번한 쿼리)
```sql
SELECT * FROM room_time_slots
WHERE room_id = ? AND slot_date = ? AND slot_time = ?
```

#### 조건 필터 인덱스
```sql
INDEX idx_date_status (slot_date, status)
```
**용도**: 날짜 범위 + 상태 필터 조회
```sql
SELECT * FROM room_time_slots
WHERE slot_date BETWEEN ? AND ? AND status = 'AVAILABLE'
```

#### 정리 작업 인덱스
```sql
INDEX idx_cleanup (slot_date)
```
**용도**: 스케줄러의 과거 데이터 삭제 작업
```sql
DELETE FROM room_time_slots WHERE slot_date < ?
```

### 외래 키 정책

**ElementCollection 테이블에만 FK 설정**:
- `weekly_slot_times.policy_id → room_operating_policies.policy_id`
- `policy_closed_dates.policy_id → room_operating_policies.policy_id`

**주 테이블 간에는 FK 없음**:
- MSA 환경에서 서비스 간 결합도 최소화
- `room_time_slots.room_id`는 Place Info Service의 Room 엔티티를 참조하지만 FK 없음
- 데이터 정합성은 애플리케이션 레벨에서 보장

---

## API 엔드포인트

### 1. 운영 정책 설정 및 슬롯 생성

#### POST /api/rooms/setup

룸의 운영 정책을 설정하고 슬롯 생성을 비동기로 요청합니다.

**Request Body**:
```json
{
  "roomId": 1,
  "recurrence": "EVERY_WEEK",
  "slots": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "09:00"
    },
    {
      "dayOfWeek": "MONDAY",
      "startTime": "10:00"
    },
    {
      "dayOfWeek": "TUESDAY",
      "startTime": "14:00"
    }
  ],
  "closedDates": [
    {
      "startDate": "2025-01-01",
      "endDate": null,
      "startTime": null,
      "endTime": null
    }
  ],
  "startDate": "2025-01-01",
  "endDate": "2025-03-01"
}
```

**Response** (202 Accepted):
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "슬롯 생성 요청이 접수되었습니다."
}
```

### 2. 슬롯 생성 상태 조회

#### GET /api/rooms/setup/{requestId}/status

슬롯 생성 요청의 진행 상태를 조회합니다.

**Response** (200 OK):
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "totalSlots": 1200,
  "requestedAt": "2025-01-01T10:00:00",
  "startedAt": "2025-01-01T10:00:05",
  "completedAt": "2025-01-01T10:02:30"
}
```

**Status 종류**:
- `REQUESTED`: 요청 대기 중
- `IN_PROGRESS`: 처리 중
- `COMPLETED`: 완료
- `FAILED`: 실패 (errorMessage 포함)

### 3. 휴무일 설정

#### POST /api/rooms/setup/closed-dates

휴무일을 추가하고 기존 슬롯을 비동기로 업데이트합니다.

**Request Body**:
```json
{
  "roomId": 1,
  "closedDates": [
    {
      "startDate": "2025-05-01",
      "endDate": "2025-05-05",
      "startTime": null,
      "endTime": null
    },
    {
      "startDate": "2025-06-15",
      "endDate": null,
      "startTime": "09:00",
      "endTime": "12:00"
    }
  ]
}
```

**Response** (202 Accepted):
```json
{
  "requestId": "660e8400-e29b-41d4-a716-446655440001",
  "message": "휴무일 업데이트 요청이 접수되었습니다.",
  "closedDateCount": 2
}
```

### 4. 예약 가능 슬롯 조회

#### GET /api/v1/reservations/available-slots

특정 룸의 특정 날짜에 예약 가능한 슬롯 목록을 조회합니다.

**Query Parameters**:
- `roomId` (required): 룸 ID
- `date` (required): 조회할 날짜 (ISO 8601 형식: YYYY-MM-DD)

**Response** (200 OK):
```json
[
  {
    "slotTime": "09:00",
    "status": "AVAILABLE"
  },
  {
    "slotTime": "10:00",
    "status": "AVAILABLE"
  },
  {
    "slotTime": "11:00",
    "status": "AVAILABLE"
  }
]
```

### 5. 단일 슬롯 예약

#### POST /api/v1/reservations

단일 슬롯을 예약 대기 상태(PENDING)로 변경합니다.

**Request Body**:
```json
{
  "roomId": 1,
  "slotDate": "2025-01-20",
  "slotTime": "14:00",
  "reservationId": 12345
}
```

**Response** (200 OK):
```json
{}
```

### 6. 다중 슬롯 예약 (신규 기능)

#### POST /api/v1/reservations/multi

특정 날짜의 여러 시간 슬롯을 한 번에 예약 대기 상태로 변경합니다.

**특징**:
- Pessimistic Lock (SELECT FOR UPDATE) 사용으로 동시성 문제 해결
- 예약 ID 자동 생성 (Snowflake ID Generator)
- 모든 슬롯이 AVAILABLE 상태인지 검증 후 일괄 처리
- 하나라도 예약 불가능하면 전체 롤백

**Request Body**:
```json
{
  "roomId": 1,
  "slotDate": "2025-01-20",
  "slotTimes": ["14:00", "15:00", "16:00"]
}
```

**Response** (200 OK):
```json
{
  "reservationId": 567890123456789,
  "roomId": 1,
  "slotDate": "2025-01-20",
  "reservedSlotTimes": ["14:00", "15:00", "16:00"]
}
```

**Error Responses**:
- `400 Bad Request`: 요청 파라미터 누락 또는 유효하지 않음
- `404 Not Found`: 일부 슬롯을 찾을 수 없음
- `409 Conflict`: 일부 슬롯이 이미 예약되어 있음 (AVAILABLE 상태가 아님)

---

## API 명세서

상세한 API 명세는 별도 문서를 참고하세요:

📘 **[API 명세서](docs/API-SPECIFICATION.md)**

### API 엔드포인트 요약

| 카테고리 | 메서드 | 엔드포인트 | 설명 |
|---------|--------|-----------|------|
| **룸 설정** | POST | `/api/rooms/setup` | 운영 정책 설정 및 슬롯 생성 요청 |
| | GET | `/api/rooms/setup/{requestId}/status` | 슬롯 생성 상태 조회 |
| | POST | `/api/rooms/setup/closed-dates` | 휴무일 설정 |
| **예약** | GET | `/api/v1/reservations/available-slots` | 예약 가능 슬롯 조회 |
| | POST | `/api/v1/reservations` | 단일 슬롯 예약 |
| | POST | `/api/v1/reservations/multi` | 다중 슬롯 예약 |

---

## 기술 스택

### Backend Framework
- **Spring Boot**: 3.5.7
- **Java**: 17 (LTS)
- **Build Tool**: Gradle

### Persistence
- **Spring Data JPA**: ORM 및 Repository 패턴
- **Hibernate**: JPA 구현체
- **MySQL/MariaDB**: RDBMS
- **H2 Database**: 테스트 환경

### Messaging
- **Apache Kafka**: 비동기 이벤트 기반 메시징
  - 슬롯 생성 요청/응답
  - 휴무일 업데이트 이벤트

### Caching & Lock
- **Spring Data Redis**: 캐싱 및 분산 락 저장소
- **ShedLock**: 분산 스케줄러 동시성 제어
  - shedlock-spring: 5.10.0
  - shedlock-provider-redis-spring: 5.10.0

### Testing
- **JUnit 5**: 테스트 프레임워크
- **Spring Boot Test**: 통합 테스트 지원
- **H2**: 인메모리 DB 기반 테스트

### Utilities
- **Lombok**: 보일러플레이트 코드 제거
- **Jackson**: JSON 직렬화/역직렬화

---

## 테스트

### 테스트 통계

- **총 테스트 파일**: 24개
- **총 Java 파일**: 117개
- **테스트 커버리지 목표**: 80% 이상

### 테스트 계층별 분류

#### 1. Unit Test (단위 테스트)

**Domain Layer**:
- `RoomOperatingPolicyTest`: 정책 기반 슬롯 생성 로직
- `RoomTimeSlotTest`: 상태 전이 검증
- `WeeklySlotScheduleTest`: Value Object 동작
- `ClosedDateRangeTest`: 휴무일 범위 계산
- `RecurrencePatternTest`: 반복 패턴 매칭
- `SlotStatusTest`: 상태 전이 규칙

**Service Layer**:
- `TimeSlotGenerationServiceImplTest`: 슬롯 생성 서비스
- `TimeSlotManagementServiceImplTest`: 슬롯 관리 서비스

#### 2. Integration Test (통합 테스트)

**Application Layer**:
- `RoomSetupApplicationServiceTest`: 운영 정책 설정 플로우
- `ClosedDateSetupApplicationServiceTest`: 휴무일 설정 플로우

**Infrastructure Layer**:
- `OperatingPolicyJpaAdapterIntegrationTest`: JPA 영속성
- `TimeSlotJpaAdapterIntegrationTest`: 슬롯 저장/조회
- `SlotGenerationRequestJpaAdapterIntegrationTest`: 요청 상태 관리
- `ClosedDateUpdateRequestJpaAdapterIntegrationTest`: 업데이트 요청 관리

#### 3. Exception Test

- `SlotExceptionsTest`: 슬롯 관련 예외 처리
- `PolicyExceptionsTest`: 정책 관련 예외 처리

### 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests RoomOperatingPolicyTest

# 테스트 리포트 생성
./gradlew test jacocoTestReport
```

### Given-When-Then (BDD) 스타일

모든 테스트는 **BDD 스타일**로 작성되어 가독성과 유지보수성을 높였습니다.

```java
@Test
@DisplayName("AVAILABLE 상태의 슬롯을 PENDING으로 전환한다")
void markAsPending_Success() {
    // Given
    RoomTimeSlot slot = RoomTimeSlot.available(1L, LocalDate.now(), LocalTime.of(9, 0));
    Long reservationId = 100L;

    // When
    slot.markAsPending(reservationId);

    // Then
    assertThat(slot.getStatus()).isEqualTo(SlotStatus.PENDING);
    assertThat(slot.getReservationId()).isEqualTo(reservationId);
}
```

---

## 설정 및 실행

### 필수 요구사항

- **Java**: 17 이상
- **MySQL**: 8.0 이상
- **Redis**: 7.0 이상
- **Kafka**: 3.0 이상

### 환경 변수 설정

```properties
# application.yml 또는 환경 변수

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/room_service
spring.datasource.username=root
spring.datasource.password=password

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=room-service-group
```

### 로컬 실행

#### 1. 데이터베이스 스키마 생성
```bash
mysql -u root -p room_service < schema.sql
```

#### 2. Redis 실행
```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

#### 3. Kafka 실행 (Docker Compose)
```bash
docker-compose up -d kafka zookeeper
```

#### 4. 애플리케이션 실행
```bash
./gradlew bootRun
```

### Docker 실행

```bash
# Docker 이미지 빌드
./gradlew bootBuildImage

# 컨테이너 실행
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/room_service \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  room-time-slot-service:1.0.0
```

---

## 프로젝트 특이점 및 분석

### 1. 아키텍처 우수성

#### 1-1. DDD + Hexagonal + CQRS 조합
- **도메인 중심 설계**: 비즈니스 로직이 도메인 계층에 집중되어 있어 변경에 유연
- **기술 독립성**: Port/Adapter 패턴으로 JPA를 MongoDB나 다른 저장소로 교체 가능
- **읽기/쓰기 분리**: CQRS로 쓰기 최적화(정규화)와 읽기 최적화(비정규화) 독립 설계 가능

#### 1-2. Aggregate 설계의 명확성
```
RoomOperatingPolicy (Aggregate Root)
    ├── WeeklySlotSchedule (Value Object)
    │    └── List<WeeklySlotTime> (Value Object)
    └── List<ClosedDateRange> (Value Object)
```
- 트랜잭션 경계가 명확하여 동시성 제어 단순화
- Aggregate 간 참조는 ID로만 하여 결합도 최소화

### 2. 비동기 처리 전략의 탁월함

#### 2-1. 202 Accepted 패턴
```
Client → [POST /api/rooms/setup] → Server
         ← 202 Accepted {requestId} ←

Client → [GET /api/rooms/setup/{requestId}/status] → Server
         ← 200 OK {status: COMPLETED} ←
```
- 대용량 슬롯 생성 시 HTTP 타임아웃 방지
- 사용자에게 즉각 응답 제공으로 UX 향상

#### 2-2. Kafka 이벤트 기반 처리
- **Producer**: ApplicationService에서 슬롯 생성 요청 이벤트 발행
- **Consumer**: 별도 워커가 배치로 슬롯 생성
- **멱등성**: 동일 requestId 중복 처리 방지

#### 2-3. 상태 추적 엔티티
```
SlotGenerationRequest:
  REQUESTED → IN_PROGRESS → COMPLETED/FAILED
```
- 실패 시 재처리 가능
- 모니터링 및 디버깅 용이

### 3. 분산 시스템 대응

#### 3-1. ShedLock 기반 스케줄러 동시성 제어
```java
@Scheduled(cron = "0 0 0 * * *")
@SchedulerLock(name = "generateDailySlotsTask",
               lockAtMostFor = "10m",
               lockAtLeastFor = "5m")
```
- 여러 인스턴스가 실행되어도 단일 스케줄러만 동작
- Redis를 Lock Storage로 사용하여 빠른 락 획득/해제

#### 3-2. 서비스 간 느슨한 결합
- Place Info Service와 HTTP API로 통신하지만 FK 없음
- 장애 전파 방지를 위한 회로 차단기 패턴 적용 가능

### 4. 도메인 모델의 풍부함

#### 4-1. 복잡한 비즈니스 규칙 표현
```java
// 날짜 기반 + 패턴 기반 휴무일 동시 지원
ClosedDateRange.ofFullDay(date)                          // 특정 날짜 종일
ClosedDateRange.ofDateRange(start, end)                  // 날짜 범위
ClosedDateRange.ofTimeRange(date, start, end)            // 특정 날짜 시간대
ClosedDateRange.ofPatternFullDay(day, pattern)           // 반복 패턴 종일
ClosedDateRange.ofPatternTimeRange(day, pattern, s, e)   // 반복 패턴 시간대
```

#### 4-2. 자기 검증(Self-Validation)
```java
public void markAsPending(Long reservationId) {
    if (status != SlotStatus.AVAILABLE) {
        throw new InvalidSlotStateTransitionException(...);
    }
    if (reservationId == null) {
        throw InvalidRequestException.requiredFieldMissing("reservationId");
    }
    // 상태 전이
}
```
- 불변 조건을 엔티티 내부에서 보호
- 외부에서 잘못된 상태 변경 시도 원천 차단

### 5. 성능 최적화 포인트

#### 5-1. 인덱스 전략
- 복합 인덱스 `(room_id, slot_date, slot_time)`: 조회 성능 극대화
- Covering Index 가능: SELECT 시 테이블 접근 없이 인덱스만으로 해결

#### 5-2. 배치 처리
```java
List<RoomTimeSlot> slots = ...; // 2개월치 슬롯 (약 1,000~3,000개)
timeSlotPort.saveAll(slots);    // Batch Insert
```
- JPA Batch Insert로 DB 왕복 횟수 최소화

#### 5-3. 페이징 준비
- `slot_date` 기반 커서 페이지네이션 적용 가능한 인덱스 구조

### 6. 테스트 용이성

#### 6-1. Hexagonal Architecture의 장점
```java
// 테스트 시 Mock Port 주입
@Test
void test() {
    OperatingPolicyPort mockPort = mock(OperatingPolicyPort.class);
    TimeSlotGenerationService service = new TimeSlotGenerationServiceImpl(mockPort);
    // 실제 DB 없이도 도메인 로직 테스트 가능
}
```

#### 6-2. H2 인메모리 DB
- 통합 테스트 시 빠른 테스트 실행
- 각 테스트마다 격리된 DB 상태 보장

### 7. 확장 가능성

#### 7-1. 새로운 반복 패턴 추가
```java
// RecurrencePattern Enum에 추가만 하면 됨
public enum RecurrencePattern {
    EVERY_WEEK { ... },
    ODD_WEEK { ... },
    EVEN_WEEK { ... },
    FIRST_WEEK_OF_MONTH {  // 신규 추가
        @Override
        public boolean matches(LocalDate date) {
            return date.get(IsoFields.WEEK_OF_MONTH) == 1;
        }
    };
}
```

#### 7-2. 새로운 저장소 교체
```java
// MongoDB Adapter 추가 시
@Component
public class OperatingPolicyMongoAdapter implements OperatingPolicyPort {
    // MongoDB 구현
}
```
- 도메인 계층 코드 무변경

### 8. 운영 편의성

#### 8-1. 명확한 로깅
```java
log.info("POST /api/rooms/setup - roomId: {}, slots: {}",
         request.getRoomId(), request.getSlots().size());
log.info("Room setup request accepted: requestId={}", response.getRequestId());
```

#### 8-2. 예외 처리 체계
- 도메인 예외: `InvalidSlotStateTransitionException`, `PolicyNotFoundException`
- 애플리케이션 예외: `InvalidRequestException`
- 각 예외는 명확한 에러 메시지 포함

#### 8-3. 상태 모니터링
- `SlotGenerationRequest`, `ClosedDateUpdateRequest` 엔티티로 요청 상태 추적
- 실패 시 error_message 저장으로 디버깅 용이

---

## 향후 개선 방향

### 1. 이벤트 소싱 (Event Sourcing)
- 슬롯 상태 변경 이력을 이벤트로 저장
- 감사(Audit) 및 디버깅 강화

### 2. CQRS Read Model 최적화
- 조회 전용 비정규화 테이블 구성
- Redis 캐싱으로 조회 성능 극대화

### 3. Saga Pattern
- 예약 시스템과의 분산 트랜잭션 처리
- 보상 트랜잭션(Compensation) 구현

### 4. Circuit Breaker
- Place Info Service 장애 시 Fallback 처리
- Resilience4j 적용

### 5. 메트릭 수집
- Micrometer + Prometheus + Grafana
- 슬롯 생성 처리 시간, 성공률 모니터링

---

**Developed by Teambind_dev_backend Team**
**Lead Developer: DDINGJOO**
**License: Proprietary**
