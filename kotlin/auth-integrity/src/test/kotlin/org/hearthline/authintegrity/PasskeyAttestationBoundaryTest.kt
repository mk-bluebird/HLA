package org.hearthline.authintegrity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class PasskeyAttestationBoundaryTest {
    private val now = Instant.parse("2026-09-27T05:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private val credentials = InMemoryCredentialRepository()
    private val sessions = InMemorySessionRepository()
    private val challenges = InMemoryChallengeRepository()
    private val authorizations = InMemoryActionAuthorizationRepository()
    private val securityHistory = InMemorySecurityHistoryRepository()

    private val webAuthn = object : WebAuthnVerifier {
        override fun verifyAssertion(
            expectedChallenge: ByteArray,
            credentialPublicKeyCose: ByteArray,
            assertion: WebAuthnAssertion
        ): Boolean = expectedChallenge.contentEquals("expected".toByteArray())
    }

    private val apple = object : AppleAppAttestVerifier {
        override fun verify(
            actionClass: ProtectedActionClass,
            challenge: IntegrityChallenge,
            evidence: RawAttestationEvidence.AppleAppAttest
        ): AttestationDecision = AttestationDecision.ALLOW
    }

    private val google = object : GooglePlayIntegrityVerifier {
        override fun verify(
            actionClass: ProtectedActionClass,
            challenge: IntegrityChallenge,
            evidence: RawAttestationEvidence.GooglePlayIntegrity
        ): AttestationDecision = AttestationDecision.ALLOW
    }

    private val passkeys = PasskeyAuthenticationService(
        clock = clock,
        credentials = credentials,
        sessions = sessions,
        webAuthn = webAuthn,
        securityHistory = securityHistory
    )

    private val integrity = AttestationActionIntegrityService(
        clock = clock,
        challenges = challenges,
        authorizations = authorizations,
        appleVerifier = apple,
        googleVerifier = google,
        securityHistory = securityHistory
    )

    @Test
    fun `revoking credential ends only sessions authenticated by that credential`() {
        val accountId = AccountId(UUID.randomUUID())
        val credentialA = CredentialId(byteArrayOf(0x01))
        val credentialB = CredentialId(byteArrayOf(0x02))

        credentials.save(
            RegisteredCredential(
                accountId = accountId,
                credentialId = credentialA,
                publicKeyCose = byteArrayOf(0x11),
                createdAt = now,
                status = CredentialStatus.ACTIVE
            )
        )

        credentials.save(
            RegisteredCredential(
                accountId = accountId,
                credentialId = credentialB,
                publicKeyCose = byteArrayOf(0x22),
                createdAt = now,
                status = CredentialStatus.ACTIVE
            )
        )

        val sessionA = AuthSession(
            sessionId = SessionId(UUID.randomUUID()),
            accountId = accountId,
            authenticatingCredentialId = credentialA,
            issuedAt = now,
            expiresAt = now.plusSeconds(600),
            status = SessionStatus.ACTIVE
        )

        val sessionB = AuthSession(
            sessionId = SessionId(UUID.randomUUID()),
            accountId = accountId,
            authenticatingCredentialId = credentialB,
            issuedAt = now,
            expiresAt = now.plusSeconds(600),
            status = SessionStatus.ACTIVE
        )

        sessions.save(sessionA)
        sessions.save(sessionB)

        val result = passkeys.revokeCredential(accountId, credentialA)

        assertTrue(result.credentialRevoked)
        assertEquals(1, result.linkedSessionsRevoked)
        assertEquals(SessionStatus.REVOKED, sessions.find(sessionA.sessionId)?.status)
        assertEquals(SessionStatus.ACTIVE, sessions.find(sessionB.sessionId)?.status)
    }

    @Test
    fun `revoked credential cannot authorize a current session`() {
        val accountId = AccountId(UUID.randomUUID())
        val credentialId = CredentialId(byteArrayOf(0x09))
        val sessionId = SessionId(UUID.randomUUID())

        credentials.save(
            RegisteredCredential(
                accountId = accountId,
                credentialId = credentialId,
                publicKeyCose = byteArrayOf(0x01),
                createdAt = now,
                status = CredentialStatus.REVOKED,
                revokedAt = now
            )
        )

        sessions.save(
            AuthSession(
                sessionId = sessionId,
                accountId = accountId,
                authenticatingCredentialId = credentialId,
                issuedAt = now,
                expiresAt = now.plusSeconds(600),
                status = SessionStatus.ACTIVE
            )
        )

        assertFailsWith<AuthenticationDenied> {
            passkeys.requireActiveSession(sessionId)
        }

        assertEquals(SessionStatus.REVOKED, sessions.find(sessionId)?.status)
    }

    @Test
    fun `unsupported attestation routes only the protected action`() {
        val accountId = AccountId(UUID.randomUUID())
        val request = """{"kind":"registration-finalize"}""".toByteArray()

        val challenge = integrity.issueChallenge(
            accountId = accountId,
            actionClass = ProtectedActionClass.REGISTRATION_FINALIZE,
            canonicalActionRequest = request
        )

        val authorization = integrity.decideAction(
            challengeId = challenge.challengeId,
            actualCanonicalActionRequest = request,
            evidence = RawAttestationEvidence.Unsupported(receivedAt = now)
        )

        assertEquals(
            AttestationDecision.ROUTE_TO_ACCESSIBLE_ALTERNATIVE,
            authorization.decision
        )

        assertEquals(
            IntegrityRoute.PASSKEY_ONLY_PATH,
            authorization.route
        )
    }

    @Test
    fun `challenge is single use and cannot be replayed`() {
        val accountId = AccountId(UUID.randomUUID())
        val request = """{"kind":"change-settings"}""".toByteArray()

        val challenge = integrity.issueChallenge(
            accountId = accountId,
            actionClass = ProtectedActionClass.SENSITIVE_SETTING_CHANGE,
            canonicalActionRequest = request
        )

        val first = integrity.decideAction(
            challengeId = challenge.challengeId,
            actualCanonicalActionRequest = request,
            evidence = RawAttestationEvidence.AppleAppAttest(
                assertionObject = byteArrayOf(0x01),
                receivedAt = now
            )
        )

        val replay = integrity.decideAction(
            challengeId = challenge.challengeId,
            actualCanonicalActionRequest = request,
            evidence = RawAttestationEvidence.AppleAppAttest(
                assertionObject = byteArrayOf(0x01),
                receivedAt = now
            )
        )

        assertEquals(AttestationDecision.ALLOW, first.decision)
        assertEquals(
            AttestationDecision.ROUTE_TO_ACCESSIBLE_ALTERNATIVE,
            replay.decision
        )
    }

    @Test
    fun `request binding mismatch cannot authorize the action`() {
        val accountId = AccountId(UUID.randomUUID())
        val originalRequest = """{"kind":"export","scope":"standard"}""".toByteArray()
        val alteredRequest = """{"kind":"export","scope":"extended"}""".toByteArray()

        val challenge = integrity.issueChallenge(
            accountId = accountId,
            actionClass = ProtectedActionClass.ACCOUNT_EXPORT_START,
            canonicalActionRequest = originalRequest
        )

        val authorization = integrity.decideAction(
            challengeId = challenge.challengeId,
            actualCanonicalActionRequest = alteredRequest,
            evidence = RawAttestationEvidence.GooglePlayIntegrity(
                signedIntegrityToken = "opaque-token-not-stored",
                receivedAt = now
            )
        )

        assertEquals(
            AttestationDecision.RETRY_WITH_NEW_CHALLENGE,
            authorization.decision
        )
    }
}
