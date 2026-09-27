CREATE TABLE lantern_scoped_nullifier_records (
    epoch_id UUID NOT NULL,
    purpose TEXT NOT NULL,
    encrypted_nullifier BYTEA NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (epoch_id, purpose, encrypted_nullifier),
    CHECK (purpose = 'registration'),
    CHECK (expires_at > accepted_at)
);

CREATE INDEX lantern_scoped_nullifier_expiry_idx
    ON lantern_scoped_nullifier_records (expires_at);
