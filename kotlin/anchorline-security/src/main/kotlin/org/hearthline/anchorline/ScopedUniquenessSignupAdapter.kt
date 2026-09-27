package org.hearthline.anchorline

import java.time.Clock
import java.time.Instant

interface RegistrationAdmissionChallengeRepository {
    fun findUnused(
        challengeId: ChallengeId,
        now: Instant
    ): RegistrationAdmissionChallenge?

    fun consume(
        challengeId: ChallengeId,
        at: Instant
    ): RegistrationAdmissionChallenge?
}

data class RegistrationAdmissionChallenge(
    val challengeId: ChallengeId,
    val challenge: ByteArray,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val consumedAt: Instant?
) {
    fun isUsable(now: Instant): Boolean =
        consumedAt == null && now.isBefore(expiresAt)
}

/**
 * This adapter consumes a registration challenge only after scoped-proof
 * verification completes. An unavailable proof leaves the caller free to use
 * an accessible non-proof registration path with a separately issued ticket.
 */
class ScopedUniquenessSignupAdapter(
    private val clock: Clock,
    private val challenges: RegistrationAdmissionChallengeRepository,
    private val uniqueness: ScopedUniquenessAdmissionService
) {
    fun verifyForRegistration(
        challengeId: ChallengeId,
        epoch: String,
        proof: OpaqueUniquenessProof
    ): ScopedUniquenessRoute {
        val now = clock.instant()

        val challenge = challenges.findUnused(challengeId, now)
            ?: return ScopedUniquenessRoute.AccessibleAlternative

        if (!challenge.isUsable(now)) {
            return ScopedUniquenessRoute.AccessibleAlternative
        }

        val route = uniqueness.verifyAndConsume(
            scope = UniquenessScope(
                relyingPartyId = "auth.hearthline.example",
                purpose = "registration",
                epoch = epoch
            ),
            challenge = challenge.challenge,
            proof = proof
        )

        if (route == ScopedUniquenessRoute.Accepted) {
            val consumed = challenges.consume(challengeId, now)

            if (consumed == null) {
                return ScopedUniquenessRoute.AccessibleAlternative
            }
        }

        return route
    }
}
