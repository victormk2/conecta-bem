ALTER TABLE event_registrations
    ADD CONSTRAINT uq_event_registrations_event_volunteer UNIQUE (event, volunteer);