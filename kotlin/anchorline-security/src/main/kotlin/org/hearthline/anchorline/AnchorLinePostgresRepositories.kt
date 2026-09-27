package org.hearthline.anchorline

import java.security.SecureRandom
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

/*
 * JDBC adapters for the AnchorLine lifecycle tables. Prepared statements only,
 * explicit transactions for multi-step security changes, and byte-array copies
 * on every returned record. No proof bytes, codes, or credential material are
 * ever logged.
 */

private fun PreparedStatement.setUuid(index: Int, value: UUID) =
    this.setObject(index, value)

private fun PreparedStatement.setInstant(index: Int, value: Instant?) {
    if (value == null) setNull(index, java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
    else setObject(index, value)
}

private fun ResultSet.uuid(name: String): UUID =
    UUID.fromString(this.getObject(name).toString())

private fun ResultSet.instant(name: String): Instant =
    this.getTimestamp(name).toInstant()

private fun ResultSet.maybeInstant(name: String): Instant? =
    this.getTimestamp(name)?.toInstant()

@Suppress("TooGenericExceptionCaught")
private fun DataSource.inTransaction(
    block: (Connection) -> Unit
) {
    connection.use { c ->
        val previousAutoCommit = c.autoCommit
        c.autoCommit = false
        try {
            block(c)
            c.commit()
        } catch (failure: Exception) {
            try {
                c.rollback()
            } catch (@Suppress("EmptyCatchBlock") rollbackFailure: Exception) {
                // Rollback failure is secondary; original failure propagates.
            }
            throw failure
        } finally {
            c.autoCommit = previousAutoCommit
        }
    }
}

class PostgresRegistrationChallengeRepository(
    private val dataSource: DataSource
) : RegistrationChallengeRepository {
    override fun save(challenge: RegistrationChallenge) {
        val sql = """
            INSERT INTO anchorline_registration_challenges
                (challenge_id, account_id, challenge, issued_at, expires_at, consumed_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, challenge.challengeId.value)
                s.setUuid(2, challenge.accountId.value)
                s.setBytes(3, challenge.challenge.copyOf())
                s.setInstant(4, challenge.issuedAt)
                s.setInstant(5, challenge.expiresAt)
                s.setInstant(6, challenge.consumedAt)
                s.executeUpdate()
            }
        }
    }

    override fun consume(
        challengeId: ChallengeId,
        at: Instant
    ): RegistrationChallenge? {
        val select = """
            UPDATE anchorline_registration_challenges
            SET consumed_at = ?
            WHERE challenge_id = ? AND consumed_at IS NULL AND expires_at > ?
            RETURNING challenge_id, account_id, challenge, issued_at, expires_at, consumed_at
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(select).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, challengeId.value)
                s.setInstant(3, at)
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return RegistrationChallenge(
                        challengeId = ChallengeId(rs.uuid("challenge_id")),
                        accountId = AccountId(rs.uuid("account_id")),
                        challenge = rs.getBytes("challenge").copyOf(),
                        issuedAt = rs.instant("issued_at"),
                        expiresAt = rs.instant("expires_at"),
                        consumedAt = rs.maybeInstant("consumed_at")
                    )
                }
            }
        }
    }

    override fun deleteExpired(now: Instant): Int {
        val sql = "DELETE FROM anchorline_registration_challenges WHERE expires_at <= ?"
        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, now)
                return s.executeUpdate()
            }
        }
    }
}

class PostgresSignupChallengeRepository(
    private val dataSource: DataSource
) : SignupChallengeRepository {
    override fun save(challenge: SignupChallenge) {
        val sql = """
            INSERT INTO anchorline_signup_challenges
                (challenge_id, challenge, issued_at, expires_at, consumed_at, global_pow_difficulty)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, challenge.challengeId.value)
                s.setBytes(2, challenge.challengeBytes.copyOf())
                s.setInstant(3, challenge.issuedAt)
                s.setInstant(4, challenge.expiresAt)
                s.setInstant(5, challenge.consumedAt)
                s.setInt(6, challenge.globalProofOfWorkDifficulty)
                s.executeUpdate()
            }
        }
    }

    override fun consume(
        challengeId: ChallengeId,
        at: Instant
    ): SignupChallenge? {
        val sql = """
            UPDATE anchorline_signup_challenges
            SET consumed_at = ?
            WHERE challenge_id = ? AND consumed_at IS NULL AND expires_at > ?
            RETURNING challenge_id, challenge, issued_at, expires_at, consumed_at, global_pow_difficulty
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, challengeId.value)
                s.setInstant(3, at)
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return SignupChallenge(
                        challengeId = ChallengeId(rs.uuid("challenge_id")),
                        challengeBytes = rs.getBytes("challenge").copyOf(),
                        issuedAt = rs.instant("issued_at"),
                        expiresAt = rs.instant("expires_at"),
                        globalProofOfWorkDifficulty = rs.getInt("global_pow_difficulty"),
                        consumedAt = rs.maybeInstant("consumed_at")
                    )
                }
            }
        }
    }

    override fun deleteExpired(now: Instant): Int {
        val sql = "DELETE FROM anchorline_signup_challenges WHERE expires_at <= ?"
        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, now)
                return s.executeUpdate()
            }
        }
    }
}

