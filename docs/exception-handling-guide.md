# 예외 처리 가이드

## 개요

이 문서는 시간 관리 도메인에서 사용하는 예외 처리 방식을 설명합니다.
모든 예외는 `CustomException`을 상속하며, 도메인 예외와 애플리케이션 예외로 구분됩니다.

## 예외 계층 구조

```
CustomException (추상 클래스)
├── Domain Exception (도메인 규칙 위반)
│   ├── SlotNotFoundException
│   ├── SlotNotAvailableException
│   ├── InvalidSlotStateTransitionException
│   ├── PolicyNotFoundException
│   └── PolicyAlreadyExistsException
└── Application Exception (입력 검증, 비즈니스 로직)
    ├── InvalidTimeRangeException
    ├── PastDateNotAllowedException
    ├── InvalidScheduleException
    └── SlotGenerationFailedException
```

## 에러 코드 체계

### Slot 관련 (SLOT_0XX)

- `SLOT_001`: 슬롯을 찾을 수 없음 (404)
- `SLOT_002`: 슬롯이 예약 불가능 (409)
- `SLOT_003`: 슬롯이 이미 예약됨 (409)
- `SLOT_004`: 잘못된 상태 전이 (400)
- `SLOT_005`: 슬롯이 이미 취소됨 (400)
- `SLOT_006`: 슬롯을 수정할 수 없음 (400)

### Policy 관련 (POLICY_0XX)

- `POLICY_001`: 운영 정책을 찾을 수 없음 (404)
- `POLICY_002`: 운영 정책이 이미 존재 (409)
- `POLICY_003`: 잘못된 스케줄 설정 (400)
- `POLICY_004`: 잘못된 휴무일 범위 (400)

### Time 관련 (TIME_0XX)

- `TIME_001`: 잘못된 시간 범위 (400)
- `TIME_002`: 과거 날짜 불허 (400)
- `TIME_003`: 슬롯 생성 실패 (500)

## Domain Exception 사용 예제

### 1. SlotNotFoundException

슬롯을 찾을 수 없을 때 사용합니다.

```java
// 기본 사용
throw new SlotNotFoundException();

// Slot ID로 생성
throw new

SlotNotFoundException(123L);

// Room ID, Date, Time으로 생성
throw new

SlotNotFoundException(101L,"2025-01-15","14:00");
```

**응답 예시:**

```json
{
  "timestamp": "2025-01-15T14:30:00",
  "status": 404,
  "code": "SLOT_001",
  "message": "슬롯을 찾을 수 없습니다. ID: 123",
  "path": "/api/slots/123",
  "exceptionType": "DOMAIN"
}
```

### 2. SlotNotAvailableException

슬롯이 예약 가능하지 않을 때 사용합니다.

```java
// 현재 상태와 함께
throw new SlotNotAvailableException("RESERVED");

// Slot ID와 상태
throw new

SlotNotAvailableException(123L,"PENDING");
```

**응답 예시:**

```json
{
  "timestamp": "2025-01-15T14:30:00",
  "status": 409,
  "code": "SLOT_002",
  "message": "슬롯을 예약할 수 없습니다. 현재 상태: RESERVED",
  "path": "/api/slots/123/reserve",
  "exceptionType": "DOMAIN"
}
```

### 3. InvalidSlotStateTransitionException

슬롯 상태 전이가 유효하지 않을 때 사용합니다.

```java
// 현재 상태와 목표 상태
throw new InvalidSlotStateTransitionException("RESERVED","PENDING");

// 커스텀 메시지
throw new

InvalidSlotStateTransitionException("이미 확정된 예약은 취소할 수 없습니다");
```

### 4. PolicyNotFoundException

운영 정책을 찾을 수 없을 때 사용합니다.

```java
// Policy ID로 생성
throw new PolicyNotFoundException(456L);

// Room ID로 생성
throw new

PolicyNotFoundException(101L,true);
```

### 5. PolicyAlreadyExistsException

운영 정책이 이미 존재할 때 사용합니다.

```java
throw new PolicyAlreadyExistsException(101L);
```

## Application Exception 사용 예제

### 1. InvalidTimeRangeException

시간 범위가 유효하지 않을 때 사용합니다.

```java
// 종료 시간이 시작 시간보다 이전
throw InvalidTimeRangeException.endBeforeStart("14:00","13:00");

// 종료 날짜가 시작 날짜보다 이전
throw InvalidTimeRangeException.

endDateBeforeStartDate("2025-01-20","2025-01-15");

// 잘못된 기간
throw InvalidTimeRangeException.

invalidDuration("-5 days");
```

**응답 예시:**

