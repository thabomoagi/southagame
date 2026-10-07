-- Add last_password_reset_at column to users table for tracking password reset frequency
ALTER TABLE users ADD COLUMN last_password_reset_at TIMESTAMP;