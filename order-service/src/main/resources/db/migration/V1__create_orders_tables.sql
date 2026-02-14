-- V1__create_orders_tables.sql
-- Order Service schema

CREATE TABLE IF NOT EXISTS coupons (
    id              CHAR(36) PRIMARY KEY,
    code            VARCHAR(50) NOT NULL UNIQUE,
    discount_type   ENUM('PERCENTAGE','FIXED') NOT NULL,
    discount_value  DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(12,2) DEFAULT 0,
    max_discount    DECIMAL(12,2),
    usage_limit     INT,
    used_count      INT DEFAULT 0,
    per_user_limit  INT DEFAULT 1,
    valid_from      DATETIME NOT NULL,
    valid_until     DATETIME NOT NULL,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_coupon_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orders (
    id              CHAR(36) NOT NULL,
    user_id         CHAR(36) NOT NULL,
    order_number    VARCHAR(50) NOT NULL UNIQUE,
    status          ENUM('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED','REFUNDED') DEFAULT 'PENDING',
    subtotal        DECIMAL(12,2) NOT NULL,
    tax_amount      DECIMAL(12,2) DEFAULT 0,
    shipping_cost   DECIMAL(12,2) DEFAULT 0,
    discount_amount DECIMAL(12,2) DEFAULT 0,
    total_amount    DECIMAL(12,2) NOT NULL,
    coupon_id       CHAR(36),
    shipping_address_snapshot JSON NOT NULL,
    idempotency_key CHAR(36) UNIQUE,
    notes           TEXT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_status (status),
    INDEX idx_orders_number (order_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_items (
    id              CHAR(36) PRIMARY KEY,
    order_id        CHAR(36) NOT NULL,
    product_id      CHAR(36) NOT NULL,
    variant_id      CHAR(36),
    product_name    VARCHAR(500) NOT NULL,
    variant_name    VARCHAR(255),
    quantity        INT NOT NULL,
    unit_price      DECIMAL(12,2) NOT NULL,
    total_price     DECIMAL(12,2) NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_oi_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_status_history (
    id              CHAR(36) PRIMARY KEY,
    order_id        CHAR(36) NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30) NOT NULL,
    changed_by      CHAR(36),
    note            VARCHAR(500),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_osh_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payments (
    id              CHAR(36) PRIMARY KEY,
    order_id        CHAR(36) NOT NULL,
    payment_method  ENUM('UPI','CARD','NET_BANKING','COD','WALLET') NOT NULL,
    gateway         VARCHAR(50),
    gateway_order_id VARCHAR(255),
    transaction_id  VARCHAR(255),
    idempotency_key CHAR(36) UNIQUE,
    amount          DECIMAL(12,2) NOT NULL,
    currency        VARCHAR(3) DEFAULT 'INR',
    status          ENUM('PENDING','SUCCESS','FAILED','REFUNDED') DEFAULT 'PENDING',
    failure_reason  VARCHAR(500),
    paid_at         DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pay_order (order_id),
    INDEX idx_pay_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shipments (
    id              CHAR(36) PRIMARY KEY,
    order_id        CHAR(36) NOT NULL,
    carrier         VARCHAR(100),
    tracking_number VARCHAR(255),
    tracking_url    VARCHAR(512),
    status          ENUM('PREPARING','PICKED_UP','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','FAILED') DEFAULT 'PREPARING',
    estimated_delivery DATETIME,
    shipped_at      DATETIME,
    delivered_at    DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ship_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS returns_refunds (
    id              CHAR(36) PRIMARY KEY,
    order_id        CHAR(36) NOT NULL,
    order_item_id   CHAR(36) NOT NULL,
    user_id         CHAR(36) NOT NULL,
    type            ENUM('RETURN','REFUND','EXCHANGE') NOT NULL,
    reason          VARCHAR(500) NOT NULL,
    status          ENUM('REQUESTED','APPROVED','PICKED_UP','RECEIVED','REFUNDED','REJECTED') DEFAULT 'REQUESTED',
    refund_amount   DECIMAL(12,2),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rr_order (order_id),
    INDEX idx_rr_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
