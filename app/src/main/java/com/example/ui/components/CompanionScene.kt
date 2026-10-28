package com.example.ui.components

import android.content.Context
import android.opengl.GLES20
import android.opengl.EGL14
import android.graphics.SurfaceTexture
import android.view.TextureView
import android.view.Surface
import android.opengl.Matrix
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.delay
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/** Real, lit, depth-tested 3D geometry. No network, WebView or downloaded model. */
@Composable
fun CompanionScene(
    characterId: String,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    reduceMotion: Boolean = false,
    reaction: String = "idle",
    reactionKey: Int = 0,
    backgroundColor: androidx.compose.ui.graphics.Color? = null,
    onPet: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val background = (backgroundColor ?: MaterialTheme.colorScheme.surface).toArgb()
    val view = remember(context) { CompanionTextureView(context) }
    var warmingUp by remember(view) { mutableStateOf(true) }
    LaunchedEffect(view) {
        // Static previews need frames after the parent entrance transition, too.
        delay(1200)
        warmingUp = false
        view.requestRender()
    }
    DisposableEffect(view, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            view.onPause()
        }
    }
    AndroidView(
        factory = { view },
        modifier = modifier.onGloballyPositioned { view.requestRender() }.semantics {
            contentDescription = if (interactive) "3D companion. Drag to rotate, pinch to zoom, tap to pet."
                else "3D companion preview"
        },
        update = {
            it.onPet = onPet
            it.interactive = interactive
            it.scene.character = characterId
            it.scene.background = background
            it.scene.reducedMotion = reduceMotion
            it.continuous = warmingUp || (interactive && !reduceMotion)
            it.react(reaction, reactionKey)
            it.requestRender()
        }
    )
}

