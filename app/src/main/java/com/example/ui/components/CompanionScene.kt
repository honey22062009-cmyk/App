package com.example.ui.components

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/** Real, lit, depth-tested 3D geometry. No network, WebView or downloaded model. */
@Composable
fun CompanionScene(
    characterId: String,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    reduceMotion: Boolean = false,
    reaction: String = "idle",
    reactionKey: Int = 0
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val background = MaterialTheme.colorScheme.surface.toArgb()
    val view = remember(context) { CompanionSurface(context) }
    DisposableEffect(view, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            view.onPause()
        }
    }
    AndroidView(
        factory = { view },
        modifier = modifier.semantics { contentDescription = "3D companion. Drag to rotate, pinch to zoom, tap to pet." },
        update = {
            it.interactive = interactive
            it.scene.character = characterId
            it.scene.background = background
            it.scene.reducedMotion = reduceMotion
            it.renderMode = if (interactive && !reduceMotion) GLSurfaceView.RENDERMODE_CONTINUOUSLY
                else GLSurfaceView.RENDERMODE_WHEN_DIRTY
            it.react(reaction, reactionKey)
            it.requestRender()
        }
    )
}

@Composable
fun InteractiveCompanion(characterId: String, name: String, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    var reaction by remember { mutableStateOf("idle") }
    var key by remember { mutableStateOf(0) }
    Column(modifier) {
        CompanionScene(characterId, Modifier.fillMaxWidth().height(230.dp), true, reduceMotion, reaction, key)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("wave" to "Wave", "jump" to "Jump", "spin" to "Spin").forEach { (id, label) ->
                TextButton(onClick = { reaction = id; key++ }, modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)) { Text(label, maxLines = 1) }
            }
        }
        Text("${name.ifBlank { characterId.replaceFirstChar(Char::titlecase) }} · drag to turn, tap to pet",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private class CompanionSurface(context: Context) : GLSurfaceView(context) {
    val scene = CompanionRenderer()
    var interactive = false
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
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(scene)
        renderMode = RENDERMODE_WHEN_DIRTY
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
        scene.reaction = "pet"
        scene.reactionAt = SystemClock.uptimeMillis()
        requestRender()
        return true
    }
}

