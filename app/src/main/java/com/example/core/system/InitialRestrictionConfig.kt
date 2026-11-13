package com.example.core.system

import android.content.Context
import androidx.room.withTransaction
import com.example.core.model.SchedulePeriod
import com.example.data.db.AppDatabase
import com.example.data.db.entity.ManagedAppEntity
import com.example.data.db.entity.SchedulePeriodEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek

/**
 * Versioned first-install configuration for the requested restriction schedule.
 *
 * The existing SchedulePeriod model represents ALLOWED hours. These windows are
 * therefore the exact complement of the requested blocked windows. Keeping that
 * contract avoids changing the meaning of schedules already saved by users.
 */
object InitialRestrictionConfig {
    private const val PREFS_NAME = "initial_restriction_config"
    private const val SEEDED_VERSION_KEY = "seeded_version"
    private const val CURRENT_VERSION = 1

    data class TargetApp(val name: String, val packageName: String)

    data class TimeWindow(val startHour: Int, val startMinute: Int, val endHour: Int, val endMinute: Int)

    // Website hosts are handled by the browser Accessibility path, not as app packages.
    val targetDomains = listOf("anifam.net")

    val targetApps = listOf(
        TargetApp("Instagram", "com.instagram.android"),
        TargetApp("Roblox", "com.roblox.client"),
        TargetApp("Genshin Impact", "com.miHoYo.GenshinImpact"),
        TargetApp("Monoposto", "com.gabama.monopostolite"),
        TargetApp("Google Play Store", "com.android.vending")
    )

    /**
     * App access windows are the complement of the user's blocked windows.
     * Mon: 09:00-15:00 and 22:00-01:00 blocked.
     * Tue-Sat: same daily windows.
     * Sun: 11:00-13:00 and 22:00-23:00 blocked.
     */
    fun allowedWindowsFor(day: DayOfWeek): List<TimeWindow> = when (day) {
        DayOfWeek.MONDAY -> listOf(
            TimeWindow(0, 0, 9, 0),
            TimeWindow(15, 0, 22, 0)
        )
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY -> listOf(
            TimeWindow(1, 0, 9, 0),
            TimeWindow(15, 0, 22, 0)
        )
        DayOfWeek.SUNDAY -> listOf(
            TimeWindow(1, 0, 11, 0),
            TimeWindow(13, 0, 22, 0),
            TimeWindow(23, 0, 0, 0)
        )
    }

    fun defaultAllowedPeriods(packageName: String): List<SchedulePeriod> =
        DayOfWeek.entries.flatMap { day ->
            allowedWindowsFor(day).map { window ->
                SchedulePeriod(
                    appPackage = packageName,
                    dayOfWeek = day,
                    startHour = window.startHour,
                    startMinute = window.startMinute,
                    endHour = window.endHour,
                    endMinute = window.endMinute,
                    isEnabled = true
                )
            }
        }

    /**
     * Seeds only once. Existing target settings are never overwritten:
     * - missing app records receive the default schedule;
     * - already-managed apps with no schedule receive it;
     * - explicitly unmanaged apps and existing schedules remain untouched.
     *
     * Apps remain configured even when not installed, so installing one later
     * does not require another configuration change.
     */
    suspend fun seedIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (preferences.getInt(SEEDED_VERSION_KEY, 0) >= CURRENT_VERSION) return@withContext

        val database = AppDatabase.getInstance(context)
        database.withTransaction {
            val managedApps = database.managedAppDao()
            val schedules = database.scheduleDao()

            targetApps.forEach { target ->
                val existing = managedApps.getByPackage(target.packageName)
                if (existing == null) {
                    managedApps.insertOrUpdate(
                        ManagedAppEntity(
                            packageName = target.packageName,
                            appName = target.name,
                            isManaged = true
                        )
                    )
                    schedules.insertAll(
                        defaultAllowedPeriods(target.packageName).map(SchedulePeriodEntity::fromDomain)
                    )
                } else if (existing.isManaged && schedules.getPeriodsForApp(target.packageName).isEmpty()) {
                    schedules.insertAll(
                        defaultAllowedPeriods(target.packageName).map(SchedulePeriodEntity::fromDomain)
                    )
                }
            }
        }

        check(preferences.edit().putInt(SEEDED_VERSION_KEY, CURRENT_VERSION).commit()) {
            "Could not persist initial restriction configuration version"
        }
    }
}
