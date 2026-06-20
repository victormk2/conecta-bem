ALTER TYPE participation_status ADD VALUE IF NOT EXISTS 'PRESENT';
ALTER TYPE participation_status ADD VALUE IF NOT EXISTS 'ABSENT';
ALTER TYPE participation_status ADD VALUE IF NOT EXISTS 'JUSTIFIED';
ALTER TYPE participation_status ADD VALUE IF NOT EXISTS 'DISMISSED';

CREATE UNIQUE INDEX IF NOT EXISTS ux_event_registrations_event_volunteer
    ON event_registrations(event, volunteer);
