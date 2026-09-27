package org.hearthline.anchorline

import java.time.Instant

interface AccountSessionRevocationPort {
    fun revokeAllForAccount(
        accountId: AccountId,
        revokedAt: Instant
    ): Int
}

class RecoverySessionAdapter(
    private val sessions: AccountSessionRevocationPort
) : RecoverySessionInvalidator {
    override fun revokeAllSessions(
        accountId: AccountId,
        at: Instant
    ) {
        sessions.revokeAllForAccount(accountId, at)
    }
}
