package com.example.receiver

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.core.system.AlarmScheduler
import com.example.data.repository.AppBlockerRepository
import com.example.ui.blocker.BlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Verifies the app's state at schedule boundaries and schedules the next boundary.
 * The interrupt shield is only brought forward when the target app is still foreground,
 * so an unrelated app is not unexpectedly covered when a background alarm fires.
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val packageName = intent?.getStringExtra("packageName")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!packageName.isNullOrBlank() && isPackageForeground(appContext, packageName)) {
                    val repository = AppBlockerRepository(appContext)
                    val evaluation = repository.evaluateApp(packageName)
                    if (evaluation.isBlocked) {
                        val appLabel = try {
                            val info = appContext.packageManager.getApplicationInfo(packageName, 0)
                            appContext.packageManager.getApplicationLabel(info).toString()
                        } catch (_: Exception) {
                            packageName
                        }

                        repository.recordBlockEvent(packageName, appLabel, "Schedule window expired")

                        val blockIntent = Intent(appContext, BlockerActivity::class.java).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                            )
                            putExtra(BlockerActivity.EXTRA_PACKAGE_NAME, packageName)
                            putExtra(BlockerActivity.EXTRA_APP_NAME, appLabel)
                            putExtra(BlockerActivity.EXTRA_NEXT_ALLOWED, evaluation.formattedNextAllowedTime)
                            putExtra(BlockerActivity.EXTRA_BLOCK_TYPE, BlockerActivity.BLOCK_TYPE_SCHEDULE)
                        }
                        appContext.startActivity(blockIntent)
                    }
                }
            } catch (e: Exception) {
                Log.w("ScheduleAlarmReceiver", "Transition verification failed", e)
            } finally {
                // Always replace the fired alarm, even when querying Usage Access or the
                // database fails. The Accessibility/monitoring services remain fallback paths.
                AlarmScheduler.scheduleNextTransition(appContext)
                pendingResult.finish()
            }
        }
    }

    private fun isPackageForeground(context: Context, packageName: String): Boolean {
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return false
        val end = System.currentTimeMillis()
        val start = end - 15_000L
        val events = try {
            usageStats.queryEvents(start, end)
        } catch (_: SecurityException) {
            return false
        } ?: return false

        val event = UsageEvents.Event()
        var mostRecentResumedPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                mostRecentResumedPackage = event.packageName
            }
        }
        return mostRecentResumedPackage == packageName
    }
}
