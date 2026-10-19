package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "managed_apps")
data class ManagedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isManaged: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
