package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.os.Bundle
import com.example.R
import java.util.Locale

/** Original bundled effects and an installed offline voice. Never records a microphone. */
internal class CompanionAudio(private val context: Context, private val onStatus: (String) -> Unit) {
    private val sounds = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val ids = listOf(R.raw.buddy_chirp, R.raw.buddy_hop, R.raw.buddy_munch, R.raw.buddy_chime).map { sounds.load(context, it, 1) }
    private val loaded = mutableSetOf<Int>()
    private var tts: TextToSpeech? = null
    private var ready = false
    private var closed = false
    private var pendingSpeech: String? = null
    private val streams = mutableListOf<Int>()
    init { sounds.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) } }

    fun effect(action: String) {
        if (closed) return
        val index = when (action) { "jump", "dance" -> 1; "snack" -> 2; "cheer", "spin" -> 3; else -> 0 }
        val id = ids[index]
        if (id in loaded) {
            streams.forEach(sounds::stop); streams.clear()
            streams.add(sounds.play(id, .42f, .42f, 1, 0, 1f))
        }
    }
    fun say(text: String) {
        if (closed || text.isBlank()) return
        if (ready) {
            speakNow(text)
            return
        }
        pendingSpeech = text
        if (tts != null) return

        onStatus("Preparing your companion's offline voice…")
        tts = TextToSpeech(context.applicationContext) { status ->
            if (closed) {
                tts?.shutdown()
                return@TextToSpeech
            }
            val engine = tts
            val offlineVoices = if (status == TextToSpeech.SUCCESS) {
                engine?.voices.orEmpty().filter { voice ->
                    !voice.isNetworkConnectionRequired &&
                        "notInstalled" !in voice.features &&
                        voice.quality >= Voice.QUALITY_NORMAL
                }
            } else {
                emptyList()
            }
            // The companion speaks English by default. Prefer a fully installed,
            // on-device Indian-English voice, then other English voices. No network voice
            // is ever selected, so speech continues to work offline.
            val englishVoices = offlineVoices.filter { it.locale.language == "en" }
            val voice = englishVoices.maxWithOrNull(
                compareByDescending<Voice> {
                    when (it.locale.country.uppercase(Locale.ROOT)) {
                        "IN" -> 3
                        "GB", "US", "AU", "CA" -> 2
                        else -> 1
                    }
                }.thenByDescending { it.quality }
                    .thenBy { it.latency }
            ) ?: offlineVoices
                .filter { it.locale.language == Locale.getDefault().language }
                .maxByOrNull { it.quality }
                ?: offlineVoices.maxByOrNull { it.quality }

            if (engine != null && voice != null) {
                engine.voice = voice
                engine.setLanguage(voice.locale)
                // A warmer pitch and slightly slower pace make longer lines easier to follow.
                engine.setPitch(1.12f)
                engine.setSpeechRate(.90f)
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                ready = true
                pendingSpeech?.let(::speakNow)
                pendingSpeech = null
                onStatus("Companion voice ready · ${voice.locale.displayName} · offline")
            } else {
                pendingSpeech = null
                onStatus("No offline voice is installed. Sound effects still work.")
                effect("wave")
            }
        }
    }

    private fun speakNow(text: String) {
        val parameters = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, .92f)
            putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, 0f)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, parameters, "companion-${System.currentTimeMillis()}")
    }

    fun stop() {
        pendingSpeech = null
        tts?.stop()
        streams.forEach(sounds::stop); streams.clear()
    }
    fun close() {
        if (closed) return
        closed = true; stop(); tts?.shutdown(); sounds.release()
    }
}
