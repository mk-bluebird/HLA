package org.hearthline.authintegrity

import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Reference implementation for Hearthline's credential and integrity boundary.
 *
 * Security properties enforced here:
 *
 * 1. A passkey assertion authenticates a registered credential for a challenge.
 *    It does not establish a human identity, physical-device identity, uniqueness,
 *    or social trust.
 *
 * 2. Credential revocation is credential-scoped because a relying party does not
 *    know, enumerate, or revoke a credential-manager "sync group".
 *
 * 3. Attestation is challenge-bound, action-scoped, single-use, short-lived, and
 *    never retained as member-level state.
 *
 * 4. Attestation outcomes cannot be sent to discovery, matching, ranking,
 *    recommendation, profile, analytics, experimentation, or counterpart systems.
 *
 * 5. Attestation unavailability/failure routes only the immediate action to an
 *    accessible alternative. It does not change ordinary member participation.
 *
 * This file deliberately does NOT include:
 * - hardware identifiers
 * - device model or OS version
 * - credential-manager provider
 * - backup eligibility/state
 * - passkey sync metadata
 * - IP-derived fingerprints
 * - attestation key identifiers
 * - durable attestation histories or counts
 */
object AuthIntegrityBoundary {
    const val CHALLENGE_TTL_SECONDS: Long = 5 * 60
    const val RAW_EVIDENCE_TTL_SECONDS: Long = 10 * 60
    const val ACTION_AUTHORIZATION_TTL_SECONDS: Long = 10 * 60
    const val MEMBER_VISIBLE_SECURITY_EVENT_TTL_SECONDS: Long = 30L * 24 * 60 * 60
    const val SESSION_TTL_SECONDS: Long = 30 * 60
    const val REVOCATION_SLO_SECONDS: Long = 60
}

@JvmInline
value class AccountId(val value: UUID)

