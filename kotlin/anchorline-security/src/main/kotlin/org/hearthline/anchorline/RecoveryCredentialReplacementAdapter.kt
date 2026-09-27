package org.hearthline.anchorline

import java.time.Clock

interface RecoveryCredentialWritePort {
    fun addRecoveredCredentialAtomically(
        accountId: AccountId,
        recoveryId: RecoveryId,
        credentialId: CredentialId,
        publicKeyCose: ByteArray
    )
}

class RecoveryCredentialReplacementAdapter(
    private val credentialWriter: RecoveryCredentialWritePort
) : RecoveryCredentialReplacementService {
    override fun addReplacementCredential(
        accountId: AccountId,
        recoveryId: RecoveryId,
        registration: ReplacementPasskeyRegistration
    ) {
        credentialWriter.addRecoveredCredentialAtomically(
            accountId = accountId,
            recoveryId = recoveryId,
            credentialId = registration.credentialId,
            publicKeyCose = registration.publicKeyCose.copyOf()
        )
    }
}
