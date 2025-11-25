# ADR-008: Payment Completion and Slot Confirmation

**Status**: Proposed
**Date**: 2025-01-25
**Decision Makers**: DDINGJOO, Teambind Backend Team
**Tags**: `event-driven`, `slot-management`, `payment-integration`, `kafka`

---

## Context

### 문제 상황

현재 시스템은 슬롯 예약 시 다음과 같은 흐름으로 동작합니다:

```
1. 슬롯 예약 요청 → AVAILABLE → PENDING 상태로 변경
2. 결제 서비스로 결제 요청 전송
3. PENDING 슬롯은 40분 후 자동으로 AVAILABLE로 복구 (만료 처리)
```

**문제점**:
- 결제가 완료되어도 슬롯 상태가 PENDING으로 유지됨
- 만료 시간(40분)이 지나면 결제 완료된 슬롯도 AVAILABLE로 복구될 위험
- 결제 완료와 슬롯 확정 사이의 연결 고리 부재

### 요구사항

결제 서비스로부터 `payment-completed` 이벤트를 수신하면:
1. 해당 예약의 PENDING 슬롯을 RESERVED 상태로 확정
2. 만료 복구 로직에서 제외되도록 보장
3. 결제 완료 시점 정보를 슬롯에 기록

---

## Decision

**결제 완료 이벤트 기반 슬롯 확정(Slot Confirmation) 메커니즘을 도입한다.**

### 1. 이벤트 스키마

**Topic**: `payment-completed`

**Payload**:
```json
{
  "paymentId": "long",
  "reservationId": "long",
  "orderId": "string",
  "paymentKey": "string",
  "amount": "long",
  "method": "string",
  "paidAt": "ISO-8601 datetime string"
}
```

**Example**:
```json
{
  "paymentId": 12345,
  "reservationId": 67890,
  "orderId": "ORDER-2025-01-25-001",
  "paymentKey": "TOSS-PAY-KEY-12345",
  "amount": 50000,
  "method": "CARD",
  "paidAt": "2025-01-25T14:30:00+09:00"
}
```

### 2. 이벤트 핸들러 구현

**위치**: `com.teambind.springproject.room.event.handler.PaymentCompletedEventHandler`

**책임**:
- Kafka `payment-completed` 토픽 구독
- 결제 완료 이벤트 수신 시 슬롯 확정 처리
- Domain Service를 통한 상태 전이 수행

**구현 방향**:
```java
@Component
@Slf4j
public class PaymentCompletedEventHandler {

    private final TimeSlotManagementService timeSlotManagementService;
    private final TimeSlotPort timeSlotPort;

    @KafkaListener(topics = "payment-completed", groupId = "room-service")
    public void handlePaymentCompleted(PaymentCompletedEventMessage message) {
        log.info("Payment completed event received: reservationId={}, paymentId={}",
                message.getReservationId(), message.getPaymentId());

        try {
            // 1. 예약 ID로 PENDING 상태의 슬롯 조회
            List<RoomTimeSlot> pendingSlots = timeSlotPort.findByReservationId(
                message.getReservationId()
            );

            if (pendingSlots.isEmpty()) {
                log.warn("No pending slots found for reservationId={}",
                        message.getReservationId());
                return;
            }

            // 2. 각 슬롯을 RESERVED 상태로 확정
            for (RoomTimeSlot slot : pendingSlots) {
                timeSlotManagementService.confirmSlot(
                    slot.getRoomId(),
                    slot.getSlotDate(),
                    slot.getSlotTime()
                );
            }

            log.info("Confirmed {} slots for reservationId={}, paymentId={}",
                    pendingSlots.size(), message.getReservationId(), message.getPaymentId());

        } catch (Exception e) {
            log.error("Failed to confirm slots for reservationId={}",
                    message.getReservationId(), e);
            // TODO: 재처리 로직 또는 DLQ 전송
        }
    }
}
```

### 3. DTO 정의

