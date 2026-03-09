CREATE TABLE event_announcements (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    author_id UUID NOT NULL,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_event_announcements_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_announcements_user FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX ix_event_announcements_event_created_at ON event_announcements(event_id, created_at DESC);
