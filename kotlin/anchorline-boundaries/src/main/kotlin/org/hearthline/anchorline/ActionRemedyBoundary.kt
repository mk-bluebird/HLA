package org.hearthline.anchorline

import java.time.Instant
import java.util.UUID

@JvmInline
value class IncidentId(val value: UUID)

enum class DirectActionCategory {
    MESSAGE_SUBMISSION,
    INVITATION_SUBMISSION,
    MEDIA_SHARE_REQUEST,
    EXPLICIT_MEDIA_SHARE_REQUEST,
    REPORTED_CONTENT_REVIEW
}

enum class RemedyState {
    ACTIVE,
    EXPIRED,
    OVERTURNED
}

/**
 * This record is scoped to a specific incident and action category.
 *
 * It has no device, network, profile, biometric, trust, score, or cross-account
 * relationship data. It must not be aggregated into a member reputation.
 */
data class ActionScopedRemedy(
    val incidentId: IncidentId,
    val accountId: AccountId,
    val actionCategory: DirectActionCategory,
    val state: RemedyState,
    val policyReference: String,
    val startedAt: Instant,
    val expiresAt: Instant,
    val appealState: String?
)

interface ActionScopedRemedyRepository {
    fun findActive(
        accountId: AccountId,
        actionCategory: DirectActionCategory,
        now: Instant
    ): ActionScopedRemedy?

    fun save(remedy: ActionScopedRemedy)

    fun deleteExpiredOrOverturned(now: Instant): Int
}

class ActionScopedRemedyAuthorizer(
    private val remedies: ActionScopedRemedyRepository
) {
    fun requireActionAvailable(
        accountId: AccountId,
        action: DirectActionCategory,
        now: Instant
    ) {
        val active = remedies.findActive(accountId, action, now)

        if (active != null && active.state == RemedyState.ACTIVE) {
            throw AuthorizationDenied(
                "This action is temporarily unavailable. You can review or appeal this decision."
            )
        }
    }
}
