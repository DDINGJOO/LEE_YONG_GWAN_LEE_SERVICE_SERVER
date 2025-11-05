-- ============================================================
-- Useful Queries for Time Slot Management
-- ============================================================
-- Description: 운영 및 디버깅에 유용한 쿼리 모음
-- ============================================================

-- ============================================================
-- 1. 정책 조회
-- ============================================================

-- 모든 정책 조회
SELECT p.policy_id,
       p.room_id,
       p.recurrence,
       COUNT(DISTINCT w.day_of_week) AS operating_days,
       COUNT(c.id)                   AS closed_date_count
FROM room_operating_policies p
         LEFT JOIN weekly_slot_times w ON p.policy_id = w.policy_id
         LEFT JOIN policy_closed_dates c ON p.policy_id = c.policy_id
GROUP BY p.policy_id, p.room_id, p.recurrence;

-- 특정 룸의 주간 운영 시간 조회
SELECT w.day_of_week, w.start_time
FROM room_operating_policies p
         JOIN weekly_slot_times w ON p.policy_id = w.policy_id
WHERE p.room_id = 101
ORDER BY FIELD(w.day_of_week, 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'),
         w.start_time;

-- 특정 룸의 휴무일 조회
SELECT c.start_date, c.end_date, c.start_time, c.end_time
FROM room_operating_policies p
         JOIN policy_closed_dates c ON p.policy_id = c.policy_id
WHERE p.room_id = 101
ORDER BY c.start_date;

-- ============================================================
-- 2. 슬롯 조회
-- ============================================================

-- 특정 날짜의 예약 가능한 슬롯 조회
SELECT room_id, slot_date, slot_time, status
FROM room_time_slots
WHERE slot_date = '2025-01-15'
  AND status = 'AVAILABLE'
ORDER BY room_id, slot_time;

-- 특정 룸의 날짜 범위 슬롯 조회
SELECT slot_date, slot_time, status, reservation_id
FROM room_time_slots
WHERE room_id = 101
  AND slot_date BETWEEN '2025-01-15' AND '2025-01-20'
ORDER BY slot_date, slot_time;

-- 상태별 슬롯 개수 통계
SELECT status, COUNT(*) AS count, MIN(slot_date) AS oldest_date, MAX(slot_date) AS newest_date
FROM room_time_slots
GROUP BY status
ORDER BY count DESC;

-- 룸별 예약 가능한 슬롯 개수
SELECT room_id, COUNT(*) AS available_slots
FROM room_time_slots
WHERE status = 'AVAILABLE'
GROUP BY room_id
ORDER BY available_slots DESC;

-- ============================================================
-- 3. 예약 관련 조회
-- ============================================================

-- 특정 예약 ID의 슬롯 조회
SELECT slot_id, room_id, slot_date, slot_time, status
FROM room_time_slots
WHERE reservation_id = 1001;

-- PENDING 상태가 오래된 슬롯 조회 (15분 이상)
SELECT slot_id, room_id, slot_date, slot_time, reservation_id, last_updated
FROM room_time_slots
WHERE status = 'PENDING'
  AND last_updated < DATE_SUB(NOW(), INTERVAL 15 MINUTE)
ORDER BY last_updated;

-- 오늘 예약된 슬롯 조회
SELECT room_id, slot_time, reservation_id
FROM room_time_slots
WHERE slot_date = CURDATE()
  AND status IN ('PENDING', 'RESERVED')
ORDER BY room_id, slot_time;

-- ============================================================
-- 4. 통계 및 모니터링
-- ============================================================

-- 날짜별 슬롯 개수
SELECT slot_date, COUNT(*) AS total_slots, SUM(CASE WHEN status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available_slots
FROM room_time_slots
GROUP BY slot_date
ORDER BY slot_date;

-- 룸별 예약률 (오늘 기준)
SELECT room_id,
       COUNT(*)                                                                          AS total_slots,
       SUM(CASE WHEN status = 'RESERVED' THEN 1 ELSE 0 END)                              AS reserved_slots,
       ROUND(SUM(CASE WHEN status = 'RESERVED' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) AS reservation_rate_percent
FROM room_time_slots
WHERE slot_date = CURDATE()
GROUP BY room_id
ORDER BY reservation_rate_percent DESC;

-- Rolling Window 범위 확인
SELECT MIN(slot_date)                           AS oldest_date,
       MAX(slot_date)                           AS newest_date,
       DATEDIFF(MAX(slot_date), MIN(slot_date)) AS day_range,
       COUNT(*)                                 AS total_slots
FROM room_time_slots;

-- 최근 업데이트된 슬롯 (최근 10분 이내)
SELECT room_id, slot_date, slot_time, status, last_updated
FROM room_time_slots
WHERE last_updated >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)
ORDER BY last_updated DESC
LIMIT 50;

-- ============================================================
-- 5. 데이터 정합성 체크
-- ============================================================

-- 중복 슬롯 체크 (uk_room_date_time 제약 위반 후보)
SELECT room_id, slot_date, slot_time, COUNT(*) AS duplicate_count
FROM room_time_slots
GROUP BY room_id, slot_date, slot_time
HAVING COUNT(*) > 1;

-- 과거 슬롯 체크 (Rolling Window 작동 확인)
SELECT COUNT(*) AS past_slots_count, MIN(slot_date) AS oldest_date
FROM room_time_slots
WHERE slot_date < DATE_SUB(CURDATE(), INTERVAL 1 DAY);

-- 정책 없는 룸의 슬롯 체크 (고아 슬롯)
SELECT DISTINCT rts.room_id
FROM room_time_slots rts
         LEFT JOIN room_operating_policies p ON rts.room_id = p.room_id
WHERE p.policy_id IS NULL;

-- RESERVED 상태인데 reservation_id가 NULL인 슬롯 (데이터 무결성 문제)
SELECT slot_id, room_id, slot_date, slot_time
FROM room_time_slots
WHERE status = 'RESERVED'
  AND reservation_id IS NULL;

-- ============================================================
-- 6. 성능 모니터링
-- ============================================================

-- 인덱스 사용 통계 (MySQL 8.0+)
-- SELECT * FROM sys.schema_index_statistics
-- WHERE table_name = 'room_time_slots';

-- 테이블 크기 확인
SELECT TABLE_NAME,
       ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2) AS size_mb,
       TABLE_ROWS
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('room_operating_policies', 'weekly_slot_times', 'policy_closed_dates', 'room_time_slots');

-- ============================================================
-- 7. 유지보수 쿼리
-- ============================================================

-- 특정 날짜의 슬롯 삭제 (Rolling Window 수동 실행)
-- DELETE FROM room_time_slots WHERE slot_date = '2025-01-01';

-- 만료된 PENDING 슬롯 복구 (15분 이상)
-- UPDATE room_time_slots
-- SET status = 'AVAILABLE', reservation_id = NULL, last_updated = NOW()
-- WHERE status = 'PENDING'
--   AND last_updated < DATE_SUB(NOW(), INTERVAL 15 MINUTE);

-- 특정 룸의 모든 슬롯 삭제
-- DELETE FROM room_time_slots WHERE room_id = 101;

-- 특정 예약의 모든 슬롯 취소
-- UPDATE room_time_slots
-- SET status = 'CANCELLED', last_updated = NOW()
-- WHERE reservation_id = 1001;
