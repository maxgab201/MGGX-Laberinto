package com.mggx.laberinto.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.mggx.laberinto.core.AhorroDeEnergia
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.maze.CaveTheme
import com.mggx.laberinto.maze.MazeGenerator
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Renderer de la cueva. Todo el dibujo pasa por aca:
 *  1) mundo (malla unica del laberinto, con texturas procedurales)
 *  2) objetos instanciados (ecos, cristales, antorchas, estalagmitas, salida)
 *  3) calcomanias de suelo (rastro de pasos, hilo de Ariadna)
 *  4) brazos en primera persona, con su propia profundidad
 *  5) vineta y tinte de pantalla
 */
class CaveRenderer(
    private val save: SaveData,
    private val input: InputState
) : GLSurfaceView.Renderer {

    companion object {
        /** Tiene que coincidir con el MAX_LUCES de los shaders. */
        const val MAX_LUCES = 8

        /**
         * Fuerza de la antorcha que lleva el jugador.
         *
         * Bajo de 2.45 a 1.55 en la 1.8.0. Con 2.45 la antorcha alumbraba como
         * un reflector: la cueva se veia entera hasta el fondo del pasillo y no
         * daba ninguna sensacion de estar bajo tierra a oscuras. Con este
         * valor hay un circulo de luz alrededor tuyo y despues negro, que es
         * lo que hace que una antorcha de la pared o un cristal a lo lejos
         * signifiquen algo.
         */
        const val LUZ_JUGADOR = 1.55f

        /**
         * Cuanto se apaga la luz ambiente del bioma.
         *
         * El ambiente es la luz "que hay porque si", sin fuente: es lo que
         * evita que un rincon sin antorchas quede completamente negro. Mientras
         * estuvo al 100% no habia oscuridad de verdad en ningun lado, se veia
         * hasta el fondo del pasillo y la linterna no se extranaba nunca.
         *
         * Se aplica ACA, al subir el uniform, y NO adentro de cada shader. Es a
         * proposito: el mundo, los objetos, los bichos y los brazos son cinco
         * programas distintos que leen el mismo `uAmbient`. Oscurecer solo uno
         * de ellos deja a los objetos MAS claros que la roca sobre la que
         * estan apoyados, y eso se ve como si flotaran recortados encima de la
         * cueva. Un solo lugar para tocarlo es la unica forma de que los cinco
         * sigan estando de acuerdo.
         */
        const val AMBIENTE = 0.52f

        // Radio en metros de cada pickup (el octaedro base de shapeGem mide
        // radio 1.0, asi que esto ES el radio real del objeto). Antes eran
        // 0.155/0.255/0.34 - el Vetagris llegaba a medir lo mismo que el
        // cuerpo entero del jugador.
        const val ESCALA_ECO = 0.075f
        const val ESCALA_ECO_GRANDE = 0.12f
        const val ESCALA_VETAGRIS = 0.16f

        /** Radio de la punta de una estalactita/estalagmita de bioma cueva. */
        const val RADIO_ESTALACTITA = 0.13f

        /**
         * Alto total de la antorcha en metros. Como el modelo de
         * [StructureMeshes.antorcha] mide 1 de alto, la escala de instancia ES
         * su altura.
         */
        const val ALTO_ANTORCHA = 0.48f

        /**
         * Alto de la llama en metros, antes del parpadeo. El modelo tiene la
         * base en y=0, asi que apoyada en el cuenco no puede hundirse en el
         * poste (que era el problema con el octaedro, que tenia el centro en
         * su posicion y quedaba media figura adentro).
         */
        const val ESCALA_LLAMA = 0.22f

        /** Que tan lejos del centro de la casilla se cuelga, hacia la pared. */
        const val SEPARACION_ANTORCHA = 0.46f

        /** A que altura del piso se clava la base de la antorcha, en metros. */
        const val ALTURA_ANTORCHA = 1.75f
    }

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

        /**
         * Id del consumible que el jugador tiene elegido en la barra.
         *
         * Va por aca y no se lee de SaveData porque no es una preferencia
         * guardada: es el objeto que tenes en la mano AHORA, y lo elige la
         * barra del HUD, que vive en el hilo de la UI. El hilo de GL lo lee
         * cada cuadro para saber que modelo poner en la mano izquierda.
         */
        @Volatile var objetoEnMano: String = ""

        /** Postura pedida: 0 de pie, 1 agachado, 2 arrastrandose. */
        @Volatile var crouchLevel = 0
        /** Se pone en true al tocar saltar y lo consume el frame siguiente. */
        @Volatile var jumpPending = false

        val running: Boolean get() = runStick || runButton || runPad || autoRun

        fun releaseAll() {
            moveX = 0f; moveY = 0f; lookDX = 0f; lookDY = 0f
            runStick = false; runButton = false; runPad = false
            jumpPending = false
        }

        fun consumeJump(): Boolean {
            val j = jumpPending
            jumpPending = false
            return j
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
    /** Se perdio el contexto de GL y hay que rearmar el nivel que ya estaba. */
    @Volatile private var needsRebuild = false
    /**
     * Modo vitrina del lobby: la cueva se dibuja de verdad, pero nadie la
     * juega. La camara pasea sola por la sala de entrada y no hay brazos,
     * ni sonidos, ni logica de partida.
     */
    @Volatile var vitrina: Boolean = false
    private var paseo: Float = 0f
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
    /**
     * Posiciones de uniforms, preguntadas una sola vez por nombre.
     *
     * Antes cada `GLES30.glGetUniformLocation` de cada cuadro le pedia al
     * driver que buscara el nombre por string en la tabla del programa: 62
     * busquedas por cuadro, casi 3.700 por segundo, siempre con el mismo
     * resultado. La posicion de un uniform no cambia mientras el programa siga
     * enlazado, asi que preguntarlo de nuevo es trabajo de CPU tirado, y en un
     * telefono eso se paga en bateria.
     */
    private val uniforms = UniformCache()

    /** La posicion del uniform [nombre] en [prog], de la cache. */
    private fun u(prog: Int, nombre: String): Int =
        uniforms.loc(prog, nombre) { pr, n -> GLES30.glGetUniformLocation(pr, n) }

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

    /** Polvo en el aire: instancias que se rearman cada cuadro. */
    private var motaProg = 0
    private val motaVao = IntArray(1)
    private val motaQuadVbo = IntArray(1)
    private val motaVbo = IntArray(1)
    private var motaCapacidad = 0
    private var motaDatos = FloatArray(0)
    private val motaPos = FloatArray(3)
    /** Ejes de la camara, para que cada mota mire siempre al ojo. */
    private var camRightX = 1f; private var camRightY = 0f; private var camRightZ = 0f
    private var camUpX = 0f; private var camUpY = 1f; private var camUpZ = 0f

    /** El espejo de agua del nivel. Va aparte porque se dibuja transparente. */
    private var waterProg = 0
    private val waterVao = IntArray(1)
    private val waterVbo = IntArray(1)
    private val waterEbo = IntArray(1)
    private var waterIndexCount = 0
    private var albedoTex = 0
    private var normalTex = 0
    private var texturedTheme: CaveTheme? = null
    private var preparedQuality = -1

    // ------------------------------------------------------------ props
    private var shapeGem: InstancedShape? = null
    private var shapeCone: InstancedShape? = null
    private var shapeBox: InstancedShape? = null
    private var shapeCylinder: InstancedShape? = null
    private var shapeArrow: InstancedShape? = null
    /** Vigas y travesanos de madera. */
    /**
     * Pasa un color ARGB de la interfaz al espacio lineal que usa el shader.
     * Sin esto los trajes se ven lavados, porque el shader ya hace su propia
     * correccion de gamma al final.
     */
    private fun sRgbLineal(argb: Long): FloatArray {
        fun canal(v: Int): Float {
            val c = v / 255f
            return if (c <= 0.04045f) c / 12.92f
            else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        }
        return floatArrayOf(
            canal(((argb shr 16) and 0xFF).toInt()),
            canal(((argb shr 8) and 0xFF).toInt()),
            canal((argb and 0xFF).toInt())
        )
    }

    // ---------------------------------------------------- luces de la cueva
    //
    // Antes la unica luz del mundo era la del jugador, asi que una galeria
    // llena de antorchas y cristales se veia igual de negra que una vacia.
    // Aca se juntan una sola vez por nivel todas las fuentes fijas, y cada
    // frame se le pasan al shader las mas cercanas.

    private class Farol(
        val x: Float, val y: Float, val z: Float,
        val r: Float, val g: Float, val b: Float,
        val alcance: Float,
        /** Las antorchas titilan; un cristal no. */
        val titila: Boolean
    )

    private var faroles: List<Farol> = emptyList()
    private val luzPos = FloatArray(MAX_LUCES * 4)
    private val luzColor = FloatArray(MAX_LUCES * 3)
    private var luzCount = 0

    /** Arrays de trabajo de [elegirLuces], reusados cuadro a cuadro. */
    private val mejoresLuz = arrayOfNulls<Farol>(MAX_LUCES)
    private val distLuz = FloatArray(MAX_LUCES)

    /**
     * Elige las [MAX_LUCES] luces mas cercanas al jugador y las deja armadas
     * en los arrays que van al shader. Es un barrido lineal: con unos cientos
     * de candidatas sale mucho mas barato que ordenar la lista entera.
     */
    private fun elegirLuces(px: Float, pz: Float, calidad: Int, parpadeo: Float) {
        luzCount = 0
        if (calidad <= 0 || faroles.isEmpty()) return
        val cupo = if (calidad >= 3) MAX_LUCES else if (calidad == 2) 6 else 4

        // Los dos arrays de trabajo son CAMPOS, no locales: creados aca
        // adentro eran dos objetos nuevos por cuadro (120 por segundo) que no
        // sobrevivian al cuadro siguiente. Basura asi no se nota en cuadros
        // por segundo, se nota en el recolector corriendo todo el tiempo, y
        // eso es bateria.
        java.util.Arrays.fill(mejoresLuz, null)
        java.util.Arrays.fill(distLuz, Float.MAX_VALUE)
        val mejores = mejoresLuz
        val dist = distLuz
        for (f in faroles) {
            val dx = f.x - px
            val dz = f.z - pz
            val d2 = dx * dx + dz * dz
            // Fuera de su propio alcance no aporta nada.
            if (d2 > (f.alcance + 2f) * (f.alcance + 2f)) continue
            var i = cupo - 1
            if (d2 >= dist[i]) continue
            while (i > 0 && d2 < dist[i - 1]) {
                dist[i] = dist[i - 1]; mejores[i] = mejores[i - 1]; i--
            }
            dist[i] = d2; mejores[i] = f
        }
        for (i in 0 until cupo) {
            val f = mejores[i] ?: break
            val k = if (f.titila) parpadeo else 1f
            luzPos[luzCount * 4] = f.x
            luzPos[luzCount * 4 + 1] = f.y
            luzPos[luzCount * 4 + 2] = f.z
            luzPos[luzCount * 4 + 3] = f.alcance
            luzColor[luzCount * 3] = f.r * k
            luzColor[luzCount * 3 + 1] = f.g * k
            luzColor[luzCount * 3 + 2] = f.b * k
            luzCount++
        }
    }

    /** Sube las luces elegidas al programa que este activo. */
    private fun subirLuces(prog: Int) {
        GLES30.glUniform1i(u(prog, "uNumLuces"), luzCount)
        if (luzCount == 0) return
        GLES30.glUniform4fv(u(prog, "uLuzPos"), luzCount, luzPos, 0)
        GLES30.glUniform3fv(u(prog, "uLuzColor"), luzCount, luzColor, 0)
    }

    /** Junta todas las fuentes fijas del nivel. Se llama al armar el nivel. */
    private fun armarFaroles(s: GameSession) {
        val lista = ArrayList<Farol>()
        val m = s.maze
        val t = s.theme
        val C = GameSession.CELL

        for (gi in s.torches) {
            val gx = gi % m.gw; val gy = gi / m.gw
            var ox = 0f; var oz = 0f
            if (m.isSolid(gx - 1, gy)) ox = -C * SEPARACION_ANTORCHA
            else if (m.isSolid(gx + 1, gy)) ox = C * SEPARACION_ANTORCHA
            else if (m.isSolid(gx, gy - 1)) oz = -C * SEPARACION_ANTORCHA
            else if (m.isSolid(gx, gy + 1)) oz = C * SEPARACION_ANTORCHA
            lista.add(
                Farol(
                    // Justo en la llama: si fuera una altura suelta, al
                    // mover la antorcha la luz quedaria colgada en otro lado.
                    (gx + 0.5f) * C + ox,
                    m.floorY(gx, gy) + ALTURA_ANTORCHA + ALTO_ANTORCHA * StructureMeshes.ALTURA_DEL_FUEGO,
                    (gy + 0.5f) * C + oz,
                    1.00f, 0.58f, 0.22f, 7.0f, true
                )
            )
        }
        for (gi in s.crystalClusters) {
            val gx = gi % m.gw; val gy = gi / m.gw
            lista.add(
                Farol(
                    (gx + 0.5f) * C, m.floorY(gx, gy) + 0.45f, (gy + 0.5f) * C,
                    t.veinR * 0.55f, t.veinG * 0.55f, t.veinB * 0.55f, 4.6f, false
                )
            )
        }
        for (gi in s.mushrooms) {
            val gx = gi % m.gw; val gy = gi / m.gw
            lista.add(
                Farol(
                    (gx + 0.5f) * C, m.floorY(gx, gy) + 0.55f, (gy + 0.5f) * C,
                    0.22f, 0.52f, 0.32f, 3.4f, false
                )
            )
        }
        for (gi in s.carbideStations) {
            val gx = gi % m.gw; val gy = gi / m.gw
            lista.add(
                Farol(
                    (gx + 0.5f) * C, m.floorY(gx, gy) + 1.10f, (gy + 0.5f) * C,
                    0.62f, 0.56f, 0.30f, 5.2f, true
                )
            )
        }
        // La salida se ve de lejos a proposito: es la referencia del nivel.
        lista.add(
            Farol(
                s.exitWorldX, m.floorY(m.exitGx, m.exitGy) + 1.2f, s.exitWorldZ,
                0.30f, 0.62f, 0.52f, 9.5f, false
            )
        )
        faroles = lista
    }

    /** Direccion en la que mira la camara este frame (para la linterna). */
    private var camDirX = 0f
    private var camDirY = 0f
    private var camDirZ = 1f
    private var shapeSlab: InstancedShape? = null
    /** Ala de murcielago: membrana con los dedos festoneados, que aletea. */
    private var shapeWing: InstancedShape? = null
    /** Pincho de trampa. */
    /** Roca suelta. */
    private var shapeBoulder: InstancedShape? = null

    // ---------------------------------------------------- modelos de bichos
    // Cada bicho tiene su malla propia (ver EnemyMeshes): antes se armaban
    // apilando las mismas primitivas genericas que el resto de la cueva.
    private var shapeMurcielago: InstancedShape? = null
    private var shapeRastreroCuerpo: InstancedShape? = null
    private var shapeRastreroPata: InstancedShape? = null
    private var shapeGuardianTorso: InstancedShape? = null
    private var shapeGuardianCabeza: InstancedShape? = null
    private var shapeGuardianBrazo: InstancedShape? = null
    private var shapeTopo: InstancedShape? = null
    private var shapeTopoPala: InstancedShape? = null
    private var shapeArana: InstancedShape? = null

    // ------------------------------------------------ modelos de estructuras
    // Ver StructureMeshes: antes casi todas reusaban las mismas primitivas.
    private var shapeAntorcha: InstancedShape? = null
    private var shapeLlama: InstancedShape? = null
    private var shapeCristal: InstancedShape? = null
    private var shapeCofre: InstancedShape? = null
    private var shapeHongo: InstancedShape? = null
    private var shapePincho: InstancedShape? = null
    private var shapeEstacion: InstancedShape? = null
    private var shapeObelisco: InstancedShape? = null
    /** Poste fino: los parantes de los marcos de la mina. */
    /** Los companieros de sala. Solo se usan en partida de a varios. */
    private var shapeMinero: InstancedShape? = null
    private var shapeCasco: InstancedShape? = null
    private var shapeCabeza: InstancedShape? = null

    private var shapePost: InstancedShape? = null
    /** Estalactita: punta fina colgando del techo. */
    private var shapeStalactite: InstancedShape? = null

    // ------------------------------------------------------------ brazos
    private val armsVao = IntArray(1)
    private val armsVbo = IntArray(1)
    private val armsEbo = IntArray(1)
    private var armsIndexCount = 0
    /**
     * Con que arma esta armada la malla de los brazos ahora mismo.
     *
     * El arma va adentro de la misma malla, asi que cambiarla obliga a
     * rehacerla. Pasa solo al equipar otra en el menu, no en cada cuadro.
     */
    private var armaEnMalla: String? = null
    private var objetoEnMalla: String? = null

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

    /**
     * Encola un nivel nuevo. Marca ready = false hasta que el hilo de GL lo
     * levante y arme su malla: si no, la pantalla de carga se iba de una en el
     * segundo nivel y el juego arrancaba mostrando todavia el nivel anterior.
     *
     * Tambien vacia la cola: si quedo algun nivel encolado sin levantar, tiene
     * que quedar descartado, nunca pisar al que se pide ahora.
     */
    private val actions = ConcurrentLinkedQueue<Pair<GameSession, () -> Unit>>()

    fun dispatch(s: GameSession, action: () -> Unit) { actions.add(s to action) }

    fun setSession(s: GameSession) {
        ready = false
        soundOut.clear()
        actions.clear()
        pending.clear()
        pending.add(s)
    }

    // ------------------------------------------------------ ciclo de vida

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.02f, 0.018f, 0.016f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glFrontFace(GLES30.GL_CCW)

        // Se perdio el contexto: los programas de recien tienen ids nuevos, y
        // una posicion de uniform guardada apuntaria a cualquier lado.
        uniforms.limpiar()

        worldProg = GLUtil.program(Shaders.WORLD_VS, Shaders.WORLD_FS)
        propProg = GLUtil.program(Shaders.PROP_VS, Shaders.PROP_FS)
        armsProg = GLUtil.program(Shaders.ARMS_VS, Shaders.ARMS_FS)
        decalProg = GLUtil.program(Shaders.DECAL_VS, Shaders.DECAL_FS)
        overlayProg = GLUtil.program(Shaders.OVERLAY_VS, Shaders.OVERLAY_FS)
        waterProg = GLUtil.program(Shaders.WATER_VS, Shaders.WATER_FS)
        motaProg = GLUtil.program(Shaders.MOTA_VS, Shaders.MOTA_FS)

        GLES30.glGenVertexArrays(1, worldVao, 0)
        GLES30.glGenBuffers(1, worldVbo, 0)
        GLES30.glGenBuffers(1, worldEbo, 0)
        GLES30.glGenVertexArrays(1, waterVao, 0)
        GLES30.glGenBuffers(1, waterVbo, 0)
        GLES30.glGenBuffers(1, waterEbo, 0)

        buildArms()
        buildDecalBuffer(2048)
        buildMotas()
        buildOverlay()
        buildShapes()

        // El contexto de GL se puede perder (pantalla apagada, cambio de app).
        // Cuando vuelve hay que rehacer texturas y malla desde cero, si no
        // queda todo en negro.
        //
        // Se marca con una bandera y NO se vuelve a encolar la partida: en la
        // cola puede haber un nivel mas nuevo esperando, y reencolar el viejo
        // lo dejaria ganar el cambio y el jugador entraria al nivel anterior.
        albedoTex = 0
        normalTex = 0
        texturedTheme = null
        worldIndexCount = 0
        lastFrameNs = 0L
        ready = false
        needsRebuild = session != null
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
        if (swapped || needsRebuild || preparedQuality != save.settings.quality) {
            needsRebuild = false
            prepareLevel(s)
            lastFrameNs = 0L
            lastPhase = GameSession.Phase.JUGANDO
            lastHealth = s.health
            ready = true
        }
        while (true) {
            val action = actions.poll() ?: break
            if (action.first === s && !input.paused && s.phase == GameSession.Phase.JUGANDO) action.second()
        }
        if (s.geometryDirty) {
            s.geometryDirty = false
            uploadWorld(WorldMesh.build(s.maze, save.settings.quality))
            uploadWater(WaterMesh.build(s.maze))
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
        if (vitrina) {
            paseoDelLobby(s, dt)
        } else if (!input.paused && s.phase == GameSession.Phase.JUGANDO) {
            var (lx, ly) = input.consumeLook()
            // El stick del mando gira de forma continua mientras se mantiene.
            val padSpeed = GameSession.PAD_LOOK_DEG * input.padSensitivity * dt
            lx += input.padLookX * padSpeed
            ly += input.padLookY * padSpeed
            val gi = GameSession.Input(
                moveX = input.moveX, moveY = input.moveY,
                lookX = lx, lookY = ly, running = input.running,
                agacharse = input.crouchLevel, saltar = input.consumeJump()
            )
            s.update(dt, gi)
        } else {
            input.consumeLook()
            // En pausa la partida se congela, pero la SALA no: hay que seguir
            // latiendo o los demas te dan por ido y te sacan de la lista.
            if (!vitrina) s.latirRed(dt)
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

        val eyeY = s.alturaCamara()
        val yaw = Math.toRadians(s.yawDeg.toDouble())
        val pitch = Math.toRadians(s.pitchDeg.toDouble())
        val cp = cos(pitch).toFloat()
        val fx = (sin(yaw) * cp).toFloat()
        val fy = sin(pitch).toFloat()
        val fz = (cos(yaw) * cp).toFloat()
        // El haz de la linterna sale por donde mira la camara.
        camDirX = fx; camDirY = fy; camDirZ = fz
        // Ejes de la camara, para orientar las motas de polvo hacia el ojo.
        //
        // Es la misma base que arma `Matrix.setLookAtM` mas abajo, calculada
        // aparte porque las motas la necesitan por separado: derecha es
        // `frente x arriba`, que con arriba = (0,1,0) da (-fz, 0, fx); y el
        // arriba de la camara es `derecha x frente`, para que quede
        // perpendicular tambien cuando estas mirando al techo o al piso.
        run {
            val rx = -fz
            val rz = fx
            val rl = kotlin.math.sqrt(rx * rx + rz * rz).coerceAtLeast(1e-5f)
            camRightX = rx / rl; camRightY = 0f; camRightZ = rz / rl
            camUpX = camRightY * fz - camRightZ * fy
            camUpY = camRightZ * fx - camRightX * fz
            camUpZ = camRightX * fy - camRightY * fx
        }

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

        elegirLuces(s.posX, s.posZ, save.settings.quality, flicker)

        GLES30.glClearColor(theme.fogR * 0.55f, theme.fogG * 0.55f, theme.fogB * 0.55f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        drawWorld(s, lr, lg, lb, lightRadius, amb, fogDensity, brightness)
        drawProps(s, lr, lg, lb, lightRadius, amb, fogDensity, brightness)
        drawDecals(s, fogDensity, brightness)
        // El agua va al final de lo del mundo: es transparente, asi que
        // necesita que ya este dibujado todo lo opaco que puede verse debajo.
        drawWater(s, lr, lg, lb, lightRadius, amb, fogDensity, brightness)
        drawMotas(s, lr, lg, lb, lightRadius, fogDensity, brightness)
        if (save.settings.showArms && !vitrina) drawArms(s, lr, lg, lb, amb, brightness, aspect, dt)
        if (!vitrina) drawOverlay(s, theme, brightness)

        limitFrameRate()
    }

    /**
     * Paseo del lobby: la camara gira despacio en la sala de entrada del nivel
     * que te espera, mirando siempre hacia el pasillo por donde vas a salir.
     * No toca el estado de la partida: solo mueve la camara.
     */
    private fun paseoDelLobby(s: GameSession, dt: Float) {
        paseo += dt
        val cx = (s.maze.startGx + 0.5f) * GameSession.CELL
        val cz = (s.maze.startGy + 0.5f) * GameSession.CELL
        // Un circulito chico para no salirse de la casilla ni entrar en la roca.
        val r = GameSession.CELL * 0.16f
        s.posX = cx + cos((paseo * 0.22f).toDouble()).toFloat() * r
        s.posZ = cz + sin((paseo * 0.22f).toDouble()).toFloat() * r
        s.posY = s.maze.floorY(s.maze.startGx, s.maze.startGy)
        // Barrido lento alrededor de la direccion de salida.
        s.yawDeg = s.initialYawPublic() + sin((paseo * 0.17f).toDouble()).toFloat() * 26f
        s.pitchDeg = -4f + sin((paseo * 0.13f + 1.1f).toDouble()).toFloat() * 5f
    }

    /**
     * Techo de cuadros por segundo. Dormir un toque baja el consumo de bateria
     * y el calor del telefono cuando el aparato podria ir mas rapido de lo pedido.
     *
     * El techo no es siempre el mismo: con el menu de pausa abierto la escena
     * de atras esta CONGELADA, asi que dibujarla 60 veces por segundo es pintar
     * 60 veces la misma imagen. La vitrina del lobby es un paseo lento de
     * camara, que a 30 se ve igual. Solo jugando de verdad hace falta todo lo
     * que el jugador haya elegido en Ajustes.
     */
    private fun limitFrameRate() {
        val target = AhorroDeEnergia.fpsObjetivo(dondeEstoy(), save.settings.targetFps)
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

    /** En que situacion esta el renderer, para decidir cuanto trabajo hacer. */
    private fun dondeEstoy(): AhorroDeEnergia.Donde = when {
        vitrina -> AhorroDeEnergia.Donde.VITRINA
        input.paused -> AhorroDeEnergia.Donde.PAUSA
        session?.phase != GameSession.Phase.JUGANDO -> AhorroDeEnergia.Donde.PAUSA
        else -> AhorroDeEnergia.Donde.JUGANDO
    }

    // ------------------------------------------------------------ montaje

    private fun prepareLevel(s: GameSession) {
        armarFaroles(s)
        if (texturedTheme != s.theme || albedoTex == 0 || preparedQuality != save.settings.quality) {
            if (albedoTex != 0) GLES30.glDeleteTextures(2, intArrayOf(albedoTex, normalTex), 0)
            val r = ProcTextures.build(s.theme, save.settings.quality)
            albedoTex = r.albedoTex
            normalTex = r.normalTex
            texturedTheme = s.theme
        }
        uploadWorld(WorldMesh.build(s.maze, save.settings.quality))
        uploadWater(WaterMesh.build(s.maze))
        preparedQuality = save.settings.quality
    }

    /** Sube el espejo de agua del nivel. Sin agua, deja el contador en cero. */
    private fun uploadWater(mesh: WaterMesh.Mesh) {
        waterIndexCount = mesh.indices.size
        if (waterIndexCount == 0) return
        GLES30.glBindVertexArray(waterVao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, waterVbo[0])
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER, mesh.vertices.size * 4,
            GLUtil.floatBuffer(mesh.vertices), GLES30.GL_STATIC_DRAW
        )
        val stride = WaterMesh.STRIDE_BYTES
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 1, GLES30.GL_FLOAT, false, stride, 12)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, waterEbo[0])
        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER, mesh.indices.size * 4,
            GLUtil.intBuffer(mesh.indices), GLES30.GL_STATIC_DRAW
        )
        GLES30.glBindVertexArray(0)
    }

    /**
     * El agua, despues de todo lo opaco.
     *
     * Mezclada y SIN escribir profundidad: si escribiera, un charco taparia lo
     * que viene detras aunque sea medio transparente, y ademas dos casillas de
     * agua vecinas se pelearian entre si. Leer profundidad si, para que la
     * roca que esta delante del charco lo tape.
     */
    private fun drawWater(
        s: GameSession, lr: Float, lg: Float, lb: Float,
        radius: Float, ambBoost: Float, fogDensity: Float, brightness: Float
    ) {
        if (waterIndexCount == 0) return
        val p = waterProg
        GLES30.glUseProgram(p)
        GLES30.glUniformMatrix4fv(u(p, "uViewProj"), 1, false, viewProj, 0)
        GLES30.glUniform3f(u(p, "uCamPos"), s.posX, s.alturaCamara(), s.posZ)
        GLES30.glUniform1f(u(p, "uTime"), time)
        GLES30.glUniform3f(u(p, "uLightColor"), lr, lg, lb)
        GLES30.glUniform1f(u(p, "uLightRadius"), radius)
        GLES30.glUniform1f(u(p, "uLightIntensity"), LUZ_JUGADOR * flicker)
        val t = s.theme
        GLES30.glUniform3f(
            u(p, "uAmbient"),
            (t.ambientR + ambBoost) * AMBIENTE,
            (t.ambientG + ambBoost) * AMBIENTE,
            (t.ambientB + ambBoost) * AMBIENTE
        )
        GLES30.glUniform3f(u(p, "uFogColor"), t.fogR, t.fogG, t.fogB)
        GLES30.glUniform1f(u(p, "uFogDensity"), fogDensity)
        // El agua toma el color de la veta del bioma, apagado: en la mina es
        // turbia y ocre, en el templo tira a turquesa. Que sea siempre azul la
        // haria ver puesta encima, ajena a la cueva.
        GLES30.glUniform3f(u(p, "uAgua"), t.veinR * 0.30f + 0.04f, t.veinG * 0.34f + 0.06f, t.veinB * 0.38f + 0.09f)
        GLES30.glUniform1f(u(p, "uBrightness"), brightness)
        GLES30.glUniform1i(u(p, "uQuality"), save.settings.quality)
        subirLuces(p)

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDepthMask(false)
        GLES30.glBindVertexArray(waterVao[0])
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, waterIndexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
    }

    /** Arma el cuadradito base y el buffer de instancias de las motas. */
    private fun buildMotas() {
        motaCapacidad = Motas.cuantas(3)
        motaDatos = FloatArray(motaCapacidad * 5)
        GLES30.glGenVertexArrays(1, motaVao, 0)
        GLES30.glGenBuffers(1, motaQuadVbo, 0)
        GLES30.glGenBuffers(1, motaVbo, 0)
        GLES30.glBindVertexArray(motaVao[0])

        // El cuadrado, uno solo para todas: dos triangulos de -1 a 1.
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, motaQuadVbo[0])
        val quad = floatArrayOf(
            -1f, -1f, 1f, -1f, 1f, 1f,
            -1f, -1f, 1f, 1f, -1f, 1f
        )
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER, quad.size * 4, GLUtil.floatBuffer(quad), GLES30.GL_STATIC_DRAW
        )
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, motaVbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, motaDatos.size * 4, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, 20, 0)
        GLES30.glVertexAttribDivisor(1, 1)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, 20, 16)
        GLES30.glVertexAttribDivisor(2, 1)
        GLES30.glBindVertexArray(0)
    }

    /**
     * El polvo del aire.
     *
     * Se dibuja al final, con mezcla ADITIVA y sin escribir profundidad: una
     * mota no tapa nada, solo suma un poco de luz donde esta. Con mezcla
     * aditiva tampoco hace falta ordenarlas de atras para adelante, que es lo
     * que suele volver caro dibujar particulas.
     */
    private fun drawMotas(
        s: GameSession, lr: Float, lg: Float, lb: Float,
        radius: Float, fogDensity: Float, brightness: Float
    ) {
        val cuantas = Motas.cuantas(save.settings.quality)
        if (cuantas == 0 || motaCapacidad == 0) return
        val n = min(cuantas, motaCapacidad)

        val px = s.posX
        val py = s.alturaCamara()
        val pz = s.posZ
        var k = 0
        for (i in 0 until n) {
            Motas.posicion(i, time, px, py, pz, motaPos)
            motaDatos[k++] = motaPos[0]
            motaDatos[k++] = motaPos[1]
            motaDatos[k++] = motaPos[2]
            motaDatos[k++] = Motas.tamano(i)
            motaDatos[k++] = Motas.brillo(i, time)
        }

        val p = motaProg
        GLES30.glUseProgram(p)
        GLES30.glUniformMatrix4fv(u(p, "uViewProj"), 1, false, viewProj, 0)
        GLES30.glUniform3f(u(p, "uCamDerecha"), camRightX, camRightY, camRightZ)
        GLES30.glUniform3f(u(p, "uCamArriba"), camUpX, camUpY, camUpZ)
        GLES30.glUniform3f(u(p, "uCamPos"), px, py, pz)
        GLES30.glUniform3f(u(p, "uLightColor"), lr, lg, lb)
        GLES30.glUniform1f(u(p, "uLightRadius"), radius)
        val t = s.theme
        GLES30.glUniform3f(u(p, "uFogColor"), t.fogR, t.fogG, t.fogB)
        GLES30.glUniform1f(u(p, "uFogDensity"), fogDensity)
        GLES30.glUniform1f(u(p, "uBrightness"), brightness)
        subirLuces(p)

        GLES30.glBindVertexArray(motaVao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, motaVbo[0])
        GLES30.glBufferSubData(
            GLES30.GL_ARRAY_BUFFER, 0, n * 5 * 4, GLUtil.floatBuffer(motaDatos)
        )
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE)
        GLES30.glDepthMask(false)
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, 6, n)
        GLES30.glDepthMask(true)
        GLES30.glDisable(GLES30.GL_BLEND)
        GLES30.glBindVertexArray(0)
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
        // El contexto anterior ya destruyo sus recursos; no borrar IDs reutilizados.
        shapeGem = InstancedShape(DetailMeshes.gem(), 1400)
        // Estalagmita (hacia arriba): antes radio 0.42 con altura 1, una
        // relacion de "gorro de fiesta" (2.4:1). Ahora una punta de verdad.
        shapeCone = InstancedShape(DetailMeshes.stalagmite(), 340)
        shapeStalactite = InstancedShape(DetailMeshes.stalagmite(true), 340)
        shapeBox = InstancedShape(DetailMeshes.roundedBox(), 700)
        shapeCylinder = InstancedShape(PropMeshes.cylinder(7, 1f, 0.1f), 900)
        shapeArrow = InstancedShape(PropMeshes.arrow(), 4)
        shapeSlab = InstancedShape(DetailMeshes.roundedBox(1f, 0.13f, 0.13f, 0.018f), 400)
        shapeWing = InstancedShape(EnemyMeshes.murcielagoAla(), 200)
        shapeBoulder = InstancedShape(DetailMeshes.boulder(), 900)
        shapePost = InstancedShape(PropMeshes.cylinder(6, 1f, 0.042f), 160)
        // Los bichos: pocos a la vez, asi que alcanza con cupos chicos.
        shapeMurcielago = InstancedShape(EnemyMeshes.murcielagoCuerpo(), 80)
        shapeRastreroCuerpo = InstancedShape(EnemyMeshes.rastreroSegmento(), 240)
        shapeRastreroPata = InstancedShape(EnemyMeshes.rastreroPata(), 320)
        shapeGuardianTorso = InstancedShape(EnemyMeshes.guardianTorso(), 80)
        shapeGuardianCabeza = InstancedShape(EnemyMeshes.guardianCabeza(), 80)
        shapeGuardianBrazo = InstancedShape(EnemyMeshes.guardianBrazo(), 160)
        shapeTopo = InstancedShape(EnemyMeshes.topoCuerpo(), 80)
        shapeTopoPala = InstancedShape(EnemyMeshes.topoPala(), 160)
        shapeArana = InstancedShape(EnemyMeshes.aranaCuerpo(), 80)
        shapeAntorcha = InstancedShape(StructureMeshes.antorcha(), 160)
        shapeLlama = InstancedShape(StructureMeshes.llama(), 160)
        shapeCristal = InstancedShape(StructureMeshes.cristal(), 900)
        shapeCofre = InstancedShape(StructureMeshes.cofre(), 120)
        shapeHongo = InstancedShape(StructureMeshes.hongo(), 700)
        shapePincho = InstancedShape(StructureMeshes.pincho(), 300)
        shapeEstacion = InstancedShape(StructureMeshes.estacionCarburo(), 60)
        shapeObelisco = InstancedShape(StructureMeshes.obeliscoSalida(), 4)
        // Companieros de sala: en una partida solitaria no se usan nunca, pero
        // salen baratos (una sala no tiene mas de un punado de jugadores).
        shapeMinero = InstancedShape(PlayerMeshes.mineroCuerpo(), 16)
        shapeCasco = InstancedShape(PlayerMeshes.mineroCasco(), 16)
        shapeCabeza = InstancedShape(PlayerMeshes.mineroCabeza(), 16)
    }

    private fun buildArms(arma: String = "", objeto: String = "") {
        GLES30.glGenVertexArrays(1, armsVao, 0)
        GLES30.glGenBuffers(1, armsVbo, 0)
        GLES30.glGenBuffers(1, armsEbo, 0)
        GLES30.glBindVertexArray(armsVao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, armsVbo[0])
        val st = ArmsMesh.STRIDE_BYTES
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, st, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, st, 12)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 1, GLES30.GL_FLOAT, false, st, 24)
        GLES30.glBindVertexArray(0)
        subirMallaDeBrazos(arma, objeto)
    }

    /**
     * Rehace y sube la malla de los brazos con el arma que corresponda.
     *
     * Se reusan el VAO y los buffers: solo cambia el contenido. Pasa cuando
     * el jugador equipa otra arma, no en cada cuadro.
     */
    private fun subirMallaDeBrazos(arma: String, objeto: String) {
        val mesh = ArmsMesh.build(ArmsMesh.Arma.por(arma), ArmsMesh.Objeto.por(objeto))
        armaEnMalla = arma
        objetoEnMalla = objeto
        GLES30.glBindVertexArray(armsVao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, armsVbo[0])
        val vb = GLUtil.floatBuffer(mesh.vertices)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, mesh.vertices.size * 4, vb, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, armsEbo[0])
        val ib = GLUtil.intBuffer(mesh.indices)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, mesh.indices.size * 4, ib, GLES30.GL_STATIC_DRAW)
        armsIndexCount = mesh.indices.size
        GLES30.glBindVertexArray(0)
    }

    /** De que esta hecha cada arma, para pintarla en la mano. */
    private fun colorDeArma(arma: String): FloatArray = when (ArmsMesh.Arma.por(arma)) {
        ArmsMesh.Arma.GARROTE -> floatArrayOf(0.42f, 0.28f, 0.17f)   // roble
        ArmsMesh.Arma.PICO -> floatArrayOf(0.52f, 0.54f, 0.58f)      // hierro
        ArmsMesh.Arma.AGUIJON -> floatArrayOf(0.55f, 0.86f, 0.95f)   // cristal
        ArmsMesh.Arma.MAZA -> floatArrayOf(0.30f, 0.30f, 0.33f)      // basalto
        ArmsMesh.Arma.HACHA -> floatArrayOf(0.72f, 0.80f, 0.88f)     // vetagris
        null -> floatArrayOf(0f, 0f, 0f)
    }

    /**
     * De que color va cada familia de objeto en la mano.
     *
     * El color lo da la FAMILIA y no el objeto suelto a proposito: asi dos
     * frascos distintos comparten forma pero no color, y de reojo se distingue
     * si llevas la pocion verde o el vial de sombra.
     */
    private fun colorDeObjeto(objeto: ArmsMesh.Objeto?): FloatArray = when (objeto) {
        ArmsMesh.Objeto.FRASCO -> floatArrayOf(0.36f, 0.78f, 0.42f)        // pocion
        ArmsMesh.Objeto.ANTORCHA -> floatArrayOf(0.40f, 0.26f, 0.15f)      // madera
        ArmsMesh.Objeto.MAPA -> floatArrayOf(0.82f, 0.73f, 0.55f)          // papel
        ArmsMesh.Objeto.PAN -> floatArrayOf(0.72f, 0.51f, 0.28f)           // corteza
        ArmsMesh.Objeto.OVILLO -> floatArrayOf(0.86f, 0.84f, 0.74f)        // lino
        ArmsMesh.Objeto.INSTRUMENTO -> floatArrayOf(0.58f, 0.56f, 0.48f)   // laton
        ArmsMesh.Objeto.PIEDRA -> floatArrayOf(0.62f, 0.72f, 0.86f)        // mineral
        ArmsMesh.Objeto.VENDA -> floatArrayOf(0.52f, 0.64f, 0.44f)         // musgo
        null -> floatArrayOf(0f, 0f, 0f)
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
        GLES30.glUniformMatrix4fv(u(p, "uViewProj"), 1, false, viewProj, 0)
        GLES30.glUniform3f(u(p, "uCamPos"), s.posX, s.alturaCamara(), s.posZ)
        GLES30.glUniform3f(u(p, "uLightColor"), lr, lg, lb)
        GLES30.glUniform1f(u(p, "uLightRadius"), radius)
        GLES30.glUniform1f(u(p, "uLightIntensity"), LUZ_JUGADOR * flicker)
        val t = s.theme
        GLES30.glUniform3f(
            u(p, "uAmbient"),
            (t.ambientR + ambBoost) * AMBIENTE,
            (t.ambientG + ambBoost) * AMBIENTE,
            (t.ambientB + ambBoost) * AMBIENTE
        )
        GLES30.glUniform3f(u(p, "uFogColor"), t.fogR, t.fogG, t.fogB)
        GLES30.glUniform1f(u(p, "uFogDensity"), fogDensity)
        GLES30.glUniform3f(u(p, "uVeinColor"), t.veinR, t.veinG, t.veinB)
        GLES30.glUniform1f(u(p, "uVeinPulse"), 0.055f)
        GLES30.glUniform1f(u(p, "uBrightness"), brightness)
        GLES30.glUniform1f(u(p, "uTime"), time)
        GLES30.glUniform1f(u(p, "uNormalStrength"), if (save.settings.quality >= 2) 1.35f else 0.7f)
        GLES30.glUniform1i(u(p, "uQuality"), save.settings.quality)
        GLES30.glUniform1f(u(p, "uSonarRange"), if (s.isSonarOn()) s.sonarRange() else 0f)
        // Linterna de carburo: con fuerza 0 el shader la ignora entera.
        val fuerza = s.linternaFuerza()
        GLES30.glUniform3f(u(p, "uSpotDir"), camDirX, camDirY, camDirZ)
        GLES30.glUniform1f(u(p, "uSpotPower"), fuerza * 2.6f)
        GLES30.glUniform1f(u(p, "uSpotRange"), radius * 2.6f)
        GLES30.glUniform1f(u(p, "uSpotCos"), 0.90f)
        subirLuces(p)
        GLES30.glUniform3f(u(p, "uSonarColor"), 0.35f, 0.85f, 1.0f)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY, albedoTex)
        GLES30.glUniform1i(u(p, "uAlbedo"), 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY, normalTex)
        GLES30.glUniform1i(u(p, "uNormalMap"), 1)

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
        val boxS = shapeBox ?: return
        val cyl = shapeCylinder ?: return
        val arrow = shapeArrow ?: return
        val slab = shapeSlab ?: return
        val boulder = shapeBoulder ?: return
        val post = shapePost ?: return
        val stalactite = shapeStalactite ?: return
        val murcielago = shapeMurcielago ?: return
        val ala = shapeWing ?: return
        val rastrero = shapeRastreroCuerpo ?: return
        val pata = shapeRastreroPata ?: return
        val torso = shapeGuardianTorso ?: return
        val cabeza = shapeGuardianCabeza ?: return
        val brazo = shapeGuardianBrazo ?: return
        val topo = shapeTopo ?: return
        val pala = shapeTopoPala ?: return
        val arana = shapeArana ?: return
        val antorcha = shapeAntorcha ?: return
        val llama = shapeLlama ?: return
        val cristal = shapeCristal ?: return
        val cofre = shapeCofre ?: return
        val hongo = shapeHongo ?: return
        val pincho = shapePincho ?: return
        val estacion = shapeEstacion ?: return
        val obelisco = shapeObelisco ?: return
        val minero = shapeMinero ?: return
        val casco = shapeCasco ?: return
        val cabezaMinero = shapeCabeza ?: return

        val cull = when (save.settings.quality) { 0 -> 22f; 1 -> 28f; 2 -> 34f; else -> 42f }
        val cull2 = cull * cull
        val px = s.posX; val pz = s.posZ
        fun near(x: Float, z: Float): Boolean {
            val dx = x - px; val dz = z - pz
            return dx * dx + dz * dz < cull2
        }

        gem.begin(); cone.begin(); boxS.begin(); cyl.begin(); arrow.begin()
        slab.begin(); ala.begin(); boulder.begin(); post.begin()
        stalactite.begin()
        murcielago.begin(); rastrero.begin(); pata.begin()
        torso.begin(); cabeza.begin(); brazo.begin()
        topo.begin(); pala.begin(); arana.begin()
        antorcha.begin(); llama.begin(); cristal.begin(); cofre.begin()
        hongo.begin(); pincho.begin(); estacion.begin(); obelisco.begin()
        minero.begin(); casco.begin(); cabezaMinero.begin()
        val C = GameSession.CELL
        val m = s.maze

        // --- ecos, cristales y cofres
        for (pk in s.pickups) {
            if (pk.taken) continue
            val x = (pk.gx + 0.5f) * C + pk.flyX
            val z = (pk.gy + 0.5f) * C + pk.flyZ
            if (!near(x, z)) continue
            val fy = WorldMesh.floorHeight(s.maze, pk.gx, pk.gy)
            when (pk.kind) {
                GameSession.PickupKind.ECO ->
                    gem.add(x, fy + 0.62f, z, ESCALA_ECO, 0.98f, 0.78f, 0.36f, 0.55f, pk.bob * 1.4f, pk.bob, 1f, 1f)
                GameSession.PickupKind.ECO_GRANDE ->
                    gem.add(x, fy + 0.72f, z, ESCALA_ECO_GRANDE, 1.0f, 0.86f, 0.42f, 0.85f, pk.bob * 1.1f, pk.bob, 1f, 1f)
                GameSession.PickupKind.VETAGRIS ->
                    gem.add(x, fy + 0.80f, z, ESCALA_VETAGRIS, 0.80f, 0.92f, 1.0f, 1.05f, pk.bob * 0.8f, pk.bob, 1f, 1f)
                GameSession.PickupKind.COFRE ->
                    // Apoyado en el piso: el modelo tiene su base en y=0.
                    cofre.add(x, fy, z, 0.62f, 0.38f, 0.26f, 0.15f, 0.05f, pk.bob * 0.15f, pk.bob, 0f, 1f)
            }
        }

        // --- relleno del suelo y del techo: cada bioma pone lo suyo
        for (gi in s.stalagmites) {
            val gx = gi % m.gw; val gy = gi / m.gw
            val x = (gx + 0.5f) * C; val z = (gy + 0.5f) * C
            if (!near(x, z)) continue
            val h = 0.42f + ((gx * 7 + gy * 13) % 7) * 0.13f
            val t = s.theme
            val fy = WorldMesh.floorHeight(s.maze, gx, gy)
            val giro = ((gx * 31 + gy * 17) % 20) * 0.31f
            when (t.biome) {
                com.mggx.laberinto.maze.Biome.MINA -> {
                    // Durmientes y riel de la vagoneta.
                    shapeSlab?.add(x, fy + 0.05f, z, C * 0.55f, 0.30f, 0.22f, 0.15f, 0.01f,
                        if ((gx + gy) % 2 == 0) 0f else 1.5708f, 0f, 0f, 1f)
                    shapeSlab?.add(x, fy + 0.12f, z, C * 0.30f, 0.36f, 0.30f, 0.26f, 0.02f,
                        if ((gx + gy) % 2 == 0) 1.5708f else 0f, 0f, 0f, 1f)
                }
                com.mggx.laberinto.maze.Biome.RUINAS,
                com.mggx.laberinto.maze.Biome.TEMPLO -> {
                    // Fuste de columna partido, con su basa cuadrada.
                    val alto = 0.55f + ((gx * 5 + gy * 3) % 5) * 0.32f
                    boxS.add(x, fy + 0.07f, z, 0.72f, t.rockR * 1.1f, t.rockG * 1.1f, t.rockB * 1.1f, 0f, giro, 0f, 0f, 1f)
                    cyl.add(x, fy + 0.12f, z, alto * 2.6f, t.rockR * 1.18f, t.rockG * 1.16f, t.rockB * 1.12f, 0f, giro, 0f, 0f, 1f)
                    if (t.biome == com.mggx.laberinto.maze.Biome.TEMPLO) {
                        gem.add(x, fy + 0.12f + alto * 2.6f, z, 0.13f, t.veinR, t.veinG, t.veinB, 0.75f, giro, 0f, 0f, 1f)
                    }
                }
                com.mggx.laberinto.maze.Biome.HONGOS -> {
                    // Hongo gigante: tronco grueso y sombrero que alumbra.
                    val alto = 1.9f + ((gx * 11 + gy * 7) % 6) * 0.32f
                    hongo.add(x, fy, z, alto, 0.60f, 0.94f, 0.70f, 0.55f, giro, 0f, 0f, 1f)
                    // La luz que largan las laminas de abajo del sombrero.
                    gem.add(x, fy + alto * 0.66f, z, 0.14f, 0.60f, 1.0f, 0.75f, 1.05f, giro, gx.toFloat(), 1f, 1f)
                }
                com.mggx.laberinto.maze.Biome.CUEVA -> {
                    if ((gx + gy) % 2 == 0) {
                        cone.add(x, fy, z, h, t.rockR * 1.15f, t.rockG * 1.15f, t.rockB * 1.15f, 0f, giro, 0f, 0f, 1f)
                    } else {
                        stalactite.add(x, WorldMesh.ceilHeight(s.maze, gx, gy), z, h * 0.78f,
                            t.rockR * 0.95f, t.rockG * 0.95f, t.rockB * 0.95f, 0f,
                            ((gx * 13 + gy * 29) % 20) * 0.31f, 0f, 0f, 1f)
                    }
                }
            }
        }

        // --- antorchas de pared
        for (gi in s.torches) {
            val gx = gi % m.gw; val gy = gi / m.gw
            val x = (gx + 0.5f) * C; val z = (gy + 0.5f) * C
            if (!near(x, z)) continue
            // Se pega a la pared mas cercana
            var ox = 0f; var oz = 0f
            if (m.isSolid(gx - 1, gy)) ox = -C * SEPARACION_ANTORCHA
            else if (m.isSolid(gx + 1, gy)) ox = C * SEPARACION_ANTORCHA
            else if (m.isSolid(gx, gy - 1)) oz = -C * SEPARACION_ANTORCHA
            else if (m.isSolid(gx, gy + 1)) oz = C * SEPARACION_ANTORCHA
            val tx = x + ox; val tz = z + oz
            val baseY = WorldMesh.floorHeight(s.maze, gx, gy) + ALTURA_ANTORCHA
            // El brazo del soporte esta modelado hacia -Z, asi que se gira para
            // que se clave en la pared contra la que quedo apoyada.
            val haciaPared = Math.atan2(ox.toDouble(), oz.toDouble()).toFloat() + Math.PI.toFloat()
            antorcha.add(
                tx, baseY, tz, ALTO_ANTORCHA,
                0.28f, 0.19f, 0.12f, 0.02f, haciaPared, 0f, 0f, 1f
            )
            val ph = ((gx * 41 + gy * 7) % 30) * 0.21f
            val fl = 0.86f + 0.14f * sin((time * 7f + ph).toDouble()).toFloat()
            // La llama tiene la base en y=0, asi que se apoya justo en la boca
            // del cuenco y no puede hundirse en el poste. Va con tipo 0 (quieta)
            // a proposito: el tipo 1 la hacia flotar 11cm y se despegaba.
            llama.add(
                tx, baseY + ALTO_ANTORCHA * StructureMeshes.ALTURA_DEL_FUEGO, tz, ESCALA_LLAMA * fl,
                1.0f, 0.62f, 0.22f, 1.35f, ph, ph, 0f, 1f
            )
        }

        // --- ambientacion: cristales, rocas, hongos y marcos de madera
        val th = s.theme
        for (gi in s.crystalClusters) {
            val gx = gi % m.gw; val gy = gi / m.gw
            val x = (gx + 0.5f) * C; val z = (gy + 0.5f) * C
            if (!near(x, z)) continue
            val fy = WorldMesh.floorHeight(s.maze, gx, gy)
            val h = ((gx * 17 + gy * 5) % 11) * 0.031f
            val ph = ((gx * 23 + gy * 11) % 40) * 0.157f
            val latido = 0.72f + 0.28f * sin((time * 0.9f + ph).toDouble()).toFloat()
            // Un racimo: uno grande y dos chicos apoyados al costado. Cada uno
            // con otro giro, y como el cristal viene inclinado en la malla, eso
            // solo alcanza para que caigan cada uno para su lado.
            cristal.add(x, fy, z, 0.62f + h * 1.6f, th.veinR, th.veinG, th.veinB, 0.95f * latido, ph, ph, 0f, 1f)
            cristal.add(x + 0.30f, fy, z - 0.16f, 0.38f, th.veinR, th.veinG, th.veinB, 0.70f * latido, ph + 1.9f, ph, 0f, 1f)
            cristal.add(x - 0.24f, fy, z + 0.22f, 0.29f, th.veinR, th.veinG, th.veinB, 0.60f * latido, ph + 4.1f, ph, 0f, 1f)
        }
        for (gi in s.rocks) {
            val gx = gi % m.gw; val gy = gi / m.gw
            val x = (gx + 0.5f) * C; val z = (gy + 0.5f) * C
            if (!near(x, z)) continue
            val fy = WorldMesh.floorHeight(s.maze, gx, gy)
            val r = ((gx * 13 + gy * 29) % 20) * 0.31f
            val e = ((gx * 7 + gy * 3) % 5) * 0.045f
            boulder.add(x + 0.24f, fy + 0.19f + e, z - 0.18f, 0.33f + e,
                th.rockR * 0.92f, th.rockG * 0.92f, th.rockB * 0.92f, 0f, r, 0f, 0f, 1f)
            boulder.add(x - 0.30f, fy + 0.13f, z + 0.26f, 0.22f,
                th.rockR * 1.06f, th.rockG * 1.06f, th.rockB * 1.06f, 0f, r + 1.7f, 0f, 0f, 1f)
        }
        for (gi in s.mushrooms) {
            val gx = gi % m.gw; val gy = gi / m.gw
            val x = (gx + 0.5f) * C; val z = (gy + 0.5f) * C
            if (!near(x, z)) continue
            val fy = WorldMesh.floorHeight(s.maze, gx, gy)
            // Tres hongos de distinto porte, siempre pegados a un costado.
            for (k in 0 until 3) {
                val ang = ((gx * 5 + gy * 11 + k * 7) % 20) * 0.314f
                val rr = 0.34f + k * 0.16f
                val hx = x + cos(ang.toDouble()).toFloat() * rr
                val hz = z + sin(ang.toDouble()).toFloat() * rr
                val porte = if (th.biome == com.mggx.laberinto.maze.Biome.HONGOS) 2.2f else 1f
                val alto = (0.36f + ((gx + gy + k) % 4) * 0.11f) * porte
                hongo.add(hx, fy, hz, alto, 0.56f, 0.93f, 0.70f, 0.42f, ang, 0f, 0f, 1f)
            }
        }
        for (gi in s.beams) {
            val gx = gi % m.gw; val gy = gi / m.gw
            val x = (gx + 0.5f) * C; val z = (gy + 0.5f) * C
            if (!near(x, z)) continue
            val fy = WorldMesh.floorHeight(s.maze, gx, gy)
            val alto = (m.ceilY(gx, gy) - fy).coerceIn(1.4f, 3.4f)
            // El marco cruza el pasillo, asi que se orienta segun por donde se pasa.
            val horizontal = m.isSolid(gx, gy - 1)
            val ang = if (horizontal) 1.5708f else 0f
            val dx = if (horizontal) 0f else C * 0.34f
            val dz = if (horizontal) C * 0.34f else 0f
            val madera = floatArrayOf(0.34f, 0.24f, 0.15f)
            post.add(x - dx, fy, z - dz, alto, madera[0], madera[1], madera[2], 0.02f, 0f, 0f, 0f, 1f)
            post.add(x + dx, fy, z + dz, alto, madera[0], madera[1], madera[2], 0.02f, 0f, 0f, 0f, 1f)
            slab.add(x, fy + alto - 0.06f, z, C * 0.78f, madera[0] * 1.1f, madera[1] * 1.1f, madera[2] * 1.1f,
                0.02f, ang, 0f, 0f, 1f)
        }

        // --- estaciones de carburo (recargan la linterna)
        for (gi in s.carbideStations) {
            val gx = gi % m.gw; val gy = gi / m.gw
            val x = (gx + 0.5f) * C; val z = (gy + 0.5f) * C
            if (!near(x, z)) continue
            val fy = WorldMesh.floorHeight(s.maze, gx, gy)
            val usada = s.estacionUsada(gi)
            val brillo = if (usada) 0.06f else 1.15f + 0.35f * sin((time * 2.2f + gx).toDouble()).toFloat()
            // Poste de hierro con el bidon y el piloto encendido.
            estacion.add(x, fy, z, 1.10f, 0.34f, 0.33f, 0.35f, 0.02f, 0f, 0f, 0f, 1f)
            gem.add(
                x, fy + 1.16f, z, 0.13f,
                if (usada) 0.45f else 1.0f, if (usada) 0.48f else 0.86f, if (usada) 0.5f else 0.40f,
                brillo, time * 0.9f, gx.toFloat(), 1f, 1f
            )
        }

        // --- trampas: cada una con su forma, no un cuadrado rojo
        //
        // Se dibujan siempre que esten cerca, no solo cuando estan
        // "descubiertas": son objetos fisicos de la cueva (pinches asomando,
        // tablas podridas, rocas colgando) y esconderlos hasta pisarlos era
        // lo que las hacia aparecer de la nada. Lo que agrega descubrirla es
        // el aviso que late, que es la ayuda de verdad.
        for (tr in s.traps) {
            val x = (tr.gx + 0.5f) * C; val z = (tr.gy + 0.5f) * C
            if (!near(x, z)) continue
            val fy = WorldMesh.floorHeight(s.maze, tr.gx, tr.gy)
            val pulse = 0.55f + 0.45f * sin((time * 3.4f + tr.gx + tr.gy).toDouble()).toFloat()
            val aviso = if (tr.armed && tr.revealed) 0.22f * pulse else 0.02f
            when (tr.kind) {
                com.mggx.laberinto.maze.MazeGenerator.TrapKind.SPIKES -> {
                    // Corona de pinches de hierro asomando del piso.
                    for (k in 0 until 7) {
                        val a = k * 0.8976f + tr.gx
                        val rr = if (k == 0) 0f else 0.52f
                        pincho.add(
                            x + cos(a.toDouble()).toFloat() * rr, fy, z + sin(a.toDouble()).toFloat() * rr,
                            0.40f + (k % 3) * 0.09f,
                            0.62f, 0.58f, 0.55f, aviso, a, 0f, 0f, 1f
                        )
                    }
                    slab.add(x, fy + 0.03f, z, 1.5f, 0.30f, 0.26f, 0.23f, aviso, 0f, 0f, 0f, 1f)
                    slab.add(x, fy + 0.03f, z, 1.5f, 0.30f, 0.26f, 0.23f, aviso, 1.5708f, 0f, 0f, 1f)
                }
                com.mggx.laberinto.maze.MazeGenerator.TrapKind.PITFALL -> {
                    // Boca de pozo con las tablas podridas partidas al medio.
                    boxS.add(x, fy - 0.28f, z, 1.35f, 0.03f, 0.03f, 0.04f, 0f, 0f, 0f, 0f, 1f)
                    slab.add(x - 0.42f, fy + 0.04f, z, 1.30f, 0.30f, 0.21f, 0.13f, aviso, 0.25f, 0f, 0f, 1f)
                    slab.add(x + 0.46f, fy + 0.04f, z, 1.10f, 0.28f, 0.19f, 0.12f, aviso, -0.3f, 0f, 0f, 1f)
                    slab.add(x, fy + 0.04f, z + 0.5f, 1.20f, 0.32f, 0.22f, 0.14f, aviso, 1.5708f, 0f, 0f, 1f)
                }
                com.mggx.laberinto.maze.MazeGenerator.TrapKind.STEAM -> {
                    // Fisura con la valvula y el vapor saliendo a chorros.
                    cyl.add(x, fy, z, 0.42f, 0.44f, 0.40f, 0.36f, 0.02f, 0f, 0f, 0f, 1f)
                    boxS.add(x, fy + 0.05f, z, 0.72f, 0.26f, 0.24f, 0.22f, aviso, 0f, 0f, 0f, 1f)
                    for (k in 0 until 4) {
                        val t2 = ((time * 0.8f + k * 0.25f) % 1f)
                        gem.add(
                            x, fy + 0.42f + t2 * 1.5f, z, 0.10f + t2 * 0.26f,
                            0.86f, 0.90f, 0.94f, (1f - t2) * 0.55f,
                            t2 * 5f, k.toFloat(), 0f, (1f - t2) * 0.7f
                        )
                    }
                }
            }
        }

        // --- bichos
        for (e in s.enemies) {
            // Al que volteaste ya no se lo dibuja: si quedara en pantalla, el
            // jugador seguiria esquivando un cadaver.
            if (!e.vivo) continue
            if (!near(e.x, e.z)) continue
            val ex2 = e.x; val ez2 = e.z; val ey = e.altura
            val r = e.rumbo
            val fwdX = sin(r.toDouble()).toFloat(); val fwdZ = cos(r.toDouble()).toFloat()
            val rgtX = cos(r.toDouble()).toFloat(); val rgtZ = -sin(r.toDouble()).toFloat()
            // Los ojos se prenden cuando te vieron.
            val ojo = if (e.alerta) 1.45f else 0.30f
            // Destello rojo del golpe recibido: es lo que hace que se sienta
            // que le pegaste, y ademas avisa cuando le queda poca vida.
            val golpe = (e.destello / 0.28f).coerceIn(0f, 1f)
            val herido = 1f - (e.vida / e.kind.vida).coerceIn(0f, 1f)
            val flash = golpe * 1.9f + herido * 0.22f
            when (e.kind) {
                com.mggx.laberinto.maze.MazeGenerator.EnemyKind.MURCIELAGO -> {
                    // Cuerpo de una sola pieza (con hocico y orejas) mirando al
                    // frente, y las dos alas colgadas de los hombros. La del
                    // lado izquierdo es la misma malla girada media vuelta, con
                    // el aleteo desfasado para que no batan como un solo panel.
                    murcielago.add(ex2, ey, ez2, 0.40f,
                        0.21f + golpe * 0.7f, 0.16f, 0.19f, flash, r, e.fase, 0f, 1f)
                    ala.add(ex2 + rgtX * 0.07f, ey + 0.05f, ez2 + rgtZ * 0.07f, 0.44f,
                        0.28f + golpe * 0.6f, 0.20f, 0.23f, flash, r, e.fase, 2f, 1f)
                    ala.add(ex2 - rgtX * 0.07f, ey + 0.05f, ez2 - rgtZ * 0.07f, 0.44f,
                        0.28f + golpe * 0.6f, 0.20f, 0.23f, flash,
                        r + Math.PI.toFloat(), e.fase + 3.14f, 2f, 1f)
                    gem.add(ex2 + fwdX * 0.15f - rgtX * 0.05f, ey + 0.06f, ez2 + fwdZ * 0.15f - rgtZ * 0.05f,
                        0.030f, 1f, 0.42f, 0.30f, ojo, 0f, 0f, 0f, 1f)
                    gem.add(ex2 + fwdX * 0.15f + rgtX * 0.05f, ey + 0.06f, ez2 + fwdZ * 0.15f + rgtZ * 0.05f,
                        0.030f, 1f, 0.42f, 0.30f, ojo, 0f, 0f, 0f, 1f)
                }
                com.mggx.laberinto.maze.MazeGenerator.EnemyKind.TOPO -> {
                    // Va pegado al piso, cabeceando como el que viene cavando,
                    // con las dos palas moviendose alternadas.
                    val cabeceo = sin((e.paso * 5.5f + e.fase).toDouble()).toFloat()
                    topo.add(
                        ex2, ey + 0.20f + cabeceo * 0.03f, ez2, 0.62f,
                        0.30f + golpe * 0.7f, 0.24f, 0.21f, flash,
                        r, e.fase, 0f, 1f
                    )
                    for (lado in 0 until 2) {
                        val s = if (lado == 0) 1f else -1f
                        val bat = sin((e.paso * 5.5f + e.fase + lado * 3.14f).toDouble()).toFloat()
                        pala.add(
                            ex2 + rgtX * 0.17f * s + fwdX * 0.16f,
                            ey + 0.13f + bat * 0.05f,
                            ez2 + rgtZ * 0.17f * s + fwdZ * 0.16f,
                            0.34f,
                            0.42f + golpe * 0.6f, 0.35f, 0.30f, flash,
                            if (lado == 0) r else r + Math.PI.toFloat(), e.fase, 0f, 1f
                        )
                    }
                    // No tiene ojos utiles: lo que se le ve es la nariz humeda.
                    gem.add(
                        ex2 + fwdX * 0.30f, ey + 0.21f, ez2 + fwdZ * 0.30f,
                        0.035f, 1f, 0.62f, 0.58f, ojo * 0.5f, 0f, 0f, 0f, 1f
                    )
                }
                com.mggx.laberinto.maze.MazeGenerator.EnemyKind.ARANA -> {
                    // Cuelga a media altura. Ocho patas quebradas repartidas
                    // alrededor del cefalotorax, las de cada lado giradas media
                    // vuelta para que la rodilla apunte siempre para afuera.
                    arana.add(
                        ex2, ey, ez2, 0.70f,
                        0.26f + golpe * 0.7f, 0.22f, 0.28f, flash, r, e.fase, 0f, 1f
                    )
                    for (k in 0 until 8) {
                        val lado = if (k % 2 == 0) -1f else 1f
                        val a = 0.26f - (k / 2) * 0.19f
                        val mueve = sin((e.paso * 4.2f + e.fase + k * 0.9f).toDouble()).toFloat()
                        pata.add(
                            ex2 + fwdX * a + rgtX * 0.18f * lado,
                            ey - 0.16f + mueve * 0.04f,
                            ez2 + fwdZ * a + rgtZ * 0.18f * lado,
                            0.40f, 0.30f, 0.26f, 0.32f, flash,
                            r + (if (lado > 0f) 0f else Math.PI.toFloat()), e.fase + k * 0.5f, 0f, 1f
                        )
                    }
                    // Los ojos en fila: es lo unico que se le ve de lejos.
                    for (k in 0 until 3) {
                        val off = (k - 1) * 0.07f
                        gem.add(
                            ex2 + fwdX * 0.26f + rgtX * off, ey + 0.06f,
                            ez2 + fwdZ * 0.26f + rgtZ * off,
                            0.028f, 1f, 0.86f, 0.42f, ojo, 0f, 0f, 0f, 1f
                        )
                    }
                }
                com.mggx.laberinto.maze.MazeGenerator.EnemyKind.RASTRERO -> {
                    // Cuerpo largo de tres placas de caparazon que ondulan al
                    // avanzar, cada vez mas chicas hacia la cola.
                    for (k in 0 until 3) {
                        val off = 0.30f - k * 0.30f
                        rastrero.add(
                            ex2 + fwdX * off, ey + 0.20f - k * 0.03f, ez2 + fwdZ * off,
                            0.46f - k * 0.09f,
                            0.74f + golpe * 0.26f, 0.71f - golpe * 0.3f, 0.62f - golpe * 0.3f,
                            flash, r, e.fase + k * 0.8f, 3f, 1f
                        )
                    }
                    // Cuatro patas quebradas. Las de un lado van giradas media
                    // vuelta para que la rodilla apunte para afuera en los dos.
                    for (k in 0 until 4) {
                        val a = if (k < 2) 0.22f else -0.16f
                        val lado = if (k % 2 == 0) -1f else 1f
                        pata.add(
                            ex2 + fwdX * a + rgtX * 0.13f * lado, ey,
                            ez2 + fwdZ * a + rgtZ * 0.13f * lado,
                            0.34f, 0.60f, 0.58f, 0.50f, 0f,
                            r + (if (lado > 0f) 0f else Math.PI.toFloat()), e.fase, 0f, 1f
                        )
                    }
                    // Es ciego: en vez de ojos tiene dos antenas que tantean.
                    gem.add(ex2 + fwdX * 0.44f - rgtX * 0.08f, ey + 0.30f, ez2 + fwdZ * 0.44f - rgtZ * 0.08f,
                        0.05f, 0.95f, 0.86f, 0.52f, ojo * 0.6f, 0f, 0f, 0f, 1f)
                    gem.add(ex2 + fwdX * 0.44f + rgtX * 0.08f, ey + 0.30f, ez2 + fwdZ * 0.44f + rgtZ * 0.08f,
                        0.05f, 0.95f, 0.86f, 0.52f, ojo * 0.6f, 0f, 0f, 0f, 1f)
                }
                com.mggx.laberinto.maze.MazeGenerator.EnemyKind.GUARDIAN -> {
                    // Torso tallado, cabeza hundida entre los hombros y dos
                    // brazos de roca colgando: se lee como un cuerpo y no como
                    // tres piedras apiladas.
                    torso.add(ex2, ey + 0.62f, ez2, 1.05f,
                        th.rockR * 0.8f + golpe * 0.5f, th.rockG * 0.8f, th.rockB * 0.82f, flash, r, 0f, 0f, 1f)
                    cabeza.add(ex2, ey + 1.18f, ez2, 0.50f,
                        th.rockR * 0.88f + golpe * 0.5f, th.rockG * 0.88f, th.rockB * 0.90f, flash, r, 0f, 0f, 1f)
                    brazo.add(ex2 - rgtX * 0.44f, ey + 0.66f, ez2 - rgtZ * 0.44f, 0.72f,
                        th.rockR * 0.75f + golpe * 0.4f, th.rockG * 0.75f, th.rockB * 0.78f, flash, r, 0f, 0f, 1f)
                    brazo.add(ex2 + rgtX * 0.44f, ey + 0.66f, ez2 + rgtZ * 0.44f, 0.72f,
                        th.rockR * 0.75f + golpe * 0.4f, th.rockG * 0.75f, th.rockB * 0.78f, flash, r, 0f, 0f, 1f)
                    gem.add(ex2 + fwdX * 0.22f - rgtX * 0.10f, ey + 1.20f, ez2 + fwdZ * 0.22f - rgtZ * 0.10f,
                        0.055f, 1f, 0.62f, 0.22f, ojo, 0f, 0f, 0f, 1f)
                    gem.add(ex2 + fwdX * 0.22f + rgtX * 0.10f, ey + 1.20f, ez2 + fwdZ * 0.22f + rgtZ * 0.10f,
                        0.055f, 1f, 0.62f, 0.22f, ojo, 0f, 0f, 0f, 1f)
                }
            }
        }

        // --- salida: columna de cristal que se ve de lejos
        run {
            val ex = s.exitWorldX; val ez = s.exitWorldZ
            val fy = WorldMesh.floorHeight(s.maze, s.maze.exitGx, s.maze.exitGy)
            val d = hypot(ex - px, ez - pz)
            val visible = d < cull * 1.9f
            if (visible) {
                val ping = if (s.exitPingFlash > 0f) 0.6f else 0f
                obelisco.add(ex, fy, ez, 2.55f, 0.55f, 0.92f, 0.78f, 0.85f + ping, 0f, 0f, 0f, 1f)
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

        // --- los companieros de sala
        //
        // Se los dibuja en la posicion SUAVIZADA (dibX/dibZ), no en la que
        // llego: las poses vienen 10 veces por segundo y la pantalla dibuja
        // 60, asi que con la posicion cruda se los veria teletransportarse.
        s.red?.let { red ->
            for (j in red.match.otros()) {
                if (j.sinPose) continue
                if (!near(j.dibX, j.dibZ)) continue

                // Alto segun como venga: parado, agachado o arrastrandose. El
                // caido va bien bajo, para que se lea de una que esta en el
                // piso esperando que lo levanten.
                val alto = when {
                    j.caido -> 0.34f
                    j.postura == 2 -> 0.80f
                    j.postura == 1 -> 1.15f
                    else -> 1.72f
                }
                val traje = tinteDeSkin(j.skin)
                // Al caido se le apaga el color: esta tirado, no laburando.
                val apagado = if (j.caido) 0.45f else 1f
                // El giro va en RADIANES: es lo que espera el shader, igual
                // que el rumbo de los bichos. El yaw del jugador viene en
                // grados, asi que se convierte una vez y se usa para todo.
                val yr = Math.toRadians(j.dibYaw.toDouble())
                val giro = yr.toFloat()
                // Caminata: el reloj son los metros que lleva caminados, no el
                // tiempo, asi que las piernas se mueven cuando avanza y se
                // quedan quietas cuando esta parado. El caido no camina.
                val anda = if (j.caido) 0f else 4f
                minero.add(
                    j.dibX, j.dibY, j.dibZ, alto,
                    traje[0] * apagado, traje[1] * apagado, traje[2] * apagado,
                    0f, giro, j.paso, anda, 1f
                )
                // El casco y la lampara acompanan el balanceo del cuerpo: el
                // shader mueve la malla del minero, pero estas son instancias
                // aparte y hay que subirlas y bajarlas desde aca.
                val balanceo = if (j.caido) 0f
                else kotlin.math.abs(sin((j.paso * 3.4f + giro).toDouble()).toFloat()) * 0.018f * alto
                // La cabeza va en instancia aparte para poder pintarla con
                // tono de piel: con una sola instancia por companiero, la cara
                // saldria del color del abrigo y el minero se veria como un
                // traje vacio con un casco encima.
                cabezaMinero.add(
                    j.dibX, j.dibY + alto * 0.845f + balanceo, j.dibZ, alto * 0.150f,
                    0.66f * apagado, 0.48f * apagado, 0.36f * apagado, 0f,
                    giro, 0f, 0f, 1f
                )
                // El casco: a 0.105 del alto del cuerpo, o sea unos 18 cm en un
                // minero de 1,72 m. Estaba en 0.30, que son 52 cm: se tragaba
                // la cabeza entera y de lejos el companiero era un hongo.
                casco.add(
                    j.dibX, j.dibY + alto * 0.945f + balanceo, j.dibZ, alto * 0.105f,
                    1.00f, 0.74f, 0.16f, if (j.caido) 0.10f else 0.35f,
                    giro, 0f, 0f, 1f
                )
                // Su lamparita de casco: ademas de ubicarlo en un pasillo
                // oscuro, dice para donde esta mirando.
                if (!j.caido) {
                    gem.add(
                        j.dibX + sin(yr).toFloat() * 0.20f,
                        j.dibY + alto * 0.975f + balanceo,
                        j.dibZ + cos(yr).toFloat() * 0.20f,
                        0.07f, 1f, 0.90f, 0.55f, 1.7f, 0f, 0f, 0f, 1f
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
                ax, s.alturaCamara() - 0.40f, az, 0.22f,
                1.0f, 0.84f, 0.36f, 1.05f,
                ang, 0f, 0f, 1f
            )
        }

        // ------------------------------------------------------- draw
        val p = propProg
        GLES30.glUseProgram(p)
        GLES30.glUniformMatrix4fv(u(p, "uViewProj"), 1, false, viewProj, 0)
        GLES30.glUniform1f(u(p, "uTime"), time)
        GLES30.glUniform3f(u(p, "uCamPos"), px, s.alturaCamara(), pz)
        GLES30.glUniform3f(u(p, "uLightColor"), lr, lg, lb)
        GLES30.glUniform1f(u(p, "uLightRadius"), radius)
        GLES30.glUniform1f(u(p, "uLightIntensity"), LUZ_JUGADOR * flicker)
        val t = s.theme
        GLES30.glUniform3f(
            u(p, "uAmbient"),
            (t.ambientR + ambBoost) * AMBIENTE,
            (t.ambientG + ambBoost) * AMBIENTE,
            (t.ambientB + ambBoost) * AMBIENTE
        )
        GLES30.glUniform3f(u(p, "uFogColor"), t.fogR, t.fogG, t.fogB)
        GLES30.glUniform1f(u(p, "uFogDensity"), fogDensity)
        GLES30.glUniform1f(u(p, "uBrightness"), brightness)

        val fuerza = s.linternaFuerza()
        val spotDir = u(p, "uSpotDir")
        GLES30.glUniform3f(spotDir, camDirX, camDirY, camDirZ)
        GLES30.glUniform1f(u(p, "uSpotPower"), fuerza * 2.6f)
        GLES30.glUniform1f(u(p, "uSpotRange"), radius * 2.6f)
        GLES30.glUniform1f(u(p, "uSpotCos"), 0.90f)
        subirLuces(p)

        GLES30.glUniform1i(u(p, "uQuality"), save.settings.quality)
        val material = u(p, "uMaterial")
        GLES30.glUniform1i(material, 0)
        cone.draw(); stalactite.draw(); boulder.draw(); torso.draw(); cabeza.draw(); brazo.draw()
        GLES30.glUniform1i(material, 1)
        boxS.draw(); slab.draw(); post.draw(); cofre.draw()
        GLES30.glUniform1i(material, 2)
        cyl.draw(); arrow.draw(); antorcha.draw(); pincho.draw(); estacion.draw(); casco.draw()
        topo.draw(); pala.draw(); arana.draw()
        GLES30.glUniform1i(material, 3)
        gem.draw(); cristal.draw(); obelisco.draw()
        GLES30.glUniform1i(material, 4)
        ala.draw(); murcielago.draw(); rastrero.draw(); pata.draw(); hongo.draw()
        GLES30.glUniform1i(material, 5)
        minero.draw(); cabezaMinero.draw()
        GLES30.glUniform1i(material, 6)
        llama.draw()
    }

    /**
     * Color del traje de una skin, para pintar a un companiero de sala.
     *
     * Sale del mismo catalogo con el que se pinta uno a si mismo (ver
     * PlayerStats.suitTint), asi el que se compro el traje de Vetagris lo ve
     * puesto tambien el de al lado. Si la skin no existe (un cliente mas
     * nuevo, un mensaje raro), se cae al traje del minero de turno.
     */
    private fun tinteDeSkin(skin: String): FloatArray {
        val color = com.mggx.laberinto.game.ItemCatalog.get(skin)?.effect?.color2 ?: 0xFF60422AL
        // Pasa por sRgbLineal como todos los colores del juego. Sin esa
        // conversion el mismo traje se veia como tres veces mas claro en el
        // companiero que en las manos de uno.
        return sRgbLineal(color)
    }

    private fun drawDecals(s: GameSession, fogDensity: Float, brightness: Float) {
        decalData.clear()
        var quads = 0
        val threadOn = s.effects.isActive(com.mggx.laberinto.game.EffectType.HILO_ARIADNA)

        fun quad(x: Float, z: Float, size: Float, r: Float, g: Float, b: Float, a: Float) {
            if (quads >= decalCapacity) return
            // Apoyado en la altura REAL del piso (con su abolladura de
            // ruido), no en el plano teorico: si no, en buena parte de cada
            // casilla la marca queda tapada por la roca (hasta 13cm de bulto).
            val y = WorldMesh.realFloorHeight(s.maze, x, z) + 0.02f
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
            val alpha = if (threadOn) 0.55f else (tp.life / s.stats.trailSeconds).coerceIn(0f, 1f) * 0.32f
            if (threadOn) quad(tp.x, tp.z, 1.05f, 0.45f, 0.95f, 0.75f, alpha)
            else quad(tp.x, tp.z, 0.72f, 0.72f, 0.66f, 0.52f, alpha)
        }
        if (quads == 0) return

        val p = decalProg
        GLES30.glUseProgram(p)
        GLES30.glUniformMatrix4fv(u(p, "uViewProj"), 1, false, viewProj, 0)
        GLES30.glUniform3f(u(p, "uCamPos"), s.posX, s.alturaCamara(), s.posZ)
        GLES30.glUniform1f(u(p, "uFogDensity"), fogDensity)
        GLES30.glUniform1f(u(p, "uBrightness"), brightness)

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
        // El arma va adentro de la misma malla que los brazos, asi que si
        // cambiaste de arma hay que rehacerla. Pasa al equipar, no por cuadro.
        // La malla de los brazos se rehace cuando cambia el arma O el objeto
        // elegido. No es por cuadro: pasa cuando el jugador toca la barra.
        if (save.armaEquipada != armaEnMalla || input.objetoEnMano != objetoEnMalla) {
            subirMallaDeBrazos(save.armaEquipada, input.objetoEnMano)
        }
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
        // El swing: sale rapido y vuelve despacio, que es como se siente un
        // golpe de verdad. golpeAnim va de 1 a 0 mientras dura.
        val g = s.golpeAnim
        val swing = if (g <= 0f) 0f else {
            val t = 1f - g                       // 0 al empezar, 1 al terminar
            if (t < 0.35f) t / 0.35f else (1f - (t - 0.35f) / 0.65f)
        }

        fun armMatrix(out: FloatArray, side: Float) {
            // Solo el brazo derecho pega; el izquierdo acompana apenas.
            val mio = if (side > 0f) swing else swing * 0.22f
            val pose = BrazoAnim.pose(side, mio, sway, bob, breathe)
            Matrix.setIdentityM(out, 0)
            Matrix.translateM(out, 0, pose.tx, pose.ty, pose.tz)
            // El brazo entra desde la esquina de abajo: se abre hacia afuera con
            // el giro en Y y baja apenas con el de X, para que se vea el dorso
            // de la mano y los dedos sin mirarlos de punta. Esta es la
            // orientacion de REPOSO: sigue pivotando cerca de la muneca (el
            // origen de la malla), que es donde se ve bien.
            Matrix.rotateM(out, 0, pose.rotYReposo, 0f, 1f, 0f)
            Matrix.rotateM(out, 0, pose.rotXReposo, 1f, 0f, 0f)
            Matrix.rotateM(out, 0, pose.rotZReposo, 0f, 0f, 1f)
            // El giro EXTRA del golpe pivotea en el codo (mucho mas lejos del
            // origen), no en la muneca: si rotara ahi, todo el antebrazo
            // describiria un arco de medio metro para llegar al mismo angulo.
            if (mio != 0f) {
                Matrix.translateM(out, 0, 0f, 0f, BrazoAnim.PIVOTE_CODO_Z)
                Matrix.rotateM(out, 0, pose.rotYSwing, 0f, 1f, 0f)
                Matrix.rotateM(out, 0, pose.rotXSwing, 1f, 0f, 0f)
                Matrix.rotateM(out, 0, pose.rotZSwing, 0f, 0f, 1f)
                Matrix.translateM(out, 0, 0f, 0f, -BrazoAnim.PIVOTE_CODO_Z)
            }
            Matrix.scaleM(out, 0, pose.escala, pose.escala, pose.escala)
        }
        armMatrix(armMatL, -1f)
        armMatrix(armMatR, 1f)

        val p = armsProg
        GLES30.glUseProgram(p)
        GLES30.glUniformMatrix4fv(u(p, "uProj"), 1, false, armProj, 0)
        GLES30.glUniformMatrix4fv(u(p, "uArmL"), 1, false, armMatL, 0)
        GLES30.glUniformMatrix4fv(u(p, "uArmR"), 1, false, armMatR, 0)
        GLES30.glUniform3f(u(p, "uLightColor"), lr, lg, lb)
        GLES30.glUniform1f(u(p, "uLightIntensity"), 1.15f * flicker)
        val t = s.theme
        GLES30.glUniform3f(
            u(p, "uAmbient"),
            (t.ambientR + ambBoost) * AMBIENTE,
            (t.ambientG + ambBoost) * AMBIENTE,
            (t.ambientB + ambBoost) * AMBIENTE
        )
        // El guante decide el DETALLE (malla, ceniza, gema...) y la skin decide
        // los colores de la piel y del traje. Son dos cosmeticos distintos y se
        // combinan libremente.
        val style = s.stats.gloveStyle
        val skin = sRgbLineal(s.stats.skinTint)
        val cloth = sRgbLineal(s.stats.suitTint)
        GLES30.glUniform3f(u(p, "uSkin"), skin[0], skin[1], skin[2])
        GLES30.glUniform3f(u(p, "uCloth"), cloth[0], cloth[1], cloth[2])
        GLES30.glUniform1i(u(p, "uSkinStyle"), s.stats.skinStyle)
        GLES30.glUniform1f(u(p, "uBrightness"), brightness)
        GLES30.glUniform1f(u(p, "uTime"), time)
        GLES30.glUniform1i(u(p, "uStyle"), style)
        val objeto = ArmsMesh.Objeto.por(input.objetoEnMano)
        val co = colorDeObjeto(objeto)
        GLES30.glUniform3f(u(p, "uObjeto"), co[0], co[1], co[2])
        GLES30.glUniform1i(u(p, "uObjetoTipo"), objeto?.ordinal ?: 0)
        val arma = colorDeArma(save.armaEquipada)
        GLES30.glUniform3f(
            u(p, "uArma"), arma[0], arma[1], arma[2]
        )

        GLES30.glBindVertexArray(armsVao[0])
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, armsIndexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun drawOverlay(s: GameSession, theme: CaveTheme, brightness: Float) {
        val p = overlayProg
        GLES30.glUseProgram(p)
        GLES30.glUniform1f(u(p, "uStrength"), 0.62f)
        GLES30.glUniform3f(u(p, "uTintColor"), theme.fogR, theme.fogG, theme.fogB)
        GLES30.glUniform1f(u(p, "uTintAmount"), 0.30f)
        val lowHp = if (s.healthFraction() < 0.28f) (0.28f - s.healthFraction()) * 1.7f else 0f
        GLES30.glUniform1f(u(p, "uHurt"), max(hurtFlash * 0.55f, lowHp))

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

