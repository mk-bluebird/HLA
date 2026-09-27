package org.hearthline.session

import java.time.Instant
import java.util.UUID

@JvmInline
value class AccountId(val value: UUID)

@JvmInline
value class CredentialId(val value: String)

@JvmInline
value class SessionId(val value: UUID)

@JvmInline
value class ActionAuthorizationId(val value: UUID)

enum class AccountStatus {
    ACTIVE,
    SECURITY_LOCKED,
    CLOSED
}

enum class CredentialStatus {
    ACTIVE,
    REVOKED
}

enum class SessionStatus {
    ACTIVE,
    REVOKED,
    EXPIRED
}

enum class SessionChannel {
    WEB,
    NATIVE_APP,
    DESKTOP_APP
}

/**
 * This record is credential-bound, not device-bound.
 *
 * There is intentionally no:
 * - device ID
 * - device name
 * - platform version
 * - browser fingerprint
 * - IP address or IP hash
 * - network/ASN/location field
 * - hardware/attestation classification
 * - "trusted device" flag
 * - presence or last-active signal
 */
data class CredentialBoundSession(
    val sessionId: SessionId,
    val accountId: AccountId,

    /*
     * This reference belongs exclusively to the authentication boundary. It exists
     * only so that revoking a credential can end sessions created by that credential.
     * It must never be emitted to product, profile, discovery, matching, analytics,
     * moderation-priority, or counterpart services.
     */
    val authenticatingCredentialId: CredentialId,

    /*
     * Credential generation changes whenever a credential is revoked/replaced.
     * This lets authorization reject stale sessions without inspecting a device.
     */
    val credentialGenerationAtIssue: Long,

    val issuedAt: Instant,
    val lastReauthenticatedAt: Instant,
    val expiresAt: Instant,
    val status: SessionStatus,
    val revokedAt: Instant? = null,

    /*
     * Coarse protocol channel is optional operational data. It is not a device
     * classification and cannot be used outside the session/authentication boundary.
     *
     * In an even stricter deployment, remove this field entirely.
     */
    val channel: SessionChannel? = null
)

data class AccountSecurityState(
    val accountId: AccountId,
    val status: AccountStatus,
    val sessionGeneration: Long,
    val updatedAt: Instant
)

data class CredentialSecurityState(
    val accountId: AccountId,
    val credentialId: CredentialId,
    val status: CredentialStatus,
    val generation: Long,
    val updatedAt: Instant
)

data class ActionAuthorization(
    val actionAuthorizationId: ActionAuthorizationId,
    val accountId: AccountId,
    val sessionId: SessionId,
    val action: SensitiveAction,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val consumedAt: Instant? = null
)

enum class SensitiveAction {
    ADD_PASSKEY,
    REMOVE_PASSKEY,
    START_ACCOUNT_EXPORT,
    CHANGE_RECOVERY_CONFIGURATION,
    DELETE_ACCOUNT
}
