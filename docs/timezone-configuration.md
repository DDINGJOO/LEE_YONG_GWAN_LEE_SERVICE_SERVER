# 타임존 설정 가이드

## 개요

Time Management Service는 모든 날짜/시간 데이터를 **한국 표준시(KST, Asia/Seoul, UTC+9)**로 통일합니다.

---

## 설정 레이어

### 1. JVM 레벨

**파일**: `JpaConfig.java`

```java
@PostConstruct
public void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
}
```

**영향 범위**:

- `LocalDateTime.now()` → 한국 시간 반환
- `Instant.now()` → UTC 기준이지만 변환 시 KST 사용
- `SimpleDateFormat` → 기본 타임존 KST

---

### 2. JDBC 연결 레벨

**파일**: `application-dev.yaml`

```yaml
spring:
  datasource:
    url: jdbc:mariadb://host:port/db?serverTimezone=Asia/Seoul
```

**영향 범위**:

- JDBC 드라이버가 DB와 통신 시 KST로 변환
- `java.sql.Timestamp` ↔ `TIMESTAMP` 컬럼 변환

---

### 3. Hibernate 레벨

**파일**: `application-dev.yaml`

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: Asia/Seoul
```

**영향 범위**:

- Hibernate가 생성하는 SQL의 TIMESTAMP 값
- Entity의 `LocalDateTime` ↔ DB `TIMESTAMP` 변환

---

### 4. MySQL/MariaDB 세션 레벨

**파일**: `schema.sql`, `seed-data.sql`

```sql
SET time_zone = '+09:00';
```

**영향 범위**:

- SQL 실행 세션의 타임존
- `NOW()`, `CURRENT_TIMESTAMP` 함수 결과
- `TIMESTAMP` 컬럼의 저장/조회 시각

---

## 타임존 통일 검증

### 1. 애플리케이션 시간 확인

```java
@RestController
public class TimeCheckController {
    @GetMapping("/time/check")
    public Map<String, String> checkTime() {
        return Map.of(
            "jvmTimezone", TimeZone.getDefault().getID(),
            "localDateTime", LocalDateTime.now().toString(),
            "instant", Instant.now().toString(),
            "zonedDateTime", ZonedDateTime.now().toString()
        );
    }
}
```

**예상 결과**:

```json
{
  "jvmTimezone": "Asia/Seoul",
  "localDateTime": "2025-01-15T14:30:00",
  "instant": "2025-01-15T05:30:00Z",
  "zonedDateTime": "2025-01-15T14:30:00+09:00[Asia/Seoul]"
}
```

---

### 2. 데이터베이스 시간 확인

```sql
-- 현재 세션의 타임존
SELECT @@session.time_zone, @@global.time_zone;

-- 현재 시각 (KST 기준)
SELECT NOW(), CURRENT_TIMESTAMP;

-- 예상 결과: 2025-01-15 14:30:00 (KST)
```

---

### 3. 통합 테스트

```java
@Test
void testTimezonConsistency() {
    // Given
    LocalDateTime now = LocalDateTime.now();
    RoomTimeSlot slot = RoomTimeSlot.available(1L, now.toLocalDate(), now.toLocalTime());

    // When
    repository.save(slot);
    repository.flush();

    RoomTimeSlot saved = repository.findById(slot.getSlotId()).get();

    // Then
    assertThat(saved.getLastUpdated()).isEqualToIgnoringSeconds(now);

    // DB에서 직접 조회하여 검증
    String sql = "SELECT last_updated FROM room_time_slots WHERE slot_id = ?";
    Timestamp dbTime = jdbcTemplate.queryForObject(sql, Timestamp.class, slot.getSlotId());
    assertThat(dbTime.toLocalDateTime()).isEqualToIgnoringSeconds(now);
}
```

---

## 타임존 이슈 트러블슈팅

### 문제 1: Entity 저장 시 시간이 9시간 차이남

**증상**:

```
애플리케이션: 2025-01-15 14:30:00
DB 저장값:    2025-01-15 05:30:00 (9시간 차이)
```

**원인**: JDBC 타임존 설정 누락

**해결**:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://...?serverTimezone=Asia/Seoul
```

