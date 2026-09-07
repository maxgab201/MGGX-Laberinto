package com.mggx.laberinto.game

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.maze.CaveTheme
import com.mggx.laberinto.maze.Maze
import com.mggx.laberinto.maze.MazeGenerator
import com.mggx.laberinto.net.MatchLink
import com.mggx.laberinto.net.NetProtocol
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Estado completo de una partida. No sabe nada de OpenGL ni de Compose:
 * el renderer lee de aca y la UI tambien. Asi se puede testear en JVM.
 */
class GameSession(
    val save: SaveData,
    val level: Int,
    seed: Long = seedForLevel(level)
) {
    companion object {
        /**
         * Semilla fija por nivel: el nivel 7 es siempre el mismo laberinto, con
         * los mismos ecos y las mismas trampas. Si cambiara en cada intento,
         * "Reintentar" no seria reintentar el nivel sino jugar otro distinto.
         */
        fun seedForLevel(level: Int): Long {
            var z = level.toLong() * -0x61c8864680b583ebL + 0x9e3779b97f4a7c15uL.toLong()
            z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
            z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
            return z xor (z ushr 31)
        }

        /** Lado de una casilla de la grilla, en metros. */
        const val CELL = 3.0f
        const val WALL_HEIGHT = 3.4f
        const val EYE_HEIGHT = 1.62f
        const val PLAYER_RADIUS = 0.34f
        const val EXIT_RADIUS = 1.25f
        /** Vueltas por segundo del giro con mando, en grados. */
        const val PAD_LOOK_DEG = 165f

        /** Gravedad, en m/s^2. */
        const val GRAVEDAD = -18f
        /**
         * Impulso del salto. Da unos 0,59 m de altura: alcanza para un escalon
         * y medio, y a proposito NO alcanza para saltear una escalera.
         */
        const val VEL_SALTO = 4.6f
        /** Velocidad de subida y de bajada por escalera, en m/s. */
        const val VEL_ESCALERA = 2.4f
        /** Caida a partir de la cual empieza a doler, en m/s. */
        const val CAIDA_SEGURA = 11f
        /** Segundos que dura un tanque lleno de carburo con la linterna prendida. */
        const val DURACION_CARBURO = 150f
        /**
         * Medio angulo del golpe, en coseno. 0.55 son unos 57 grados a cada
         * lado: hay que apuntarle al bicho, pero no al pixel.
         */
        const val COS_CONO_GOLPE = 0.55f
        /** Cuanto dura la animacion del swing, en segundos. */
        const val DURACION_SWING = 0.26f
    }

    enum class Phase { JUGANDO, PAUSA, GANADO, PERDIDO }

    enum class PickupKind { ECO, ECO_GRANDE, VETAGRIS, COFRE }

    class Pickup(
        val gx: Int, val gy: Int,
        val kind: PickupKind,
        var taken: Boolean = false,
        var bob: Float = 0f,
        /** Cuando el iman la atrae, va volando hacia el jugador. */
        var flyX: Float = 0f, var flyZ: Float = 0f, var flying: Boolean = false
    )

    class TrapInstance(
        val gx: Int, val gy: Int,
        val kind: MazeGenerator.TrapKind,
        var armed: Boolean = true,
        var cooldown: Float = 0f,
        var revealed: Boolean = false
    )

    class Mark(val x: Float, val z: Float, val colorIndex: Int)

    class TrailPoint(val x: Float, val z: Float, var life: Float)

    // ------------------------------------------------------------- mundo
    val blueprint = MazeGenerator.generate(level, seed)
    val maze: Maze = blueprint.maze
    val theme: CaveTheme = blueprint.theme
    val stats = PlayerStats(save)
    val effects = ActiveEffects()
    private val rnd = Random(seed xor 0x9E3779B9L)

    /** Se pone en true cuando cambia la geometria (pico) y hay que rehacer la malla. */
    @Volatile var geometryDirty: Boolean = false

    // ------------------------------------------------------------ jugador
    var posX: Float = (maze.startGx + 0.5f) * CELL
    var posZ: Float = (maze.startGy + 0.5f) * CELL
    var yawDeg: Float = 0f
    var pitchDeg: Float = 0f
    var velX: Float = 0f
    var velZ: Float = 0f

    /** Altura de los pies. La cueva tiene relieve, asi que no siempre es cero. */
    var posY: Float = maze.floorY(maze.startGx, maze.startGy)
    var velY: Float = 0f
    /** Como va parado: lo pide el jugador, pero el techo tiene la ultima palabra. */
    var postura: Postura = Postura.DE_PIE
        private set
    var enSuelo: Boolean = true
        private set
    /** Esta parado en una casilla con escalera: puede subir y baja despacio. */
    var enEscalera: Boolean = false
        private set
    /** Altura del ojo suavizada, para que agacharse no sea un tiron de camara. */
    private var alturaOjoSuave: Float = Postura.DE_PIE.alturaOjo

    var health: Float = stats.maxHealth
    var stamina: Float = stats.maxStamina
    var livesLeft: Int = stats.extraLives

    var phase: Phase = Phase.JUGANDO
        private set

    // ------------------------------------------------------- progreso
    var elapsedMs: Long = 0L; private set
    var ecosCollected: Int = 0; private set
    var vetagrisCollected: Int = 0; private set
    var steps: Int = 0; private set
    var damageTakenTotal: Float = 0f; private set

    /** Casillas ya vistas por el jugador (para el minimapa). */
    val revealed = BooleanArray(maze.gw * maze.gh)
    /** Casillas por las que efectivamente camino. */
    val walked = BooleanArray(maze.gw * maze.gh)

    val pickups = ArrayList<Pickup>()
    val traps = ArrayList<TrapInstance>()
    val marks = ArrayList<Mark>()
    val trail = ArrayList<TrailPoint>()
    val torches: List<Int> = blueprint.torches
    val stalagmites: List<Int> = blueprint.stalagmites
    val carbideStations: List<Int> = blueprint.carbide
    val crystalClusters: List<Int> = blueprint.crystalClusters
    val rocks: List<Int> = blueprint.rocks
    val mushrooms: List<Int> = blueprint.mushrooms
    val beams: List<Int> = blueprint.beams

    // ---------------------------------------------------------- de a varios
    /**
     * Enlace con la sala, si esta partida es de a varios. En una partida
     * solitaria queda en null y nada de lo que sigue se ejecuta.
     */
    var red: MatchLink? = null

    /**
     * Cooperativo: estas caido esperando que un companiero te levante. No te
     * podes mover, pero seguis viendo (y sigue corriendo el reloj).
     */
    var caido: Boolean = false
        private set

    /** Indice de casilla, que es como viajan los hechos del mundo por la red. */
    private fun indiceDe(gx: Int, gy: Int): Int = gy * maze.gw + gx

    /** Bichos vivos del nivel y su cabeza compartida. */
    val enemies: List<Enemy> = blueprint.enemies.mapIndexed { i, e ->
        Enemy(
            e.kind,
            (e.gx + 0.5f) * CELL, (e.gy + 0.5f) * CELL,
            e.gx, e.gy,
            (i * 0.7391f) % 6.2831f
        )
    }
    private val brain = EnemyBrain(maze, seed)

    // ---------------------------------------------------------- linterna
    /** La linterna esta encendida. */
    var linternaEncendida: Boolean = false
        private set
    /** Carburo que le queda, de 0 a 1. */
    var carburo: Float = if (stats.tieneLinterna) 1f else 0f
        private set
    /** Estaciones de carga ya usadas (se gastan una vez por nivel). */
    private val estacionesUsadas = HashSet<Int>()
    fun estacionUsada(gi: Int): Boolean = estacionesUsadas.contains(gi)

    // ------------------------------------------------------------- pelea
    /** Segundos que faltan para poder volver a golpear. */
    var golpeRecarga: Float = 0f
        private set
    /** De 1 a 0 mientras dura el swing: lo usa el renderer para el brazo. */
    var golpeAnim: Float = 0f
        private set
    /** Bichos que volteaste en este nivel. */
    var bichosVolteados: Int = 0
        private set

    /** Cargas activas de objetos de uso puntual. */
    var pickCharges = 0; private set
    var phaseCharges = 0; private set
    var chalkCharges = 0; private set
    var undoTrapCharges = 0; private set

    var phaseWindow = 0f      // segundos con colision desactivada
    var lastJunctionX = posX
    var lastJunctionZ = posZ

    /** Mensajes cortos para el HUD ("Antorcha encendida", "Trampa!"). */
    private val toasts = ArrayList<Pair<String, Float>>()
    fun toast(msg: String, seconds: Float = 2.4f) {
        toasts.removeAll { it.first == msg }
        toasts.add(msg to seconds)
        if (toasts.size > 3) toasts.removeAt(0)
    }
    fun currentToasts(): List<String> = toasts.map { it.first }

    /** Ping de la salida (reliquia Rosa de los Vientos). */
    var exitPingTimer = 0f; private set
    var exitPingFlash = 0f; private set
    /** Recarga del sonar gratis (reliquia Diapason). */
    var freeSonarTimer = 0f; private set

    private var timerFrozen = 0f
    private var sinceDamage = 0f
    private var bobPhase = 0f
    private var stepAccum = 0f
    private var runBurstLeft = stats.startBurstSeconds

    /** Sonidos pendientes que el motor de audio va a consumir este frame. */
    private val soundQueue = ArrayList<Sfx>()
    enum class Sfx { PASO, ECO, ECO_GRANDE, VETAGRIS, COFRE, TRAMPA, DANO, USAR, ROMPER, GANAR, PERDER, MARCA, ZUMBIDO, BICHO, GOLPE, IMPACTO }
    fun drainSounds(): List<Sfx> {
        if (soundQueue.isEmpty()) return emptyList()
        val out = ArrayList(soundQueue); soundQueue.clear(); return out
    }
    private fun play(s: Sfx) { if (soundQueue.size < 8) soundQueue.add(s) }

    // ------------------------------------------------------------- init
    init {
        blueprint.coins.forEach { pickups.add(Pickup(it % maze.gw, it / maze.gw, PickupKind.ECO)) }
        blueprint.bigCoins.forEach { pickups.add(Pickup(it % maze.gw, it / maze.gw, PickupKind.ECO_GRANDE)) }
        blueprint.crystals.forEach { pickups.add(Pickup(it % maze.gw, it / maze.gw, PickupKind.VETAGRIS)) }
        blueprint.chests.forEach { pickups.add(Pickup(it % maze.gw, it / maze.gw, PickupKind.COFRE)) }
        blueprint.traps.forEach { traps.add(TrapInstance(it.gx, it.gy, it.kind)) }

        // Orientacion inicial: mirando hacia donde arranca el camino a la salida.
        // Nunca de frente a una pared.
        yawDeg = initialYaw()

        // Revelado inicial por la mejora Cartografo Innato.
        if (stats.startMapReveal > 0f) revealFraction(stats.startMapReveal)
        revealAround(maze.startGx, maze.startGy, 3)
        if (stats.exitPingSeconds > 0f) exitPingTimer = stats.exitPingSeconds
        if (stats.freeSonarSeconds > 0f) freeSonarTimer = stats.freeSonarSeconds

        // Poder Memoria de la Sima: lo que ya exploraste de ESTE nivel vuelve.
        if (stats.mapaPersistente) save.restoreExplored(level, revealed)
        // Poder Ojo de la Veta: lo que vale la pena ya viene marcado.
        if (stats.verTesoros) {
            for (pk in pickups) {
                if (pk.kind == PickupKind.VETAGRIS || pk.kind == PickupKind.COFRE) {
                    revealAround(pk.gx, pk.gy, 1)
                }
            }
        }
        // Poder Pico Eterno: un golpe de pico de regalo en cada bajada.
        if (stats.picosGratis > 0) pickCharges += stats.picosGratis

        markWalked()
    }

    /**
     * Angulo de arranque. Se mira hacia el primer tramo del camino a la salida;
     * si por lo que sea no hay camino, se busca cualquier vecino transitable.
     * El mapeo es: yaw 0 = +Z, 90 = +X, 180 = -Z, 270 = -X.
     */
    private fun initialYaw(): Float {
        val path = maze.solutionPath
        if (path.size >= 2) {
            // El primer paso siempre es una casilla pegada y transitable:
            // apuntar mas lejos podria dar una diagonal contra la roca.
            val i = path[1]
            val tx = (i % maze.gw + 0.5f) * CELL
            val tz = (i / maze.gw + 0.5f) * CELL
            val dx = tx - posX
            val dz = tz - posZ
            if (abs(dx) > 0.01f || abs(dz) > 0.01f) {
                return Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat().let {
                    if (it < 0f) it + 360f else it
                }
            }
        }
        return when {
            maze.isOpen(maze.startGx, maze.startGy + 1) -> 0f
            maze.isOpen(maze.startGx + 1, maze.startGy) -> 90f
            maze.isOpen(maze.startGx, maze.startGy - 1) -> 180f
            maze.isOpen(maze.startGx - 1, maze.startGy) -> 270f
            else -> 0f
        }
    }

    /** Angulo de arranque, para que el paseo del lobby mire al lugar correcto. */
    fun initialYawPublic(): Float = initialYaw()

    // ------------------------------------------------------------ consultas

    val exitWorldX: Float get() = (maze.exitGx + 0.5f) * CELL
    val exitWorldZ: Float get() = (maze.exitGy + 0.5f) * CELL

    fun distanceToExit(): Float = hypot(exitWorldX - posX, exitWorldZ - posZ)

    /**
     * Cuanto tenes que girar para quedar de frente a la salida, en grados.
     * Positivo = la salida esta a tu derecha; negativo = a tu izquierda.
     */
    fun bearingToExit(): Float {
        val ang = Math.toDegrees(atan2((exitWorldX - posX).toDouble(), (exitWorldZ - posZ).toDouble())).toFloat()
        var d = yawDeg - ang
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return d
    }

    fun gridX(): Int = (posX / CELL).toInt().coerceIn(0, maze.gw - 1)
    fun gridY(): Int = (posZ / CELL).toInt().coerceIn(0, maze.gh - 1)

    /** Multiplicador de luz total (antorcha + buffs + bengala). */
    fun lightRadiusNow(): Float {
        var r = stats.lightRadius
        r *= effects.multiplier(EffectType.LUZ_RADIO)
        if (effects.isActive(EffectType.BENGALA)) r *= effects.magnitude(EffectType.BENGALA, 1f)
        return r
    }

    fun ambientBoost(): Float {
        var a = stats.minAmbient
        if (effects.isActive(EffectType.VISION_NOCTURNA))
            a = max(a, effects.magnitude(EffectType.VISION_NOCTURNA))
        return a
    }

    fun isCompassOn(): Boolean =
        effects.isActive(EffectType.BRUJULA) || save.settings.compassAlwaysOn

    fun isSonarOn(): Boolean = effects.isActive(EffectType.SONAR)

    fun sonarRange(): Float = effects.magnitude(EffectType.SONAR, 0f)

    fun healthFraction(): Float = (health / stats.maxHealth).coerceIn(0f, 1f)
    fun staminaFraction(): Float = (stamina / stats.maxStamina).coerceIn(0f, 1f)

    fun fovDegrees(): Float =
        (72f + stats.fovBonus + save.settings.fovExtra).coerceIn(60f, 110f)

    // ------------------------------------------------------------- update

    class Input(
        var moveX: Float = 0f,      // -1 izquierda .. 1 derecha
        var moveY: Float = 0f,      // -1 atras .. 1 adelante
        var lookX: Float = 0f,      // grados a aplicar este frame
        var lookY: Float = 0f,
        var running: Boolean = false,
        /** 0 de pie, 1 agachado, 2 arrastrandose. */
        var agacharse: Int = 0,
        var saltar: Boolean = false
    )

    fun pause() { if (phase == Phase.JUGANDO) phase = Phase.PAUSA }
    fun resume() { if (phase == Phase.PAUSA) phase = Phase.JUGANDO }

    fun update(dtRaw: Float, input: Input) {
        if (phase != Phase.JUGANDO) return
        val dt = dtRaw.coerceIn(0f, 0.05f)   // techo anti-saltos tras un lag

        effects.update(dt)
        if (runBurstLeft > 0f) runBurstLeft -= dt
        if (golpeRecarga > 0f) golpeRecarga -= dt
        if (golpeAnim > 0f) golpeAnim = max(0f, golpeAnim - dt / DURACION_SWING)
        if (phaseWindow > 0f) phaseWindow -= dt
        if (esperaLevantada > 0f) esperaLevantada -= dt
        if (recargaRevivir > 0f) recargaRevivir -= dt
        sinceDamage += dt

        // --- cronometro (se puede congelar)
        if (effects.isActive(EffectType.CONGELAR_RELOJ)) timerFrozen = effects.remaining(EffectType.CONGELAR_RELOJ)
        if (timerFrozen > 0f) timerFrozen -= dt else elapsedMs += (dt * 1000f).toLong()

        // --- camara
        val lookMul = effects.multiplier(EffectType.GIRO_RAPIDO)
        // Con frente = (sin yaw, cos yaw), subir el yaw gira hacia la izquierda,
        // asi que arrastrar hacia la derecha tiene que RESTAR.
        yawDeg = normalizeAngle(yawDeg - input.lookX * lookMul)
        pitchDeg = (pitchDeg + input.lookY * lookMul).coerceIn(-82f, 82f)

        // --- movimiento. Caido se sigue mirando alrededor, pero no se camina:
        // por eso la camara de arriba si se movio y esto no.
        if (!caido) moveStep(dt, input)

        // --- la sala, si esta partida es de a varios
        pasoDeRed(dt)

        // --- regeneracion / desgaste
        // Tirado en el piso no se regenera: si no, con una reliquia de
        // regeneracion el caido llegaba a la vida llena mirando el techo, y
        // la media barra con la que te levantan no significaba nada.
        if (!caido && stats.regenPerSecond > 0f && sinceDamage > 2.5f && health < stats.maxHealth) {
            health = min(stats.maxHealth, health + stats.regenPerSecond * dt)
        }

        // --- reliquias con temporizador
        if (stats.exitPingSeconds > 0f) {
            exitPingTimer -= dt
            if (exitPingTimer <= 0f) { exitPingTimer = stats.exitPingSeconds; exitPingFlash = 2.2f }
        }
        if (exitPingFlash > 0f) exitPingFlash -= dt
        if (stats.freeSonarSeconds > 0f && freeSonarTimer > 0f) freeSonarTimer -= dt

        // --- linterna, bichos y rastro
        updateLinterna(dt)
        updateEnemies(dt, input)
        updateTrail(dt)
        // --- objetos y trampas
        updatePickups(dt)
        updateTraps(dt)

        // --- avisos que se apagan solos
        for (i in toasts.indices.reversed()) {
            val (m, t) = toasts[i]
            val nt = t - dt
            if (nt <= 0f) toasts.removeAt(i) else toasts[i] = m to nt
        }

        // --- llegada. Caido no se sale: primero que te levanten.
        if (!caido && hypot(exitWorldX - posX, exitWorldZ - posZ) <= EXIT_RADIUS) win()
    }

    private fun normalizeAngle(a: Float): Float {
        var x = a
        while (x >= 360f) x -= 360f
        while (x < 0f) x += 360f
        return x
    }

    private fun moveStep(dt: Float, input: Input) {
        // La postura y la altura se resuelven primero: definen por donde entra
        // el jugador y a que velocidad puede ir.
        pasoVertical(dt, input)

        var speed = stats.walkSpeed
        speed *= effects.multiplier(EffectType.VELOCIDAD)
        if (runBurstLeft > 0f) speed *= (1f + stats.startBurstSpeed)
        // El poder Reptador saca la penalidad de ir agachado.
        speed *= if (stats.posturaLibre) 1f else postura.velocidad

        val wantsRun = (input.running || save.settings.autoRun) && stamina > 1f && postura.puedeCorrer
        val moving = abs(input.moveX) > 0.02f || abs(input.moveY) > 0.02f

        if (wantsRun && moving) {
            speed *= stats.runMultiplier
            stamina = max(0f, stamina - stats.staminaDrain * dt)
        } else {
            stamina = min(stats.maxStamina, stamina + PlayerStats.STAMINA_REGEN * dt)
        }

        val yawRad = Math.toRadians(yawDeg.toDouble())
        val fx = sin(yawRad).toFloat()
        val fz = cos(yawRad).toFloat()
        // Derecha de la PANTALLA = frente x arriba, que es exactamente el vector
        // lateral que arma Matrix.setLookAtM. Con el signo al reves el personaje
        // se movia para el lado contrario al que empujabas el joystick.
        val rx = -fz
        val rz = fx

        var dx = fx * input.moveY + rx * input.moveX
        var dz = fz * input.moveY + rz * input.moveX
        val mag = hypot(dx, dz)
        if (mag > 1f) { dx /= mag; dz /= mag }

        val targetVX = dx * speed
        val targetVZ = dz * speed
        // Aceleracion: el aceite la sube y baja la friccion.
        val accel = 16f * effects.multiplier(EffectType.DESLIZAR)
        val k = (accel * dt).coerceAtMost(1f)
        velX += (targetVX - velX) * k
        velZ += (targetVZ - velZ) * k

        val stepX = velX * dt
        val stepZ = velZ * dt
        val ghost = phaseWindow > 0f

        if (ghost) {
            posX += stepX; posZ += stepZ
            clampToWorld()
        } else {
            // Colision por eje: permite deslizarse contra la pared.
            if (!bloqueado(posX + stepX, posZ)) posX += stepX else velX = 0f
            if (!bloqueado(posX, posZ + stepZ)) posZ += stepZ else velZ = 0f
        }

        // Cabeceo y pasos
        val travelled = hypot(stepX, stepZ)
        if (travelled > 0.0001f) {
            bobPhase += travelled * 2.6f
            stepAccum += travelled
            if (stepAccum >= 1.55f) {
                stepAccum = 0f
                steps++
                play(Sfx.PASO)
            }
            markWalked()
        }

        revealAround(gridX(), gridY(), 2)
        rememberJunction()
    }

    /**
     * Alto libre en un punto, mirando todas las casillas que toca el cuerpo del
     * jugador. Al tomar el minimo, el jugador se agacha un poquito ANTES de
     * meterse en la gatera y no despues.
     */
    fun alturaLibreEn(x: Float, z: Float): Float {
        val r = PLAYER_RADIUS
        var libre = Maze.ALTO_NORMAL
        val gx0 = ((x - r) / CELL).toInt()
        val gx1 = ((x + r) / CELL).toInt()
        val gy0 = ((z - r) / CELL).toInt()
        val gy1 = ((z + r) / CELL).toInt()
        for (gy in gy0..gy1) {
            for (gx in gx0..gx1) {
                if (!maze.inBounds(gx, gy) || maze.isSolid(gx, gy)) continue
                val c = maze.ceilClearance[maze.index(gx, gy)]
                if (c < libre) libre = c
            }
        }
        return libre
    }

    /**
     * Postura y movimiento vertical: agacharse, saltar, caer y escaleras.
     *
     * Agacharse no gasta aguante a proposito: es una forma de avanzar, no un
     * esfuerzo. Lo unico que consume aguante es correr.
     */
    private fun pasoVertical(dt: Float, input: Input) {
        val gx = gridX(); val gy = gridY()
        val i = maze.index(gx, gy)
        val suelo = maze.floorY(gx, gy)
        val libre = alturaLibreEn(posX, posZ)
        enEscalera = maze.hasLadder(gx, gy)

        // La postura la pide el jugador, pero si el techo esta bajo se agacha
        // solo. Es a proposito: la cueva nunca te frena en seco por no haber
        // apretado un boton, agacharse no cuesta aguante y el boton sigue
        // sirviendo para agacharte donde vos quieras.
        val pedida = when {
            input.agacharse >= 2 -> Postura.ARRASTRANDOSE
            input.agacharse == 1 -> Postura.AGACHADO
            else -> Postura.DE_PIE
        }
        val cabe = Postura.paraAltura(libre) ?: Postura.ARRASTRANDOSE
        postura = if (pedida.alturaCuerpo <= cabe.alturaCuerpo) pedida else cabe

        // --- salto: solo de pie, con suelo bajo los pies y techo arriba.
        if (input.saltar && enSuelo && postura.puedeSaltar && libre >= Postura.DE_PIE.alturaCuerpo) {
            velY = VEL_SALTO * stats.saltoExtra
            enSuelo = false
        }

        if (!enSuelo || posY > suelo + 0.001f) {
            velY += GRAVEDAD * dt
            // Agarrado a la escalera la caida es un descenso controlado.
            if (enEscalera && velY < -VEL_ESCALERA) velY = -VEL_ESCALERA
            posY += velY * dt
            if (velY <= 0f && posY <= suelo) {
                val golpe = -velY
                posY = suelo
                velY = 0f
                enSuelo = true
                if (golpe > CAIDA_SEGURA && !stats.sinDanoDeCaida) {
                    applyDamage((golpe - CAIDA_SEGURA) * 3.2f)
                    toast("Mala caida")
                }
            } else {
                enSuelo = false
            }
        } else if (posY < suelo - 0.001f) {
            // Subiendo. Un escalon se sube caminando; un desnivel grande solo se
            // sube por escalera, y lleva su tiempo.
            val falta = suelo - posY
            posY += if (falta <= Maze.SUBIDA_CAMINANDO) falta else min(falta, VEL_ESCALERA * dt)
            if (posY > suelo - 0.001f) posY = suelo
            velY = 0f
            enSuelo = true
        } else {
            posY = suelo
            velY = 0f
            enSuelo = true
        }

        // La camara acompana el cambio de postura sin pegar el tiron.
        val k = (10f * dt).coerceIn(0f, 1f)
        alturaOjoSuave += (postura.alturaOjo - alturaOjoSuave) * k
    }

    /**
     * Colision de verdad: la roca, mas el relieve.
     *
     * [collides] sigue significando "el circulo del jugador toca roca" porque
     * lo usan las trampas y los objetos. Esto ademas frena cuando el escalon es
     * demasiado alto para subirlo caminando (y no hay escalera) o cuando el
     * techo no deja pasar con la postura actual.
     */
    fun bloqueado(x: Float, z: Float): Boolean {
        if (collides(x, z)) return true
        val gx = (x / CELL).toInt()
        val gy = (z / CELL).toInt()
        if (!maze.inBounds(gx, gy)) return true
        // El techo solo frena si no entra ni arrastrandose: eso no deberia
        // pasar nunca (lo garantiza el generador), pero si pasara el jugador
        // quedaria encerrado, asi que el chequeo se queda de red.
        if (alturaLibreEn(x, z) < Postura.ARRASTRANDOSE.alturaCuerpo) return true
        val destino = maze.floorY(gx, gy)
        if (destino - posY > Maze.SUBIDA_CAMINANDO) {
            // Escalon grande: solo con escalera en alguna de las dos puntas.
            if (!maze.hasLadder(gx, gy) && !maze.hasLadder(gridX(), gridY())) return true
        }
        return false
    }

    private fun clampToWorld() {
        val minP = CELL * 0.6f
        posX = posX.coerceIn(minP, (maze.gw - 0.6f) * CELL)
        posZ = posZ.coerceIn(minP, (maze.gh - 0.6f) * CELL)
    }

    /** Circulo del jugador contra las casillas solidas vecinas. */
    fun collides(x: Float, z: Float): Boolean {
        val r = PLAYER_RADIUS
        val gx0 = ((x - r) / CELL).toInt()
        val gx1 = ((x + r) / CELL).toInt()
        val gy0 = ((z - r) / CELL).toInt()
        val gy1 = ((z + r) / CELL).toInt()
        for (gy in gy0..gy1) {
            for (gx in gx0..gx1) {
                if (!maze.isSolid(gx, gy)) continue
                val minX = gx * CELL
                val minZ = gy * CELL
                val nx = x.coerceIn(minX, minX + CELL)
                val nz = z.coerceIn(minZ, minZ + CELL)
                val ddx = x - nx
                val ddz = z - nz
                if (ddx * ddx + ddz * ddz < r * r) return true
            }
        }
        return false
    }

    private fun markWalked() {
        val i = maze.index(gridX(), gridY())
        if (i in walked.indices) walked[i] = true
    }

    private fun rememberJunction() {
        val gx = gridX(); val gy = gridY()
        var open = 0
        if (maze.isOpen(gx + 1, gy)) open++
        if (maze.isOpen(gx - 1, gy)) open++
        if (maze.isOpen(gx, gy + 1)) open++
        if (maze.isOpen(gx, gy - 1)) open++
        if (open >= 3) { lastJunctionX = (gx + 0.5f) * CELL; lastJunctionZ = (gy + 0.5f) * CELL }
    }

    fun revealAround(gx: Int, gy: Int, radius: Int) {
        for (y in gy - radius..gy + radius) {
            for (x in gx - radius..gx + radius) {
                if (!maze.inBounds(x, y)) continue
                revealed[maze.index(x, y)] = true
            }
        }
    }

    fun revealFraction(f: Float) {
        if (f <= 0f) return
        if (f >= 1f) { revealed.fill(true); return }
        val cells = ArrayList<Int>()
        for (i in revealed.indices) if (!revealed[i]) cells.add(i)
        cells.shuffle(rnd)
        val n = (cells.size * f).toInt()
        for (i in 0 until n) revealed[cells[i]] = true
    }

    private fun updateTrail(dt: Float) {
        val threadOn = effects.isActive(EffectType.HILO_ARIADNA)
        val life = if (threadOn) 999f else stats.trailSeconds
        if (trail.isEmpty() || hypot(trail.last().x - posX, trail.last().z - posZ) > 0.75f) {
            if (trail.size < 900) trail.add(TrailPoint(posX, posZ, life))
        }
        if (!threadOn) {
            for (i in trail.indices.reversed()) {
                trail[i].life -= dt
                if (trail[i].life <= 0f) trail.removeAt(i)
            }
        }
    }

    private fun updatePickups(dt: Float) {
        val magnetR = stats.pickupRadius
        val bonus = effects.multiplier(EffectType.BONUS_ECOS)
        for (p in pickups) {
            if (p.taken) continue
            p.bob += dt
            val px = (p.gx + 0.5f) * CELL
            val pz = (p.gy + 0.5f) * CELL
            if (p.flying) {
                val dx = posX - (px + p.flyX)
                val dz = posZ - (pz + p.flyZ)
                p.flyX += dx * min(1f, dt * 4.5f)
                p.flyZ += dz * min(1f, dt * 4.5f)
                if (hypot(dx, dz) < 0.5f) { collect(p, bonus); continue }
                continue
            }
            val d = hypot(px + p.flyX - posX, pz + p.flyZ - posZ)
            val need = when (p.kind) {
                PickupKind.COFRE -> 1.4f
                else -> magnetR
            }
            if (d <= need) collect(p, bonus)
        }
    }

    private fun collect(p: Pickup, bonus: Float) {
        p.taken = true
        // Avisar antes de repartir: para los demas este objeto ya no esta,
        // aunque los ecos me los lleve yo.
        red?.avisarTomado(indiceDe(p.gx, p.gy))
        when (p.kind) {
            PickupKind.ECO -> {
                var v = 5
                if (stats.ecoCritChance > 0f && rnd.nextFloat() < stats.ecoCritChance) {
                    v *= 10; toast("Veta rica! x10")
                }
                ecosCollected += (v * bonus).toInt()
                play(Sfx.ECO)
            }
            PickupKind.ECO_GRANDE -> {
                var v = 25
                if (stats.ecoCritChance > 0f && rnd.nextFloat() < stats.ecoCritChance) {
                    v *= 10; toast("Veta rica! x10")
                }
                ecosCollected += (v * bonus).toInt()
                play(Sfx.ECO_GRANDE)
            }
            PickupKind.VETAGRIS -> {
                vetagrisCollected += 1
                toast("Vetagris encontrado")
                play(Sfx.VETAGRIS)
            }
            PickupKind.COFRE -> {
                val loot = 40 + rnd.nextInt(50 + level * 4)
                ecosCollected += (loot * bonus).toInt()
                // Un cofre tambien puede tener un consumible.
                if (rnd.nextFloat() < 0.55f) {
                    val pool = ItemCatalog.ofKind(ItemKind.CONSUMIBLE)
                        .filter { it.unlockLevel <= save.maxLevel }
                    if (pool.isNotEmpty()) {
                        val it2 = pool[rnd.nextInt(pool.size)]
                        save.grantConsumable(it2.id, 1)
                        toast("Cofre: ${it2.name}")
                    }
                } else toast("Cofre: $loot ecos")
                play(Sfx.COFRE)
            }
        }
    }

    // -------------------------------------------------------------- pelea

    fun puedeGolpear(): Boolean = phase == Phase.JUGANDO && golpeRecarga <= 0f

    /**
     * Un golpe cuerpo a cuerpo hacia donde estas mirando.
     *
     * Le pega a TODOS los bichos vivos que entren en el cono: si te rodean
     * tres murcielagos, un buen golpe se lleva a los tres puestos. Devuelve
     * cuantos toco, o -1 si todavia no se podia golpear.
     */
    fun golpear(): Int {
        if (!puedeGolpear()) return -1
        golpeRecarga = stats.cadenciaGolpe
        golpeAnim = 1f
        play(Sfx.GOLPE)

        val yawRad = Math.toRadians(yawDeg.toDouble())
        val fx = sin(yawRad).toFloat()
        val fz = cos(yawRad).toFloat()
        val alcance = stats.alcanceGolpe
        val ojoY = posY + postura.alturaOjo

        var tocados = 0
        for (e in enemies) {
            if (!e.vivo) continue
            val dx = e.x - posX
            val dz = e.z - posZ
            val d = hypot(dx, dz)
            // El alcance se mide hasta el borde del bicho, no hasta su centro:
            // si no, a un guardian gordo no le llegabas nunca.
            if (d > alcance + e.kind.radio) continue
            // Y tiene que estar mas o menos a tu altura: un murcielago que
            // pasa por arriba de la cabeza no se toca desde el piso.
            if (abs(e.altura + e.kind.radio - ojoY) > 1.5f) continue
            if (d > 0.05f) {
                val cos = (dx * fx + dz * fz) / d
                if (cos < COS_CONO_GOLPE) continue
            }
            tocados++
            val cayo = e.recibirGolpe(stats.danoGolpe, posX, posZ, stats.empujeGolpe)
            if (cayo) voltear(e)
        }
        if (tocados > 0) play(Sfx.IMPACTO)
        return tocados
    }

    private fun voltear(e: Enemy) {
        bichosVolteados++
        val premio = (e.kind.recompensa * (1f + level * 0.02f)).toInt().coerceAtLeast(1)
        ecosCollected += premio
        toast("${e.kind.etiqueta} volteado  +$premio")
        play(Sfx.ECO)
    }

    /** Bichos vivos que quedan en el nivel. */
    fun bichosVivos(): Int = enemies.count { it.vivo }

    // ------------------------------------------------------------ linterna

    /** Enciende o apaga la linterna. Devuelve si quedo encendida. */
    fun toggleLinterna(): Boolean {
        if (!stats.tieneLinterna) return false
        if (!linternaEncendida && carburo <= 0.01f) {
            toast("Sin carburo: buscá una estación")
            return false
        }
        linternaEncendida = !linternaEncendida
        play(Sfx.USAR)
        toast(if (linternaEncendida) "Linterna encendida" else "Linterna apagada")
        return linternaEncendida
    }

    /**
     * Gasta carburo mientras esta prendida y la recarga al pisar una estacion.
     * Cada estacion sirve una sola vez por bajada.
     */
    private fun updateLinterna(dt: Float) {
        if (!stats.tieneLinterna) return
        if (linternaEncendida) {
            carburo = (carburo - dt / DURACION_CARBURO).coerceAtLeast(0f)
            if (carburo <= 0f) {
                linternaEncendida = false
                toast("Se acabó el carburo")
            }
        }
        if (carburo >= 0.999f) return
        for (gi in carbideStations) {
            if (estacionesUsadas.contains(gi)) continue
            val gx = gi % maze.gw
            val gy = gi / maze.gw
            val d = hypot((gx + 0.5f) * CELL - posX, (gy + 0.5f) * CELL - posZ)
            if (d > 1.35f) continue
            estacionesUsadas.add(gi)
            carburo = 1f
            play(Sfx.USAR)
            toast("Linterna recargada")
            break
        }
    }

    /** Aporte de la linterna al alcance de la luz, ya con el carburo que queda. */
    fun linternaFuerza(): Float =
        if (linternaEncendida && carburo > 0f) (0.35f + 0.65f * carburo) else 0f

    // -------------------------------------------------------------- bichos

    /** Bichos que ahora mismo te estan persiguiendo. */
    fun enemigosAlerta(): Int = enemies.count { it.alerta && it.vivo }

    /**
     * Los bichos.
     *
     * OJO, limitacion conocida del modo de a varios: los bichos NO viajan por
     * la red. Nacen en el mismo lugar en los dos telefonos (la cueva es la
     * misma), pero de ahi en mas cada uno corre su propia cabeza y persigue a
     * su propio jugador, asi que al rato estan en lugares distintos en cada
     * pantalla.
     *
     * Es a proposito y no un olvido: sincronizarlos significa mandar la
     * posicion de veinte bichos diez veces por segundo, y ademas decidir cual
     * de los dos telefonos manda (hoy no hay ninguno que mande sobre el otro).
     * Eso es otro laburo entero. Para jugar con amigos alcanza asi: lo que si
     * esta sincronizado es todo lo que deja marca en el mundo (las monedas,
     * las paredes rotas, las trampas) y donde esta cada uno.
     */
    private fun updateEnemies(dt: Float, input: Input) {
        if (enemies.isEmpty()) return
        // Arrastrandose con Paso de Sombra directamente no te ven.
        val invisible = stats.invisibleArrastrandose && postura == Postura.ARRASTRANDOSE
        val ruidoso = (input.running || save.settings.autoRun) &&
            (abs(input.moveX) > 0.02f || abs(input.moveY) > 0.02f) && postura.puedeCorrer
        brain.update(
            dt = dt,
            enemigos = enemies,
            px = posX, pz = posZ, pRadio = PLAYER_RADIUS,
            alcanceEscala = stats.sigilo,
            detectable = !invisible && phaseWindow <= 0f,
            ruidoso = ruidoso,
            cell = CELL,
            pisoDe = { gx, gy -> maze.floorY(gx, gy) },
            muerde = { e -> mordidaDe(e) }
        )
    }

    private fun mordidaDe(e: Enemy) {
        var dmg = e.kind.dano * (1f + level * 0.010f)
        dmg *= stats.damageTaken
        dmg *= (1f - effects.magnitude(EffectType.RESISTENCIA, 0f))
        if (healthFraction() < 0.30f) dmg *= (1f - stats.criticalArmor)
        applyDamage(dmg)
        toast("${e.kind.etiqueta}!")
        play(Sfx.BICHO)
    }

    private fun updateTraps(dt: Float) {
        val immune = effects.isActive(EffectType.INMUNE_TRAMPAS)
        val sense = stats.trapSenseRange
        var nearHum = false
        for (t in traps) {
            if (t.cooldown > 0f) t.cooldown -= dt
            val tx = (t.gx + 0.5f) * CELL
            val tz = (t.gy + 0.5f) * CELL
            val d = hypot(tx - posX, tz - posZ)
            if (sense > 0f && d <= sense) { t.revealed = true; if (d <= 5f) nearHum = true }
            if (!t.armed || t.cooldown > 0f || immune) continue
            if (d < 1.0f) triggerTrap(t)
        }
        if (nearHum && rnd.nextFloat() < dt * 1.2f) play(Sfx.ZUMBIDO)
    }

    private fun triggerTrap(t: TrapInstance) {
        t.cooldown = 2.5f
        t.revealed = true
        red?.avisarTrampa(indiceDe(t.gx, t.gy))
        if (undoTrapCharges > 0) {
            undoTrapCharges--
            toast("El Cristal de Retroceso te salvo")
            return
        }
        val isHeat = t.kind == MazeGenerator.TrapKind.STEAM
        if (isHeat && stats.heatImmune) { toast("La Escama aguanto el calor"); return }

        var dmg = when (t.kind) {
            MazeGenerator.TrapKind.SPIKES -> 22f
            MazeGenerator.TrapKind.PITFALL -> 30f
            MazeGenerator.TrapKind.STEAM -> 16f
            MazeGenerator.TrapKind.ROCKFALL -> 26f
        } * (1f + level * 0.012f)

        dmg *= stats.damageTaken
        dmg *= (1f - effects.magnitude(EffectType.RESISTENCIA, 0f))
        if (healthFraction() < 0.30f) dmg *= (1f - stats.criticalArmor)

        applyDamage(dmg)
        toast(
            when (t.kind) {
                MazeGenerator.TrapKind.SPIKES -> "Pinches!"
                MazeGenerator.TrapKind.PITFALL -> "Te comio el pozo!"
                MazeGenerator.TrapKind.STEAM -> "Vapor hirviendo!"
                MazeGenerator.TrapKind.ROCKFALL -> "Se vino la roca!"
            }
        )
        play(Sfx.TRAMPA)
    }

    fun applyDamage(amount: Float) {
        if (amount <= 0f) return
        // Al caido no se le pega mas: ya esta en el piso esperando que lo
        // levanten, y si siguiera recibiendo dano el primer bicho que pase lo
        // mandaria a PERDIDO sin que nadie pueda hacer nada.
        if (caido) return
        health -= amount
        damageTakenTotal += amount
        sinceDamage = 0f
        play(Sfx.DANO)
        if (health <= 0f) {
            if (livesLeft > 0) {
                livesLeft--
                health = stats.maxHealth * 0.5f
                toast("La cuerda te salvo. Te quedan $livesLeft")
            } else {
                lose()
            }
        }
    }

    // -------------------------------------------------------- usar objetos

    /** Usa un consumible del inventario. Devuelve true si se aplico. */
    fun useItem(id: String): Boolean {
        if (phase != Phase.JUGANDO) return false
        val item = ItemCatalog.get(id) ?: return false
        if (item.kind != ItemKind.CONSUMIBLE) return false
        if (save.stockOf(id) <= 0) { toast("No te queda ninguno"); return false }

        val applied = applyConsumable(item)
        if (!applied) return false

        // El Corazon Gemelo puede evitar el gasto.
        val saved = stats.consumableSaveChance > 0f && rnd.nextFloat() < stats.consumableSaveChance
        if (saved) toast("El Corazon Gemelo lo conservo") else save.consume(id)
        play(Sfx.USAR)
        return true
    }

    /** Aplica el efecto. Devuelve false si en este momento no tiene sentido usarlo. */
    private fun applyConsumable(item: ShopItem): Boolean {
        val e = item.effect
        val dur = e.duration * (if (e.type == EffectType.HILO_ARIADNA) stats.threadMultiplier else 1f)
        when (e.type) {
            EffectType.LUZ_RADIO, EffectType.VELOCIDAD, EffectType.BRUJULA,
            EffectType.VISION_NOCTURNA, EffectType.CONGELAR_RELOJ, EffectType.BENGALA,
            EffectType.RESISTENCIA, EffectType.GIRO_RAPIDO, EffectType.SONAR,
            EffectType.DESLIZAR, EffectType.BONUS_ECOS, EffectType.INMUNE_TRAMPAS,
            EffectType.HILO_ARIADNA -> {
                effects.apply(e.type, e.magnitude, dur, item.id)
                toast(item.name)
            }
            EffectType.RECARGA_AGUANTE -> {
                if (stamina >= stats.maxStamina - 0.5f) { toast("Ya tenes el aguante lleno"); return false }
                stamina = stats.maxStamina
                toast("Aguante al maximo")
            }
            EffectType.CURAR -> {
                if (health >= stats.maxHealth - 0.5f) { toast("Ya estas entero"); return false }
                health = min(stats.maxHealth, health + e.magnitude)
                toast("+${e.magnitude.toInt()} de vida")
            }
            EffectType.COMIDA -> {
                if (health >= stats.maxHealth - 0.5f && stamina >= stats.maxStamina - 0.5f) {
                    toast("No te entra nada mas"); return false
                }
                health = min(stats.maxHealth, health + e.magnitude)
                stamina = min(stats.maxStamina, stamina + stats.maxStamina * 0.25f)
                toast("Comiste algo")
            }
            EffectType.REVELAR_MAPA -> {
                revealFraction(e.magnitude)
                toast(if (e.magnitude >= 1f) "Mapa completo" else "Mapa parcial revelado")
            }
            EffectType.TIZA -> { chalkCharges += e.charges; toast("$chalkCharges marcas de tiza") }
            EffectType.ROMPE_PARED -> { pickCharges += e.charges; toast("Apunta a una pared y toca Romper") }
            EffectType.ATRAVESAR_PARED -> { phaseCharges += e.charges; toast("Apunta a una pared y toca Atravesar") }
            EffectType.DESHACER_TRAMPA -> { undoTrapCharges += e.charges; toast("Proxima trampa anulada") }
            EffectType.IMAN_TOTAL -> {
                var n = 0
                for (p in pickups) if (!p.taken && p.kind != PickupKind.COFRE) { p.flying = true; n++ }
                if (n == 0) { toast("No queda nada que atraer"); return false }
                toast("$n ecos volando hacia vos")
            }
            EffectType.VOLVER_CRUCE -> {
                if (hypot(lastJunctionX - posX, lastJunctionZ - posZ) < 1.5f) {
                    toast("Ya estas en el cruce"); return false
                }
                posX = lastJunctionX; posZ = lastJunctionZ
                velX = 0f; velZ = 0f
                toast("De vuelta en el cruce")
            }
            else -> return false
        }
        return true
    }

    /** Sonar gratis de la reliquia Diapason de Cuarzo. */
    fun useFreeSonar(): Boolean {
        if (stats.freeSonarSeconds <= 0f || freeSonarTimer > 0f) return false
        effects.apply(EffectType.SONAR, 9f, 10f, "rel_diapason")
        freeSonarTimer = stats.freeSonarSeconds
        toast("Grito de Eco (Diapason)")
        play(Sfx.USAR)
        return true
    }

    /** Deja una marca de tiza en el piso donde estas parado. */
    fun dropChalk(): Boolean {
        if (chalkCharges <= 0) { toast("No te queda tiza"); return false }
        chalkCharges--
        marks.add(Mark(posX, posZ, marks.size % 4))
        toast("Marca dejada (${chalkCharges} restantes)")
        play(Sfx.MARCA)
        return true
    }

    /** Rompe la pared que tenes justo enfrente. */
    fun breakWall(): Boolean {
        if (pickCharges <= 0) { toast("No tenes pico"); return false }
        val target = wallAhead() ?: run { toast("No hay pared enfrente"); return false }
        maze.setSolid(target.first, target.second, false)
        if (maze.isSolid(target.first, target.second)) { toast("Esa pared es del borde"); return false }
        pickCharges--
        geometryDirty = true
        maze.refreshSolution()
        red?.avisarRoto(indiceDe(target.first, target.second))
        toast("Pared rota")
        play(Sfx.ROMPER)
        return true
    }

    /** Activa unos segundos de paso fantasma para cruzar la pared de enfrente. */
    fun phaseThrough(): Boolean {
        if (phaseCharges <= 0) { toast("No tenes Salto Fantasma"); return false }
        if (wallAhead() == null) { toast("No hay pared enfrente"); return false }
        phaseCharges--
        phaseWindow = 2.6f
        toast("Atravesa ahora!")
        play(Sfx.USAR)
        return true
    }

    /** Casilla solida mas cercana en la direccion en la que mira el jugador. */
    fun wallAhead(maxDist: Float = 3.2f): Pair<Int, Int>? {
        val yawRad = Math.toRadians(yawDeg.toDouble())
        val fx = sin(yawRad).toFloat()
        val fz = cos(yawRad).toFloat()
        var d = 0.25f
        while (d <= maxDist) {
            val gx = ((posX + fx * d) / CELL).toInt()
            val gy = ((posZ + fz * d) / CELL).toInt()
            if (maze.isSolid(gx, gy)) {
                if (gx <= 0 || gy <= 0 || gx >= maze.gw - 1 || gy >= maze.gh - 1) return null
                return gx to gy
            }
            d += 0.25f
        }
        return null
    }

    fun canBreakWall(): Boolean = pickCharges > 0 && wallAhead() != null
    fun canPhase(): Boolean = phaseCharges > 0 && wallAhead() != null

    // ------------------------------------------------------------ de a varios

    /**
     * Un paso de sala: manda lo tuyo, aplica lo de los demas y hace cumplir
     * las reglas del modo. En una partida solitaria no hace nada.
     */
    private var ultimoErrorRed: String? = null

    private fun mostrarErrorRed(r: MatchLink) {
        val error = r.errorConexion ?: return
        if (error != ultimoErrorRed) {
            ultimoErrorRed = error
            toast(error, 8f)
        }
    }

    private fun pasoDeRed(dt: Float) {
        val r = red ?: return
        mostrarErrorRed(r)
        for (m in r.bombear(dt, posX, posY, posZ, yawDeg, postura.ordinal)) {
            aplicarDeOtro(m)
        }
        when (r.match.modo) {
            NetProtocol.Modo.CARRERA -> {
                // El primero que sale corta la partida para todos. Al que no
                // gano le queda lo que junto, igual que si se hubiera vuelto.
                if (phase == Phase.JUGANDO && r.match.ganador() != null && !llegaste) {
                    val g = r.match.ganador()
                    toast("Gano ${g?.nombre ?: "el otro"}")
                    phase = Phase.PERDIDO
                    guardarExploracion()
                    play(Sfx.PERDER)
                }
            }
            NetProtocol.Modo.COOPERATIVO -> {
                if (!caido) levantarCaidosCerca(r)
                // Si cayeron todos no hay quien levante a nadie: se termino.
                if (caido && r.match.equipoCaido() && phase == Phase.JUGANDO) {
                    toast("Cayeron todos")
                    phase = Phase.PERDIDO
                    guardarExploracion()
                    play(Sfx.PERDER)
                }
            }
        }
    }

    /**
     * Mantiene viva la sala cuando la partida no esta corriendo (pausa).
     *
     * Sin esto, abrir el mapa 15 segundos te sacaba de la sala: el companiero
     * dejaba de recibir latidos tuyos y te daba por ido.
     */
    fun latirRed(dt: Float) {
        val r = red ?: return
        mostrarErrorRed(r)
        for (m in r.latir(dt)) aplicarDeOtro(m)
    }

    /** Un hecho del mundo que hizo otro y me tiene que cambiar la cueva. */
    private fun aplicarDeOtro(m: NetProtocol.Mensaje) {
        if (m.de == red?.yo) return
        when (m.tipo) {
            NetProtocol.Tipo.TOMAR -> {
                val i = m.entero(0)
                for (p in pickups) {
                    if (p.taken || indiceDe(p.gx, p.gy) != i) continue
                    p.taken = true
                    // En cooperativo la plata es de los dos, que es lo que
                    // promete el modo: lo que levanta uno lo cobran todos. En
                    // carrera no, ahi cada uno junta lo suyo.
                    if (red?.match?.modo == NetProtocol.Modo.COOPERATIVO) {
                        when (p.kind) {
                            PickupKind.ECO -> ecosCollected += 5
                            PickupKind.ECO_GRANDE -> ecosCollected += 25
                            PickupKind.VETAGRIS -> vetagrisCollected += 1
                            // El cofre reparte un botin al azar que solo sabe
                            // el que lo abrio, asi que se paga el minimo.
                            PickupKind.COFRE -> ecosCollected += 40
                        }
                        toast("Tu companiero encontro algo  (+compartido)")
                    }
                    break
                }
            }
            NetProtocol.Tipo.ROMPER -> {
                val i = m.entero(0)
                val gx = i % maze.gw
                val gy = i / maze.gw
                if (maze.isSolid(gx, gy)) {
                    maze.setSolid(gx, gy, false)
                    if (!maze.isSolid(gx, gy)) {
                        geometryDirty = true
                        maze.refreshSolution()
                    }
                }
            }
            NetProtocol.Tipo.TRAMPA -> {
                // Una trampa que ya salto otro queda descubierta y descargada
                // para todos: es la misma trampa.
                val i = m.entero(0)
                for (t in traps) {
                    if (indiceDe(t.gx, t.gy) == i) { t.revealed = true; t.cooldown = 2.5f; break }
                }
            }
            NetProtocol.Tipo.REVIVIR -> {
                // Si todavia no cumpliste el tiempo en el piso, el pedido se
                // ignora: el companiero lo va a repetir mientras siga al lado.
                if (m.arg(0) == red?.yo && caido && esperaLevantada <= 0f) levantarme()
            }
            NetProtocol.Tipo.LLEGADA -> {
                if (red?.match?.modo == NetProtocol.Modo.COOPERATIVO && phase == Phase.JUGANDO) {
                    toast("${red?.match?.jugador(m.de)?.nombre ?: "Tu companiero"} salio")
                }
            }
            else -> Unit
        }
    }

    /**
     * Cooperativo: al acercarte a un caido, lo levantas.
     *
     * Aca solo se PIDE: el que decide si se levanta es el propio caido, que
     * es el unico que sabe cuanto lleva en el piso. Si se marcara aca que ya
     * esta levantado, los dos telefonos quedarian contando cosas distintas.
     */
    private fun levantarCaidosCerca(r: MatchLink) {
        if (recargaRevivir > 0f) return
        for (j in r.match.otros()) {
            if (!j.caido || j.sinPose) continue
            if (hypot(j.x - posX, j.z - posZ) > RADIO_LEVANTAR) continue
            r.avisarRevivir(j.id)
            recargaRevivir = 0.5f
            break
        }
    }

    /** Te levantaron: volves a la vida con media barra. */
    private fun levantarme() {
        caido = false
        health = max(health, stats.maxHealth * 0.5f)
        // Avisar que ya estas de pie: el que te levanto no da por hecho que
        // funciono, justamente porque podias estar todavia sin poder.
        red?.avisarDePie()
        toast("Te levantaron!")
        play(Sfx.USAR)
    }

    /** Ya llegaste a la salida (en carrera, el que llega primero gana). */
    private var llegaste = false

    /**
     * Segundos que faltan para que te puedan levantar.
     *
     * Sin esta espera el cooperativo no tiene sentido: dos que van pegados se
     * levantan en el mismo frame en que caen y no pierden nunca. Con unos
     * segundos en el piso, caer cuesta algo y hay que ir a buscar al otro.
     */
    private var esperaLevantada = 0f

    /** Para no mandar un pedido de levantada en cada frame. */
    private var recargaRevivir = 0f

    /** Cuanto se queda uno en el piso antes de que lo puedan levantar. */
    private val ESPERA_LEVANTADA = 2.5f

    /** Cerca de cuantos metros hay que estar para levantar a un caido. */
    private val RADIO_LEVANTAR = 1.5f

    /** Segundos que te faltan tirado antes de que te puedan levantar. */
    fun esperaParaLevantarte(): Float = max(0f, esperaLevantada)

    // --------------------------------------------------------- fin de nivel

    private fun win() {
        if (phase != Phase.JUGANDO) return
        llegaste = true
        red?.avisarLlegada(elapsedMs)
        phase = Phase.GANADO
        guardarExploracion()
        play(Sfx.GANAR)
    }

    private fun lose() {
        if (phase != Phase.JUGANDO) return
        val r = red
        // En cooperativo no se pierde de a uno: quedas caido, seguis viendo, y
        // un companiero que se te acerque te levanta. Si caen todos, ahi si se
        // termina (lo resuelve pasoDeRed, que es quien ve el estado de todos).
        if (r != null && r.match.modo == NetProtocol.Modo.COOPERATIVO && !caido) {
            caido = true
            health = 0f
            velX = 0f; velZ = 0f
            esperaLevantada = ESPERA_LEVANTADA
            r.avisarCaido()
            toast("Caiste. Esperá que te levanten")
            play(Sfx.PERDER)
            return
        }
        phase = Phase.PERDIDO
        health = 0f
        guardarExploracion()
        play(Sfx.PERDER)
    }

    /** Poder Memoria de la Sima: deja escrito lo que exploraste de este nivel. */
    private fun guardarExploracion() {
        if (stats.mapaPersistente) save.rememberExplored(level, revealed)
    }

    /**
     * Abandonar el nivel a proposito, desde el menu de pausa.
     *
     * No pasa por [lose]: en cooperativo esa via te deja caido esperando que
     * te levanten, con lo cual el boton "Abandonar" no abandonaba nada.
     */
    fun forceLose() {
        if (phase != Phase.JUGANDO) return
        caido = false
        phase = Phase.PERDIDO
        health = 0f
        guardarExploracion()
        play(Sfx.PERDER)
    }

    /** Recompensa final. Se llama una sola vez desde la pantalla de resultado. */
    class Reward(
        val baseEcos: Int, val timeBonus: Int, val exploreBonus: Int,
        val yieldMultiplier: Float, val totalEcos: Int, val vetagris: Int
    )

    fun computeReward(): Reward {
        val base = ecosCollected
        val parSeconds = 22f + maze.solutionLength * 1.35f
        val secs = elapsedMs / 1000f
        val timeBonus = if (secs < parSeconds) ((parSeconds - secs) * 1.6f).toInt().coerceAtMost(400) else 0
        val explored = revealed.count { it }.toFloat() / max(1, maze.openCount() + maze.gw)
        val exploreBonus = (explored * 60f).toInt()
        val levelBonus = 18 + level * 6
        val mult = stats.ecoYield
        val total = ((base + timeBonus + exploreBonus + levelBonus) * mult).toInt()
        var vtg = vetagrisCollected
        if (stats.vetagrisIncome && save.levelsSinceVetagris >= 4) vtg += 1
        return Reward(base, timeBonus, exploreBonus, mult, total, vtg)
    }

    /** Guarda el resultado en el perfil. Idempotente por partida. */
    private var settled = false
    fun settle(): Reward? {
        if (settled) return null
        settled = true
        val r = computeReward()
        return if (phase == Phase.GANADO) {
            save.addEcos(r.totalEcos)
            if (r.vetagris > 0) save.addVetagris(r.vetagris)
            // En una sala el nivel lo elige el anfitrion, y puede estar muy
            // por encima del tuyo. Ganarlo cuenta como victoria y te avanza
            // uno, pero no te regala de golpe los veinte niveles que no
            // jugaste: el progreso sigue siendo tuyo, no del que te invito.
            val acreditable = if (red != null) level.coerceAtMost(save.maxLevel) else level
            save.onLevelCompleted(acreditable, elapsedMs, steps)
            if (stats.vetagrisIncome) save.consumeVetagrisCounter()
            r
        } else {
            // Al perder te llevas la mitad de lo que juntaste en el nivel.
            val half = (ecosCollected * 0.5f).toInt()
            save.addEcos(half)
            save.onRunFailed(elapsedMs)
            Reward(ecosCollected, 0, 0, 1f, half, 0)
        }
    }

    /** Altura del ojo en el mundo: relieve + postura + cabeceo. */
    fun alturaCamara(): Float = posY + alturaOjoSuave + headBobOffset()

    fun headBobOffset(): Float =
        sin(bobPhase.toDouble()).toFloat() * 0.045f * save.settings.headBob
}

