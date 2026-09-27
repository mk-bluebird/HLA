package org.hearthline.anchorline

import java.time.Instant
import java.util.UUID

@JvmInline
value class OpaqueEpochId(val value: UUID)

@JvmInline
value class LifecycleKeyReference(val value: UUID)

data class NullifierEpochShard(
    val shardId: UUID,
    val epochId: OpaqueEpochId,
    val purpose: String,
    val lifecycleKeyReference: LifecycleKeyReference,
    val closesAt: Instant
) {
    init {
        require(purpose == "registration") {
            "Nullifier shards are restricted to registration."
        }
    }
}

interface NullifierShardEraser {
    fun eraseShard(
        shardId: UUID,
        lifecycleKeyReference: LifecycleKeyReference
    )
}