**위치**: `com.teambind.springproject.message.dto.PaymentCompletedEventMessage`

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEventMessage {

    private Long paymentId;
    private Long reservationId;
    private String orderId;
    private String paymentKey;
    private Long amount;
    private String method;
    private LocalDateTime paidAt;
}
```

### 4. 슬롯 상태 전이

**기존 Domain Service 활용**:
```java
// TimeSlotManagementService.confirmSlot() 메서드 사용
public void confirmSlot(Long roomId, LocalDate slotDate, LocalTime slotTime) {
    RoomTimeSlot slot = findSlot(roomId, slotDate, slotTime);

    // 도메인 로직: PENDING → RESERVED 상태 전이
    slot.confirm();
    timeSlotPort.save(slot);

    log.info("Slot confirmed: slotId={}, roomId={}, reservationId={}",
            slot.getSlotId(), roomId, slot.getReservationId());
}
```

**RoomTimeSlot Entity**:
```java
public void confirm() {
    if (status != SlotStatus.PENDING) {
        throw new InvalidSlotStateTransitionException(
            String.format("Cannot confirm slot in %s state", status)
        );
    }
    this.status = SlotStatus.RESERVED;
    this.lastUpdated = LocalDateTime.now();
}
```

### 5. 만료 복구 로직 보호

**기존 로직**:
```java
// TimeSlotManagementServiceImpl.restoreExpiredPendingSlots()
public int restoreExpiredPendingSlots() {
    // PENDING 상태이고 lastUpdated가 40분 이전인 슬롯만 조회
    List<RoomTimeSlot> expiredSlots = timeSlotPort.findExpiredPendingSlots(
        pendingExpirationMinutes
    );

    for (RoomTimeSlot slot : expiredSlots) {
        slot.cancel(); // PENDING → AVAILABLE
    }

    timeSlotPort.saveAll(expiredSlots);
    return expiredSlots.size();
}
```

**보호 메커니즘**:
- `findExpiredPendingSlots()`는 **PENDING 상태만** 조회
- 결제 완료 시 RESERVED로 변경되므로 자동으로 제외됨
- 추가 검증 로직 불필요

---

## Architecture Alignment

### Hexagonal Architecture 준수

**Event Handler → Domain Service → Entity** 흐름 유지:

```
[Infrastructure Layer]
    PaymentCompletedEventHandler (Kafka Consumer)
        ↓
[Domain Layer]
    TimeSlotManagementService (Domain Service)
        ↓
    RoomTimeSlot.confirm() (Entity)
        ↓
    TimeSlotPort (Port Interface)
        ↓
[Infrastructure Layer]
    TimeSlotJpaAdapter (Adapter)
```

**장점**:
- Infrastructure 계층(Kafka)이 Domain 계층을 직접 수정하지 않음
- Domain Service를 통해 비즈니스 로직 캡슐화
- Port/Adapter 패턴으로 기술 스택 변경 용이

### ADR-007 정합성

본 설계는 **ADR-007: Event Handler Architecture Alignment**의 원칙을 따릅니다:

1. **Domain Service 활용**: `timeSlotManagementService.confirmSlot()` 호출
2. **트랜잭션 원자성**: `@Transactional` 메서드 사용
3. **Port 인터페이스 의존**: Infrastructure 계층 직접 접근 금지

---

## Consequences

### 장점

1. **결제-슬롯 동기화 보장**
   - 결제 완료 시 즉시 슬롯 확정
   - 만료 복구 로직에서 자동 제외

2. **이벤트 기반 느슨한 결합**
   - 결제 서비스와 룸 서비스 간 직접 의존성 없음
   - Kafka 기반 비동기 통신

3. **아키텍처 일관성**
   - 기존 Hexagonal Architecture 패턴 유지
   - Domain Service 재사용

4. **확장성**
   - 추후 결제 취소 이벤트(`payment-cancelled`) 처리 용이
   - 환불 완료 이벤트(`refund-completed`)와 동일 패턴 적용 가능

### 단점 및 트레이드오프

1. **이벤트 유실 위험**
   - Kafka 장애 시 슬롯 확정 누락 가능
   - **완화 방안**: Kafka acks=all, 최소 2 replica 설정

2. **순서 보장 이슈**
   - 결제 완료 이벤트가 만료 복구 스케줄러보다 늦게 도착할 가능성
   - **완화 방안**:
     - 결제 서비스에서 즉시 발행 (지연 최소화)
     - 만료 시간 40분으로 충분한 여유 확보

3. **중복 이벤트 처리**
   - 동일 paymentId로 중복 이벤트 수신 가능
   - **완화 방안**: `confirmSlot()`의 멱등성 보장 (PENDING이 아니면 예외 발생)

---

## 시나리오별 동작

### 정상 케이스

```
1. 사용자 슬롯 예약 → PENDING 상태로 변경 (reservationId=123)
2. 결제 요청 → 결제 서비스로 전송
3. 결제 완료 → payment-completed 이벤트 발행 (5초 소요)
4. PaymentCompletedEventHandler가 이벤트 수신
5. confirmSlot() 호출 → PENDING → RESERVED
6. 40분 후 만료 복구 스케줄러 실행 → RESERVED 슬롯은 제외됨
```

### 결제 실패 케이스

```
1. 사용자 슬롯 예약 → PENDING 상태로 변경
2. 결제 요청 → 결제 실패
3. payment-completed 이벤트 발행되지 않음
4. 40분 후 만료 복구 스케줄러 실행 → PENDING → AVAILABLE
```

### 결제 완료 이벤트 지연 케이스

```
1. 사용자 슬롯 예약 → PENDING (10:00)
2. 결제 완료 → payment-completed 이벤트 발행 (10:02)
3. Kafka 지연으로 이벤트 수신 지연 (10:45 수신)
4. 만료 복구 스케줄러가 먼저 실행 (10:40) → AVAILABLE로 복구
5. PaymentCompletedEventHandler가 이벤트 수신 (10:45)
6. confirmSlot() 호출 → 슬롯 상태가 AVAILABLE이므로 예외 발생

