package com.example

import android.graphics.Bitmap
import android.app.NotificationManager
import android.content.Context
import android.content.ContentValues
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.example.core.system.TrackerReminderScheduler
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.preferences.SettingsPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReliabilityUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val settings get() = SettingsPreferences(compose.activity.applicationContext)

    private fun waitFor(text: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        Thread.sleep(500) // Allow the separate OpenGL surface to submit its first frame.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Android did not produce a screenshot" }
        fun furPixels(image: Bitmap): Int {
            val pixels = IntArray(image.width * image.height)
            image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
            return pixels.count { pixel ->
                val r = (pixel shr 16) and 255; val g = (pixel shr 8) and 255; val b = pixel and 255
                r in 80..245 && g in 40..190 && b in 20..150 && r > g * 1.15 && g > b * 1.1
            }
        }
        var renderedPixels = if (name == "02-companions") furPixels(bitmap) else Int.MAX_VALUE
        val deadline = android.os.SystemClock.uptimeMillis() + 5000
        while (renderedPixels <= 500 && android.os.SystemClock.uptimeMillis() < deadline) {
            bitmap.recycle()
            Thread.sleep(250)
            bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            renderedPixels = furPixels(bitmap)
        }
        val resolver = instrumentation.targetContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/StrictBlockerQA")
        }
        val uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        checkNotNull(resolver.openOutputStream(uri)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (name == "02-companions") {
            // Visible native pixels are required, not just a Compose label.
            assertTrue("The live 3D companion preview must be visible", renderedPixels > 500)
        }
        bitmap.recycle()
    }

    @Test fun sixStepSetupHasNoDockAndDoesNotRepeatAfterRecreation() {
        runBlocking { settings.clearAllUserPreferences() }
        compose.activityRule.scenario.recreate()
        waitFor("Welcome to Strict Blocker")
        compose.onNodeWithText("Home").assertDoesNotExist()
        compose.onNodeWithText("Settings").assertDoesNotExist()
        capture("01-setup")
        compose.onNodeWithText("Get started").performClick()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Honey")
        compose.onNodeWithText("Next").performClick()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("18")
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Male", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Next").performClick()
        waitFor("Choose your companion")
        capture("02-companions")
        compose.onNodeWithText("Next").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Nova")
        compose.onNodeWithText("Continue").performScrollTo().performClick()
        compose.waitUntil(30_000) { runBlocking { settings.isOnboardingCompletedFlow.first() } }
        compose.activityRule.scenario.recreate()
        waitFor("Hi, Honey")
        compose.onNodeWithText("Welcome to Strict Blocker").assertDoesNotExist()
        compose.onNodeWithText("Home").assertIsDisplayed()
        capture("03-home")
    }

    @Test fun quickRecordingHidesDockSavesAndSurvivesReopening() {
        runBlocking {
            settings.clearAllUserPreferences()
            settings.saveLocalProfile("Honey", 18, "male", "luna", "Luna")
            settings.setQuickTourSeen(true)
        }
        compose.activityRule.scenario.recreate()
        waitFor("Hi, Honey")
        compose.onNodeWithText("Wave").performScrollTo().performClick()
        compose.onNodeWithText("Feed").performScrollTo().performClick()
        compose.onNodeWithText("Crunch, crunch. Thank you!").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("3D companion. Drag to rotate, pinch to zoom, tap to pet.").performScrollTo()
        capture("08-bunny")
        compose.onNodeWithText("Record today").performScrollTo().performClick()
        waitFor("How was today?")
        compose.onNodeWithText("Home").assertDoesNotExist()
        capture("04-recording")
        compose.onNodeWithText("Good").performClick()
        compose.onNodeWithText("Save today's entry").performClick()
        compose.waitUntil(10_000) { runBlocking { settings.dailyCheckInsFlow.first().isNotEmpty() } }
        waitFor("Saved today")
        compose.onNodeWithText("Saved today").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1 day recorded · 1 of 7 this week").performScrollTo().assertIsDisplayed()
        capture("09-saved-entry")
        compose.onNodeWithText("Edit today's entry").performScrollTo().performClick()
        waitFor("How was today?")
        compose.onNodeWithText("Tough").performClick()
        compose.onNodeWithText("Save today's entry").performClick()
        compose.waitUntil(10_000) { runBlocking { settings.dailyCheckInsFlow.first().single().rating == "difficult" } }
        compose.activityRule.scenario.recreate()
        waitFor("Hi, Honey")
        assertEquals("difficult", runBlocking { settings.dailyCheckInsFlow.first().single().rating })
        compose.onNodeWithText("Saved today").assertIsDisplayed()
        assertEquals(1, runBlocking { settings.dailyCheckInsFlow.first().size })
        compose.onNodeWithText("Progress").performClick()
        waitFor("Your progress")
        capture("05-progress")
        compose.onNodeWithText("Settings").performClick()
        waitFor("Preferences and protection")
        capture("06-settings")
    }

    @Test fun proIsInTheDockAndPastelAppearanceChoicesPersist() {
        runBlocking {
            settings.clearAllUserPreferences()
            settings.saveLocalProfile("Honey", 18, "male", "luna", "Luna")
            settings.setQuickTourSeen(true)
        }
        compose.activityRule.scenario.recreate()
        waitFor("Hi, Honey")
        compose.onNodeWithText("Pro").performClick()
        waitFor("Meet Strict Blocker Pro")
        compose.onNodeWithTag("glass_dock").assertIsDisplayed()
        capture("10-pro")
        compose.onNodeWithText("Settings").performClick()
        waitFor("Rose")
        compose.onNodeWithText("Lilac").performClick()
        compose.waitUntil(5000) { runBlocking { settings.accentStyleFlow.first() == "lilac" } }
        compose.onNodeWithText("Dark").performClick()
        compose.waitUntil(5000) { runBlocking { settings.themeModeFlow.first() == "dark" } }
        capture("11-lilac-dark")
        compose.onNodeWithText("Light").performClick()
        capture("12-lilac-light")
        compose.activityRule.scenario.recreate()
        waitFor("Hi, Honey")
        assertEquals("lilac", runBlocking { settings.accentStyleFlow.first() })
        assertEquals("light", runBlocking { settings.themeModeFlow.first() })
        assertNull("The retired VPN must not be declared", compose.activity.packageManager.getPackageInfo(
            compose.activity.packageName, android.content.pm.PackageManager.GET_SERVICES).services
            ?.firstOrNull { it.permission == "android.permission.BIND_VPN_SERVICE" })
    }

    @Test fun firstUseTourExplainsThreeSectionsAndStaysDismissed() {
        runBlocking {
            settings.clearAllUserPreferences()
            settings.saveLocalProfile("Honey", 18, "male", "luna", "Luna")
        }
        compose.activityRule.scenario.recreate()
        waitFor("Welcome to your day")
        compose.onNodeWithText("Next tip").performClick()
        waitFor("Set your boundaries")
        compose.onNodeWithText("Next tip").performClick()
        waitFor("Make it yours")
        capture("13-quick-tour")
        compose.onNodeWithText("Let's begin").performClick()
        compose.waitUntil(5000) { runBlocking { settings.quickTourSeenFlow.first() } }
        compose.activityRule.scenario.recreate()
        waitFor("Hi, Honey")
        compose.onNodeWithText("Welcome to your day").assertDoesNotExist()
    }

    @Test fun scheduledReminderArrivesWhileAppIsInBackground() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fun shell(command: String) {
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
        }
        shell("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")
        shell("appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow")
        val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifications.cancel(6301)
        assertTrue("The exact test alarm must be registered", TrackerReminderScheduler.scheduleTest(context))
        compose.activity.runOnUiThread { compose.activity.moveTaskToBack(true) }
        compose.waitUntil(75_000) {
            notifications.activeNotifications.any { it.id == 6301 &&
                it.notification.extras.getCharSequence("android.title")?.toString() == "Reminder test" }
        }
        shell("cmd statusbar expand-notifications")
        capture("07-scheduled-notification")
        shell("cmd statusbar collapse")
    }
}