class PostgresSignupTicketRepository(
    private val dataSource: DataSource
) : SignupTicketRepository {
    override fun save(ticket: SignupTicket) {
        val sql = """
            INSERT INTO anchorline_signup_tickets
                (ticket_id, issued_at, expires_at, consumed_at,
                 passkey_registration_remaining, age_proof_remaining, account_shell_remaining)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, ticket.ticketId.value)
                s.setInstant(2, ticket.issuedAt)
                s.setInstant(3, ticket.expiresAt)
                val fullyConsumed =
                    ticket.passkeyRegistrationRemaining <= 0 &&
                        ticket.ageProofRemaining <= 0 &&
                        ticket.accountShellRemaining <= 0
                s.setInstant(4, if (fullyConsumed) ticket.expiresAt else null)
                s.setInt(5, ticket.passkeyRegistrationRemaining)
                s.setInt(6, ticket.ageProofRemaining)
                s.setInt(7, ticket.accountShellRemaining)
                s.executeUpdate()
            }
        }
    }

    override fun deleteExpired(now: Instant): Int {
        val sql = "DELETE FROM anchorline_signup_tickets WHERE expires_at <= ?"
        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, now)
                return s.executeUpdate()
            }
        }
    }

    override fun consumePasskeyRegistration(
        ticketId: SignupTicketId,
        at: Instant
    ): Boolean = consumeCapability(ticketId.value, "passkey_registration_remaining", at)

    override fun consumeAgeProof(
        ticketId: SignupTicketId,
        at: Instant
    ): Boolean = consumeCapability(ticketId.value, "age_proof_remaining", at)

    override fun consumeAccountShell(
        ticketId: SignupTicketId,
        at: Instant
    ): Boolean = consumeCapability(ticketId.value, "account_shell_remaining", at)

    private fun consumeCapability(
        ticketId: UUID,
        column: String,
        at: Instant
    ): Boolean {
        // Column name is from a fixed internal allowlist, never caller input.
        val sql = """
            UPDATE anchorline_signup_tickets
            SET $column = $column - 1
            WHERE ticket_id = ? AND expires_at > ? AND $column > 0
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, ticketId)
                s.setInstant(2, at)
                return s.executeUpdate() == 1
            }
        }
    }
}

class PostgresPasskeyRepository(
    private val dataSource: DataSource
) : PasskeyRepository {
    override fun save(passkey: RegisteredPasskey) {
        val sql = """
            INSERT INTO anchorline_passkeys
                (account_id, credential_id, public_key, created_at, status, generation, revoked_at, replaced_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, passkey.accountId.value)
                s.setBytes(2, passkey.credentialId.value.copyOf())
                s.setBytes(3, passkey.publicKeyCose.copyOf())
                s.setInstant(4, passkey.createdAt)
                s.setString(5, passkey.status.name)
                s.setLong(6, passkey.generation)
                s.setInstant(7, passkey.revokedAt)
                s.setInstant(8, passkey.replacedAt)
                s.executeUpdate()
            }
        }
    }

    override fun find(
        accountId: AccountId,
        credentialId: CredentialId
    ): RegisteredPasskey? {
        val sql = SELECT_PASSKEY + " WHERE account_id = ? AND credential_id = ?"
        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, accountId.value)
                s.setBytes(2, credentialId.value.copyOf())
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return readPasskey(rs)
                }
            }
        }
    }

    override fun listActive(accountId: AccountId): List<RegisteredPasskey> {
        val sql = SELECT_PASSKEY +
            " WHERE account_id = ? AND status = 'ACTIVE' ORDER BY created_at"
        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, accountId.value)
                s.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(readPasskey(rs))
                    }
                }
            }
        }
    }

    override fun revokeAndIncrement(
        accountId: AccountId,
        credentialId: CredentialId,
        revokedAt: Instant
    ): RegisteredPasskey? {
        val sql = """
            UPDATE anchorline_passkeys
            SET status = 'REVOKED',
                generation = generation + 1,
                revoked_at = ?
            WHERE account_id = ? AND credential_id = ?
            RETURNING account_id, credential_id, public_key, created_at, status, generation, revoked_at, replaced_at
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, revokedAt)
                s.setUuid(2, accountId.value)
                s.setBytes(3, credentialId.value.copyOf())
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return readPasskey(rs)
                }
            }
        }
    }

    companion object {
        private const val SELECT_PASSKEY = """
            SELECT account_id, credential_id, public_key, created_at, status, generation, revoked_at, replaced_at
            FROM anchorline_passkeys
        """.trimIndent()

        fun readPasskey(rs: ResultSet): RegisteredPasskey =
            RegisteredPasskey(
                accountId = AccountId(rs.uuid("account_id")),
                credentialId = CredentialId(rs.getBytes("credential_id").copyOf()),
                publicKeyCose = rs.getBytes("public_key").copyOf(),
                createdAt = rs.instant("created_at"),
                status = CredentialStatus.valueOf(rs.getString("status")),
                generation = rs.getLong("generation"),
                revokedAt = rs.maybeInstant("revoked_at"),
                replacedAt = rs.maybeInstant("replaced_at")
            )
    }
}

