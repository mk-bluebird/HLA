package org.hearthline.anchorline

import java.sql.Connection
import java.sql.PreparedStatement
import java.time.Instant
import javax.sql.DataSource

class PostgresScopedNullifierRepository(
    private val dataSource: DataSource
) : ScopedNullifierRepository {
    override fun insertIfUnused(value: AcceptedScopedNullifier): Boolean {
        val sql = """
            INSERT INTO anchorline_scoped_nullifiers (
                nullifier_hash,
                relying_party_id,
                purpose,
                epoch,
                accepted_at,
                expires_at,
                policy_version
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (
                nullifier_hash,
                relying_party_id,
                purpose,
                epoch
            ) DO NOTHING
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setBytes(1, value.key.nullifierHash.copyBytes())
                statement.setString(2, value.key.relyingPartyId)
                statement.setString(3, value.key.purpose)
                statement.setString(4, value.key.epoch)
                statement.setObject(5, value.acceptedAt)
                statement.setObject(6, value.expiresAt)
                statement.setString(7, value.policyVersion)

                return statement.executeUpdate() == 1
            }
        }
    }

    override fun deleteExpired(now: Instant): Int {
        val sql = """
            DELETE FROM anchorline_scoped_nullifiers
            WHERE expires_at <= ?
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, now)
                return statement.executeUpdate()
            }
        }
    }
}
