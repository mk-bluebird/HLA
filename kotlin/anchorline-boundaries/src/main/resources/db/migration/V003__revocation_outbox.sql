CREATE TABLE anchorline_revocation_outbox (
    event_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    credential_id BYTEA NULL,
    account_generation BIGINT NOT NULL,
    credential_generation BIGINT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    delivery_deadline TIMESTAMPTZ NOT NULL,
    delivered_at TIMESTAMPTZ NULL,
    CHECK (delivery_deadline > occurred_at)
);

CREATE INDEX anchorline_revocation_outbox_deadline_idx
    ON anchorline_revocation_outbox (delivery_deadline);
