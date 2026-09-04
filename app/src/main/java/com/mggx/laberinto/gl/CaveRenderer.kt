package com.mggx.laberinto.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.maze.CaveTheme
import com.mggx.laberinto.maze.MazeGenerator
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Renderer de la cueva. Todo el dibujo pasa por aca:
 *  1) mundo (malla unica del laberinto, con texturas procedurales)
 *  2) objetos instanciados (ecos, cristales, antorchas, estalagmitas, salida)
 *  3) calcomanias de suelo (rastro de pasos, hilo de Ariadna, tiza)
 *  4) brazos en primera persona, con su propia profundidad
 *  5) vineta y tinte de pantalla
 */
class CaveRenderer(
    private val save: SaveData,
    private val input: InputState
) : GLSurfaceView.Renderer {

    /** Estado de entrada compartido con la vista (se escribe desde el hilo de UI). */
    class InputState {
        @Volatile var moveX = 0f
        @Volatile var moveY = 0f
        @Volatile var lookDX = 0f
        @Volatile var lookDY = 0f
        @Volatile var paused = false

        // Correr puede venir de tres lados a la vez; con banderas separadas
        // ninguno le pisa el estado al otro.
        @Volatile var runStick = false
        @Volatile var runButton = false
        @Volatile var runPad = false
        @Volatile var autoRun = false

        /** Stick derecho del mando: se aplica de forma continua por frame. */
        @Volatile var padLookX = 0f
        @Volatile var padLookY = 0f
        @Volatile var padSensitivity = 1f

        val running: Boolean get() = runStick || runButton || runPad || autoRun

        fun releaseAll() {
            moveX = 0f; moveY = 0f; lookDX = 0f; lookDY = 0f
            runStick = false; runButton = false; runPad = false
        }

        fun consumeLook(): Pair<Float, Float> {
            val x = lookDX; val y = lookDY
            lookDX = 0f; lookDY = 0f
            return x to y
        }
    }

    // ------------------------------------------------------------- estado
    @Volatile var session: GameSession? = null
        private set
    private val pending = ConcurrentLinkedQueue<GameSession>()
    @Volatile var ready = false
        private set
    @Volatile var fps = 0f
        private set
    /** Se dispara cuando la partida termina, para que la UI muestre el resultado. */
    @Volatile var onPhaseChanged: ((GameSession.Phase) -> Unit)? = null
    /** Sonidos que la partida genero y todavia no consumio el motor de audio. */
    val soundOut = ConcurrentLinkedQueue<GameSession.Sfx>()

    private var lastPhase = GameSession.Phase.JUGANDO

    // ---------------------------------------------------------- programas
    private var worldProg = 0
    private var propProg = 0
    private var armsProg = 0
    private var decalProg = 0
    private var overlayProg = 0

    // ------------------------------------------------------------ mundo
    private val worldVao = IntArray(1)
    private val worldVbo = IntArray(1)
    private val worldEbo = IntArray(1)
    private var worldIndexCount = 0
    private var albedoTex = 0
    private var normalTex = 0
    private var texturedTheme: CaveTheme? = null

    // ------------------------------------------------------------ props
    private var shapeGem: InstancedShape? = null
    private var shapeCone: InstancedShape? = null
    private var shapeConeDown: InstancedShape? = null
    private var shapeBox: InstancedShape? = null
    private var shapeCylinder: InstancedShape? = null
    private var shapeArrow: InstancedShape? = null

    // ------------------------------------------------------------ brazos
    private val armsVao = IntArray(1)
    private val armsVbo = IntArray(1)
    private val armsEbo = IntArray(1)
    private var armsIndexCount = 0

    // --------------------------------------------------------- decals
    private val decalVao = IntArray(1)
    private val decalVbo = IntArray(1)
    private var decalCapacity = 0
    private val decalData = FloatList(4096)

    // -------------------------------------------------------- overlay
    private val overlayVao = IntArray(1)
    private val overlayVbo = IntArray(1)

    // ------------------------------------------------------------ camara
    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val viewProj = FloatArray(16)
    private val armProj = FloatArray(16)
    private val armMatL = FloatArray(16)
    private val armMatR = FloatArray(16)
    private val tmp = FloatArray(16)

    private var width = 1
    private var height = 1
    private var lastFrameNs = 0L
    private var time = 0f
    private var flicker = 1f
    private var flickerSeed = 0f
    private var hurtFlash = 0f
    private var lastHealth = -1f
    private var frameAccum = 0f
    private var frameCount = 0
    private var lastPresentNs = 0L

    // ------------------------------------------------------------- API

    fun setSession(s: GameSession) { pending.add(s) }

    // ------------------------------------------------------ ciclo de vida

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.02f, 0.018f, 0.016f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glFrontFace(GLES30.GL_CCW)

        worldProg = GLUtil.program(Shaders.WORLD_VS, Shaders.WORLD_FS)
        propProg = GLUtil.program(Shaders.PROP_VS, Shaders.PROP_FS)
        armsProg = GLUtil.program(Shaders.ARMS_VS, Shaders.ARMS_FS)
        decalProg = GLUtil.program(Shaders.DECAL_VS, Shaders.DECAL_FS)
        overlayProg = GLUtil.program(Shaders.OVERLAY_VS, Shaders.OVERLAY_FS)

        GLES30.glGenVertexArrays(1, worldVao, 0)
        GLES30.glGenBuffers(1, worldVbo, 0)
        GLES30.glGenBuffers(1, worldEbo, 0)

        buildArms()
        buildDecalBuffer(2048)
        buildOverlay()
        buildShapes()

        texturedTheme = null
        worldIndexCount = 0
        lastFrameNs = 0L
        ready = false
    }

    override fun onSurfaceChanged(unused: GL10?, w: Int, h: Int) {
        width = max(1, w)
        height = max(1, h)
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(unused: GL10?) {
        // --- cambio de nivel pendiente
        var swapped = false
        while (true) {
            val s = pending.poll() ?: break
            session = s
            swapped = true
        }
        val s = session
        if (s == null) {
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
            return
        }
        if (swapped) {
            prepareLevel(s)
            lastFrameNs = 0L
            lastPhase = GameSession.Phase.JUGANDO
            lastHealth = s.health
            ready = true
        }
        if (s.geometryDirty) {
            s.geometryDirty = false
            uploadWorld(WorldMesh.build(s.maze))
        }

        // ------------------------------------------------------- tiempo
        val now = System.nanoTime()
        var dt = if (lastFrameNs == 0L) 0f else (now - lastFrameNs) / 1_000_000_000f
        lastFrameNs = now
        dt = dt.coerceIn(0f, 0.1f)
        time += dt
        frameAccum += dt
        frameCount++
        if (frameAccum >= 0.5f) {
            fps = frameCount / frameAccum
            frameAccum = 0f; frameCount = 0
        }

        // ------------------------------------------------------- logica
        if (!input.paused && s.phase == GameSession.Phase.JUGANDO) {
            var (lx, ly) = input.consumeLook()
            // El stick del mando gira de forma continua mientras se mantiene.
            val padSpeed = GameSession.PAD_LOOK_DEG * input.padSensitivity * dt
            lx += input.padLookX * padSpeed
            ly += input.padLookY * padSpeed
            val gi = GameSession.Input(
                moveX = input.moveX, moveY = input.moveY,
                lookX = lx, lookY = ly, running = input.running
            )
            s.update(dt, gi)
        } else {
            input.consumeLook()
        }
        s.drainSounds().forEach { soundOut.add(it) }

        if (s.phase != lastPhase) {
            lastPhase = s.phase
            onPhaseChanged?.invoke(s.phase)
        }
        if (lastHealth >= 0f && s.health < lastHealth - 0.5f) hurtFlash = 1f
        lastHealth = s.health
        if (hurtFlash > 0f) hurtFlash = max(0f, hurtFlash - dt * 1.6f)

        // ------------------------------------------------------- camara
        val fov = s.fovDegrees()
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(proj, 0, fov, aspect, 0.045f, 120f)

        val eyeY = GameSession.EYE_HEIGHT + s.headBobOffset()
        val yaw = Math.toRadians(s.yawDeg.toDouble())
        val pitch = Math.toRadians(s.pitchDeg.toDouble())
        val cp = cos(pitch).toFloat()
        val fx = (sin(yaw) * cp).toFloat()
        val fy = sin(pitch).toFloat()
        val fz = (cos(yaw) * cp).toFloat()

        Matrix.setLookAtM(
            view, 0,
            s.posX, eyeY, s.posZ,
            s.posX + fx, eyeY + fy, s.posZ + fz,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0)

        // ------------------------------------------------------- antorcha
        if (save.settings.torchFlicker) {
            flickerSeed += dt * 9.5f
            flicker = 0.88f + 0.12f * (sin(flickerSeed) * 0.6f + sin(flickerSeed * 2.37f + 1.3f) * 0.4f)
        } else flicker = 1f

        val lightRadius = s.lightRadiusNow()
        val tint = s.stats.lightTint
        val lr = ((tint shr 16) and 0xFF) / 255f
        val lg = ((tint shr 8) and 0xFF) / 255f
        val lb = (tint and 0xFF) / 255f
        val amb = s.ambientBoost()
        val theme = s.theme
        val fogDensity = theme.fogDensity * save.settings.fogIntensity
        val brightness = save.settings.brightness

        GLES30.glClearColor(theme.fogR * 0.55f, theme.fogG * 0.55f, theme.fogB * 0.55f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        drawWorld(s, lr, lg, lb, lightRadius, amb, fogDensity, brightness)
        drawProps(s, lr, lg, lb, lightRadius, amb, fogDensity, brightness)
        drawDecals(s, fogDensity, brightness)
        if (save.settings.showArms) drawArms(s, lr, lg, lb, amb, brightness, aspect, dt)
        drawOverlay(s, theme, brightness)

        limitFrameRate()
    }

    /**
     * Techo de cuadros por segundo. Dormir un toque baja el consumo de bateria
     * y el calor del telefono cuando el aparato podria ir mas rapido de lo pedido.
     */
    private fun limitFrameRate() {
        val target = save.settings.targetFps
        if (target <= 0) return
        val frameNs = 1_000_000_000L / target
        val now = System.nanoTime()
        if (lastPresentNs != 0L) {
            val sleep = frameNs - (now - lastPresentNs)
            if (sleep > 1_000_000L) {
                try { Thread.sleep(sleep / 1_000_000L, (sleep % 1_000_000L).toInt()) }
                catch (_: InterruptedException) { Thread.currentThread().interrupt() }
            }
        }
        lastPresentNs = System.nanoTime()
    }

    // ------------------------------------------------------------ montaje

    private fun prepareLevel(s: GameSession) {
        if (texturedTheme != s.theme || albedoTex == 0) {
            if (albedoTex != 0) GLES30.glDeleteTextures(2, intArrayOf(albedoTex, normalTex), 0)
            val r = ProcTextures.build(s.theme, save.settings.quality)
            albedoTex = r.albedoTex
            normalTex = r.normalTex
            texturedTheme = s.theme
        }
        uploadWorld(WorldMesh.build(s.maze))
    }

    private fun uploadWorld(mesh: WorldMesh.Mesh) {
        GLES30.glBindVertexArray(worldVao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, worldVbo[0])
        val vb = GLUtil.floatBuffer(mesh.vertices)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mesh.vertices.size * 4, vb, GLES30.GL_STATIC_DRAW)

        val st = WorldMesh.STRIDE_BYTES
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, st, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, st, 12)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, st, 24)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, st, 32)
        GLES30.glEnableVertexAttribArray(4)
        GLES30.glVertexAttribPointer(4, 1, GLES30.GL_FLOAT, false, st, 36)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, worldEbo[0])
        val ib = GLUtil.intBuffer(mesh.indices)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, mesh.indices.size * 4, ib, GLES30.GL_STATIC_DRAW)
        worldIndexCount = mesh.indices.size
        GLES30.glBindVertexArray(0)
        GLUtil.checkError("uploadWorld")
    }

    private fun buildShapes() {
        shapeGem?.release(); shapeCone?.release(); shapeConeDown?.release()
        shapeBox?.release(); shapeCylinder?.release(); shapeArrow?.release()
        shapeGem = InstancedShape(PropMeshes.octahedron(1.5f), 420)
        shapeCone = InstancedShape(PropMeshes.cone(7, 1f, 0.42f, false), 340)
        shapeConeDown = InstancedShape(PropMeshes.cone(7, 1f, 0.36f, true), 340)
        shapeBox = InstancedShape(PropMeshes.box(1f, 1f, 1f), 220)
        shapeCylinder = InstancedShape(PropMeshes.cylinder(7, 1f, 0.1f), 220)
        shapeArrow = InstancedShape(PropMeshes.arrow(), 4)
    }

    private fun buildArms() {
        val mesh = ArmsMesh.build()
        GLES30.glGenVertexArrays(1, armsVao, 0)
        GLES30.glGenBuffers(1, armsVbo, 0)
        GLES30.glGenBuffers(1, armsEbo, 0)
        GLES30.glBindVertexArray(armsVao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, armsVbo[0])
        val vb = GLUtil.floatBuffer(mesh.vertices)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mesh.vertices.size * 4, vb, GLES30.GL_STATIC_DRAW)
        val st = ArmsMesh.STRIDE_BYTES
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, st, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, st, 12)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, st, 24)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, armsEbo[0])
        val ib = GLUtil.intBuffer(mesh.indices)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, mesh.indices.size * 4, ib, GLES30.GL_STATIC_DRAW)
        armsIndexCount = mesh.indices.size
        GLES30.glBindVertexArray(0)
    }

    private fun buildDecalBuffer(quads: Int) {
        decalCapacity = quads
        GLES30.glGenVertexArrays(1, decalVao, 0)
        GLES30.glGenBuffers(1, decalVbo, 0)
        GLES30.glBindVertexArray(decalVao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, decalVbo[0])
        // pos(3) uv(2) color(4) = 9 floats, 6 vertices por quad
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quads * 6 * 9 * 4, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 36, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 36, 12)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, 36, 20)
        GLES30.glBindVertexArray(0)
    }

    private fun buildOverlay() {
        GLES30.glGenVertexArrays(1, overlayVao, 0)
        GLES30.glGenBuffers(1, overlayVbo, 0)
        GLES30.glBindVertexArray(overlayVao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, overlayVbo[0])
        val quad = floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f)   // triangulo que cubre la pantalla
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quad.size * 4, GLUtil.floatBuffer(quad), GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0)
        GLES30.glBindVertexArray(0)
    }

    // ------------------------------------------------------------ dibujo

    private fun drawWorld(
        s: GameSession, lr: Float, lg: Float, lb: Float,
        radius: Float, ambBoost: Float, fogDensity: Float, brightness: Float
    ) {
        if (worldIndexCount == 0) return
        val p = worldProg
        GLES30.glUseProgram(p)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(p, "uViewProj"), 1, false, viewProj, 0)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uCamPos"), s.posX, GameSession.EYE_HEIGHT, s.posZ)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uLightColor"), lr, lg, lb)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uLightRadius"), radius)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uLightIntensity"), 2.45f * flicker)
        val t = s.theme
        GLES30.glUniform3f(
            GLES30.glGetUniformLocation(p, "uAmbient"),
            t.ambientR + ambBoost, t.ambientG + ambBoost, t.ambientB + ambBoost
        )
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uFogColor"), t.fogR, t.fogG, t.fogB)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uFogDensity"), fogDensity)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uVeinColor"), t.veinR, t.veinG, t.veinB)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uVeinPulse"), 0.09f)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uBrightness"), brightness)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uTime"), time)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uNormalStrength"), if (save.settings.quality >= 2) 1f else 0.6f)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(p, "uQuality"), save.settings.quality)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uSonarRange"), if (s.isSonarOn()) s.sonarRange() else 0f)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uSonarColor"), 0.35f, 0.85f, 1.0f)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY, albedoTex)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(p, "uAlbedo"), 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY, normalTex)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(p, "uNormalMap"), 1)

        GLES30.glBindVertexArray(worldVao[0])
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, worldIndexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun drawProps(
        s: GameSession, lr: Float, lg: Float, lb: Float,
        radius: Float, ambBoost: Float, fogDensity: Float, brightness: Float
    ) {
        val gem = shapeGem ?: return
        val cone = shapeCone ?: return
        val coneD = shapeConeDown ?: return
        val boxS = shapeBox ?: return
        val cyl = shapeCylinder ?: return
        val arrow = shapeArrow ?: return

        val cull = when (save.settings.quality) { 0 -> 22f; 1 -> 28f; 2 -> 34f; else -> 42f }
        val cull2 = cull * cull
        val px = s.posX; val pz = s.posZ
        fun near(x: Float, z: Float): Boolean {
            val dx = x - px; val dz = z - pz
            return dx * dx + dz * dz < cull2
        }

        gem.begin(); cone.begin(); coneD.begin(); boxS.begin(); cyl.begin(); arrow.begin()
        val C = GameSession.CELL
        val m = s.maze

        // --- ecos, cristales y cofres
        for (pk in s.pickups) {
            if (pk.taken) continue
            val x = (pk.gx + 0.5f) * C + pk.flyX
            val z = (pk.gy + 0.5f) * C + pk.flyZ
            if (!near(x, z)) continue
            val fy = WorldMesh.floorHeight(pk.gx, pk.gy)
            when (pk.kind) {
                GameSession.PickupKind.ECO ->
                    gem.add(x, fy + 0.62f, z, 0.155f, 0.98f, 0.78f, 0.36f, 0.55f, pk.bob * 1.4f, pk.bob, 1f, 1f)
                GameSession.PickupKind.ECO_GRANDE ->
                    gem.add(x, fy + 0.72f, z, 0.255f, 1.0f, 0.86f, 0.42f, 0.85f, pk.bob * 1.1f, pk.bob, 1f, 1f)
                GameSession.PickupKind.VETAGRIS ->
                    gem.add(x, fy + 0.80f, z, 0.34f, 0.80f, 0.92f, 1.0f, 1.05f, pk.bob * 0.8f, pk.bob, 1f, 1f)
                GameSession.PickupKind.COFRE -> {
                    boxS.add(x, fy + 0.30f, z, 0.62f, 0.36f, 0.24f, 0.14f, 0.04f, pk.bob * 0.15f, pk.bob, 0f, 1f)
                    boxS.add(x, fy + 0.60f, z, 0.30f, 0.86f, 0.66f, 0.24f, 0.20f, pk.bob * 0.15f, pk.bob, 0f, 1f)
                }
            }
        }

        // --- estalagmitas (suelo) y estalactitas (techo)
        for (gi in s.stalagmites) {
            val gx = gi % m.gw; val gy = gi / m.gw
            val x = (gx + 0.5f) * C; val z = (gy + 0.5f) * C
            if (!near(x, z)) continue
            val h = 0.42f + ((gx * 7 + gy * 13) % 7) * 0.13f
            val t = s.theme
            if ((gx + gy) % 2 == 0) {
                cone.add(x, WorldMesh.floorHeight(gx, gy), z, h, t.rockR * 1.15f, t.rockG * 1.15f, t.rockB * 1.15f, 0f,
                    ((gx * 31 + gy * 17) % 20) * 0.31f, 0f, 0f, 1f)
            } else {
                coneD.add(x, WorldMesh.ceilHeight(gx, gy), z, h * 0.78f, t.rockR * 0.95f, t.rockG * 0.95f, t.rockB * 0.95f, 0f,
                    ((gx * 13 + gy * 29) % 20) * 0.31f, 0f, 0f, 1f)
            }
        }

        // --- antorchas de pared
        for (gi in s.torches) {
            val gx = gi % m.gw; val gy = gi / m.gw
            val x = (gx + 0.5f) * C; val z = (gy + 0.5f) * C
            if (!near(x, z)) continue
            // Se pega a la pared mas cercana
            var ox = 0f; var oz = 0f
            if (m.isSolid(gx - 1, gy)) ox = -C * 0.38f
            else if (m.isSolid(gx + 1, gy)) ox = C * 0.38f
            else if (m.isSolid(gx, gy - 1)) oz = -C * 0.38f
            else if (m.isSolid(gx, gy + 1)) oz = C * 0.38f
            val tx = x + ox; val tz = z + oz
            val baseY = WorldMesh.floorHeight(gx, gy) + 1.75f
            cyl.add(tx, baseY, tz, 0.42f, 0.28f, 0.19f, 0.12f, 0.02f, 0f, 0f, 0f, 1f)
            val ph = ((gx * 41 + gy * 7) % 30) * 0.21f
            val fl = 0.86f + 0.14f * sin((time * 7f + ph).toDouble()).toFloat()
            gem.add(tx, baseY + 0.50f, tz, 0.17f * fl, 1.0f, 0.62f, 0.22f, 1.35f, time * 2.1f + ph, ph, 1f, 1f)
        }

        // --- trampas descubiertas
        for (tr in s.traps) {
            if (!tr.revealed) continue
            val x = (tr.gx + 0.5f) * C; val z = (tr.gy + 0.5f) * C
            if (!near(x, z)) continue
            val fy = WorldMesh.floorHeight(tr.gx, tr.gy)
            val pulse = 0.55f + 0.45f * sin((time * 3.4f + tr.gx + tr.gy).toDouble()).toFloat()
            boxS.add(x, fy + 0.045f, z, 1.55f, 0.85f, 0.14f, 0.10f, 0.30f * pulse, 0f, 0f, 0f, 1f)
        }

        // --- salida: columna de cristal que se ve de lejos
        run {
            val ex = s.exitWorldX; val ez = s.exitWorldZ
            val fy = WorldMesh.floorHeight(s.maze.exitGx, s.maze.exitGy)
            val d = hypot(ex - px, ez - pz)
            val visible = d < cull * 1.9f
            if (visible) {
                val ping = if (s.exitPingFlash > 0f) 0.6f else 0f
                cyl.add(ex, fy, ez, 2.55f, 0.55f, 0.92f, 0.78f, 0.85f + ping, 0f, 0f, 0f, 1f)
                for (i in 0 until 5) {
                    val a = time * 0.55f + i * (2f * Math.PI.toFloat() / 5f)
                    val rr = 0.85f
                    gem.add(
                        ex + cos(a.toDouble()).toFloat() * rr,
                        fy + 0.85f + sin((time * 1.4f + i).toDouble()).toFloat() * 0.16f,
                        ez + sin(a.toDouble()).toFloat() * rr,
                        0.24f, 0.62f, 1.0f, 0.86f, 1.15f + ping, a, i.toFloat(), 1f, 1f
                    )
                }
            }
        }

        // --- flecha de la brujula
        if (s.isCompassOn()) {
            // Flota delante del jugador y apunta siempre a la salida.
            val yawRad = Math.toRadians(s.yawDeg.toDouble())
            val ax = px + sin(yawRad).toFloat() * 1.55f
            val az = pz + cos(yawRad).toFloat() * 1.55f
            // El shader gira la malla (que mira a +Z) con: dir = (sin a, cos a)
            val ang = Math.atan2((s.exitWorldX - px).toDouble(), (s.exitWorldZ - pz).toDouble()).toFloat()
            arrow.add(
                ax, GameSession.EYE_HEIGHT - 0.40f, az, 0.22f,
                1.0f, 0.84f, 0.36f, 1.05f,
                ang, 0f, 0f, 1f
            )
        }

        // ------------------------------------------------------- draw
        val p = propProg
        GLES30.glUseProgram(p)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(p, "uViewProj"), 1, false, viewProj, 0)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uTime"), time)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uCamPos"), px, GameSession.EYE_HEIGHT, pz)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uLightColor"), lr, lg, lb)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uLightRadius"), radius)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uLightIntensity"), 2.45f * flicker)
        val t = s.theme
        GLES30.glUniform3f(
            GLES30.glGetUniformLocation(p, "uAmbient"),
            t.ambientR + ambBoost, t.ambientG + ambBoost, t.ambientB + ambBoost
        )
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uFogColor"), t.fogR, t.fogG, t.fogB)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uFogDensity"), fogDensity)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uBrightness"), brightness)

        gem.draw(); cone.draw(); coneD.draw(); boxS.draw(); cyl.draw(); arrow.draw()
    }

    private fun drawDecals(s: GameSession, fogDensity: Float, brightness: Float) {
        decalData.clear()
        var quads = 0
        val threadOn = s.effects.isActive(com.mggx.laberinto.game.EffectType.HILO_ARIADNA)

        fun quad(x: Float, z: Float, gx: Int, gy: Int, size: Float, r: Float, g: Float, b: Float, a: Float) {
            if (quads >= decalCapacity) return
            val y = WorldMesh.floorHeight(gx, gy) + 0.022f
            val h = size * 0.5f
            val v = floatArrayOf(
                x - h, y, z - h, 0f, 0f,
                x + h, y, z - h, 1f, 0f,
                x + h, y, z + h, 1f, 1f,
                x - h, y, z + h, 0f, 1f
            )
            fun put(i: Int) {
                decalData.add(v[i * 5], v[i * 5 + 1], v[i * 5 + 2], v[i * 5 + 3], v[i * 5 + 4], r, g, b, a)
            }
            put(0); put(1); put(2)
            put(0); put(2); put(3)
            quads++
        }

        val px = s.posX; val pz = s.posZ
        for (tp in s.trail) {
            val dx = tp.x - px; val dz = tp.z - pz
            if (dx * dx + dz * dz > 900f) continue
            val gx = (tp.x / GameSession.CELL).toInt()
            val gy = (tp.z / GameSession.CELL).toInt()
            val alpha = if (threadOn) 0.55f else (tp.life / s.stats.trailSeconds).coerceIn(0f, 1f) * 0.32f
            if (threadOn) quad(tp.x, tp.z, gx, gy, 1.05f, 0.45f, 0.95f, 0.75f, alpha)
            else quad(tp.x, tp.z, gx, gy, 0.72f, 0.72f, 0.66f, 0.52f, alpha)
        }
        val markColors = arrayOf(
            floatArrayOf(1f, 0.85f, 0.35f), floatArrayOf(0.45f, 0.9f, 1f),
            floatArrayOf(0.6f, 1f, 0.55f), floatArrayOf(1f, 0.55f, 0.75f)
        )
        for (mk in s.marks) {
            val gx = (mk.x / GameSession.CELL).toInt()
            val gy = (mk.z / GameSession.CELL).toInt()
            val c = markColors[mk.colorIndex % markColors.size]
            quad(mk.x, mk.z, gx, gy, 1.5f, c[0], c[1], c[2], 0.78f)
        }
        if (quads == 0) return

        val p = decalProg
        GLES30.glUseProgram(p)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(p, "uViewProj"), 1, false, viewProj, 0)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uCamPos"), s.posX, GameSession.EYE_HEIGHT, s.posZ)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uFogDensity"), fogDensity)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uBrightness"), brightness)

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)
        GLES30.glBindVertexArray(decalVao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, decalVbo[0])
        val arr = decalData.toArray()
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, arr.size * 4, GLUtil.floatBuffer(arr))
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, quads * 6)
        GLES30.glBindVertexArray(0)
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    private fun drawArms(
        s: GameSession, lr: Float, lg: Float, lb: Float,
        ambBoost: Float, brightness: Float, aspect: Float, dt: Float
    ) {
        if (armsIndexCount == 0) return
        // Los brazos usan su propia proyeccion, mas cerrada, y limpian profundidad
        // para que nunca los atraviese una pared.
        Matrix.perspectiveM(armProj, 0, 62f, aspect, 0.01f, 4f)
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT)

        val speed = hypot(s.velX, s.velZ) / max(0.001f, s.stats.walkSpeed)
        val bob = sin((time * 7.2f).toDouble()).toFloat() * 0.022f * speed * save.settings.headBob
        val sway = sin((time * 3.6f).toDouble()).toFloat() * 0.016f * speed
        val breathe = sin((time * 1.25f).toDouble()).toFloat() * 0.006f

        // Las manos tienen que entrar dentro del cono de vision: con 62 grados
        // de campo vertical, a 44 cm de la camara el borde de abajo esta a
        // 0.44 * tan(31) = 0.264. Por eso la altura es -0.205 y no mas baja.
        fun armMatrix(out: FloatArray, side: Float) {
            Matrix.setIdentityM(out, 0)
            Matrix.translateM(
                out, 0,
                side * (0.205f + sway * side * 0.5f),
                -0.190f + bob + breathe,
                -0.50f - abs(sway) * 0.4f
            )
            // Las manos se inclinan hacia abajo y hacia adentro: asi se ve el
            // dorso y los dedos, en vez de mirarlos de punta.
            Matrix.rotateM(out, 0, side * -16f, 0f, 1f, 0f)
            Matrix.rotateM(out, 0, -20f + bob * 80f, 1f, 0f, 0f)
            Matrix.rotateM(out, 0, side * 11f, 0f, 0f, 1f)
        }
        armMatrix(armMatL, -1f)
        armMatrix(armMatR, 1f)

        val p = armsProg
        GLES30.glUseProgram(p)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(p, "uProj"), 1, false, armProj, 0)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(p, "uArmL"), 1, false, armMatL, 0)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(p, "uArmR"), 1, false, armMatR, 0)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uLightColor"), lr, lg, lb)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uLightIntensity"), 2.0f * flicker)
        val t = s.theme
        GLES30.glUniform3f(
            GLES30.glGetUniformLocation(p, "uAmbient"),
            t.ambientR + ambBoost, t.ambientG + ambBoost, t.ambientB + ambBoost
        )
        val style = s.stats.gloveStyle
        val skin = when (style) {
            2 -> floatArrayOf(0.32f, 0.27f, 0.25f)
            4 -> floatArrayOf(0.74f, 0.66f, 0.60f)
            else -> floatArrayOf(0.68f, 0.50f, 0.39f)
        }
        val cloth = when (style) {
            1 -> floatArrayOf(0.36f, 0.37f, 0.40f)
            2 -> floatArrayOf(0.19f, 0.17f, 0.17f)
            3 -> floatArrayOf(0.40f, 0.28f, 0.17f)
            4 -> floatArrayOf(0.44f, 0.52f, 0.58f)
            else -> floatArrayOf(0.47f, 0.33f, 0.21f)
        }
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uSkin"), skin[0], skin[1], skin[2])
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uCloth"), cloth[0], cloth[1], cloth[2])
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uBrightness"), brightness)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uTime"), time)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(p, "uStyle"), style)

        GLES30.glBindVertexArray(armsVao[0])
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, armsIndexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun drawOverlay(s: GameSession, theme: CaveTheme, brightness: Float) {
        val p = overlayProg
        GLES30.glUseProgram(p)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uStrength"), 0.62f)
        GLES30.glUniform3f(GLES30.glGetUniformLocation(p, "uTintColor"), theme.fogR, theme.fogG, theme.fogB)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uTintAmount"), 0.30f)
        val lowHp = if (s.healthFraction() < 0.28f) (0.28f - s.healthFraction()) * 1.7f else 0f
        GLES30.glUniform1f(GLES30.glGetUniformLocation(p, "uHurt"), max(hurtFlash * 0.55f, lowHp))

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glBindVertexArray(overlayVao[0])
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
        GLES30.glBindVertexArray(0)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_BLEND)
    }
}
