package com.example.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyFocusTaskTest {
    @Test
    fun taskRoundTripsWithUnicodeAndCompletionState() {
        val original = DailyFocusTask.create(
            localDate = LocalDate.of(2026, 11, 12),
            title = "Revise Biology · Human Physiology"
        ).copy(completed = true, createdAtMillis = 1_794_456_789_000L)

        val restored = DailyFocusTask.decode(original.encode())

        assertEquals(original, restored)
    }

    @Test
    fun taskTitleIsTrimmedAndLimitedToEightyCharacters() {
        val task = DailyFocusTask.create(
            localDate = LocalDate.of(2026, 11, 12),
            title = "  " + "A".repeat(120) + "  "
        )

        assertEquals(80, task.title.length)
        assertEquals(task, DailyFocusTask.decode(task.encode()))
    }

    @Test
    fun malformedTaskRecordsAreIgnored() {
        assertNull(DailyFocusTask.decode(""))
        assertNull(DailyFocusTask.decode("id|not-a-date|true|123|bad"))
        assertNull(DailyFocusTask.decode("id|2026-11-12|maybe|123|dGVzdA"))
    }

    @Test
    fun completedStateCanBeChangedWithoutChangingTaskIdentity() {
        val task = DailyFocusTask.create(LocalDate.of(2026, 11, 12), "Finish chemistry notes")
        val completed = task.copy(completed = true)

        assertEquals(task.id, completed.id)
        assertEquals(task.title, completed.title)
        assertTrue(completed.completed)
    }
}
