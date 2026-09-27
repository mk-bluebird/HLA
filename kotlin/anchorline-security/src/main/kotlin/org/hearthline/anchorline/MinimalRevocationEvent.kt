package org.hearthline.anchorline

import java.time.Instant
import java.util.UUID

@JvmInline
value class RevocationEventId(val value: UUID)

data class MinimalRevocationEvent(
    val eventId: RevocationEventId,
    val accountId: AccountId,
    val credentialId: CredentialId?,
    val accountGeneration: Long,
    val credentialGeneration: Long?,
    val occurredAt: Instant,
    val deliveryDeadline: Instant
) {
    init {
        require(deliveryDeadline.isAfter(occurredAt)) {
            "Revocation delivery deadline must follow event creation."
        }
    }
}
