package com.example.data.preferences

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Device-local entry lock. It is not a cloud account or encryption for app data. */
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
            .putInt(KEY_FAILURES, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .putInt(KEY_LOCK_LEVEL, 0)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (!pin.matches(Regex("^[0-9]{6}$"))) return false
        if (remainingLockoutSeconds() > 0L) return false

        val salt = preferences.getString(KEY_SALT, null) ?: return false
        val expected = preferences.getString(KEY_HASH, null) ?: return false
        val matches = runCatching {
            val actualHash = derive(pin, Base64.decode(salt, Base64.NO_WRAP))
            MessageDigest.isEqual(Base64.decode(expected, Base64.NO_WRAP), actualHash)
        }.getOrDefault(false)

        if (matches) {
            preferences.edit()
                .putInt(KEY_FAILURES, 0)
                .putLong(KEY_LOCKED_UNTIL, 0L)
                .putInt(KEY_LOCK_LEVEL, 0)
                .apply()
            return true
        }

        val failures = preferences.getInt(KEY_FAILURES, 0) + 1
        if (failures >= MAX_ATTEMPTS) {
            val level = preferences.getInt(KEY_LOCK_LEVEL, 0).coerceIn(0, 4)
            val lockMillis = (BASE_LOCK_MILLIS * (1L shl level)).coerceAtMost(MAX_LOCK_MILLIS)
            preferences.edit()
                .putInt(KEY_FAILURES, 0)
                .putInt(KEY_LOCK_LEVEL, (level + 1).coerceAtMost(4))
                .putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + lockMillis)
                .apply()
        } else {
            preferences.edit().putInt(KEY_FAILURES, failures).apply()
        }
        return false
    }

    /** Remaining lockout time, rounded up to whole seconds for the sign-in UI. */
    fun remainingLockoutSeconds(): Long {
        val remaining = preferences.getLong(KEY_LOCKED_UNTIL, 0L) - System.currentTimeMillis()
        return if (remaining <= 0L) 0L else (remaining + 999L) / 1_000L
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
        const val KEY_FAILURES = "pin_failed_attempts"
        const val KEY_LOCKED_UNTIL = "pin_locked_until"
        const val KEY_LOCK_LEVEL = "pin_lock_level"
        const val MAX_ATTEMPTS = 5
        const val BASE_LOCK_MILLIS = 30_000L
        const val MAX_LOCK_MILLIS = 5 * 60_000L
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
    }
}
