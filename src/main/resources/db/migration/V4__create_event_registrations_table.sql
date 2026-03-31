DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'registration_status') THEN
        CREATE TYPE registration_status AS ENUM ('PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED'));
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS event_registrations (
    id UUID PRIMARY KEY,
    event UUID NOT NULL,
    volunteer UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    justification VARCHAR(2000),
    registered_at TIMESTAMP NOT NULL,
    status_updated_at TIMESTAMP,
    CONSTRAINT fk_event_registrations_event FOREIGN KEY (event) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_registrations_user FOREIGN KEY (volunteer) REFERENCES users(id) ON DELETE CASCADE,
);

CREATE INDEX ix_event_registrations_event ON event_registrations(event);
CREATE INDEX ix_event_registrations_user ON event_registrations(volunteer);
