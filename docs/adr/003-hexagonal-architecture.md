# ADR-003: Hexagonal Architecture 적용

**Status**: Accepted
**Date**: 2025-01-15
**Decision Makers**: Teambind_dev_backend Team
**Technical Story**: 도메인 중심 아키텍처 설계

---

## Context

Room Time Slot Management Service는 복잡한 도메인 로직(운영 정책, 슬롯 상태 관리, 반복 패턴 등)을 포함하고 있습니다. 도메인 로직을 기술 스택(데이터베이스, 프레임워크, 외부 API)으로부터 격리하여 유지보수성과 테스트 용이성을 향상시키기 위한 아키텍처 패턴이 필요합니다.

### 요구사항

1. **도메인 로직 격리**
   - 도메인 규칙은 데이터베이스나 프레임워크에 의존하지 않아야 함
   - 외부 시스템 변경이 도메인 로직에 영향을 주지 않아야 함

2. **테스트 용이성**
   - 도메인 로직을 외부 의존성 없이 단위 테스트 가능
   - Mock 객체로 쉽게 대체 가능한 구조

3. **확장성**
   - 새로운 데이터베이스나 외부 API 추가 시 도메인 코드 변경 최소화
   - 기술 스택 변경에 유연하게 대응

4. **유지보수성**
   - 명확한 책임 분리
   - 의존성 방향 제어 (Domain ← Application ← Infrastructure)

---

## Decision Drivers

- 도메인 로직의 복잡성 (운영 정책, 상태 전이, 반복 패턴)
- 외부 시스템 통합 (Place Info Service API, Kafka)
- 테스트 커버리지 목표 (80% 이상)
- 팀의 DDD(Domain-Driven Design) 경험
- 마이크로서비스 아키텍처로의 확장 가능성

---

## Considered Options

### Option 1: Hexagonal Architecture (Ports & Adapters)

**개념**:
```
┌─────────────────────────────────────────────────────┐
│                   Application                       │
│          (Use Case Orchestration)                   │
└─────────────────────────────────────────────────────┘
                      ↓ uses
┌─────────────────────────────────────────────────────┐
│                     Domain                          │
│  ┌───────────────┐       ┌──────────────────┐      │
│  │   Entity      │       │  Domain Service  │      │
│  │   (Aggregate) │  ←──  │  (Business Rule) │      │
│  └───────────────┘       └──────────────────┘      │
│  ┌───────────────┐       ┌──────────────────┐      │
│  │ Value Object  │       │  Port (Interface)│      │
│  └───────────────┘       └──────────────────┘      │
└─────────────────────────────────────────────────────┘
                      ↑ implements
┌─────────────────────────────────────────────────────┐
│                 Infrastructure                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │  JpaAdapter  │  │ KafkaAdapter │  │ApiAdapter │ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
└─────────────────────────────────────────────────────┘
```

**구현 예시**:

*Domain Layer (Port 정의)*:
```java
package com.teambind.springproject.room.domain.port;

public interface OperatingPolicyPort {
    Optional<RoomOperatingPolicy> findByRoomId(Long roomId);
    RoomOperatingPolicy save(RoomOperatingPolicy policy);
    void delete(Long policyId);
}
```

*Infrastructure Layer (Adapter 구현)*:
```java
package com.teambind.springproject.room.adapter;

@Repository
public class OperatingPolicyJpaAdapter implements OperatingPolicyPort {

    private final OperatingPolicyJpaRepository jpaRepository;

    @Override
    public Optional<RoomOperatingPolicy> findByRoomId(Long roomId) {
        return jpaRepository.findByRoomId(roomId);
    }

    @Override
    public RoomOperatingPolicy save(RoomOperatingPolicy policy) {
        return jpaRepository.save(policy);
    }
}
```

*Application Layer*:
```java
package com.teambind.springproject.room.application;

@Service
public class RoomSetupApplicationService {

    private final OperatingPolicyPort policyPort; // Port 의존

    public void createPolicy(CreatePolicyCommand command) {
        // 도메인 로직 활용
        RoomOperatingPolicy policy = RoomOperatingPolicy.create(...);
        policyPort.save(policy);
    }
}
```

**장점**:
- 도메인 로직이 기술 스택에 독립적
- 테스트 시 Port를 Mock으로 쉽게 대체
- 의존성 방향이 명확 (Infrastructure → Domain)
- 외부 시스템 변경 시 Adapter만 수정

**단점**:
- Port/Adapter 인터페이스 작성 오버헤드
- 레이어 증가로 인한 코드 복잡도 증가
- 팀원 학습 곡선

### Option 2: Layered Architecture (전통적 3계층)

**개념**:
```
Controller → Service → Repository → Database
```

**장점**:
- 단순하고 직관적
- 팀원 대부분이 익숙함
- 빠른 개발 속도