@Composable
fun InteractiveCompanion(characterId: String, name: String, reduceMotion: Boolean, modifier: Modifier = Modifier,
    backgroundColor: Color? = null, soundEnabled: Boolean = false, onSoundChange: (Boolean) -> Unit = {}) {
    var reaction by remember { mutableStateOf("idle") }
    var key by remember { mutableStateOf(0) }
    var caption by remember { mutableStateOf("A little company for your day.") }
    var audioStatus by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val audio = remember(context) { CompanionAudio(context) { audioStatus = it } }
    DisposableEffect(audio, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) audio.stop() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); audio.close() }
    }
    LaunchedEffect(soundEnabled) { if (!soundEnabled) audio.stop() }
    fun act(action: String) {
        reaction = action; key++
        caption = when (action) {
            "wave" -> "Hey, I'm right here."
            "jump" -> "One little hop at a time!"
            "spin" -> "A fresh perspective. Wheee!"
            "dance" -> "Tiny victory dance!"
            "snack" -> "Crunch, crunch. Thank you!"
            "rest" -> "Breathe in… and slowly out."
            "talk" -> "You don't need a perfect day. Just your next small step."
            else -> "That feels nice. You’ve got this."
        }
        if (soundEnabled) {
            if (action == "talk") audio.say(caption) else audio.effect(action)
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().background(
            Brush.radialGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .7f), Color.Transparent)),
            RoundedCornerShape(28.dp))) {
            CompanionScene(characterId, Modifier.fillMaxWidth().height(260.dp), true, reduceMotion,
                reaction, key, backgroundColor ?: Color.Transparent, onPet = { act("pet") })
        }
        Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        listOf("wave" to "Wave", "jump" to "Jump", "spin" to "Spin", "dance" to "Dance", "snack" to "Feed", "rest" to "Breathe")
            .chunked(3).forEach { actions ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    actions.forEach { (id, label) ->
                        FilledTonalButton(onClick = { act(id) }, modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 8.dp)) {
                            Text(label, maxLines = 1)
                        }
                    }
                }
            }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { act("talk"); if (!soundEnabled) audioStatus = "Turn on sounds to hear the offline voice." }) { Text("Say something") }
            TextButton(onClick = { onSoundChange(!soundEnabled); audioStatus = "" }) { Text(if (soundEnabled) "Sound on" else "Sound off") }
        }
        if (audioStatus.isNotBlank()) Text(audioStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${name.ifBlank { characterId.replaceFirstChar(Char::titlecase) }} · drag, pinch or tap to pet",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private class CompanionTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {
    val scene = CompanionState()
    @Volatile private var worker: DrawThread? = null
    @Volatile private var paused = false
    @Volatile var continuous = false
        set(value) { field = value; requestRender() }
    var interactive = false
    var onPet: (() -> Unit)? = null
    private var previousX = 0f
    private var previousY = 0f
    private var moved = false
    private var lastReactionKey = -1
    private val pinch = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scene.zoom = (scene.zoom / detector.scaleFactor).coerceIn(3.6f, 6.5f)
            moved = true
            requestRender()
            return true
        }
    })

    init {
        // TextureView participates in Compose alpha, clipping and screen transitions.
        isOpaque = false
        surfaceTextureListener = this
    }

    fun onResume() { paused = false; requestRender() }
    fun onPause() { paused = true; requestRender() }
    fun requestRender() { worker?.requestFrame() }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        worker?.stopRendering()
        worker = DrawThread(texture, width, height).apply { start() }
    }
    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        worker?.resize(width, height)
    }
    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        val current = worker
        worker = null
        current?.stopRendering()
        if (current == null) texture.release()
        // The render thread releases its SurfaceTexture only after EGL has detached.
        return false
    }

    private inner class DrawThread(
        private val texture: SurfaceTexture,
        @Volatile private var frameWidth: Int,
        @Volatile private var frameHeight: Int
    ) : Thread("StrictBlocker-Companion") {
        private val signal = Object()
        @Volatile private var running = true
        @Volatile private var requested = true
        @Volatile private var resized = true
        fun requestFrame() { requested = true; synchronized(signal) { signal.notifyAll() } }
        fun resize(width: Int, height: Int) {
            frameWidth = width; frameHeight = height; resized = true; requestFrame()
        }
        fun stopRendering() { running = false; synchronized(signal) { signal.notifyAll() } }

        override fun run() {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            var context = EGL14.EGL_NO_CONTEXT
            var target = EGL14.EGL_NO_SURFACE
            val surface = Surface(texture)
            try {
                val version = IntArray(2)
                check(EGL14.eglInitialize(display, version, 0, version, 1))
                val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
                val count = IntArray(1)
                val attributes = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_DEPTH_SIZE, 16, EGL14.EGL_NONE)
                check(EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0) && count[0] > 0)
                context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT,
                    intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
                check(context != EGL14.EGL_NO_CONTEXT)
                target = EGL14.eglCreateWindowSurface(display, configs[0], surface, intArrayOf(EGL14.EGL_NONE), 0)
                check(target != EGL14.EGL_NO_SURFACE && EGL14.eglMakeCurrent(display, target, target, context))
                EGL14.eglSwapInterval(display, 1)
                val renderer = CompanionRenderer(scene)
                renderer.initialize()
                while (running) {
                    synchronized(signal) {
                        while (running && (paused || (!requested && !continuous))) signal.wait()
                    }
                    if (!running) break
                    requested = false
                    val started = SystemClock.uptimeMillis()
                    if (resized) { resized = false; renderer.resize(frameWidth, frameHeight) }
                    renderer.drawFrame()
                    if (!EGL14.eglSwapBuffers(display, target)) break
                    if (continuous) {
                        val remaining = 33L - (SystemClock.uptimeMillis() - started)
                        if (remaining > 0) SystemClock.sleep(remaining)
                    }
                }
            } catch (error: Exception) {
                android.util.Log.w("StrictBlocker", "Companion renderer stopped", error)
            } finally {
                if (display != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    if (target != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, target)
                    if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
                    EGL14.eglTerminate(display)
                    EGL14.eglReleaseThread()
                }
                surface.release()
                texture.release()
            }
        }
    }

    fun react(action: String, key: Int) {
        if (key == lastReactionKey) return
        lastReactionKey = key
        scene.reaction = action
        scene.reactionAt = SystemClock.uptimeMillis()
        if (scene.reducedMotion && action == "spin") scene.yaw += 90f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactive) return false
        pinch.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                previousX = event.x; previousY = event.y; moved = false
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - previousX
                val dy = event.y - previousY
                if (abs(dx) + abs(dy) > 3f) moved = true
                if (!pinch.isInProgress) {
                    scene.yaw += dx * .5f
                    scene.pitch = (scene.pitch + dy * .2f).coerceIn(-18f, 24f)
                }
                previousX = event.x; previousY = event.y
                requestRender()
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (!moved) performClick()
            }
            MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        onPet?.invoke()
        scene.reaction = "pet"
        scene.reactionAt = SystemClock.uptimeMillis()
        requestRender()
        return true
    }
}

