package org.hearthline.anchorline

import java.time.Instant
import java.util.UUID

@JvmInline
value class AccountId(val value: UUID)

@JvmInline
value class CredentialId private constructor(
    private val bytes: ByteArray
) {
    fun copyBytes(): ByteArray = bytes.copyOf()

    override fun equals(other: Any?): Boolean =
        other is CredentialId && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()

    companion object {
        fun of(value: ByteArray): CredentialId {
            require(value.isNotEmpty()) {
                "Credential identifier cannot be empty."
            }
            require(value.size <= 1024) {
                "Credential identifier exceeds the permitted size."
            }
            return CredentialId(value.copyOf())
        }
    }
}

@JvmInline
value class ChallengeId(val value: UUID)

@JvmInline
value class SessionId(val value: UUID)

@JvmInline
value class CapabilityGrantId(val value: UUID)

@JvmInline
value class EpochId(val value: UUID)

enum class TrustZone {
    AUTHENTICATION,
    RECOVERY,
    AGE_GATE,
    INTEGRITY,
    GOVERNANCE,
    CONNECTION_PRODUCT,

    PROFILE,
    DISCOVERY,
    MATCHING,
    RANKING,
    RECOMMENDATION,
    MESSAGING,
    COUNTERPART_NOTIFICATION,
    ANALYTICS,
    EXPERIMENTATION,
    ADVERTISING,
    MODERATION_PRIORITY,
    TRUST_SCORING,
    SAFETY_SCORING,
    RESEARCH
}

enum class AccountStatus {
    ACTIVE,
    SECURITY_LOCKED,
    CLOSED
}

enum class CredentialStatus {
    ACTIVE,
    REVOKED,
    REPLACED
}

enum class SessionStatus {
    ACTIVE,
    REVOKED,
    EXPIRED
}

enum class CapabilityStatus {
    ACTIVE,
    CONSUMED,
    REVOKED,
    EXPIRED
}

enum class SensitiveAction {
    ADD_PASSKEY,
    REMOVE_PASSKEY,
    START_EXPORT,
    DOWNLOAD_EXPORT,
    CHANGE_RECOVERY,
    COMPLETE_RECOVERY,
    DELETE_ACCOUNT
}

class BoundaryViolation(message: String) : IllegalStateException(message)

class AuthorizationDenied(message: String) : RuntimeException(message)

class FreshAuthenticationRequired(message: String) : RuntimeException(message)

data class LifecycleExpiry(
    val expiresAt: Instant
) {
    fun expiredAt(now: Instant): Boolean = !now.isBefore(expiresAt)
}
