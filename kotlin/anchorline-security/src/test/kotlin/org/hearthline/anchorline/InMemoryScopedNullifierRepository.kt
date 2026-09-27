package org.hearthline.anchorline

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class InMemoryScopedNullifierRepository : ScopedNullifierRepository {
    private val values =
        ConcurrentHashMap<ScopedNullifierKey, AcceptedScopedNullifier>()

    override fun insertIfUnused(value: AcceptedScopedNullifier): Boolean =
        values.putIfAbsent(value.key, value) == null

    override fun deleteExpired(now: Instant): Int {
        var deleted = 0

        values.entries.forEach { entry ->
            if (now >= entry.value.expiresAt) {
                if (values.remove(entry.key, entry.value)) {
                    deleted += 1
                }
            }
        }

        return deleted
    }

    fun size(): Int = values.size
}
