-- Flyway migration: create users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP
);

CREATE UNIQUE INDEX ux_users_email ON users(email);

