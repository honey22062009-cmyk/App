package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an application tracked/managed by the strict blocker.
 *
 * @param packageName The unique Android package name, serving as the Primary Key.
 * @param appName User-friendly label of the application.
 * @param isManaged Flag indicating if this app is actively managed by blocker rules.
 * @param createdAt Timestamp when this app was added to the database.
 */
@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isManaged: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
