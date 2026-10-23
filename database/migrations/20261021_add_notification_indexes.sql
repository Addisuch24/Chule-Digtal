-- Migration: Add composite index for fast recipient unread notification queries
ALTER TABLE notifications ADD INDEX idx_notifications_recipient_unread (recipient_id, is_read, created_at);