@JvmInline
value class CredentialId(val value: ByteArray) {
    override fun equals(other: Any?): Boolean =
        other is CredentialId && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()

    fun stableStoreKey(): String =
        value.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

@JvmInline
value class SessionId(val value: UUID)

@JvmInline
value class ChallengeId(val value: UUID)

@JvmInline
value class ActionAuthorizationId(val value: UUID)

enum class CredentialStatus {
    ACTIVE,
    REVOKED
}

enum class SessionStatus {
    ACTIVE,
    REVOKED,
    EXPIRED
}

enum class ProtectedActionClass {
    REGISTRATION_FINALIZE,
    PASSKEY_ADD,
    PASSKEY_REMOVE,
    ACCOUNT_EXPORT_START,
    SENSITIVE_SETTING_CHANGE,
    HIGH_RATE_INVITATION_SUBMIT,
    HIGH_RATE_MEDIA_UPLOAD
}

enum class AttestationDecision {
    ALLOW,
    ROUTE_TO_ACCESSIBLE_ALTERNATIVE,
    RETRY_WITH_NEW_CHALLENGE,
    DENY_THIS_ACTION
}

enum class IntegrityRoute {
    STANDARD_PATH,
    PASSKEY_ONLY_PATH,
    LOWER_RATE_PATH,
    DELAYED_COMPLETION_PATH,
    HUMAN_REVIEW_OF_ACTION_PATH
}

enum class SecurityEventKind {
    PASSKEY_REGISTERED,
    PASSKEY_REVOKED,
    SESSION_REVOKED,
    ALL_SESSIONS_REVOKED,
    SENSITIVE_ACTION_ROUTED,
    RECOVERY_LOCK_ENABLED
}

/**
 * This data model contains only verification material needed by the authentication
 * boundary. A public-key credential is not a device record and must never be
 * enriched with device, sync-provider, or attestation metadata.
 */
data class RegisteredCredential(
    val accountId: AccountId,
    val credentialId: CredentialId,
    val publicKeyCose: ByteArray,
    val createdAt: Instant,
    val status: CredentialStatus,
    val revokedAt: Instant? = null
)

data class AuthSession(
    val sessionId: SessionId,
    val accountId: AccountId,
    val authenticatingCredentialId: CredentialId,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val status: SessionStatus,
    val revokedAt: Instant? = null
)

/**
 * A one-time server challenge. `requestBindingHash` is a SHA-256 hash of a
 * canonical action request, not raw profile content, a member identifier, or
 * any personally identifying value.
 */
data class IntegrityChallenge(
    val challengeId: ChallengeId,
    val accountId: AccountId?,
    val actionClass: ProtectedActionClass,
    val challengeBytes: ByteArray,
    val requestBindingHash: ByteArray,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val consumedAt: Instant? = null
) {
    fun isExpired(now: Instant): Boolean = now >= expiresAt
    fun isConsumed(): Boolean = consumedAt != null
}

/**
 * Raw evidence must remain in verifier-local memory only. It is not serializable,
 * not placed on an event bus, and not written to account, profile, analytics,
 * matching, discovery, or moderation stores.
 */
sealed interface RawAttestationEvidence {
    val receivedAt: Instant

    data class AppleAppAttest(
        val assertionObject: ByteArray,
        override val receivedAt: Instant
    ) : RawAttestationEvidence

    data class GooglePlayIntegrity(
        val signedIntegrityToken: String,
        override val receivedAt: Instant
    ) : RawAttestationEvidence

    data class Unsupported(
        override val receivedAt: Instant
    ) : RawAttestationEvidence
}

/**
 * This is intentionally minimal. It represents an action-local authorization,
 * not a member attribute, trust score, or device state.
 */
data class ActionAuthorization(
    val authorizationId: ActionAuthorizationId,
    val actionClass: ProtectedActionClass,
    val challengeId: ChallengeId,
    val decision: AttestationDecision,
    val route: IntegrityRoute,
    val issuedAt: Instant,
    val expiresAt: Instant
) {
    fun isExpired(now: Instant): Boolean = now >= expiresAt
}

/**
 * The member-facing receipt is deliberately unable to contain:
 * - platform/vendor name
 * - device details
 * - raw verdict
 * - key identifiers
 * - cumulative pass/fail counts
 * - a persistent reliability classification
 */
data class SecurityHistoryReceipt(
    val accountId: AccountId,
    val eventKind: SecurityEventKind,
    val actionClass: ProtectedActionClass?,
    val outcome: String,
    val occurredAt: Instant,
    val expiresAt: Instant
)

interface CredentialRepository {
    fun find(accountId: AccountId, credentialId: CredentialId): RegisteredCredential?
    fun save(credential: RegisteredCredential)
    fun revoke(accountId: AccountId, credentialId: CredentialId, revokedAt: Instant): Boolean
    fun allActiveForAccount(accountId: AccountId): List<RegisteredCredential>
    fun revokeAllForAccount(accountId: AccountId, revokedAt: Instant): Int
}

interface SessionRepository {
    fun find(sessionId: SessionId): AuthSession?
    fun save(session: AuthSession)
    fun revoke(sessionId: SessionId, revokedAt: Instant): Boolean
    fun revokeByCredential(
        accountId: AccountId,
        credentialId: CredentialId,
        revokedAt: Instant
    ): Int

    fun revokeAllForAccount(accountId: AccountId, revokedAt: Instant): Int
}

interface ChallengeRepository {
    fun save(challenge: IntegrityChallenge)
    fun find(challengeId: ChallengeId): IntegrityChallenge?
    fun consume(challengeId: ChallengeId, consumedAt: Instant): IntegrityChallenge?
    fun deleteExpired(now: Instant): Int
}

interface ActionAuthorizationRepository {
    fun save(authorization: ActionAuthorization)
    fun find(authorizationId: ActionAuthorizationId): ActionAuthorization?
    fun deleteExpired(now: Instant): Int
}

interface SecurityHistoryRepository {
    fun append(receipt: SecurityHistoryReceipt)
    fun deleteExpired(now: Instant): Int
}

/**
 * Cryptographic protocol adapter.
 *
 * Production implementations must use a reviewed WebAuthn library and fully
 * validate:
 * - clientDataJSON type
 * - challenge
 * - origin
 * - RP ID hash
 * - user-verification policy
 * - authenticator signature
 * - public key
 * - signature-counter behavior according to policy
 */
interface WebAuthnVerifier {
    fun verifyAssertion(
        expectedChallenge: ByteArray,
        credentialPublicKeyCose: ByteArray,
        assertion: WebAuthnAssertion
    ): Boolean
}

data class WebAuthnAssertion(
    val credentialId: CredentialId,
    val clientDataJson: ByteArray,
    val authenticatorData: ByteArray,
    val signature: ByteArray,
    val userHandle: ByteArray?
)

/**
 * Apple and Google verification must be server-side and challenge-bound.
 *
 * Implementations must reject:
 * - mismatched audience/app identity
 * - mismatched request binding
 * - expired evidence
 * - replayed challenge
 * - unsupported issuer chain
 *
 * These interfaces intentionally return only an action-local decision.
 * They do not expose device identifiers, key IDs, platform versions, or raw
 * verdict characteristics to callers.
 */
interface AppleAppAttestVerifier {
    fun verify(
        actionClass: ProtectedActionClass,
        challenge: IntegrityChallenge,
        evidence: RawAttestationEvidence.AppleAppAttest
    ): AttestationDecision
}

interface GooglePlayIntegrityVerifier {
    fun verify(
        actionClass: ProtectedActionClass,
        challenge: IntegrityChallenge,
        evidence: RawAttestationEvidence.GooglePlayIntegrity
    ): AttestationDecision
}

class PasskeyAuthenticationService(
    private val clock: Clock,
    private val credentials: CredentialRepository,
    private val sessions: SessionRepository,
    private val webAuthn: WebAuthnVerifier,
    private val securityHistory: SecurityHistoryRepository
) {
    /**
     * A successful assertion proves only possession/use of a registered credential
     * for this ceremony. It must not create any device, trust, or member-quality
     * attribute.
     */
    fun authenticate(
        accountId: AccountId,
        expectedChallenge: ByteArray,
        assertion: WebAuthnAssertion
    ): AuthSession {
        val now = clock.instant()
        val credential = credentials.find(accountId, assertion.credentialId)
            ?: throw AuthenticationDenied("Credential is not registered for this account.")

        if (credential.status != CredentialStatus.ACTIVE) {
            throw AuthenticationDenied("Credential is no longer active.")
        }

        val verified = webAuthn.verifyAssertion(
            expectedChallenge = expectedChallenge,
            credentialPublicKeyCose = credential.publicKeyCose,
            assertion = assertion
        )

        if (!verified) {
            throw AuthenticationDenied("Passkey assertion could not be verified.")
        }

        return AuthSession(
            sessionId = SessionId(UUID.randomUUID()),
            accountId = accountId,
            authenticatingCredentialId = credential.credentialId,
            issuedAt = now,
            expiresAt = now.plusSeconds(AuthIntegrityBoundary.SESSION_TTL_SECONDS),
            status = SessionStatus.ACTIVE
        ).also(sessions::save)
    }

    /**
     * Honest credential-scoped revocation.
     *
     * This does not claim to delete a passkey from a device, enumerate copies,
     * or revoke a provider-defined sync group. It prevents this credential ID
     * from creating future Hearthline sessions and ends linked existing sessions.
     */
    fun revokeCredential(
        accountId: AccountId,
        credentialId: CredentialId
    ): CredentialRevocationResult {
        val now = clock.instant()

        val revokedCredential = credentials.revoke(accountId, credentialId, now)
        val revokedSessions = sessions.revokeByCredential(accountId, credentialId, now)

        if (revokedCredential) {
            securityHistory.append(
                SecurityHistoryReceipt(
                    accountId = accountId,
                    eventKind = SecurityEventKind.PASSKEY_REVOKED,
                    actionClass = ProtectedActionClass.PASSKEY_REMOVE,
                    outcome = "credential_removed",
                    occurredAt = now,
                    expiresAt = now.plusSeconds(
                        AuthIntegrityBoundary.MEMBER_VISIBLE_SECURITY_EVENT_TTL_SECONDS
                    )
                )
            )
        }

        return CredentialRevocationResult(
            credentialRevoked = revokedCredential,
            linkedSessionsRevoked = revokedSessions,
            memberNotice =
                "This sign-in credential can no longer access Hearthline. " +
                    "If the passkey is saved in a password manager or device, " +
                    "remove it there separately if you no longer want it stored."
        )
    }

    fun signOutEverywhere(accountId: AccountId): Int {
        val now = clock.instant()
        val count = sessions.revokeAllForAccount(accountId, now)

        securityHistory.append(
            SecurityHistoryReceipt(
                accountId = accountId,
                eventKind = SecurityEventKind.ALL_SESSIONS_REVOKED,
                actionClass = null,
                outcome = "all_sessions_ended",
                occurredAt = now,
                expiresAt = now.plusSeconds(
                    AuthIntegrityBoundary.MEMBER_VISIBLE_SECURITY_EVENT_TTL_SECONDS
                )
            )
        )

        return count
    }

    /**
     * Enforces server-authoritative authorization at every sensitive write.
     * A bearer token alone must never override credential or session revocation.
     */
    fun requireActiveSession(sessionId: SessionId): AuthSession {
        val now = clock.instant()
        val session = sessions.find(sessionId)
            ?: throw AuthenticationDenied("Session was not found.")

        if (session.status != SessionStatus.ACTIVE) {
            throw AuthenticationDenied("Session is no longer active.")
        }

        if (now >= session.expiresAt) {
            sessions.revoke(sessionId, now)
            throw AuthenticationDenied("Session has expired.")
        }

        val credential = credentials.find(session.accountId, session.authenticatingCredentialId)
            ?: throw AuthenticationDenied("The authenticating credential is unavailable.")

        if (credential.status != CredentialStatus.ACTIVE) {
            sessions.revoke(sessionId, now)
            throw AuthenticationDenied("The authenticating credential was revoked.")
        }

        return session
    }
}

data class CredentialRevocationResult(
    val credentialRevoked: Boolean,
    val linkedSessionsRevoked: Int,
    val memberNotice: String
)

class AttestationActionIntegrityService(
    private val clock: Clock,
    private val challenges: ChallengeRepository,
    private val authorizations: ActionAuthorizationRepository,
    private val appleVerifier: AppleAppAttestVerifier,
    private val googleVerifier: GooglePlayIntegrityVerifier,
    private val securityHistory: SecurityHistoryRepository
) {
    /**
     * `canonicalActionRequest` must be an action-specific minimal representation.
     * It must not include messages, profile fields, precise location, account IDs,
     * device metadata, or other unnecessary personal data.
     */
    fun issueChallenge(
        accountId: AccountId?,
        actionClass: ProtectedActionClass,
        canonicalActionRequest: ByteArray
    ): IntegrityChallenge {
        val now = clock.instant()
        val challenge = IntegrityChallenge(
            challengeId = ChallengeId(UUID.randomUUID()),
            accountId = accountId,
            actionClass = actionClass,
            challengeBytes = randomChallengeBytes(),
            requestBindingHash = sha256(canonicalActionRequest),
            issuedAt = now,
            expiresAt = now.plusSeconds(AuthIntegrityBoundary.CHALLENGE_TTL_SECONDS)
        )

        challenges.save(challenge)
        return challenge
    }

    /**
     * Verifies one integrity response for one action. Raw evidence is accepted only
     * as a method argument and is discarded when this method returns.
     */
    fun decideAction(
        challengeId: ChallengeId,
        actualCanonicalActionRequest: ByteArray,
        evidence: RawAttestationEvidence
    ): ActionAuthorization {
        val now = clock.instant()
        val originalChallenge = challenges.find(challengeId)
            ?: return routedAuthorization(
                challengeId = challengeId,
                actionClass = ProtectedActionClass.REGISTRATION_FINALIZE,
                now = now,
                route = IntegrityRoute.PASSKEY_ONLY_PATH
            )

        val challenge = challenges.consume(challengeId, now)
            ?: return routedAuthorization(
                challengeId = challengeId,
                actionClass = originalChallenge.actionClass,
                now = now,
                route = IntegrityRoute.PASSKEY_ONLY_PATH
            )

        if (challenge.isExpired(now)) {
            return authorization(
                challenge = challenge,
                now = now,
                decision = AttestationDecision.RETRY_WITH_NEW_CHALLENGE,
                route = IntegrityRoute.PASSKEY_ONLY_PATH
            )
        }

        if (!MessageDigest.isEqual(
                challenge.requestBindingHash,
                sha256(actualCanonicalActionRequest)
            )
        ) {
            return authorization(
                challenge = challenge,
                now = now,
                decision = AttestationDecision.RETRY_WITH_NEW_CHALLENGE,
                route = IntegrityRoute.PASSKEY_ONLY_PATH
            )
        }

        val decision = when (evidence) {
            is RawAttestationEvidence.AppleAppAttest ->
                appleVerifier.verify(challenge.actionClass, challenge, evidence)

            is RawAttestationEvidence.GooglePlayIntegrity ->
                googleVerifier.verify(challenge.actionClass, challenge, evidence)

            is RawAttestationEvidence.Unsupported ->
                AttestationDecision.ROUTE_TO_ACCESSIBLE_ALTERNATIVE
        }

        val route = routeFor(challenge.actionClass, decision)
        val authorization = authorization(challenge, now, decision, route)

        challenge.accountId?.let { accountId ->
            if (decision != AttestationDecision.ALLOW) {
                securityHistory.append(
                    SecurityHistoryReceipt(
                        accountId = accountId,
                        eventKind = SecurityEventKind.SENSITIVE_ACTION_ROUTED,
                        actionClass = challenge.actionClass,
                        outcome = when (decision) {
                            AttestationDecision.ROUTE_TO_ACCESSIBLE_ALTERNATIVE ->
                                "accessible_alternative_available"

                            AttestationDecision.RETRY_WITH_NEW_CHALLENGE ->
                                "new_challenge_needed"

                            AttestationDecision.DENY_THIS_ACTION ->
                                "action_not_completed"

                            AttestationDecision.ALLOW ->
                                "not_applicable"
                        },
                        occurredAt = now,
                        expiresAt = now.plusSeconds(
                            AuthIntegrityBoundary.MEMBER_VISIBLE_SECURITY_EVENT_TTL_SECONDS
                        )
                    )
                )
            }
        }

        return authorization
    }

    fun requireUsableAuthorization(
        authorizationId: ActionAuthorizationId,
        requiredAction: ProtectedActionClass
    ): ActionAuthorization {
        val now = clock.instant()
        val authorization = authorizations.find(authorizationId)
            ?: throw ActionAuthorizationDenied("Action authorization was not found.")

        if (authorization.actionClass != requiredAction) {
            throw ActionAuthorizationDenied("Action authorization is scoped to another action.")
        }

        if (authorization.isExpired(now)) {
            throw ActionAuthorizationDenied("Action authorization expired.")
        }

        return authorization
    }

    private fun authorization(
        challenge: IntegrityChallenge,
        now: Instant,
        decision: AttestationDecision,
        route: IntegrityRoute
    ): ActionAuthorization {
        val authorization = ActionAuthorization(
            authorizationId = ActionAuthorizationId(UUID.randomUUID()),
            actionClass = challenge.actionClass,
            challengeId = challenge.challengeId,
            decision = decision,
            route = route,
            issuedAt = now,
            expiresAt = now.plusSeconds(AuthIntegrityBoundary.ACTION_AUTHORIZATION_TTL_SECONDS)
        )

        authorizations.save(authorization)
        return authorization
    }

    private fun routedAuthorization(
        challengeId: ChallengeId,
        actionClass: ProtectedActionClass,
        now: Instant,
        route: IntegrityRoute
    ): ActionAuthorization {
        val authorization = ActionAuthorization(
            authorizationId = ActionAuthorizationId(UUID.randomUUID()),
            actionClass = actionClass,
            challengeId = challengeId,
            decision = AttestationDecision.ROUTE_TO_ACCESSIBLE_ALTERNATIVE,
            route = route,
            issuedAt = now,
            expiresAt = now.plusSeconds(AuthIntegrityBoundary.ACTION_AUTHORIZATION_TTL_SECONDS)
        )

        authorizations.save(authorization)
        return authorization
    }

    private fun routeFor(
        action: ProtectedActionClass,
        decision: AttestationDecision
    ): IntegrityRoute {
        if (decision == AttestationDecision.ALLOW) {
            return IntegrityRoute.STANDARD_PATH
        }

        return when (action) {
            ProtectedActionClass.REGISTRATION_FINALIZE,
            ProtectedActionClass.PASSKEY_ADD,
            ProtectedActionClass.PASSKEY_REMOVE,
            ProtectedActionClass.SENSITIVE_SETTING_CHANGE ->
                IntegrityRoute.PASSKEY_ONLY_PATH

            ProtectedActionClass.ACCOUNT_EXPORT_START ->
                IntegrityRoute.DELAYED_COMPLETION_PATH

            ProtectedActionClass.HIGH_RATE_INVITATION_SUBMIT,
            ProtectedActionClass.HIGH_RATE_MEDIA_UPLOAD ->
                IntegrityRoute.LOWER_RATE_PATH
        }
    }

    private fun randomChallengeBytes(): ByteArray {
        val seed = UUID.randomUUID().toString().toByteArray()
        return sha256(seed)
    }

    private fun sha256(value: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value)
}

class AuthenticationDenied(message: String) : RuntimeException(message)
class ActionAuthorizationDenied(message: String) : RuntimeException(message)
