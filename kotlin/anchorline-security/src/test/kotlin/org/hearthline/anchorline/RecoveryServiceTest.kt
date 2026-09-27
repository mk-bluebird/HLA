package org.hearthline.anchorline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class RecoveryServiceTest {
    private val now = Instant.parse("2026-09-27T07:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `recovery authorization enters hold without creating a session`() {
        val fixture = fixture()
        val accountId = AccountId(UUID.randomUUID())

        val challenge = fixture.service.begin(accountId)

        val result = fixture.service.authorize(
            recoveryId = challenge.recoveryId,
            attempt = RecoveryAttempt.IndependentPasskey(
                assertion = byteArrayOf(0x01)
            )
        )

        assertEquals(RecoveryState.HOLD, result.state)
        assertEquals(
            RecoveryAuthorizationMethod.SECOND_REGISTERED_PASSKEY,
            result.authorizationMethod
        )
        assertEquals(0, fixture.sessions.revocationCalls)
        assertEquals(0, fixture.credentials.addCalls)
    }

    @Test
    fun `recovery completion before mandatory hold fails`() {
        val fixture = fixture()
        val accountId = AccountId(UUID.randomUUID())

        val challenge = fixture.service.begin(accountId)

        fixture.service.authorize(
            recoveryId = challenge.recoveryId,
            attempt = RecoveryAttempt.IndependentPasskey(
                assertion = byteArrayOf(0x01)
            )
        )

        assertFailsWith<RecoveryDenied> {
            fixture.service.completeAfterHold(
                recoveryId = challenge.recoveryId,
                replacement = ReplacementPasskeyRegistration(
                    credentialId = CredentialId(byteArrayOf(0x02)),
                    publicKeyCose = byteArrayOf(0x03)
                )
            )
        }

        assertEquals(0, fixture.sessions.revocationCalls)
        assertEquals(0, fixture.credentials.addCalls)
    }

    @Test
    fun `active credential cancellation ends recovery during hold`() {
        val fixture = fixture()
        val accountId = AccountId(UUID.randomUUID())

        val challenge = fixture.service.begin(accountId)

        fixture.service.authorize(
            recoveryId = challenge.recoveryId,
            attempt = RecoveryAttempt.IndependentPasskey(
                assertion = byteArrayOf(0x01)
            )
        )

        val cancelled = fixture.service.cancelWithActiveCredential(
            accountId = accountId,
            recoveryId = challenge.recoveryId
        )

        assertEquals(RecoveryState.CANCELLED, cancelled.state)
        assertTrue(cancelled.cancelledAt != null)
        assertEquals(1, fixture.notifier.cancelled)
    }

    @Test
    fun `invalid recovery proof routes to review without a privileged session`() {
        val fixture = fixture(passkeyProofValid = false)
        val accountId = AccountId(UUID.randomUUID())

        val challenge = fixture.service.begin(accountId)

        val result = fixture.service.authorize(
            recoveryId = challenge.recoveryId,
            attempt = RecoveryAttempt.IndependentPasskey(
                assertion = byteArrayOf(0x01)
            )
        )

        assertEquals(RecoveryState.REVIEW_REQUIRED, result.state)
        assertEquals(0, fixture.sessions.revocationCalls)
        assertEquals(0, fixture.credentials.addCalls)
    }

    private fun fixture(
        passkeyProofValid: Boolean = true
    ): Fixture {
        val cases = InMemoryRecoveryCases()
        val challenges = InMemoryRecoveryChallenges()
        val codes = InMemoryRecoveryCodes()
        val sessions = RecordingSessionInvalidator()
        val credentials = RecordingCredentialReplacement()
        val notifier = RecordingNotifier()

        val service = RecoveryService(
            clock = clock,
            random = SecureRandom(),
            recoveryCases = cases,
            challenges = challenges,
            recoveryCodes = codes,
            recoveryCodeHasher = TestRecoveryCodeHasher(),
            proofVerifier = object : IndependentRecoveryProofVerifier {
                override fun verifyIndependentRegisteredPasskey(
                    accountId: AccountId,
                    challenge: ByteArray,
                    assertion: ByteArray
                ): Boolean = passkeyProofValid

                override fun verifyPreestablishedFederatedBinding(
                    accountId: AccountId,
                    authorizationCode: String,
                    pkceVerifier: String
                ): Boolean = false
            },
            credentialReplacement = credentials,
            sessions = sessions,
            notifier = notifier
        )

        return Fixture(service, sessions, credentials, notifier)
    }

    private data class Fixture(
        val service: RecoveryService,
        val sessions: RecordingSessionInvalidator,
        val credentials: RecordingCredentialReplacement,
        val notifier: RecordingNotifier
    )

    private class InMemoryRecoveryCases : RecoveryCaseRepository {
        private val values = mutableMapOf<RecoveryId, RecoveryCase>()

        override fun save(value: RecoveryCase) {
            values[value.recoveryId] = value
        }

        override fun find(recoveryId: RecoveryId): RecoveryCase? =
            values[recoveryId]

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
            val expired = values.values
                .filter { now >= it.expiresAt }
                .map { it.recoveryId }

            expired.forEach(values::remove)
            return expired.size
        }
    }

    private class InMemoryRecoveryChallenges : RecoveryChallengeRepository {
        private val values = mutableMapOf<RecoveryId, RecoveryChallenge>()

        override fun save(value: RecoveryChallenge) {
            values[value.recoveryId] = value
        }

        override fun consume(
            recoveryId: RecoveryId,
            at: Instant
        ): RecoveryChallenge? {
            val current = values[recoveryId] ?: return null
            if (current.consumedAt != null) {
                return null
            }

            val consumed = current.copy(consumedAt = at)
            values[recoveryId] = consumed
            return consumed
        }

        override fun deleteExpired(now: Instant): Int {
            val expired = values.values
                .filter { now >= it.expiresAt }
                .map { it.recoveryId }

            expired.forEach(values::remove)
            return expired.size
        }
    }

    private class InMemoryRecoveryCodes : RecoveryCodeRepository {
        override fun save(verifier: RecoveryCodeVerifier) = Unit

        override fun findActive(accountId: AccountId): List<RecoveryCodeVerifier> =
            emptyList()

        override fun consumeOneAndRevokeRest(
            accountId: AccountId,
            code: CharArray,
            hasher: RecoveryCodeHasher,
            at: Instant
        ): Boolean = false

        override fun revokeAll(accountId: AccountId, at: Instant): Int = 0
    }

    private class RecordingSessionInvalidator : RecoverySessionInvalidator {
        var revocationCalls: Int = 0

        override fun revokeAllSessions(
            accountId: AccountId,
            at: Instant
        ) {
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

    private class RecordingNotifier : RecoveryNotifier {
        var started: Int = 0
        var authorized: Int = 0
        var cancelled: Int = 0
        var completed: Int = 0

        override fun recoveryStarted(
            accountId: AccountId,
            recoveryId: RecoveryId
        ) {
            started += 1
        }

        override fun recoveryAuthorized(
            accountId: AccountId,
            recoveryId: RecoveryId
        ) {
            authorized += 1
        }

        override fun recoveryCancelled(
            accountId: AccountId,
            recoveryId: RecoveryId
        ) {
            cancelled += 1
        }

        override fun recoveryCompleted(
            accountId: AccountId,
            recoveryId: RecoveryId
        ) {
            completed += 1
        }
    }

    private class TestRecoveryCodeHasher : RecoveryCodeHasher {
        override fun hash(code: CharArray, salt: ByteArray): ByteArray =
            (code.concatToString() + salt.decodeToString()).toByteArray()

        override fun verify(
            code: CharArray,
            salt: ByteArray,
            verifier: ByteArray
        ): Boolean =
            hash(code, salt).contentEquals(verifier)
    }
}
