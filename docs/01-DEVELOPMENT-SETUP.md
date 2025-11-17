# 개발 환경 설정

**Version**: 1.1.0
**Last Updated**: 2025-01-17

## 목차

- [시스템 요구사항](#시스템-요구사항)
- [로컬 개발 환경 구성](#로컬-개발-환경-구성)
- [데이터베이스 설정](#데이터베이스-설정)
- [애플리케이션 실행](#애플리케이션-실행)
- [테스트 실행](#테스트-실행)
- [문제 해결](#문제-해결)

---

## 시스템 요구사항

### 필수 요구사항

| 항목 | 버전 | 확인 명령어 |
|------|------|------------|
| Java | 17 이상 | `java -version` |
| Gradle | 8.x 이상 | `./gradlew --version` |
| MySQL/MariaDB | 8.0 / 10.11 이상 | `mysql --version` |
| Redis | 7.x 이상 (선택) | `redis-cli --version` |
| Git | 최신 버전 | `git --version` |

### 권장 개발 도구

- **IDE**: IntelliJ IDEA 2023.x 이상
- **API 테스트**: Postman 또는 IntelliJ HTTP Client
- **DB 클라이언트**: DBeaver, MySQL Workbench
- **Redis 클라이언트**: RedisInsight

---

## 로컬 개발 환경 구성

### 1. 프로젝트 클론

```bash
git clone https://github.com/DDINGJOO/LEE_YONG_GWAN_LEE_SERVICE_SERVER.git
cd LEE_YONG_GWAN_LEE_SERVICE_SERVER
```

### 2. 프로젝트 구조 확인

```
LEE_YONG_GWAN_LEE_SERVICE_SERVER/
├── docs/                   # 문서
├── springProject/          # 메인 프로젝트
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/      # 소스 코드
│   │   │   └── resources/ # 설정 파일
│   │   └── test/          # 테스트 코드
│   └── build.gradle       # 빌드 설정
└── README.md
```

### 3. Gradle 빌드

```bash
cd springProject
./gradlew clean build
```

**빌드 성공 확인**:
```
BUILD SUCCESSFUL in 30s
```

---

## 데이터베이스 설정

### 1. MySQL/MariaDB 설치 (로컬)

#### macOS (Homebrew)
```bash
brew install mysql
brew services start mysql
```

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
```

#### Windows
[MySQL 공식 다운로드](https://dev.mysql.com/downloads/installer/)에서 설치

### 2. 데이터베이스 생성

```bash
mysql -u root -p
```

```sql
-- 데이터베이스 생성
CREATE DATABASE room_service CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 생성 (선택)
CREATE USER 'room_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON room_service.* TO 'room_user'@'localhost';
FLUSH PRIVILEGES;

-- 연결 확인
USE room_service;
```

### 3. 스키마 생성

프로젝트 루트의 `schema.sql`을 실행합니다.

```bash
mysql -u root -p room_service < schema.sql
```

**생성된 테이블 확인**:
```sql
USE room_service;
SHOW TABLES;

-- 예상 결과:
-- +--------------------------------+
-- | Tables_in_room_service         |
-- +--------------------------------+
-- | closed_date_update_requests    |
-- | policy_closed_dates            |
-- | room_operating_policies        |
-- | room_time_slots                |
-- | slot_generation_requests       |
-- | weekly_slot_times              |
-- +--------------------------------+
```

### 4. 타임존 설정 확인

```sql
-- 세션 타임존 확인
SELECT @@session.time_zone, @@global.time_zone;

-- KST로 설정
SET time_zone = '+09:00';

-- 현재 시각 확인 (KST 기준)
SELECT NOW();
```

---

## 애플리케이션 실행

### 1. 설정 파일 작성

`springProject/src/main/resources/application-dev.yml` 파일을 생성합니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/room_service?serverTimezone=Asia/Seoul
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # 스키마 검증만 수행
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        jdbc:
          time_zone: Asia/Seoul
        dialect: org.hibernate.dialect.MySQLDialect

  data:
    redis:
      host: localhost
      port: 6379
      password: # Redis 비밀번호 (없으면 생략)

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: room-service-group

room:
  timeSlot:
    pending:
      expiration:
        minutes: 40
    rollingWindow:
      days: 30

logging:
  level:
    com.teambind.springproject: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
```

### 2. 애플리케이션 실행

#### Gradle로 실행
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

#### IDE에서 실행
- IntelliJ IDEA에서 `SpringProjectApplication` 클래스 실행
- VM options에 `-Dspring.profiles.active=dev` 추가

#### JAR로 실행
```bash
./gradlew bootJar
java -jar -Dspring.profiles.active=dev build/libs/springProject-0.0.1-SNAPSHOT.jar
```

### 3. 실행 확인

애플리케이션이 정상 실행되면 다음과 같은 로그가 출력됩니다:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.5.7)

[INFO ] Starting SpringProjectApplication
[INFO ] No active profile set, falling back to default profiles: default
[INFO ] Started SpringProjectApplication in 3.456 seconds
```

### 4. API 동작 확인

```bash
# Health Check (Spring Actuator가 있는 경우)
curl http://localhost:8080/actuator/health

# 타임존 확인 API (구현된 경우)
curl http://localhost:8080/time/check
```

---

## 테스트 실행

### 1. 전체 테스트 실행

```bash
./gradlew test
```

**실행 결과 예시**:
```
테스트 결과: SUCCESS
총 테스트: 24개
성공: 24개
실패: 0개
스킵: 0개
소요 시간: 12345ms
```

### 2. 특정 테스트 클래스 실행

```bash
./gradlew test --tests RoomOperatingPolicyTest
```

### 3. 특정 테스트 메서드 실행

```bash
./gradlew test --tests RoomTimeSlotTest.markAsPending_Success
```

### 4. 테스트 커버리지 확인

```bash
./gradlew test jacocoTestReport
```

**리포트 확인**:
```
build/reports/jacoco/test/html/index.html
```

### 5. 테스트 환경

테스트는 **H2 인메모리 데이터베이스**를 사용하므로 MySQL 없이도 실행 가능합니다.

`src/test/resources/application-test.yml`:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  h2:
    console:
      enabled: true
```

---

## 문제 해결

### 문제 1: 데이터베이스 연결 실패

**증상**:
```
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago.
```

**해결책**:
1. MySQL/MariaDB 서비스 실행 확인
   ```bash
   # macOS
   brew services list

   # Linux
   sudo systemctl status mysql
   ```

2. 데이터베이스 URL 및 포트 확인
   ```bash
   mysql -u root -p -h localhost -P 3306
   ```

3. 방화벽 설정 확인

### 문제 2: 타임존 관련 오류

**증상**:
```
The server time zone value 'KST' is unrecognized
```

**해결책**:
1. JDBC URL에 타임존 명시
   ```yaml
   url: jdbc:mysql://localhost:3306/room_service?serverTimezone=Asia/Seoul
   ```

2. MySQL 타임존 테이블 로드 (필요시)
   ```bash
   mysql_tzinfo_to_sql /usr/share/zoneinfo | mysql -u root -p mysql
   ```

자세한 내용은 [타임존 설정 가이드](guides/timezone-configuration.md)를 참고하세요.

### 문제 3: Port 8080이 이미 사용 중

**증상**:
```
Web server failed to start. Port 8080 was already in use.
```

**해결책**:
1. 사용 중인 프로세스 종료
   ```bash
   # macOS/Linux
   lsof -i :8080
   kill -9 <PID>

   # Windows
   netstat -ano | findstr :8080
   taskkill /PID <PID> /F
   ```

2. 포트 변경
   ```yaml
   # application-dev.yml
   server:
     port: 8081
   ```

### 문제 4: Gradle 빌드 실패

**증상**:
```
Could not resolve all dependencies
```

**해결책**:
1. Gradle 캐시 정리
   ```bash
   ./gradlew clean --refresh-dependencies
   ```

2. Gradle Wrapper 재다운로드
   ```bash
   ./gradlew wrapper --gradle-version=8.5
   ```

3. 프록시 설정 (회사 네트워크인 경우)
   ```bash
   # gradle.properties
   systemProp.http.proxyHost=proxy.company.com
   systemProp.http.proxyPort=8080
   systemProp.https.proxyHost=proxy.company.com
   systemProp.https.proxyPort=8080
   ```

### 문제 5: H2 테스트 실패

**증상**:
```
Table "ROOM_OPERATING_POLICIES" not found
```

**해결책**:
1. JPA DDL 설정 확인
   ```yaml
   # application-test.yml
   spring:
     jpa:
       hibernate:
         ddl-auto: create-drop  # 테스트 시 자동 생성
   ```

2. `@Sql` 어노테이션으로 스키마 로드
   ```java
   @Sql("/schema.sql")
   @SpringBootTest
   class MyTest {
       // ...
   }
   ```

---

## Docker를 사용한 로컬 환경 (선택)

### 1. Docker Compose 파일 작성

`docker-compose.yml`:
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: room_service
      TZ: Asia/Seoul
    ports:
      - "3306:3306"
    volumes:
      - ./schema.sql:/docker-entrypoint-initdb.d/schema.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

### 2. Docker 컨테이너 실행

```bash
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 종료
docker-compose down
```

---

## 다음 단계

개발 환경 설정이 완료되었습니다. 다음 문서를 참고하세요:

- [아키텍처 개요](00-ARCHITECTURE-OVERVIEW.md)
- [ShedLock 설정 가이드](guides/shedlock-setup.md)
- [타임존 설정 가이드](guides/timezone-configuration.md)
- [예외 처리 가이드](guides/exception-handling.md)

---

**문의**: Teambind_dev_backend Team
