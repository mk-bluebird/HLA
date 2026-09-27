package org.hearthline.anchorline

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class CredentialPurpose {
    PRIMARY_ACCESS,
    INDEPENDENT_BACKUP,
    ACCESSIBILITY_OPTION,
    REPLACEMENT_AFTER_LOSS
}

/**
 * Optional labels are authored by the member; no device-derived labels are
 * generated. These labels remain inside the security UI and cannot egress to
 * social-product systems.
 */
data class AccountCredential(
    val accountId: AccountId,
    val credentialId: CredentialId,
    val publicKeyCose: ByteArray,
    val createdAt: Instant,
    val status: CredentialStatus,
    val generation: Long,
    val purpose: CredentialPurpose,
    val memberProvidedLabel: String?,
    val revokedAt: Instant? = null
)

interface AccountCredentialRepository {
    fun find(
        accountId: AccountId,
        credentialId: CredentialId
    ): AccountCredential?

    fun save(value: AccountCredential)

    fun listActive(accountId: AccountId): List<AccountCredential>

    fun revoke(
        accountId: AccountId,
        credentialId: CredentialId,
        at: Instant
    ): Boolean
}

interface ExistingCredentialFreshAuthorizationVerifier {
    fun verify(
        accountId: AccountId,
        credentialId: CredentialId,
        challenge: ByteArray,
        assertion: ByteArray
    ): Boolean
}

class MultiPasskeyContinuityService(
    private val clock: Clock,
    private val credentials: AccountCredentialRepository,
    private val authorizer: ExistingCredentialFreshAuthorizationVerifier
) {
    /**
     * INV-CONTINUITY-01:
     *
     * An active credential authorizes adding a new credential to the same local
     * account. This establishes local authorization continuity only.
     */
    fun addCredential(
        accountId: AccountId,
        authorizingCredentialId: CredentialId,
        freshChallenge: ByteArray,
        freshAssertion: ByteArray,
        newCredentialId: CredentialId,
        newPublicKeyCose: ByteArray,
        purpose: CredentialPurpose,
        memberProvidedLabel: String?
    ): AccountCredential {
        require(memberProvidedLabel == null || memberProvidedLabel.length <= 64) {
            "Credential label exceeds 64 characters."
        }

        val authorized = authorizer.verify(
            accountId = accountId,
            credentialId = authorizingCredentialId,
            challenge = freshChallenge,
            assertion = freshAssertion
        )

        if (!authorized) {
            throw AuthorizationDenied(
                "A fresh assertion from an existing active credential is required."
            )
        }

        if (credentials.find(accountId, newCredentialId) != null) {
            throw AuthorizationDenied("Credential is already registered to this account.")
        }

        return AccountCredential(
            accountId = accountId,
            credentialId = newCredentialId,
            publicKeyCose = newPublicKeyCose.copyOf(),
            createdAt = clock.instant(),
            status = CredentialStatus.ACTIVE,
            generation = 1L,
            purpose = purpose,
            memberProvidedLabel = memberProvidedLabel
        ).also(credentials::save)
    }

    fun removeCredential(
        accountId: AccountId,
        credentialId: CredentialId
    ): Boolean {
        val active = credentials.listActive(accountId)

        if (
            active.size <= 1 &&
            active.any { it.credentialId == credentialId }
        ) {
            throw AuthorizationDenied(
                "A final passkey cannot be removed until another sign-in or " +
                    "recovery method has been established."
            )
        }

        return credentials.revoke(accountId, credentialId, clock.instant())
    }
}

/**
 * INV-CONTINUITY-05:
 *
 * Consolidation is never automatic. The request contains only the two account
 * IDs chosen by the member after separate fresh authentication to both accounts.
 * There are no similarity inputs.
 */
data class AccountConsolidationRequest(
    val requestId: UUID,
    val sourceAccountId: AccountId,
    val destinationAccountId: AccountId,
    val sourceFreshAuthorizationAt: Instant,
    val destinationFreshAuthorizationAt: Instant,
    val createdAt: Instant,
    val cancellationDeadline: Instant,
    val status: ConsolidationStatus
)

enum class ConsolidationStatus {
    PENDING_CONFIRMATION,
    CANCELLED,
    COMPLETED,
    EXPIRED
}

