# API 명세서

**Version**: 1.0.0
**Last Updated**: 2025-01-17
**Base URL**: `/api`

## 목차

- [개요](#개요)
- [인증](#인증)
- [공통 응답 포맷](#공통-응답-포맷)
- [에러 코드](#에러-코드)
- [API 엔드포인트](#api-엔드포인트)
  - [1. 룸 설정 API](#1-룸-설정-api)
  - [2. 예약 API](#2-예약-api)

---

## 개요

Room Time Slot Management Service의 REST API 명세서입니다.

### 주요 기능

- 룸 운영 정책 설정 및 시간 슬롯 자동 생성
- 휴무일 설정 및 슬롯 상태 관리
- 예약 가능 슬롯 조회
- 단일/다중 슬롯 예약

### API 버전 관리

- **v1**: `/api/v1` - 현재 안정 버전
- **Setup API**: `/api/rooms/setup` - 버전 관리 없음 (내부 관리용)

---

## 인증

현재 인증은 구현되지 않았습니다. 추후 JWT 기반 인증이 추가될 예정입니다.

---

## 공통 응답 포맷

### 성공 응답

```json
{
  "data": { ... },
  "timestamp": "2025-01-17T10:30:00"
}
```

### 에러 응답

```json
{
  "timestamp": "2025-01-17T10:30:00",
  "status": 400,
  "code": "SLOT_002",
  "message": "슬롯을 예약할 수 없습니다. 현재 상태: RESERVED",
  "path": "/api/v1/reservations",
  "exceptionType": "DOMAIN"
}
```

---

## 에러 코드

### Slot 관련 (SLOT_0XX)

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| SLOT_001 | 404 | 슬롯을 찾을 수 없음 |
| SLOT_002 | 409 | 슬롯이 예약 불가능 |
| SLOT_003 | 409 | 슬롯이 이미 예약됨 |
| SLOT_004 | 400 | 잘못된 상태 전이 |

### Policy 관련 (POLICY_0XX)

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| POLICY_001 | 404 | 운영 정책을 찾을 수 없음 |
| POLICY_002 | 409 | 운영 정책이 이미 존재 |
| POLICY_003 | 400 | 잘못된 스케줄 설정 |

### Time 관련 (TIME_0XX)

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| TIME_001 | 400 | 잘못된 시간 범위 |
| TIME_002 | 400 | 과거 날짜 불허 |
| TIME_003 | 500 | 슬롯 생성 실패 |

---

## API 엔드포인트

## 1. 룸 설정 API

### 1.1. 룸 운영 정책 설정 및 슬롯 생성 요청

룸의 운영 시간 정책을 설정하고 시간 슬롯을 자동으로 생성합니다.

```
POST /api/rooms/setup
```

#### Request Body

```json
{
  "roomId": 101,
  "slots": [
    {
      "dayOfWeek": "MONDAY",
      "startTimes": ["09:00", "10:00", "11:00", "14:00", "15:00"],
      "recurrencePattern": "EVERY_WEEK"
    },
    {
      "dayOfWeek": "TUESDAY",
      "startTimes": ["09:00", "10:00", "11:00"],
      "recurrencePattern": "EVERY_WEEK"
    }
  ]
}
```

#### Request Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| roomId | Long | ✅ | 룸 ID |
| slots | Array | ✅ | 요일별 슬롯 시작 시각 목록 |
| slots[].dayOfWeek | String | ✅ | 요일 (MONDAY ~ SUNDAY) |
| slots[].startTimes | Array<String> | ✅ | 시작 시각 목록 (HH:mm 형식) |
| slots[].recurrencePattern | String | ✅ | 반복 패턴 (EVERY_WEEK, ODD_WEEK, EVEN_WEEK) |

#### Response (202 Accepted)

```json
{
  "requestId": "abc-123-def-456",
  "roomId": 101,
  "startDate": "2025-01-17",
  "endDate": "2025-02-16",
  "status": "REQUESTED",
  "requestedAt": "2025-01-17T10:30:00"
}
```

#### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| requestId | String | 슬롯 생성 요청 ID (UUID) |
| roomId | Long | 룸 ID |
| startDate | String | 슬롯 생성 시작 날짜 |
| endDate | String | 슬롯 생성 종료 날짜 |
| status | String | 요청 상태 (REQUESTED, PROCESSING, COMPLETED, FAILED) |
| requestedAt | String | 요청 시각 (ISO 8601) |

#### 처리 플로우

```
1. 운영 정책 저장 (RoomOperatingPolicy)
   ↓
2. 슬롯 생성 요청 저장 (SlotGenerationRequest)
   ↓
3. Kafka 이벤트 발행 (SlotGenerationRequestedEvent)
   ↓
4. 202 Accepted 응답 반환
   ↓
5. 비동기 슬롯 생성 (백그라운드)
```

#### cURL Example

```bash
curl -X POST http://localhost:8080/api/rooms/setup \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 101,
    "slots": [
      {
        "dayOfWeek": "MONDAY",
        "startTimes": ["09:00", "10:00", "11:00"],
        "recurrencePattern": "EVERY_WEEK"
      }
    ]
  }'
```

---

### 1.2. 슬롯 생성 상태 조회

슬롯 생성 요청의 진행 상태를 조회합니다.

```
GET /api/rooms/setup/{requestId}/status
```

#### Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| requestId | String | ✅ | 슬롯 생성 요청 ID |

#### Response (200 OK)

```json
{
  "requestId": "abc-123-def-456",
  "roomId": 101,
  "status": "COMPLETED",
  "totalSlotsGenerated": 150,
  "startDate": "2025-01-17",
  "endDate": "2025-02-16",
  "requestedAt": "2025-01-17T10:30:00",
  "completedAt": "2025-01-17T10:31:00"
}
```

#### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| requestId | String | 슬롯 생성 요청 ID |
| roomId | Long | 룸 ID |
| status | String | 요청 상태 (REQUESTED, PROCESSING, COMPLETED, FAILED) |
| totalSlotsGenerated | Integer | 생성된 슬롯 개수 (COMPLETED 시에만) |
| startDate | String | 슬롯 생성 시작 날짜 |
| endDate | String | 슬롯 생성 종료 날짜 |
| requestedAt | String | 요청 시각 |
| completedAt | String | 완료 시각 (COMPLETED 시에만) |
| failedAt | String | 실패 시각 (FAILED 시에만) |
| errorMessage | String | 에러 메시지 (FAILED 시에만) |

#### cURL Example

```bash
curl -X GET http://localhost:8080/api/rooms/setup/abc-123-def-456/status
```

---

### 1.3. 휴무일 설정

룸의 휴무일을 설정하고 해당 날짜의 슬롯 상태를 CLOSED로 변경합니다.

```
POST /api/rooms/setup/closed-dates
```

#### Request Body

```json
{
  "roomId": 101,
  "closedDates": [
    {
      "startDate": "2025-01-01",
      "endDate": "2025-01-01",
      "startTime": null,
      "endTime": null,
      "reason": "신정"
    },
    {
      "startDate": "2025-02-10",
      "endDate": "2025-02-10",
      "startTime": "14:00",
      "endTime": "18:00",
      "reason": "임시 휴무"
    }
  ]
}
```

#### Request Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| roomId | Long | ✅ | 룸 ID |
| closedDates | Array | ✅ | 휴무일 목록 |
| closedDates[].startDate | String | ✅ | 시작 날짜 (yyyy-MM-dd) |
| closedDates[].endDate | String | ✅ | 종료 날짜 (yyyy-MM-dd) |
| closedDates[].startTime | String | ❌ | 시작 시각 (HH:mm, null이면 전일) |
| closedDates[].endTime | String | ❌ | 종료 시각 (HH:mm, null이면 전일) |
| closedDates[].reason | String | ❌ | 휴무 사유 |

#### Response (202 Accepted)

```json
{
  "requestId": "def-456-ghi-789",
  "roomId": 101,
  "closedDateCount": 2,
  "status": "REQUESTED",
  "requestedAt": "2025-01-17T10:30:00"
}
```

#### cURL Example

```bash
curl -X POST http://localhost:8080/api/rooms/setup/closed-dates \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 101,
    "closedDates": [
      {
        "startDate": "2025-01-01",
        "endDate": "2025-01-01",
        "reason": "신정"
      }
    ]
  }'
```

---

## 2. 예약 API

### 2.1. 예약 가능 슬롯 조회

특정 룸의 특정 날짜에 예약 가능한 슬롯 목록을 조회합니다.

```
GET /api/v1/reservations/available-slots
```

#### Query Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| roomId | Long | ✅ | 룸 ID |
| date | String | ✅ | 조회할 날짜 (yyyy-MM-dd) |

#### Response (200 OK)

```json
[
  {
    "slotId": 12345,
    "roomId": 101,
    "slotDate": "2025-01-20",
    "slotTime": "09:00",
    "status": "AVAILABLE"
  },
  {
    "slotId": 12346,
    "roomId": 101,
    "slotDate": "2025-01-20",
    "slotTime": "10:00",
    "status": "AVAILABLE"
  }
]
```

#### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| slotId | Long | 슬롯 ID |
| roomId | Long | 룸 ID |
| slotDate | String | 슬롯 날짜 |
| slotTime | String | 슬롯 시각 (HH:mm) |
| status | String | 슬롯 상태 (항상 AVAILABLE) |

#### cURL Example

```bash
curl -X GET "http://localhost:8080/api/v1/reservations/available-slots?roomId=101&date=2025-01-20"
```

---

### 2.2. 단일 슬롯 예약

특정 시간 슬롯을 예약 대기 상태(PENDING)로 변경합니다.

```
POST /api/v1/reservations
```

#### Request Body

```json
{
  "roomId": 101,
  "slotDate": "2025-01-20",
  "slotTime": "14:00",
  "reservationId": 567890123456789
}
```

#### Request Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| roomId | Long | ✅ | 룸 ID |
| slotDate | String | ✅ | 슬롯 날짜 (yyyy-MM-dd) |
| slotTime | String | ✅ | 슬롯 시각 (HH:mm) |
| reservationId | Long | ✅ | 예약 ID (외부 시스템에서 생성) |

#### Response (200 OK)

```
(Empty Body)
```

#### 처리 플로우

```
1. 슬롯 조회 및 검증 (AVAILABLE 상태 확인)
   ↓
2. 슬롯 상태를 PENDING으로 변경
   ↓
3. Kafka 이벤트 발행 (SlotReservedEvent)
   ↓
4. 200 OK 응답 반환
```

#### Error Responses

- **404 NOT FOUND**: 슬롯을 찾을 수 없음
- **409 CONFLICT**: 슬롯이 이미 예약됨 또는 예약 불가능

#### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 101,
    "slotDate": "2025-01-20",
    "slotTime": "14:00",
    "reservationId": 567890123456789
  }'
```

---

### 2.3. 다중 슬롯 예약 (신규)

특정 날짜의 여러 시간 슬롯을 한 번에 예약 대기 상태로 변경합니다.

```
POST /api/v1/reservations/multi
```

#### Request Body

```json
{
  "roomId": 101,
  "slotDate": "2025-01-20",
  "slotTimes": ["14:00", "15:00", "16:00"]
}
```

#### Request Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| roomId | Long | ✅ | 룸 ID |
| slotDate | String | ✅ | 슬롯 날짜 (yyyy-MM-dd) |
| slotTimes | Array<String> | ✅ | 슬롯 시각 목록 (HH:mm) |

#### Response (200 OK)

```json
{
  "reservationId": 567890123456789,
  "roomId": 101,
  "slotDate": "2025-01-20",
  "reservedSlotTimes": ["14:00", "15:00", "16:00"]
}
```

#### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| reservationId | Long | 예약 ID (Snowflake ID 자동 생성) |
| roomId | Long | 룸 ID |
| slotDate | String | 슬롯 날짜 |
| reservedSlotTimes | Array<String> | 예약된 슬롯 시각 목록 |

#### 주요 특징

- **예약 ID 자동 생성**: Snowflake ID Generator 사용
- **Pessimistic Lock**: `SELECT FOR UPDATE`로 동시성 문제 해결
- **원자적 처리**: 하나라도 예약 불가능하면 전체 롤백
- **일괄 검증**: 모든 슬롯이 AVAILABLE 상태인지 사전 확인

#### 처리 플로우

```
1. 예약 ID 자동 생성 (Snowflake)
   ↓
2. 슬롯 조회 (Pessimistic Lock)
   ↓
3. 모든 슬롯이 AVAILABLE인지 검증
   ↓
4. 모든 슬롯 상태를 PENDING으로 변경
   ↓
5. Kafka 이벤트 발행 (SlotReservedEvent)
   ↓
6. 200 OK 응답 반환
```

#### Error Responses

- **404 NOT FOUND**: 슬롯을 찾을 수 없음
- **409 CONFLICT**: 슬롯이 이미 예약됨 또는 예약 불가능

#### cURL Example

```bash
curl -X POST http://localhost:8080/api/v1/reservations/multi \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 101,
    "slotDate": "2025-01-20",
    "slotTimes": ["14:00", "15:00", "16:00"]
  }'
```

---

## 부록

### A. 슬롯 상태 (SlotStatus)

| 상태 | 설명 |
|------|------|
| AVAILABLE | 예약 가능 |
| PENDING | 예약 대기 (40분 후 자동 만료) |
| RESERVED | 예약 확정 |
| CANCELLED | 예약 취소 |
| CLOSED | 휴무일 |

### B. 반복 패턴 (RecurrencePattern)

| 패턴 | 설명 |
|------|------|
| EVERY_WEEK | 매주 반복 |
| ODD_WEEK | 홀수 주차만 반복 |
| EVEN_WEEK | 짝수 주차만 반복 |

### C. 요일 (DayOfWeek)

```
MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
```

### D. 요청 상태 (GenerationStatus)

| 상태 | 설명 |
|------|------|
| REQUESTED | 요청됨 |
| PROCESSING | 처리 중 |
| COMPLETED | 완료 |
| FAILED | 실패 |

---

## 변경 이력

### v1.0.0 (2025-01-17)

- 초기 API 명세 작성
- 룸 설정 API 3개 (운영 정책 설정, 상태 조회, 휴무일 설정)
- 예약 API 3개 (가능 슬롯 조회, 단일 예약, 다중 예약)

---

**Maintained by**: Teambind_dev_backend Team
