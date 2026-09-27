package org.hearthline.anchorline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * Workflow-level tests using in-memory ports. Fixed clocks control TTLs;
 * mutable clocks simulate time passing where needed.
 */
class AnchorLineWorkflowIntegrationTest {
    private val start = Instant.parse("2026-09-27T10:00:00Z")

    private class MutableClock(startInstant: Instant) : Clock() {
        private var current: Instant = startInstant

        override fun getZone(): ZoneOffset = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId): Clock = this

        override fun instant(): Instant = current

        fun advance(amount: Duration) {
            current = current.plus(amount)
        }
    }

    // 1. Public registration configuration

    @Test
    fun `registration options always use none attestation`() {
        val fixture = registrationFixture()
        val options = fixture.workflow.beginPasskeyRegistration(
            accountId = AccountId(UUID.randomUUID()),
            username = "member",
            displayName = "Member",
            ticketId = fixture.ticketId
        )

        assertEquals(AttestationConveyance.NONE, options.attestation)
    }

    @Test
    fun `direct indirect and enterprise attestation attempts fail`() {
        for (attempt in listOf(
            AttestationConveyance.DIRECT,
            AttestationConveyance.INDIRECT,
            AttestationConveyance.ENTERPRISE
        )) {
            assertFailsWith<BoundaryViolation> {
                ConsumerRegistrationOptions(
                    challenge = ByteArray(32),
                    rpId = "auth.hearthline.example",
                    rpName = "Hearthline",
                    accountId = AccountId(UUID.randomUUID()),
                    username = "member",
                    displayName = "Member",
                    timeoutMillis = 60_000,
                    residentKey = ResidentKeyRequirement.REQUIRED,
                    userVerification = UserVerificationRequirement.REQUIRED,
                    attestation = attempt,
                    excludedCredentialIds = emptyList()
                )
            }
        }
    }

    // 2. No provenance persistence

    @Test
    fun `durable passkey model has no provenance fields`() {
        val prohibited = setOf(
            "aaguid", "attestation", "device", "platform", "backup",
            "provider", "os", "browser", "manufacturer", "model", "enclave",
            "tpm", "strongbox", "hardware"
        )

        val fieldNames = RegisteredPasskey::class.java.declaredFields
            .map { it.name.lowercase() }

        assertTrue(fieldNames.isNotEmpty())
        assertTrue(
            fieldNames.none { name -> prohibited.any { marker -> name.contains(marker) } },
            "RegisteredPasskey must not carry provenance fields: $fieldNames"
        )

        val declaredProperties = RegisteredPasskey::class.members
            .map { it.name.lowercase() }
        assertTrue(
            declaredProperties.none { name -> prohibited.any { marker -> name.contains(marker) } }
        )
    }

    // 3. Protected-data egress

    @Test
    fun `protected classifications are rejected for every prohibited destination`() {
        val prohibitedDestinations = listOf(
            ServiceZone.PROFILE,
            ServiceZone.DISCOVERY,
            ServiceZone.MATCHING,
            ServiceZone.RANKING,
            ServiceZone.RECOMMENDATION,
            ServiceZone.MESSAGING,
            ServiceZone.COUNTERPART_NOTIFICATION,
            ServiceZone.ANALYTICS,
            ServiceZone.EXPERIMENTATION,
            ServiceZone.MODERATION_PRIORITY,
            ServiceZone.TRUST_SCORING,
            ServiceZone.SAFETY_SCORING
        )

        val classifications = setOf(
            ProtectedDataClass.PASSKEY_CREDENTIAL,
            ProtectedDataClass.SESSION_BINDING,
            ProtectedDataClass.RECOVERY_STATE,
            ProtectedDataClass.AGE_PROOF,
            ProtectedDataClass.FEDERATION_SUBJECT
        )

        for (destination in prohibitedDestinations) {
            for (classification in classifications) {
                assertFailsWith<BoundaryViolation> {
                    ProtectedSecurityDataEgressPolicy.validate(
                        BoundaryEvent(
                            name = "protected-event",
                            classifications = setOf(classification),
                            destination = destination
                        )
                    )
                }
            }
        }

        // Nullifier material is likewise protected.
        for (destination in prohibitedDestinations) {
            assertFailsWith<BoundaryViolation> {
                ProtectedSecurityDataEgressPolicy.validate(
                    BoundaryEvent(
                        name = "nullifier-accepted",
                        classifications = setOf(
                            ProtectedDataClass.PASSKEY_CREDENTIAL,
                            ProtectedDataClass.SESSION_STATUS
                        ),
                        destination = destination
                    )
                )
            }
        }

        // Allowed boundaries accept them.
        for (destination in listOf(
            ServiceZone.AUTHENTICATION_BOUNDARY,
            ServiceZone.AGE_GATE,
            ServiceZone.RECOVERY_BOUNDARY,
            ServiceZone.SECURITY_HISTORY,
            ServiceZone.APPEAL_BOUNDARY
        )) {
            ProtectedSecurityDataEgressPolicy.validate(
                BoundaryEvent(
                    name = "protected-event",
                    classifications = classifications,
                    destination = destination
                )
            )
        }
    }

    // 4. Federation isolation

    @Test
    fun `federation binding is account-local and never merges accounts`() {
        val clock = Clock.fixed(start, ZoneOffset.UTC)
        val bindings = InMemoryFederatedLoginBindingRepository()
        val cipher = TestFederationSubjectCipher()
        val service = OptionalFederationService(clock, bindings, cipher)

        val accountA = AccountId(UUID.randomUUID())
        val accountB = AccountId(UUID.randomUUID())
        val configuration = OidcClientConfiguration(
            provider = FederatedProvider.APPLE,
            issuerUri = "https://appleid.apple.com",
            clientId = "hearthline-client",
            redirectUri = "https://auth.hearthline.example/oidc/callback",
            scopes = setOf("openid"),
            environment = DeploymentEnvironment.PRODUCTION
        )
        val subject = VerifiedOidcSubject(configuration.issuerUri, byteArrayOf(1, 2, 3))

        val binding = service.bindToExistingAccount(accountA, configuration, subject)
        assertEquals(accountA, binding.accountId)

        // Same issuer+subject for a different local account is refused, not merged.
        assertFailsWith<AuthorizationDenied> {
            service.bindToExistingAccount(accountB, configuration, subject)
        }
        assertEquals(1, bindings.count())
        assertEquals(accountA, bindings.findByIssuerAndPairwiseSubject(
            configuration.issuerUri, subject.pairwiseSubject.copyOf(), cipher
        )?.accountId)

        // Re-binding to the same explicit account is allowed (idempotent convenience).
        val second = service.bindToExistingAccount(accountA, configuration, subject)
        assertEquals(accountA, second.accountId)
    }

    @Test
    fun `federation claim collection rejects email picture oid tokens and account age`() {
        assertFailsWith<BoundaryViolation> {
            FederationDataMinimization.rejectUnexpectedClaims(
                setOf("email", "picture", "oid", "access_token", "provider_account_age")
            )
        }

        FederationDataMinimization.rejectUnexpectedClaims(setOf("sub"))
    }

    // 5. AgeGate minimization

    @Test
    fun `agegate accepts only the age over 30 predicate semantic`() {
        AgeProofSemanticPolicy.requireAllowed("age_over_30", emptySet())

        assertFailsWith<IllegalArgumentException> {
            AgeProofSemanticPolicy.requireAllowed("age_over_21", emptySet())
        }

        for (source in listOf(
            "hardware_attestation", "payment_instrument", "passkey",
            "provider_trust", "provider_account_age"
        )) {
            assertFailsWith<BoundaryViolation> {
                AgeProofSemanticPolicy.requireAllowed("age_over_30", setOf(source))
            }
        }
    }

    @Test
    fun `agegate unavailable invalid expired and nonce mismatch route to accessible review`() {
        for (result in listOf(
            AgePredicateResult.UNAVAILABLE,
            AgePredicateResult.INVALID,
            AgePredicateResult.EXPIRED,
            AgePredicateResult.NONCE_MISMATCH
        )) {
            val clock = MutableClock(start)
            val grants = InMemoryAgeEligibilityGrantRepository()
            val service = AgeGateService(
                clock = clock,
                challenges = InMemoryAgeProofChallenges(),
                grants = grants,
                verifier = object : ThirdPartyAgePredicateVerifier {
                    override fun verify(
                        expectedAudience: String,
                        expectedNonce: ByteArray,
                        rawPresentation: OpaqueAgeProofPresentation
                    ): AgePredicateResult = result
                }
            )

            val accountId = AccountId(UUID.randomUUID())
            val challenge = service.begin(accountId)

            val route = service.verify(
                accountId,
                challenge.challengeId,
                OpaqueAgeProofPresentation(byteArrayOf(9))
            )

            assertEquals(AgeGateRoute.AccessibleReview, route)
            assertEquals(AgeEligibilityState.REVIEW_REQUESTED, grants.find(accountId)?.state)
        }
    }

    @Test
    fun `product-visible age grant exposes only state expiry and policy version`() {
        val clock = MutableClock(start)
        val grants = InMemoryAgeEligibilityGrantRepository()
        val service = AgeGateService(
            clock = clock,
            challenges = InMemoryAgeProofChallenges(),
            grants = grants,
            verifier = object : ThirdPartyAgePredicateVerifier {
                override fun verify(
                    expectedAudience: String,
                    expectedNonce: ByteArray,
                    rawPresentation: OpaqueAgeProofPresentation
                ): AgePredicateResult = AgePredicateResult.AGE_OVER_30
            }
        )

        val accountId = AccountId(UUID.randomUUID())
        val challenge = service.begin(accountId)
        assertEquals(
            AgeGateRoute.Eligible,
            service.verify(accountId, challenge.challengeId, OpaqueAgeProofPresentation(byteArrayOf(1)))
        )

        val grant = requireNotNull(grants.find(accountId))
        val exposedPropertyNames = AgeEligibilityGrant::class.members
            .filter { it.name != "\$stable_mock_for_inline_class_init_check" }
            .map { it.name.lowercase() }

        val prohibited = setOf(
            "dob", "dateofbirth", "age", "agerange", "document", "name",
            "email", "credential", "device", "proof", "subject", "payment"
        )
        assertTrue(
            exposedPropertyNames.none { property ->
                prohibited.any { marker -> property.contains(marker) }
            },
            "Age grant projection leaked: $exposedPropertyNames"
        )
        assertEquals(AgeEligibilityState.ELIGIBLE, grant.state)
        assertEquals(AgeGateService.POLICY_VERSION, grant.policyVersion)
    }

    // 6. Signup replay resistance

    @Test
    fun `consumed signup challenge cannot produce a second ticket`() {
        val clock = MutableClock(start)
        val capacity = FixedSignupCapacity(SignupCapacityMode.OPEN, difficulty = 0)
        val challenges = InMemorySignupChallenges()
        val tickets = InMemorySignupTickets()
        val service = PrivacyPreservingSignupAdmissionService(
            clock, capacity, challenges, tickets, Sha256ProofOfWorkVerifier()
        )

        val issued = service.begin() as SignupAdmissionResult.ChallengeIssued
        val first = service.exchange(issued.challengeId, null)
        assertTrue(first is SignupAdmissionResult.TicketIssued)

        val replay = service.exchange(issued.challengeId, null)
        assertTrue(replay is SignupAdmissionResult.Rejected)
    }

    @Test
    fun `consumed ticket capability cannot be reused`() {
        val tickets = InMemorySignupTickets()
        val ticket = SignupTicket(
            ticketId = SignupTicketId(UUID.randomUUID()),
            issuedAt = start,
            expiresAt = start.plus(Duration.ofMinutes(10)),
            passkeyRegistrationRemaining = 1,
            ageProofRemaining = 1,
            accountShellRemaining = 1
        )
        tickets.save(ticket)

        assertTrue(tickets.consumePasskeyRegistration(ticket.ticketId, start))
        assertTrue(!tickets.consumePasskeyRegistration(ticket.ticketId, start))

        // Expired tickets never yield capabilities.
        assertTrue(
            !tickets.consumeAgeProof(
                ticket.ticketId,
                start.plus(Duration.ofMinutes(11))
            )
        )
    }

    @Test
    fun `pow difficulty is read only from global capacity`() {
        val clock = MutableClock(start)
        val capacity = FixedSignupCapacity(SignupCapacityMode.ELEVATED_LOAD, difficulty = 12)
        val challenges = InMemorySignupChallenges()
        val service = PrivacyPreservingSignupAdmissionService(
            clock, capacity, challenges, InMemorySignupTickets(), Sha256ProofOfWorkVerifier()
        )

        val issued = service.begin() as SignupAdmissionResult.ChallengeIssued
        assertEquals(12, issued.requiredDifficulty)
        assertEquals(12, challenges.stored(issued.challengeId)?.globalProofOfWorkDifficulty)

        // Missing proof of work routes to the accessible alternative, not rejection.
        val alt = service.exchange(issued.challengeId, null)
        assertTrue(alt is SignupAdmissionResult.AccessibleAlternativeAvailable)
    }

    @Test
    fun `safety and exit actions remain permitted regardless of signup pacing`() {
        for (action in AlwaysAvailableSafetyAction.entries) {
            assertTrue(SignupPacingBoundary.requireAlwaysAvailable(action))
        }
    }

    // 7. Scoped uniqueness

    @Test
    fun `scoped uniqueness accepts first proof and flags duplicates within scope`() {
        val clock = MutableClock(start)
        val nullifiers = InMemoryScopedNullifierRepository()
        val verifier = ScriptedUniquenessVerifier(mutableListOf(
            VerifiedScopedUniquenessProof.unused(
                ScopedNullifierHash.of(ByteArray(32) { it.toByte() })
            ),
            VerifiedScopedUniquenessProof.alreadyUsed()
        ))
        val service = ScopedUniquenessAdmissionService(clock, verifier, nullifiers)
        val scope = UniquenessScope("auth.hearthline.example", "registration", "2026-q3")

        assertEquals(
            ScopedUniquenessRoute.Accepted,
            service.verifyAndConsume(scope, ByteArray(32) { 1 }, proof())
        )
        assertEquals(
            ScopedUniquenessRoute.DuplicateWithinScope,
            service.verifyAndConsume(scope, ByteArray(32) { 1 }, proof())
        )
        assertEquals(1, nullifiers.size())
    }

    @Test
    fun `invalid unavailable expired and wrong scope proofs route to accessible alternative`() {
        for (failure in listOf(
            VerifiedScopedUniquenessProof.unavailable(),
            VerifiedScopedUniquenessProof.invalid(),
            VerifiedScopedUniquenessProof.expired(),
            VerifiedScopedUniquenessProof.wrongScope()
        )) {
            val service = ScopedUniquenessAdmissionService(
                Clock.fixed(start, ZoneOffset.UTC),
                ScriptedUniquenessVerifier(mutableListOf(failure)),
                InMemoryScopedNullifierRepository()
            )

            assertEquals(
                ScopedUniquenessRoute.AccessibleAlternative,
                service.verifyAndConsume(
                    UniquenessScope("auth.hearthline.example", "registration", "2026-q3"),
                    ByteArray(32) { 1 },
                    proof()
                )
            )
        }
    }

    @Test
    fun `scoped uniqueness interface accepts no account id`() {
        val parameterTypes = AnonymousUniquenessProofVerifier::class.java
            .methods
            .flatMap { method -> method.parameterTypes.toList() }

        assertTrue(
            parameterTypes.none { it == AccountId::class.java },
            "Scoped uniqueness verifier must remain account-blind"
        )

        val admissionParams = ScopedUniquenessAdmissionService::class
            .java.constructors.first().parameterTypes.toList()
        assertTrue(AccountId::class.java !in admissionParams)
    }

    @Test
    fun `nullifier material cannot egress to prohibited social destinations`() {
        assertFailsWith<BoundaryViolation> {
            ProtectedSecurityDataEgressPolicy.validate(
                BoundaryEvent(
                    name = "scoped-nullifier-recorded",
                    classifications = setOf(ProtectedDataClass.PASSKEY_CREDENTIAL),
                    destination = ServiceZone.MATCHING
                )
            )
        }
    }

    // 8. Recovery

    @Test
    fun `independent passkey recovery enters hold without session or credential changes`() {
        val fixture = recoveryFixture()
        val accountId = AccountId(UUID.randomUUID())
        val challenge = fixture.service.begin(accountId)

        val authorized = fixture.service.authorize(
            challenge.recoveryId,
            RecoveryAttempt.IndependentPasskey(byteArrayOf(1))
        )

        assertEquals(RecoveryState.HOLD, authorized.state)
        assertEquals(0, fixture.sessions.revocationCalls)
        assertEquals(0, fixture.replacements.addCalls)
    }

    @Test
    fun `offline recovery code is one-time and revokes remaining codes`() {
        val clock = MutableClock(start)
        val codes = InMemoryRecoveryCodeStore()
        val hasher = TestShaRecoveryCodeHasher()
        val storeBacked = InMemoryRecoveryCodesWithStore(codes, hasher)

        val service = recoveryFixture(codes = storeBacked).service

        val accountId = AccountId(UUID.randomUUID())
        val first = service.generateOfflineRecoveryCode(accountId)
        val second = service.generateOfflineRecoveryCode(accountId)

        assertEquals(2, storeBacked.findActive(accountId).size)

        val accepted = storeBacked.consumeOneAndRevokeRest(
            accountId, first.toCharArray(), hasher, clock.instant()
        )
        assertTrue(accepted)
        assertEquals(0, storeBacked.findActive(accountId).size)

        // Replay of the same code fails after consumption.
        assertTrue(
            !storeBacked.consumeOneAndRevokeRest(
                accountId, first.toCharArray(), hasher, clock.instant()
            )
        )
        // The unused sibling code was revoked with the used one.
        assertTrue(
            !storeBacked.consumeOneAndRevokeRest(
                accountId, second.toCharArray(), hasher, clock.instant()
            )
        )
    }

    @Test
    fun `recovery cannot complete before hold expiry`() {
        val fixture = recoveryFixture()
        val accountId = AccountId(UUID.randomUUID())
        val challenge = fixture.service.begin(accountId)

        fixture.service.authorize(
            challenge.recoveryId,
            RecoveryAttempt.IndependentPasskey(byteArrayOf(1))
        )

        assertFailsWith<RecoveryDenied> {
            fixture.service.completeAfterHold(
                challenge.recoveryId,
                ReplacementPasskeyRegistration(
                    CredentialId(byteArrayOf(2)), byteArrayOf(3)
                )
            )
        }
        assertEquals(0, fixture.sessions.revocationCalls)
    }

    @Test
    fun `cancellation during hold works through active credential path`() {
        val fixture = recoveryFixture()
        val accountId = AccountId(UUID.randomUUID())
        val challenge = fixture.service.begin(accountId)

        fixture.service.authorize(
            challenge.recoveryId,
            RecoveryAttempt.IndependentPasskey(byteArrayOf(1))
        )

        val cancelled = fixture.service.cancelWithActiveCredential(accountId, challenge.recoveryId)
        assertEquals(RecoveryState.CANCELLED, cancelled.state)

        // A completed transition after cancellation is impossible.
        assertFailsWith<RecoveryDenied> {
            fixture.service.completeAfterHold(
                challenge.recoveryId,
                ReplacementPasskeyRegistration(CredentialId(byteArrayOf(2)), byteArrayOf(3))
            )
        }
    }

    @Test
    fun `recovery completion adds replacement credential and revokes all sessions`() {
        val clock = MutableClock(start)
        val fixture = recoveryFixture(clock = clock)
        val accountId = AccountId(UUID.randomUUID())
        val challenge = fixture.service.begin(accountId)

        fixture.service.authorize(
            challenge.recoveryId,
            RecoveryAttempt.IndependentPasskey(byteArrayOf(1))
        )

        clock.advance(Duration.ofHours(25))

        val completed = fixture.service.completeAfterHold(
            challenge.recoveryId,
            ReplacementPasskeyRegistration(CredentialId(byteArrayOf(2)), byteArrayOf(3))
        )

        assertEquals(RecoveryState.COMPLETED, completed.state)
        assertEquals(1, fixture.replacements.addCalls)
        assertEquals(1, fixture.sessions.revocationCalls)
    }

    @Test
    fun `non-proof facts cannot satisfy the recovery proof interface`() {
        val methodNames = IndependentRecoveryProofVerifier::class.java.methods
            .map { it.name }
        assertTrue(
            methodNames.all {
                it.startsWith("verifyIndependentRegisteredPasskey") ||
                    it.startsWith("verifyPreestablishedFederatedBinding")
            }
        )

        val parameterTypes = IndependentRecoveryProofVerifier::class.java.methods
            .flatMap { it.parameterTypes.toList() }

        // Email/SMS/profile facts/messages/photos/location/device data/staff
        // recognition have no representable parameter type on this interface.
        for (forbidden in listOf(
            "email", "sms", "phone", "profile", "message", "photo",
            "location", "device", "ip", "staff"
        )) {
            assertTrue(
                parameterTypes.none {
                    it.simpleName.lowercase().contains(forbidden) ||
                        it.name.lowercase().contains(forbidden)
                },
                "Recovery proof interface must not accept $forbidden inputs"
            )
        }

        // AgeGate results are also not a recovery proof input.
        assertTrue(
            parameterTypes.none { it == AgeGateRoute::class.java || it == AgeEligibilityGrant::class.java }
        )
    }

    // 9. Credential-bound sessions

    @Test
    fun `revoking a credential invalidates bound sessions`() {
        val clock = MutableClock(start)
        val fixture = sessionFixture(clock)

        val session = fixture.sessions.issueAfterVerifiedPasskey(
            fixture.accountId, fixture.credentialId
        )

        val revoked = fixture.service.revokeCredential(fixture.accountId, fixture.credentialId)
        assertTrue(revoked >= 1)

        assertFailsWith<AuthorizationDenied> {
            fixture.sessions.requireActiveSession(session.sessionId)
        }
    }

    @Test
    fun `credential generation mismatch denies authorization before expiry`() {
        val clock = MutableClock(start)
        val fixture = sessionFixture(clock)

        val session = fixture.sessions.issueAfterVerifiedPasskey(
            fixture.accountId, fixture.credentialId
        )

        // Bump credential generation without touching session expiry.
        fixture.credentialStates.bumpGeneration(fixture.accountId, fixture.credentialId)

        assertFailsWith<AuthorizationDenied> {
            fixture.sessions.requireActiveSession(session.sessionId)
        }
    }

    @Test
    fun `fresh step-up rotates session id before sensitive authorization`() {
        val clock = MutableClock(start)
        val fixture = sessionFixture(clock)

        val prior = fixture.sessions.issueAfterVerifiedPasskey(
            fixture.accountId, fixture.credentialId
        )

        val rotated = fixture.sessions.rotateAfterFreshPasskey(
            prior.sessionId, fixture.credentialId
        )

        assertTrue(prior.sessionId != rotated.sessionId)
        assertFailsWith<AuthorizationDenied> {
            fixture.sessions.requireActiveSession(prior.sessionId)
        }

        val authorization = fixture.sessions.beginSensitiveAction(
            rotated.sessionId, SensitiveAction.ADD_PASSKEY
        )

        // Action authorizations are single-use and expire.
        fixture.sessions.consumeSensitiveAction(
            rotated.sessionId, authorization.authorizationId, SensitiveAction.ADD_PASSKEY
        )
        assertFailsWith<AuthorizationDenied> {
            fixture.sessions.consumeSensitiveAction(
                rotated.sessionId, authorization.authorizationId, SensitiveAction.ADD_PASSKEY
            )
        }

        clock.advance(Duration.ofMinutes(6))
        val late = fixture.sessions.beginSensitiveAction(
            rotated.sessionId, SensitiveAction.REMOVE_PASSKEY
        )
        clock.advance(Duration.ofMinutes(10))
        assertFailsWith<AuthorizationDenied> {
            fixture.sessions.consumeSensitiveAction(
                rotated.sessionId, late.authorizationId, SensitiveAction.REMOVE_PASSKEY
            )
        }
    }

    @Test
    fun `session state cannot egress to product or social destinations`() {
        assertFailsWith<BoundaryViolation> {
            ProtectedSecurityDataEgressPolicy.validate(
                BoundaryEvent(
                    name = "session-state",
                    classifications = setOf(
                        ProtectedDataClass.SESSION_BINDING,
                        ProtectedDataClass.SESSION_STATUS
                    ),
                    destination = ServiceZone.PROFILE
                )
            )
        }
    }

    // 10. Multi-passkey continuity

    @Test
    fun `adding a credential requires fresh assertion by an existing active credential`() {
        val clock = MutableClock(start)
        val credentials = InMemoryAccountCredentials()
        val accountId = AccountId(UUID.randomUUID())
        val existing = CredentialId(byteArrayOf(10))
        val incoming = CredentialId(byteArrayOf(11))

        credentials.save(
            AccountCredential(
                accountId = accountId,
                credentialId = existing,
                publicKeyCose = byteArrayOf(1),
                createdAt = clock.instant(),
                status = CredentialStatus.ACTIVE,
                generation = 1L,
                purpose = CredentialPurpose.PRIMARY_ACCESS,
                memberProvidedLabel = null
            )
        )

        val service = MultiPasskeyContinuityService(
            clock = clock,
            credentials = credentials,
            authorizer = object : ExistingCredentialFreshAuthorizationVerifier {
                override fun verify(
                    accountId: AccountId,
                    credentialId: CredentialId,
                    challenge: ByteArray,
                    assertion: ByteArray
                ): Boolean = credentialId == existing && assertion.contentEquals(CHALLENGE_OK)
            }
        )

        // The new credential cannot authorize itself.
        assertFailsWith<AuthorizationDenied> {
            service.addCredential(
                accountId = accountId,
                authorizingCredentialId = incoming,
                freshChallenge = ByteArray(32),
                freshAssertion = CHALLENGE_OK,
                newCredentialId = incoming,
                newPublicKeyCose = byteArrayOf(2),
                purpose = CredentialPurpose.INDEPENDENT_BACKUP,
                memberProvidedLabel = null
            )
        }

        val added = service.addCredential(
            accountId = accountId,
            authorizingCredentialId = existing,
            freshChallenge = ByteArray(32),
            freshAssertion = CHALLENGE_OK,
            newCredentialId = incoming,
            newPublicKeyCose = byteArrayOf(2),
            purpose = CredentialPurpose.INDEPENDENT_BACKUP,
            memberProvidedLabel = "Backup key"
        )
        assertEquals(incoming, added.credentialId)
        assertEquals(2, credentials.listActive(accountId).size)
    }

    @Test
    fun `cross-account linking policy rejects every correlation input`() {
        assertFailsWith<BoundaryViolation> {
            CrossAccountLinkingPolicy.reject(
                setOf(
                    "credential_id", "public_key", "federation_subject", "device",
                    "network", "profile", "photo_similarity", "location",
                    "relationship", "behavior"
                )
            )
        }

        // Only non-correlation inputs pass; nothing here concludes human/device identity.
        CrossAccountLinkingPolicy.reject(emptySet())
    }

    // Fixtures and in-memory ports

    private companion object {
        val CHALLENGE_OK = byteArrayOf(0x7f, 0x7f)
    }

    private fun proof(): OpaqueUniquenessProof =
        OpaqueUniquenessProof.of(ByteArray(64) { it.toByte() })

    private class FixedSignupCapacity(
        private val mode: SignupCapacityMode,
        private val difficulty: Int
    ) : GlobalSignupCapacity {
        override fun currentMode(): SignupCapacityMode = mode
        override fun retryAfterSeconds(): Long = 60
        override fun proofOfWorkDifficulty(): Int = difficulty
    }

    private class ScriptedUniquenessVerifier(
        private val script: MutableList<VerifiedScopedUniquenessProof>
    ) : AnonymousUniquenessProofVerifier {
        override fun verify(
            expectedScope: UniquenessScope,
            expectedChallenge: ByteArray,
            proof: OpaqueUniquenessProof
        ): VerifiedScopedUniquenessProof {
            assertTrue(script.isNotEmpty(), "Verifier script exhausted")
            return script.removeAt(0)
        }
    }

    private class TestFederationSubjectCipher : FederationSubjectCipher {
        private val plaintextByBinding = mutableMapOf<String, ByteArray>()

        override fun encrypt(plaintext: ByteArray): EncryptedValue =
            EncryptedValue(
                ciphertext = plaintext.map { (it.toInt() xor 0x5b).toByte() }.toByteArray(),
                keyVersion = 1
            )

        override fun decrypt(value: EncryptedValue): ByteArray =
            value.ciphertext.map { (it.toInt() xor 0x5b).toByte() }.toByteArray()
    }

    private class InMemoryFederatedLoginBindingRepository :
        FederatedLoginBindingRepository {
        private val values = mutableListOf<FederatedLoginBinding>()

        override fun save(binding: FederatedLoginBinding) {
            values.removeAll {
                it.issuerUri == binding.issuerUri &&
                    it.accountId == binding.accountId &&
                    it.status == FederationBindingStatus.ACTIVE &&
                    it.encryptedPairwiseSubject.ciphertext.contentEquals(
                        binding.encryptedPairwiseSubject.ciphertext
                    )
            }
            values += binding
        }

        override fun findByIssuerAndPairwiseSubject(
            issuerUri: String,
            subject: ByteArray,
            cipher: FederationSubjectCipher
        ): FederatedLoginBinding? =
            values.firstOrNull {
                it.issuerUri == issuerUri &&
                    it.status == FederationBindingStatus.ACTIVE &&
                    cipher.decrypt(it.encryptedPairwiseSubject).contentEquals(subject)
            }

        override fun findForAccount(
            accountId: AccountId,
            bindingId: FederationBindingId
        ): FederatedLoginBinding? =
            values.firstOrNull {
                it.accountId == accountId && it.bindingId == bindingId
            }

        override fun disable(
            accountId: AccountId,
            bindingId: FederationBindingId,
            at: Instant
        ): Boolean {
            val index = values.indexOfFirst {
                it.accountId == accountId &&
                    it.bindingId == bindingId &&
                    it.status == FederationBindingStatus.ACTIVE
            }
            if (index < 0) return false
            values[index] = values[index].copy(status = FederationBindingStatus.DISABLED)
            return true
        }

        fun count(): Int = values.size
    }

    private class InMemoryAgeProofChallenges : AgeProofChallengeRepository {
        private val values = mutableMapOf<ChallengeId, AgeProofChallenge>()

        override fun save(challenge: AgeProofChallenge) {
            values[challenge.challengeId] = challenge
        }

        override fun consume(challengeId: ChallengeId, at: Instant): AgeProofChallenge? {
            val current = values[challengeId] ?: return null
            if (current.consumedAt != null || at >= current.expiresAt) return null
            val consumed = current.copy(consumedAt = at)
            values[challengeId] = consumed
            return consumed
        }

        override fun deleteExpired(now: Instant): Int {
            val expired = values.values.filter { now >= it.expiresAt }.map { it.challengeId }
            expired.forEach(values::remove)
            return expired.size
        }
    }

    private class InMemoryAgeEligibilityGrantRepository : AgeEligibilityGrantRepository {
        private val values = mutableMapOf<AccountId, AgeEligibilityGrant>()

        override fun save(grant: AgeEligibilityGrant) {
            values[grant.accountId] = grant
        }

        override fun find(accountId: AccountId): AgeEligibilityGrant? = values[accountId]
    }

    private class InMemorySignupChallenges : SignupChallengeRepository {
        private val values = mutableMapOf<ChallengeId, SignupChallenge>()

        override fun save(challenge: SignupChallenge) {
            values[challenge.challengeId] = challenge
        }

        fun stored(challengeId: ChallengeId): SignupChallenge? = values[challengeId]

        override fun consume(challengeId: ChallengeId, at: Instant): SignupChallenge? {
            val current = values[challengeId] ?: return null
            if (current.consumedAt != null || at >= current.expiresAt) return null
            val consumed = current.copy(consumedAt = at)
            values[challengeId] = consumed
            return consumed
        }

        override fun deleteExpired(now: Instant): Int {
            val expired = values.values.filter { now >= it.expiresAt }.map { it.challengeId }
            expired.forEach(values::remove)
            return expired.size
        }
    }

    private class InMemorySignupTickets : SignupTicketRepository {
        private val values = mutableMapOf<SignupTicketId, SignupTicket>()

        override fun save(ticket: SignupTicket) {
            values[ticket.ticketId] = ticket
        }

        override fun deleteExpired(now: Instant): Int {
            val expired = values.values.filter { now >= it.expiresAt }.map { it.ticketId }
            expired.forEach(values::remove)
            return expired.size
        }

        override fun consumePasskeyRegistration(
            ticketId: SignupTicketId,
            at: Instant
        ): Boolean = consumeCapability(ticketId, at) { it.passkeyRegistrationRemaining } {
            it.copy(passkeyRegistrationRemaining = it.passkeyRegistrationRemaining - 1)
        }

        override fun consumeAgeProof(ticketId: SignupTicketId, at: Instant): Boolean =
            consumeCapability(ticketId, at, { it.ageProofRemaining }, {
                it.copy(ageProofRemaining = it.ageProofRemaining - 1)
            })

        override fun consumeAccountShell(ticketId: SignupTicketId, at: Instant): Boolean =
            consumeCapability(ticketId, at, { it.accountShellRemaining }, {
                it.copy(accountShellRemaining = it.accountShellRemaining - 1)
            })

        private fun consumeCapability(
            ticketId: SignupTicketId,
            at: Instant,
            remaining: (SignupTicket) -> Int,
            decrement: (SignupTicket) -> SignupTicket
        ): Boolean {
            val current = values[ticketId] ?: return false
            if (at >= current.expiresAt || remaining(current) <= 0) return false
            values[ticketId] = decrement(current)
            return true
        }
    }

    private class RegistrationFixture(
        val workflow: AnchorLineEnrollmentWorkflow,
        val ticketId: SignupTicketId
    )

    private fun registrationFixture(): RegistrationFixture {
        val clock = MutableClock(start)
        val tickets = InMemorySignupTickets()
        val ticket = SignupTicket(
            ticketId = SignupTicketId(UUID.randomUUID()),
            issuedAt = start,
            expiresAt = start.plus(Duration.ofMinutes(10)),
            passkeyRegistrationRemaining = 5,
            ageProofRemaining = 5,
            accountShellRemaining = 5
        )
        tickets.save(ticket)

        val workflow = AnchorLineEnrollmentWorkflow(
            clock = clock,
            signupAdmission = PrivacyPreservingSignupAdmissionService(
                clock,
                FixedSignupCapacity(SignupCapacityMode.OPEN, 0),
                InMemorySignupChallenges(),
                tickets,
                Sha256ProofOfWorkVerifier()
            ),
            tickets = tickets,
            registration = PublicRegistrationService(
                clock, InMemoryRegistrationChallenges(), InMemoryPasskeys()
            ),
            webAuthnVerifier = unusedWebAuthnVerifier(),
            scopedUniqueness = ScopedUniquenessSignupAdapter(
                clock,
                InMemoryAdmissionChallenges(),
                ScopedUniquenessAdmissionService(
                    clock, ScriptedUniquenessVerifier(mutableListOf()), InMemoryScopedNullifierRepository()
                )
            ),
            ageGate = AgeGateService(
                clock,
                InMemoryAgeProofChallenges(),
                InMemoryAgeEligibilityGrantRepository(),
                object : ThirdPartyAgePredicateVerifier {
                    override fun verify(
                        expectedAudience: String,
                        expectedNonce: ByteArray,
                        rawPresentation: OpaqueAgeProofPresentation
                    ) = AgePredicateResult.UNAVAILABLE
                }
            ),
            recovery = recoveryFixture(clock = clock).service
        )

        return RegistrationFixture(workflow, ticket.ticketId)
    }

    private fun unusedWebAuthnVerifier(): WebAuthnVerifierPort =
        object : WebAuthnVerifierPort {
            override fun verifyRegistration(
                expectedChallenge: ByteArray,
                clientDataJson: String,
                attestationObject: ByteArray
            ): VerifiedPublicRegistration =
                throw UnsupportedOperationException("Not used in workflow tests")

            override fun verifyAssertion(
                accountId: AccountId,
                credentialId: CredentialId,
                expectedChallenge: ByteArray,
                clientDataJson: String,
                authenticatorData: ByteArray,
                signature: ByteArray,
                userPresent: Boolean,
                userVerified: Boolean
            ): Boolean = false
        }

    private class RecoveryFixture(
        val service: RecoveryService,
        val sessions: RecordingSessionInvalidator,
        val replacements: RecordingCredentialReplacement
    )

    private fun recoveryFixture(
        clock: MutableClock = MutableClock(start),
        codes: RecoveryCodeRepository = InMemoryNoOpRecoveryCodes()
    ): RecoveryFixture {
        val sessions = RecordingSessionInvalidator()
        val replacements = RecordingCredentialReplacement()

        val service = RecoveryService(
            clock = clock,
            random = SecureRandom(),
            recoveryCases = InMemoryRecoveryCaseStore(),
            challenges = InMemoryRecoveryChallengeStore(),
            recoveryCodes = codes,
            recoveryCodeHasher = TestShaRecoveryCodeHasher(),
            proofVerifier = object : IndependentRecoveryProofVerifier {
                override fun verifyIndependentRegisteredPasskey(
                    accountId: AccountId,
                    challenge: ByteArray,
                    assertion: ByteArray
                ): Boolean = assertion.contentEquals(byteArrayOf(1))

                override fun verifyPreestablishedFederatedBinding(
                    accountId: AccountId,
                    authorizationCode: String,
                    pkceVerifier: String
                ): Boolean = false
            },
            credentialReplacement = replacements,
            sessions = sessions,
            notifier = InMemoryRecoveryNotifier()
        )

        return RecoveryFixture(service, sessions, replacements)
    }

    private class SessionFixture(
        val accountId: AccountId,
        val credentialId: CredentialId,
        val sessions: CredentialBoundSessionService,
        val credentialStates: InMemoryCredentialSecurityStates
    )

    private fun sessionFixture(clock: MutableClock): SessionFixture {
        val accountId = AccountId(UUID.randomUUID())
        val credentialId = CredentialId(byteArrayOf(42, 43))

        val accounts = InMemoryAccountSecurityStates(accountId)
        val credentialStates = InMemoryCredentialSecurityStates(accountId, credentialId)
        val sessions = InMemorySessions()
        val actions = InMemoryActionAuthorizations()

        val service = CredentialBoundSessionService(
            clock, accounts, credentialStates, sessions, actions
        )

        return SessionFixture(accountId, credentialId, service, credentialStates)
    }

    private class InMemoryRegistrationChallenges : RegistrationChallengeRepository {
        private val values = mutableMapOf<ChallengeId, RegistrationChallenge>()

        override fun save(challenge: RegistrationChallenge) {
            values[challenge.challengeId] = challenge
        }

        override fun consume(challengeId: ChallengeId, at: Instant): RegistrationChallenge? {
            val current = values[challengeId] ?: return null
            if (current.consumedAt != null || at >= current.expiresAt) return null
            val consumed = current.copy(consumedAt = at)
            values[challengeId] = consumed
            return consumed
        }

        override fun deleteExpired(now: Instant): Int {
            val expired = values.values.filter { now >= it.expiresAt }.map { it.challengeId }
            expired.forEach(values::remove)
            return expired.size
        }
    }

    private class InMemoryPasskeys : PasskeyRepository {
        private val values = mutableMapOf<Pair<AccountId, CredentialId>, RegisteredPasskey>()

        override fun save(passkey: RegisteredPasskey) {
            values[passkey.accountId to passkey.credentialId] = passkey
        }

        override fun find(
            accountId: AccountId,
            credentialId: CredentialId
        ): RegisteredPasskey? = values[accountId to credentialId]

        override fun listActive(accountId: AccountId): List<RegisteredPasskey> =
            values.values.filter {
                it.accountId == accountId && it.status == CredentialStatus.ACTIVE
            }

        override fun revokeAndIncrement(
            accountId: AccountId,
            credentialId: CredentialId,
            revokedAt: Instant
        ): RegisteredPasskey? {
            val current = values[accountId to credentialId] ?: return null
            val updated = current.copy(
                status = CredentialStatus.REVOKED,
                generation = current.generation + 1,
                revokedAt = revokedAt
            )
            values[accountId to credentialId] = updated
            return updated
        }
    }

    private class InMemoryAdmissionChallenges :
        RegistrationAdmissionChallengeRepository {
        private val values = mutableMapOf<ChallengeId, RegistrationAdmissionChallenge>()

        fun save(challenge: RegistrationAdmissionChallenge) {
            values[challenge.challengeId] = challenge
        }

        override fun findUnused(
            challengeId: ChallengeId,
            now: Instant
        ): RegistrationAdmissionChallenge? =
            values[challengeId]?.takeIf { it.consumedAt == null }

        override fun consume(
            challengeId: ChallengeId,
            at: Instant
        ): RegistrationAdmissionChallenge? {
            val current = values[challengeId] ?: return null
            if (!current.isUsable(at)) return null
            val consumed = current.copy(consumedAt = at)
            values[challengeId] = consumed
            return consumed
        }
    }

    private class InMemoryAccountCredentials : AccountCredentialRepository {
        private val values = mutableMapOf<Pair<AccountId, CredentialId>, AccountCredential>()

        override fun find(
            accountId: AccountId,
            credentialId: CredentialId
        ): AccountCredential? = values[accountId to credentialId]

        override fun save(value: AccountCredential) {
            values[value.accountId to value.credentialId] = value
        }

        override fun listActive(accountId: AccountId): List<AccountCredential> =
            values.values.filter {
                it.accountId == accountId && it.status == CredentialStatus.ACTIVE
            }

        override fun revoke(
            accountId: AccountId,
            credentialId: CredentialId,
            at: Instant
        ): Boolean {
            val current = values[accountId to credentialId] ?: return false
            values[accountId to credentialId] = current.copy(
                status = CredentialStatus.REVOKED,
                revokedAt = at
            )
            return true
        }
    }

    private class InMemoryAccountSecurityStates(accountId: AccountId) :
        AccountSecurityStateRepository {
        private var state = AccountSecurityState(
            accountId = accountId,
            status = AccountStatus.ACTIVE,
            sessionGeneration = 1,
            updatedAt = start
        )

        override fun find(id: AccountId): AccountSecurityState? =
            if (id == state.accountId) state else null
    }

    private class InMemoryCredentialSecurityStates(
        private val accountId: AccountId,
        private val credentialId: CredentialId
    ) : CredentialSecurityStateRepository {
        private var state = CredentialSecurityState(
            accountId = accountId,
            credentialId = credentialId,
            status = CredentialStatus.ACTIVE,
            generation = 1,
            updatedAt = start
        )

        fun bumpGeneration(accountId: AccountId, credentialId: CredentialId) {
            state = state.copy(generation = state.generation + 1, updatedAt = start.plusSeconds(1))
        }

        override fun find(
            id: AccountId,
            cred: CredentialId
        ): CredentialSecurityState? =
            if (id == accountId && cred == credentialId) state else null

        override fun revokeAndIncrement(
            id: AccountId,
            cred: CredentialId,
            at: Instant
        ): CredentialSecurityState? {
            if (id != accountId || cred != credentialId) return null
            state = state.copy(
                status = CredentialStatus.REVOKED,
                generation = state.generation + 1,
                updatedAt = at
            )
            return state
        }
    }

    private class InMemorySessions : CredentialBoundSessionRepository {
        private val values = mutableMapOf<SessionId, CredentialBoundSession>()

        override fun save(session: CredentialBoundSession) {
            values[session.sessionId] = session
        }

        override fun find(sessionId: SessionId): CredentialBoundSession? = values[sessionId]

        override fun revoke(sessionId: SessionId, at: Instant): Boolean {
            val current = values[sessionId] ?: return false
            if (current.status != SessionStatus.ACTIVE) return false
            values[sessionId] = current.copy(status = SessionStatus.REVOKED, revokedAt = at)
            return true
        }

        override fun expire(sessionId: SessionId, at: Instant): Boolean {
            val current = values[sessionId] ?: return false
            if (current.status != SessionStatus.ACTIVE) return false
            values[sessionId] = current.copy(status = SessionStatus.EXPIRED)
            return true
        }

        override fun revokeByCredential(
            accountId: AccountId,
            credentialId: CredentialId,
            at: Instant
        ): Int {
            var count = 0
            values.entries.forEach { (id, session) ->
                if (
                    session.accountId == accountId &&
                    session.authenticatingCredentialId == credentialId &&
                    session.status == SessionStatus.ACTIVE
                ) {
                    values[id] = session.copy(status = SessionStatus.REVOKED, revokedAt = at)
                    count += 1
                }
            }
            return count
        }

        override fun revokeAllForAccount(accountId: AccountId, at: Instant): Int {
            var count = 0
            values.entries.forEach { (id, session) ->
                if (session.accountId == accountId && session.status == SessionStatus.ACTIVE) {
                    values[id] = session.copy(status = SessionStatus.REVOKED, revokedAt = at)
                    count += 1
                }
            }
            return count
        }

        override fun deleteExpired(now: Instant): Int {
            val expired = values.values.filter { now >= it.expiresAt }.map { it.sessionId }
            expired.forEach(values::remove)
            return expired.size
        }
    }

    private class InMemoryActionAuthorizations : ActionAuthorizationRepository {
        private val values = mutableMapOf<ActionAuthorizationId, ActionAuthorization>()

        override fun save(value: ActionAuthorization) {
            values[value.authorizationId] = value
        }

        override fun consume(
            authorizationId: ActionAuthorizationId,
            at: Instant
        ): ActionAuthorization? {
            val current = values[authorizationId] ?: return null
            if (current.consumedAt != null || at >= current.expiresAt) return null
            val consumed = current.copy(consumedAt = at)
            values[authorizationId] = consumed
            return consumed
        }

        override fun deleteExpired(now: Instant): Int {
            val expired = values.values.filter { now >= it.expiresAt }
                .map { it.authorizationId }
            expired.forEach(values::remove)
            return expired.size
        }
    }

    private class InMemoryRecoveryCaseStore : RecoveryCaseRepository {
        private val values = mutableMapOf<RecoveryId, RecoveryCase>()

        override fun save(value: RecoveryCase) {
            values[value.recoveryId] = value
        }

        override fun find(recoveryId: RecoveryId): RecoveryCase? = values[recoveryId]

        override fun update(
            recoveryId: RecoveryId,
            transform: (RecoveryCase) -> RecoveryCase
        ): RecoveryCase? {
            val current = values[recoveryId] ?: return null
            val updated = transform(current)
            values[recoveryId] = updated
            return updated
        }

        override fun deleteExpired(now: Instant): Int {
            val expired = values.values.filter { now >= it.expiresAt }.map { it.recoveryId }
            expired.forEach(values::remove)
            return expired.size
        }
    }

    private class InMemoryRecoveryChallengeStore : RecoveryChallengeRepository {
        private val values = mutableMapOf<RecoveryId, RecoveryChallenge>()

        override fun save(value: RecoveryChallenge) {
            values[value.recoveryId] = value
        }

        override fun consume(recoveryId: RecoveryId, at: Instant): RecoveryChallenge? {
            val current = values[recoveryId] ?: return null
            if (current.consumedAt != null) return null
            val consumed = current.copy(consumedAt = at)
            values[recoveryId] = consumed
            return consumed
        }

        override fun deleteExpired(now: Instant): Int {
            val expired = values.values.filter { now >= it.expiresAt }.map { it.recoveryId }
            expired.forEach(values::remove)
            return expired.size
        }
    }

    private class InMemoryNoOpRecoveryCodes : RecoveryCodeRepository {
        override fun save(verifier: RecoveryCodeVerifier) = Unit
        override fun findActive(accountId: AccountId): List<RecoveryCodeVerifier> = emptyList()
        override fun consumeOneAndRevokeRest(
            accountId: AccountId,
            code: CharArray,
            hasher: RecoveryCodeHasher,
            at: Instant
        ): Boolean = false

        override fun revokeAll(accountId: AccountId, at: Instant): Int = 0
    }

    private class TestShaRecoveryCodeHasher : RecoveryCodeHasher {
        override fun hash(code: CharArray, salt: ByteArray): ByteArray =
            CryptoSupport.sha256(code.concatToString().toByteArray() + salt)

        override fun verify(
            code: CharArray,
            salt: ByteArray,
            verifier: ByteArray
        ): Boolean = hash(code, salt).contentEquals(verifier)
    }

    /**
     * Models the transactional repository contract: mark one used, revoke rest,
     * atomic under a single lock.
     */
    private class InMemoryRecoveryCodeStore {
        val rows = mutableListOf<RecoveryCodeVerifier>()
        private var locked = false

        fun synchronized(block: () -> Unit) {
            // Single-threaded tests; flag documents the intended row lock.
            assertTrue(!locked)
            locked = true
            try {
                block()
            } finally {
                locked = false
            }
        }
    }

    private class InMemoryRecoveryCodesWithStore(
        private val store: InMemoryRecoveryCodeStore,
        private val hasher: RecoveryCodeHasher
    ) : RecoveryCodeRepository {
        override fun save(verifier: RecoveryCodeVerifier) {
            store.rows += verifier
        }

        override fun findActive(accountId: AccountId): List<RecoveryCodeVerifier> =
            store.rows.filter {
                it.accountId == accountId && it.usedAt == null && it.revokedAt == null
            }

        override fun consumeOneAndRevokeRest(
            accountId: AccountId,
            code: CharArray,
            hasher: RecoveryCodeHasher,
            at: Instant
        ): Boolean {
            var consumed = false
            store.synchronized {
                val active = findActive(accountId)
                val winner = active.firstOrNull {
                    hasher.verify(code, it.salt, it.verifier)
                }
                if (winner != null) {
                    store.rows.replaceAll { row ->
                        when {
                            row === winner -> row.copy(usedAt = at)
                            row.accountId == accountId &&
                                row.usedAt == null && row.revokedAt == null ->
                                row.copy(revokedAt = at)
                            else -> row
                        }
                    }
                    consumed = true
                }
            }
            return consumed
        }

        override fun revokeAll(accountId: AccountId, at: Instant): Int {
            var count = 0
            store.synchronized {
                store.rows.replaceAll { row ->
                    if (row.accountId == accountId && row.usedAt == null && row.revokedAt == null) {
                        count += 1
                        row.copy(revokedAt = at)
                    } else {
                        row
                    }
                }
            }
            return count
        }
    }

    private class RecordingSessionInvalidator : RecoverySessionInvalidator {
        var revocationCalls: Int = 0

        override fun revokeAllSessions(accountId: AccountId, at: Instant) {
            revocationCalls += 1
        }
    }

    private class RecordingCredentialReplacement :
        RecoveryCredentialReplacementService {
        var addCalls: Int = 0

        override fun addReplacementCredential(
            accountId: AccountId,
            recoveryId: RecoveryId,
            registration: ReplacementPasskeyRegistration
        ) {
            addCalls += 1
        }
    }

    private class InMemoryRecoveryNotifier : RecoveryNotifier {
        override fun recoveryStarted(accountId: AccountId, recoveryId: RecoveryId) = Unit
        override fun recoveryAuthorized(accountId: AccountId, recoveryId: RecoveryId) = Unit
        override fun recoveryCancelled(accountId: AccountId, recoveryId: RecoveryId) = Unit
        override fun recoveryCompleted(accountId: AccountId, recoveryId: RecoveryId) = Unit
    }
}
