USE chule_digita_db;

ALTER TABLE orders
    ADD COLUMN title VARCHAR(150) NULL AFTER order_number;

ALTER TABLE orders
    MODIFY order_status ENUM(
        'DRAFT',
        'PENDING',
        'CONFIRMED',
        'PROCESSING',
        'READY',
        'COMPLETED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING';
