-- ============================================================
-- Sample Seed Data for Development/Testing
-- ============================================================
-- Description: 개발 및 테스트용 샘플 데이터
-- Note: 프로덕션 환경에서는 실행하지 않음
-- Timezone: Asia/Seoul (KST, UTC+9)
-- ============================================================

-- 타임존 설정: 한국 표준시
SET time_zone = '+09:00';

-- ============================================================
-- 1. Sample Room Operating Policy
-- ============================================================
-- 룸 ID 101: 매주 월-금 09:00, 13:00 운영
INSERT INTO room_operating_policies (room_id, recurrence, created_at, updated_at)
VALUES (101, 'EVERY_WEEK', NOW(), NOW());

SET @policy_id_101 = LAST_INSERT_ID();

-- 월요일 슬롯
INSERT INTO weekly_slot_times (policy_id, day_of_week, start_time)
VALUES (@policy_id_101, 'MONDAY', '09:00:00'),
       (@policy_id_101, 'MONDAY', '13:00:00');

-- 화요일 슬롯
INSERT INTO weekly_slot_times (policy_id, day_of_week, start_time)
VALUES (@policy_id_101, 'TUESDAY', '09:00:00'),
       (@policy_id_101, 'TUESDAY', '13:00:00');

-- 수요일 슬롯
INSERT INTO weekly_slot_times (policy_id, day_of_week, start_time)
VALUES (@policy_id_101, 'WEDNESDAY', '09:00:00'),
       (@policy_id_101, 'WEDNESDAY', '13:00:00');

-- 목요일 슬롯
INSERT INTO weekly_slot_times (policy_id, day_of_week, start_time)
VALUES (@policy_id_101, 'THURSDAY', '09:00:00'),
       (@policy_id_101, 'THURSDAY', '13:00:00');

-- 금요일 슬롯
INSERT INTO weekly_slot_times (policy_id, day_of_week, start_time)
VALUES (@policy_id_101, 'FRIDAY', '09:00:00'),
       (@policy_id_101, 'FRIDAY', '13:00:00');

-- 휴무일 설정: 2025년 1월 1일 (신정)
INSERT INTO policy_closed_dates (policy_id, start_date, end_date, start_time, end_time)
VALUES (@policy_id_101, '2025-01-01', NULL, NULL, NULL);

-- ============================================================
-- 2. Sample Room Operating Policy (홀수주)
-- ============================================================
-- 룸 ID 102: 홀수주 월-금 10:00, 14:00 운영
INSERT INTO room_operating_policies (room_id, recurrence, created_at, updated_at)
VALUES (102, 'ODD_WEEK', NOW(), NOW());

SET @policy_id_102 = LAST_INSERT_ID();

-- 월-금 슬롯
INSERT INTO weekly_slot_times (policy_id, day_of_week, start_time)
VALUES (@policy_id_102, 'MONDAY', '10:00:00'),
       (@policy_id_102, 'MONDAY', '14:00:00'),
       (@policy_id_102, 'TUESDAY', '10:00:00'),
       (@policy_id_102, 'TUESDAY', '14:00:00'),
       (@policy_id_102, 'WEDNESDAY', '10:00:00'),
       (@policy_id_102, 'WEDNESDAY', '14:00:00'),
       (@policy_id_102, 'THURSDAY', '10:00:00'),
       (@policy_id_102, 'THURSDAY', '14:00:00'),
       (@policy_id_102, 'FRIDAY', '10:00:00'),
       (@policy_id_102, 'FRIDAY', '14:00:00');

-- 휴무일 설정: 2025년 1월 27일 ~ 29일 (구정 연휴)
INSERT INTO policy_closed_dates (policy_id, start_date, end_date, start_time, end_time)
VALUES (@policy_id_102, '2025-01-27', '2025-01-29', NULL, NULL);

-- ============================================================
-- 3. Sample Time Slots (오늘과 내일)
-- ============================================================
-- 룸 101의 오늘 슬롯 (예약 가능)
INSERT INTO room_time_slots (room_id, slot_date, slot_time, status, reservation_id, last_updated)
VALUES (101, CURDATE(), '09:00:00', 'AVAILABLE', NULL, NOW()),
       (101, CURDATE(), '13:00:00', 'AVAILABLE', NULL, NOW());

-- 룸 101의 내일 슬롯 (일부 예약됨)
INSERT INTO room_time_slots (room_id, slot_date, slot_time, status, reservation_id, last_updated)
VALUES (101, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00', 'RESERVED', 1001, NOW()),
       (101, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '13:00:00', 'AVAILABLE', NULL, NOW());

-- 룸 102의 오늘 슬롯 (PENDING 상태)
INSERT INTO room_time_slots (room_id, slot_date, slot_time, status, reservation_id, last_updated)
VALUES (102, CURDATE(), '10:00:00', 'PENDING', 1002, NOW()),
       (102, CURDATE(), '14:00:00', 'AVAILABLE', NULL, NOW());

-- ============================================================
-- 4. Verification Queries
-- ============================================================
-- 데이터 확인용 쿼리 (주석 해제하여 실행)

-- 정책 확인
-- SELECT * FROM room_operating_policies;

-- 주간 슬롯 확인
-- SELECT p.room_id, w.day_of_week, w.start_time
-- FROM room_operating_policies p
-- JOIN weekly_slot_times w ON p.policy_id = w.policy_id
-- ORDER BY p.room_id, w.day_of_week, w.start_time;

-- 휴무일 확인
-- SELECT p.room_id, c.start_date, c.end_date
-- FROM room_operating_policies p
-- JOIN policy_closed_dates c ON p.policy_id = c.policy_id
-- ORDER BY p.room_id, c.start_date;

-- 슬롯 상태 확인
-- SELECT room_id, slot_date, slot_time, status, reservation_id
-- FROM room_time_slots
-- ORDER BY room_id, slot_date, slot_time;

-- 예약 가능한 슬롯 개수 확인
-- SELECT room_id, COUNT(*) as available_count
-- FROM room_time_slots
-- WHERE status = 'AVAILABLE'
-- GROUP BY room_id;
