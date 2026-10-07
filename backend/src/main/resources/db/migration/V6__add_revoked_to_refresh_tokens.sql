-- Add revoked flag to refresh_tokens for rotation/reuse detection.
-- When a token is rotated or a user changes their password, it is marked
-- revoked instead of being silently dropped so that a replayed/stolen token
-- can be detected and all the user's sessions revoked.
ALTER TABLE refresh_tokens ADD COLUMN revoked BOOLEAN NOT NULL DEFAULT FALSE;