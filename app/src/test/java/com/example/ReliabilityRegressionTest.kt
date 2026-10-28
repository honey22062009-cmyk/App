package com.example

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.system.TrackerReminderScheduler
import com.example.data.preferences.SettingsPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ReliabilityRegressionTest {
    private lateinit var context: Context
    private lateinit var settings: SettingsPreferences
    @Before fun prepare() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        settings = SettingsPreferences(context)
        settings.clearAllUserPreferences()
    }

    @Test fun completedSetupSurvivesRepositoryRecreationAndAccountSignOut() = runBlocking {
        settings.saveLocalProfile("Honey", 18, "male", "luna", "Moon")
        settings.signOutKeepRememberedAccount()
        val reopened = SettingsPreferences(context)
        assertTrue(reopened.isOnboardingCompletedFlow.first())
        assertEquals("Honey", reopened.userNameFlow.first())
        assertEquals("luna", reopened.selectedCharacterIdFlow.first())
        assertEquals("Moon", reopened.characterNameFlow.first())
    }

    @Test fun olderCompleteProfileRecoversAMissingCompletionFlag() = runBlocking {
        settings.saveLocalProfile("Honey", 18, "male", "nova", null)
        settings.setOnboardingCompleted(false)
        assertTrue(SettingsPreferences(context).isOnboardingCompletedFlow.first())
        settings.clearAllUserPreferences()
        assertFalse(settings.isOnboardingCompletedFlow.first())
    }

    @Test fun quickEntryUpdatesOneDayWithoutResettingStreakOrDiscardingSleepNotes() = runBlocking {
        settings.ensureStreakTrackerStarted()
        val started = settings.trackerStartedAtFlow.first()
        settings.saveSleepEntry("2026-10-28", 480, "Earlier bedtime")
        settings.saveDailyEntry("2026-10-28", "easy", 435)
        settings.saveDailyEntry("2026-10-28", "okay", null)
        assertEquals(1, settings.dailyCheckInsFlow.first().size)
        assertEquals("okay", settings.dailyCheckInsFlow.first().single().rating)
        assertEquals(435, settings.sleepEntriesFlow.first().single().durationMinutes)
        assertEquals("Earlier bedtime", settings.sleepEntriesFlow.first().single().note)
        assertEquals(started, settings.trackerStartedAtFlow.first())
    }

    @Test fun reminderTwoMinutesAwayStaysTodayAndPassedTimeMovesToTomorrow() {
        val now = ZonedDateTime.parse("2026-10-28T22:15:30+05:30[Asia/Kolkata]")
        assertEquals("2026-10-28T22:17+05:30[Asia/Kolkata]", TrackerReminderScheduler.nextOccurrence(22, 17, now).toString())
        assertEquals(now.toLocalDate().plusDays(1), TrackerReminderScheduler.nextOccurrence(22, 15, now).toLocalDate())
        assertEquals(6, TrackerReminderScheduler.nextOccurrence(6, 0, now).hour)
    }

    @Test fun alarmIsActuallyRegisteredAndDisablingReminderCancelsIt() = runBlocking {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadow = shadowOf(manager)
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(true)
        settings.setDailyReminder(true, 22, 17)
        TrackerReminderScheduler.refreshNow(context)
        val daily = shadow.scheduledAlarms.first { shadowOf(it.operation).savedIntent.action == TrackerReminderScheduler.ACTION_DAILY_CHECK_IN }
        assertTrue(daily.triggerAtTime > System.currentTimeMillis())
        settings.setDailyReminder(false, 22, 17)
        TrackerReminderScheduler.refreshNow(context)
        assertFalse(shadow.scheduledAlarms.any { shadowOf(it.operation).savedIntent.action == TrackerReminderScheduler.ACTION_DAILY_CHECK_IN })
    }
}
