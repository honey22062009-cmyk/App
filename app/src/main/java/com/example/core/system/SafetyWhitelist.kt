package com.example.core.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Ensures system-critical applications can NEVER be managed or blocked.
 * Prevents bricking or locking out essential device functionality.
 */
object SafetyWhitelist {

    /**
     * Hardcoded package identifiers known to be essential system infrastructure.
     */
    private val ESSENTIAL_SYSTEM_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.settings",
        "com.samsung.android.app.telephonyui",
        "com.samsung.android.dialer",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.android.vending", // Google Play Store
        "com.sec.android.app.launcher", // Samsung One UI Home
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher3"
    )

    /**
     * Checks if [packageName] is a system-critical app or the blocker itself.
     */
    fun isSystemCritical(context: Context, packageName: String): Boolean {
        // 1. Blocker application itself MUST NOT be blocked
        if (packageName == context.packageName) return true

        // 2. Known essential system components
        if (ESSENTIAL_SYSTEM_PACKAGES.contains(packageName)) return true

        // 3. Current default Home / Launcher app
        if (isCurrentLauncher(context, packageName)) return true

        // 4. Default Telephony / Emergency Dialer
        if (isDefaultDialer(context, packageName)) return true

        return false
    }

    /**
     * Dynamically identifies the currently active home launcher application.
     */
    private fun isCurrentLauncher(context: Context, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    /**
     * Dynamically identifies the default dialer application.
     */
    private fun isDefaultDialer(context: Context, packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_DIAL)
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo?.activityInfo?.packageName == packageName
    }
}
