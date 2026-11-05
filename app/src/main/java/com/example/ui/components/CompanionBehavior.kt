package com.example.ui.components

import kotlin.random.Random

internal data class CompanionCue(
    val action: String,
    val line: String
)

/** Dialogue and gentle idle behaviour are kept separate from rendering and app state. */
internal object CompanionBehavior {
    private val idleCues = listOf(
        CompanionCue("curious", "I heard a tiny sound. Investigating. Very serious work."),
        CompanionCue("ear_twitch", "One ear says hello. The other is still waking up."),
        CompanionCue("stretch", "A little stretch. Even tiny assistants need a reset."),
        CompanionCue("look_around", "Checking our cosy little corner. All clear."),
        CompanionCue("rest", "A quiet breath. Nothing needs doing this exact second."),
        CompanionCue("tail_wag", "Just a tiny tail wiggle. Carry on."),
        CompanionCue("listen", "I'm listening. Take your time."),
        CompanionCue("think", "Thinking very hard. The ears are helping.")
    )

    private val dialogue = mapOf(
        "wave" to listOf(
            "Hey, you're back. I kept a wave ready.",
            "Hello again. I hope your day has had a soft landing.",
            "There you are. One official bunny greeting.",
            "Hi! Both ears are reporting for duty.",
            "A little wave for your corner of the screen.",
            "Welcome back. We can take the next bit slowly."
        ),
        "jump" to listOf(
            "Boing! A little energy, delivered.",
            "That hop had excellent form. I checked.",
            "Up we go. And safely back on the floor.",
            "Tiny leap, surprisingly large celebration.",
            "My ears arrived a moment after the rest of me.",
            "One hop for the bunny, one small win for the day."
        ),
        "spin" to listOf(
            "A fresh perspective. Literally.",
            "Checking every angle. Very important bunny work.",
            "Ta-da. I have returned to my original position.",
            "I spun so you would not have to.",
            "The room is still here. Excellent news.",
            "A tiny turn can make a screen feel less boring."
        ),
        "dance" to listOf(
            "Tiny victory dance. No choreography required.",
            "That was the focused-feet shuffle.",
            "A little celebration belongs in every good plan.",
            "My left paw says this is a masterpiece.",
            "Dancing counts as a very small reset.",
            "And that concludes today's extremely exclusive performance."
        ),
        "snack" to listOf(
            // Keep the established UI-test phrases while adding more varied lines.
            "Crunch, crunch. Thank you!",
            "A tiny snack for a tiny assistant.",
            "Mmm. Back to being your study-side companion.",
            "Crunch, crunch. Thank you for the imaginary snack.",
            "Fuel acquired. Tiny assistant back online.",
            "That was delicious. I shall remember it dramatically.",
            "A snack break is a perfectly respectable pause.",
            "Mmm. Even my whiskers approve.",
            "Back to helping, with renewed bunny determination."
        ),
        "rest" to listOf(
            "Breathe in gently, then let the shoulders soften.",
            "A quiet moment. No rush.",
            "Unclench your jaw if you notice it is tense.",
            "A pause is part of a steady rhythm, not a failure.",
            "Let's take one calm breath and then choose the next step.",
            "You are allowed to rest between things."
        ),
        "talk" to listOf(
            "You do not need a perfect day. Just your next small step.",
            "You have already started. That counts.",
            "Take the next thing one piece at a time.",
            "A pause is allowed. Your progress is not a race.",
            "Let's make the next step small enough to actually begin.",
            "You can change the plan without giving up on yourself.",
            "One honest check-in is more useful than pretending everything is perfect.",
            "Progress can be quiet. It still counts."
        ),
        "pet" to listOf(
            "That was a very good head pat.",
            "Ears approved. Thank you.",
            "I felt that. Gentle pats, please. I am very important.",
            "One pat received. Bunny mood improved.",
            "My ears have filed a formal thank-you.",
            "You found the exact cosy spot."
        ),
        "curious" to listOf(
            "What was that? I am investigating with both ears.",
            "I noticed something interesting. It might just be a pixel.",
            "A curious mind makes a better companion.",
            "Looking around before we choose what comes next.",
            "I have a question. First, let me finish looking adorable."
        ),
        "ear_twitch" to listOf(
            "Did you see that? One ear moved before I decided to.",
            "Tiny ear check. Everything is still attached.",
            "My ears have their own plans today.",
            "I heard something kind. Or perhaps a notification.",
            "Ears up. Attention gently restored."
        ),
        "look_around" to listOf(
            "A quick look around. Our little corner is still cosy.",
            "Checking the room, then checking in with you.",
            "Nothing urgent spotted. We can focus on one thing.",
            "The view is nice from down here.",
            "I like noticing the small details."
        ),
        "stretch" to listOf(
            "Stretch your hands and let your shoulders relax.",
            "A gentle reset. No dramatic workout montage needed.",
            "Shake out the tension and return when you're ready.",
            "My paws are stretched. Your hands can have a turn.",
            "Small movement, fresh start."
        ),
        "cheer" to listOf(
            "That deserves a cheer!",
            "Tiny cheer squad, reporting for duty.",
            "Your effort deserves a little celebration.",
            "I am waving both paws. This is serious support.",
            "A small win is still a win."
        ),
        "listen" to listOf(
            "I'm listening. You can take your time.",
            "No need to rush the explanation.",
            "One thing at a time. I am all ears. Literally.",
            "Let's make room for the thought before the answer.",
            "You do not have to organise every word first."
        ),
        "think" to listOf(
            "Let's pause and pick the simplest next step.",
            "I am thinking. The ears are doing most of the work.",
            "A tiny plan beats ten imaginary perfect plans.",
            "We can break a big task into a smaller first move.",
            "One clear decision is enough for now."
        ),
        "focus" to listOf(
            "Choose one task. Let the rest wait its turn.",
            "A short focused session is a good place to begin.",
            "Put the next step where you can see it.",
            "One thing at a time. I will guard the imaginary clipboard.",
            "Let's make the first five minutes count."
        ),
        "tip" to listOf(
            "Try writing the very next action, not the whole plan.",
            "Put the most important task where you can see it.",
            "A quick daily check-in helps your progress view stay accurate.",
            "When a task feels large, make the starting step smaller.",
            "A reminder works best when it is specific and realistic.",
            "Review your goal occasionally. It should still make sense to you."
        ),
        "checkin" to listOf(
            "Let's save today's check-in while the day is still fresh.",
            "A quick record makes your weekly view more useful.",
            "No perfect answer needed. Just record how today actually felt.",
            "One small check-in. Then we can get back to the rest of your day.",
            "Today's entry is ready when you are."
        ),
        "goal" to listOf(
            "Pick a goal that feels clear and realistic, not impressive on paper.",
            "We can adjust a goal when life changes. That is allowed.",
            "A useful goal gives you direction without turning into pressure.",
            "Make the target specific enough to review later.",
            "Let's choose a next milestone you can actually track."
        ),
        "progress" to listOf(
            "Your progress page keeps the history in one place.",
            "Look at the pattern, not just one day.",
            "Your personal best is useful information, not a demand.",
            "A reset or a slower day does not erase everything you learned.",
            "Let's review what is working and keep what helps."
        ),
        "celebrate" to listOf(
            "You reached a milestone. Take a moment to notice it.",
            "That took consistency. A tiny celebration is due.",
            "Milestone spotted. Bunny-approved.",
            "Keep the achievement. Leave the pressure behind.",
            "A little progress deserves a little confetti."
        ),
        "remind" to listOf(
            "Quick reminder: save your check-in if you have not done it yet.",
            "Take a moment to review your goal. Does it still fit?",
            "The next step is allowed to be small.",
            "Check your schedule before you start another task.",
            "A short pause can help you decide what matters next."
        )
    )

