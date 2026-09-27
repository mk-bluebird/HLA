CREATE TABLE anchorline_scoped_nullifiers (
    nullifier_hash BYTEA NOT NULL,
    relying_party_id TEXT NOT NULL,
    purpose TEXT NOT NULL,
    epoch TEXT NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    policy_version TEXT NOT NULL,
    PRIMARY KEY (
        nullifier_hash,
        relying_party_id,
        purpose,
        epoch
    ),
    CHECK (expires_at > accepted_at),
    CHECK (relying_party_id = 'auth.hearthline.example'),
    CHECK (purpose = 'registration')
);

CREATE INDEX anchorline_scoped_nullifiers_expiry_idx
    ON anchorline_scoped_nullifiers (expires_at);
