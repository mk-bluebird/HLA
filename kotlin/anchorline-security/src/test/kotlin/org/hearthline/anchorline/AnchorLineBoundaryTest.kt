package org.hearthline.anchorline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class AnchorLineBoundaryTest {
    private val now = Instant.parse("2026-09-27T06:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `GATE-HW-01 public registration uses none attestation`() {
        val service = PublicRegistrationService(
            clock = clock,
            challenges = InMemoryRegistrationChallenges(),
            passkeys = InMemoryPasskeys()
        )

        val options = service.begin(
            accountId = AccountId(UUID.randomUUID()),
            username = "member",
            displayName = "Member"
        )

        assertEquals(AttestationConveyance.NONE, options.attestation)
        assertEquals(UserVerificationRequirement.REQUIRED, options.userVerification)
    }

    @Test
    fun `GATE-HW-03 rejects direct indirect and enterprise attestation`() {
        assertFailsWith<BoundaryViolation> {
            ConsumerRegistrationAttestationPolicy.requireAllowed(
                AttestationConveyance.DIRECT
            )
        }

        assertFailsWith<BoundaryViolation> {
            ConsumerRegistrationAttestationPolicy.requireAllowed(
                AttestationConveyance.INDIRECT
            )
        }

        assertFailsWith<BoundaryViolation> {
            ConsumerRegistrationAttestationPolicy.requireAllowed(
                AttestationConveyance.ENTERPRISE
            )
        }
    }

    @Test
    fun `GATE-HW-05 rejects provenance and device fields`() {
        assertFailsWith<BoundaryViolation> {
            ProhibitedSecurityFieldNames.requireAllowed(
                setOf(
                    "accountId",
                    "credentialId",
                    "aaguid",
                    "deviceModel",
                    "backupState"
                )
            )
        }
    }

    @Test
    fun `GATE-HW-06 rejects protected credential data sent to discovery`() {
        assertFailsWith<BoundaryViolation> {
            ProtectedSecurityDataEgressPolicy.validate(
                BoundaryEvent(
                    name = "credential-registered",
                    classifications = setOf(
                        ProtectedDataClass.PASSKEY_CREDENTIAL,
                        ProtectedDataClass.PASSKEY_PUBLIC_KEY
                    ),
                    destination = ServiceZone.DISCOVERY
                )
            )
        }
    }

    @Test
    fun `INV-FED-04 rejects ordinary provider profile and trust claims`() {
        assertFailsWith<BoundaryViolation> {
            FederationDataMinimization.rejectUnexpectedClaims(
                setOf("sub", "email", "picture", "provider_risk_score", "oid")
            )
        }
    }

    @Test
    fun `INV-AGE-02 rejects hardware and payment as age proof`() {
        assertFailsWith<BoundaryViolation> {
            AgeProofSemanticPolicy.requireAllowed(
                predicateName = "age_over_30",
                proofSources = setOf("hardware_attestation", "payment_instrument")
            )
        }
    }

    @Test
    fun `INV-AGE-02 accepts only narrow age over predicate`() {
        AgeProofSemanticPolicy.requireAllowed(
            predicateName = "age_over_30",
            proofSources = emptySet()
        )
    }

    @Test
    fun `GATE-SIGNUP-05 safety exit actions always bypass pacing`() {
        assertTrue(
            SignupPacingBoundary.requireAlwaysAvailable(
                AlwaysAvailableSafetyAction.WITHDRAW_CONSENT
            )
        )

        assertTrue(
            SignupPacingBoundary.requireAlwaysAvailable(
                AlwaysAvailableSafetyAction.DELETE_ACCOUNT
            )
        )
    }

    @Test
    fun `INV-CONTINUITY-04 rejects all automatic cross-account correlation inputs`() {
        assertFailsWith<BoundaryViolation> {
            CrossAccountLinkingPolicy.reject(
                setOf(
                    "credential_id",
                    "device",
                    "profile",
                    "photo_similarity",
                    "federation_subject",
                    "ip"
                )
            )
        }
    }

    @Test
    fun `GATE-SESSION-04 rejects session state sent to counterpart notifications`() {
        assertFailsWith<BoundaryViolation> {
            ProtectedSecurityDataEgressPolicy.validate(
                BoundaryEvent(
                    name = "session-created",
                    classifications = setOf(
                        ProtectedDataClass.SESSION_BINDING,
                        ProtectedDataClass.SESSION_STATUS
                    ),
                    destination = ServiceZone.COUNTERPART_NOTIFICATION
                )
            )
        }
    }

    private class InMemoryRegistrationChallenges : RegistrationChallengeRepository {
        private val values = mutableMapOf<ChallengeId, RegistrationChallenge>()

        override fun save(challenge: RegistrationChallenge) {
            values[challenge.challengeId] = challenge
        }

        override fun consume(
            challengeId: ChallengeId,
            at: Instant
        ): RegistrationChallenge? {
            val value = values[challengeId] ?: return null
            if (value.consumedAt != null) return null

            val consumed = value.copy(consumedAt = at)
            values[challengeId] = consumed
            return consumed
        }

        override fun deleteExpired(now: Instant): Int {
            val expired = values.values
                .filter { now >= it.expiresAt }
                .map { it.challengeId }

            expired.forEach(values::remove)
            return expired.size
        }
    }

    private class InMemoryPasskeys : PasskeyRepository {
        private val values = mutableMapOf<String, RegisteredPasskey>()

        private fun key(accountId: AccountId, credentialId: CredentialId): String =
            "${accountId.value}:${credentialId.encoded()}"

        override fun save(passkey: RegisteredPasskey) {
            values[key(passkey.accountId, passkey.credentialId)] = passkey
        }

        override fun find(
            accountId: AccountId,
            credentialId: CredentialId
        ): RegisteredPasskey? = values[key(accountId, credentialId)]

        override fun listActive(accountId: AccountId): List<RegisteredPasskey> =
            values.values.filter {
                it.accountId == accountId &&
                    it.status == CredentialStatus.ACTIVE
            }

        override fun revokeAndIncrement(
            accountId: AccountId,
            credentialId: CredentialId,
            revokedAt: Instant
        ): RegisteredPasskey? {
            val key = key(accountId, credentialId)
            val current = values[key] ?: return null

            val updated = current.copy(
                status = CredentialStatus.REVOKED,
                generation = current.generation + 1,
                revokedAt = revokedAt
            )

            values[key] = updated
            return updated
        }
    }
}