    val idleActionIds: Set<String> = idleCues.mapTo(linkedSetOf()) { it.action }
    val dialogueActionIds: Set<String> = dialogue.keys

    fun nextIdleCue(random: Random = Random.Default): CompanionCue =
        idleCues[random.nextInt(idleCues.size)]

    fun dialogueOptions(action: String): List<String> =
        dialogue[action] ?: listOf("I'm right here. Keep going at your own pace.")

    fun dialogue(action: String, random: Random = Random.Default): String {
        val lines = dialogueOptions(action)
        return lines[random.nextInt(lines.size)]
    }

    fun dashboardNudge(
        currentDays: Int,
        bestDays: Int,
        goalDays: Int,
        checkInDone: Boolean
    ): String = when {
        !checkInDone -> "Today's check-in is still open. A quick honest entry keeps your weekly view useful."
        goalDays > 0 && currentDays >= goalDays -> "You reached your saved $goalDays-day goal. Review it or choose a new milestone."
        goalDays > 0 -> {
            val remaining = (goalDays - currentDays).coerceAtLeast(0)
            "$remaining ${if (remaining == 1) "day" else "days"} to your saved goal. Keep the next step realistic."
        }
        currentDays > 0 && currentDays >= bestDays -> "You're matching your personal best. Notice the progress, without turning it into pressure."
        currentDays > 0 -> "Your current streak is $currentDays days. Your progress history is there whenever you want to review it."
        else -> "Start with one small, useful action. You can record today's check-in or set a goal."
    }
}