class PostgresAccountSecurityStateRepository(
    private val dataSource: DataSource
) : AccountSecurityStateRepository {
    override fun find(accountId: AccountId): AccountSecurityState? {
        val sql = """
            SELECT account_id, status, session_generation, updated_at
            FROM anchorline_account_security_state
            WHERE account_id = ?
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, accountId.value)
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return AccountSecurityState(
                        accountId = AccountId(rs.uuid("account_id")),
                        status = AccountStatus.valueOf(rs.getString("status")),
                        sessionGeneration = rs.getLong("session_generation"),
                        updatedAt = rs.instant("updated_at")
                    )
                }
            }
        }
    }
}

class PostgresCredentialSecurityStateRepository(
    private val dataSource: DataSource
) : CredentialSecurityStateRepository {
    override fun find(
        accountId: AccountId,
        credentialId: CredentialId
    ): CredentialSecurityState? {
        val sql = """
            SELECT account_id, credential_id, status, generation, updated_at
            FROM anchorline_credential_security_state
            WHERE account_id = ? AND credential_id = ?
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, accountId.value)
                s.setBytes(2, credentialId.value.copyOf())
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return CredentialSecurityState(
                        accountId = AccountId(rs.uuid("account_id")),
                        credentialId = CredentialId(rs.getBytes("credential_id").copyOf()),
                        status = CredentialStatus.valueOf(rs.getString("status")),
                        generation = rs.getLong("generation"),
                        updatedAt = rs.instant("updated_at")
                    )
                }
            }
        }
    }

    override fun revokeAndIncrement(
        accountId: AccountId,
        credentialId: CredentialId,
        at: Instant
    ): CredentialSecurityState? {
        var updated: CredentialSecurityState? = null

        dataSource.inTransaction { connection ->
            val sql = """
                UPDATE anchorline_credential_security_state
                SET status = 'REVOKED', generation = generation + 1, updated_at = ?
                WHERE account_id = ? AND credential_id = ?
                RETURNING account_id, credential_id, status, generation, updated_at
            """.trimIndent()

            connection.prepareStatement(sql).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, accountId.value)
                s.setBytes(3, credentialId.value.copyOf())
                s.executeQuery().use { rs ->
                    if (rs.next()) {
                        updated = CredentialSecurityState(
                            accountId = AccountId(rs.uuid("account_id")),
                            credentialId = CredentialId(rs.getBytes("credential_id").copyOf()),
                            status = CredentialStatus.valueOf(rs.getString("status")),
                            generation = rs.getLong("generation"),
                            updatedAt = rs.instant("updated_at")
                        )
                    }
                }
            }

            val revokeSessions = """
                UPDATE anchorline_sessions
                SET status = 'REVOKED', revoked_at = ?
                WHERE account_id = ? AND credential_id = ? AND status = 'ACTIVE'
            """.trimIndent()

            connection.prepareStatement(revokeSessions).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, accountId.value)
                s.setBytes(3, credentialId.value.copyOf())
                s.executeUpdate()
            }
        }

        return updated
    }
}

