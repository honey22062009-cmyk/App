package com.example.data.preferences

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Device-local app entry lock. This is not a cloud account or data encryption. */
class LocalProfileAuthStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "strict_blocker_local_profile_auth", Context.MODE_PRIVATE
    )

    fun hasPin(): Boolean =
        !preferences.getString(KEY_SALT, null).isNullOrBlank() &&
            !preferences.getString(KEY_HASH, null).isNullOrBlank()

    fun savePin(pin: String) {
        require(pin.matches(Regex("^[0-9]{6}$"))) { "PIN must contain exactly 6 digits." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derive(pin, salt)
        preferences.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (!pin.matches(Regex("^[0-9]{6}$"))) return false
        val salt = preferences.getString(KEY_SALT, null) ?: return false
        val expected = preferences.getString(KEY_HASH, null) ?: return false
        return runCatching {
            val actualHash = derive(pin, Base64.decode(salt, Base64.NO_WRAP))
            MessageDigest.isEqual(Base64.decode(expected, Base64.NO_WRAP), actualHash)
        }.getOrDefault(false)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val KEY_SALT = "pin_salt"
        const val KEY_HASH = "pin_hash"
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
    }
}
