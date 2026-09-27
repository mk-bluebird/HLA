package org.hearthline.anchorline

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@JvmInline
value class ScopedNullifierHash(val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean =
        other is ScopedNullifierHash && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()
}

data class UniquenessScope(
    val relyingPartyId: String,
    val purpose: String,
    val epoch: String
) {
    init {
        require(relyingPartyId == "auth.hearthline.example") {
            "Uniqueness proofs must be bound to Hearthline's exact relying party."
        }

        require(purpose == "registration") {
            "This reference implementation permits uniqueness proofs only for registration."
        }

        require(epoch.isNotBlank() && epoch.length <= 64) {
            "Uniqueness proof epoch must be present and bounded."
        }
    }
}

/**
 * Raw proof bytes stay inside the verifier call. They are never returned,
 * serialized to product events, logged, or stored in the account model.
 */
@JvmInline
value class OpaqueUniquenessProof(val bytes: ByteArray)

enum class ScopedUniquenessVerification {
    UNUSED_FOR_SCOPE_AND_EPOCH,
    ALREADY_USED_FOR_SCOPE_AND_EPOCH,
    UNAVAILABLE,
    INVALID,
    EXPIRED,
    WRONG_SCOPE
}

interface AnonymousUniquenessProofVerifier {
    fun verify(
        expectedScope: UniquenessScope,
        expectedChallenge: ByteArray,
        proof: OpaqueUniquenessProof
    ): VerifiedScopedUniquenessProof
}

data class VerifiedScopedUniquenessProof(
    val result: ScopedUniquenessVerification,
    val nullifierHash: ScopedNullifierHash?
)

data class AcceptedScopedNullifier(
    val nullifierHash: ScopedNullifierHash,
    val scope: UniquenessScope,
    val acceptedAt: Instant,
    val expiresAt: Instant,
    val policyVersion: String
)

interface ScopedNullifierRepository {
    /**
     * Inserts only if no matching nullifier/scope/epoch already exists.
     * This must be transactionally unique at database level.
     */
    fun insertIfUnused(value: AcceptedScopedNullifier): Boolean

    fun deleteExpired(now: Instant): Int
}

sealed interface ScopedUniquenessRoute {
    data object Accepted : ScopedUniquenessRoute
    data object DuplicateWithinScope : ScopedUniquenessRoute
    data object AccessibleAlternative : ScopedUniquenessRoute
}

/**
 * This service is intentionally account-blind:
 *
 * - It does not accept AccountId.
 * - It does not receive credential IDs, passkeys, federation IDs, profile data,
 *   device data, IP data, or behavioral data.
 * - It does not write any member-level uniqueness status.
 *
 * It merely decides whether a short-lived anonymous admission proof can be
 * consumed for one registration scope/epoch.
 */
class ScopedUniquenessAdmissionService(
    private val clock: Clock,
    private val verifier: AnonymousUniquenessProofVerifier,
    private val nullifiers: ScopedNullifierRepository
) {
    companion object {
        private val NULLIFIER_TTL: Duration = Duration.ofDays(30)
        const val POLICY_VERSION = "scoped-uniqueness-registration-v1"
    }

    fun verifyAndConsume(
        scope: UniquenessScope,
        challenge: ByteArray,
        proof: OpaqueUniquenessProof
    ): ScopedUniquenessRoute {
        val now = clock.instant()

        val verified = verifier.verify(
            expectedScope = scope,
            expectedChallenge = challenge,
            proof = proof
        )

        if (verified.result == ScopedUniquenessVerification.ALREADY_USED_FOR_SCOPE_AND_EPOCH) {
            return ScopedUniquenessRoute.DuplicateWithinScope
        }

        if (verified.result != ScopedUniquenessVerification.UNUSED_FOR_SCOPE_AND_EPOCH) {
            return ScopedUniquenessRoute.AccessibleAlternative
        }

        val nullifier = verified.nullifierHash
            ?: return ScopedUniquenessRoute.AccessibleAlternative

        val inserted = nullifiers.insertIfUnused(
            AcceptedScopedNullifier(
                nullifierHash = nullifier,
                scope = scope,
                acceptedAt = now,
                expiresAt = now.plus(NULLIFIER_TTL),
                policyVersion = POLICY_VERSION
            )
        )

        return if (inserted) {
            ScopedUniquenessRoute.Accepted
        } else {
            ScopedUniquenessRoute.DuplicateWithinScope
        }
    }
}
