-- V1__create_catalog_tables.sql
-- Catalog Service schema

CREATE TABLE IF NOT EXISTS sellers (
    id              CHAR(36) PRIMARY KEY,
    user_id         CHAR(36) NOT NULL UNIQUE,
    store_name      VARCHAR(255) NOT NULL,
    store_slug      VARCHAR(255) NOT NULL UNIQUE,
    description     TEXT,
    logo_url        VARCHAR(512),
    gst_number      VARCHAR(20),
    avg_rating      DECIMAL(3,2) DEFAULT 0.00,
    status          ENUM('PENDING','ACTIVE','SUSPENDED') DEFAULT 'PENDING',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sellers_slug (store_slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS categories (
    id              CHAR(36) PRIMARY KEY,
    parent_id       CHAR(36),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL UNIQUE,
    image_url       VARCHAR(512),
    display_order   INT DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    level           TINYINT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES categories(id),
    INDEX idx_cat_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS products (
    id              CHAR(36) PRIMARY KEY,
    seller_id       CHAR(36) NOT NULL,
    category_id     CHAR(36) NOT NULL,
    name            VARCHAR(500) NOT NULL,
    slug            VARCHAR(500) NOT NULL UNIQUE,
    description     TEXT,
    base_price      DECIMAL(12,2) NOT NULL,
    currency        VARCHAR(3) DEFAULT 'INR',
    status          ENUM('DRAFT','ACTIVE','ARCHIVED') DEFAULT 'DRAFT',
    avg_rating      DECIMAL(3,2) DEFAULT 0.00,
    review_count    INT DEFAULT 0,
    total_sold      BIGINT DEFAULT 0,
    weight_grams    INT,
    version         INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_prod_seller (seller_id),
    INDEX idx_prod_category (category_id),
    INDEX idx_prod_status (status),
    INDEX idx_prod_price (base_price),
    FULLTEXT INDEX idx_prod_search (name, description),
    FOREIGN KEY (seller_id) REFERENCES sellers(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_variants (
    id              CHAR(36) PRIMARY KEY,
    product_id      CHAR(36) NOT NULL,
    sku             VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    price           DECIMAL(12,2) NOT NULL,
    compare_at_price DECIMAL(12,2),
    attributes      JSON,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_pv_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_images (
    id              CHAR(36) PRIMARY KEY,
    product_id      CHAR(36) NOT NULL,
    variant_id      CHAR(36),
    url             VARCHAR(512) NOT NULL,
    alt_text        VARCHAR(255),
    display_order   INT DEFAULT 0,
    is_primary      BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_pi_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS inventory (
    id              CHAR(36) PRIMARY KEY,
    variant_id      CHAR(36) NOT NULL,
    seller_id       CHAR(36) NOT NULL,
    quantity        INT NOT NULL DEFAULT 0,
    reserved        INT NOT NULL DEFAULT 0,
    reorder_level   INT DEFAULT 10,
    version         INT DEFAULT 0,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_inv (variant_id, seller_id),
    FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    INDEX idx_inv_variant (variant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reviews (
    id              CHAR(36) PRIMARY KEY,
    user_id         CHAR(36) NOT NULL,
    product_id      CHAR(36) NOT NULL,
    rating          TINYINT NOT NULL,
    title           VARCHAR(255),
    body            TEXT,
    is_verified     BOOLEAN DEFAULT FALSE,
    status          ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    UNIQUE KEY uq_review (user_id, product_id),
    INDEX idx_rev_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wishlists (
    id              CHAR(36) PRIMARY KEY,
    user_id         CHAR(36) NOT NULL,
    product_id      CHAR(36) NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    UNIQUE KEY uq_wish (user_id, product_id),
    INDEX idx_wl_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
