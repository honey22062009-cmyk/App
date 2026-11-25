package com.example.data.backup

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import android.database.sqlite.SQLiteDatabase
import com.example.data.db.AppDatabase
import com.example.data.preferences.FocusSessionPreferences
import com.example.data.preferences.PersonalProgressPreferences
import com.example.data.preferences.SettingsPreferences
import com.example.data.repository.AppBlockerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object BackupManager {
    private val allowedFiles = setOf(
        "strict_blocker.db",
        "strict_blocker.db-wal",
        "strict_blocker.db-shm",
        "strict_blocker_settings.preferences_pb",
        "strict_blocker_focus.preferences_pb",
        "strict_blocker_habits.preferences_pb"
    )

    suspend fun export(context: Context, repository: AppBlockerRepository, destination: Uri) =
        withContext(Dispatchers.IO) {
            repository.database.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)")
                .use { it.moveToFirst() }
            // Checkpoint WAL before snapshotting; keep Room open so the live app container remains valid.
            val candidates = listOf(
                context.getDatabasePath("strict_blocker.db"),
                File(context.getDatabasePath("strict_blocker.db").path + "-wal"),
                File(context.getDatabasePath("strict_blocker.db").path + "-shm"),
                File(context.filesDir, "datastore/strict_blocker_settings.preferences_pb"),
                File(context.filesDir, "datastore/strict_blocker_focus.preferences_pb"),
                File(context.filesDir, "datastore/strict_blocker_habits.preferences_pb")
            )
            val output = context.contentResolver.openOutputStream(destination)
                ?: error("Could not open the selected backup destination.")
            ZipOutputStream(output.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("strict-blocker-backup.txt"))
                zip.write("Strict Blocker backup\nformat=2\ncreatedAt=${System.currentTimeMillis()}\n".toByteArray())
                zip.closeEntry()
                candidates.filter { it.exists() && it.name in allowedFiles }.forEach { file ->
                    zip.putNextEntry(ZipEntry(file.name))
                    file.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }

    suspend fun restore(context: Context, source: Uri): Int = withContext(Dispatchers.IO) {
        val rawInput = context.contentResolver.openInputStream(source) ?: error("Could not read the selected backup.")
        val input = BufferedInputStream(rawInput)
        input.mark(8)
        val firstByte = input.read()
        input.reset()

        if (firstByte == '{'.code) {
            val jsonBytes = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var totalJsonBytes = 0
            try {
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    totalJsonBytes += count
                    require(totalJsonBytes <= 10 * 1024 * 1024) { "JSON backup is larger than 10 MB." }
                    jsonBytes.write(buffer, 0, count)
                }
            } finally {
                input.close()
            }
            val json = String(jsonBytes.toByteArray(), StandardCharsets.UTF_8)
            val entries = SettingsPreferences(context).importLegacyBackupJson(json)
            PersonalProgressPreferences(context).importLegacyBackup(entries)
            val focusEntry = entries.optJSONObject("focus_session_records_v1")
            val focusRecords = if (focusEntry?.optString("type") == "string_set") {
                val values = focusEntry.optJSONArray("value")
                buildSet {
                    if (values != null) {
                        for (index in 0 until values.length()) {
                            values.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                }
            } else emptySet()
            FocusSessionPreferences(context).importLegacyRecords(focusRecords)
            return@withContext entries.length()
        }

        val staging = File(context.cacheDir, "strict-blocker-restore-${System.currentTimeMillis()}").apply { mkdirs() }
        var totalBytes = 0L
        var imported = 0
        try {
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    if (!entry.isDirectory && name in allowedFiles) {
                        val target = File(staging, name)
                        target.outputStream().buffered().use { out ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count <= 0) break
                                totalBytes += count
                                require(totalBytes <= 100L * 1024L * 1024L) { "Backup is larger than 100 MB." }
                                out.write(buffer, 0, count)
                            }
                        }
                        imported++
                    }
                    zip.closeEntry()
                }
            }
            require(imported > 0) { "This file does not contain a supported Strict Blocker backup." }

            // Basic SQLite integrity check before replacing the active local database.
            val stagedDb = File(staging, "strict_blocker.db")
            if (stagedDb.exists()) {
                val check = SQLiteDatabase.openDatabase(stagedDb.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                try {
                    check.rawQuery("PRAGMA quick_check", null).use { cursor ->
                        require(cursor.moveToFirst() && cursor.getString(0) == "ok") { "The database in this backup is damaged." }
                    }
                } finally {
                    check.close()
                }
            }

            AppDatabase.closeInstance()
            val databaseDir = context.getDatabasePath("strict_blocker.db").parentFile!!.apply { mkdirs() }
            val dataStoreDir = File(context.filesDir, "datastore").apply { mkdirs() }
            staging.listFiles()?.forEach { file ->
                val target = when (file.name) {
                    "strict_blocker.db", "strict_blocker.db-wal", "strict_blocker.db-shm" -> File(databaseDir, file.name)
                    else -> File(dataStoreDir, file.name)
                }
                file.copyTo(target, overwrite = true)
            }
            imported
        } finally {
            staging.deleteRecursively()
        }
    }
}