```json
{
  "timestamp": "2025-01-15T14:30:00",
  "status": 400,
  "code": "TIME_001",
  "message": "종료 시간이 시작 시간보다 이전입니다. 시작: 14:00, 종료: 13:00",
  "path": "/api/slots",
  "exceptionType": "APPLICATION"
}
```

### 2. PastDateNotAllowedException

과거 날짜가 허용되지 않을 때 사용합니다.

```java
// 날짜만
throw new PastDateNotAllowedException("2024-01-01");

// 필드명과 날짜
throw new

PastDateNotAllowedException("startDate","2024-01-01");
```

### 3. InvalidScheduleException

스케줄 설정이 유효하지 않을 때 사용합니다.

```java
// 빈 스케줄
throw InvalidScheduleException.emptySchedule();

// 잘못된 슬롯 시간
throw InvalidScheduleException.

invalidSlotTime("MONDAY","25:00");

// 중복된 슬롯 시간
throw InvalidScheduleException.

duplicateSlotTime("TUESDAY","09:00");
```

### 4. SlotGenerationFailedException

타임 슬롯 생성에 실패했을 때 사용합니다.

```java
// 날짜로 생성
throw SlotGenerationFailedException.forDate("2025-01-15",cause);

// Room ID로 생성
throw SlotGenerationFailedException.

forRoom(101L,cause);
```

**응답 예시:**

```json
{
  "timestamp": "2025-01-15T14:30:00",
  "status": 500,
  "code": "TIME_003",
  "message": "타임 슬롯 생성에 실패했습니다. 날짜: 2025-01-15",
  "path": "/api/slots/generate",
  "exceptionType": "APPLICATION"
}
```

## 서비스 레이어 사용 예제

### Slot 조회

```java
public RoomTimeSlot getSlot(Long slotId) {
	return slotRepository.findById(slotId)
			.orElseThrow(() -> new SlotNotFoundException(slotId));
}
```

### Slot 예약

```java
public void reserveSlot(Long slotId, Long reservationId) {
	RoomTimeSlot slot = getSlot(slotId);
	
	if (!slot.isAvailable()) {
		throw new SlotNotAvailableException(slotId, slot.getStatus().name());
	}
	
	try {
		slot.markAsPending(reservationId);
		slotRepository.save(slot);
	} catch (IllegalStateException e) {
		throw new InvalidSlotStateTransitionException(e.getMessage());
	}
}
```

### Policy 생성

```java
public RoomOperatingPolicy createPolicy(Long roomId, WeeklySlotSchedule schedule) {
	if (policyRepository.existsByRoomId(roomId)) {
		throw new PolicyAlreadyExistsException(roomId);
	}
	
	if (schedule.getSlotTimes().isEmpty()) {
		throw InvalidScheduleException.emptySchedule();
	}
	
	return policyRepository.save(
			RoomOperatingPolicy.create(roomId, schedule, RecurrencePattern.EVERY_WEEK, List.of())
	);
}
```

### Slot 생성

```java
public List<RoomTimeSlot> generateSlots(Long roomId, LocalDate date) {
	if (date.isBefore(LocalDate.now())) {
		throw new PastDateNotAllowedException("date", date.toString());
	}
	
	RoomOperatingPolicy policy = policyRepository.findByRoomId(roomId)
			.orElseThrow(() -> new PolicyNotFoundException(roomId, true));
	
	try {
		List<RoomTimeSlot> slots = policy.generateSlotsFor(date, SlotUnit.HOUR);
		return slotRepository.saveAll(slots);
	} catch (Exception e) {
		throw SlotGenerationFailedException.forRoom(roomId, e);
	}
}
```

## 글로벌 예외 핸들러

모든 예외는 `GlobalExceptionHandler`에서 처리되며, 일관된 형식의 응답을 반환합니다.

```java

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handlePlaceException(
			CustomException ex, HttpServletRequest request) {
		ErrorResponse errorResponse = ErrorResponse.of(ex, request.getRequestURI());
		return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
	}
}
```

## 베스트 프랙티스

1. **도메인 예외**: 도메인 규칙 위반 시 사용
	- 엔티티를 찾을 수 없음
	- 상태 전이 규칙 위반
	- 비즈니스 제약 조건 위반

2. **애플리케이션 예외**: 입력 검증, 비즈니스 로직 오류 시 사용
	- 잘못된 입력값
	- 과거 날짜 검증
	- 스케줄 설정 오류
	- 시스템 오류

3. **예외 메시지**: 사용자가 이해할 수 있는 명확한 메시지 작성

4. **에러 코드**: 클라이언트가 에러를 구분할 수 있도록 일관된 코드 사용

5. **HTTP 상태 코드**: 적절한 HTTP 상태 코드 사용
	- 404: Not Found
	- 409: Conflict
	- 400: Bad Request
	- 500: Internal Server Error
