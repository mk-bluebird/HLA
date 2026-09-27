package org.hearthline.anchorline

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID

@JvmInline
value class LedgerEntryId(val value: UUID)

@JvmInline
value class LedgerHash(val value: ByteArray) {
    override fun equals(other: Any?): Boolean =
        other is LedgerHash && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()

    fun base64Url(): String =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value)
}

data class SignupLedgerEntry(
    val entryId: LedgerEntryId,
    val accountId: AccountId,
    val ceremonyId: SignupCeremonyId,
    val recordedAt: Instant,
    val previousHash: LedgerHash?,
    val currentHash: LedgerHash,
    val expiresAt: Instant
)

interface SignupLedgerRepository {
    fun latestForAccount(accountId: AccountId): SignupLedgerEntry?

    /**
     * This must append atomically with the provenance receipt or with a
     * transactionally linked outbox event.
     */
    fun append(entry: SignupLedgerEntry)

    fun deleteExpired(now: Instant): Int
}

class TamperEvidentSignupLedger(
    private val ledger: SignupLedgerRepository
) {
    fun append(receipt: SignupProvenanceReceipt): SignupLedgerEntry {
        require(receipt.accountId != null) {
            "Only completed signup ceremonies enter the account ledger."
        }

        val previous = ledger.latestForAccount(receipt.accountId)

        val currentHash = LedgerHash(
            sha256(
                canonicalReceiptBytes(receipt) +
                    (previous?.currentHash?.value ?: ByteArray(0))
            )
        )

        return SignupLedgerEntry(
            entryId = LedgerEntryId(UUID.randomUUID()),
            accountId = receipt.accountId,
            ceremonyId = receipt.ceremonyId,
            recordedAt = receipt.completedAt,
            previousHash = previous?.currentHash,
            currentHash = currentHash,
            expiresAt = receipt.expiresAt
        ).also(ledger::append)
    }

    private fun canonicalReceiptBytes(
        receipt: SignupProvenanceReceipt
    ): ByteArray {
        val payload = buildString {
            append(receipt.ceremonyId.value)
            append('|')
            append(receipt.accountId?.value)
            append('|')
            append(receipt.outcome.name)
            append('|')
            append(receipt.evidence.map { it.name }.sorted().joinToString(","))
            append('|')
            append(receipt.registrationPolicyVersion)
            append('|')
            append(receipt.webAuthnVerifierVersion)
            append('|')
            append(receipt.ageGatePolicyVersion ?: "")
            append('|')
            append(receipt.startedAt)
            append('|')
            append(receipt.completedAt)
            append('|')
            append(receipt.expiresAt)
        }

        return payload.toByteArray(StandardCharsets.UTF_8)
    }

    private fun sha256(value: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value)
}
