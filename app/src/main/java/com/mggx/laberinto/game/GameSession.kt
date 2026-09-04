package com.mggx.laberinto.game

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.maze.CaveTheme
import com.mggx.laberinto.maze.Maze
import com.mggx.laberinto.maze.MazeGenerator
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
    seed: Long = System.nanoTime()
) {
    companion object {
        /** Lado de una casilla de la grilla, en metros. */
        const val CELL = 3.0f
        const val WALL_HEIGHT = 3.4f
        const val EYE_HEIGHT = 1.62f
        const val PLAYER_RADIUS = 0.34f
        const val EXIT_RADIUS = 1.25f
        /** Vueltas por segundo del giro con mando, en grados. */
        const val PAD_LOOK_DEG = 165f
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
    private var hurtCooldown = 0f
    private var sinceDamage = 0f
    private var bobPhase = 0f
    private var stepAccum = 0f
    private var runBurstLeft = stats.startBurstSeconds

    /** Sonidos pendientes que el motor de audio va a consumir este frame. */
    private val soundQueue = ArrayList<Sfx>()
    enum class Sfx { PASO, ECO, ECO_GRANDE, VETAGRIS, COFRE, TRAMPA, DANO, USAR, ROMPER, GANAR, PERDER, MARCA, ZUMBIDO }
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

        // Orientacion inicial: mirando hacia el pasillo abierto.
        yawDeg = when {
            maze.isOpen(maze.startGx + 1, maze.startGy) -> 90f
            maze.isOpen(maze.startGx, maze.startGy + 1) -> 180f
            maze.isOpen(maze.startGx - 1, maze.startGy) -> 270f
            else -> 0f
        }

        // Revelado inicial por la mejora Cartografo Innato.
        if (stats.startMapReveal > 0f) revealFraction(stats.startMapReveal)
        revealAround(maze.startGx, maze.startGy, 3)
        if (stats.exitPingSeconds > 0f) exitPingTimer = stats.exitPingSeconds
        if (stats.freeSonarSeconds > 0f) freeSonarTimer = stats.freeSonarSeconds
        markWalked()
    }

    // ------------------------------------------------------------ consultas

    val exitWorldX: Float get() = (maze.exitGx + 0.5f) * CELL
    val exitWorldZ: Float get() = (maze.exitGy + 0.5f) * CELL

    fun distanceToExit(): Float = hypot(exitWorldX - posX, exitWorldZ - posZ)

    /** Angulo (grados) desde el frente del jugador hacia la salida. */
    fun bearingToExit(): Float {
        val ang = Math.toDegrees(atan2((exitWorldX - posX).toDouble(), (exitWorldZ - posZ).toDouble())).toFloat()
        var d = ang - yawDeg
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
        var running: Boolean = false
    )

    fun pause() { if (phase == Phase.JUGANDO) phase = Phase.PAUSA }
    fun resume() { if (phase == Phase.PAUSA) phase = Phase.JUGANDO }

    fun update(dtRaw: Float, input: Input) {
        if (phase != Phase.JUGANDO) return
        val dt = dtRaw.coerceIn(0f, 0.05f)   // techo anti-saltos tras un lag

        effects.update(dt)
        if (runBurstLeft > 0f) runBurstLeft -= dt
        if (phaseWindow > 0f) phaseWindow -= dt
        if (hurtCooldown > 0f) hurtCooldown -= dt
        sinceDamage += dt

        // --- cronometro (se puede congelar)
        if (effects.isActive(EffectType.CONGELAR_RELOJ)) timerFrozen = effects.remaining(EffectType.CONGELAR_RELOJ)
        if (timerFrozen > 0f) timerFrozen -= dt else elapsedMs += (dt * 1000f).toLong()

        // --- camara
        val lookMul = effects.multiplier(EffectType.GIRO_RAPIDO)
        yawDeg = normalizeAngle(yawDeg + input.lookX * lookMul)
        pitchDeg = (pitchDeg + input.lookY * lookMul).coerceIn(-82f, 82f)

        // --- movimiento
        moveStep(dt, input)

        // --- regeneracion / desgaste
        if (stats.regenPerSecond > 0f && sinceDamage > 2.5f && health < stats.maxHealth) {
            health = min(stats.maxHealth, health + stats.regenPerSecond * dt)
        }

        // --- reliquias con temporizador
        if (stats.exitPingSeconds > 0f) {
            exitPingTimer -= dt
            if (exitPingTimer <= 0f) { exitPingTimer = stats.exitPingSeconds; exitPingFlash = 2.2f }
        }
        if (exitPingFlash > 0f) exitPingFlash -= dt
        if (stats.freeSonarSeconds > 0f && freeSonarTimer > 0f) freeSonarTimer -= dt

        // --- rastro
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

        // --- llegada
        if (hypot(exitWorldX - posX, exitWorldZ - posZ) <= EXIT_RADIUS) win()
    }

    private fun normalizeAngle(a: Float): Float {
        var x = a
        while (x >= 360f) x -= 360f
        while (x < 0f) x += 360f
        return x
    }

    private fun moveStep(dt: Float, input: Input) {
        var speed = stats.walkSpeed
        speed *= effects.multiplier(EffectType.VELOCIDAD)
        if (runBurstLeft > 0f) speed *= (1f + stats.startBurstSpeed)

        val wantsRun = (input.running || save.settings.autoRun) && stamina > 1f
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
        // Derecha = frente rotado 90 grados
        val rx = fz
        val rz = -fx

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
            if (!collides(posX + stepX, posZ)) posX += stepX else velX = 0f
            if (!collides(posX, posZ + stepZ)) posZ += stepZ else velZ = 0f
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

    // --------------------------------------------------------- fin de nivel

    private fun win() {
        if (phase != Phase.JUGANDO) return
        phase = Phase.GANADO
        play(Sfx.GANAR)
    }

    private fun lose() {
        if (phase != Phase.JUGANDO) return
        phase = Phase.PERDIDO
        health = 0f
        play(Sfx.PERDER)
    }

    fun forceLose() { lose() }

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
            save.onLevelCompleted(level, elapsedMs, steps)
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

    fun headBobOffset(): Float =
        sin(bobPhase.toDouble()).toFloat() * 0.045f * save.settings.headBob
}