private class CompanionState {
    @Volatile var character = "nova"
    @Volatile var background = 0xfffafaf8.toInt()
    @Volatile var reducedMotion = false
    @Volatile var yaw = -12f
    @Volatile var pitch = 4f
    @Volatile var zoom = 4.8f
    @Volatile var reaction = "idle"
    @Volatile var reactionAt = 0L
}

private class CompanionRenderer(private val state: CompanionState) {
    private val character get() = state.character
    private val background get() = state.background
    private val reducedMotion get() = state.reducedMotion
    private val yaw get() = state.yaw
    private val pitch get() = state.pitch
    private val zoom get() = state.zoom
    private val reaction get() = state.reaction
    private val reactionAt get() = state.reactionAt
    private var program = 0
    private lateinit var uniforms: IntArray
    private lateinit var sphere: Mesh
    private lateinit var cone: Mesh
    private lateinit var bunnyBody: Mesh
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val base = FloatArray(16)
    private val model = FloatArray(16)
    private val mv = FloatArray(16)
    private val mvp = FloatArray(16)
    private val inverse = FloatArray(16)
    private val normal = FloatArray(16)
    private var aspect = 1f

    fun initialize() {
        program = GLES20.glCreateProgram()
        fun shader(type: Int, source: String): Int = GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, source); GLES20.glCompileShader(it)
            val status = IntArray(1); GLES20.glGetShaderiv(it, GLES20.GL_COMPILE_STATUS, status, 0)
            check(status[0] != 0) { GLES20.glGetShaderInfoLog(it) }
        }
        val vertex = shader(GLES20.GL_VERTEX_SHADER, """
            uniform mat4 uMvp; uniform mat4 uModel; uniform mat4 uNormal;
            attribute vec3 aPosition; attribute vec3 aNormal;
            varying vec3 vNormal; varying vec3 vPosition;
            void main() {
                gl_Position = uMvp * vec4(aPosition, 1.0);
                vPosition = (uModel * vec4(aPosition, 1.0)).xyz;
                vNormal = normalize((uNormal * vec4(aNormal, 0.0)).xyz);
            }
        """.trimIndent())
        val fragment = shader(GLES20.GL_FRAGMENT_SHADER, """
            precision mediump float; uniform vec3 uColor;
            varying vec3 vNormal; varying vec3 vPosition;
            void main() {
                vec3 n = normalize(vNormal);
                vec3 l = normalize(vec3(-3.0, 5.0, 6.0) - vPosition);
                float diffuse = max(dot(n, l), 0.0);
                float rim = pow(1.0 - max(dot(n, vec3(0.0, 0.0, 1.0)), 0.0), 3.0);
                vec3 h = normalize(l + vec3(0.0, 0.0, 1.0));
                float specular = pow(max(dot(n, h), 0.0), 28.0) * 0.075;
                gl_FragColor = vec4(uColor * (0.44 + 0.56 * diffuse) + specular + rim * 0.055, 1.0);
            }
        """.trimIndent())
        GLES20.glAttachShader(program, vertex); GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)
        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
        check(linked[0] != 0) { GLES20.glGetProgramInfoLog(program) }
        uniforms = arrayOf("uMvp", "uModel", "uNormal", "uColor").map { GLES20.glGetUniformLocation(program, it) }.toIntArray()
        GLES20.glDeleteShader(vertex); GLES20.glDeleteShader(fragment)
        sphere = Mesh(CompanionGeometry.sphere())
        cone = Mesh(CompanionGeometry.cone())
        bunnyBody = Mesh(CompanionGeometry.bunnyBody())
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    fun resize(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspect = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(projection, 0, 37f, aspect, .1f, 40f)
    }

    fun drawFrame() {
        val bg = background
        GLES20.glClearColor(((bg shr 16) and 255) / 255f, ((bg shr 8) and 255) / 255f, (bg and 255) / 255f, ((bg ushr 24) and 255) / 255f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        Matrix.setLookAtM(view, 0, 0f, .4f, zoom, 0f, .20f, 0f, 0f, 1f, 0f)
        val time = SystemClock.uptimeMillis() / 1000f
        val elapsed = ((SystemClock.uptimeMillis() - reactionAt) / 1000f).coerceAtLeast(0f)
        val duration = when (reaction) { "rest" -> 8f; "dance" -> 3.6f; "snack", "talk" -> 3f; else -> 2f }
        val acting = !reducedMotion && elapsed < duration
        val phase = (elapsed / duration).coerceIn(0f, 1f)
        val pulse = sin(phase * PI.toFloat()).coerceAtLeast(0f)
        val bounce = when {
            reducedMotion -> 0f
            acting && reaction == "jump" -> abs(sin(phase * PI.toFloat() * 2)) * .52f
            acting && reaction == "dance" -> abs(sin(elapsed * 10f)) * .10f * pulse
            else -> sin(time * 2f) * .022f
        }
        val spin = if (acting && reaction == "spin") 360f * phase.let { it * it * (3f - 2f * it) } else 0f
        Matrix.setIdentityM(base, 0)
        // A soft stage shadow makes the character feel grounded, including mid-jump.
        draw(sphere, 0f, -.93f, -.06f, .47f - bounce * .18f, .025f, .29f - bounce * .10f, 0xdccbdc)
        Matrix.translateM(base, 0, if (acting && reaction == "dance") sin(elapsed * 5f) * .14f * pulse else 0f, bounce - .06f, 0f)
        Matrix.rotateM(base, 0, pitch + if (acting && reaction == "snack") sin(elapsed * 12f) * 3f else 0f, 1f, 0f, 0f)
        Matrix.rotateM(base, 0, yaw + spin, 0f, 1f, 0f)
        if (acting && reaction in setOf("pet", "dance")) Matrix.rotateM(base, 0, sin(elapsed * 7f) * 7f * pulse, 0f, 0f, 1f)
        val breathe = if (acting && reaction == "rest") sin(elapsed * PI.toFloat() / 4f) * .045f else 0f
        Matrix.scaleM(base, 0, 1f + breathe, 1f + breathe * .5f, 1f + breathe)
        val coat = when (character) {
            "milo" -> 0xc4a782; "aria" -> 0xe9c6a4; "kai" -> 0x54666a
            "luna" -> 0xffeee8; "sol" -> 0xf1cb68; else -> 0xdf8d56
        }
        val cream = 0xffefda
        val dark = 0x3b2c3e
        fun ball(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, color: Int, rz: Float = 0f) =
            draw(sphere, x, y, z, sx, sy, sz, color, rz)
        if (character == "luna") {
            // One continuous surface joins torso, neck and head; no floating head or ball joints.
            draw(bunnyBody, 0f, 0f, 0f, 1f, 1f, 1f, coat)
            ball(0f, -.30f, .285f, .29f, .32f, .055f, 0xfff9f0)
            ball(-.25f, -.78f, .14f, .25f, .20f, .34f, coat)
            ball(.25f, -.78f, .14f, .25f, .20f, .34f, coat)
            ball(-.25f, -.76f, .43f, .12f, .065f, .024f, 0xe6a0b6)
            ball(.25f, -.76f, .43f, .12f, .065f, .024f, 0xe6a0b6)
            ball(0f, -.46f, -.40f, .23f, .23f, .23f, 0xfffaf3)
        } else {
            ball(0f, -.23f, -.05f, .48f, .61f, .36f, coat)
            // Neck overlaps the torso and head all the way around.
            ball(0f, .18f, .0f, .34f, .27f, .30f, coat)
            ball(0f, -.25f, .28f, .31f, .39f, .11f, cream)
            ball(-.27f, -.78f, .16f, .23f, .18f, .31f, coat)
            ball(.27f, -.78f, .16f, .23f, .18f, .31f, coat)
            ball(.38f, -.48f, -.32f, .22f, .37f, .20f, coat, -40f)
            ball(.54f, -.20f, -.34f, .15f, .19f, .17f, cream)
            ball(0f, .57f, .03f, .63f, .56f, .47f, coat)
        }
        // Articulated limbs pivot at the shoulder, with overlapping shoulder sockets.
        fun arm(side: Float, angle: Float) {
            val saved = base.copyOf()
            Matrix.translateM(base, 0, side * .40f, .01f, 0f)
            Matrix.rotateM(base, 0, angle, 0f, 0f, 1f)
            ball(0f, -.17f, 0f, .18f, .30f, .20f, coat)
            ball(0f, -.36f, .015f, .17f, .17f, .18f, coat)
            System.arraycopy(saved, 0, base, 0, 16)
        }
        val wave = when {
            reducedMotion && reaction == "wave" -> 125f
            acting && reaction == "wave" -> sin(elapsed * 15f) * 22f * pulse + 125f * pulse
            acting && reaction == "dance" -> (sin(elapsed * 10f) * 35f + 50f) * pulse
            acting && reaction == "snack" -> 65f * pulse
            else -> 12f
        }
        arm(-1f, if (acting && reaction == "snack") -65f * pulse else -12f)
        arm(1f, wave)
        if (acting && reaction == "snack") {
            draw(cone, .05f, .20f, .48f, .11f, .30f, .10f, 0xf09a68, 150f)
            ball(.10f, .40f, .47f, .13f, .08f, .08f, 0xb9a1dd)
        }
        // Species: fox, bear, cat, panda, rabbit and owl. Each has actual 3D ears/features.
        if (character == "milo" || character == "kai") {
            ball(-.48f, 1.0f, -.04f, .23f, .24f, .16f, if (character == "kai") dark else coat)
            ball(.48f, 1.0f, -.04f, .23f, .24f, .16f, if (character == "kai") dark else coat)
        } else if (character == "luna") {
            val earMotion = if (acting && reaction in setOf("pet", "jump", "dance")) sin(elapsed * 10f) * 12f * pulse else 0f
            for (side in listOf(-1f, 1f)) {
                val saved = base.copyOf()
                Matrix.translateM(base, 0, side * .28f, .87f, -.02f)
                Matrix.rotateM(base, 0, side * (10f + earMotion), 0f, 0f, 1f)
                ball(0f, .35f, 0f, .17f, .54f, .13f, coat)
                ball(0f, .38f, .105f, .092f, .36f, .035f, 0xf0b4c8)
                System.arraycopy(saved, 0, base, 0, 16)
            }
        } else {
            draw(cone, -.43f, .97f, 0f, .25f, .39f, .22f, coat, 15f)
            draw(cone, .43f, .97f, 0f, .25f, .39f, .22f, coat, -15f)
            draw(cone, -.43f, .98f, .14f, .13f, .24f, .06f, cream, 15f)
            draw(cone, .43f, .98f, .14f, .13f, .24f, .06f, cream, -15f)
        }
        ball(-.23f, .40f, .41f, .26f, .23f, .10f, cream)
        ball(.23f, .40f, .41f, .26f, .23f, .10f, cream)
        val blink = (!reducedMotion && time % 4.7f > 4.52f) || ((acting || reducedMotion) && reaction in setOf("pet", "rest"))
        if (character == "kai" || character == "sol") {
            ball(-.25f, .65f, .41f, .21f, .24f, .075f, if (character == "kai") dark else cream)
            ball(.25f, .65f, .41f, .21f, .24f, .075f, if (character == "kai") dark else cream)
        }
        ball(-.23f, .66f, .481f, .074f, if (blink) .014f else .10f, .04f, dark)
        ball(.23f, .66f, .481f, .074f, if (blink) .014f else .10f, .04f, dark)
        if (!blink) {
            ball(-.25f, .70f, .518f, .024f, .030f, .012f, 0xffffff)
            ball(.21f, .70f, .518f, .024f, .030f, .012f, 0xffffff)
        }
        ball(0f, .43f, .535f, .075f, .055f, .045f, if (character == "luna") 0xc57c99 else dark)
        ball(0f, .30f, .485f, .075f, if (acting && reaction in setOf("talk", "snack")) .025f + abs(sin(elapsed * 15f)) * .033f else .025f, .019f, dark)
        ball(-.40f, .43f, .449f, .085f, .044f, .014f, 0xd99081)
        ball(.40f, .43f, .449f, .085f, .044f, .014f, 0xd99081)
        // Rose heart charm, embedded in the tummy rather than floating in front.
        ball(-.04f, -.12f, .364f, .065f, .075f, .023f, 0xd48cb3, 30f)
        ball(.04f, -.12f, .364f, .065f, .075f, .023f, 0xd48cb3, -30f)
    }

    private fun draw(mesh: Mesh, x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, color: Int, rz: Float = 0f) {
        System.arraycopy(base, 0, model, 0, 16)
        Matrix.translateM(model, 0, x, y, z)
        Matrix.rotateM(model, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(model, 0, sx, sy, sz)
        Matrix.multiplyMM(mv, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, mv, 0)
        Matrix.invertM(inverse, 0, model, 0); Matrix.transposeM(normal, 0, inverse, 0)
        GLES20.glUniformMatrix4fv(uniforms[0], 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uniforms[1], 1, false, model, 0)
        GLES20.glUniformMatrix4fv(uniforms[2], 1, false, normal, 0)
        GLES20.glUniform3f(uniforms[3], ((color shr 16) and 255) / 255f, ((color shr 8) and 255) / 255f, (color and 255) / 255f)
        mesh.draw(program)
    }
}

private class Mesh(vertices: FloatArray) {
    private val count = vertices.size / 6
    private val buffer = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(vertices); position(0) }
    fun draw(program: Int) {
        val position = GLES20.glGetAttribLocation(program, "aPosition")
        val normal = GLES20.glGetAttribLocation(program, "aNormal")
        buffer.position(0); GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 24, buffer)
        buffer.position(3); GLES20.glVertexAttribPointer(normal, 3, GLES20.GL_FLOAT, false, 24, buffer)
        GLES20.glEnableVertexAttribArray(position); GLES20.glEnableVertexAttribArray(normal)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
    }
}

