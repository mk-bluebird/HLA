package org.hearthline.anchorline

import java.time.Clock
import java.time.Instant

/**
 * TTL cleanup for expired security artifacts. The report contains counts by
 * data class only; no account IDs, devices, providers, IPs, or content.
 */
data class CleanupReport(
    val registrationChallengesDeleted: Int,
    val signupChallengesDeleted: Int,
    val signupTicketsDeleted: Int,
    val ageProofChallengesDeleted: Int,
    val recoveryCasesDeleted: Int,
    val recoveryChallengesDeleted: Int,
    val expiredSessionsDeleted: Int,
    val actionAuthorizationsDeleted: Int,
    val scopedNullifiersDeleted: Int
)

class AnchorLineLifecycleCleanupJob(
    private val clock: Clock,
    private val registrationChallenges: RegistrationChallengeRepository,
    private val signupChallenges: SignupChallengeRepository,
    private val signupTickets: SignupTicketRepository,
    private val ageProofChallenges: AgeProofChallengeRepository,
    private val recoveryCases: RecoveryCaseRepository,
    private val recoveryChallenges: RecoveryChallengeRepository,
    private val sessions: CredentialBoundSessionRepository,
    private val actions: ActionAuthorizationRepository,
    private val nullifiers: ScopedNullifierRepository
) {
    fun runOnce(): CleanupReport {
        val now: Instant = clock.instant()

        return CleanupReport(
            registrationChallengesDeleted = registrationChallenges.deleteExpired(now),
            signupChallengesDeleted = signupChallenges.deleteExpired(now),
            signupTicketsDeleted = signupTickets.deleteExpired(now),
            ageProofChallengesDeleted = ageProofChallenges.deleteExpired(now),
            recoveryCasesDeleted = recoveryCases.deleteExpired(now),
            recoveryChallengesDeleted = recoveryChallenges.deleteExpired(now),
            expiredSessionsDeleted = sessions.deleteExpired(now),
            actionAuthorizationsDeleted = actions.deleteExpired(now),
            scopedNullifiersDeleted = nullifiers.deleteExpired(now)
        )
    }
}
