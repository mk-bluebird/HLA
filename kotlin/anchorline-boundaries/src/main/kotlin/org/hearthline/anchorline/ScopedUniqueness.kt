package org.hearthline.anchorline

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class ScopedUniquenessScope(
    val relyingPartyId: String,
    val purpose: String,
    val epochId: EpochId
) {
    init {
        require(relyingPartyId == "auth.hearthline.example")
        require(purpose == "registration")
    }
}

@JvmInline
value class ScopedNullifier private constructor(
    private val bytes: ByteArray
) {
    fun copyBytes(): ByteArray = bytes.copyOf()

    companion object {
        fun of(value: ByteArray): ScopedNullifier {
            require(value.size >= 16)
            return ScopedNullifier(value.copyOf())
        }
    }
}

@JvmInline
value class OpaqueUniquenessProof private constructor(
    private val bytes: ByteArray
) {
    fun copyBytes(): ByteArray = bytes.copyOf()

    companion object {
        fun of(value: ByteArray): OpaqueUniquenessProof {
            require(value.isNotEmpty())
            require(value.size <= 256 * 1024)
            return OpaqueUniquenessProof(value.copyOf())
        }
    }
}

enum class ScopedProofResult {
    UNUSED_FOR_SCOPE,
    ALREADY_USED_FOR_SCOPE,
    UNAVAILABLE,
    INVALID,
    EXPIRED,
    WRONG_SCOPE
}

data class VerifiedScopedProof private constructor(
    val result: ScopedProofResult,
    val nullifier: ScopedNullifier?
) {
    init {
        if (result == ScopedProofResult.UNUSED_FOR_SCOPE) {
            requireNotNull(nullifier)
        } else {
            require(nullifier == null)
        }
    }

    companion object {
        fun unused(nullifier: ScopedNullifier) =
            VerifiedScopedProof(ScopedProofResult.UNUSED_FOR_SCOPE, nullifier)

        fun alreadyUsed() =
            VerifiedScopedProof(ScopedProofResult.ALREADY_USED_FOR_SCOPE, null)

        fun unavailable() =
            VerifiedScopedProof(ScopedProofResult.UNAVAILABLE, null)

        fun invalid() =
            VerifiedScopedProof(ScopedProofResult.INVALID, null)

        fun expired() =
            VerifiedScopedProof(ScopedProofResult.EXPIRED, null)

        fun wrongScope() =
            VerifiedScopedProof(ScopedProofResult.WRONG_SCOPE, null)
    }
}

interface ScopedUniquenessVerifier {
    /**
     * No account, credential, passkey, federation, device, network, profile,
     * behavioral, or location parameter is accepted.
     */
    fun verify(
        scope: ScopedUniquenessScope,
        freshChallenge: ByteArray,
        proof: OpaqueUniquenessProof
    ): VerifiedScopedProof
}

data class ScopedNullifierRecord(
    val epochId: EpochId,
    val purpose: String,
    val encryptedNullifier: ByteArray,
    val acceptedAt: Instant,
    val expiresAt: Instant
)

interface ScopedNullifierRepository {
    /**
     * Requires database-enforced one-time insertion within exact epoch/purpose.
     */
    fun insertIfUnused(value: ScopedNullifierRecord): Boolean

    fun eraseEpoch(epochId: EpochId): Int
}

/**
 * This service is account-blind. It produces only a route for the current
 * registration attempt and cannot write a member-level uniqueness status.
 */
class ScopedUniquenessService(
    private val clock: Clock,
    private val verifier: ScopedUniquenessVerifier,
    private val repository: ScopedNullifierRepository,
    private val encryptNullifier: (ScopedNullifier) -> ByteArray
) {
    companion object {
        private val TTL = Duration.ofDays(30)
    }

    fun verify(
        scope: ScopedUniquenessScope,
        freshChallenge: ByteArray,
        proof: OpaqueUniquenessProof
    ): ScopedUniquenessRoute {
        require(freshChallenge.size in 16..1024)

        val now = clock.instant()
        val verified = verifier.verify(
            scope = scope,
            freshChallenge = freshChallenge.copyOf(),
            proof = proof
        )

        return when (verified.result) {
            ScopedProofResult.ALREADY_USED_FOR_SCOPE ->
                ScopedUniquenessRoute.DuplicateWithinScope

            ScopedProofResult.UNUSED_FOR_SCOPE -> {
                val inserted = repository.insertIfUnused(
                    ScopedNullifierRecord(
                        epochId = scope.epochId,
                        purpose = scope.purpose,
                        encryptedNullifier = encryptNullifier(
                            requireNotNull(verified.nullifier)
                        ),
                        acceptedAt = now,
                        expiresAt = now.plus(TTL)
                    )
                )

                if (inserted) {
                    ScopedUniquenessRoute.Accepted
                } else {
                    ScopedUniquenessRoute.DuplicateWithinScope
                }
            }

            ScopedProofResult.UNAVAILABLE,
            ScopedProofResult.INVALID,
            ScopedProofResult.EXPIRED,
            ScopedProofResult.WRONG_SCOPE ->
                ScopedUniquenessRoute.AccessibleAlternative
        }
    }
}

sealed interface ScopedUniquenessRoute {
    data object Accepted : ScopedUniquenessRoute
    data object DuplicateWithinScope : ScopedUniquenessRoute
    data object AccessibleAlternative : ScopedUniquenessRoute
}