class PostgresCredentialBoundSessionRepository(
    private val dataSource: DataSource
) : CredentialBoundSessionRepository, AccountSessionRevocationPort {
    override fun save(session: CredentialBoundSession) {
        val sql = """
            INSERT INTO anchorline_sessions
                (session_id, account_id, credential_id, credential_generation_at_issue,
                 account_session_generation_at_issue, issued_at, last_fresh_auth_at,
                 expires_at, status, sender_constraint_public_key_hash, revoked_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, session.sessionId.value)
                s.setUuid(2, session.accountId.value)
                s.setBytes(3, session.authenticatingCredentialId.value.copyOf())
                s.setLong(4, session.credentialGenerationAtIssue)
                s.setLong(5, session.accountSessionGenerationAtIssue)
                s.setInstant(6, session.issuedAt)
                s.setInstant(7, session.lastFreshAuthenticationAt)
                s.setInstant(8, session.expiresAt)
                s.setString(9, session.status.name)
                s.setBytes(10, session.senderConstraintPublicKeyHash?.copyOf())
                s.setInstant(11, session.revokedAt)
                s.executeUpdate()
            }
        }
    }

    override fun find(sessionId: SessionId): CredentialBoundSession? {
        val sql = SELECT_SESSION + " WHERE session_id = ?"
        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, sessionId.value)
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return readSession(rs)
                }
            }
        }
    }

    override fun revoke(sessionId: SessionId, at: Instant): Boolean =
        transition(sessionId.value, at, "ACTIVE", "REVOKED", revokeStamp = true)

    override fun expire(sessionId: SessionId, at: Instant): Boolean =
        transition(sessionId.value, at, "ACTIVE", "EXPIRED", revokeStamp = false)

    override fun revokeByCredential(
        accountId: AccountId,
        credentialId: CredentialId,
        at: Instant
    ): Int {
        val sql = """
            UPDATE anchorline_sessions
            SET status = 'REVOKED', revoked_at = ?
            WHERE account_id = ? AND credential_id = ? AND status = 'ACTIVE'
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, accountId.value)
                s.setBytes(3, credentialId.value.copyOf())
                return s.executeUpdate()
            }
        }
    }

    override fun revokeAllForAccount(accountId: AccountId, at: Instant): Int {
        val sql = """
            UPDATE anchorline_sessions
            SET status = 'REVOKED', revoked_at = ?
            WHERE account_id = ? AND status = 'ACTIVE'
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, accountId.value)
                return s.executeUpdate()
            }
        }
    }

    /**
     * Implements AccountSessionRevocationPort so recovery revocation can be
     * combined with other writes in one transaction when needed.
     */
    override fun revokeAllForAccount(accountId: AccountId, revokedAt: Instant): Int =
        revokeAllForAccount(accountId, revokedAt)

    override fun deleteExpired(now: Instant): Int {
        val sql = "DELETE FROM anchorline_sessions WHERE expires_at <= ?"
        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, now)
                return s.executeUpdate()
            }
        }
    }

    private fun transition(
        sessionId: UUID,
        at: Instant,
        from: String,
        to: String,
        revokeStamp: Boolean
    ): Boolean {
        val stampClause = if (revokeStamp) ", revoked_at = ?" else ""
        val sql = """
            UPDATE anchorline_sessions
            SET status = '$to'$stampClause
            WHERE session_id = ? AND status = '$from'
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                var index = 1
                if (revokeStamp) {
                    s.setInstant(index++, at)
                }
                s.setUuid(index, sessionId)
                return s.executeUpdate() == 1
            }
        }
    }

    companion object {
        private const val SELECT_SESSION = """
            SELECT session_id, account_id, credential_id, credential_generation_at_issue,
                   account_session_generation_at_issue, issued_at, last_fresh_auth_at,
                   expires_at, status, sender_constraint_public_key_hash, revoked_at
            FROM anchorline_sessions
        """.trimIndent()

        fun readSession(rs: ResultSet): CredentialBoundSession =
            CredentialBoundSession(
                sessionId = SessionId(rs.uuid("session_id")),
                accountId = AccountId(rs.uuid("account_id")),
                authenticatingCredentialId =
                    CredentialId(rs.getBytes("credential_id").copyOf()),
                credentialGenerationAtIssue = rs.getLong("credential_generation_at_issue"),
                accountSessionGenerationAtIssue =
                    rs.getLong("account_session_generation_at_issue"),
                issuedAt = rs.instant("issued_at"),
                lastFreshAuthenticationAt = rs.instant("last_fresh_auth_at"),
                expiresAt = rs.instant("expires_at"),
                status = SessionStatus.valueOf(rs.getString("status")),
                senderConstraintPublicKeyHash =
                    rs.getBytes("sender_constraint_public_key_hash")?.copyOf(),
                revokedAt = rs.maybeInstant("revoked_at")
            )
    }
}

class PostgresActionAuthorizationRepository(
    private val dataSource: DataSource
) : ActionAuthorizationRepository {
    override fun save(value: ActionAuthorization) {
        val sql = """
            INSERT INTO anchorline_action_authorizations
                (authorization_id, account_id, session_id, action, issued_at, expires_at, consumed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, value.authorizationId.value)
                s.setUuid(2, value.accountId.value)
                s.setUuid(3, value.sessionId.value)
                s.setString(4, value.action.name)
                s.setInstant(5, value.issuedAt)
                s.setInstant(6, value.expiresAt)
                s.setInstant(7, value.consumedAt)
                s.executeUpdate()
            }
        }
    }

    override fun consume(
        authorizationId: ActionAuthorizationId,
        at: Instant
    ): ActionAuthorization? {
        val sql = """
            UPDATE anchorline_action_authorizations
            SET consumed_at = ?
            WHERE authorization_id = ? AND consumed_at IS NULL AND expires_at > ?
            RETURNING authorization_id, account_id, session_id, action, issued_at, expires_at, consumed_at
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, authorizationId.value)
                s.setInstant(3, at)
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return ActionAuthorization(
                        authorizationId = ActionAuthorizationId(rs.uuid("authorization_id")),
                        accountId = AccountId(rs.uuid("account_id")),
                        sessionId = SessionId(rs.uuid("session_id")),
                        action = SensitiveAction.valueOf(rs.getString("action")),
                        issuedAt = rs.instant("issued_at"),
                        expiresAt = rs.instant("expires_at"),
                        consumedAt = rs.maybeInstant("consumed_at")
                    )
                }
            }
        }
    }

    override fun deleteExpired(now: Instant): Int {
        val sql = "DELETE FROM anchorline_action_authorizations WHERE expires_at <= ?"
        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, now)
                return s.executeUpdate()
            }
        }
    }
}

