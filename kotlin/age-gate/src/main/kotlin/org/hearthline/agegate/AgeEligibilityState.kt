package org.hearthline.agegate

import java.time.Instant
import java.util.UUID

@JvmInline
value class AccountId(val value: UUID)

enum class AgeEligibilityState {
    UNKNOWN,
    ELIGIBLE,
    NOT_ELIGIBLE,
    UNAVAILABLE,
    EXPIRED,
    REVIEW_REQUESTED
}

/**
 * This is the only age-related record available to ordinary Hearthline product
 * services. It intentionally contains no DOB, exact age, age range, document
 * data, identity-provider subject, payment data, device data, or proof method.
 */
data class AgeEligibilityGrant(
    val accountId: AccountId,
    val state: AgeEligibilityState,
    val policyVersion: String,
    val issuedAt: Instant?,
    val expiresAt: Instant?,
    val updatedAt: Instant
) {
    fun allowsAdultOnlyParticipation(now: Instant): Boolean =
        state == AgeEligibilityState.ELIGIBLE &&
            expiresAt != null &&
            now.isBefore(expiresAt)
}

/**
 * These values are internal routing outcomes only. They must not be exposed
 * to other members or copied into profile, matching, discovery, safety,
 * reputation, ranking, analytics, or recommendation systems.
 */
enum class AgeGateDecision {
    ELIGIBLE,
    NOT_ELIGIBLE,
    ROUTE_TO_ACCESSIBLE_REVIEW,
    RETRY_WITH_NEW_PRESENTATION
}
