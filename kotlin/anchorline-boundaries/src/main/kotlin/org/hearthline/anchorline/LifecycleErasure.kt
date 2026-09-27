package org.hearthline.anchorline

import java.time.Instant
import java.util.UUID

@JvmInline
value class LifecycleKeyId(val value: UUID)

enum class ErasureTarget {
    SESSION_SENDER_CONSTRAINT,
    ADMISSION_SIGNAL,
    RAW_AGE_PROOF,
    RAW_UNIQUENESS_PROOF,
    NULLIFIER_EPOCH_SHARD,
    SIGNUP_PROVENANCE,
    APPEAL_CASE
}

data class LifecycleKeyBinding(
    val keyId: LifecycleKeyId,
    val target: ErasureTarget,
    val expiresAt: Instant
)

interface LifecycleKeyManager {
    fun destroy(keyId: LifecycleKeyId)
    fun verifyDestroyed(keyId: LifecycleKeyId): Boolean
}

interface ErasureTargetRepository {
    fun deleteEncryptedMaterial(
        target: ErasureTarget,
        keyId: LifecycleKeyId
    ): Int
}

/**
 * Cryptographic erasure requires deletion of encrypted material and lifecycle
 * key destruction. Production implementations must also destroy wrapped backup
 * copies of the same lifecycle key.
 */
class CryptographicErasureService(
    private val keys: LifecycleKeyManager,
    private val material: ErasureTargetRepository
) {
    fun erase(binding: LifecycleKeyBinding): Boolean {
        material.deleteEncryptedMaterial(binding.target, binding.keyId)
        keys.destroy(binding.keyId)
        return keys.verifyDestroyed(binding.keyId)
    }
}
