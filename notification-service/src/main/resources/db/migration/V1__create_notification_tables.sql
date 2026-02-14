-- V1__create_notification_tables.sql
-- Notification Service schema

CREATE TABLE IF NOT EXISTS notification_logs (
    id              CHAR(36) PRIMARY KEY,
    user_id         CHAR(36),
    event_type      VARCHAR(50) NOT NULL,
    channel         VARCHAR(20) DEFAULT 'EMAIL',
    recipient       VARCHAR(255) NOT NULL,
    subject         VARCHAR(500),
    body            TEXT,
    status          ENUM('PENDING','SENT','FAILED','DLQ') DEFAULT 'PENDING',
    failure_reason  VARCHAR(500),
    retry_count     INT DEFAULT 0,
    idempotency_key CHAR(36) UNIQUE,
    event_payload   JSON,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    sent_at         DATETIME,
    INDEX idx_nl_user (user_id),
    INDEX idx_nl_type (event_type),
    INDEX idx_nl_status (status),
    INDEX idx_nl_idempotency (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
