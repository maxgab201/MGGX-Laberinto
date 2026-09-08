package com.mggx.laberinto.maze

import kotlin.math.abs
import kotlin.random.Random

/**
 * Le da alturas a la cueva: terrazas, pozos con escalera y tramos bajos donde
 * hay que agacharse o arrastrarse.
 *
 * La regla que hace que el nivel siga siendo siempre pasable es que el relieve
 * se propaga por BFS desde el inicio, y en cada paso solo puede:
 *
 *  - quedarse igual o moverse UN escalon, que se sube caminando; o
 *  - dar un salto grande, y entonces se marca escalera en las dos puntas.
 *
 * Como toda casilla se alcanza desde el inicio por un camino de transiciones
 * de ese tipo, cualquier recorrido que existiera en el mapa plano sigue
 * existiendo con relieve. El techo puede bajar, pero nunca por debajo de lo
 * que entra arrastrandose, asi que tampoco bloquea.
 */
object ReliefGenerator {

    /** Cuantas casillas seguidas mantiene su altura antes de poder cambiar. */
    private const val LARGO_TERRAZA = 3

    fun apply(maze: Maze, level: Int, rnd: Random) {
        val n = maze.gw * maze.gh
        val visitado = BooleanArray(n)
        val cola = IntArray(n)
        var cabeza = 0
        var cola_ = 0

        // Cuanto relieve tiene el nivel: los primeros son planos y tranquilos.
        val probCambio = when {
            level <= 2 -> 0f
            level <= 6 -> 0.16f
            level <= 14 -> 0.26f
            else -> 0.34f
        }
        val probPozo = when {
            level <= 5 -> 0f
            level <= 12 -> 0.035f
            else -> 0.055f
        }
        val probTechoBajo = when {
            level <= 3 -> 0f
            level <= 10 -> 0.10f
            else -> 0.17f
        }

        val inicio = maze.index(maze.startGx, maze.startGy)
        val salida = maze.index(maze.exitGx, maze.exitGy)
        // Cuantas casillas seguidas lleva la terraza actual, por casilla.
        val corrida = IntArray(n)

        visitado[inicio] = true
        maze.floorLevel[inicio] = 0
        cola[cola_++] = inicio

        while (cabeza < cola_) {
            val actual = cola[cabeza++]
            val cx = actual % maze.gw
            val cy = actual / maze.gw
            val alturaActual = maze.floorLevel[actual]

            for (d in 0 until 4) {
                val nx = cx + DX[d]
                val ny = cy + DY[d]
                if (!maze.inBounds(nx, ny) || maze.isSolid(nx, ny)) continue
                val vecino = maze.index(nx, ny)
                if (visitado[vecino]) continue
                visitado[vecino] = true

                var altura = alturaActual
                var ponerEscalera = false

                if (corrida[actual] >= LARGO_TERRAZA && rnd.nextFloat() < probPozo &&
                    vecino != salida && actual != inicio
                ) {
                    // Pozo o repecho grande: siempre con escalera en las dos puntas.
                    val salto = 3 + rnd.nextInt(3)
                    altura += if (rnd.nextBoolean()) salto else -salto
                    ponerEscalera = true
                } else if (corrida[actual] >= LARGO_TERRAZA && rnd.nextFloat() < probCambio) {
                    // Un escalon: se sube caminando.
                    altura += if (rnd.nextBoolean()) 1 else -1
                }

                maze.floorLevel[vecino] = altura.coerceIn(-14, 14)
                corrida[vecino] = if (maze.floorLevel[vecino] == alturaActual) corrida[actual] + 1 else 0

                if (ponerEscalera && maze.floorLevel[vecino] != alturaActual) {
                    maze.ladder[vecino] = true
                    maze.ladder[actual] = true
                }
                cola[cola_++] = vecino
            }
        }

        // Las casillas que quedaron sin visitar (bolsones que el BFS no toco
        // porque estan aisladas) se dejan a la altura del vecino abierto.
        for (i in 0 until n) {
            if (visitado[i] || maze.solid[i]) continue
            val cx = i % maze.gw
            val cy = i / maze.gw
            for (d in 0 until 4) {
                val vx = cx + DX[d]
                val vy = cy + DY[d]
                if (maze.inBounds(vx, vy) && !maze.isSolid(vx, vy) && visitado[maze.index(vx, vy)]) {
                    maze.floorLevel[i] = maze.floorLevel[maze.index(vx, vy)]
                    break
                }
            }
        }

        // El BFS solo controla las aristas de su propio arbol. El laberinto
        // tiene ciclos, y por esas otras aristas dos casillas vecinas podian
        // quedar a alturas muy distintas. Se acercan con un suavizado, sin
        // tocar las escaleras, donde el desnivel es a proposito.
        for (pasada in 0 until 16) {
            var cambios = 0
            for (gy in 1 until maze.gh - 1) {
                for (gx in 1 until maze.gw - 1) {
                    val i = maze.index(gx, gy)
                    if (maze.solid[i] || maze.ladder[i]) continue
                    var minV = Int.MAX_VALUE
                    var maxV = Int.MIN_VALUE
                    for (d in 0 until 4) {
                        val nx = gx + DX[d]
                        val ny = gy + DY[d]
                        if (!maze.inBounds(nx, ny) || maze.isSolid(nx, ny)) continue
                        val j = maze.index(nx, ny)
                        if (maze.ladder[j]) continue
                        if (maze.floorLevel[j] < minV) minV = maze.floorLevel[j]
                        if (maze.floorLevel[j] > maxV) maxV = maze.floorLevel[j]
                    }
                    if (minV == Int.MAX_VALUE) continue
                    val piso = maxV - 1
                    val techo = minV + 1
                    if (piso > techo) continue   // vecinos incompatibles: va escalera
                    val nuevo = maze.floorLevel[i].coerceIn(piso, techo)
                    if (nuevo != maze.floorLevel[i]) {
                        maze.floorLevel[i] = nuevo
                        cambios++
                    }
                }
            }
            if (cambios == 0) break
        }

        // Ultimo control: lo que siga siendo un desnivel grande lleva escalera
        // en las dos puntas. Asi ninguna transicion queda imposible de pasar.
        for (gy in 0 until maze.gh) {
            for (gx in 0 until maze.gw) {
                if (maze.isSolid(gx, gy)) continue
                for (d in 0 until 4) {
                    val nx = gx + DX[d]
                    val ny = gy + DY[d]
                    if (!maze.inBounds(nx, ny) || maze.isSolid(nx, ny)) continue
                    if (abs(maze.floorY(nx, ny) - maze.floorY(gx, gy)) <= Maze.SUBIDA_CAMINANDO) continue
                    maze.ladder[maze.index(gx, gy)] = true
                    maze.ladder[maze.index(nx, ny)] = true
                }
            }
        }

        // ------------------------------------------------------------ techos
        for (i in 0 until n) {
            if (maze.solid[i]) continue
            maze.ceilClearance[i] = Maze.ALTO_NORMAL
        }
        // Zona franca: alrededor del inicio y de la salida siempre se anda de pie.
        val libres = HashSet<Int>()
        for (centro in intArrayOf(inicio, salida)) {
            val cx = centro % maze.gw
            val cy = centro / maze.gw
            for (dy in -2..2) for (dx in -2..2) {
                if (maze.inBounds(cx + dx, cy + dy)) libres.add(maze.index(cx + dx, cy + dy))
            }
        }

        if (probTechoBajo > 0f) {
            for (gy in 1 until maze.gh - 1) {
                for (gx in 1 until maze.gw - 1) {
                    val i = maze.index(gx, gy)
                    if (maze.solid[i] || i in libres || maze.ladder[i]) continue
                    if (rnd.nextFloat() >= probTechoBajo) continue

                    // Los tramos bajos van de a dos o tres casillas seguidas,
                    // como una gatera de verdad y no un bache suelto.
                    val arrastrarse = rnd.nextFloat() < 0.3f
                    val alto = if (arrastrarse) 0.78f + rnd.nextFloat() * 0.16f
                    else 1.26f + rnd.nextFloat() * 0.22f
                    val horizontal = rnd.nextBoolean()
                    val largo = 2 + rnd.nextInt(2)
                    for (k in 0 until largo) {
                        val tx = if (horizontal) gx + k else gx
                        val ty = if (horizontal) gy else gy + k
                        if (!maze.inBounds(tx, ty)) break
                        val j = maze.index(tx, ty)
                        if (maze.solid[j] || j in libres || maze.ladder[j]) break
                        maze.ceilClearance[j] = alto
                    }
                }
            }
        }
    }

    /** Chequeo defensivo: ninguna transicion puede quedar imposible de pasar. */
    fun esTransitable(maze: Maze): Boolean {
        for (gy in 0 until maze.gh) {
            for (gx in 0 until maze.gw) {
                if (maze.isSolid(gx, gy)) continue
                val i = maze.index(gx, gy)
                // Ninguna casilla puede tener menos alto del que entra arrastrandose.
                if (maze.ceilClearance[i] < 0.70f) return false
                for (d in 0 until 4) {
                    val nx = gx + DX[d]
                    val ny = gy + DY[d]
                    if (!maze.inBounds(nx, ny) || maze.isSolid(nx, ny)) continue
                    val desnivel = abs(maze.floorY(nx, ny) - maze.floorY(gx, gy))
                    if (desnivel <= Maze.SUBIDA_CAMINANDO) continue
                    // Desnivel grande: tiene que haber escalera en alguna punta.
                    if (!maze.hasLadder(gx, gy) && !maze.hasLadder(nx, ny)) return false
                }
            }
        }
        return true
    }

    private val DX = intArrayOf(1, -1, 0, 0)
    private val DY = intArrayOf(0, 0, 1, -1)
}
