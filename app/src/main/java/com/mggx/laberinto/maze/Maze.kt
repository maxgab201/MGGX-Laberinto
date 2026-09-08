package com.mggx.laberinto.maze

/**
 * Laberinto en "grilla expandida": cada celda logica ocupa 2 casillas + 1 de pared,
 * asi que el tablero solido mide (cols*2+1) x (rows*2+1).
 *
 * solid[i] == true  -> roca (pared)
 * solid[i] == false -> aire (transitable)
 */
class Maze(val cols: Int, val rows: Int) {

    val gw: Int = cols * 2 + 1
    val gh: Int = rows * 2 + 1

    /** Mapa de solidez en coordenadas de grilla expandida. */
    val solid = BooleanArray(gw * gh) { true }

    // ------------------------------------------------------------- relieve
    //
    // La cueva tiene alturas. Para que el nivel SIEMPRE se pueda pasar, el
    // relieve se arma de forma que ninguna transicion pueda bloquear:
    //  - entre casillas vecinas el piso cambia como mucho un escalon, que se
    //    sube caminando;
    //  - donde hay un desnivel grande siempre hay una escalera;
    //  - el techo puede bajar hasta obligar a agacharse o arrastrarse, pero
    //    nunca por debajo de lo que entra arrastrandose.
    // Asi la conectividad sigue siendo exactamente la del mapa de solidez.

    /** Altura del piso de cada casilla, en escalones. */
    val floorLevel = IntArray(gw * gh)

    /** Metros libres entre el piso y el techo de cada casilla. */
    val ceilClearance = FloatArray(gw * gh) { ALTO_NORMAL }

    /** Casillas con escalera: se puede subir y bajar por ellas. */
    val ladder = BooleanArray(gw * gh)

    fun floorY(gx: Int, gy: Int): Float =
        if (inBounds(gx, gy)) floorLevel[index(gx, gy)] * ESCALON else 0f

    fun ceilY(gx: Int, gy: Int): Float =
        floorY(gx, gy) + if (inBounds(gx, gy)) ceilClearance[index(gx, gy)] else ALTO_NORMAL

    fun hasLadder(gx: Int, gy: Int): Boolean =
        inBounds(gx, gy) && ladder[index(gx, gy)]

    /** Celda logica de inicio y de salida. */
    var startCol = 0
    var startRow = 0
    var exitCol = 0
    var exitRow = 0

    /** Largo del camino minimo (en casillas de grilla) entre inicio y salida. */
    var solutionLength = 0
        internal set

    /** Camino minimo inicio -> salida en coordenadas de grilla expandida. */
    var solutionPath: List<Int> = emptyList()
        internal set

    // ---------------------------------------------------------------- helpers

    fun index(gx: Int, gy: Int): Int = gy * gw + gx

    fun inBounds(gx: Int, gy: Int): Boolean = gx in 0 until gw && gy in 0 until gh

    fun isSolid(gx: Int, gy: Int): Boolean {
        if (!inBounds(gx, gy)) return true
        return solid[index(gx, gy)]
    }

    fun isOpen(gx: Int, gy: Int): Boolean = !isSolid(gx, gy)

    fun setSolid(gx: Int, gy: Int, value: Boolean) {
        if (!inBounds(gx, gy)) return
        // El borde exterior nunca se abre: mantiene la cueva cerrada.
        if (gx == 0 || gy == 0 || gx == gw - 1 || gy == gh - 1) return
        solid[index(gx, gy)] = value
    }

    companion object {
        /** Cuanto sube un escalon de piso, en metros. */
        const val ESCALON = 0.42f

        /** Alto libre de un tramo normal, donde se camina de pie. */
        const val ALTO_NORMAL = 3.4f

        /**
         * Un escalon se sube caminando. Mas que esto ya necesita escalera, y
         * el generador se encarga de que siempre haya una.
         */
        const val SUBIDA_CAMINANDO = ESCALON + 0.06f
    }

    val startGx: Int get() = startCol * 2 + 1
    val startGy: Int get() = startRow * 2 + 1
    val exitGx: Int get() = exitCol * 2 + 1
    val exitGy: Int get() = exitRow * 2 + 1

    /** Cantidad de casillas transitables. */
    fun openCount(): Int = solid.count { !it }

    // ---------------------------------------------------------------- caminos

    /**
     * BFS sobre la grilla expandida. Devuelve el array de distancias
     * (-1 = inalcanzable) partiendo de (sx, sy).
     */
    fun bfsDistances(sx: Int, sy: Int): IntArray {
        val dist = IntArray(gw * gh) { -1 }
        if (isSolid(sx, sy)) return dist
        val queue = IntArray(gw * gh)
        var head = 0
        var tail = 0
        val s = index(sx, sy)
        dist[s] = 0
        queue[tail++] = s
        while (head < tail) {
            val cur = queue[head++]
            val cx = cur % gw
            val cy = cur / gw
            val d = dist[cur] + 1
            // 4-vecinos
            neighbor(cx + 1, cy, d, dist, queue, tail).let { tail = it }
            neighbor(cx - 1, cy, d, dist, queue, tail).let { tail = it }
            neighbor(cx, cy + 1, d, dist, queue, tail).let { tail = it }
            neighbor(cx, cy - 1, d, dist, queue, tail).let { tail = it }
        }
        return dist
    }

    private fun neighbor(nx: Int, ny: Int, d: Int, dist: IntArray, queue: IntArray, tail: Int): Int {
        if (!inBounds(nx, ny)) return tail
        val ni = index(nx, ny)
        if (solid[ni] || dist[ni] != -1) return tail
        dist[ni] = d
        queue[tail] = ni
        return tail + 1
    }

    /** true si existe al menos un camino entre inicio y salida. */
    fun isSolvable(): Boolean {
        if (isSolid(startGx, startGy) || isSolid(exitGx, exitGy)) return false
        val dist = bfsDistances(startGx, startGy)
        return dist[index(exitGx, exitGy)] >= 0
    }

    /** Reconstruye el camino minimo inicio -> salida. Vacio si no hay solucion. */
    fun shortestPath(): List<Int> {
        val dist = bfsDistances(startGx, startGy)
        val goal = index(exitGx, exitGy)
        if (dist[goal] < 0) return emptyList()
        val path = ArrayList<Int>(dist[goal] + 1)
        var cur = goal
        path.add(cur)
        while (dist[cur] > 0) {
            val cx = cur % gw
            val cy = cur / gw
            val want = dist[cur] - 1
            var next = -1
            if (inBounds(cx + 1, cy) && dist[index(cx + 1, cy)] == want) next = index(cx + 1, cy)
            else if (inBounds(cx - 1, cy) && dist[index(cx - 1, cy)] == want) next = index(cx - 1, cy)
            else if (inBounds(cx, cy + 1) && dist[index(cx, cy + 1)] == want) next = index(cx, cy + 1)
            else if (inBounds(cx, cy - 1) && dist[index(cx, cy - 1)] == want) next = index(cx, cy - 1)
            if (next < 0) break
            cur = next
            path.add(cur)
        }
        path.reverse()
        return path
    }

    internal fun refreshSolution() {
        val p = shortestPath()
        solutionPath = p
        solutionLength = if (p.isEmpty()) 0 else p.size - 1
    }
}
