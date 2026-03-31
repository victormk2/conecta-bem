DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'event_category') THEN
        CREATE TYPE event_category AS ENUM ('ENVIRONMENT', 'SOCIAL', 'EDUCATION', 'HEALTH', 'ANIMAL_WELFARE', 'OTHER');
    END IF;
END$$;

-- Flyway migration: create events table
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(3000),
    address UUID NOT NULL,
    category event_category NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP,
    capacity INTEGER,
    owner UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_events_owner FOREIGN KEY (owner) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_events_address FOREIGN KEY (address) REFERENCES addresses(id) ON DELETE CASCADE
);

CREATE INDEX ix_events_owner_starts_at ON events(owner, starts_at);
