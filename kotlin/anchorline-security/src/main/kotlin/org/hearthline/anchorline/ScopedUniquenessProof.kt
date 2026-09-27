package org.hearthline.anchorline

import java.time.Clock
import java.time.Duration
import java.time.Instant

@JvmInline
value class ScopedNullifierHash private constructor(
    private val value: ByteArray
) {
    fun copyBytes(): ByteArray = value.copyOf()

    override fun equals(other: Any?): Boolean =
        other is ScopedNullifierHash && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()

    companion object {
        const val MINIMUM_BYTES: Int = 16

        fun of(bytes: ByteArray): ScopedNullifierHash {
            require(bytes.size >= MINIMUM_BYTES) {
                "Scoped nullifier hash must contain at least 128 bits."
            }
            return ScopedNullifierHash(bytes.copyOf())
        }
    }
}

data class UniquenessScope(
    val relyingPartyId: String,
    val purpose: String,
    val epoch: String
) {
    init {
        require(relyingPartyId == "auth.hearthline.example") {
            "Uniqueness proof must be bound to Hearthline's registered relying party."
        }
        require(purpose == "registration") {
            "Scoped uniqueness is permitted only for registration."
        }
        require(epoch.isNotBlank() && epoch.length <= 64) {
            "Uniqueness epoch must be present and at most 64 characters."
        }
        require(epoch.all { it.isLetterOrDigit() || it in setOf('-', '_', '.') }) {
            "Uniqueness epoch contains unsupported characters."
        }
    }
}

data class ScopedNullifierKey(
    val nullifierHash: ScopedNullifierHash,
    val relyingPartyId: String,
    val purpose: String,
    val epoch: String
) {
    companion object {
        fun from(
            nullifierHash: ScopedNullifierHash,
            scope: UniquenessScope
        ): ScopedNullifierKey =
            ScopedNullifierKey(
                nullifierHash = nullifierHash,
                relyingPartyId = scope.relyingPartyId,
                purpose = scope.purpose,
                epoch = scope.epoch
            )
    }
}

@JvmInline
value class OpaqueUniquenessProof private constructor(
    private val value: ByteArray
) {
    fun copyBytes(): ByteArray = value.copyOf()

    companion object {
        const val MAXIMUM_BYTES: Int = 256 * 1024

        fun of(bytes: ByteArray): OpaqueUniquenessProof {
            require(bytes.isNotEmpty()) {
                "Uniqueness proof cannot be empty."
            }
            require(bytes.size <= MAXIMUM_BYTES) {
                "Uniqueness proof exceeds the permitted size."
            }
            return OpaqueUniquenessProof(bytes.copyOf())
        }
    }
}

enum class ScopedUniquenessVerification {
    UNUSED_FOR_SCOPE_AND_EPOCH,
    ALREADY_USED_FOR_SCOPE_AND_EPOCH,
    UNAVAILABLE,
    INVALID,
    EXPIRED,
    WRONG_SCOPE
}

data class VerifiedScopedUniquenessProof private constructor(
    val result: ScopedUniquenessVerification,
    val nullifierHash: ScopedNullifierHash?
) {
    init {
        when (result) {
            ScopedUniquenessVerification.UNUSED_FOR_SCOPE_AND_EPOCH ->
                requireNotNull(nullifierHash) {
                    "An unused scoped proof must supply a nullifier hash."
                }

            ScopedUniquenessVerification.ALREADY_USED_FOR_SCOPE_AND_EPOCH,
            ScopedUniquenessVerification.UNAVAILABLE,
            ScopedUniquenessVerification.INVALID,
            ScopedUniquenessVerification.EXPIRED,
            ScopedUniquenessVerification.WRONG_SCOPE ->
                require(nullifierHash == null) {
                    "A non-accepted uniqueness result must not disclose a nullifier hash."
                }
        }
    }

    companion object {
        fun unused(nullifierHash: ScopedNullifierHash): VerifiedScopedUniquenessProof =
            VerifiedScopedUniquenessProof(
                result = ScopedUniquenessVerification.UNUSED_FOR_SCOPE_AND_EPOCH,
                nullifierHash = nullifierHash
            )

        fun alreadyUsed(): VerifiedScopedUniquenessProof =
            VerifiedScopedUniquenessProof(
                result = ScopedUniquenessVerification.ALREADY_USED_FOR_SCOPE_AND_EPOCH,
                nullifierHash = null
            )

        fun unavailable(): VerifiedScopedUniquenessProof =
            VerifiedScopedUniquenessProof(
                result = ScopedUniquenessVerification.UNAVAILABLE,
                nullifierHash = null
            )

        fun invalid(): VerifiedScopedUniquenessProof =
            VerifiedScopedUniquenessProof(
                result = ScopedUniquenessVerification.INVALID,
                nullifierHash = null
            )

        fun expired(): VerifiedScopedUniquenessProof =
            VerifiedScopedUniquenessProof(
                result = ScopedUniquenessVerification.EXPIRED,
                nullifierHash = null
            )

        fun wrongScope(): VerifiedScopedUniquenessProof =
            VerifiedScopedUniquenessProof(
                result = ScopedUniquenessVerification.WRONG_SCOPE,
                nullifierHash = null
            )
    }
}

