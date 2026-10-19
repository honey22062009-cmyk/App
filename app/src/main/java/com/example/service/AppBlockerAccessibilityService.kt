package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.core.system.SafetyWhitelist
import com.example.data.repository.AppBlockerRepository
import com.example.ui.blocker.BlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * High-performance, zero-latency accessibility service.
 * Intercepts window state changes to enforce scheduled app blocking.
 *
 * Privacy Note: canRetrieveWindowContent is set to false in xml config.
 * This service never reads screen text, passwords, or keyboard inputs.
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var repository: AppBlockerRepository

    private var lastBlockedPackage: String? = null
    private var lastBlockTimestamp: Long = 0L

    override fun onCreate() {
        super.onCreate()
        repository = AppBlockerRepository(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip system critical apps, launchers, and blocker itself
        if (SafetyWhitelist.isSystemCritical(this, packageName)) return

        // Throttle repeated triggers within 400ms for the exact same package
        val now = System.currentTimeMillis()
        if (packageName == lastBlockedPackage && (now - lastBlockTimestamp) < 400L) {
            return
        }

        serviceScope.launch {
            // Check if global protection is active
            val protectionEnabled = repository.settings.isProtectionEnabledFlow.first()
            if (!protectionEnabled) return@launch

            val evaluation = repository.evaluateApp(packageName)
            if (evaluation.isBlocked) {
                lastBlockedPackage = packageName
                lastBlockTimestamp = now

                val appLabel = try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    packageName
                }

                // Record block event
                repository.recordBlockEvent(packageName, appLabel, evaluation.reason)

                // Launch Fullscreen Shield Activity
                val intent = Intent(this@AppBlockerAccessibilityService, BlockerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    putExtra(BlockerActivity.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(BlockerActivity.EXTRA_APP_NAME, appLabel)
                    putExtra(BlockerActivity.EXTRA_NEXT_ALLOWED, evaluation.formattedNextAllowedTime)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {
        // Required by AccessibilityService
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
