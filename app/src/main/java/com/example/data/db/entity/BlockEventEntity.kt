package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Entity(
    tableName = "block_events",
    indices = [Index(value = ["timestamp"])]
)
data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appPackage: String,
    val appName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String = ""
) {
    val formattedTime: String
        get() {
            val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
            return dt.format(DateTimeFormatter.ofPattern("MMM dd, hh:mm:ss a", Locale.getDefault()))
        }
}
