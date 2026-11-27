package com.example.data.preferences

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Device-local profile directory. It stores profile metadata only.
 * Private habit/streak details remain in the app's existing local stores;
 * no cloud account or remote data collection is used.
 */
@Entity(tableName = "local_user_profiles")
data class LocalUserProfileEntity(
    @PrimaryKey val profileId: String = "device-owner",
    val displayName: String,
    val avatarId: String = "bunny",
    val createdAtMillis: Long,
    val lastLoginAtMillis: Long
)

@Dao
interface LocalUserProfileDao {
    @Query("SELECT * FROM local_user_profiles WHERE profileId = 'device-owner' LIMIT 1")
    fun observeProfile(): Flow<LocalUserProfileEntity?>

    @Query("SELECT * FROM local_user_profiles WHERE profileId = 'device-owner' LIMIT 1")
    suspend fun getProfile(): LocalUserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: LocalUserProfileEntity)

    @Query("UPDATE local_user_profiles SET lastLoginAtMillis = :timestamp WHERE profileId = 'device-owner'")
    suspend fun updateLastLogin(timestamp: Long)
}

@Database(entities = [LocalUserProfileEntity::class], version = 1, exportSchema = false)
abstract class LocalUserDatabase : RoomDatabase() {
    abstract fun profileDao(): LocalUserProfileDao

    companion object {
        @Volatile private var instance: LocalUserDatabase? = null

        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }

        fun getInstance(context: Context): LocalUserDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalUserDatabase::class.java,
                    "strict_blocker_users.db"
                ).build().also { instance = it }
            }
    }
}

class LocalUserProfileStore(context: Context) {
    private val dao = LocalUserDatabase.getInstance(context).profileDao()

    val profileFlow: Flow<LocalUserProfileEntity?> = dao.observeProfile()

    suspend fun ensureProfile(name: String) {
        val cleanName = name.trim().take(40).ifBlank { "My profile" }
        val current = dao.getProfile()
        if (current == null) {
            val now = System.currentTimeMillis()
            dao.upsert(
                LocalUserProfileEntity(
                    displayName = cleanName,
                    createdAtMillis = now,
                    lastLoginAtMillis = now
                )
            )
        } else if (current.displayName != cleanName) {
            dao.upsert(current.copy(displayName = cleanName))
        }
    }

    suspend fun saveProfile(name: String, avatarId: String = "bunny") {
        val cleanName = name.trim().take(40).ifBlank { "My profile" }
        val safeAvatar = avatarId.takeIf { it in AVATARS } ?: "bunny"
        val current = dao.getProfile()
        val now = System.currentTimeMillis()
        dao.upsert(
            LocalUserProfileEntity(
                displayName = cleanName,
                avatarId = safeAvatar,
                createdAtMillis = current?.createdAtMillis ?: now,
                lastLoginAtMillis = current?.lastLoginAtMillis ?: now
            )
        )
    }

    suspend fun markSignedIn() {
        dao.updateLastLogin(System.currentTimeMillis())
    }

    private companion object {
        val AVATARS = setOf("bunny", "shield", "star", "moon")
    }
}
