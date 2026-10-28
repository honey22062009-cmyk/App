package com.example

import android.app.Activity
import android.app.UiAutomation
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.preferences.SettingsPreferences
import com.example.service.BrowserGuardStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** A real separate test-APK window that exposes browser-style accessibility fields. Never ships. */
class BrowserFixtureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        layout.addView(EditText(this).apply {
            id = com.example.test.R.id.qa_browser_url_bar
            contentDescription = "Web address"
            setText(intent.getStringExtra("address") ?: "https://example.com")
        })
        layout.addView(TextView(this).apply { text = "New private tab · You've gone incognito"; textSize = 24f })
        layout.addView(TextView(this).apply { text = "Ordinary page copy can mention porn or sex without closing this browser." })
        setContentView(layout)
        layout.isFocusableInTouchMode = true
        layout.requestFocus()
    }
}

@RunWith(AndroidJUnit4::class)
class BrowserGuardNativeTest {
    @Test fun openingBrowserAndPrivateLabelsAreAllowedButExplicitAddressInterrupts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val settings = SettingsPreferences(context)
        fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { String(it.readBytes()).trim() }
        fun waitUntil(message: String, condition: () -> Boolean) {
            val deadline = android.os.SystemClock.uptimeMillis() + 15_000
            while (!condition() && android.os.SystemClock.uptimeMillis() < deadline) Thread.sleep(100)
            assertTrue(message, condition())
        }
        val previous = shell("settings get secure enabled_accessibility_services").takeUnless { it == "null" }.orEmpty()
        val previousEnabled = shell("settings get secure accessibility_enabled").takeIf { it == "1" } ?: "0"
        val component = "${context.packageName}/com.example.service.AppBlockerAccessibilityService"
        try {
            runBlocking {
                settings.setProtectionEnabled(false) // Test only the browser rule, with no app schedule.
                settings.setBrowserInspectionConsent(true)
                settings.setAdultProtectionEnabled(true)
            }
            val services = (previous.split(":").filter { it.isNotBlank() } + component).distinct().joinToString(":")
            shell("settings put secure enabled_accessibility_services $services")
            shell("settings put secure accessibility_enabled 1")
            waitUntil("Accessibility browser service connected") { BrowserGuardStatus.isConnected.value }
            fun launch(address: String) {
                context.startActivity(Intent().setComponent(ComponentName(instrumentation.context.packageName, BrowserFixtureActivity::class.java.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("address", address))
            }
            val before = runBlocking { settings.adultBlocksCountFlow.first() }
            val inspected = BrowserGuardStatus.inspections.value
            launch("https://www.google.com/search?q=physics")
            waitUntil("The real browser window was inspected") { BrowserGuardStatus.inspections.value > inspected }
            Thread.sleep(800)
            assertEquals("Opening a browser, private label or page paragraph must not block", before, runBlocking { settings.adultBlocksCountFlow.first() })
            assertEquals(instrumentation.context.packageName, automation.rootInActiveWindow?.packageName?.toString())
            launch("https://www.google.com/search?q=sex")
            waitUntil("An actual explicit search triggers the service") { runBlocking { settings.adultBlocksCountFlow.first() } > before }
            waitUntil("Explicit browser content is removed from view") { automation.rootInActiveWindow?.packageName?.toString() != instrumentation.context.packageName }
            // No package cooldown: a later clean browser is still allowed.
            Thread.sleep(1300)
            launch("https://example.com")
            waitUntil("Clean browsing can resume") { automation.rootInActiveWindow?.packageName?.toString() == instrumentation.context.packageName }
            Thread.sleep(800)
            assertEquals(before + 1, runBlocking { settings.adultBlocksCountFlow.first() })
        } finally {
            shell("settings put secure enabled_accessibility_services ${if (previous.isEmpty()) "null" else previous}")
            shell("settings put secure accessibility_enabled $previousEnabled")
            runBlocking { settings.setProtectionEnabled(true) }
        }
    }
}
