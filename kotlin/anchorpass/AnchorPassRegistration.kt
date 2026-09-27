package org.hearthline.anchorpass

import android.content.Context
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.PublicKeyCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WebAuthnCreationOptions(
    val creationOptionsJson: String
)

data class WebAuthnRegistration(
    val credentialJson: String
)

interface AnchorPassRegistrationApi {
    suspend fun fetchRegistrationOptions(): WebAuthnCreationOptions
    suspend fun verifyRegistration(
        registration: WebAuthnRegistration
    ): RegisteredPasskey
}

data class RegisteredPasskey(
    val credentialId: String,
    val createdAt: String
)

class AnchorPassRegistration(
    private val context: Context,
    private val api: AnchorPassRegistrationApi
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun registerPasskey(): RegisteredPasskey =
        withContext(Dispatchers.Main) {
            val options = api.fetchRegistrationOptions()

            val request: CreateCredentialRequest =
                CreatePublicKeyCredentialRequest(
                    options.creationOptionsJson
                )

            val result = credentialManager.createCredential(
                context,
                request
            )

            val credential = result.data

            require(credential is PublicKeyCredential) {
                "Expected a WebAuthn public-key registration credential"
            }

            api.verifyRegistration(
                WebAuthnRegistration(
                    credentialJson =
                        credential.registrationResponseJson
                )
            )
        }
}
