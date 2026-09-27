package org.hearthline.anchorline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ScopedUniquenessProofTest {
    private val now = Instant.parse("2026-09-27T07:30:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `accepts one unused proof for one registration scope epoch`() {
        val nullifiers = InMemoryScopedNullifierRepository()

        val service = ScopedUniquenessAdmissionService(
            clock = clock,
            verifier = verifierReturningUnused(),
            nullifiers = nullifiers
        )

        val result = service.verifyAndConsume(
            scope = registrationScope("2026-09"),
            challenge = ByteArray(32) { 0x01 },
            proof = OpaqueUniquenessProof.of(byteArrayOf(0x02))
        )

        assertEquals(ScopedUniquenessRoute.Accepted, result)
        assertEquals(1, nullifiers.size())
    }

    @Test
    fun `reused proof becomes duplicate within same scope epoch`() {
        val nullifiers = InMemoryScopedNullifierRepository()
        val service = ScopedUniquenessAdmissionService(
            clock = clock,
            verifier = verifierReturningUnused(),
            nullifiers = nullifiers
        )

        val scope = registrationScope("2026-09")
        val challenge = ByteArray(32) { 0x01 }
        val proof = OpaqueUniquenessProof.of(byteArrayOf(0x02))

        val first = service.verifyAndConsume(scope, challenge, proof)
        val second = service.verifyAndConsume(scope, challenge, proof)

        assertEquals(ScopedUniquenessRoute.Accepted, first)
        assertEquals(ScopedUniquenessRoute.DuplicateWithinScope, second)
        assertEquals(1, nullifiers.size())
    }

    @Test
    fun `invalid unavailable expired and wrong scope route to accessible alternative`() {
        val verificationResults = listOf(
            VerifiedScopedUniquenessProof.invalid(),
            VerifiedScopedUniquenessProof.unavailable(),
            VerifiedScopedUniquenessProof.expired(),
            VerifiedScopedUniquenessProof.wrongScope()
        )

        verificationResults.forEach { verification ->
            val service = ScopedUniquenessAdmissionService(
                clock = clock,
                verifier = object : AnonymousUniquenessProofVerifier {
                    override fun verify(
                        expectedScope: UniquenessScope,
                        expectedChallenge: ByteArray,
                        proof: OpaqueUniquenessProof
                    ): VerifiedScopedUniquenessProof = verification
                },
                nullifiers = InMemoryScopedNullifierRepository()
            )

            assertEquals(
                ScopedUniquenessRoute.AccessibleAlternative,
                service.verifyAndConsume(
                    scope = registrationScope("2026-09"),
                    challenge = ByteArray(32) { 0x01 },
                    proof = OpaqueUniquenessProof.of(byteArrayOf(0x02))
                )
            )
        }
    }

    @Test
    fun `rejects invalid scope purpose and relying party`() {
        assertFailsWith<IllegalArgumentException> {
            UniquenessScope(
                relyingPartyId = "other.example",
                purpose = "registration",
                epoch = "2026-09"
            )
        }

        assertFailsWith<IllegalArgumentException> {
            UniquenessScope(
                relyingPartyId = "auth.hearthline.example",
                purpose = "discovery",
                epoch = "2026-09"
            )
        }
    }

    @Test
    fun `does not disclose nullifier for non-accepted verifier result`() {
        assertFailsWith<IllegalArgumentException> {
            VerifiedScopedUniquenessProof(
                result = ScopedUniquenessVerification.INVALID,
                nullifierHash = ScopedNullifierHash.of(ByteArray(32) { 0x03 })
            )
        }
    }

    private fun registrationScope(epoch: String): UniquenessScope =
        UniquenessScope(
            relyingPartyId = "auth.hearthline.example",
            purpose = "registration",
            epoch = epoch
        )

    private fun verifierReturningUnused(): AnonymousUniquenessProofVerifier =
        object : AnonymousUniquenessProofVerifier {
            override fun verify(
                expectedScope: UniquenessScope,
                expectedChallenge: ByteArray,
                proof: OpaqueUniquenessProof
            ): VerifiedScopedUniquenessProof =
                VerifiedScopedUniquenessProof.unused(
                    ScopedNullifierHash.of(ByteArray(32) { 0x55 })
                )
        }
}