interface AccountConsolidationRepository {
    fun save(request: AccountConsolidationRequest)
    fun find(requestId: UUID): AccountConsolidationRequest?
    fun update(
        requestId: UUID,
        transform: (AccountConsolidationRequest) -> AccountConsolidationRequest
    ): AccountConsolidationRequest?
}

interface AccountConsolidationExecutor {
    /**
     * Executes only the member-approved transfer plan. Production code should
     * receive a typed plan of explicitly selected categories, not all account
     * data by default.
     */
    fun execute(
        sourceAccountId: AccountId,
        destinationAccountId: AccountId
    )
}

class MemberInitiatedAccountConsolidationService(
    private val clock: Clock,
    private val repository: AccountConsolidationRepository,
    private val executor: AccountConsolidationExecutor,
    private val sessions: RecoverySessionInvalidator
) {
    companion object {
        private val CANCELLATION_WINDOW: Duration = Duration.ofHours(24)
    }

    fun beginAfterDoubleFreshAuthentication(
        sourceAccountId: AccountId,
        destinationAccountId: AccountId,
        sourceFreshAuthorizationAt: Instant,
        destinationFreshAuthorizationAt: Instant
    ): AccountConsolidationRequest {
        require(sourceAccountId != destinationAccountId) {
            "Source and destination accounts must differ."
        }

        val now = clock.instant()

        return AccountConsolidationRequest(
            requestId = UUID.randomUUID(),
            sourceAccountId = sourceAccountId,
            destinationAccountId = destinationAccountId,
            sourceFreshAuthorizationAt = sourceFreshAuthorizationAt,
            destinationFreshAuthorizationAt = destinationFreshAuthorizationAt,
            createdAt = now,
            cancellationDeadline = now.plus(CANCELLATION_WINDOW),
            status = ConsolidationStatus.PENDING_CONFIRMATION
        ).also(repository::save)
    }

    fun cancel(requestId: UUID): AccountConsolidationRequest {
        val request = repository.find(requestId)
            ?: throw AuthorizationDenied("Consolidation request was not found.")

        if (request.status != ConsolidationStatus.PENDING_CONFIRMATION) {
            throw AuthorizationDenied("Consolidation cannot be cancelled now.")
        }

        return requireNotNull(
            repository.update(requestId) {
                it.copy(status = ConsolidationStatus.CANCELLED)
            }
        )
    }

    fun completeAfterCoolingOff(requestId: UUID): AccountConsolidationRequest {
        val now = clock.instant()
        val request = repository.find(requestId)
            ?: throw AuthorizationDenied("Consolidation request was not found.")

        if (request.status != ConsolidationStatus.PENDING_CONFIRMATION) {
            throw AuthorizationDenied("Consolidation is not pending.")
        }

        if (now < request.cancellationDeadline) {
            throw AuthorizationDenied("Consolidation cooling-off period is active.")
        }

        executor.execute(
            sourceAccountId = request.sourceAccountId,
            destinationAccountId = request.destinationAccountId
        )

        /*
         * Sessions are ended on both accounts after a state-changing
         * consolidation, requiring fresh sign-in.
         */
        sessions.revokeAllSessions(request.sourceAccountId, now)
        sessions.revokeAllSessions(request.destinationAccountId, now)

        return requireNotNull(
            repository.update(requestId) {
                it.copy(status = ConsolidationStatus.COMPLETED)
            }
        )
    }
}

/**
 * INV-CONTINUITY-03 and INV-CONTINUITY-04:
 *
 * There is deliberately no cross-account lookup API. This policy object exists
 * to make prohibited matching inputs explicit at design and test time.
 */
object CrossAccountLinkingPolicy {
    private val prohibitedInputs = setOf(
        "credential_id",
        "public_key",
        "credential_timing",
        "backup_state",
        "credential_manager",
        "hardware_property",
        "attestation_property",
        "session_binding",
        "federation_subject",
        "email",
        "phone",
        "device",
        "network",
        "ip",
        "profile",
        "photo_similarity",
        "location",
        "relationship",
        "behavior"
    )

    fun reject(inputs: Set<String>) {
        val violations = inputs.intersect(prohibitedInputs)

        if (violations.isNotEmpty()) {
            throw BoundaryViolation(
                "Cross-account linking inputs are prohibited: " +
                    violations.sorted().joinToString()
            )
        }
    }
}
