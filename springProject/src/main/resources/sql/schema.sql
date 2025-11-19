-- Room Service Schema
-- MySQL/MariaDB
-- Character Set: utf8mb4
-- Engine: InnoDB

-- Drop existing tables (in reverse dependency order)
DROP TABLE IF EXISTS policy_closed_dates;
DROP TABLE IF EXISTS weekly_slot_times;
DROP TABLE IF EXISTS closed_date_update_requests;
DROP TABLE IF EXISTS slot_generation_requests;
DROP TABLE IF EXISTS room_time_slots;
DROP TABLE IF EXISTS room_operating_policies;

-- Main Entity: Room Operating Policy
CREATE TABLE room_operating_policies
(
    policy_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id    BIGINT      NOT NULL,
    recurrence VARCHAR(20) NOT NULL,
    slot_unit VARCHAR(20) NOT NULL,
    created_at DATETIME    NOT NULL,
    updated_at DATETIME    NOT NULL,
    UNIQUE KEY uk_room_id (room_id),
    INDEX idx_room_id (room_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ElementCollection: Weekly Slot Times
CREATE TABLE weekly_slot_times
(
    policy_id   BIGINT      NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time  TIME        NOT NULL,
    INDEX idx_policy_id (policy_id),
    FOREIGN KEY (policy_id) REFERENCES room_operating_policies (policy_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ElementCollection: Policy Closed Dates
CREATE TABLE policy_closed_dates
(
    policy_id          BIGINT NOT NULL,
    start_date         DATE,
    end_date           DATE,
    day_of_week        VARCHAR(20),
    recurrence_pattern VARCHAR(20),
    start_time         TIME,
    end_time           TIME,
    INDEX idx_policy_id (policy_id),
    FOREIGN KEY (policy_id) REFERENCES room_operating_policies (policy_id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Main Entity: Room Time Slot
CREATE TABLE room_time_slots
(
    slot_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id        BIGINT      NOT NULL,
    slot_date      DATE        NOT NULL,
    slot_time      TIME        NOT NULL,
    status         VARCHAR(20) NOT NULL,
    reservation_id BIGINT,
    last_updated   DATETIME    NOT NULL,
    INDEX idx_room_date_time (room_id, slot_date, slot_time),
    INDEX idx_date_status (slot_date, status),
    INDEX idx_cleanup (slot_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Main Entity: Slot Generation Request
CREATE TABLE slot_generation_requests
(
    request_id    VARCHAR(36) PRIMARY KEY,
    room_id       BIGINT      NOT NULL,
    start_date    DATE        NOT NULL,
    end_date      DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL,
    total_slots   INT,
    requested_at  DATETIME    NOT NULL,
    started_at    DATETIME,
    completed_at  DATETIME,
    error_message VARCHAR(1000),
    INDEX idx_room_id (room_id),
    INDEX idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Main Entity: Closed Date Update Request
CREATE TABLE closed_date_update_requests
(
    request_id        VARCHAR(36) PRIMARY KEY,
    room_id           BIGINT      NOT NULL,
    closed_date_count INT         NOT NULL,
    status            VARCHAR(20) NOT NULL,
    affected_slots    INT,
    requested_at      DATETIME    NOT NULL,
    started_at        DATETIME,
    completed_at      DATETIME,
    error_message     VARCHAR(1000),
    INDEX idx_room_id (room_id),
    INDEX idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