**단점**:
- 도메인 로직이 Service 계층에 분산
- Repository가 JPA에 강하게 의존
- 테스트 시 실제 데이터베이스 필요
- 외부 시스템 변경 시 Service 코드 수정 필요

### Option 3: Clean Architecture

**개념**:
- Hexagonal Architecture와 유사하나 더 엄격한 계층 분리
- Use Case별 명시적 클래스 작성
- DTO, Mapper 클래스 추가

**장점**:
- 매우 명확한 책임 분리
- Use Case별 독립적 구현

**단점**:
- 과도한 클래스 증가 (Mapper, DTO, Use Case 등)
- 소규모 프로젝트에는 오버엔지니어링
- 개발 속도 저하

---

## Decision Outcome

**선택: Option 1 - Hexagonal Architecture (Ports & Adapters)**

### 선택 이유

1. **도메인 로직 격리**
   - `RoomOperatingPolicy`, `RoomTimeSlot` 엔티티가 JPA 어노테이션에 의존하지만, 핵심 비즈니스 로직은 순수 Java로 구현
   - Port 인터페이스를 통해 Repository 추상화

2. **테스트 용이성**
   ```java
   @Test
   void generateSlots_ShouldCreateHourlySlots() {
       // Given: 순수 도메인 객체 생성 (DB 불필요)
       RoomOperatingPolicy policy = RoomOperatingPolicy.create(
           101L, schedule, RecurrencePattern.EVERY_WEEK, List.of()
       );

       // When: 도메인 메서드 호출
       List<RoomTimeSlot> slots = policy.generateSlotsFor(
           LocalDate.of(2025, 1, 15), SlotUnit.HOUR
       );

       // Then: 도메인 로직 검증 (외부 의존성 없음)
       assertThat(slots).hasSize(24);
   }
   ```

3. **확장성**
   - Place Info Service API 연동 시 `PlaceInfoApiClient` Port 정의
   - 구현체 변경 시 도메인 코드 무변경
   ```java
   // Domain Port
   public interface PlaceInfoApiClient {
       PlaceInfo getPlaceInfo(Long placeId);
   }

   // Infrastructure Adapter (RestTemplate)
   public class PlaceInfoApiClientImpl implements PlaceInfoApiClient { ... }

   // 향후 WebClient로 변경해도 도메인 코드 영향 없음
   ```

4. **의존성 방향 제어**
   ```
   Infrastructure (Adapter) → Domain (Port)
   Application Service → Domain

   Domain은 어떤 계층에도 의존하지 않음 ✅
   ```

5. **적절한 복잡도**
   - Clean Architecture보다 단순
   - Layered Architecture보다 명확한 책임 분리
   - 팀 규모 및 프로젝트 복잡도에 적합

---

## Implementation Details

### 패키지 구조

```
com.teambind.springproject.room/
├── domain/                           # Domain Layer
│   ├── entity/                       # Aggregate Root, Entity
│   │   ├── RoomOperatingPolicy.java
│   │   └── RoomTimeSlot.java
│   ├── vo/                           # Value Object
│   │   ├── WeeklySlotSchedule.java
│   │   └── ClosedDateRange.java
│   ├── enums/                        # Strategy Pattern
│   │   ├── RecurrencePattern.java
│   │   └── SlotStatus.java
│   ├── service/                      # Domain Service
│   │   ├── TimeSlotGenerationService.java
│   │   └── TimeSlotManagementService.java
│   └── port/                         # Port (Interface)
│       ├── OperatingPolicyPort.java
│       ├── TimeSlotPort.java
│       └── PlaceInfoApiClient.java
│
├── application/                      # Application Layer
│   ├── service/
│   │   ├── RoomSetupApplicationService.java
│   │   ├── ClosedDateSetupApplicationService.java
│   │   └── TimeSlotQueryService.java
│   └── dto/                          # DTO (Input/Output)
│       ├── request/
│       └── response/
│
├── adapter/                          # Infrastructure Layer
│   ├── persistence/                  # Database Adapter
│   │   ├── OperatingPolicyJpaAdapter.java
│   │   ├── TimeSlotJpaAdapter.java
│   │   └── jpa/                      # JPA Repository
│   ├── external/                     # External API Adapter
│   │   └── PlaceInfoApiClientImpl.java
│   └── messaging/                    # Kafka Adapter
│       ├── SlotEventProducer.java
│       └── PolicyEventConsumer.java
│
└── presentation/                     # Presentation Layer
    └── controller/
        └── RoomSetupController.java
```

### Port 작성 가이드라인

1. **도메인 용어 사용**
   ```java
   // Good: 도메인 용어
   public interface OperatingPolicyPort {
       Optional<RoomOperatingPolicy> findByRoomId(Long roomId);
   }

   // Bad: 기술 용어
   public interface OperatingPolicyJpaRepository extends JpaRepository { ... }
   ```

