package com.example.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Local-only inventory and capability review. Requested permissions are signals, not proof
 * of abuse or malware. No app inventory or permission list is uploaded by this scanner.
 */
class AppCapabilityScanner(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    fun scanInstalledApps(): List<AppCapabilityReport> {
        val queryFlags = PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or
            PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
        val packages = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(queryFlags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(queryFlags)
        }
        return packages.asSequence()
            .filterNot { it.packageName == appContext.packageName }
            .mapNotNull(::toReport)
            .sortedWith(compareByDescending<AppCapabilityReport> { it.signals.size }.thenBy { it.label.lowercase() })
            .toList()
    }

    private fun toReport(info: PackageInfo): AppCapabilityReport? {
        val appInfo = info.applicationInfo ?: return null
        val label = runCatching { packageManager.getApplicationLabel(appInfo).toString() }
            .getOrDefault(info.packageName)
        val requestedList = info.requestedPermissions.orEmpty().toList()
        val requested = requestedList.toSet()
        val permissionFlags = info.requestedPermissionsFlags ?: IntArray(0)
        val granted = requestedList.filterIndexed { index, _ ->
            index < permissionFlags.size &&
                (permissionFlags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
        }
        val signals = buildList {
            if (requested.any { it in LOCATION_PERMISSIONS }) add("Location access requested")
            if (requested.any { it in CONTACT_PERMISSIONS }) add("Contacts access requested")
            if (requested.any { it in MICROPHONE_PERMISSIONS }) add("Microphone access requested")
            if (requested.any { it in CAMERA_PERMISSIONS }) add("Camera access requested")
            if (requested.any { it in SMS_PERMISSIONS }) add("SMS access requested")
            if (requested.any { it in ACCESSIBILITY_PERMISSIONS }) add("Accessibility-related capability declared")
            if (requested.any { it in OVERLAY_PERMISSIONS }) add("Display-over-other-apps capability requested")
            if (requested.any { it in INSTALL_PERMISSIONS }) add("Package installation capability requested")
            if (requested.any { it in USAGE_PERMISSIONS }) add("App-usage visibility capability requested")
        }
        val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        return AppCapabilityReport(
            packageName = info.packageName,
            label = label,
            isSystemApp = isSystem,
            requestedPermissionCount = requested.size,
            requestedPermissions = requestedList.sorted(),
            grantedPermissions = granted.sorted(),
            declaredActivityCount = info.activities?.size ?: 0,
            declaredServiceCount = info.services?.size ?: 0,
            declaredReceiverCount = info.receivers?.size ?: 0,
            declaredProviderCount = info.providers?.size ?: 0,
            signals = signals
        )
    }

    companion object {
        private val LOCATION_PERMISSIONS = setOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_BACKGROUND_LOCATION")
        private val CONTACT_PERMISSIONS = setOf("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS")
        private val MICROPHONE_PERMISSIONS = setOf("android.permission.RECORD_AUDIO")
        private val CAMERA_PERMISSIONS = setOf("android.permission.CAMERA")
        private val SMS_PERMISSIONS = setOf("android.permission.READ_SMS", "android.permission.SEND_SMS", "android.permission.RECEIVE_SMS")
        private val ACCESSIBILITY_PERMISSIONS = setOf("android.permission.BIND_ACCESSIBILITY_SERVICE")
        private val OVERLAY_PERMISSIONS = setOf("android.permission.SYSTEM_ALERT_WINDOW")
        private val INSTALL_PERMISSIONS = setOf("android.permission.REQUEST_INSTALL_PACKAGES")
        private val USAGE_PERMISSIONS = setOf("android.permission.PACKAGE_USAGE_STATS")
    }
}

data class AppCapabilityReport(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val requestedPermissionCount: Int,
    val requestedPermissions: List<String>,
    val grantedPermissions: List<String>,
    val declaredActivityCount: Int,
    val declaredServiceCount: Int,
    val declaredReceiverCount: Int,
    val declaredProviderCount: Int,
    val signals: List<String>
)
