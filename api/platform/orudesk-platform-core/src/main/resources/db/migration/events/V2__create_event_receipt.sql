-- Idempotent receivers (Day 54). The outbox delivers at least once: a listener can run twice for the same fact
-- (two instances resubmitting, a crash after the work but before "completed" was written). A listener whose work
-- must not happen twice records a key here in its own transaction; the primary key makes the second try a no-op.
CREATE TABLE event_receipt (
    receipt_key   VARCHAR(200)              NOT NULL PRIMARY KEY,
    received_at   TIMESTAMP WITH TIME ZONE  NOT NULL
);

-- For the cleanup of old receipts
CREATE INDEX ix_event_receipt_received_at ON event_receipt (received_at);