**문제**: 결제 완료되었으나 슬롯이 복구됨
**완화 방안**:
- 결제 서비스에서 즉시 발행 (지연 최소화)
- 만료 시간을 충분히 길게 설정 (현재 40분)
- 추후 보상 트랜잭션(Saga) 패턴 도입 검토
```

---

## 구현 체크리스트

### Phase 1: 이벤트 수신 인프라 (우선순위: High)

- [ ] `PaymentCompletedEventMessage` DTO 생성
- [ ] `PaymentCompletedEventHandler` Kafka Consumer 구현
- [ ] Kafka 토픽 `payment-completed` 생성 및 설정
- [ ] Consumer Group ID 설정 (`room-service`)

### Phase 2: 슬롯 확정 로직 (우선순위: High)

- [ ] `confirmSlot()` 메서드 활용 (이미 구현됨)
- [ ] `findByReservationId()` Port 메서드 확인 (이미 구현됨)
- [ ] 트랜잭션 경계 설정 확인

### Phase 3: 테스트 (우선순위: High)

- [ ] 단위 테스트: `PaymentCompletedEventHandler` 테스트
  - Mock Kafka 메시지로 정상 케이스 검증
  - 슬롯이 없는 경우 예외 처리 검증
- [ ] 통합 테스트: 결제 완료 → 슬롯 확정 E2E 테스트
- [ ] 만료 복구 로직과 통합 테스트 (RESERVED 슬롯 제외 확인)

### Phase 4: 모니터링 및 알림 (우선순위: Medium)

- [ ] 슬롯 확정 성공/실패 로그 추가
- [ ] 메트릭 수집 (Micrometer)
  - `slot.confirmed.count`
  - `slot.confirmation.failed.count`
- [ ] 실패 시 알림 전송 (Slack, Email)

### Phase 5: 에러 처리 및 복원력 (우선순위: Medium)

- [ ] Dead Letter Queue(DLQ) 설정
- [ ] 재시도 정책 설정 (Spring Kafka Retry)
- [ ] 멱등성 보장 검증

---

## 연관 ADR

- **[ADR-004: Transactional Outbox Pattern](004-transactional-outbox-pattern.md)**: 이벤트 발행 신뢰성
- **[ADR-004-1: Hybrid Outbox Pattern](004-1-hybrid-outbox-pattern.md)**: 즉시 발행 + 백업 전략
- **[ADR-007: Event Handler Architecture Alignment](007-event-handler-architecture-alignment.md)**: 핸들러 아키텍처 정합성

---

## 참고 자료

### Payment Service Event Specification

결제 서비스에서 발행하는 `payment-completed` 이벤트 스펙:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| paymentId | Long | Yes | 결제 고유 ID |
| reservationId | Long | Yes | 예약 ID (외래 키) |
| orderId | String | Yes | 주문 ID |
| paymentKey | String | Yes | 결제 승인 키 (토스페이먼츠) |
| amount | Long | Yes | 결제 금액 (원) |
| method | String | Yes | 결제 수단 (CARD, TRANSFER 등) |
| paidAt | LocalDateTime | Yes | 결제 완료 시각 (ISO-8601) |

### 관련 도메인 엔티티

**RoomTimeSlot 상태 전이**:
```
AVAILABLE → PENDING → RESERVED (결제 완료)
          ↓
      AVAILABLE (만료 복구)
```

**SlotStatus Enum**:
```java
public enum SlotStatus {
    AVAILABLE,  // 예약 가능
    PENDING,    // 예약 대기 (결제 진행 중)
    RESERVED,   // 예약 확정 (결제 완료)
    CLOSED      // 휴무일/운영 종료
}
```

---

## 버전 이력

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-01-25 | DDINGJOO | Initial proposal |

---

**Maintained by**: Teambind Backend Team
**Lead Developer**: DDINGJOO