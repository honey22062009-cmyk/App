package com.example

import android.graphics.Bitmap
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
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "qa").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
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
            settings.saveLocalProfile("Honey", 18, "male", "nova", "Nova")
        }
        compose.activityRule.scenario.recreate()
        waitFor("Hi, Honey")
        compose.onNodeWithText("Wave").performClick()
        compose.onNodeWithText("Record today").performScrollTo().performClick()
        waitFor("How was today?")
        compose.onNodeWithText("Home").assertDoesNotExist()
        capture("04-recording")
        compose.onNodeWithText("Good").performClick()
        compose.onNodeWithText("Save today's entry").performClick()
        compose.waitUntil(10_000) { runBlocking { settings.dailyCheckInsFlow.first().isNotEmpty() } }
        compose.activityRule.scenario.recreate()
        waitFor("Hi, Honey")
        assertEquals("easy", runBlocking { settings.dailyCheckInsFlow.first().single().rating })
        compose.onNodeWithText("Progress").performClick()
        waitFor("Your progress")
        capture("05-progress")
        compose.onNodeWithText("Settings").performClick()
        waitFor("Preferences and protection")
        capture("06-settings")
    }
}
