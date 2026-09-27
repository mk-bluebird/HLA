package org.hearthline.anchorline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.Instant
import java.util.UUID

class BoundaryControlsTest {
    @Test
    fun `behavioral biometric and score fields are rejected`() {
        assertFailsWith<BoundaryViolation> {
            ProhibitedFields.requireAllowed(
                setOf(
                    "accountId",
                    "keystrokeTiming",
                    "humanScore",
                    "deviceGraph"
                )
            )
        }
    }

    @Test
    fun `protected nullifier data cannot enter discovery`() {
        assertFailsWith<BoundaryViolation> {
            ZoneEgressPolicy.validate(
                BoundaryEvent(
                    name = "nullifier-accepted",
                    classifications = setOf(
                        ProtectedDataClass.NULLIFIER,
                        ProtectedDataClass.UNIQUENESS_PROOF
                    ),
                    destination = TrustZone.DISCOVERY
                )
            )
        }
    }

    @Test
    fun `conditional UI requires conditional mediation with no allow credentials`() {
        val request = ConditionalWebAuthnRequest(
            rpId = "auth.hearthline.example",
            challenge = ByteArray(32) { 1 },
            userVerificationRequired = true,
            mediation = "conditional",
            allowCredentials = null
        )

        assertEquals("conditional", request.mediation)
    }

    @Test
    fun `conditional UI rejects an allow credentials list`() {
        assertFailsWith<IllegalArgumentException> {
            ConditionalWebAuthnRequest(
                rpId = "auth.hearthline.example",
                challenge = ByteArray(32) { 1 },
                userVerificationRequired = true,
                mediation = "conditional",
                allowCredentials = listOf(
                    CredentialId.of(byteArrayOf(0x01))
                )
            )
        }
    }

    @Test
    fun `consumer enrollment rejects direct attestation`() {
        assertFailsWith<IllegalArgumentException> {
            ConsumerEnrollmentPolicy(
                attestation = AttestationConveyance.DIRECT,
                userVerificationRequired = true,
                route = EnrollmentRoute.STANDARD_PASSKEY
            )
        }
    }

    @Test
    fun `credential binding never returns conflicting account`() {
        val otherAccount = AccountId(UUID.randomUUID())
        val requestedAccount = AccountId(UUID.randomUUID())
        val credential = CredentialId.of(byteArrayOf(0x01))

        val repository = object : ActiveCredentialBindingRepository {
            override fun findForCredential(
                credentialId: CredentialId
            ): ActiveCredentialBinding =
                ActiveCredentialBinding(
                    credentialId = credential,
                    accountId = otherAccount,
                    boundAt = Instant.now(),
                    revokedAt = null
                )

            override fun bindIfUnbound(
                binding: ActiveCredentialBinding
            ): Boolean = false

            override fun revoke(
                credentialId: CredentialId,
                accountId: AccountId,
                at: Instant
            ): Boolean = false
        }

        val result = AccountLocalCredentialBindingService(repository).bind(
            accountId = requestedAccount,
            credentialId = credential,
            at = Instant.now()
        )

        assertEquals(CredentialBindingResult.Unavailable, result)
    }

    @Test
    fun `age gate rejects passkey and payment proof sources`() {
        assertFailsWith<BoundaryViolation> {
            AgeGateProofSourcePolicy.requireAllowed(
                predicateName = "age_over_30",
                sources = setOf("passkey", "payment")
            )
        }
    }
}
