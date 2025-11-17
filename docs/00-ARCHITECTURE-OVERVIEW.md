# 아키텍처 개요

**Version**: 1.1.0
**Last Updated**: 2025-01-17

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [아키텍처 스타일](#아키텍처-스타일)
- [계층 구조](#계층-구조)
- [핵심 도메인 모델](#핵심-도메인-모델)
- [기술 스택](#기술-스택)
- [주요 설계 결정](#주요-설계-결정)

---

## 프로젝트 소개

Room Time Slot Management Service는 음악 스튜디오, 공연장, 연습실 등의 **시간 슬롯 관리 및 예약 가능 시간 조회**를 담당하는 마이크로서비스입니다.

### 핵심 책임

- **운영 정책 관리**: 요일별, 시간별 운영 시간 및 반복 패턴(매주/홀수주/짝수주) 관리
- **시간 슬롯 생성**: Rolling Window 방식으로 30일치 예약 가능 슬롯 자동 생성 (설정 가능)
- **슬롯 상태 관리**: 예약 가능(AVAILABLE) → 예약 대기(PENDING) → 예약 확정(RESERVED) 상태 관리
- **휴무일 관리**: 날짜 기반, 시간 기반, 패턴 기반 휴무일 설정
- **다중 슬롯 예약**: Pessimistic Lock 기반 동시성 제어로 여러 슬롯 동시 예약 지원

### 제외 범위

- 예약 승인/거절 처리
- 사용자 인증/권한 관리
- 결제 처리
- 추가 상품 관리

---

## 아키텍처 스타일

### 1. Domain-Driven Design (DDD)

프로젝트는 DDD의 핵심 패턴을 적용합니다.

#### Aggregate Root
```
RoomOperatingPolicy (운영 정책)
├── WeeklySlotSchedule (Value Object)
│   └── List<WeeklySlotTime>
└── List<ClosedDateRange> (Value Object)

RoomTimeSlot (시간 슬롯)
├── SlotStatus (Enum)
├── reservationId (외부 Aggregate 참조)
└── lastUpdated
```

#### Value Objects
- `WeeklySlotSchedule`: 주간 운영 시간 스케줄 (불변)
- `WeeklySlotTime`: 특정 요일의 시작 시각 (불변)
- `ClosedDateRange`: 휴무일 범위 정보 (불변)

#### Domain Services
- `TimeSlotGenerationService`: 정책 기반 슬롯 생성
- `TimeSlotManagementService`: 슬롯 상태 관리

### 2. Hexagonal Architecture (Ports & Adapters)

도메인 계층을 외부 의존성으로부터 격리합니다.

```
[Domain Layer]
    ├── Entity (Aggregate Root)
    ├── Value Object
    ├── Enum
    ├── Domain Service
    └── Port (Interface)

[Infrastructure Layer]
    ├── Adapter (Port 구현)
    │   ├── JpaAdapter (Persistence)
    │   └── ApiClientAdapter (External API)
    └── Configuration
```

**장점**:
- 도메인 로직과 기술 스택 분리
- 테스트 시 Mock 객체로 쉽게 대체
- 기술 변경 시 도메인 코드 무변경

### 3. CQRS (Command Query Responsibility Segregation)

읽기와 쓰기 책임을 분리합니다.

**Command (쓰기)**:
- `RoomSetupApplicationService`: 운영 정책 설정 + 슬롯 생성 요청
- `ClosedDateSetupApplicationService`: 휴무일 설정 + 슬롯 업데이트
- `ReservationApplicationService`: 단일/다중 슬롯 예약 처리

**Query (읽기)**:
- `TimeSlotQueryService`: 슬롯 조회, 가용 시간 조회
- DTO 최적화로 읽기 성능 향상

---

## 계층 구조

```
┌─────────────────────────────────────────────────────────┐
│              Presentation Layer                          │
│  └── Controller (REST API)                              │
│       ├── RoomSetupController                           │
│       └── ReservationController                         │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│              Application Layer                           │
│  └── ApplicationService (UseCase 조율)                  │
│       ├── RoomSetupApplicationService (Command)         │
│       ├── ClosedDateSetupApplicationService (Command)   │
│       ├── ReservationApplicationService (Command)       │
│       └── TimeSlotQueryService (Query)                  │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│              Domain Layer                                │
│  ├── Entity                                             │
│  │    ├── RoomOperatingPolicy (Aggregate Root)         │
│  │    ├── RoomTimeSlot (Aggregate Root)                │
│  │    ├── SlotGenerationRequest                        │
│  │    └── ClosedDateUpdateRequest                      │
│  ├── Value Object                                       │
│  │    ├── WeeklySlotSchedule                           │
│  │    ├── WeeklySlotTime                               │
│  │    └── ClosedDateRange                              │
│  ├── Enum (Strategy Pattern)                           │
│  │    ├── RecurrencePattern                            │
│  │    ├── SlotStatus                                   │
│  │    └── SlotUnit                                     │
│  ├── Domain Service                                     │
│  │    ├── TimeSlotGenerationService                    │
│  │    └── TimeSlotManagementService                    │
│  └── Port (Interface)                                   │
│       ├── OperatingPolicyPort                          │
│       ├── TimeSlotPort                                  │
│       └── PlaceInfoApiClient                           │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│              Infrastructure Layer                        │
│  ├── Persistence Adapter                                │
│  │    ├── OperatingPolicyJpaAdapter                    │
│  │    ├── TimeSlotJpaAdapter                           │
│  │    └── JpaRepository                                │
│  ├── External API Adapter                               │
│  │    └── PlaceInfoApiClientImpl                       │
│  ├── Messaging (Kafka)                                  │
│  │    ├── Producer                                      │
│  │    └── Consumer                                      │
│  └── Scheduler                                          │
│       └── RollingWindowScheduler (ShedLock)            │
└─────────────────────────────────────────────────────────┘
```

---

## 핵심 도메인 모델

### 1. RoomOperatingPolicy (운영 정책)

룸별 운영 시간 정책을 관리하는 Aggregate Root입니다.

**책임**:
- 요일별 운영 시간 슬롯 관리
- 반복 패턴 관리 (매주/홀수주/짝수주)
- 휴무일 예외 관리
- 특정 날짜에 슬롯 생성 여부 판단
- 정책 기반 시간 슬롯 생성

**주요 메서드**:
```java
// 슬롯 생성 여부 판단
boolean shouldGenerateSlotsOn(LocalDate date)

// 슬롯 생성
List<RoomTimeSlot> generateSlotsFor(LocalDate date, SlotUnit slotUnit)

// 휴무일 관리
void addClosedDate(ClosedDateRange closedDateRange)
void updateClosedDates(List<ClosedDateRange> newClosedDates)
```

### 2. RoomTimeSlot (시간 슬롯)

예약 가능한 시간 슬롯을 나타내는 Entity입니다.

**상태 전이**:
```
AVAILABLE → PENDING → RESERVED
           ↓
       AVAILABLE (취소 시)

CLOSED ↔ AVAILABLE (휴무일 설정/해제)
```

**주요 메서드**:
```java
// 상태 전이
void markAsPending(Long reservationId)
void confirm()
void cancel()
void markAsClosed()
void markAsAvailable()

// 상태 확인
boolean isAvailable()
boolean isReserved()
```

### 3. RecurrencePattern (반복 패턴)

Strategy Pattern을 적용한 Enum입니다.

```java
public enum RecurrencePattern {
    EVERY_WEEK {
        public boolean matches(LocalDate date) { return true; }
    },
    ODD_WEEK {
        public boolean matches(LocalDate date) {
            int weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            return weekOfYear % 2 == 1;
        }
    },
    EVEN_WEEK {
        public boolean matches(LocalDate date) {
            int weekOfYear = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            return weekOfYear % 2 == 0;
        }
    };

    public abstract boolean matches(LocalDate date);
}
```

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

### Messaging & Caching
- **Apache Kafka**: 비동기 이벤트 기반 메시징
- **Spring Data Redis**: 캐싱 및 분산 락
- **ShedLock**: 분산 스케줄러 동시성 제어

### Testing
- **JUnit 5**: 테스트 프레임워크
- **Spring Boot Test**: 통합 테스트
- **Given-When-Then**: BDD 스타일 테스트

---

## 주요 설계 결정

프로젝트의 핵심 아키텍처 결정 사항은 별도 ADR(Architecture Decision Record)에 기록되어 있습니다.

### ADR 목록

1. **[ADR-001: MySQL 데이터베이스 선택](adr/001-database-selection.md)**
   - MariaDB 10.11 LTS 선택 이유
   - MySQL vs PostgreSQL vs MongoDB 비교

2. **[ADR-002: Rolling Window 전략](adr/002-rolling-window-strategy.md)**
   - 30일 선행 생성 방식 (설정 가능)
   - 일일 배치 스케줄러 구현

3. **[ADR-003: Hexagonal Architecture 적용](adr/003-hexagonal-architecture.md)**
   - Port/Adapter 패턴 적용
   - 도메인 격리 전략

### 핵심 패턴

#### 1. Factory Pattern (정적 팩토리 메서드)
```java
// 의도가 명확한 생성자 대신 팩토리 메서드 제공
RoomTimeSlot slot = RoomTimeSlot.available(roomId, date, time);
ClosedDateRange holiday = ClosedDateRange.ofFullDay(LocalDate.of(2025, 1, 1));
```

#### 2. Strategy Pattern (Enum 기반)
```java
// 반복 패턴을 Enum으로 구현하여 확장성 확보
public enum RecurrencePattern {
    EVERY_WEEK { ... },
    ODD_WEEK { ... },
    EVEN_WEEK { ... }
}
```

#### 3. State Pattern
```java
// 엔티티 내부에서 상태 전이 규칙 검증
public void markAsPending(Long reservationId) {
    if (status != SlotStatus.AVAILABLE) {
        throw new InvalidSlotStateTransitionException(...);
    }
    // 상태 전이 수행
}
```

---

## 배포 아키텍처

### Rolling Window 스케줄러

```
매일 자정 (KST 00:00)
    ↓
[Scheduler + ShedLock]
    ↓
어제 날짜 슬롯 삭제
    ↓
오늘 + 30일 슬롯 생성 (설정: room.timeSlot.rollingWindow.days)
    ↓
항상 30일치 슬롯 유지
```

**PENDING 슬롯 만료 관리**:
```
5분마다 실행
    ↓
[Scheduler + ShedLock]
    ↓
40분 이상 PENDING 상태 슬롯 조회
    ↓
AVAILABLE 상태로 복구
```

**동시성 제어**:
- ShedLock을 사용하여 다중 인스턴스 환경에서 단일 스케줄러만 실행
- Redis 기반 분산 락

### 데이터베이스 인덱스 전략

```sql
-- 복합 인덱스 (조회 성능 최적화)
INDEX idx_room_date_time (room_id, slot_date, slot_time)

-- 필터 조회용 인덱스
INDEX idx_date_status (slot_date, status)

-- 정리 작업용 인덱스
INDEX idx_cleanup (slot_date)
```

---

## 확장 계획

### Phase 1 (완료)
- 기본 운영 정책 관리
- Rolling Window 슬롯 생성
- 상태 관리 및 조회
- Kafka 이벤트 기반 비동기 처리

### Phase 2 (현재)
- 다중 슬롯 예약 기능 (Pessimistic Lock)
- 예약 만료 자동 복구
- PENDING 슬롯 관리

### Phase 3 (계획)
- Redis 캐싱 레이어 추가
- 성능 최적화
- 모니터링 대시보드
- Circuit Breaker 패턴 적용

---

## 참고 문서

- [개발 환경 설정](01-DEVELOPMENT-SETUP.md)
- [ShedLock 설정 가이드](guides/shedlock-setup.md)
- [타임존 설정 가이드](guides/timezone-configuration.md)
- [예외 처리 가이드](guides/exception-handling.md)
- [아키텍처 결정 기록 (ADR)](adr/)

---

**Maintained by**: Teambind_dev_backend Team
**Lead Developer**: DDINGJOO
