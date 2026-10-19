package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.system.AlarmScheduler
import com.example.data.repository.AppBlockerRepository
import com.example.ui.blocker.BlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires at the exact minute of a schedule cutoff or opening.
 * Triggers immediate verification and schedules the subsequent transition.
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val packageName = intent?.getStringExtra("packageName")

        if (!packageName.isNullOrBlank()) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = AppBlockerRepository(context)
                    val evaluation = repository.evaluateApp(packageName)
                    if (evaluation.isBlocked) {
                        val appLabel = try {
                            val info = context.packageManager.getApplicationInfo(packageName, 0)
                            context.packageManager.getApplicationLabel(info).toString()
                        } catch (_: Exception) {
                            packageName
                        }

                        repository.recordBlockEvent(packageName, appLabel, "Schedule window expired")

                        val blockIntent = Intent(context, BlockerActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            putExtra(BlockerActivity.EXTRA_PACKAGE_NAME, packageName)
                            putExtra(BlockerActivity.EXTRA_APP_NAME, appLabel)
                            putExtra(BlockerActivity.EXTRA_NEXT_ALLOWED, evaluation.formattedNextAllowedTime)
                        }
                        context.startActivity(blockIntent)
                    }
                } finally {
                    AlarmScheduler.scheduleNextTransition(context)
                    pendingResult.finish()
                }
            }
        } else {
            AlarmScheduler.scheduleNextTransition(context)
        }
    }
}