class PostgresAgeProofChallengeRepository(
    private val dataSource: DataSource
) : AgeProofChallengeRepository {
    override fun save(challenge: AgeProofChallenge) {
        val sql = """
            INSERT INTO anchorline_age_proof_challenges
                (challenge_id, account_id, audience, nonce, issued_at, expires_at, consumed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, challenge.challengeId.value)
                s.setUuid(2, challenge.accountId.value)
                s.setString(3, challenge.audience)
                s.setBytes(4, challenge.nonce.copyOf())
                s.setInstant(5, challenge.issuedAt)
                s.setInstant(6, challenge.expiresAt)
                s.setInstant(7, challenge.consumedAt)
                s.executeUpdate()
            }
        }
    }

    override fun consume(
        challengeId: ChallengeId,
        at: Instant
    ): AgeProofChallenge? {
        val sql = """
            UPDATE anchorline_age_proof_challenges
            SET consumed_at = ?
            WHERE challenge_id = ? AND consumed_at IS NULL AND expires_at > ?
            RETURNING challenge_id, account_id, audience, nonce, issued_at, expires_at, consumed_at
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, challengeId.value)
                s.setInstant(3, at)
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return AgeProofChallenge(
                        challengeId = ChallengeId(rs.uuid("challenge_id")),
                        accountId = AccountId(rs.uuid("account_id")),
                        audience = rs.getString("audience"),
                        nonce = rs.getBytes("nonce").copyOf(),
                        issuedAt = rs.instant("issued_at"),
                        expiresAt = rs.instant("expires_at"),
                        consumedAt = rs.maybeInstant("consumed_at")
                    )
                }
            }
        }
    }

    override fun deleteExpired(now: Instant): Int {
        val sql = "DELETE FROM anchorline_age_proof_challenges WHERE expires_at <= ?"
        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, now)
                return s.executeUpdate()
            }
        }
    }
}

class PostgresAgeEligibilityGrantRepository(
    private val dataSource: DataSource
) : AgeEligibilityGrantRepository {
    override fun save(grant: AgeEligibilityGrant) {
        val sql = """
            INSERT INTO anchorline_age_eligibility_grants
                (account_id, eligibility_state, expires_at, policy_version, updated_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (account_id) DO UPDATE SET
                eligibility_state = EXCLUDED.eligibility_state,
                expires_at = EXCLUDED.expires_at,
                policy_version = EXCLUDED.policy_version,
                updated_at = EXCLUDED.updated_at
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, grant.accountId.value)
                s.setString(2, grant.state.name)
                s.setInstant(3, grant.expiresAt)
                s.setString(4, grant.policyVersion)
                s.setInstant(5, grant.updatedAt)
                s.executeUpdate()
            }
        }
    }

    override fun find(accountId: AccountId): AgeEligibilityGrant? {
        val sql = """
            SELECT account_id, eligibility_state, expires_at, policy_version, updated_at
            FROM anchorline_age_eligibility_grants
            WHERE account_id = ?
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, accountId.value)
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return AgeEligibilityGrant(
                        accountId = AccountId(rs.uuid("account_id")),
                        state = AgeEligibilityState.valueOf(rs.getString("eligibility_state")),
                        expiresAt = rs.maybeInstant("expires_at"),
                        policyVersion = rs.getString("policy_version"),
                        updatedAt = rs.instant("updated_at")
                    )
                }
            }
        }
    }
}

