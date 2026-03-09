-- Flyway migration: create events table
CREATE TABLE events (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(3000),
    location VARCHAR(255) NOT NULL,
    activity_type VARCHAR(255) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP,
    capacity INTEGER,
    owner_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX ix_events_owner_id_starts_at ON events(owner_id, starts_at);
