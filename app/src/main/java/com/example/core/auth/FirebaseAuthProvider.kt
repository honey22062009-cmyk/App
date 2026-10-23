package com.example.core.auth

import android.content.Context
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth

/** Uses Firebase Email/Password auth when project values are configured at build time. */
object FirebaseAuthProvider {
    private const val APP_NAME = "strict_blocker_auth"

    fun getAuth(context: Context): FirebaseAuth? {
        return try {
            val defaultApp = FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            val app = defaultApp ?: run {
                val apiKey = BuildConfig.FIREBASE_API_KEY
                val appId = BuildConfig.FIREBASE_APP_ID
                val projectId = BuildConfig.FIREBASE_PROJECT_ID
                val senderId = BuildConfig.FIREBASE_MESSAGING_SENDER_ID
                if (apiKey.isBlank() || appId.isBlank() || projectId.isBlank() || senderId.isBlank()) {
                    return null
                }
                FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME } ?: FirebaseApp.initializeApp(
                    context,
                    FirebaseOptions.Builder().setApiKey(apiKey).setApplicationId(appId)
                        .setProjectId(projectId).setGcmSenderId(senderId).build(),
                    APP_NAME
                )
            }
            FirebaseAuth.getInstance(app)
        } catch (_: Exception) {
            null
        }
    }
}
