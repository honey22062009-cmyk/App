package com.example.core.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock

/**
 * Ensures system-critical applications can NEVER be managed or blocked.
 * Expensive launcher and dialer resolution is cached instead of running on
 * every Accessibility event. Package changes can invalidate the cache.
 */
object SafetyWhitelist {
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
        "com.android.vending",
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher3"
    )

    private data class ResolvedDefaults(
        val packageNames: Set<String>,
        val expiresAtElapsed: Long
    )

    @Volatile
    private var resolvedDefaults: ResolvedDefaults? = null

    fun invalidateCache() {
        resolvedDefaults = null
    }

    fun preload(context: Context) {
        resolved(context.applicationContext)
    }

    fun isSystemCritical(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return true
        if (packageName in ESSENTIAL_SYSTEM_PACKAGES) return true
        return packageName in resolved(context.applicationContext)
    }

    private fun resolved(context: Context): Set<String> {
        val now = SystemClock.elapsedRealtime()
        val current = resolvedDefaults
        if (current != null && now < current.expiresAtElapsed) return current.packageNames

        val packages = buildSet {
            runCatching {
                val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo?.packageName?.let(::add)
            }
            runCatching {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                context.packageManager.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo?.packageName?.let(::add)
            }
        }
        resolvedDefaults = ResolvedDefaults(packages, now + CACHE_TTL_MILLIS)
        return packages
    }

    private const val CACHE_TTL_MILLIS = 60_000L
}
