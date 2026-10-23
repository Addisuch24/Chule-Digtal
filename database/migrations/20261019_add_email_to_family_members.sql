-- ============================================================
-- Migration: Add email column to family_members table
-- Date: 2026-10-19
-- Purpose: The backend auth module uses an email field for
--          registration and profile, but the original schema
--          did not include this column.
-- ============================================================

ALTER TABLE family_members
    ADD COLUMN email VARCHAR(255) NULL AFTER phone;

ALTER TABLE family_members
    ADD UNIQUE INDEX uq_family_members_email (email);