class PostgresRecoveryCaseRepository(
    private val dataSource: DataSource
) : RecoveryCaseRepository {
    override fun save(value: RecoveryCase) {
        val sql = """
            INSERT INTO anchorline_recovery_cases
                (recovery_id, account_id, state, created_at, expires_at, authorized_at,
                 hold_ends_at, completed_at, cancelled_at, authorization_method)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, value.recoveryId.value)
                s.setUuid(2, value.accountId.value)
                s.setString(3, value.state.name)
                s.setInstant(4, value.createdAt)
                s.setInstant(5, value.expiresAt)
                s.setInstant(6, value.authorizedAt)
                s.setInstant(7, value.holdEndsAt)
                s.setInstant(8, value.completedAt)
                s.setInstant(9, value.cancelledAt)
                s.setString(10, value.authorizationMethod?.name)
                s.executeUpdate()
            }
        }
    }

    override fun find(recoveryId: RecoveryId): RecoveryCase? {
        val sql = SELECT_CASE + " WHERE recovery_id = ?"
        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, recoveryId.value)
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return readCase(rs)
                }
            }
        }
    }

    /**
     * Compare-and-set on the persisted state row: the transform runs against a
     * locked row and the final UPDATE re-checks the state in its WHERE clause,
     * so HOLD -> COMPLETED and HOLD -> CANCELLED cannot both succeed.
     */
    override fun update(
        recoveryId: RecoveryId,
        transform: (RecoveryCase) -> RecoveryCase
    ): RecoveryCase? {
        var result: RecoveryCase? = null

        dataSource.inTransaction { connection ->
            val selectSql = SELECT_CASE + " WHERE recovery_id = ? FOR UPDATE"
            val existing = connection.prepareStatement(selectSql).use { s ->
                s.setUuid(1, recoveryId.value)
                s.executeQuery().use { rs -> if (rs.next()) readCase(rs) else null }
            } ?: return null

            val updated = transform(existing)
            val expectedState = existing.state.name

            val sql = """
                UPDATE anchorline_recovery_cases
                SET state = ?, authorized_at = ?, hold_ends_at = ?, completed_at = ?,
                    cancelled_at = ?, authorization_method = ?
                WHERE recovery_id = ? AND state = ?
            """.trimIndent()

            val affected = connection.prepareStatement(sql).use { s ->
                s.setString(1, updated.state.name)
                s.setInstant(2, updated.authorizedAt)
                s.setInstant(3, updated.holdEndsAt)
                s.setInstant(4, updated.completedAt)
                s.setInstant(5, updated.cancelledAt)
                s.setString(6, updated.authorizationMethod?.name)
                s.setUuid(7, recoveryId.value)
                s.setString(8, expectedState)
                s.executeUpdate()
            }

            if (affected != 1) {
                throw IllegalStateException("Recovery case state changed concurrently.")
            }

            result = updated
        }

        return result
    }

    override fun deleteExpired(now: Instant): Int {
        val sql = "DELETE FROM anchorline_recovery_cases WHERE expires_at <= ?"
        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, now)
                return s.executeUpdate()
            }
        }
    }

    companion object {
        private const val SELECT_CASE = """
            SELECT recovery_id, account_id, state, created_at, expires_at, authorized_at,
                   hold_ends_at, completed_at, cancelled_at, authorization_method
            FROM anchorline_recovery_cases
        """.trimIndent()

        fun readCase(rs: ResultSet): RecoveryCase =
            RecoveryCase(
                recoveryId = RecoveryId(rs.uuid("recovery_id")),
                accountId = AccountId(rs.uuid("account_id")),
                state = RecoveryState.valueOf(rs.getString("state")),
                createdAt = rs.instant("created_at"),
                expiresAt = rs.instant("expires_at"),
                authorizedAt = rs.maybeInstant("authorized_at"),
                holdEndsAt = rs.maybeInstant("hold_ends_at"),
                authorizationMethod = rs.getString("authorization_method")
                    ?.let { RecoveryAuthorizationMethod.valueOf(it) },
                completedAt = rs.maybeInstant("completed_at"),
                cancelledAt = rs.maybeInstant("cancelled_at")
            )
    }
}

class PostgresRecoveryChallengeRepository(
    private val dataSource: DataSource
) : RecoveryChallengeRepository {
    override fun save(value: RecoveryChallenge) {
        val sql = """
            INSERT INTO anchorline_recovery_challenges
                (recovery_id, challenge, issued_at, expires_at, consumed_at)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, value.recoveryId.value)
                s.setBytes(2, value.challenge.copyOf())
                s.setInstant(3, value.issuedAt)
                s.setInstant(4, value.expiresAt)
                s.setInstant(5, value.consumedAt)
                s.executeUpdate()
            }
        }
    }

    override fun consume(
        recoveryId: RecoveryId,
        at: Instant
    ): RecoveryChallenge? {
        val sql = """
            UPDATE anchorline_recovery_challenges
            SET consumed_at = ?
            WHERE recovery_id = ? AND consumed_at IS NULL AND expires_at > ?
            RETURNING recovery_id, challenge, issued_at, expires_at, consumed_at
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, recoveryId.value)
                s.setInstant(3, at)
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return RecoveryChallenge(
                        recoveryId = RecoveryId(rs.uuid("recovery_id")),
                        challenge = rs.getBytes("challenge").copyOf(),
                        issuedAt = rs.instant("issued_at"),
                        expiresAt = rs.instant("expires_at"),
                        consumedAt = rs.maybeInstant("consumed_at")
                    )
                }
            }
        }
    }

    override fun deleteExpired(now: Instant): Int {
        val sql = "DELETE FROM anchorline_recovery_challenges WHERE expires_at <= ?"
        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, now)
                return s.executeUpdate()
            }
        }
    }
}

class PostgresRecoveryCodeRepository(
    private val dataSource: DataSource,
    private val secureRandom: SecureRandom
) : RecoveryCodeRepository {
    override fun save(verifier: RecoveryCodeVerifier) {
        val sql = """
            INSERT INTO anchorline_recovery_code_verifiers
                (account_id, verifier, salt, created_at, used_at, revoked_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, verifier.accountId.value)
                s.setBytes(2, verifier.verifier.copyOf())
                s.setBytes(3, verifier.salt.copyOf())
                s.setInstant(4, verifier.createdAt)
                s.setInstant(5, verifier.usedAt)
                s.setInstant(6, verifier.revokedAt)
                s.executeUpdate()
            }
        }
    }

    override fun findActive(accountId: AccountId): List<RecoveryCodeVerifier> {
        val sql = """
            SELECT account_id, verifier, salt, created_at, used_at, revoked_at
            FROM anchorline_recovery_code_verifiers
            WHERE account_id = ? AND used_at IS NULL AND revoked_at IS NULL
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, accountId.value)
                s.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                RecoveryCodeVerifier(
                                    accountId = AccountId(rs.uuid("account_id")),
                                    verifier = rs.getBytes("verifier").copyOf(),
                                    salt = rs.getBytes("salt").copyOf(),
                                    createdAt = rs.instant("created_at"),
                                    usedAt = rs.maybeInstant("used_at"),
                                    revokedAt = rs.maybeInstant("revoked_at")
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * One-time use: the winning row is marked used and all other active codes
     * for the account are revoked inside a single transaction. The boolean
     * result never reveals which condition failed.
     */
    override fun consumeOneAndRevokeRest(
        accountId: AccountId,
        code: CharArray,
        hasher: RecoveryCodeHasher,
        at: Instant
    ): Boolean {
        var matchedIndex: Int? = null
        var total = 0

        dataSource.inTransaction { connection ->
            val selectSql = """
                SELECT verifier, salt
                FROM anchorline_recovery_code_verifiers
                WHERE account_id = ? AND used_at IS NULL AND revoked_at IS NULL
                FOR UPDATE
            """.trimIndent()

            val rows = connection.prepareStatement(selectSql).use { s ->
                s.setUuid(1, accountId.value)
                s.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                Pair(
                                    rs.getBytes("verifier").copyOf(),
                                    rs.getBytes("salt").copyOf()
                                )
                            )
                        }
                    }
                }
            }

            total = rows.size
            rows.forEachIndexed { index, (verifier, salt) ->
                if (matchedIndex == null && hasher.verify(code, salt, verifier)) {
                    matchedIndex = index
                }
            }

            if (matchedIndex == null) {
                return@inTransaction
            }

            // Match the winning row by exact verifier/salt bytes; no identity columns exist.
            val winner = rows[matchedIndex!!]

            val markUsedSql = """
                UPDATE anchorline_recovery_code_verifiers
                SET used_at = ?
                WHERE account_id = ? AND verifier = ? AND salt = ?
                  AND used_at IS NULL AND revoked_at IS NULL
            """.trimIndent()

            connection.prepareStatement(markUsedSql).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, accountId.value)
                s.setBytes(3, winner.first)
                s.setBytes(4, winner.second)
                if (s.executeUpdate() != 1) {
                    throw IllegalStateException("Recovery code consumption race lost.")
                }
            }

            val revokeRestSql = """
                UPDATE anchorline_recovery_code_verifiers
                SET revoked_at = ?
                WHERE account_id = ? AND used_at IS NULL AND revoked_at IS NULL
            """.trimIndent()

            connection.prepareStatement(revokeRestSql).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, accountId.value)
                s.executeUpdate()
            }
        }

        return matchedIndex != null && total >= 1
    }

    override fun revokeAll(accountId: AccountId, at: Instant): Int {
        val sql = """
            UPDATE anchorline_recovery_code_verifiers
            SET revoked_at = ?
            WHERE account_id = ? AND used_at IS NULL AND revoked_at IS NULL
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, accountId.value)
                return s.executeUpdate()
            }
        }
    }
}

