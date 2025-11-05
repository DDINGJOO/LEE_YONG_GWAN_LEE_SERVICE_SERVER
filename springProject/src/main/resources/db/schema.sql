-- ============================================================
-- Room Operating Policy & Time Slot Management Schema
-- ============================================================
-- Description: 룸 운영 정책 및 시간 슬롯 관리 테이블
-- Author: Time Management Service
-- Version: 1.0
-- Timezone: Asia/Seoul (KST, UTC+9)
-- ============================================================

-- 타임존 설정: 한국 표준시
SET time_zone = '+09:00';

-- ============================================================
-- 1. Room Operating Policy (운영 정책)
-- ============================================================
CREATE TABLE IF NOT EXISTS room_operating_policies
(
    policy_id  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '정책 ID',
    room_id    BIGINT      NOT NULL UNIQUE COMMENT '룸 ID (Place Info 서비스 참조)',
    recurrence VARCHAR(20) NOT NULL COMMENT '반복 패턴 (EVERY_WEEK, ODD_WEEK, EVEN_WEEK)',
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',

    INDEX idx_room_id (room_id),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '룸 운영 시간 정책';

-- ============================================================
-- 2. Weekly Slot Times (주간 슬롯 시간)
-- ============================================================
CREATE TABLE IF NOT EXISTS weekly_slot_times
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    policy_id   BIGINT      NOT NULL COMMENT '정책 ID (FK)',
    day_of_week VARCHAR(10) NOT NULL COMMENT '요일 (MONDAY, TUESDAY, ...)',
    start_time  TIME        NOT NULL COMMENT '슬롯 시작 시각',

    CONSTRAINT fk_weekly_slot_policy
        FOREIGN KEY (policy_id) REFERENCES room_operating_policies (policy_id)
            ON DELETE CASCADE,

    INDEX idx_policy_day (policy_id, day_of_week)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '요일별 운영 시간 슬롯';

-- ============================================================
-- 3. Policy Closed Dates (휴무일)
-- ============================================================
CREATE TABLE IF NOT EXISTS policy_closed_dates
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    policy_id  BIGINT NOT NULL COMMENT '정책 ID (FK)',
    start_date DATE   NOT NULL COMMENT '휴무 시작 날짜',
    end_date   DATE COMMENT '휴무 종료 날짜 (NULL이면 단일 날짜)',
    start_time TIME COMMENT '휴무 시작 시각 (NULL이면 하루 종일)',
    end_time   TIME COMMENT '휴무 종료 시각 (NULL이면 하루 종일)',

    CONSTRAINT fk_closed_date_policy
        FOREIGN KEY (policy_id) REFERENCES room_operating_policies (policy_id)
            ON DELETE CASCADE,

    INDEX idx_policy_date (policy_id, start_date, end_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '정책별 휴무일 범위';

-- ============================================================
-- 4. Room Time Slots (생성된 시간 슬롯)
-- ============================================================
CREATE TABLE IF NOT EXISTS room_time_slots
(
    slot_id        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '슬롯 ID',
    room_id        BIGINT      NOT NULL COMMENT '룸 ID',
    slot_date      DATE        NOT NULL COMMENT '슬롯 날짜',
    slot_time      TIME        NOT NULL COMMENT '슬롯 시각',
    status         VARCHAR(20) NOT NULL COMMENT '슬롯 상태 (AVAILABLE, PENDING, RESERVED, CANCELLED, CLOSED)',
    reservation_id BIGINT COMMENT '예약 ID (예약된 경우)',
    last_updated   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '최종 수정 시각',

    -- 성능 최적화 인덱스
    INDEX idx_room_date_time (room_id, slot_date, slot_time),
    INDEX idx_date_status (slot_date, status),
    INDEX idx_cleanup (slot_date),
    INDEX idx_reservation (reservation_id),

    -- 중복 방지 (동일 룸, 날짜, 시간에 하나의 슬롯만 존재)
    UNIQUE KEY uk_room_date_time (room_id, slot_date, slot_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '룸별 시간 슬롯 (Rolling Window: 2달치)';

-- ============================================================
-- 5. Partition 설정 (선택사항 - 대용량 데이터 대비)
-- ============================================================
-- 슬롯 데이터가 많아질 경우 월별 파티셔닝 고려
-- ALTER TABLE room_time_slots
-- PARTITION BY RANGE (TO_DAYS(slot_date)) (
--     PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
--     PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
--     ...
-- );

-- ============================================================
-- 6. 인덱스 설명
-- ============================================================
-- room_operating_policies:
--   - idx_room_id: Room ID로 정책 조회 (정책 등록/조회 시)
--   - idx_created_at: 정책 생성 시각 기준 조회

-- weekly_slot_times:
--   - idx_policy_day: 특정 정책의 특정 요일 슬롯 조회

-- policy_closed_dates:
--   - idx_policy_date: 특정 정책의 날짜 범위 휴무일 조회

-- room_time_slots:
--   - idx_room_date_time: 특정 룸의 특정 날짜/시간 슬롯 조회 (가장 빈번한 쿼리)
--   - idx_date_status: 날짜와 상태 기반 조회 (예: 특정 날짜의 AVAILABLE 슬롯)
--   - idx_cleanup: Rolling Window 배치 작업용 (어제 슬롯 삭제)
--   - idx_reservation: 예약 ID로 슬롯 조회 (예약 취소 시)
--   - uk_room_date_time: 중복 슬롯 방지 (데이터 무결성)

-- ============================================================
-- 7. 예상 데이터 볼륨
-- ============================================================
-- 가정:
--   - Room 수: 1,000개
--   - 슬롯 단위: 30분 (하루 48개 슬롯)
--   - Rolling Window: 60일
--
-- 계산:
--   - 1,000개 Room × 48개 슬롯/일 × 60일 = 2,880,000개 슬롯
--   - 예상 테이블 크기: 약 300MB (인덱스 포함)
--
-- 성능:
--   - idx_room_date_time로 단일 슬롯 조회: < 1ms
--   - 특정 날짜 범위 조회: < 10ms
--   - Rolling Window 배치 (삭제 + 생성): < 5초
