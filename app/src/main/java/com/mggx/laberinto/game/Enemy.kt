package com.mggx.laberinto.game

import com.mggx.laberinto.maze.Maze
import com.mggx.laberinto.maze.MazeGenerator
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Un bicho de la mina.
 *
 * La cabeza la pone [EnemyBrain]: aca solo vive el estado. El movimiento es en
 * metros del mundo, no en casillas, asi que se mueven suave y no a saltos.
 */
class Enemy(
    val kind: MazeGenerator.EnemyKind,
    var x: Float,
    var z: Float,
    /** Casilla donde nacio: es a donde vuelve cuando te pierde. */
    val nidoGx: Int,
    val nidoGy: Int,
    /** Desfase de animacion para que no se muevan todos igual. */
    val fase: Float
) {
    /** Te esta persiguiendo. */
    var alerta: Boolean = false
    /** Segundos que le quedan de interes antes de volver a lo suyo. */
    var interes: Float = 0f
    /** Segundos hasta que pueda volver a pegarte. */
    var recarga: Float = 0f
    /** A donde va cuando no te persigue. */
    var vagarGx: Int = nidoGx
    var vagarGy: Int = nidoGy
    /** Altura del cuerpo sobre el piso (el murcielago vuela). */
    var altura: Float = 0f
    /** Para la animacion del renderer: cuanto se mueve. */
    var paso: Float = 0f
    /** Hacia donde mira, en radianes (0 = +Z), para dibujarlo derecho. */
    var rumbo: Float = 0f

    val vuela: Boolean get() = kind == MazeGenerator.EnemyKind.MURCIELAGO

    fun distanciaA(px: Float, pz: Float): Float = hypot(px - x, pz - z)
}

/**
 * La inteligencia de los bichos, aparte de la sesion para poder probarla sola.
 *
 * Persiguen por el campo de distancias del laberinto (BFS desde el jugador),
 * asi que doblan bien en las esquinas en vez de quedarse trabados contra la
 * pared apuntando en linea recta.
 */
class EnemyBrain(private val maze: Maze, seed: Long) {

    private val rnd = Random(seed xor 0x5DEECE66DL)
    private var campo: IntArray? = null
    private var campoDesde: Int = -1
    private var refresco: Float = 0f

    /** Cada cuanto se recalcula el campo de distancias, en segundos. */
    private val PERIODO = 0.30f

    /**
     * Un paso de simulacion.
     *
     * [detectable] dice si el jugador se puede ver ahora mismo (el sigilo y la
     * postura entran por aca), [ruidoso] si esta corriendo, y [muerde] se llama
     * cuando un bicho llega a tocarlo.
     */
    fun update(
        dt: Float,
        enemigos: List<Enemy>,
        px: Float, pz: Float, pRadio: Float,
        alcanceEscala: Float,
        detectable: Boolean,
        ruidoso: Boolean,
        cell: Float,
        pisoDe: (Int, Int) -> Float,
        muerde: (Enemy) -> Unit
    ) {
        if (enemigos.isEmpty()) return

        val pgx = (px / cell).toInt().coerceIn(0, maze.gw - 1)
        val pgy = (pz / cell).toInt().coerceIn(0, maze.gh - 1)
        val pIdx = maze.index(pgx, pgy)

        refresco -= dt
        if (refresco <= 0f || campo == null || campoDesde != pIdx) {
            refresco = PERIODO
            campoDesde = pIdx
            campo = maze.bfsDistances(pgx, pgy)
        }
        val d = campo!!

        for (e in enemigos) {
            if (e.recarga > 0f) e.recarga -= dt

            val dist = e.distanciaA(px, pz)
            val alcance = e.kind.alcance * alcanceEscala
            val loNota = detectable && dist <= alcance &&
                (!e.kind.soloOye || ruidoso || dist <= alcance * 0.35f)

            if (loNota) {
                e.alerta = true
                e.interes = if (e.kind.guardian) 2.5f else 4.5f
            } else if (e.interes > 0f) {
                e.interes -= dt
                if (e.interes <= 0f) e.alerta = false
            }

            // El guardian no sale de su pedazo de cueva.
            val lejosDelNido = hypot(
                (e.nidoGx + 0.5f) * cell - e.x,
                (e.nidoGy + 0.5f) * cell - e.z
            )
            if (e.kind.guardian && lejosDelNido > cell * 2.2f) e.alerta = false

            val objetivo = if (e.alerta) siguientePaso(e, d, cell, px, pz) else vagar(e, cell)
            if (objetivo != null) {
                mover(e, objetivo.first, objetivo.second, e.kind.velocidad * dt, cell)
            }

            // Altura: el murcielago vuela a media altura, el resto pisa el piso.
            val gx = (e.x / cell).toInt().coerceIn(0, maze.gw - 1)
            val gy = (e.z / cell).toInt().coerceIn(0, maze.gh - 1)
            e.altura = pisoDe(gx, gy) + if (e.vuela) 1.45f else 0f

            if (dist <= pRadio + e.kind.radio + 0.18f && e.recarga <= 0f) {
                e.recarga = e.kind.recarga
                muerde(e)
            }
        }
    }