/**
 * A verifier receives only a fresh one-time registration challenge, exact scope,
 * and opaque proof. It must not receive AccountId, credential IDs, passkeys,
 * federation bindings, profile data, network/device data, or behavioral data.
 */
interface AnonymousUniquenessProofVerifier {
    fun verify(
        expectedScope: UniquenessScope,
        expectedChallenge: ByteArray,
        proof: OpaqueUniquenessProof
    ): VerifiedScopedUniquenessProof
}

data class AcceptedScopedNullifier(
    val key: ScopedNullifierKey,
    val acceptedAt: Instant,
    val expiresAt: Instant,
    val policyVersion: String
) {
    init {
        require(expiresAt.isAfter(acceptedAt)) {
            "Scoped nullifier expiry must follow acceptance."
        }
        require(policyVersion.isNotBlank() && policyVersion.length <= 128) {
            "Policy version must be present and bounded."
        }
    }
}

interface ScopedNullifierRepository {
    /**
     * Inserts only when this exact nullifier/scope/epoch key has not been
     * accepted. Implement as a transactional insert with a database unique
     * constraint on:
     *
     * (nullifier_hash, relying_party_id, purpose, epoch)
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
 * Account-blind scoped admission service.
 *
 * This service does not persist proof bytes, holder identity, account identity,
 * credential data, device data, network data, or a member-level uniqueness
 * outcome. It persists only a short-lived scoped nullifier when a proof is
 * accepted.
 */
class ScopedUniquenessAdmissionService(
    private val clock: Clock,
    private val verifier: AnonymousUniquenessProofVerifier,
    private val nullifiers: ScopedNullifierRepository
) {
    companion object {
        private val NULLIFIER_TTL: Duration = Duration.ofDays(30)

        const val POLICY_VERSION: String =
            "scoped-uniqueness-registration-v1"
    }

    fun verifyAndConsume(
        scope: UniquenessScope,
        challenge: ByteArray,
        proof: OpaqueUniquenessProof
    ): ScopedUniquenessRoute {
        require(challenge.size in 16..1024) {
            "Uniqueness challenge must be between 16 and 1024 bytes."
        }

        val now = clock.instant()

        val verified = verifier.verify(
            expectedScope = scope,
            expectedChallenge = challenge.copyOf(),
            proof = proof
        )

        return when (verified.result) {
            ScopedUniquenessVerification.ALREADY_USED_FOR_SCOPE_AND_EPOCH ->
                ScopedUniquenessRoute.DuplicateWithinScope

            ScopedUniquenessVerification.UNUSED_FOR_SCOPE_AND_EPOCH -> {
                val nullifierHash = requireNotNull(verified.nullifierHash)

                val inserted = nullifiers.insertIfUnused(
                    AcceptedScopedNullifier(
                        key = ScopedNullifierKey.from(nullifierHash, scope),
                        acceptedAt = now,
                        expiresAt = now.plus(NULLIFIER_TTL),
                        policyVersion = POLICY_VERSION
                    )
                )

                if (inserted) {
                    ScopedUniquenessRoute.Accepted
                } else {
                    ScopedUniquenessRoute.DuplicateWithinScope
                }
            }

            ScopedUniquenessVerification.UNAVAILABLE,
            ScopedUniquenessVerification.INVALID,
            ScopedUniquenessVerification.EXPIRED,
            ScopedUniquenessVerification.WRONG_SCOPE ->
                ScopedUniquenessRoute.AccessibleAlternative
        }
    }

    fun deleteExpiredNullifiers(): Int =
        nullifiers.deleteExpired(clock.instant())
}
