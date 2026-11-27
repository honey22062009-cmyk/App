package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import com.example.R
import java.util.Locale

/** Original bundled effects and an installed offline voice. Never records a microphone. */
internal class CompanionAudio(private val context: Context, private val onStatus: (String) -> Unit) {
    private val sounds = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
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
            streams.add(sounds.play(id, .55f, .55f, 1, 0, 1f))
        }
    }
    fun say(text: String) {
        if (closed) return
        if (ready) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "companion"); return }
        pendingSpeech = text
        if (tts != null) return
        onStatus("Preparing an offline voice…")
        tts = TextToSpeech(context.applicationContext) { status ->
            if (closed) { tts?.shutdown(); return@TextToSpeech }
            val engine = tts
            val voice = if (status == TextToSpeech.SUCCESS) engine?.voices.orEmpty().filter {
                !it.isNetworkConnectionRequired && "notInstalled" !in it.features && it.locale.language == Locale.getDefault().language
            }.maxByOrNull { it.quality } ?: engine?.voices.orEmpty().filter {
                !it.isNetworkConnectionRequired && "notInstalled" !in it.features && it.locale.language == "en"
            }.maxByOrNull { it.quality } else null
            if (engine != null && voice != null) {
                engine.voice = voice
                engine.setPitch(1.18f); engine.setSpeechRate(.9f)
                ready = true
                pendingSpeech?.let { engine.speak(it, TextToSpeech.QUEUE_FLUSH, null, "companion") }
                pendingSpeech = null
                onStatus("Offline companion voice")
            } else {
                pendingSpeech = null
                onStatus("No offline voice installed. Sound effects still work.")
                effect("wave")
            }
        }
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
