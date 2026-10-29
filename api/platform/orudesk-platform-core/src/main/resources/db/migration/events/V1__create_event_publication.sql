-- Day 51: the event publication registry ("outbox") of Spring Modulith. When a module publishes an event that
-- an @ApplicationModuleListener listens to, one row per listener is written here IN THE PUBLISHER'S TRANSACTION.
-- The listener runs after the commit; when it succeeds the row gets a completion_date. A row without one is
-- work that still has to happen: after a crash, a failed listener, or an email server that was down.
--
-- The same table as Spring Modulith's own schema (v2, PostgreSQL), created by Flyway rather than at startup, so
-- the schema has one owner and a self-hoster can read every table's history in the migrations.
CREATE TABLE event_publication (
    id                      UUID                     NOT NULL PRIMARY KEY,
    listener_id             TEXT                     NOT NULL,   -- which listener method (one row each)
    event_type              TEXT                     NOT NULL,   -- the event's Java class
    serialized_event        TEXT                     NOT NULL,   -- the event as JSON: never put secrets in events
    publication_date        TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date         TIMESTAMP WITH TIME ZONE NULL,       -- NULL: not done yet
    status                  TEXT                     NULL,       -- PUBLISHED, PROCESSING, COMPLETED, FAILED, RESUBMITTED
    completion_attempts     INT                      NULL,
    last_resubmission_date  TIMESTAMP WITH TIME ZONE NULL
);

-- Spring Modulith finds a publication by its event's JSON when marking it complete: a hash index makes that
-- equality lookup cheap without the size limit of a B-tree entry (a long JSON can exceed 2.7 kB)
CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);

-- "What's still incomplete?" and "delete what completed a week ago" both ask by completion date
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
