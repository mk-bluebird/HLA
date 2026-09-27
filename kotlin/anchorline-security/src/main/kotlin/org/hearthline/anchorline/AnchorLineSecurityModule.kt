package org.hearthline.anchorline

import java.security.SecureRandom
import java.time.Clock
import javax.sql.DataSource

/**
 * Composition root for AnchorLine security services. All wiring is explicit;
 * no service locators, global singletons, reflection, or hidden configuration.
 * Cryptographic verification stays behind injected external ports.
 */
class AnchorLineSecurityModule(
    dataSource: DataSource,
    clock: Clock,
    secureRandom: SecureRandom,
    webAuthnVerifier: WebAuthnVerifierPort,
    ageProofVerifier: ThirdPartyAgePredicateVerifier,
    uniquenessProofVerifier: AnonymousUniquenessProofVerifier,
    recoveryProofVerifier: IndependentRecoveryProofVerifier,
    recoveryCodeHasher: RecoveryCodeHasher,
    recoveryNotifier: RecoveryNotifier,
    federationSubjectCipher: FederationSubjectCipher
) {
    private val registrationChallenges =
        PostgresRegistrationChallengeRepository(dataSource)
    private val passkeys = PostgresPasskeyRepository(dataSource)
    private val signupChallenges = PostgresSignupChallengeRepository(dataSource)
    private val signupTickets = PostgresSignupTicketRepository(dataSource)
    private val admissionChallenges =
        PostgresRegistrationAdmissionChallengeRepository(dataSource)
    private val accountSecurityStates =
        PostgresAccountSecurityStateRepository(dataSource)
    private val credentialSecurityStates =
        PostgresCredentialSecurityStateRepository(dataSource)
    private val sessions = PostgresCredentialBoundSessionRepository(dataSource)
    private val actions = PostgresActionAuthorizationRepository(dataSource)
    private val ageProofChallenges = PostgresAgeProofChallengeRepository(dataSource)
    private val ageGrants = PostgresAgeEligibilityGrantRepository(dataSource)
    private val recoveryCases = PostgresRecoveryCaseRepository(dataSource)
    private val recoveryChallenges = PostgresRecoveryChallengeRepository(dataSource)
    private val recoveryCodes = PostgresRecoveryCodeRepository(dataSource, secureRandom)
    private val federationBindings =
        PostgresFederatedLoginBindingRepository(dataSource, federationSubjectCipher)
    private val nullifiers = PostgresScopedNullifierRepository(dataSource)

    val publicRegistrationService = PublicRegistrationService(
        clock = clock,
        challenges = registrationChallenges,
        passkeys = passkeys
    )

    /**
     * Global capacity and proof-of-work difficulty are external operational
     * inputs derived from aggregate load only. Defaults keep signup open at
     * zero difficulty; deployments may replace this port.
     */
    var globalSignupCapacity: GlobalSignupCapacity = object : GlobalSignupCapacity {
        override fun currentMode(): SignupCapacityMode = SignupCapacityMode.OPEN
        override fun retryAfterSeconds(): Long = 0L
        override fun proofOfWorkDifficulty(): Int = 0
    }
        set(value) {
            field = value
            signupAdmissionService = buildSignupAdmissionService(value)
        }

    private fun buildSignupAdmissionService(
        capacity: GlobalSignupCapacity
    ): PrivacyPreservingSignupAdmissionService =
        PrivacyPreservingSignupAdmissionService(
            clock = clock,
            capacity = capacity,
            challenges = signupChallenges,
            tickets = signupTickets,
            proofOfWork = Sha256ProofOfWorkVerifier()
        )

    var signupAdmissionService: PrivacyPreservingSignupAdmissionService =
        buildSignupAdmissionService(globalSignupCapacity)
        private set

    val scopedUniquenessAdmissionService = ScopedUniquenessAdmissionService(
        clock = clock,
        verifier = uniquenessProofVerifier,
        nullifiers = nullifiers
    )

    val scopedUniquenessSignupAdapter = ScopedUniquenessSignupAdapter(
        clock = clock,
        challenges = admissionChallenges,
        uniqueness = scopedUniquenessAdmissionService
    )

    val ageGateService = AgeGateService(
        clock = clock,
        challenges = ageProofChallenges,
        grants = ageGrants,
        verifier = ageProofVerifier
    )

    val credentialBoundSessionService = CredentialBoundSessionService(
        clock = clock,
        accounts = accountSecurityStates,
        credentials = credentialSecurityStates,
        sessions = sessions,
        actions = actions
    )

    val recoveryService = RecoveryService(
        clock = clock,
        random = secureRandom,
        recoveryCases = recoveryCases,
        challenges = recoveryChallenges,
        recoveryCodes = recoveryCodes,
        recoveryCodeHasher = recoveryCodeHasher,
        proofVerifier = recoveryProofVerifier,
        credentialReplacement = RecoveryCredentialReplacementAdapter(
            PostgresRecoveryCredentialWriteAdapter(passkeys)
        ),
        sessions = RecoverySessionAdapter(sessions),
        notifier = recoveryNotifier
    )

    val optionalFederationService = OptionalFederationService(
        clock = clock,
        bindings = federationBindings,
        cipher = federationSubjectCipher
    )

    val lifecycleCleanupJob = AnchorLineLifecycleCleanupJob(
        clock = clock,
        registrationChallenges = registrationChallenges,
        signupChallenges = signupChallenges,
        signupTickets = signupTickets,
        ageProofChallenges = ageProofChallenges,
        recoveryCases = recoveryCases,
        recoveryChallenges = recoveryChallenges,
        sessions = sessions,
        actions = actions,
        nullifiers = nullifiers
    )

    val enrollmentWorkflow = AnchorLineEnrollmentWorkflow(
        clock = clock,
        signupAdmission = signupAdmissionService,
        tickets = signupTickets,
        registration = publicRegistrationService,
        webAuthnVerifier = webAuthnVerifier,
        scopedUniqueness = scopedUniquenessSignupAdapter,
        ageGate = ageGateService,
        recovery = recoveryService
    )

    /**
     * Passes every protected-classification event through the egress policy.
     * Only AuthenticationBoundary, AgeGate, RecoveryBoundary, SecurityHistory,
     * and AppealBoundary are accepted destinations.
     */
    fun emitProtectedEvent(event: BoundaryEvent) {
        ProtectedSecurityDataEgressPolicy.validate(event)
    }
}
