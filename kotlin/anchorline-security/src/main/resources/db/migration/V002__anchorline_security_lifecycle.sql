-- AnchorLine security lifecycle schema (forward-only).
-- Privacy-minimized: no IP, user agent, fingerprint, device ID, hardware class,
-- AAGUID, attestation, browser/platform/OS, location, presence, provider risk,
-- provider account age, email, name, DOB, age range, document, profile,
-- message, risk/trust/uniqueness score columns anywhere in this migration.

CREATE TABLE anchorline_registration_challenges (
    challenge_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    challenge BYTEA NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CHECK (expires_at > issued_at)
);

CREATE INDEX anchorline_registration_challenges_expiry_idx
    ON anchorline_registration_challenges (expires_at);

CREATE INDEX anchorline_registration_challenges_account_idx
    ON anchorline_registration_challenges (account_id);

CREATE TABLE anchorline_signup_challenges (
    challenge_id UUID PRIMARY KEY,
    challenge BYTEA NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    global_pow_difficulty INT NOT NULL DEFAULT 0,
    CHECK (expires_at > issued_at),
    CHECK (global_pow_difficulty >= 0)
);

CREATE INDEX anchorline_signup_challenges_expiry_idx
    ON anchorline_signup_challenges (expires_at);

CREATE TABLE anchorline_signup_tickets (
    ticket_id UUID PRIMARY KEY,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    passkey_registration_remaining INT NOT NULL,
    age_proof_remaining INT NOT NULL,
    account_shell_remaining INT NOT NULL,
    CHECK (expires_at > issued_at),
    CHECK (passkey_registration_remaining >= 0),
    CHECK (age_proof_remaining >= 0),
    CHECK (account_shell_remaining >= 0)
);

CREATE INDEX anchorline_signup_tickets_expiry_idx
    ON anchorline_signup_tickets (expires_at);

CREATE TABLE anchorline_passkeys (
    account_id UUID NOT NULL,
    credential_id BYTEA NOT NULL,
    public_key BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL,
    generation BIGINT NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_at TIMESTAMPTZ,
    PRIMARY KEY (account_id, credential_id),
    CHECK (status IN ('ACTIVE', 'REVOKED', 'REPLACED')),
    CHECK (generation >= 1)
);

CREATE INDEX anchorline_passkeys_account_idx
    ON anchorline_passkeys (account_id, status);

CREATE TABLE anchorline_account_security_state (
    account_id UUID PRIMARY KEY,
    status TEXT NOT NULL,
    session_generation BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (status IN ('ACTIVE', 'SECURITY_LOCKED', 'CLOSED')),
    CHECK (session_generation >= 0)
);

CREATE TABLE anchorline_credential_security_state (
    account_id UUID NOT NULL,
    credential_id BYTEA NOT NULL,
    status TEXT NOT NULL,
    generation BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (account_id, credential_id),
    CHECK (status IN ('ACTIVE', 'REVOKED', 'REPLACED')),
    CHECK (generation >= 1)
);

CREATE INDEX anchorline_credential_security_state_account_idx
    ON anchorline_credential_security_state (account_id);

CREATE TABLE anchorline_sessions (
    session_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    credential_id BYTEA NOT NULL,
    credential_generation_at_issue BIGINT NOT NULL,
    account_session_generation_at_issue BIGINT NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    last_fresh_auth_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status TEXT NOT NULL,
    sender_constraint_public_key_hash BYTEA,
    revoked_at TIMESTAMPTZ,
    CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    CHECK (expires_at > issued_at)
);

CREATE INDEX anchorline_sessions_expiry_idx
    ON anchorline_sessions (expires_at);

CREATE INDEX anchorline_sessions_account_idx
    ON anchorline_sessions (account_id, status);

CREATE INDEX anchorline_sessions_credential_revocation_idx
    ON anchorline_sessions (account_id, credential_id);

CREATE TABLE anchorline_action_authorizations (
    authorization_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    session_id UUID NOT NULL,
    action TEXT NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CHECK (expires_at > issued_at)
);

CREATE INDEX anchorline_action_authorizations_expiry_idx
    ON anchorline_action_authorizations (expires_at);

CREATE INDEX anchorline_action_authorizations_account_idx
    ON anchorline_action_authorizations (account_id);

CREATE TABLE anchorline_age_proof_challenges (
    challenge_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    audience TEXT NOT NULL,
    nonce BYTEA NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CHECK (expires_at > issued_at)
);

CREATE INDEX anchorline_age_proof_challenges_expiry_idx
    ON anchorline_age_proof_challenges (expires_at);

CREATE INDEX anchorline_age_proof_challenges_account_idx
    ON anchorline_age_proof_challenges (account_id);

CREATE TABLE anchorline_age_eligibility_grants (
    account_id UUID PRIMARY KEY,
    eligibility_state TEXT NOT NULL,
    expires_at TIMESTAMPTZ,
    policy_version TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (eligibility_state IN (
        'UNKNOWN', 'ELIGIBLE', 'NOT_ELIGIBLE',
        'UNAVAILABLE', 'EXPIRED', 'REVIEW_REQUESTED'
    ))
);

CREATE TABLE anchorline_recovery_cases (
    recovery_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    state TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    authorized_at TIMESTAMPTZ,
    hold_ends_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    authorization_method TEXT,
    CHECK (expires_at > created_at),
    CHECK (state IN (
        'REQUESTED', 'HOLD', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'REVIEW_REQUIRED'
    )),
    CHECK (authorization_method IS NULL OR authorization_method IN (
        'SECOND_REGISTERED_PASSKEY', 'INDEPENDENT_SECURITY_KEY',
        'OFFLINE_RECOVERY_CODE', 'PREESTABLISHED_FEDERATED_BINDING',
        'GOVERNED_EXCEPTIONAL_PROCESS'
    ))
);

CREATE INDEX anchorline_recovery_cases_expiry_idx
    ON anchorline_recovery_cases (expires_at);

CREATE INDEX anchorline_recovery_cases_account_lookup_idx
    ON anchorline_recovery_cases (account_id, state);

CREATE TABLE anchorline_recovery_challenges (
    recovery_id UUID PRIMARY KEY,
    challenge BYTEA NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CHECK (expires_at > issued_at)
);

CREATE INDEX anchorline_recovery_challenges_expiry_idx
    ON anchorline_recovery_challenges (expires_at);

CREATE TABLE anchorline_recovery_code_verifiers (
    account_id UUID NOT NULL,
    verifier BYTEA NOT NULL,
    salt BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX anchorline_recovery_code_verifiers_account_idx
    ON anchorline_recovery_code_verifiers (account_id);

CREATE TABLE anchorline_federated_login_bindings (
    binding_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    issuer_uri TEXT NOT NULL,
    encrypted_subject BYTEA NOT NULL,
    encryption_key_version INT NOT NULL,
    client_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_successful_use_at TIMESTAMPTZ,
    status TEXT NOT NULL,
    CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED'))
);

CREATE UNIQUE INDEX anchorline_federated_login_bindings_active_idx
    ON anchorline_federated_login_bindings (issuer_uri, encrypted_subject)
    WHERE status = 'ACTIVE';

CREATE INDEX anchorline_federated_login_bindings_account_idx
    ON anchorline_federated_login_bindings (account_id);