---

### 문제 2: NOW() 함수 결과가 UTC

**증상**:

```sql
SELECT NOW(); -- 2025-01-15 05:30:00 (UTC)
```

**원인**: MySQL 세션 타임존이 UTC

**해결**:

```sql
SET time_zone = '+09:00';
SELECT NOW(); -- 2025-01-15 14:30:00 (KST)
```

---

### 문제 3: 스케줄러가 UTC 기준으로 실행됨

**증상**:

```
@Scheduled(cron = "0 0 2 * * *") // 새벽 2시 실행 예상
실제 실행: 오전 11시 (UTC 02:00 = KST 11:00)
```

**원인**: JVM 타임존 미설정

**해결**:

```java
@Configuration
public class JpaConfig {
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }
}
```

---

### 문제 4: LocalDateTime vs ZonedDateTime

**LocalDateTime** (권장):

```java
LocalDateTime now = LocalDateTime.now(); // 2025-01-15T14:30:00 (타임존 정보 없음)
// JVM 타임존 설정에 의존
```

**ZonedDateTime** (명시적):

```java
ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
// 2025-01-15T14:30:00+09:00[Asia/Seoul] (타임존 정보 포함)
```

**권장 사항**:

- JVM 타임존을 KST로 고정했으므로 `LocalDateTime` 사용
- 외부 API 연동 시에만 `ZonedDateTime` 고려

---

## 프로덕션 배포 시 체크리스트

### 1. 환경 변수 확인

```bash
# 애플리케이션 시작 옵션
-Duser.timezone=Asia/Seoul
```

### 2. Docker 컨테이너 타임존

```dockerfile
# Dockerfile
FROM openjdk:17-jdk-slim
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
```

### 3. Kubernetes 설정

```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: time-service
    env:
    - name: TZ
      value: "Asia/Seoul"
```

### 4. AWS RDS 설정

```sql
-- Parameter Group에서 time_zone 설정
-- time_zone = Asia/Seoul
```

---

## 모니터링

### 1. 타임존 설정 확인 API

```bash
curl http://localhost:8080/actuator/env | jq '.propertySources[] | select(.name | contains("systemProperties")) | .properties."user.timezone"'
```

### 2. 정기 검증 스크립트

```bash
#!/bin/bash
# verify-timezone.sh

APP_TIME=$(curl -s http://localhost:8080/time/check | jq -r '.localDateTime')
DB_TIME=$(mysql -h localhost -u user -p -e "SELECT NOW()" | tail -1)

echo "Application Time: $APP_TIME"
echo "Database Time:    $DB_TIME"

if [[ "$APP_TIME" == "$DB_TIME"* ]]; then
  echo "✅ Timezone synchronized"
else
  echo "❌ Timezone mismatch detected"
  exit 1
fi
```

---

## 참고 자료

- [MySQL Timezone Documentation](https://dev.mysql.com/doc/refman/8.0/en/time-zone-support.html)
- [Spring Boot Timezone Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.data.spring.jpa.properties)
- [Java Time API Best Practices](https://www.oracle.com/technical-resources/articles/java/jf14-date-time.html)

---

## 요약

| 레이어       | 설정 위치            | 값                         |
|-----------|------------------|---------------------------|
| JVM       | JpaConfig.java   | Asia/Seoul                |
| JDBC      | application.yaml | serverTimezone=Asia/Seoul |
| Hibernate | application.yaml | time_zone: Asia/Seoul     |
| MySQL     | schema.sql       | SET time_zone = '+09:00'  |

모든 레이어가 **Asia/Seoul (KST, UTC+9)**로 통일되어 있습니다.
