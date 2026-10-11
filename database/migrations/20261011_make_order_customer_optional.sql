USE chule_digita_db;

ALTER TABLE orders
    DROP FOREIGN KEY fk_orders_customer;

ALTER TABLE orders
    MODIFY customer_id BIGINT UNSIGNED NULL;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE;