class PostgresFederatedLoginBindingRepository(
    private val dataSource: DataSource,
    private val cipher: FederationSubjectCipher
) : FederatedLoginBindingRepository {
    override fun save(binding: FederatedLoginBinding) {
        val sql = """
            INSERT INTO anchorline_federated_login_bindings
                (binding_id, account_id, issuer_uri, encrypted_subject, encryption_key_version,
                 client_id, created_at, last_successful_use_at, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, binding.bindingId.value)
                s.setUuid(2, binding.accountId.value)
                s.setString(3, binding.issuerUri)
                s.setBytes(4, binding.encryptedPairwiseSubject.ciphertext.copyOf())
                s.setInt(5, binding.encryptedPairwiseSubject.keyVersion)
                s.setString(6, binding.clientId)
                s.setInstant(7, binding.createdAt)
                s.setInstant(8, binding.lastSuccessfulUseAt)
                s.setString(9, binding.status.name)
                s.executeUpdate()
            }
        }
    }

    override fun findByIssuerAndPairwiseSubject(
        issuerUri: String,
        subject: ByteArray,
        cipher: FederationSubjectCipher
    ): FederatedLoginBinding? {
        val sql = """
            SELECT binding_id, account_id, issuer_uri, encrypted_subject, encryption_key_version,
                   client_id, created_at, last_successful_use_at, status
            FROM anchorline_federated_login_bindings
            WHERE issuer_uri = ? AND status = 'ACTIVE'
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setString(1, issuerUri)
                s.executeQuery().use { rs ->
                    while (rs.next()) {
                        val binding = readBinding(rs)
                        val decrypted = cipher.decrypt(binding.encryptedPairwiseSubject)
                        if (CryptoSupport.constantTimeEquals(decrypted, subject)) {
                            return binding
                        }
                    }
                    return null
                }
            }
        }
    }

    override fun findForAccount(
        accountId: AccountId,
        bindingId: FederationBindingId
    ): FederatedLoginBinding? {
        val sql = """
            SELECT binding_id, account_id, issuer_uri, encrypted_subject, encryption_key_version,
                   client_id, created_at, last_successful_use_at, status
            FROM anchorline_federated_login_bindings
            WHERE account_id = ? AND binding_id = ?
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, accountId.value)
                s.setUuid(2, bindingId.value)
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return readBinding(rs)
                }
            }
        }
    }

    override fun disable(
        accountId: AccountId,
        bindingId: FederationBindingId,
        at: Instant
    ): Boolean {
        val sql = """
            UPDATE anchorline_federated_login_bindings
            SET status = 'DISABLED'
            WHERE account_id = ? AND binding_id = ? AND status = 'ACTIVE'
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, accountId.value)
                s.setUuid(2, bindingId.value)
                return s.executeUpdate() == 1
            }
        }
    }

    private fun readBinding(rs: ResultSet): FederatedLoginBinding =
        FederatedLoginBinding(
            bindingId = FederationBindingId(rs.uuid("binding_id")),
            accountId = AccountId(rs.uuid("account_id")),
            issuerUri = rs.getString("issuer_uri"),
            encryptedPairwiseSubject = EncryptedValue(
                ciphertext = rs.getBytes("encrypted_subject").copyOf(),
                keyVersion = rs.getInt("encryption_key_version")
            ),
            clientId = rs.getString("client_id"),
            createdAt = rs.instant("created_at"),
            lastSuccessfulUseAt = rs.maybeInstant("last_successful_use_at"),
            status = FederationBindingStatus.valueOf(rs.getString("status"))
        )
}

