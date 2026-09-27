package org.hearthline.anchorpass

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WebAuthnRequestOptions(
    val challengeJson: String
)

data class WebAuthnAssertion(
    val credentialJson: String
)

interface AnchorPassApi {
    suspend fun fetchSignInOptions(): WebAuthnRequestOptions
    suspend fun verifySignInAssertion(
        assertion: WebAuthnAssertion
    ): SessionResult
}

data class SessionResult(
    val sessionId: String,
    val expiresAt: String,
    val adultPolicyState: AdultPolicyState
)

enum class AdultPolicyState {
    ELIGIBLE_30_PLUS,
    REVIEW_REQUIRED,
    EXPIRED,
    NOT_ELIGIBLE
}

class AnchorPassCredentialManager(
    private val context: Context,
    private val api: AnchorPassApi
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): SessionResult = withContext(Dispatchers.Main) {
        val options = api.fetchSignInOptions()

        val passkeyOption = GetPublicKeyCredentialOption(
            options.challengeJson
        )

        val request = GetCredentialRequest(
            listOf(passkeyOption)
        )

        val result = credentialManager.getCredential(
            context,
            request
        )

        val credential = result.credential

        require(credential is PublicKeyCredential) {
            "Expected a WebAuthn public-key credential"
        }

        api.verifySignInAssertion(
            WebAuthnAssertion(
                credentialJson = credential.authenticationResponseJson
            )
        )
    }
}
