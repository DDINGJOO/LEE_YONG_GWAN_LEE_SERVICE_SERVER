# ADR-001: 데이터베이스 선택

**Status**: Accepted
**Date**: 2025-01-15
**Decision Makers**: Teambind_dev_backend Team
**Technical Story**: Room Time Slot Management Service의 데이터베이스 선택

---

## Context

Room Time Slot Management Service는 음악 스튜디오, 공연장 등의 시간 슬롯 관리 및 예약 가능 시간 조회를 담당하는 마이크로서비스입니다. 적절한 데이터베이스를 선택하기 위해 다음 요구사항을 고려했습니다:

### 요구사항

1. **관계형 데이터 모델**
   - 운영 정책(Policy)과 시간 슬롯(Slot) 간의 명확한 관계
   - 외래 키 제약 조건을 통한 데이터 무결성 보장
   - 복잡한 조인 쿼리 지원

2. **트랜잭션 지원**
   - ACID 속성 보장
   - 슬롯 상태 변경 시 일관성 유지
   - 동시성 제어 (Optimistic Locking)

3. **시간 데이터 처리**
   - DATETIME, TIMESTAMP 타입 지원
   - 타임존 설정 기능
   - 날짜/시간 함수 (NOW(), DATE_ADD() 등)

4. **성능**
   - 복합 인덱스를 통한 빠른 조회
   - 대량 INSERT (일일 슬롯 생성)
   - 효율적인 범위 검색 (날짜 범위 조회)

5. **운영**
   - 안정성과 성숙도
   - 커뮤니티 지원
   - Spring Boot와의 통합 용이성

---

## Decision Drivers

- 관계형 데이터 모델의 필요성
- ACID 트랜잭션 보장
- 타임존 처리 요구사항
- Spring Data JPA 호환성
- 팀의 기술 스택 및 운영 경험
- 오픈소스 라이선스 및 비용

---

## Considered Options

### Option 1: MySQL 8.0 / MariaDB 10.11 LTS

**장점**:
- 성숙하고 안정적인 RDBMS
- Spring Boot와의 완벽한 통합
- 우수한 성능 (InnoDB 스토리지 엔진)
- 타임존 처리 기능 (SET time_zone)
- 복합 인덱스 지원
- 대규모 커뮤니티 및 문서
- 무료 오픈소스 (GPL 라이선스)
- 팀 내 운영 경험 보유

**단점**:
- NoSQL 대비 수평 확장 제약
- JSON 기능이 PostgreSQL 대비 제한적

**기술적 평가**:
```sql
-- 복합 인덱스를 활용한 효율적인 조회
CREATE INDEX idx_room_date_time ON room_time_slots (room_id, slot_date, slot_time);

-- 슬롯 조회 쿼리 성능 (EXPLAIN 결과)
EXPLAIN SELECT * FROM room_time_slots
WHERE room_id = 101
  AND slot_date BETWEEN '2025-01-15' AND '2025-01-20';
-- key: idx_room_date_time, rows: ~100 (인덱스 사용)
```

### Option 2: PostgreSQL 14+

**장점**:
- 강력한 JSON/JSONB 지원
- 고급 쿼리 기능 (CTE, Window Functions)
- 뛰어난 확장성
- MVCC 기반 동시성 제어
- 엄격한 데이터 무결성

**단점**:
- 팀 내 운영 경험 부족
- MySQL 대비 설정 복잡도 높음
- 메모리 사용량이 더 높음
- 프로젝트에 JSON 활용 요구사항 없음

**평가**:
- 본 프로젝트에서 PostgreSQL의 고급 기능(JSON, CTE 등)이 필요하지 않음
- 운영 학습 곡선 대비 실질적 이점 적음

### Option 3: MongoDB 6.0+

**장점**:
- 유연한 스키마
- 수평 확장 용이
- 대용량 데이터 처리

**단점**:
- 관계형 데이터 모델 표현 부적합
- 트랜잭션 지원 제한적 (4.0 이후 개선되었으나 RDBMS 대비 제약)
- 외래 키 제약 조건 미지원
- Spring Data JPA 대신 Spring Data MongoDB 필요
- 조인 쿼리 성능 이슈

**평가**:
- 시간 슬롯 도메인은 명확한 관계형 구조를 가짐
- 스키마 유연성이 장점보다는 데이터 무결성 리스크로 작용
- NoSQL의 이점을 활용할 수 있는 유스케이스 없음

---

## Decision Outcome

