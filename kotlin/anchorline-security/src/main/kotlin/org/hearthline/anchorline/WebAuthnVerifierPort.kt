package org.hearthline.anchorline

/**
 * External WebAuthn ceremony verifier port. The module never implements
 * WebAuthn cryptography itself; production deployments inject an adapter.
 */
interface WebAuthnVerifierPort {
    fun verifyRegistration(
        expectedChallenge: ByteArray,
        clientDataJson: String,
        attestationObject: ByteArray
    ): VerifiedPublicRegistration

    fun verifyAssertion(
        accountId: AccountId,
        credentialId: CredentialId,
        expectedChallenge: ByteArray,
        clientDataJson: String,
        authenticatorData: ByteArray,
        signature: ByteArray,
        userPresent: Boolean,
        userVerified: Boolean
    ): Boolean
}
