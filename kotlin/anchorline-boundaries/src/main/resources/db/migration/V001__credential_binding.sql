CREATE TABLE anchorline_active_credential_bindings (
    credential_id BYTEA PRIMARY KEY,
    account_id UUID NOT NULL,
    bound_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL
);

CREATE INDEX anchorline_active_credential_bindings_account_idx
    ON anchorline_active_credential_bindings (account_id);
