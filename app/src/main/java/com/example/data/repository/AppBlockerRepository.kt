package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.room.withTransaction
import com.example.core.evaluator.ScheduleEvaluator
import com.example.core.system.AlarmScheduler
import com.example.core.model.BlockEvaluationResult
import com.example.core.model.SchedulePeriod
import com.example.core.system.SafetyWhitelist
import com.example.data.db.AppDatabase
import com.example.data.db.dao.AppDao
import com.example.data.db.dao.BlockEventDao
import com.example.data.db.dao.ManagedAppDao
import com.example.data.db.dao.ScheduleDao
import com.example.data.db.dao.ScheduleEntityDao
import com.example.data.db.entity.BlockEventEntity
import com.example.data.db.entity.ManagedAppEntity
import com.example.data.db.entity.SchedulePeriodEntity
import com.example.data.preferences.SettingsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDateTime

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isManaged: Boolean,
    val periodCount: Int,
    val isSystemCritical: Boolean
)

data class AppGroup(
    val name: String,
    val packageNames: Set<String>
)

data class NextTransitionInfo(
    val appName: String,
    val packageName: String,
    val transitionDateTime: LocalDateTime,
    val isOpening: Boolean
)

class AppBlockerRepository(
    private val context: Context,
    val database: AppDatabase = AppDatabase.getInstance(context),
    val managedAppDao: ManagedAppDao = database.managedAppDao(),
    val scheduleDao: ScheduleDao = database.scheduleDao(),
    val blockEventDao: BlockEventDao = database.blockEventDao(),
    val settings: SettingsPreferences = SettingsPreferences(context),
    val appDao: AppDao = database.appDao(),
    val scheduleEntityDao: ScheduleEntityDao = database.scheduleEntityDao()
) {

    val managedAppsFlow: Flow<List<ManagedAppEntity>> = managedAppDao.getAllManagedAppsFlow()
    val activeManagedAppsFlow: Flow<List<ManagedAppEntity>> = managedAppDao.getActiveManagedAppsFlow()
    val allPeriodsFlow: Flow<List<SchedulePeriod>> = scheduleDao.getAllPeriodsFlow().map { list ->
        list.map { it.toDomain() }
    }
    val recentBlocksFlow: Flow<List<BlockEventEntity>> = blockEventDao.getRecentBlockEventsFlow()
    val totalBlocksCountFlow: Flow<Int> = blockEventDao.getTotalBlocksCountFlow()
    val appGroupsFlow: Flow<List<AppGroup>> = settings.appGroupsFlow.map { encoded ->
        encoded.mapNotNull { entry ->
            val parts = entry.split("::", limit = 2)
            val name = parts.getOrNull(0)?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val packages = parts.getOrNull(1)
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet()
            AppGroup(name = name, packageNames = packages)
        }.sortedBy { it.name.lowercase() }
    }

    fun getPeriodsForAppFlow(packageName: String): Flow<List<SchedulePeriod>> {
        return scheduleDao.getPeriodsForAppFlow(packageName).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Retrieves all installed launchable applications, filtered by safety rules.
     */
    suspend fun getInstalledLaunchableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (_: Exception) {
            try {
                pm.queryIntentActivities(intent, 0)
            } catch (_: Exception) {
                emptyList()
            }
        }

        val managedApps = managedAppDao.getAllManagedAppsFlow().first().associateBy { it.packageName }
        val allPeriods = scheduleDao.getAllPeriodsFlow().first().groupBy { it.appPackage }

        val seenPackages = mutableSetOf<String>()
        val result = mutableListOf<InstalledAppInfo>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val isCritical = SafetyWhitelist.isSystemCritical(context, pkg)
            val appLabel = try {
                info.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkg
            }

            val isManaged = managedApps[pkg]?.isManaged ?: false
            val periods = allPeriods[pkg] ?: emptyList()

            result.add(
                InstalledAppInfo(
                    packageName = pkg,
                    appName = appLabel,
                    isManaged = isManaged,
                    periodCount = periods.size,
                    isSystemCritical = isCritical
                )
            )
        }

        result.sortedWith(compareByDescending<InstalledAppInfo> { it.isManaged }.thenBy { it.appName.lowercase() })
    }

    suspend fun setAppManaged(packageName: String, appName: String, isManaged: Boolean) = withContext(Dispatchers.IO) {
        if (SafetyWhitelist.isSystemCritical(context, packageName)) return@withContext

        val existing = managedAppDao.getByPackage(packageName)
        if (existing == null) {
            managedAppDao.insertOrUpdate(
                ManagedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    isManaged = isManaged
                )
            )
        } else {
            managedAppDao.setManagedStatus(packageName, isManaged)
        }
        AlarmScheduler.scheduleNextTransition(context)
    }

    suspend fun saveAppGroup(name: String, packageNames: Set<String>) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val current = appGroupsFlow.first().toMutableList()
        current.removeAll { it.name.equals(cleanName, ignoreCase = true) }
        current.add(AppGroup(cleanName, packageNames))
        settings.setAppGroups(
            current.map { group ->
                group.name + "::" + group.packageNames.sorted().joinToString(",")
            }.toSet()
        )
    }

    suspend fun deleteAppGroup(name: String) {
        val current = appGroupsFlow.first().filterNot { it.name == name }
        settings.setAppGroups(
            current.map { group ->
                group.name + "::" + group.packageNames.sorted().joinToString(",")
            }.toSet()
        )
    }

    suspend fun removeManagedApp(packageName: String) = withContext(Dispatchers.IO) {
        managedAppDao.deleteByPackage(packageName)
        scheduleDao.deleteByPackage(packageName)
        AlarmScheduler.scheduleNextTransition(context)
    }

    suspend fun saveSchedulePeriod(period: SchedulePeriod) = withContext(Dispatchers.IO) {
        val existingApp = managedAppDao.getByPackage(period.appPackage)
        if (existingApp == null) {
            val appName = try {
                val appInfo = context.packageManager.getApplicationInfo(period.appPackage, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                period.appPackage
            }
            managedAppDao.insertOrUpdate(
                ManagedAppEntity(
                    packageName = period.appPackage,
                    appName = appName,
                    isManaged = true
                )
            )
        } else if (!existingApp.isManaged) {
            // Saving a schedule must activate the app; otherwise the saved rule is ignored
            // by evaluateApp because inactive records are explicitly allowed.
            managedAppDao.setManagedStatus(period.appPackage, true)
        }
        scheduleDao.insert(SchedulePeriodEntity.fromDomain(period))
        AlarmScheduler.scheduleNextTransition(context)
    }

    suspend fun updateSchedulePeriod(period: SchedulePeriod) = withContext(Dispatchers.IO) {
        scheduleDao.update(SchedulePeriodEntity.fromDomain(period))
        AlarmScheduler.scheduleNextTransition(context)
    }

    suspend fun deleteSchedulePeriod(id: Long) = withContext(Dispatchers.IO) {
        scheduleDao.deleteById(id)
        AlarmScheduler.scheduleNextTransition(context)
    }

    suspend fun togglePeriodEnabled(id: Long, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        scheduleDao.setPeriodEnabled(id, isEnabled)
        AlarmScheduler.scheduleNextTransition(context)
    }

    suspend fun duplicateSchedulePeriod(period: SchedulePeriod, newDay: DayOfWeek) = withContext(Dispatchers.IO) {
        val newPeriod = period.copy(
            id = 0,
            dayOfWeek = newDay
        )
        scheduleDao.insert(SchedulePeriodEntity.fromDomain(newPeriod))
        AlarmScheduler.scheduleNextTransition(context)
    }

    suspend fun mergeOverlappingForApp(packageName: String) = withContext(Dispatchers.IO) {
        val existingEntities = scheduleDao.getPeriodsForApp(packageName)
        val domainList = existingEntities.map { it.toDomain() }
        val mergedDomain = ScheduleEvaluator.mergeOverlapping(domainList)

        database.withTransaction {
            scheduleDao.deleteByPackage(packageName)
            scheduleDao.insertAll(mergedDomain.map { SchedulePeriodEntity.fromDomain(it) })
        }
        AlarmScheduler.scheduleNextTransition(context)
    }

    /**
     * Evaluates the status of [packageName] right now.
     */
    suspend fun evaluateApp(packageName: String, dateTime: LocalDateTime = LocalDateTime.now()): BlockEvaluationResult = withContext(Dispatchers.IO) {
        val managedApp = managedAppDao.getByPackage(packageName)
        if (managedApp == null || !managedApp.isManaged) {
            return@withContext BlockEvaluationResult(
                isBlocked = false,
                reason = "Application is not managed by Strict Blocker."
            )
        }

        val periods = scheduleDao.getPeriodsForApp(packageName).map { it.toDomain() }
        ScheduleEvaluator.evaluate(periods, dateTime)
    }

    suspend fun recordBlockEvent(packageName: String, appName: String, reason: String) = withContext(Dispatchers.IO) {
        blockEventDao.insert(
            BlockEventEntity(
                appPackage = packageName,
                appName = appName,
                timestamp = System.currentTimeMillis(),
                reason = reason
            )
        )
    }

    suspend fun recordAdultBlockEvent(reason: String = "18+ Adult Content Blocked") = withContext(Dispatchers.IO) {
        settings.incrementAdultBlocksCount()
        blockEventDao.insert(
            BlockEventEntity(
                appPackage = "com.aistudio.18blocker.adultfilter",
                appName = "18+ Adult Shield",
                timestamp = System.currentTimeMillis(),
                reason = reason
            )
        )
    }

    /**
     * Purges all user data locally: managed apps, schedules, block logs, and preferences.
     * Complies with GDPR/CCPA right to erasure / data deletion requests.
     */
    suspend fun deleteAllUserData() = withContext(Dispatchers.IO) {
        // Clear both the current tables and legacy tables retained by the database schema.
        // Each delete runs in a transaction so callers never see a partially cleared database.
        database.withTransaction {
            scheduleDao.deleteAll()
            managedAppDao.deleteAll()
            scheduleEntityDao.deleteAll()
            appDao.deleteAll()
            blockEventDao.clearAll()
        }
        settings.clearAllUserPreferences()
        AlarmScheduler.scheduleNextTransition(context)
    }

    /**
     * Determines the earliest upcoming opening or closing boundary across managed apps.
     * Closing boundaries matter as much as opening boundaries for strict schedule cutoffs.
     */
    suspend fun findNextGlobalTransition(): NextTransitionInfo? = withContext(Dispatchers.IO) {
        val activeApps = managedAppDao.getActiveManagedApps()
        if (activeApps.isEmpty()) return@withContext null

        val allPeriods = scheduleDao.getAllEnabledPeriods().map { it.toDomain() }
        val now = LocalDateTime.now()
        var earliestTransition: NextTransitionInfo? = null

        for (app in activeApps) {
            val appPeriods = allPeriods.filter { it.appPackage == app.packageName }
            val transition = ScheduleEvaluator.findNextTransition(appPeriods, now) ?: continue
            if (earliestTransition == null ||
                transition.dateTime.isBefore(earliestTransition.transitionDateTime)
            ) {
                earliestTransition = NextTransitionInfo(
                    appName = app.appName,
                    packageName = app.packageName,
                    transitionDateTime = transition.dateTime,
                    isOpening = transition.isOpening
                )
            }
        }

        earliestTransition
    }
}
