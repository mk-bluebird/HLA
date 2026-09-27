package org.hearthline.anchorline

import java.time.Instant

enum class AgeEligibilityState {
    UNKNOWN,
    ELIGIBLE,
    NOT_ELIGIBLE,
    REVIEW_REQUESTED,
    EXPIRED
}

/**
 * This is the only AgeGate projection permitted outside the AgeGate boundary.
 */
data class AgeEligibilityProjection(
    val accountId: AccountId,
    val state: AgeEligibilityState,
    val expiresAt: Instant?,
    val updatedAt: Instant
)

object AgeGateProofSourcePolicy {
    private val prohibitedSources = setOf(
        "passkey",
        "federation",
        "payment",
        "device_attestation",
        "app_attestation",
        "hardware",
        "provider_account_age",
        "provider_trust",
        "behavioral_biometric",
        "device_ownership",
        "network",
        "location"
    )

    fun requireAllowed(
        predicateName: String,
        sources: Set<String>
    ) {
        require(predicateName == "age_over_30") {
            "AgeGate accepts only the age_over_30 predicate."
        }

        val violations = sources.intersect(prohibitedSources)
        if (violations.isNotEmpty()) {
            throw BoundaryViolation(
                "Prohibited AgeGate proof source: " +
                    violations.sorted().joinToString()
            )
        }
    }
}
