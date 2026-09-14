package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GoogleUserData(
    val email: String,
    val displayName: String,
    val idToken: String? = null,
    val photoUrl: String? = null
)

class AuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(webClientId: String? = null): Result<GoogleUserData> {
        return withContext(Dispatchers.IO) {
            try {
                // If a webClientId is provided or configured
                val clientId = webClientId?.takeIf { it.isNotBlank() } ?: "default_client_id"
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    Result.success(
                        GoogleUserData(
                            email = googleIdTokenCredential.id,
                            displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id.substringBefore("@"),
                            idToken = googleIdTokenCredential.idToken,
                            photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                        )
                    )
                } else {
                    Result.failure(IllegalStateException("Unsupported credential type: ${credential.type}"))
                }
            } catch (e: Exception) {
                Log.w("AuthManager", "CredentialManager sign in failed or requires Web Client ID: ${e.message}")
                Result.failure(e)
            }
        }
    }
}
