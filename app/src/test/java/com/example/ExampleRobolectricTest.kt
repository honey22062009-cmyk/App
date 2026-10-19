package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.system.SafetyWhitelist
import com.example.data.db.AppDatabase
import com.example.data.db.entity.AppEntity
import com.example.data.db.entity.ManagedAppEntity
import com.example.data.db.entity.ScheduleEntity
import com.example.data.db.entity.SchedulePeriodEntity
import com.example.ui.navigation.Screen
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Strict Blocker", appName)
    }

    @Test
    fun testScreenNavigationItems_neverNull() {
        val items = Screen.navItems
        assertEquals(5, items.size)
        items.forEach { screen ->
            assertNotNull(screen)
            assertNotNull(screen.route)
            assertNotNull(screen.title)
            assertNotNull(screen.selectedIcon)
            assertNotNull(screen.unselectedIcon)
            assertTrue(screen.route.isNotEmpty())
        }
    }

    @Test
    fun testAdultContentEvaluator_identifiesAdultDomainsAndSafeSearch() {
        // Adult domain evaluation
        val pornResult = com.example.core.evaluator.AdultContentEvaluator.evaluateText("https://www.pornhub.com/video")
        assertTrue(pornResult is com.example.core.evaluator.AdultContentEvaluator.EvaluationResult.Blocked)

        val cleanResult = com.example.core.evaluator.AdultContentEvaluator.evaluateText("https://en.wikipedia.org/wiki/Android")
        assertTrue(cleanResult is com.example.core.evaluator.AdultContentEvaluator.EvaluationResult.Clean)

        // Browser package identification
        assertTrue(com.example.core.evaluator.AdultContentEvaluator.isBrowserPackage("com.android.chrome"))
        assertTrue(com.example.core.evaluator.AdultContentEvaluator.isBrowserPackage("org.mozilla.firefox"))
        assertFalse(com.example.core.evaluator.AdultContentEvaluator.isBrowserPackage("com.android.calculator2"))
    }

    @Test
    fun testSafetyWhitelist_systemAppsNeverBlockable() {
        assertTrue(SafetyWhitelist.isSystemCritical(context, context.packageName))
        assertTrue(SafetyWhitelist.isSystemCritical(context, "com.android.systemui"))
        assertTrue(SafetyWhitelist.isSystemCritical(context, "com.android.settings"))
        assertTrue(SafetyWhitelist.isSystemCritical(context, "com.sec.android.app.launcher"))

        assertFalse(SafetyWhitelist.isSystemCritical(context, "com.google.android.youtube"))
        assertFalse(SafetyWhitelist.isSystemCritical(context, "com.instagram.android"))
    }

    @Test
    fun testRoomDatabase_insertAndQueryManagedApp() = runBlocking {
        val dao = database.managedAppDao()
        val app = ManagedAppEntity(
            packageName = "com.google.android.youtube",
            appName = "YouTube",
            isManaged = true
        )
        dao.insertOrUpdate(app)

        val retrieved = dao.getByPackage("com.google.android.youtube")
        assertNotNull(retrieved)
        assertEquals("YouTube", retrieved?.appName)
        assertTrue(retrieved?.isManaged == true)
    }

    @Test
    fun testRoomDatabase_schedulePeriodPersistence() = runBlocking {
        database.managedAppDao().insertOrUpdate(
            ManagedAppEntity(
                packageName = "com.google.android.youtube",
                appName = "YouTube",
                isManaged = true
            )
        )
        val scheduleDao = database.scheduleDao()
        val period = SchedulePeriodEntity(
            appPackage = "com.google.android.youtube",
            dayOfWeek = 1, // Monday
            startHour = 18,
            startMinute = 0,
            endHour = 19,
            endMinute = 0,
            isEnabled = true
        )
        val id = scheduleDao.insert(period)
        assertTrue(id > 0)

        val retrieved = scheduleDao.getPeriodsForApp("com.google.android.youtube")
        assertEquals(1, retrieved.size)
        assertEquals(18, retrieved[0].startHour)
        assertEquals(19, retrieved[0].endHour)
    }

    @Test
    fun testAppEntity_and_ScheduleEntity_foreignKeyRelationship() = runBlocking {
        val appDao = database.appDao()
        val scheduleEntityDao = database.scheduleEntityDao()

        val app = AppEntity(
            packageName = "com.example.sampleapp",
            appName = "Sample App",
            isManaged = true
        )
        appDao.insertOrUpdate(app)

        val retrievedApp = appDao.getByPackage("com.example.sampleapp")
        assertNotNull(retrievedApp)
        assertEquals("Sample App", retrievedApp?.appName)

        val schedule = ScheduleEntity(
            appPackage = "com.example.sampleapp",
            dayOfWeek = 2, // Tuesday
            startHour = 14,
            startMinute = 0,
            endHour = 16,
            endMinute = 30,
            isEnabled = true
        )
        val scheduleId = scheduleEntityDao.insert(schedule)
        assertTrue(scheduleId > 0)

        val schedules = scheduleEntityDao.getSchedulesForApp("com.example.sampleapp")
        assertEquals(1, schedules.size)
        assertEquals(14, schedules[0].startHour)
        assertEquals(16, schedules[0].endHour)

        // Verify cascading delete: deleting AppEntity cascades to delete ScheduleEntity
        appDao.delete(app)
        val remainingSchedules = scheduleEntityDao.getSchedulesForApp("com.example.sampleapp")
        assertEquals(0, remainingSchedules.size)
    }

    @Test
    fun testAppContainer_dependencyInjectionInstantiation() {
        val container = com.example.core.di.DefaultAppContainer(context)
        assertNotNull(container.database)
        assertNotNull(container.appDao)
        assertNotNull(container.scheduleEntityDao)
        assertNotNull(container.managedAppDao)
        assertNotNull(container.scheduleDao)
        assertNotNull(container.blockEventDao)
        assertNotNull(container.settingsPreferences)
        assertNotNull(container.repository)
    }

    @Test
    fun testDataDeletionRequest_purgesAllRecords() = runBlocking {
        val repo = com.example.data.repository.AppBlockerRepository(
            context = context,
            database = database,
            managedAppDao = database.managedAppDao(),
            scheduleDao = database.scheduleDao(),
            blockEventDao = database.blockEventDao(),
            settings = com.example.data.preferences.SettingsPreferences(context)
        )

        // Insert sample records
        database.managedAppDao().insertOrUpdate(
            ManagedAppEntity(packageName = "com.test.app", appName = "Test App", isManaged = true)
        )
        database.scheduleDao().insert(
            SchedulePeriodEntity(appPackage = "com.test.app", dayOfWeek = 1, startHour = 8, startMinute = 0, endHour = 9, endMinute = 0)
        )
        database.blockEventDao().insert(
            com.example.data.db.entity.BlockEventEntity(appPackage = "com.test.app", appName = "Test App", reason = "Blocked")
        )

        // Verify inserted
        assertEquals(1, database.managedAppDao().getActiveManagedApps().size)
        assertEquals(1, database.scheduleDao().getPeriodsForApp("com.test.app").size)

        // Execute data deletion request
        repo.deleteAllUserData()

        // Verify complete purge
        assertEquals(0, database.managedAppDao().getActiveManagedApps().size)
        assertEquals(0, database.scheduleDao().getPeriodsForApp("com.test.app").size)
    }
}