private class CompanionRenderer : GLSurfaceView.Renderer {
    @Volatile var character = "nova"
    @Volatile var background = 0xfffafaf8.toInt()
    @Volatile var reducedMotion = false
    @Volatile var yaw = -12f
    @Volatile var pitch = 4f
    @Volatile var zoom = 4.8f
    @Volatile var reaction = "idle"
    @Volatile var reactionAt = 0L
    private var program = 0
    private lateinit var sphere: Mesh
    private lateinit var cone: Mesh
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val base = FloatArray(16)
    private val model = FloatArray(16)
    private val mv = FloatArray(16)
    private val mvp = FloatArray(16)
    private val inverse = FloatArray(16)
    private val normal = FloatArray(16)
    private var aspect = 1f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
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
                float specular = pow(max(dot(n, h), 0.0), 40.0) * 0.20;
                gl_FragColor = vec4(uColor * (0.44 + 0.56 * diffuse) + specular + rim * 0.055, 1.0);
            }
        """.trimIndent())
        GLES20.glAttachShader(program, vertex); GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vertex); GLES20.glDeleteShader(fragment)
        sphere = Mesh(CompanionGeometry.sphere())
        cone = Mesh(CompanionGeometry.cone())
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspect = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(projection, 0, 37f, aspect, .1f, 40f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val bg = background
        GLES20.glClearColor(((bg shr 16) and 255) / 255f, ((bg shr 8) and 255) / 255f, (bg and 255) / 255f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        Matrix.setLookAtM(view, 0, 0f, .4f, zoom, 0f, .20f, 0f, 0f, 1f, 0f)
        val time = SystemClock.uptimeMillis() / 1000f
        val elapsed = ((SystemClock.uptimeMillis() - reactionAt) / 1000f).coerceAtLeast(0f)
        val acting = !reducedMotion && elapsed < 1.8f
        val bounce = if (reducedMotion && reaction == "jump") .20f
            else if (acting && reaction == "jump") abs(sin(elapsed * PI.toFloat() * 2f)) * .46f
            else if (reducedMotion) 0f else sin(time * 2f) * .025f
        val spin = if (acting && reaction == "spin") 360f * (elapsed / 1.8f).let { it * it * (3f - 2f * it) } else 0f
        Matrix.setIdentityM(base, 0)
        Matrix.translateM(base, 0, 0f, bounce - .06f, 0f)
        Matrix.rotateM(base, 0, pitch, 1f, 0f, 0f)
        Matrix.rotateM(base, 0, yaw + spin, 0f, 1f, 0f)
        val coat = when (character) {
            "milo" -> 0xc4a782; "aria" -> 0xe9c6a4; "kai" -> 0x54666a
            "luna" -> 0xbbd9cf; "sol" -> 0xf1cb68; else -> 0xdf8d56
        }
        val cream = 0xffefda
        val dark = 0x26342f
        fun ball(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, color: Int, rz: Float = 0f) =
            draw(sphere, x, y, z, sx, sy, sz, color, rz)
        // Rounded body, tummy, feet and a visible tail give the figure a full silhouette.
        ball(0f, -.23f, -.05f, .48f, .61f, .36f, coat)
        ball(0f, -.25f, .28f, .31f, .39f, .11f, cream)
        ball(-.27f, -.78f, .16f, .23f, .16f, .31f, dark)
        ball(.27f, -.78f, .16f, .23f, .16f, .31f, dark)
        ball(.48f, -.48f, -.32f, .22f, .43f, .20f, coat, -40f)
        ball(.64f, -.20f, -.34f, .15f, .19f, .17f, cream)
        val wave = if (reducedMotion && reaction == "wave") 125f
            else if (acting && reaction == "wave") sin(elapsed * 17f) * 25f + 125f else 20f
        ball(-.53f, -.24f, .02f, .17f, .34f, .20f, coat, -20f)
        ball(.53f, if (wave > 50f) .03f else -.24f, .02f, .17f, .34f, .20f, coat, wave)
        ball(0f, .57f, .03f, .63f, .56f, .47f, coat)
        // Species: fox, bear, cat, panda, rabbit and owl. Each has actual 3D ears/features.
        if (character == "milo" || character == "kai") {
            ball(-.48f, 1.0f, -.04f, .23f, .24f, .16f, if (character == "kai") dark else coat)
            ball(.48f, 1.0f, -.04f, .23f, .24f, .16f, if (character == "kai") dark else coat)
        } else if (character == "luna") {
            ball(-.29f, 1.15f, -.04f, .15f, .48f, .12f, coat, -12f)
            ball(.29f, 1.15f, -.04f, .15f, .48f, .12f, coat, 12f)
            ball(-.29f, 1.17f, .06f, .075f, .32f, .04f, 0xe5a8a3, -12f)
            ball(.29f, 1.17f, .06f, .075f, .32f, .04f, 0xe5a8a3, 12f)
        } else {
            draw(cone, -.43f, .97f, 0f, .25f, .39f, .22f, coat, 15f)
            draw(cone, .43f, .97f, 0f, .25f, .39f, .22f, coat, -15f)
            draw(cone, -.43f, .98f, .14f, .13f, .24f, .06f, cream, 15f)
            draw(cone, .43f, .98f, .14f, .13f, .24f, .06f, cream, -15f)
        }
        ball(-.23f, .40f, .41f, .26f, .23f, .10f, cream)
        ball(.23f, .40f, .41f, .26f, .23f, .10f, cream)
        val blink = (!reducedMotion && time % 4.7f > 4.52f) || ((acting || reducedMotion) && reaction == "pet")
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
        ball(0f, .43f, .535f, .09f, .065f, .055f, dark)
        ball(0f, .30f, .485f, .085f, .024f, .019f, dark)
        ball(-.40f, .43f, .449f, .085f, .044f, .014f, 0xd99081)
        ball(.40f, .43f, .449f, .085f, .044f, .014f, 0xd99081)
        // Small green shield medallion ties the companions to the app identity.
        ball(0f, -.13f, .404f, .12f, .14f, .04f, 0x277c67)
    }

    private fun draw(mesh: Mesh, x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, color: Int, rz: Float = 0f) {
        System.arraycopy(base, 0, model, 0, 16)
        Matrix.translateM(model, 0, x, y, z)
        Matrix.rotateM(model, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(model, 0, sx, sy, sz)
        Matrix.multiplyMM(mv, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, mv, 0)
        Matrix.invertM(inverse, 0, model, 0); Matrix.transposeM(normal, 0, inverse, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uMvp"), 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uModel"), 1, false, model, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uNormal"), 1, false, normal, 0)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uColor"), ((color shr 16) and 255) / 255f, ((color shr 8) and 255) / 255f, (color and 255) / 255f)
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
    fun sphere(rows: Int = 18, columns: Int = 24): FloatArray {
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
