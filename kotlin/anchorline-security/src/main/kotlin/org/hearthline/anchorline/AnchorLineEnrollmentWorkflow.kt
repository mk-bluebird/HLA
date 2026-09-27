package org.hearthline.anchorline

import java.time.Clock

/**
 * Narrow orchestration facade across signup admission, optional scoped
 * uniqueness, passkey registration, AgeGate, and recovery start. Each trust
 * zone keeps its own model; this facade merges none of them and accepts no
 * IP/device/browser/profile/federation-risk inputs.
 */
class AnchorLineEnrollmentWorkflow(
    private val clock: Clock,
    private val signupAdmission: PrivacyPreservingSignupAdmissionService,
    private val tickets: SignupTicketRepository,
    private val registration: PublicRegistrationService,
    private val webAuthnVerifier: WebAuthnVerifierPort,
    private val scopedUniqueness: ScopedUniquenessSignupAdapter,
    private val ageGate: AgeGateService,
    private val recovery: RecoveryService
) {
    fun beginSignup(): SignupAdmissionResult = signupAdmission.begin()

    fun exchangeSignupChallenge(
        challengeId: ChallengeId,
        proofOfWork: ProofOfWorkSolution?
    ): SignupAdmissionResult = signupAdmission.exchange(challengeId, proofOfWork)

    /**
     * Optional only. A failed or unavailable proof routes to the accessible
     * alternative and never changes account visibility or participation.
     */
    fun attemptOptionalScopedUniqueness(
        challengeId: ChallengeId,
        epoch: String,
        proof: OpaqueUniquenessProof
    ): ScopedUniquenessRoute =
        scopedUniqueness.verifyForRegistration(
            challengeId = challengeId,
            epoch = epoch,
            proof = proof
        )

    /**
     * Consumes exactly one passkey-registration capability from the ticket,
     * then begins the registration ceremony. Registration is not a proof of
     * uniqueness or social trust.
     */
    fun beginPasskeyRegistration(
        accountId: AccountId,
        username: String,
        displayName: String,
        ticketId: SignupTicketId
    ): ConsumerRegistrationOptions {
        val consumed = tickets.consumePasskeyRegistration(ticketId, clock.instant())

        if (!consumed) {
            throw SignupCapabilityUnavailable(
                "This registration step is no longer available. Use the " +
                    "accessible alternative or restart registration."
            )
        }

        return registration.begin(
            accountId = accountId,
            username = username,
            displayName = displayName
        )
    }

    /**
     * Completes the age predicate after a verifier-backed presentation. The
     * opaque proof bytes go only to AgeGate and are never stored here.
     */
    fun completeAgeGate(
        accountId: AccountId,
        challengeId: ChallengeId,
        presentation: OpaqueAgeProofPresentation
    ): AgeGateRoute = ageGate.verify(accountId, challengeId, presentation)

    fun beginRecovery(accountId: AccountId): RecoveryChallenge =
        recovery.begin(accountId)
}

class SignupCapabilityUnavailable(message: String) : RuntimeException(message)
