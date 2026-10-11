USE chule_digita_db;

CREATE TABLE IF NOT EXISTS business_notes (
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
