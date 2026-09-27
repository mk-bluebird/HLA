package org.hearthline.anchorline

/**
 * This object represents an optional UI-level completion control, not a
 * biometric system. It stores no timing, pointer, touch, keyboard, gesture,
 * voice, motion, or behavioral information.
 *
 * The output is deliberately local-only. It cannot be serialized, logged,
 * transmitted, attached to a signup ticket, or associated with an account.
 */
class LocalInteractionControl {
    private var completedForCurrentScreen: Boolean = false

    fun markCompleted(): LocalInteractionResult {
        completedForCurrentScreen = true
        return LocalInteractionResult.Completed
    }

    fun skipForAccessibleAlternative(): LocalInteractionResult =
        LocalInteractionResult.AccessibleAlternative

    fun canSubmitCurrentScreen(): Boolean = completedForCurrentScreen

    fun resetOnScreenExit() {
        completedForCurrentScreen = false
    }
}

sealed interface LocalInteractionResult {
    data object Completed : LocalInteractionResult
    data object AccessibleAlternative : LocalInteractionResult
}

/**
 * A compile-time boundary for payload serializers and analytics adapters.
 *
 * There is intentionally no method returning interaction timing, gesture
 * sequence, pointer path, keystroke rhythm, completion duration, confidence,
 * human/bot label, or behavioral score.
 */
interface LocalOnlyInteractionBoundary {
    fun submitToServer(): Nothing =
        throw BoundaryViolation(
            "Local interaction-control state must never be transmitted to a server."
        )

    fun emitAnalytics(): Nothing =
        throw BoundaryViolation(
            "Local interaction-control state must never be emitted to analytics."
        )
}
