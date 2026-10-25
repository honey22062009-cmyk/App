package com.example.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.example.data.preferences.SettingsPreferences
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class GoogleAccountProfile(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUri: String?
)

class GoogleAuthManager(
    context: Context,
    private val settings: SettingsPreferences
) {
    private val credentialManager = CredentialManager.create(context.applicationContext)

    suspend fun signIn(activity: Activity): Result<GoogleAccountProfile> {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank() || clientId == "CHANGE_ME") {
            return Result.failure(
                IllegalStateException(
                    "Google Sign-In is not configured. Set GOOGLE_WEB_CLIENT_ID in .env."
                )
            )
        }

        return try {
            // Smooth path for returning users: try an already-authorized Google
            // account first, then fall back to the full account picker.
            val response = try {
                requestCredential(
                    activity = activity,
                    clientId = clientId,
                    authorizedOnly = true,
                    autoSelect = true
                )
            } catch (_: NoCredentialException) {
                requestCredential(
                    activity = activity,
                    clientId = clientId,
                    authorizedOnly = false,
                    autoSelect = false
                )
            }

            val credential = response.credential
            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                val profile = GoogleAccountProfile(
                    id = google.id,
                    email = google.id,
                    displayName = google.displayName,
                    photoUri = google.profilePictureUri?.toString()
                )
                settings.saveSignedInAccount(
                    id = profile.id,
                    email = profile.email,
                    displayName = profile.displayName,
                    photoUri = profile.photoUri
                )
                Result.success(profile)
            } else {
                Result.failure(IllegalStateException("Unexpected Google credential type."))
            }
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private suspend fun requestCredential(
        activity: Activity,
        clientId: String,
        authorizedOnly: Boolean,
        autoSelect: Boolean
    ) = credentialManager.getCredential(
        request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(authorizedOnly)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(autoSelect)
                    .build()
            )
            .build(),
        context = activity
    )

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } finally {
            settings.signOutKeepRememberedAccount()
        }
    }

    suspend fun forgetRememberedAccount() {
        settings.forgetRememberedAccount()
    }
}