    /**
     * Centro de la casilla vecina que acerca al jugador. Cuando ya lo tiene
     * pegado (misma casilla o la de al lado) va derecho a el: si siguiera
     * yendo a centros de casilla se quedaria dando vueltas sin alcanzarlo.
     */
    private fun siguientePaso(
        e: Enemy, d: IntArray, cell: Float, px: Float, pz: Float
    ): Pair<Float, Float>? {
        val gx = (e.x / cell).toInt().coerceIn(0, maze.gw - 1)
        val gy = (e.z / cell).toInt().coerceIn(0, maze.gh - 1)
        val aca = d[maze.index(gx, gy)]
        if (aca < 0) return null
        if (aca <= 1) return px to pz
        var mejorX = -1; var mejorY = -1; var mejor = aca
        for (k in 0 until 4) {
            val nx = gx + DX[k]; val ny = gy + DY[k]
            if (!maze.inBounds(nx, ny) || maze.isSolid(nx, ny)) continue
            val v = d[maze.index(nx, ny)]
            if (v in 0 until mejor) { mejor = v; mejorX = nx; mejorY = ny }
        }
        if (mejorX < 0) return null
        return (mejorX + 0.5f) * cell to (mejorY + 0.5f) * cell
    }

    /** Paseo tranquilo entre casillas cercanas al nido. */
    private fun vagar(e: Enemy, cell: Float): Pair<Float, Float>? {
        val tx = (e.vagarGx + 0.5f) * cell
        val tz = (e.vagarGy + 0.5f) * cell
        if (hypot(tx - e.x, tz - e.z) > 0.35f) return tx to tz

        // Llego: elige otra casilla vecina abierta, o vuelve al nido si se fue lejos.
        val gx = (e.x / cell).toInt().coerceIn(0, maze.gw - 1)
        val gy = (e.z / cell).toInt().coerceIn(0, maze.gh - 1)
        val lejos = kotlin.math.abs(gx - e.nidoGx) + kotlin.math.abs(gy - e.nidoGy)
        if (lejos > 6) { e.vagarGx = e.nidoGx; e.vagarGy = e.nidoGy; return null }
        val opciones = ArrayList<Int>(4)
        for (k in 0 until 4) {
            val nx = gx + DX[k]; val ny = gy + DY[k]
            if (maze.inBounds(nx, ny) && !maze.isSolid(nx, ny)) opciones.add(maze.index(nx, ny))
        }
        if (opciones.isEmpty()) return null
        val elegido = opciones[rnd.nextInt(opciones.size)]
        e.vagarGx = elegido % maze.gw
        e.vagarGy = elegido / maze.gw
        return null
    }

    /** Avanza hacia un punto sin meterse en la roca. */
    private fun mover(e: Enemy, tx: Float, tz: Float, paso: Float, cell: Float) {
        val dx = tx - e.x
        val dz = tz - e.z
        val len = hypot(dx, dz)
        if (len < 1e-4f) return
        val ux = dx / len * minOf(paso, len)
        val uz = dz / len * minOf(paso, len)
        if (!chocaRoca(e.x + ux, e.z, e.kind.radio, cell)) e.x += ux
        if (!chocaRoca(e.x, e.z + uz, e.kind.radio, cell)) e.z += uz
        e.paso += minOf(paso, len)
        // Gira suave hacia donde va, para que no pegue latigazos de 180 grados.
        val deseado = kotlin.math.atan2(dx, dz)
        var delta = deseado - e.rumbo
        while (delta > Math.PI) delta -= (2.0 * Math.PI).toFloat()
        while (delta < -Math.PI) delta += (2.0 * Math.PI).toFloat()
        e.rumbo += delta * 0.25f
    }

    private fun chocaRoca(x: Float, z: Float, r: Float, cell: Float): Boolean {
        val gx0 = ((x - r) / cell).toInt()
        val gx1 = ((x + r) / cell).toInt()
        val gy0 = ((z - r) / cell).toInt()
        val gy1 = ((z + r) / cell).toInt()
        for (gy in gy0..gy1) {
            for (gx in gx0..gx1) {
                if (!maze.isSolid(gx, gy)) continue
                val minX = gx * cell
                val minZ = gy * cell
                val nx = x.coerceIn(minX, minX + cell)
                val nz = z.coerceIn(minZ, minZ + cell)
                val ddx = x - nx
                val ddz = z - nz
                if (ddx * ddx + ddz * ddz < r * r) return true
            }
        }
        return false
    }

    private companion object {
        val DX = intArrayOf(1, -1, 0, 0)
        val DY = intArrayOf(0, 0, 1, -1)
    }
}
