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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
fun InteractiveCompanion(
    characterId: String,
    name: String,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    soundEnabled: Boolean = false,
    onSoundChange: (Boolean) -> Unit = {},
    currentStreak: Int = 0,
    bestStreak: Int = 0,
    goalDays: Int = 0,
    checkInDone: Boolean = false,
    onRecordToday: () -> Unit = {},
    onEditGoal: () -> Unit = {},
    onStartFocusSession: () -> Unit = {},
    dashboardMode: Boolean = false
) {
    var reaction by remember { mutableStateOf("idle") }
    var key by remember { mutableStateOf(0) }
    var caption by remember(characterId, currentStreak, bestStreak, goalDays, checkInDone, dashboardMode) {
        mutableStateOf(
            if (dashboardMode) CompanionBehavior.dashboardNudge(currentStreak, bestStreak, goalDays, checkInDone)
            else CompanionBehavior.dialogue("wave")
        )
    }
    var audioStatus by remember { mutableStateOf("") }
    var lastInteractionAt by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var showMoreActions by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val audio = remember(context) { CompanionAudio(context) { audioStatus = it } }
    DisposableEffect(audio, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                audio.stop()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); audio.close() }
    }
    LaunchedEffect(soundEnabled) { if (!soundEnabled) audio.stop() }

    // The companion stays visibly present, but it never interrupts or speaks by itself.
    // Quiet idle cues rotate between a glance, ear movement, stretch and short caption.
    LaunchedEffect(reduceMotion, characterId) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay((5_500L..9_000L).random())
            val now = SystemClock.uptimeMillis()
            if (now - lastInteractionAt >= 6_500L) {
                val cue = CompanionBehavior.nextIdleCue()
                reaction = cue.action
                key++
                caption = cue.line
                lastInteractionAt = now
            }
        }
    }

    fun act(action: String) {
        lastInteractionAt = SystemClock.uptimeMillis()
        reaction = action
        key++
        caption = CompanionBehavior.dialogue(action)
        if (soundEnabled) {
            if (action != "talk") audio.effect(action)
            audio.say(caption)
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("YOUR COMPANION", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(name.ifBlank { characterId.replaceFirstChar(Char::titlecase) },
                    style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            }
            Text(if (soundEnabled) "VOICE ON" else "VOICE OFF",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Box(
            Modifier.fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .78f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .35f),
                            Color.Transparent)
                    ),
                    RoundedCornerShape(28.dp)
                )
        ) {
            CompanionScene(
                characterId,
                Modifier.fillMaxWidth().height(286.dp),
                true,
                reduceMotion,
                reaction,
                key,
                backgroundColor ?: Color.Transparent,
                onPet = { act("pet") }
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .54f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("✦", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium)
                Text(caption, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }

        if (dashboardMode) {
            Text("QUICK HELP", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onRecordToday,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) { Text(if (checkInDone) "Edit check-in" else "Quick check-in", maxLines = 1) }
                OutlinedButton(
                    onClick = onEditGoal,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) { Text(if (goalDays > 0) "Review goal" else "Set a goal", maxLines = 1) }
            }
    
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .34f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("FOCUS ROOM", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Choose a duration and study with your companion in a dedicated animated space.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = {
                        act("focus")
                        onStartFocusSession()
                    }) { Text("Open") }
                }
            }
    
        }

        Text("PLAY & RESET", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        val everydayActions = listOf(
            "wave" to "Wave", "jump" to "Hop", "dance" to "Dance",
            "snack" to "Feed", "rest" to "Breathe", "cheer" to "Cheer"
        )
        everydayActions.chunked(3).forEach { actions ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                actions.forEach { (id, label) ->
                    FilledTonalButton(
                        onClick = { act(id) },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) { Text(label, maxLines = 1) }
                }
            }
        }
        if (showMoreActions) {
            listOf("spin" to "Spin", "focus" to "Focus", "tip" to "Tip",
                "listen" to "Listen", "think" to "Think", "remind" to "Remind")
                .chunked(3).forEach { actions ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        actions.forEach { (id, label) ->
                            FilledTonalButton(
                                onClick = { act(id) },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) { Text(label, maxLines = 1) }
                        }
                    }
                }
        }
        TextButton(onClick = { showMoreActions = !showMoreActions }) {
            Text(if (showMoreActions) "Show fewer actions" else "More actions")
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = {
                act("talk")
                if (!soundEnabled) audioStatus = "Turn on sound to hear the installed offline voice."
            }) { Text("Say something") }
            TextButton(onClick = { onSoundChange(!soundEnabled); audioStatus = "" }) {
                Text(if (soundEnabled) "Mute voice" else "Enable voice")
            }
        }
        if (audioStatus.isNotBlank()) {
            Text(audioStatus, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Drag to look around · pinch to zoom · tap to pet",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
/** EGL_DEFAULT_DISPLAY is process-wide. One leaving scene must not terminate a sibling's context. */
private object CompanionEglDisplay {
    private var display = EGL14.EGL_NO_DISPLAY

    @Synchronized fun acquire(): android.opengl.EGLDisplay {
        if (display == EGL14.EGL_NO_DISPLAY) {
            val next = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(next != EGL14.EGL_NO_DISPLAY)
            val version = IntArray(2)
            check(EGL14.eglInitialize(next, version, 0, version, 1))
            display = next
        }
        return display
    }

    // Keep the default display initialized for the process. HWUI may also use it.
    // Each scene still destroys its own surface/context and releases its render thread.

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
            var display = EGL14.EGL_NO_DISPLAY
            var context = EGL14.EGL_NO_CONTEXT
            var target = EGL14.EGL_NO_SURFACE
            val surface = Surface(texture)
            try {
                display = CompanionEglDisplay.acquire()
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
                    if (!EGL14.eglSwapBuffers(display, target)) {
                        android.util.Log.w("StrictBlocker", "Companion surface lost: ${EGL14.eglGetError().toString(16)}")
                        break
                    }
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
    private lateinit var bunnyEar: Mesh
    private lateinit var bunnyInnerEar: Mesh
    private lateinit var softLimb: Mesh
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
                vec3 v = normalize(vec3(0.0, 0.4, 5.0) - vPosition);
                vec3 h = normalize(l + v);
                float diffuse = max((dot(n, l) + 0.24) / 1.24, 0.0);
                float rim = pow(1.0 - max(dot(n, v), 0.0), 2.6);
                // Directional micro-strands and broad mottling soften the synthetic finish.
                float fibreA = sin((vPosition.y + sin(vPosition.x * 29.0) * 0.012) * 142.0);
                float fibreB = sin((vPosition.z + sin(vPosition.y * 17.0) * 0.009) * 104.0);
                float mottling = sin(vPosition.x * 21.0 + vPosition.y * 13.0) *
                                 sin(vPosition.z * 19.0 - vPosition.y * 11.0);
                float fur = fibreA * 0.010 + fibreB * 0.007 + mottling * 0.015;
                float subsurface = pow(max(dot(-n, l), 0.0), 2.0) * 0.045;
                float specular = pow(max(dot(n, h), 0.0), 42.0) * 0.018;
                vec3 coat = uColor * (0.57 + 0.43 * diffuse + fur);
                coat += uColor * vec3(1.0, 0.72, 0.72) * subsurface;
                coat += vec3(1.0) * specular + vec3(0.035, 0.025, 0.03) * rim;
                gl_FragColor = vec4(coat, 1.0);
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
        bunnyEar = Mesh(CompanionGeometry.bunnyEar())
        bunnyInnerEar = Mesh(CompanionGeometry.bunnyEar(inner = true))
        softLimb = Mesh(CompanionGeometry.softLimb())
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
        val duration = when (reaction) {
            "rest" -> 8f
            "dance" -> 3.6f
            "snack", "talk" -> 3f
            "stretch", "cheer" -> 2.8f
            "curious", "ear_twitch", "look_around", "listen", "think", "tail_wag" -> 2.5f
            else -> 2f
        }
        val acting = !reducedMotion && elapsed < duration
        val phase = (elapsed / duration).coerceIn(0f, 1f)
        val pulse = sin(phase * PI.toFloat()).coerceAtLeast(0f)
        val bounce = when {
            reducedMotion -> 0f
            acting && reaction in setOf("jump", "happy_hop") -> abs(sin(phase * PI.toFloat() * 2)) * .52f
            acting && reaction == "dance" -> abs(sin(elapsed * 10f)) * .10f * pulse
            acting && reaction == "stretch" -> sin(phase * PI.toFloat()) * .07f
            acting && reaction == "cheer" -> abs(sin(phase * PI.toFloat() * 2f)) * .18f
            acting && reaction == "curious" -> sin(phase * PI.toFloat()) * .035f
            acting && reaction == "wiggle" -> abs(sin(elapsed * 13f)) * .065f * pulse
            else -> sin(time * 1.7f) * .018f
        }
        val spin = if (acting && reaction == "spin") 360f * phase.let { it * it * (3f - 2f * it) } else 0f
        Matrix.setIdentityM(base, 0)
        // A soft stage shadow makes the character feel grounded, including mid-jump.
        draw(sphere, 0f, -.93f, -.06f, .47f - bounce * .18f, .025f, .29f - bounce * .10f, 0xdccbdc)
        Matrix.translateM(base, 0, if (acting && reaction == "dance") sin(elapsed * 5f) * .14f * pulse else 0f, bounce - .06f, 0f)
        Matrix.rotateM(base, 0, pitch + when {
            acting && reaction == "snack" -> sin(elapsed * 12f) * 3f
            acting && reaction == "look_up" -> -12f * pulse
            else -> 0f
        }, 1f, 0f, 0f)
        val curiousTurn = if (acting && reaction in setOf("curious", "look_around")) sin(phase * PI.toFloat()) * 15f else 0f
        val idleLook = if (!reducedMotion && !acting) sin(time * .37f) * 2.8f else 0f
        Matrix.rotateM(base, 0, yaw + spin + curiousTurn + idleLook, 0f, 1f, 0f)
        val idleSway = if (!reducedMotion && !acting) sin(time * .72f) * 1.5f else 0f
        val reactionTilt = if (acting && reaction in setOf("pet", "dance", "curious", "listen", "think", "wiggle")) sin(elapsed * 5.5f) * 8f * pulse else 0f
        Matrix.rotateM(base, 0, idleSway + reactionTilt, 0f, 0f, 1f)
        val breathe = if (acting && reaction == "rest") sin(elapsed * PI.toFloat() / 4f) * .045f else 0f
        Matrix.scaleM(base, 0, 1f + breathe, 1f + breathe * .5f, 1f + breathe)
        val coat = when (character) {
            "milo" -> 0xc4a782; "aria" -> 0xe9c6a4; "kai" -> 0xf1e9e0
            "luna" -> 0xffeee8; "sol" -> 0xf1cb68; else -> 0xdf8d56
        }
        val limbCoat = if (character == "kai") 0x494650 else coat
        val cream = 0xffefda
        val dark = 0x3b2c3e
        fun ball(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, color: Int, rz: Float = 0f) =
            draw(sphere, x, y, z, sx, sy, sz, color, rz)
        if (character == "luna") {
            // Luna's head, tapered neck and seated torso share one continuous mesh.
            draw(bunnyBody, 0f, 0f, 0f, 1f, 1f, 1f, coat)
            // Soft chest fur and rounded haunches follow the body's silhouette.
            ball(0f, -.29f, .285f, .235f, .31f, .055f, 0xfff9f3)
            ball(-.20f, -.68f, .13f, .20f, .22f, .24f, coat)
            ball(.20f, -.68f, .13f, .20f, .22f, .24f, coat)
            // Forefeet with individual toe marks and soft pink pads.
            ball(-.19f, -.86f, .32f, .17f, .105f, .25f, coat)
            ball(.19f, -.86f, .32f, .17f, .105f, .25f, coat)
            ball(-.19f, -.89f, .515f, .105f, .025f, .075f, 0xe9aabd)
            ball(.19f, -.89f, .515f, .105f, .025f, .075f, 0xe9aabd)
            for (toe in -1..1) {
                ball(-.19f + toe * .055f, -.88f, .555f, .018f, .012f, .012f, 0xffdce5)
                ball(.19f + toe * .055f, -.88f, .555f, .018f, .012f, .012f, 0xffdce5)
            }
            val tailWag = when {
                reducedMotion -> 0f
                acting && reaction == "tail_wag" -> sin(elapsed * 15f) * 30f * pulse
                else -> sin(time * 3.1f) * 5f
            }
            ball(0f, -.55f, -.39f, .13f, .14f, .13f, 0xfffaf5, tailWag)
        } else {
            // Distinct proportions keep the fox, bear, cat, panda and owl from sharing
            // one interchangeable stack of spheres.
            val body = when (character) {
                "milo" -> floatArrayOf(.55f, .64f, .41f, .64f, .57f, .47f) // round bear
                "aria" -> floatArrayOf(.43f, .62f, .31f, .58f, .52f, .43f) // sleek cat
                "kai" -> floatArrayOf(.53f, .61f, .40f, .62f, .56f, .47f) // stocky panda
                "sol" -> floatArrayOf(.49f, .67f, .39f, .59f, .62f, .46f) // tall owl
                else -> floatArrayOf(.45f, .65f, .34f, .61f, .55f, .44f) // fox
            }
            ball(0f, -.23f, -.05f, body[0], body[1], body[2], coat)
            ball(0f, .18f, .0f, body[0] * .70f, .29f, body[2] * .83f, coat)
            ball(0f, -.25f, .28f, body[0] * .64f, .39f, .11f, if (character == "kai") cream else cream)
            ball(-.25f, -.77f, .16f, .22f, .19f, .29f, limbCoat)
            ball(.25f, -.77f, .16f, .22f, .19f, .29f, limbCoat)
            ball(-.24f, -.87f, .36f, .16f, .07f, .19f, limbCoat)
            ball(.24f, -.87f, .36f, .16f, .07f, .19f, limbCoat)
            if (character == "kai") {
                // Panda facial patches sit behind the eye layers.
                ball(-.23f, .65f, .405f, .15f, .18f, .045f, limbCoat, -18f)
                ball(.23f, .65f, .405f, .15f, .18f, .045f, limbCoat, 18f)
            }
            if (character != "sol") {
                ball(0f, .57f, .03f, body[3], body[4], body[5], coat)
            } else {
                // Owl head is intentionally larger and sits lower into the chest.
                ball(0f, .61f, .03f, body[3], body[4], body[5], coat)
                draw(cone, 0f, .33f, .49f, .12f, .16f, .10f, 0xe29b53, 180f)
                for (side in listOf(-1f, 1f)) {
                    ball(side * .30f, -.12f, .28f, .13f, .22f, .06f, 0xe2b96e, side * 18f)
                }
            }
            // Each companion has a species-specific tail silhouette and idle swish.
            val tailSwing = if (reducedMotion) 0f else
                sin(time * 3.1f) * 7f + if (acting && reaction in setOf("tail_wag", "wiggle")) sin(elapsed * 14f) * 26f * pulse else 0f
            when (character) {
                "milo" -> ball(0f, -.51f, -.39f, .13f, .13f, .13f, coat, tailSwing)
                "aria" -> ball(-.39f, -.38f, -.39f, .10f, .29f, .11f, coat, -24f + tailSwing)
                "kai" -> ball(0f, -.55f, -.39f, .12f, .12f, .12f, cream, tailSwing)
                "sol" -> ball(0f, -.56f, -.34f, .20f, .18f, .10f, 0xe1b75e, tailSwing)
                else -> {
                    ball(-.25f, -.48f, -.43f, .23f, .25f, .30f, coat, -24f + tailSwing)
                    ball(-.31f, -.63f, -.66f, .14f, .15f, .17f, cream, -24f + tailSwing)
                }
            }
        }
        // Articulated limbs pivot at the shoulder, with overlapping shoulder sockets.
        fun arm(side: Float, angle: Float) {
            val saved = base.copyOf()
            Matrix.translateM(base, 0, side * .40f, .01f, 0f)
            Matrix.rotateM(base, 0, angle, 0f, 0f, 1f)
            // Broader shoulder-to-wrist mesh, with an overlapping shoulder socket,
            // a distinct palm and small toe-like finger pads.
            ball(0f, -.015f, .0f, .135f, .145f, .145f, limbCoat)
            draw(softLimb, 0f, -.18f, 0f, .27f, .36f, .245f, limbCoat)
            ball(0f, -.39f, .045f, .145f, .135f, .14f, limbCoat)
            ball(0f, -.405f, .158f, .078f, .040f, .020f, 0xe9aabd)
            for (finger in -1..1) {
                ball(finger * .045f, -.445f, .15f, .018f, .026f, .014f, 0xffdce5)
            }
            System.arraycopy(saved, 0, base, 0, 16)
        }
        val wave = when {
            reducedMotion && reaction == "wave" -> 125f
            acting && reaction in setOf("wave", "paw_wave") -> sin(elapsed * 15f) * 22f * pulse + 125f * pulse
            acting && reaction == "dance" -> (sin(elapsed * 10f) * 35f + 50f) * pulse
            acting && reaction == "snack" -> 65f * pulse
            acting && reaction == "stretch" -> -92f * pulse
            acting && reaction == "curious" -> -48f * pulse
            !reducedMotion && !acting -> sin(time * 1.65f) * 3f
            else -> 12f
        }
        val leftArm = when {
            acting && reaction == "snack" -> -65f * pulse
            acting && reaction == "stretch" -> 92f * pulse
            else -> -12f
        }
        val idleArm = if (!reducedMotion && !acting) sin(time * 1.35f) * 5f else 0f
        arm(-1f, leftArm + idleArm)
        arm(1f, wave - idleArm)
        if (acting && reaction == "snack") {
            draw(cone, .05f, .20f, .48f, .11f, .30f, .10f, 0xf09a68, 150f)
            ball(.10f, .40f, .47f, .13f, .08f, .08f, 0xb9a1dd)
        }
        // Species: fox, bear, cat, panda, rabbit and owl. Each has actual 3D ears/features.
        if (character == "milo" || character == "kai") {
            val earWiggle = when {
                acting && reaction == "ear_twitch" -> sin(elapsed * 18f) * 18f * pulse
                acting && reaction in setOf("happy_hop", "jump", "dance") -> sin(elapsed * 11f) * 10f * pulse
                !reducedMotion && !acting -> sin(time * 1.8f) * 2.8f
                else -> 0f
            }
            ball(-.48f, 1.0f, -.04f, .23f, .24f, .16f, if (character == "kai") limbCoat else coat, -earWiggle)
            ball(.48f, 1.0f, -.04f, .23f, .24f, .16f, if (character == "kai") limbCoat else coat, earWiggle)
        } else if (character == "luna") {
            val earMotion = when {
                acting && reaction in setOf("pet", "jump", "dance", "cheer") -> sin(elapsed * 10f) * 15f * pulse
                acting && reaction == "ear_twitch" -> sin(elapsed * 18f) * 24f * pulse
                acting && reaction == "listen" -> sin(elapsed * 7f) * 5f * pulse
                !reducedMotion && !acting && time % 10.5f > 9.2f -> sin((time % 10.5f - 9.2f) * PI.toFloat() / 1.3f) * 7f
                else -> 0f
            }
            for (side in listOf(-1f, 1f)) {
                val saved = base.copyOf()
                Matrix.translateM(base, 0, side * .19f, 1.02f, -.015f)
                Matrix.rotateM(base, 0, side * (7f + earMotion), 0f, 0f, 1f)
                draw(bunnyEar, 0f, .34f, 0f, 1f, .52f, 1f, coat)
                draw(bunnyInnerEar, 0f, .35f, 0f, 1f, .49f, 1f, 0xf0b4c8)
                System.arraycopy(saved, 0, base, 0, 16)
            }
        } else {
            val earWiggle = when {
                acting && reaction == "ear_twitch" -> sin(elapsed * 18f) * 18f * pulse
                acting && reaction in setOf("happy_hop", "jump", "dance") -> sin(elapsed * 11f) * 10f * pulse
                !reducedMotion && !acting -> sin(time * 1.8f) * 2.8f
                else -> 0f
            }
            val earScale = if (character == "sol") .62f else if (character == "aria") .90f else 1f
            draw(cone, -.43f, .97f, 0f, .25f * earScale, .39f * earScale, .22f, coat, 15f - earWiggle)
            draw(cone, .43f, .97f, 0f, .25f * earScale, .39f * earScale, .22f, coat, -15f + earWiggle)
            draw(cone, -.43f, .98f, .14f, .13f * earScale, .24f * earScale, .06f, cream, 15f - earWiggle)
            draw(cone, .43f, .98f, .14f, .13f * earScale, .24f * earScale, .06f, cream, -15f + earWiggle)
        }
        val blinkCycle = time % 6.4f
        val blink = (!reducedMotion && (blinkCycle > 6.18f || (time % 17.2f in 11.1f..11.28f))) ||
            ((acting || reducedMotion) && reaction in setOf("pet", "rest", "curious", "look_around", "listen"))
        if (character == "luna") {
            // Layered eyes with sclera, iris, pupil and natural catchlights.
            ball(-.17f, .635f, .354f, .092f, if (blink) .012f else .116f, .054f, 0xfffaf5)
            ball(.17f, .635f, .354f, .092f, if (blink) .012f else .116f, .054f, 0xfffaf5)
            ball(-.17f, .635f, .397f, .060f, if (blink) .009f else .082f, .035f, 0x795440)
            ball(.17f, .635f, .397f, .060f, if (blink) .009f else .082f, .035f, 0x795440)
            ball(-.17f, .632f, .424f, .033f, if (blink) .006f else .060f, .020f, dark)
            ball(.17f, .632f, .424f, .033f, if (blink) .006f else .060f, .020f, dark)
            if (!blink) {
                ball(-.187f, .675f, .443f, .019f, .024f, .010f, 0xffffff)
                ball(.153f, .675f, .443f, .019f, .024f, .010f, 0xffffff)
                ball(-.145f, .603f, .444f, .008f, .010f, .006f, 0xffd6df)
                ball(.195f, .603f, .444f, .008f, .010f, .006f, 0xffd6df)
            }
            // Split muzzle, small nose, smile, whiskers and blush.
            ball(-.058f, .405f, .415f, .105f, .078f, .072f, 0xfffaf4)
            ball(.058f, .405f, .415f, .105f, .078f, .072f, 0xfffaf4)
            ball(0f, .447f, .475f, .042f, .030f, .026f, 0xd48ba7)
            ball(0f, .355f, .474f, .012f, .037f, .009f, 0x70515c)
            ball(-.025f, .326f, .468f, .034f, .010f, .008f, 0x70515c, -12f)
            ball(.025f, .326f, .468f, .034f, .010f, .008f, 0x70515c, 12f)
            ball(-.315f, .407f, .410f, .155f, .008f, .006f, 0xc5b2b0, 13f)
            ball(-.325f, .385f, .405f, .160f, .007f, .006f, 0xc5b2b0, -3f)
            ball(-.305f, .365f, .400f, .145f, .007f, .006f, 0xc5b2b0, -18f)
            ball(.315f, .407f, .410f, .155f, .008f, .006f, 0xc5b2b0, -13f)
            ball(.325f, .385f, .405f, .160f, .007f, .006f, 0xc5b2b0, 3f)
            ball(.305f, .365f, .400f, .145f, .007f, .006f, 0xc5b2b0, 18f)
            ball(-.31f, .425f, .401f, .039f, .019f, .010f, 0xf0c2ce)
            ball(.31f, .425f, .401f, .039f, .019f, .010f, 0xf0c2ce)
        } else {
            ball(-.23f, .40f, .41f, .26f, .23f, .10f, cream)
            ball(.23f, .40f, .41f, .26f, .23f, .10f, cream)
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
            ball(0f, .43f, .535f, .075f, .055f, .045f, dark)
            ball(0f, .30f, .485f, .075f, if (acting && reaction in setOf("talk", "snack")) .025f + abs(sin(elapsed * 15f)) * .033f else .025f, .019f, dark)
            ball(-.40f, .43f, .449f, .085f, .044f, .014f, 0xd99081)
            ball(.40f, .43f, .449f, .085f, .044f, .014f, 0xd99081)
        }
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
        // A seated rabbit profile: haunches, shoulders, tapered neck and head.
        // This is a single lofted surface, not a stack of separate body spheres.
        val profile = listOf(
            floatArrayOf(-.92f, 0f, 0f), floatArrayOf(-.78f, .25f, .25f),
            floatArrayOf(-.58f, .36f, .32f), floatArrayOf(-.34f, .41f, .35f),
            floatArrayOf(-.10f, .35f, .30f), floatArrayOf(.08f, .28f, .25f),
            floatArrayOf(.24f, .29f, .26f), floatArrayOf(.40f, .33f, .30f),
            floatArrayOf(.60f, .46f, .39f), floatArrayOf(.78f, .43f, .37f),
            floatArrayOf(.94f, .25f, .22f), floatArrayOf(1.02f, 0f, 0f))
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
            val y = -.92f + 1.94f * row / rows
            val angle = 2 * PI.toFloat() * col / columns
            val c = cos(angle); val sn = sin(angle)
            val rx = radius(y, 1); val rz = radius(y, 2)
            val drx = (radius((y + .001f).coerceAtMost(1.02f), 1) - radius((y - .001f).coerceAtLeast(-.92f), 1)) / .002f
            val drz = (radius((y + .001f).coerceAtMost(1.02f), 2) - radius((y - .001f).coerceAtLeast(-.92f), 2)) / .002f
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

    /** Tapered, softly curved capsule mesh used for continuous shoulder-to-wrist limbs. */
    fun softLimb(rows: Int = 32, columns: Int = 24): FloatArray {
        val out = ArrayList<Float>(rows * columns * 36)
        fun radius(t: Float): Float {
            val profile = listOf(
                0f to .015f, .08f to .055f, .25f to .105f,
                .52f to .135f, .76f to .125f, .93f to .075f, 1f to .012f
            )
            val index = (0 until profile.lastIndex).firstOrNull { t <= profile[it + 1].first } ?: profile.lastIndex - 1
            val a = profile[index]; val b = profile[index + 1]
            val f = ((t - a.first) / (b.first - a.first)).coerceIn(0f, 1f)
            val smooth = f * f * (3f - 2f * f)
            return a.second + (b.second - a.second) * smooth
        }
        fun vertex(row: Int, col: Int) {
            val t = row.toFloat() / rows
            val y = 1f - 2f * t
            val r = radius(t)
            val angle = 2f * PI.toFloat() * col / columns
            val ca = cos(angle); val sa = sin(angle)
            val curve = sin(t * PI.toFloat()) * .045f
            val x = curve + r * ca
            val z = r * .88f * sa
            val dx = .045f * PI.toFloat() * cos(t * PI.toFloat())
            val nx = ca
            val ny = dx * ca
            val nz = sa / .88f
            val length = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(.0001f)
            out.addAll(listOf(x, y, z, nx / length, ny / length, nz / length))
        }
        for (row in 0 until rows) for (col in 0 until columns) {
            vertex(row, col); vertex(row + 1, col); vertex(row, col + 1)
            vertex(row, col + 1); vertex(row + 1, col); vertex(row + 1, col + 1)
        }
        return out.toFloatArray()
    }

    /** Rounded tapered rabbit ear loft. Inner ear is a thinner inset shell. */
    fun bunnyEar(inner: Boolean = false, rows: Int = 36, columns: Int = 24): FloatArray {
        val out = ArrayList<Float>(rows * columns * 36)
        fun vertex(row: Int, col: Int) {
            val along = row.toFloat() / rows
            val y = -1f + 2f * along
            val envelope = sin(PI.toFloat() * along).coerceAtLeast(0f).pow(.72f)
            val widthScale = if (inner) .62f else 1f
            val depthScale = if (inner) .24f else 1f
            val width = (.31f * envelope * (1f - .16f * along) + .012f) * widthScale
            val depth = (.095f * envelope + .008f) * depthScale
            val angle = 2f * PI.toFloat() * col / columns
            val ca = cos(angle); val sa = sin(angle)
            val x = width * ca + sin(along * PI.toFloat()) * .025f
            val z = depth * sa + if (inner) .105f else 0f
            val nx = ca * .48f
            val ny = if (along < .5f) .12f else -.10f
            val nz = sa * .86f + if (inner) .10f else 0f
            val length = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(.0001f)
            out.addAll(listOf(x, y, z, nx / length, ny / length, nz / length))
        }
        for (row in 0 until rows) for (col in 0 until columns) {
            vertex(row, col); vertex(row + 1, col); vertex(row, col + 1)
            vertex(row, col + 1); vertex(row + 1, col); vertex(row + 1, col + 1)
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