internal object CompanionGeometry {
    fun sphere(rows: Int = 24, columns: Int = 32): FloatArray {
        val result = ArrayList<Float>()
        fun vertex(r: Int, c: Int) {
            val latitude = PI * r / rows
            val longitude = 2 * PI * c / columns
            val x = (sin(latitude) * cos(longitude)).toFloat()
            val y = cos(latitude).toFloat()
            val z = (sin(latitude) * sin(longitude)).toFloat()
            result.addAll(listOf(x, y, z, x, y, z))
        }
        for (r in 0 until rows) for (c in 0 until columns) {
            vertex(r, c); vertex(r + 1, c); vertex(r, c + 1)
            vertex(r, c + 1); vertex(r + 1, c); vertex(r + 1, c + 1)
        }
        return result.toFloatArray()
    }

    /** Closed, smooth lathed body: torso and head share vertices and normals at the neck. */
    fun bunnyBody(rows: Int = 64, columns: Int = 48): FloatArray {
        val profile = listOf(
            floatArrayOf(-.90f, 0f, 0f), floatArrayOf(-.76f, .30f, .27f),
            floatArrayOf(-.40f, .46f, .35f), floatArrayOf(-.12f, .43f, .33f),
            floatArrayOf(.12f, .31f, .29f), floatArrayOf(.27f, .40f, .35f),
            floatArrayOf(.54f, .61f, .46f), floatArrayOf(.82f, .52f, .40f),
            floatArrayOf(1.01f, .28f, .23f), floatArrayOf(1.09f, 0f, 0f))
        fun radius(y: Float, component: Int): Float {
            val index = (0 until profile.lastIndex).firstOrNull { y <= profile[it + 1][0] } ?: (profile.lastIndex - 1)
            val a = profile[index]; val b = profile[index + 1]
            val t = ((y - a[0]) / (b[0] - a[0])).coerceIn(0f, 1f)
            val prev = profile[(index - 1).coerceAtLeast(0)]
            val next = profile[(index + 2).coerceAtMost(profile.lastIndex)]
            val ma = (b[component] - prev[component]) / (b[0] - prev[0]) * (b[0] - a[0])
            val mb = (next[component] - a[component]) / (next[0] - a[0]) * (b[0] - a[0])
            return ((2*t*t*t - 3*t*t + 1)*a[component] + (t*t*t - 2*t*t + t)*ma +
                (-2*t*t*t + 3*t*t)*b[component] + (t*t*t - t*t)*mb).coerceAtLeast(0f)
        }
        val out = ArrayList<Float>(rows * columns * 36)
        fun vertex(row: Int, col: Int) {
            val y = -.90f + 1.99f * row / rows
            val angle = 2 * PI.toFloat() * col / columns
            val c = cos(angle); val sn = sin(angle)
            val rx = radius(y, 1); val rz = radius(y, 2)
            val drx = (radius((y + .001f).coerceAtMost(1.09f), 1) - radius((y - .001f).coerceAtLeast(-.90f), 1)) / .002f
            val drz = (radius((y + .001f).coerceAtMost(1.09f), 2) - radius((y - .001f).coerceAtLeast(-.90f), 2)) / .002f
            val nx = rz * c; val nz = rx * sn
            val ny = -(rz * drx * c * c + rx * drz * sn * sn)
            val length = sqrt(nx*nx + ny*ny + nz*nz)
            out.addAll(listOf(rx*c, y, rz*sn, if (length > .00001f) nx/length else 0f,
                if (length > .00001f) ny/length else if (row == 0) -1f else 1f,
                if (length > .00001f) nz/length else 0f))
        }
        for (r in 0 until rows) for (c in 0 until columns) {
            vertex(r,c); vertex(r+1,c); vertex(r,c+1)
            vertex(r,c+1); vertex(r+1,c); vertex(r+1,c+1)
        }
        return out.toFloatArray()
    }

    fun cone(): FloatArray {
        val result = ArrayList<Float>()
        for (i in 0 until 24) {
            val a = 2 * PI * i / 24; val b = 2 * PI * (i + 1) / 24
            for ((x, y, z) in listOf(Triple(0f, 1f, 0f), Triple(cos(a).toFloat(), 0f, sin(a).toFloat()), Triple(cos(b).toFloat(), 0f, sin(b).toFloat()))) {
                val angle = (a + b) / 2
                result.addAll(listOf(x, y, z, (cos(angle) / sqrt(2.0)).toFloat(), .7071f, (sin(angle) / sqrt(2.0)).toFloat()))
            }
        }
        return result.toFloatArray()
    }
}