class PostgresRegistrationAdmissionChallengeRepository(
    private val dataSource: DataSource
) : RegistrationAdmissionChallengeRepository {
    override fun findUnused(
        challengeId: ChallengeId,
        now: Instant
    ): RegistrationAdmissionChallenge? {
        val sql = """
            SELECT challenge_id, challenge, issued_at, expires_at, consumed_at
            FROM anchorline_signup_challenges
            WHERE challenge_id = ? AND consumed_at IS NULL
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setUuid(1, challengeId.value)
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return RegistrationAdmissionChallenge(
                        challengeId = ChallengeId(rs.uuid("challenge_id")),
                        challenge = rs.getBytes("challenge").copyOf(),
                        issuedAt = rs.instant("issued_at"),
                        expiresAt = rs.instant("expires_at"),
                        consumedAt = rs.maybeInstant("consumed_at")
                    )
                }
            }
        }
    }

    override fun consume(
        challengeId: ChallengeId,
        at: Instant
    ): RegistrationAdmissionChallenge? {
        val sql = """
            UPDATE anchorline_signup_challenges
            SET consumed_at = ?
            WHERE challenge_id = ? AND consumed_at IS NULL AND expires_at > ?
            RETURNING challenge_id, challenge, issued_at, expires_at, consumed_at
        """.trimIndent()

        dataSource.connection.use { c ->
            c.prepareStatement(sql).use { s ->
                s.setInstant(1, at)
                s.setUuid(2, challengeId.value)
                s.setInstant(3, at)
                s.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    return RegistrationAdmissionChallenge(
                        challengeId = ChallengeId(rs.uuid("challenge_id")),
                        challenge = rs.getBytes("challenge").copyOf(),
                        issuedAt = rs.instant("issued_at"),
                        expiresAt = rs.instant("expires_at"),
                        consumedAt = rs.maybeInstant("consumed_at")
                    )
                }
            }
        }
    }
}

/**
 * Writes a recovered replacement passkey through the durable passkey table.
 * The recovery timestamp comes from RecoveryService's injected clock so no
 * wall-clock source is introduced here.
 */
class PostgresRecoveryCredentialWriteAdapter(
    private val passkeys: PasskeyRepository,
    private val clock: Clock
) : RecoveryCredentialWritePort {
    override fun addRecoveredCredentialAtomically(
        accountId: AccountId,
        recoveryId: RecoveryId,
        credentialId: CredentialId,
        publicKeyCose: ByteArray
    ) {
        passkeys.save(
            RegisteredPasskey(
                accountId = accountId,
                credentialId = credentialId,
                publicKeyCose = publicKeyCose.copyOf(),
                createdAt = clock.instant(),
                status = CredentialStatus.ACTIVE,
                generation = 1L
            )
        )
    }
}
