package com.example

import com.example.data.preferences.FocusSessionRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionRecordTest {
    @Test
    fun recordRoundTripsWithReadableActivityAndStatus() {
        val original = FocusSessionRecord.create(
            startedAtMillis = 1_800_000_000_000L,
            plannedSeconds = 45 * 60,
            actualSeconds = 45 * 60,
            activity = "  Revision  ",
            taskName = "  Biology   revision  ",
            completed = true
        )
        val decoded = FocusSessionRecord.decode(original.encode())
        assertEquals(original, decoded)
        assertEquals("Revision", decoded?.activity)
        assertEquals("Biology revision", decoded?.taskName)
        assertTrue(decoded?.completed == true)
    }

    @Test
    fun unfinishedSessionIsStoredAsEndedEarlyAndTimesAreClamped() {
        val record = FocusSessionRecord.create(
            startedAtMillis = 0L,
            plannedSeconds = 25 * 60,
            actualSeconds = 12 * 60,
            activity = "",
            completed = true
        )
        assertEquals(1L, record.startedAtMillis)
        assertEquals("Study", record.activity)
        assertEquals("", record.taskName)
        assertFalse(record.completed)
        assertEquals(12 * 60, FocusSessionRecord.decode(record.encode())?.actualSeconds)
    }

    @Test
    fun malformedRecordsAreIgnored() {
        assertNull(FocusSessionRecord.decode("bad|record"))
    }
}
