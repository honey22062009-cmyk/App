package com.example.ui.components

import kotlin.random.Random

internal data class CompanionCue(
    val action: String,
    val line: String
)

/** Pure companion behaviour data, kept separate so idle cues and dialogue can be tested. */
internal object CompanionBehavior {
    private val idleCues = listOf(
        CompanionCue("curious", "Did you hear that? I thought I heard a tiny sound."),
        CompanionCue("ear_twitch", "Just checking that everything is cosy."),
        CompanionCue("stretch", "A little stretch makes room for a fresh start."),
        CompanionCue("look_around", "Taking a tiny look around. Everything seems peaceful."),
        CompanionCue("rest", "Taking one calm breath with you.")
    )

    private val dialogue = mapOf(
        "wave" to listOf(
            "Hey, you made it back.",
            "Hi there. I saved you a little wave.",
            "Hello again. Ready when you are."
        ),
        "jump" to listOf(
            "One little hop, then the next.",
            "Boing! A tiny celebration.",
            "That was fun. My ears nearly took off."
        ),
        "spin" to listOf(
            "A fresh perspective. Wheee!",
            "Checking every angle. Very important bunny work.",
            "Ta-da. I have returned to my original position."
        ),
        "dance" to listOf(
            "Tiny victory dance!",
            "That deserves a little celebration.",
            "I call this the focused-feet shuffle."
        ),
        "snack" to listOf(
            "Crunch, crunch. Thank you!",
            "A tiny snack for a tiny assistant.",
            "Mmm. Back to being your study-side companion."
        ),
        "rest" to listOf(
            "Breathe in gently, then let it go.",
            "A quiet moment. No rush.",
            "Let's loosen up and take one calm breath."
        ),
        "talk" to listOf(
            "You don't need a perfect day. Just your next small step.",
            "You have already started. That counts.",
            "Take the next thing one piece at a time.",
            "A pause is allowed. Your progress is not a race."
        ),
        "pet" to listOf(
            "That was a very good head pat.",
            "I felt that. More of a gentle pat, please.",
            "Ears approved. Thank you."
        )
    )

    val idleActionIds: Set<String> = idleCues.mapTo(linkedSetOf()) { it.action }

    fun nextIdleCue(random: Random = Random.Default): CompanionCue =
        idleCues[random.nextInt(idleCues.size)]

    fun dialogueOptions(action: String): List<String> =
        dialogue[action] ?: listOf("I'm right here. Keep going at your own pace.")

    fun dialogue(action: String, random: Random = Random.Default): String {
        val lines = dialogueOptions(action)
        return lines[random.nextInt(lines.size)]
    }
}
