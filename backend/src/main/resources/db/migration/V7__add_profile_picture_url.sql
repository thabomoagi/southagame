-- Ensures the profile_picture_url column exists on the users table.
-- Idempotent: safe on both fresh installs (where V1 already creates it)
-- and existing databases that predate this migration.
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_picture_url VARCHAR(500);
