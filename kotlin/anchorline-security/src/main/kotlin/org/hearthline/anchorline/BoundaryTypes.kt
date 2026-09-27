package org.hearthline.anchorline

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

@JvmInline
value class AccountId(val value: UUID)

@JvmInline
value class CredentialId(val value: ByteArray) {
    override fun equals(other: Any?): Boolean =
        other is CredentialId && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()

    fun encoded(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value)
}

@JvmInline
value class SessionId(val value: UUID)

@JvmInline
value class ChallengeId(val value: UUID)

@JvmInline
value class RecoveryId(val value: UUID)

@JvmInline
value class FederationBindingId(val value: UUID)

@JvmInline
value class ActionAuthorizationId(val value: UUID)

@JvmInline
value class SignupTicketId(val value: UUID)

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

enum class FederationBindingStatus {
    ACTIVE,
    DISABLED,
    DELETED
}

enum class AgeEligibilityState {
    UNKNOWN,
    ELIGIBLE,
    NOT_ELIGIBLE,
    UNAVAILABLE,
    EXPIRED,
    REVIEW_REQUESTED
}

enum class RecoveryState {
    REQUESTED,
    HOLD,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    REVIEW_REQUIRED
}

enum class SensitiveAction {
    ADD_PASSKEY,
    REMOVE_PASSKEY,
    START_ACCOUNT_EXPORT,
    CHANGE_RECOVERY_CONFIGURATION,
    DELETE_ACCOUNT,
    CONSOLIDATE_ACCOUNTS
}

enum class SignupCapacityMode {
    OPEN,
    ELEVATED_LOAD,
    QUEUED,
    PAUSED
}

/**
 * These destinations are intentionally named as architectural boundaries,
 * rather than product features. EgressPolicy rejects protected security
 * material when a request attempts to target a prohibited zone.
 */
enum class ServiceZone {
    AUTHENTICATION_BOUNDARY,
    AGE_GATE,
    RECOVERY_BOUNDARY,
    SECURITY_HISTORY,
    APPEAL_BOUNDARY,

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
    ELIGIBILITY_ENGINE
}

object CryptoSupport {
    private val secureRandom = SecureRandom()

    fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also(secureRandom::nextBytes)

    fun sha256(value: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value)

    fun constantTimeEquals(left: ByteArray, right: ByteArray): Boolean =
        MessageDigest.isEqual(left, right)

    fun base64Url(value: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value)
}

class BoundaryViolation(message: String) : IllegalStateException(message)

class AuthorizationDenied(message: String) : RuntimeException(message)

class FreshPasskeyRequired(message: String) : RuntimeException(message)

class RecoveryDenied(message: String) : RuntimeException(message)

class RegistrationDenied(message: String) : RuntimeException(message)
