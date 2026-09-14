package com.mggx.laberinto

import com.mggx.laberinto.maze.Maze
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Un visor para MIRAR el relieve de una cueva entera.
 *
 * Por que existe: es el mismo agujero que tenian los modelos antes de
 * [VisorDeMallas]. El relieve —las terrazas, los pozos, los techos bajos— se
 * generaba a ciegas y solo se verificaba por reglas ("ninguna transicion queda
 * imposible"). Eso agarra los bloqueos, pero no dice nada sobre lo unico que
 * importa cuando uno esta adentro: si la cueva tiene FORMA o es ruido.
 *
 * Dibuja dos cosas:
 *
 *  - la PLANTA, con la altura del piso en colores, para ver si las alturas se
 *    agrupan en zonas o saltan de casilla en casilla; y
 *  - el CORTE a lo largo del camino a la salida, con el piso y el techo, que
 *    es literalmente lo que va a ver el jugador mientras camina.
 */
object VisorDeRelieve {

    /** Cuantos pixeles por casilla en la planta. */
    private const val LADO = 7

    // -------------------------------------------------------------- colores

    /** Rampa de altura: azul hondo -> verde -> naranja -> blanco. */
    private fun rampa(t: Float): Triple<Float, Float, Float> {
        val u = t.coerceIn(0f, 1f)
        val paradas = arrayOf(
            floatArrayOf(0.12f, 0.20f, 0.42f),
            floatArrayOf(0.15f, 0.42f, 0.45f),
            floatArrayOf(0.30f, 0.58f, 0.32f),
            floatArrayOf(0.72f, 0.60f, 0.26f),
            floatArrayOf(0.92f, 0.86f, 0.76f)
        )
        val x = u * (paradas.size - 1)
        val i = min(x.toInt(), paradas.size - 2)
        val f = x - i
        val a = paradas[i]; val b = paradas[i + 1]
        return Triple(
            a[0] + (b[0] - a[0]) * f,
            a[1] + (b[1] - a[1]) * f,
            a[2] + (b[2] - a[2]) * f
        )
    }

    private fun bloque(
        l: VisorDeMallas.Lienzo, px: Int, py: Int, lado: Int,
        r: Float, g: Float, b: Float
    ) {
        for (y in 0 until lado) for (x in 0 until lado) {
            l.pixel(px + x, py + y, -1f, r, g, b)
        }
    }

    // --------------------------------------------------------------- planta

    /**
     * La planta del nivel con la altura del piso en colores.
     *
     * Marcas: las escaleras llevan un punto claro, los tramos donde hay que
     * agacharse van rayados, el agua tira el color hacia el azul, y el inicio
     * y la salida llevan una cruz.
     */
    fun planta(maze: Maze, l: VisorDeMallas.Lienzo, ox: Int, oy: Int) {
        var minN = Int.MAX_VALUE
        var maxN = Int.MIN_VALUE
        for (i in 0 until maze.gw * maze.gh) {
            if (maze.solid[i]) continue
            minN = min(minN, maze.floorLevel[i])
            maxN = max(maxN, maze.floorLevel[i])
        }
        val rango = max(1, maxN - minN)

        for (gy in 0 until maze.gh) {
            for (gx in 0 until maze.gw) {
                val px = ox + gx * LADO
                val py = oy + gy * LADO
                if (maze.isSolid(gx, gy)) {
                    bloque(l, px, py, LADO, 0.10f, 0.10f, 0.12f)
                    continue
                }
                val i = maze.index(gx, gy)
                var (r, g, b) = rampa((maze.floorLevel[i] - minN).toFloat() / rango)
                if (maze.hayAgua(gx, gy)) { r *= 0.45f; g *= 0.70f; b = min(1f, b * 1.5f + 0.18f) }
                bloque(l, px, py, LADO, r, g, b)

                // Tramo bajo: rayado. Es lo que mas se siente al caminar.
                if (Maze.altoLibreReal(maze.ceilClearance[i]) < 1.9f) {
                    for (k in 0 until LADO step 2) {
                        for (y in 0 until LADO) {
                            l.pixel(px + ((k + y) % LADO), py + y, -2f, 0.05f, 0.05f, 0.06f)
                        }
                    }
                }
                // Escalera: punto claro en el medio.
                if (maze.ladder[i]) {
                    bloque(l, px + LADO / 2 - 1, py + LADO / 2 - 1, 2, 1f, 0.95f, 0.55f)
                }
            }
        }
        cruz(l, ox + maze.startGx * LADO, oy + maze.startGy * LADO, 0.35f, 1f, 0.45f)
        cruz(l, ox + maze.exitGx * LADO, oy + maze.exitGy * LADO, 1f, 0.35f, 0.9f)
    }

    private fun cruz(l: VisorDeMallas.Lienzo, px: Int, py: Int, r: Float, g: Float, b: Float) {
        for (k in 0 until LADO) {
            l.pixel(px + k, py + LADO / 2, -3f, r, g, b)
            l.pixel(px + LADO / 2, py + k, -3f, r, g, b)
        }
    }

    // ---------------------------------------------------------------- corte

    /**
     * El corte a lo largo del camino a la salida: piso abajo, techo arriba.
     *
     * Es la vista que mas dice, porque es la unica que muestra lo que el
     * jugador va a recorrer de verdad. Una cueva con relieve bueno tiene aca
     * pendientes largas y salones altos; una con relieve de ruido tiene
     * dientes de sierra de una casilla.
     */
    fun corte(maze: Maze, l: VisorDeMallas.Lienzo, ox: Int, oy: Int, ancho: Int, alto: Int) {
        val camino = maze.solutionPath
        if (camino.isEmpty()) return
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in camino) {
            val gx = i % maze.gw; val gy = i / maze.gw
            minY = min(minY, maze.floorY(gx, gy))
            maxY = max(maxY, maze.ceilY(gx, gy))
        }
        if (maxY - minY < 0.5f) maxY = minY + 0.5f
        val escala = (alto - 4) / (maxY - minY)

        fun fila(y: Float): Int = oy + alto - 2 - ((y - minY) * escala).roundToInt()

        for (k in camino.indices) {
            val gx = camino[k] % maze.gw
            val gy = camino[k] / maze.gw
            val x0 = ox + k * ancho / camino.size
            val x1 = ox + (k + 1) * ancho / camino.size
            val piso = fila(maze.floorY(gx, gy))
            val techo = fila(maze.ceilY(gx, gy))
            val agachado = Maze.altoLibreReal(maze.ceilClearance[maze.index(gx, gy)]) < 1.9f
            for (x in x0 until max(x1, x0 + 1)) {
                // La roca de arriba y la de abajo.
                for (y in piso until oy + alto) l.pixel(x, y, -1f, 0.26f, 0.23f, 0.20f)
                for (y in oy until techo) l.pixel(x, y, -1f, 0.17f, 0.16f, 0.18f)
                // El hueco por donde se pasa.
                for (y in techo until piso) {
                    val c = if (agachado) 0.30f else 0.06f
                    l.pixel(x, y, -1f, c, c * 0.85f, c * 0.6f)
                }
                if (maze.hasLadder(gx, gy)) {
                    for (y in max(oy, piso - 10) until piso step 3) {
                        l.pixel(x, y, -2f, 1f, 0.9f, 0.45f)
                    }
                }
                l.pixel(x, piso, -2f, 0.62f, 0.56f, 0.46f)
                l.pixel(x, techo, -2f, 0.42f, 0.40f, 0.46f)
            }
        }
    }

    // ---------------------------------------------------------------- hoja

    /** Una hoja con la planta y el corte de cada nivel pedido. */
    fun hoja(niveles: List<Pair<String, Maze>>, nombre: String): File {
        val anchoPlanta = niveles.maxOf { it.second.gw } * LADO
        val altoPlanta = niveles.maxOf { it.second.gh } * LADO
        val altoCorte = 92
        val margen = 10
        val ancho = max(anchoPlanta, 520) + margen * 2
        val altoFila = altoPlanta + altoCorte + margen * 2
        val l = VisorDeMallas.Lienzo(ancho, altoFila * niveles.size)
        l.fondo(20, 21, 24)
        for ((fila, par) in niveles.withIndex()) {
            val oy = fila * altoFila + margen
            planta(par.second, l, margen, oy)
            corte(par.second, l, margen, oy + altoPlanta + margen, ancho - margen * 2, altoCorte)
            for (x in 0 until ancho) l.pixel(x, fila * altoFila, -4f, 0.30f, 0.31f, 0.35f)
        }
        return VisorDeMallas.guardar(l, nombre)
    }

    /**
     * Cuanto se parecen las alturas de dos casillas que estan CERCA EN EL
     * ESPACIO, aunque no esten conectadas.
     *
     * Es la medida de si la cueva tiene terreno o fideos. Con la altura
     * propagada por los pasillos, dos galerias separadas por una sola pared
     * podian quedar a varios metros de desnivel, porque el relieve habia
     * llegado a cada una por un camino distinto. Con un campo de terreno, dos
     * casillas vecinas en el espacio leen casi el mismo numero.
     *
     * Devuelve el desnivel medio, en escalones, entre pares de casillas
     * abiertas a distancia [radio] que NO son vecinas directas.
     */
    fun desnivelEntreVecinasDePared(maze: Maze, radio: Int = 2): Float {
        var suma = 0L
        var pares = 0L
        for (gy in 0 until maze.gh) for (gx in 0 until maze.gw) {
            if (maze.isSolid(gx, gy)) continue
            val a = maze.floorLevel[maze.index(gx, gy)]
            for (dy in -radio..radio) for (dx in -radio..radio) {
                if (abs(dx) + abs(dy) <= 1) continue          // ella misma o vecina directa
                val nx = gx + dx; val ny = gy + dy
                if (!maze.inBounds(nx, ny) || maze.isSolid(nx, ny)) continue
                suma += abs(maze.floorLevel[maze.index(nx, ny)] - a)
                pares++
            }
        }
        return if (pares == 0L) 0f else suma.toFloat() / pares
    }

    /** Que fraccion de la cueva obliga a agacharse. */
    fun fraccionDeGatera(maze: Maze): Float {
        var bajas = 0; var n = 0
        for (i in 0 until maze.gw * maze.gh) {
            if (maze.solid[i]) continue
            n++
            if (Maze.altoLibreReal(maze.ceilClearance[i]) < 1.9f) bajas++
        }
        return if (n == 0) 0f else bajas.toFloat() / n
    }

    /** Cuanto salta el piso entre casillas vecinas, en escalones. */
    fun saltosEntreVecinas(maze: Maze): IntArray {
        val cuenta = IntArray(24)
        for (gy in 0 until maze.gh) for (gx in 0 until maze.gw) {
            if (maze.isSolid(gx, gy)) continue
            for (d in intArrayOf(0, 1)) {
                val nx = gx + if (d == 0) 1 else 0
                val ny = gy + if (d == 0) 0 else 1
                if (!maze.inBounds(nx, ny) || maze.isSolid(nx, ny)) continue
                val s = abs(maze.floorLevel[maze.index(nx, ny)] - maze.floorLevel[maze.index(gx, gy)])
                cuenta[min(s, cuenta.size - 1)]++
            }
        }
        return cuenta
    }
}
