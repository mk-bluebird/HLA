package org.hearthline.anchorline

import java.time.Instant
import java.util.UUID

@JvmInline
value class SignupReceiptId(val value: UUID)

data class TimeBucket(
    val startInclusive: Instant,
    val durationSeconds: Long
) {
    init {
        require(durationSeconds == 900L) {
            "Published provenance time buckets must be 15 minutes."
        }
    }
}

data class MinimalSignupProvenance(
    val receiptId: SignupReceiptId,
    val accountId: AccountId,
    val registrationCompleted: Boolean,
    val registrationPolicyReference: String,
    val completedTimeBucket: TimeBucket,
    val expiresAt: Instant
) {
    init {
        require(registrationCompleted) {
            "This receipt represents only a completed registration."
        }

        require(registrationPolicyReference.isNotBlank()) {
            "Registration policy reference is required."
        }

        require(expiresAt.isAfter(completedTimeBucket.startInclusive)) {
            "Receipt expiry must follow its time bucket."
        }
    }
}
