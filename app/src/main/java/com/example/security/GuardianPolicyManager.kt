package com.example.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

/**
 * Guardian-only package controls. These APIs work only after a guardian has provisioned
 * Strict Blocker as the device owner. Ordinary installs fail closed and never pretend
 * that an uninstall or suspension succeeded.
 */
class GuardianPolicyManager(context: Context) {
    private val appContext = context.applicationContext
    private val policy = appContext.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(appContext, GuardianDeviceAdminReceiver::class.java)

    val isGuardianManaged: Boolean
        get() = policy.isDeviceOwnerApp(appContext.packageName)

    fun applyRestrictions(packageNames: Set<String>): GuardianPolicyResult {
        if (!isGuardianManaged) {
            return GuardianPolicyResult(
                isDeviceOwner = false,
                suspended = emptySet(),
                uninstallBlocked = emptySet(),
                failures = packageNames.associateWith { "Guardian device-owner setup is required." }
            )
        }

        val targets = packageNames.asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .filterNot { it == appContext.packageName }
            .distinct()
            .toList()
        if (targets.isEmpty()) {
            return GuardianPolicyResult(true, emptySet(), emptySet(), emptyMap())
        }

        val failures = linkedMapOf<String, String>()
        val suspended = linkedSetOf<String>()
        val uninstallBlocked = linkedSetOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val rejected = runCatching {
                policy.setPackagesSuspended(admin, targets.toTypedArray(), true).toSet()
            }.getOrElse { error ->
                targets.forEach { failures[it] = error.message ?: "Package suspension failed." }
                targets.toSet()
            }
            targets.filterNot(rejected::contains).forEach(suspended::add)
            rejected.forEach { failures.putIfAbsent(it, "Android refused to suspend this package.") }
        } else {
            targets.forEach { failures[it] = "Package suspension requires Android 7.0 or newer." }
        }

        targets.forEach { packageName ->
            try {
                policy.setUninstallBlocked(admin, packageName, true)
                uninstallBlocked += packageName
            } catch (error: SecurityException) {
                failures.putIfAbsent(packageName, error.message ?: "Android refused uninstall protection.")
            } catch (error: RuntimeException) {
                failures.putIfAbsent(packageName, error.message ?: "Uninstall protection failed.")
            }
        }

        return GuardianPolicyResult(true, suspended, uninstallBlocked, failures)
    }

    /** Called only after the guardian explicitly removes a package from the restriction list. */
    fun removeRestrictions(packageName: String): Result<Unit> = runCatching {
        check(isGuardianManaged) { "Guardian device-owner setup is required." }
        require(packageName.isNotBlank() && packageName != appContext.packageName)
        policy.setUninstallBlocked(admin, packageName, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val rejected = policy.setPackagesSuspended(admin, arrayOf(packageName), false)
            check(rejected.isEmpty()) { "Android refused to restore this package." }
        }
    }
}

data class GuardianPolicyResult(
    val isDeviceOwner: Boolean,
    val suspended: Set<String>,
    val uninstallBlocked: Set<String>,
    val failures: Map<String, String>
)
