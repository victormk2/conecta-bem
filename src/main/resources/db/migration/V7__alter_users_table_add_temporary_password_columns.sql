ALTER TABLE users
    ADD COLUMN IF NOT EXISTS temporary_password VARCHAR(10),
    ADD COLUMN IF NOT EXISTS temporary_password_expires_at TIMESTAMP;