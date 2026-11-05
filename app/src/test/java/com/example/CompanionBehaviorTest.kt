package com.example

import com.example.ui.components.CompanionBehavior
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionBehaviorTest {
    @Test
    fun automaticIdleCuesAreAvailableAndHaveDialogue() {
        val expected = setOf("curious", "ear_twitch", "stretch", "look_around", "rest", "tail_wag", "listen", "think")
        assertEquals(expected, CompanionBehavior.idleActionIds)

        repeat(100) { index ->
            val cue = CompanionBehavior.nextIdleCue(Random(index))
            assertTrue(cue.action in expected)
            assertTrue(cue.line.isNotBlank())
        }
    }

    @Test
    fun manualActionsHaveSeveralContextualLines() {
        val actions = listOf(
            "wave", "jump", "spin", "dance", "snack", "rest", "talk", "pet",
            "curious", "ear_twitch", "look_around", "stretch", "cheer", "listen",
            "think", "focus", "tip", "checkin", "goal", "progress", "celebrate", "remind"
        )
        actions.forEach { action ->
            val options = CompanionBehavior.dialogueOptions(action)
            assertTrue("$action should have multiple lines", options.size >= 2)
            assertTrue(options.all(String::isNotBlank))
            repeat(10) { index ->
                assertTrue(CompanionBehavior.dialogue(action, Random(index)) in options)
            }
        }
    }

    @Test
    fun dashboardNudgeReflectsSavedCheckInAndGoal() {
        assertTrue(CompanionBehavior.dashboardNudge(3, 5, 0, false).contains("check-in"))
        assertTrue(CompanionBehavior.dashboardNudge(3, 5, 7, true).contains("4 days"))
        assertTrue(CompanionBehavior.dashboardNudge(7, 7, 7, true).contains("reached"))
        assertTrue(CompanionBehavior.dashboardNudge(5, 5, 0, true).contains("personal best"))
    }

    @Test
    fun unknownActionFallsBackToAUsefulLine() {
        val options = CompanionBehavior.dialogueOptions("unknown")
        assertEquals(1, options.size)
        assertFalse(options.single().isBlank())
    }
}
