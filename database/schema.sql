-- ============================================================
-- CHULE DIGITA
-- Family Business Management System
-- Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS chule_digita_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chule_digita_db;


-- ============================================================
-- 1. FAMILY MEMBERS
-- ============================================================

CREATE TABLE family_members (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NULL,

    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);


-- ============================================================
-- 2. CUSTOMERS
-- ============================================================

CREATE TABLE customers (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NULL,

    created_by BIGINT UNSIGNED NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_customers_created_by
        FOREIGN KEY (created_by)
        REFERENCES family_members(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_customers_name (full_name),
    INDEX idx_customers_phone (phone),
    INDEX idx_customers_created_by (created_by)
);


-- ============================================================
-- 3. CUSTOMER NOTES
-- ============================================================

CREATE TABLE customer_notes (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    customer_id BIGINT UNSIGNED NOT NULL,

    note_text TEXT NOT NULL,

    created_by BIGINT UNSIGNED NOT NULL,

    verification_status ENUM(
        'NOT_REQUIRED',
        'PENDING',
        'VERIFIED',
        'REJECTED'
    ) NOT NULL DEFAULT 'NOT_REQUIRED',

    verified_by BIGINT UNSIGNED NULL,
    verified_at TIMESTAMP NULL,
    verification_note TEXT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_notes_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_notes_created_by
        FOREIGN KEY (created_by)
        REFERENCES family_members(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_notes_verified_by
        FOREIGN KEY (verified_by)
        REFERENCES family_members(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    INDEX idx_notes_customer (customer_id),
    INDEX idx_notes_created_by (created_by),
    INDEX idx_notes_verification (verification_status)
);


-- ============================================================
-- 4. BUSINESS NOTES (Shared family notes)
-- ============================================================

CREATE TABLE business_notes (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    note_text TEXT NOT NULL,

    created_by BIGINT UNSIGNED NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_business_notes_created_by
        FOREIGN KEY (created_by)
        REFERENCES family_members(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_business_notes_created_by (created_by)
);


-- ============================================================
-- 5. ORDERS
-- ============================================================

CREATE TABLE orders (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    customer_id BIGINT UNSIGNED NULL,

    order_number VARCHAR(50) NOT NULL UNIQUE,

    title VARCHAR(150) NULL,

    description TEXT NULL,

    order_status ENUM(
        'DRAFT',
        'PENDING',
        'CONFIRMED',
        'PROCESSING',
        'READY',
        'COMPLETED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',

    taken_by BIGINT UNSIGNED NOT NULL,

    verification_status ENUM(
        'NOT_REQUIRED',
        'PENDING',
        'VERIFIED',
        'REJECTED'
    ) NOT NULL DEFAULT 'PENDING',

    verified_by BIGINT UNSIGNED NULL,
    verified_at TIMESTAMP NULL,
    verification_note TEXT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    CONSTRAINT fk_orders_taken_by
        FOREIGN KEY (taken_by)
        REFERENCES family_members(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_orders_verified_by
        FOREIGN KEY (verified_by)
        REFERENCES family_members(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    INDEX idx_orders_customer (customer_id),
    INDEX idx_orders_taken_by (taken_by),
    INDEX idx_orders_status (order_status),
    INDEX idx_orders_verification (verification_status)
);


-- ============================================================
-- 6. FINANCIAL MOVEMENTS
-- ============================================================

CREATE TABLE financial_movements (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    customer_id BIGINT UNSIGNED NOT NULL,

    amount DECIMAL(15,2) NOT NULL,

    movement_type ENUM(
        'DEBIT',
        'CREDIT'
    ) NOT NULL,

    description TEXT NULL,

    reference_type VARCHAR(50) NULL,
    reference_id BIGINT UNSIGNED NULL,

    -- Used to prevent duplicate payment requests
    idempotency_key VARCHAR(100) NULL,

    recorded_by BIGINT UNSIGNED NOT NULL,

    verification_status ENUM(
        'PENDING',
        'VERIFIED',
        'REJECTED'
    ) NOT NULL DEFAULT 'PENDING',

    verified_by BIGINT UNSIGNED NULL,
    verified_at TIMESTAMP NULL,
    verification_note TEXT NULL,

    transaction_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_financial_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_financial_recorded_by
        FOREIGN KEY (recorded_by)
        REFERENCES family_members(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_financial_verified_by
        FOREIGN KEY (verified_by)
        REFERENCES family_members(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    INDEX idx_financial_customer (customer_id),
    INDEX idx_financial_recorded_by (recorded_by),
    INDEX idx_financial_type (movement_type),
    INDEX idx_financial_verification (verification_status),
    INDEX idx_financial_transaction_at (transaction_at),

    UNIQUE INDEX uq_financial_idempotency_key (idempotency_key)
);


-- ============================================================
-- 7. ACTIVITIES / AUDIT LOG
-- ============================================================

CREATE TABLE activities (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    actor_id BIGINT UNSIGNED NOT NULL,

    activity_type VARCHAR(100) NOT NULL,

    entity_type VARCHAR(50) NOT NULL,

    entity_id BIGINT UNSIGNED NOT NULL,

    description TEXT NULL,

    metadata JSON NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_activities_actor
        FOREIGN KEY (actor_id)
        REFERENCES family_members(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_activities_actor (actor_id),
    INDEX idx_activities_entity (entity_type, entity_id),
    INDEX idx_activities_type (activity_type),
    INDEX idx_activities_created_at (created_at)
);


-- ============================================================
-- 8. NOTIFICATIONS
-- ============================================================

CREATE TABLE notifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,

    recipient_id BIGINT UNSIGNED NOT NULL,

    activity_id BIGINT UNSIGNED NULL,

    notification_type VARCHAR(100) NOT NULL,

    title VARCHAR(255) NOT NULL,

    message TEXT NOT NULL,

    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    read_at TIMESTAMP NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id)
        REFERENCES family_members(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_notifications_activity
        FOREIGN KEY (activity_id)
        REFERENCES activities(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    INDEX idx_notifications_recipient (recipient_id),
    INDEX idx_notifications_read (is_read),
    INDEX idx_notifications_created_at (created_at)
);


-- ============================================================
-- END OF CHULE DIGITA DATABASE SCHEMA
-- ============================================================