2. **도메인 객체 반환**
   ```java
   // Good: 도메인 Entity 반환
   RoomOperatingPolicy findByRoomId(Long roomId);

   // Bad: DTO 반환
   PolicyDTO findByRoomId(Long roomId);
   ```

3. **간결한 인터페이스**
   ```java
   // Good: 필요한 메서드만 정의
   public interface TimeSlotPort {
       List<RoomTimeSlot> findAvailableSlots(Long roomId, LocalDate date);
       void save(RoomTimeSlot slot);
   }

   // Bad: 불필요한 메서드 포함
   public interface TimeSlotPort extends JpaRepository<RoomTimeSlot, Long> {
       // 너무 많은 메서드 노출
   }
   ```

### 테스트 전략

**1. 도메인 단위 테스트 (외부 의존성 없음)**:
```java
@Test
void shouldGenerateSlotsOn_EvenWeek_ReturnsFalse() {
    // Given
    RoomOperatingPolicy policy = RoomOperatingPolicy.create(
        101L, schedule, RecurrencePattern.ODD_WEEK, List.of()
    );
    LocalDate evenWeekDate = LocalDate.of(2025, 1, 13); // 2025년 2주차 (짝수)

    // When
    boolean shouldGenerate = policy.shouldGenerateSlotsOn(evenWeekDate);

    // Then
    assertThat(shouldGenerate).isFalse();
}
```

**2. Application Service 테스트 (Port Mock)**:
```java
@ExtendWith(MockitoExtension.class)
class RoomSetupApplicationServiceTest {

    @Mock
    private OperatingPolicyPort policyPort;

    @InjectMocks
    private RoomSetupApplicationService service;

    @Test
    void createPolicy_Success() {
        // Given
        when(policyPort.findByRoomId(101L)).thenReturn(Optional.empty());

        // When
        service.createPolicy(command);

        // Then
        verify(policyPort).save(any(RoomOperatingPolicy.class));
    }
}
```

**3. Adapter 통합 테스트 (실제 DB)**:
```java
@DataJpaTest
class OperatingPolicyJpaAdapterTest {

    @Autowired
    private OperatingPolicyJpaAdapter adapter;

    @Test
    void findByRoomId_ReturnsPolicy() {
        // Given: DB에 Policy 저장
        RoomOperatingPolicy policy = adapter.save(createPolicy());

        // When
        Optional<RoomOperatingPolicy> found = adapter.findByRoomId(101L);

        // Then
        assertThat(found).isPresent();
    }
}
```

---

## Consequences

### Positive

- **도메인 보호**: 도메인 로직이 기술 스택 변경에 영향받지 않음
- **테스트 용이**: 도메인 로직을 외부 의존성 없이 테스트 가능
- **확장성**: 새로운 Adapter 추가 시 도메인 코드 무변경
- **명확성**: 각 계층의 책임이 명확하게 분리됨

### Negative

- **초기 개발 비용**: Port/Adapter 인터페이스 작성 시간 증가
  - **완화 방안**:
    - 핵심 도메인 로직에만 Port 적용
    - 단순 CRUD는 직접 Repository 사용 허용

- **코드 증가**: 인터페이스 + 구현체로 클래스 수 증가
  - **완화 방안**:
    - 실질적 이점이 있는 경우에만 Port 작성
    - 단순 조회 API는 QueryService에서 직접 Repository 사용 가능

### Trade-offs

- **퍼포먼스 vs 유지보수성**: Port를 통한 간접 호출로 약간의 성능 오버헤드 발생하지만, 유지보수성 이점이 더 큼
- **단순함 vs 확장성**: 초기 코드 복잡도는 증가하지만, 장기적으로 변경에 유연함

---

## Validation

### 의존성 방향 검증

```bash
# Domain 레이어가 Infrastructure에 의존하지 않는지 확인
grep -r "import.*adapter" domain/
# Expected: 결과 없음

# Application이 Domain만 의존하는지 확인
grep -r "import.*adapter" application/
# Expected: 결과 없음
```

### 테스트 커버리지 검증

```bash
./gradlew test jacocoTestReport

# Domain Layer 커버리지 목표: 90% 이상
# Application Layer 커버리지 목표: 80% 이상
```

---

## References

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Ports and Adapters Pattern](https://herbertograca.com/2017/09/14/ports-adapters-architecture/)
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

---

## Future Considerations

- **Phase 2**: CQRS 패턴 적용 (Command와 Query 완전 분리)
- **Phase 3**: Event Sourcing 도입 (상태 변경 이력 관리)
- **Phase 4**: Multi-Module 구조로 전환 (domain, application, infrastructure 모듈 분리)

---

**Maintained by**: Teambind_dev_backend Team
**Lead Developer**: DDINGJOO
