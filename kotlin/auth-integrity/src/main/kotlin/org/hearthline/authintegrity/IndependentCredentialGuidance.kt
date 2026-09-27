package org.hearthline.authintegrity

/**
 * This service produces member-controlled guidance only.
 *
 * It receives no device telemetry, no AAGUID, no hardware attestation,
 * no platform type, and no inventory of the member's devices.
 */
class IndependentCredentialGuidance {
    fun recommendation(
        activeCredentialCount: Int,
        memberRequestedSecurityReview: Boolean
    ): CredentialGuidance {
        if (!memberRequestedSecurityReview) {
            return CredentialGuidance(
                title = "Your sign-in choices",
                message =
                    "You can add more than one passkey to keep access to your " +
                        "account if a device is lost or replaced.",
                actions = listOf(
                    "Add another passkey",
                    "Review active sign-in credentials",
                    "Sign out of other sessions"
                )
            )
        }

        return when {
            activeCredentialCount == 0 -> CredentialGuidance(
                title = "Add a sign-in method",
                message =
                    "Add a passkey from a device or security key you control. " +
                        "You can choose the option that works with your access needs.",
                actions = listOf("Add a passkey", "View recovery options")
            )

            activeCredentialCount == 1 -> CredentialGuidance(
                title = "Consider an independent backup sign-in method",
                message =
                    "A second passkey or a compatible security key can help you " +
                        "regain access if one device, password manager, or account " +
                        "recovery path becomes unavailable. This is optional.",
                actions = listOf(
                    "Add another passkey",
                    "Use a compatible security key",
                    "Keep current setup"
                )
            )

            else -> CredentialGuidance(
                title = "You have multiple sign-in methods",
                message =
                    "You can remove a credential you no longer use or keep more " +
                        "than one sign-in method for account continuity.",
                actions = listOf(
                    "Review sign-in credentials",
                    "Sign out everywhere",
                    "Keep current setup"
                )
            )
        }
    }
}

data class CredentialGuidance(
    val title: String,
    val message: String,
    val actions: List<String>
)