**선택: MySQL 8.0 / MariaDB 10.11 LTS**

### 선택 이유

1. **관계형 모델 적합성**
   - Policy와 Slot 간의 명확한 1:N 관계
   - 외래 키 제약으로 데이터 무결성 보장
   - 복잡한 조인 쿼리 최적화 가능

2. **트랜잭션 및 동시성**
   - InnoDB 엔진의 ACID 보장
   - Optimistic Locking (@Version) 완벽 지원
   - 슬롯 상태 변경 시 일관성 보장

3. **시간 데이터 처리**
   - DATETIME, TIMESTAMP 완벽 지원
   - 타임존 설정 (`SET time_zone = '+09:00'`)
   - 날짜 함수 풍부 (NOW(), DATE_ADD(), WEEKOFYEAR() 등)

4. **성능**
   - 복합 인덱스를 통한 빠른 범위 조회
   - 대량 INSERT 최적화 (Rolling Window 일일 생성)
   - InnoDB Buffer Pool 캐싱

5. **운영 및 생태계**
   - 팀 내 MySQL/MariaDB 운영 경험 보유
   - Spring Boot Starter JPA 기본 지원
   - 풍부한 모니터링 도구 (Percona Monitoring, Prometheus Exporter)

6. **비용 및 라이선스**
   - 오픈소스 무료
   - 클라우드 RDS 지원 (AWS RDS MySQL/MariaDB)

### MariaDB 10.11 LTS vs MySQL 8.0

최종적으로 **MariaDB 10.11 LTS**를 선택:
- MySQL과 호환되는 기능
- 완전 오픈소스 (GPL v2)
- Oracle 종속성 없음
- 2028년까지 LTS 지원

---

## Consequences

### Positive

- Spring Data JPA와의 완벽한 통합
- 팀의 기존 운영 노하우 활용
- 안정적인 트랜잭션 및 데이터 무결성
- 효율적인 인덱스 기반 조회 성능
- 타임존 설정을 통한 일관된 시간 데이터 관리

### Negative

- NoSQL 대비 수평 확장 제약
  - **완화 방안**: Read Replica를 통한 읽기 부하 분산
  - 현재 트래픽 예상치로는 단일 인스턴스로 충분

- JSON 필드 처리 제한
  - **완화 방안**: 필요시 ElementCollection으로 Value Object 매핑
  - 현재 요구사항에서는 JSON 활용 불필요

### Neutral

- 데이터베이스 벤더 종속성
  - JPA를 사용하므로 필요 시 다른 RDBMS로 마이그레이션 가능
  - 벤더 특화 기능(타임존 설정 등)은 추상화 계층에서 처리

---

## Validation

### 성능 검증

```sql
-- 1. 복합 인덱스 성능 테스트
EXPLAIN SELECT * FROM room_time_slots
WHERE room_id = 101
  AND slot_date = '2025-01-15'
  AND status = 'AVAILABLE';
-- key: idx_room_date_time, type: ref, rows: ~50

-- 2. 대량 INSERT 성능 (2개월치 슬롯 생성)
INSERT INTO room_time_slots (...) VALUES (...); -- 7,200 rows
-- Query OK, 7200 rows affected (0.85 sec)

-- 3. 날짜 범위 조회 (7일치)
SELECT * FROM room_time_slots
WHERE room_id = 101
  AND slot_date BETWEEN '2025-01-15' AND '2025-01-21'
  AND status = 'AVAILABLE';
-- 700 rows in set (0.02 sec)
```

### 동시성 테스트

```java
// Optimistic Locking 테스트
@Version
private Long version;

// 동시에 2개의 트랜잭션이 같은 슬롯을 예약 시도
// → 하나는 성공, 하나는 OptimisticLockingFailureException
```

---

## References

- [MySQL 8.0 Documentation](https://dev.mysql.com/doc/refman/8.0/en/)
- [MariaDB 10.11 Release Notes](https://mariadb.com/kb/en/mariadb-10-11-release-notes/)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [InnoDB Storage Engine](https://dev.mysql.com/doc/refman/8.0/en/innodb-storage-engine.html)

---

## Notes

- 향후 읽기 부하가 증가하면 Read Replica 도입 검토
- 슬롯 통계/분석 요구사항 발생 시 별도 분석 DB (ClickHouse, BigQuery) 고려
- 현재 아키텍처는 연간 1,000만 건 이하의 슬롯 데이터 처리에 적합

---

**Maintained by**: Teambind_dev_backend Team
**Lead Developer**: DDINGJOO
