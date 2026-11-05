package com.example.data.backup

import android.util.Base64
import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.db.entity.BlockEventEntity
import com.example.data.db.entity.ManagedAppEntity
import com.example.data.db.entity.SchedulePeriodEntity
import com.example.data.preferences.SettingsPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * End-to-end encrypted backup. The recovery phrase is never uploaded or stored.
 * Cloud document contains only salt, nonce, ciphertext and schema metadata.
 */
class CloudBackupRepository(
    private val database: AppDatabase,
    private val settings: SettingsPreferences,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun createOrUpdateBackup(recoveryPhrase: CharArray): Result<Unit> = runCancellableCatching {
        requireRecoveryPhrase(recoveryPhrase)
        val uid = requireNotNull(auth.currentUser?.uid) {
            "Sign in before creating a cloud backup."
        }
        val payload = withContext(Dispatchers.IO) { createSnapshot() }
        val encrypted = withContext(Dispatchers.Default) {
            BackupCryptography.encrypt(payload.toByteArray(Charsets.UTF_8), recoveryPhrase)
        }
        require(encrypted.ciphertext.length < MAX_CIPHERTEXT_CHARS) {
            "Backup is too large for one secure cloud record."
        }
        firestore.collection("users").document(uid)
            .collection("backups").document("current")
            .set(
                mapOf(
                    "schemaVersion" to SCHEMA_VERSION,
                    "cipher" to "AES-256-GCM",
                    "kdf" to "PBKDF2-HMAC-SHA256-310000",
                    "salt" to encrypted.salt,
                    "nonce" to encrypted.nonce,
                    "ciphertext" to encrypted.ciphertext,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitResult()
    }

    suspend fun restoreBackup(recoveryPhrase: CharArray): Result<Unit> = runCancellableCatching {
        requireRecoveryPhrase(recoveryPhrase)
        val uid = requireNotNull(auth.currentUser?.uid) {
            "Sign in with the same account used for the backup."
        }
        val document = firestore.collection("users").document(uid)
            .collection("backups").document("current").get().awaitResult()
        check(document.exists()) { "No cloud backup exists for this account." }
        val schema = document.getLong("schemaVersion")?.toInt()
            ?: error("Backup schema is missing.")
        require(schema == SCHEMA_VERSION) { "This backup version is not supported by this app build." }
        val encrypted = EncryptedPayload(
            salt = requireNotNull(document.getString("salt")),
            nonce = requireNotNull(document.getString("nonce")),
            ciphertext = requireNotNull(document.getString("ciphertext"))
        )
        val clear = withContext(Dispatchers.Default) {
            BackupCryptography.decrypt(encrypted, recoveryPhrase)
        }
        try {
            withContext(Dispatchers.IO) { restoreSnapshot(String(clear, Charsets.UTF_8)) }
        } finally {
            clear.fill(0)
        }
    }

    private suspend fun createSnapshot(): String {
        val managed = database.managedAppDao().getAllManagedAppsFlow().first()
        val schedules = database.scheduleDao().getAllPeriodsFlow().first()
        val events = database.blockEventDao().getRecentBlockEventsFlow().first()
        val preferenceValues = settings.exportCloudSafePreferences()

        val typedPreferences = JSONObject()
        preferenceValues.forEach { (key, value) ->
            val entry = JSONObject()
            when (value) {
                is Boolean -> entry.put("type", "boolean").put("value", value)
                is Int -> entry.put("type", "int").put("value", value)
                is Long -> entry.put("type", "long").put("value", value)
                is Double -> entry.put("type", "long").put("value", value.toLong())
                is String -> entry.put("type", "string").put("value", value)
                is List<*> -> entry.put("type", "string_set").put("value", JSONArray(value.filterIsInstance<String>()))
            }
            typedPreferences.put(key, entry)
        }

        val managedJson = JSONArray().apply {
            managed.forEach {
                put(JSONObject().put("packageName", it.packageName).put("appName", it.appName)
                    .put("isManaged", it.isManaged).put("createdAt", it.createdAt))
            }
        }
        val schedulesJson = JSONArray().apply {
            schedules.forEach {
                put(JSONObject().put("id", it.id).put("appPackage", it.appPackage)
                    .put("dayOfWeek", it.dayOfWeek).put("startHour", it.startHour)
                    .put("startMinute", it.startMinute).put("endHour", it.endHour)
                    .put("endMinute", it.endMinute).put("isEnabled", it.isEnabled))
            }
        }
        val eventsJson = JSONArray().apply {
            events.forEach {
                put(JSONObject().put("id", it.id).put("appPackage", it.appPackage)
                    .put("appName", it.appName).put("timestamp", it.timestamp).put("reason", it.reason))
            }
        }
        return JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("preferences", typedPreferences)
            .put("managedApps", managedJson)
            .put("schedules", schedulesJson)
            .put("recentBlockEvents", eventsJson)
            .toString()
    }

    private suspend fun restoreSnapshot(serialized: String) {
        val root = JSONObject(serialized)
        require(root.getInt("schemaVersion") == SCHEMA_VERSION)
        val managedApps = root.getJSONArray("managedApps").toManagedApps()
        val schedules = root.getJSONArray("schedules").toSchedules()
        val events = root.getJSONArray("recentBlockEvents").toEvents()
        val preferences = root.getJSONObject("preferences").toTypedPreferences()

        require(managedApps.size <= MAX_MANAGED_APPS)
        require(schedules.size <= MAX_SCHEDULES)
        require(events.size <= MAX_BLOCK_EVENTS)
        val packageNames = managedApps.mapTo(mutableSetOf()) { it.packageName }
        require(schedules.all { it.appPackage in packageNames }) {
            "Backup contains a schedule without a matching managed app."
        }

        database.withTransaction {
            database.scheduleDao().deleteAll()
            database.managedAppDao().deleteAll()
            database.blockEventDao().clearAll()
            database.managedAppDao().insertAll(managedApps)
            database.scheduleDao().insertAll(schedules)
            database.blockEventDao().insertAll(events)
        }
        settings.restoreCloudSafePreferences(preferences)
    }

    private fun JSONArray.toManagedApps(): List<ManagedAppEntity> =
        (0 until length()).map { index ->
            val item = getJSONObject(index)
            ManagedAppEntity(
                packageName = item.getString("packageName").take(255),
                appName = item.getString("appName").take(200),
                isManaged = item.optBoolean("isManaged", true),
                createdAt = item.optLong("createdAt")
            )
        }

    private fun JSONArray.toSchedules(): List<SchedulePeriodEntity> =
        (0 until length()).map { index ->
            val item = getJSONObject(index)
            SchedulePeriodEntity(
                id = item.optLong("id").coerceAtLeast(0),
                appPackage = item.getString("appPackage").take(255),
                dayOfWeek = item.getInt("dayOfWeek").coerceIn(1, 7),
                startHour = item.getInt("startHour").coerceIn(0, 23),
                startMinute = item.getInt("startMinute").coerceIn(0, 59),
                endHour = item.getInt("endHour").coerceIn(0, 23),
                endMinute = item.getInt("endMinute").coerceIn(0, 59),
                isEnabled = item.optBoolean("isEnabled", true)
            )
        }

    private fun JSONArray.toEvents(): List<BlockEventEntity> =
        (0 until length()).map { index ->
            val item = getJSONObject(index)
            BlockEventEntity(
                id = item.optLong("id").coerceAtLeast(0),
                appPackage = item.getString("appPackage").take(255),
                appName = item.getString("appName").take(200),
                timestamp = item.optLong("timestamp").coerceAtLeast(0),
                reason = item.optString("reason").take(500)
            )
        }

    private fun JSONObject.toTypedPreferences(): Map<String, Any> {
        val result = linkedMapOf<String, Any>()
        keys().forEach { key ->
            val entry = getJSONObject(key)
            when (entry.getString("type")) {
                "boolean" -> result[key] = entry.getBoolean("value")
                "int" -> result[key] = entry.getInt("value")
                "long" -> result[key] = entry.getLong("value")
                "string" -> result[key] = entry.getString("value").take(4096)
                "string_set" -> {
                    val array = entry.getJSONArray("value")
                    result[key] = (0 until array.length()).map { array.getString(it).take(512) }.take(500)
                }
            }
        }
        return result
    }

    private fun requireRecoveryPhrase(value: CharArray) {
        require(value.size >= 16) { "Use a recovery phrase with at least 16 characters." }
    }

    companion object {
        private const val SCHEMA_VERSION = 1
        private const val MAX_CIPHERTEXT_CHARS = 900_000
        private const val MAX_MANAGED_APPS = 2_000
        private const val MAX_SCHEDULES = 10_000
        private const val MAX_BLOCK_EVENTS = 100
    }
}

private data class EncryptedPayload(val salt: String, val nonce: String, val ciphertext: String)

private object BackupCryptography {
    private const val ITERATIONS = 310_000
    private const val KEY_BITS = 256
    private val random = SecureRandom()

    fun encrypt(clear: ByteArray, phrase: CharArray): EncryptedPayload {
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(phrase, salt), GCMParameterSpec(128, nonce))
        val encrypted = cipher.doFinal(clear)
        return EncryptedPayload(
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(nonce, Base64.NO_WRAP),
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        )
    }

    fun decrypt(payload: EncryptedPayload, phrase: CharArray): ByteArray {
        val salt = Base64.decode(payload.salt, Base64.NO_WRAP)
        val nonce = Base64.decode(payload.nonce, Base64.NO_WRAP)
        val encrypted = Base64.decode(payload.ciphertext, Base64.NO_WRAP)
        require(salt.size == 16 && nonce.size == 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(phrase, salt), GCMParameterSpec(128, nonce))
        return cipher.doFinal(encrypted)
    }

    private fun deriveKey(phrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(phrase, salt, ITERATIONS, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
            try {
                SecretKeySpec(bytes, "AES")
            } finally {
                bytes.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                @Suppress("UNCHECKED_CAST")
                continuation.resume(task.result as T)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Cloud operation failed.")
                )
            }
        }
    }


private suspend fun <T> runCancellableCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
