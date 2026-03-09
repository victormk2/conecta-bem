CREATE TABLE event_registrations (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    volunteer_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    justification VARCHAR(2000),
    registered_at TIMESTAMP NOT NULL,
    status_updated_at TIMESTAMP,
    CONSTRAINT fk_event_registrations_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_registrations_user FOREIGN KEY (volunteer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ux_event_registrations_event_user UNIQUE (event_id, volunteer_id)
);

CREATE INDEX ix_event_registrations_event ON event_registrations(event_id);
CREATE INDEX ix_event_registrations_user ON event_registrations(volunteer_id);
