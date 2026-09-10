package com.mggx.laberinto.maze

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Generador de laberintos de cueva.
 *
 * Garantias:
 *  1. Se parte de un laberinto PERFECTO (recursive backtracker): todas las celdas
 *     conectadas, exactamente un camino entre cualquier par -> siempre resoluble.
 *  2. Todo lo que se hace despues (trenzado, camaras, grietas) solo ABRE roca,
 *     nunca cierra pasillos, asi que la conectividad se conserva.
 *  3. Igual se verifica con BFS al final. Si algo fallara, se regenera sin
 *     post-procesos (fallback determinista que no puede fallar).
 */
object MazeGenerator {

    data class Blueprint(
        val maze: Maze,
        val level: Int,
        val seed: Long,
        val theme: CaveTheme,
        val coins: List<Int>,
        val bigCoins: List<Int>,
        val crystals: List<Int>,
        val traps: List<Trap>,
        val chests: List<Int>,
        val torches: List<Int>,
        val stalagmites: List<Int>,
        /** Estaciones de carburo: recargan la linterna. */
        val carbide: List<Int>,
        /** Racimos de cristal que brillan solos. */
        val crystalClusters: List<Int>,
        /** Rocas sueltas del derrumbe. */
        val rocks: List<Int>,
        /** Hongos luminosos de cueva. */
        val mushrooms: List<Int>,
        /** Marcos de madera de la mina vieja. */
        val beams: List<Int>,
        /** Bichos que viven en el nivel. */
        val enemies: List<EnemySpawn>
    )

    data class Trap(val gx: Int, val gy: Int, val kind: TrapKind)

    enum class TrapKind { SPIKES, PITFALL, STEAM, ROCKFALL }

    /** Bicho puesto en el nivel: donde nace y de que clase es. */
    data class EnemySpawn(val gx: Int, val gy: Int, val kind: EnemyKind)

    /**
     * Los tres bichos de la mina. Cada uno se juega distinto:
     * el murcielago te encuentra rapido pero pega poco, el rastrero solo te
     * escucha si corres, y el guardian no te persigue pero pega durisimo.
     */
    enum class EnemyKind(
        val etiqueta: String,
        /** Distancia a la que te detecta, en metros. */
        val alcance: Float,
        /** Velocidad de persecucion, en m/s. */
        val velocidad: Float,
        /** Dano por mordisco. */
        val dano: Float,
        /** Segundos entre mordiscos. */
        val recarga: Float,
        /** Radio de su cuerpo, en metros. */
        val radio: Float,
        /** Solo detecta al que corre (murcielago no, rastrero si). */
        val soloOye: Boolean,
        /** No persigue: se queda cuidando su pedazo de cueva. */
        val guardian: Boolean,
        /** Cuanto castigo aguanta antes de caer. */
        val vida: Float,
        /** Ecos que suelta al morir. */
        val recompensa: Int
    ) {
        MURCIELAGO("Murcielago de sima", 9.5f, 4.1f, 7f, 1.5f, 0.34f, false, false, 16f, 6),
        RASTRERO("Rastrero ciego", 13f, 3.4f, 16f, 2.0f, 0.44f, true, false, 44f, 14),
        GUARDIAN("Guardian de roca", 6.5f, 1.9f, 30f, 2.6f, 0.62f, false, true, 120f, 40)
    }

    /** Dimensiones logicas del nivel. Crece de forma sostenida pero acotada. */
    fun cellsForLevel(level: Int): Pair<Int, Int> {
        val l = max(1, level)
        // Crecimiento suave: nivel 1 -> 7x7, nivel 20 -> ~19x17, nivel 60 -> ~35x31
        val base = 7.0 + (l - 1) * 0.48
        var c = base.toInt()
        var r = (base * 0.88).toInt()
        c = c.coerceIn(7, 41)
        r = r.coerceIn(6, 35)
        // Un poco de variedad de forma segun el nivel, sin romper el rango.
        when (l % 4) {
            0 -> c = min(41, c + 2)
            2 -> r = min(35, r + 2)
        }
        return Pair(c, r)
    }

    /** Largo objetivo aproximado del recorrido, para mostrar dificultad. */
    fun difficultyLabel(level: Int): String = when {
        level <= 5 -> "Grieta"
        level <= 12 -> "Galeria"
        level <= 22 -> "Caverna"
        level <= 35 -> "Sima"
        level <= 50 -> "Abismo"
        else -> "Corazon de la Roca"
    }

    fun generate(level: Int, seed: Long = System.nanoTime()): Blueprint {
        val (cols, rows) = cellsForLevel(level)
        val rnd = Random(seed)
        val theme = CaveTheme.forLevel(level)

        var maze = carve(cols, rows, rnd, level)

        if (!maze.isSolvable()) {
            // Fallback: laberinto perfecto puro, sin post-proceso. Imposible que falle.
            maze = carvePerfectOnly(cols, rows, Random(seed xor 0x5DEECE66DL))
        }
        maze.refreshSolution()

        // Relieve: terrazas, pozos con escalera y tramos bajos. Por como se
        // arma no puede cortar el paso, pero se controla igual: si algo saliera
        // mal, el nivel queda plano antes que imposible.
        ReliefGenerator.apply(maze, level, rnd)
        if (!ReliefGenerator.esTransitable(maze)) {
            maze.floorLevel.fill(0)
            maze.ceilClearance.fill(Maze.ALTO_NORMAL)
            maze.ladder.fill(false)
        }

        val populated = populate(maze, level, rnd, theme)
        return populated.copy(seed = seed)
    }

    // ------------------------------------------------------------------ carve

    private fun carvePerfectOnly(cols: Int, rows: Int, rnd: Random): Maze {
        val maze = Maze(cols, rows)
        recursiveBacktracker(maze, rnd)
        placeStartAndExit(maze)
        return maze
    }

    private fun carve(cols: Int, rows: Int, rnd: Random, level: Int): Maze {
        val maze = Maze(cols, rows)
        recursiveBacktracker(maze, rnd)

        // --- Trenzado: elimina callejones sin salida abriendo una pared extra.
        // Solo abre roca => la conectividad no puede empeorar.
        val braid = when {
            level <= 3 -> 0.10f       // niveles iniciales: mas guiados
            level <= 10 -> 0.20f
            level <= 25 -> 0.30f
            else -> 0.38f
        }
        braidDeadEnds(maze, rnd, braid)

        // --- Camaras: salas abiertas que dan aire de cueva natural.
        val chambers = when {
            level <= 2 -> 1
            level <= 8 -> 2
            level <= 20 -> 3
            else -> 3 + (level / 18).coerceAtMost(3)
        }
        carveChambers(maze, rnd, chambers)

        // --- Erosion: redondea esquinas sueltas de roca aislada.
        erode(maze, rnd, if (level <= 5) 0.04f else 0.08f)

        placeStartAndExit(maze)
        return maze
    }

    /** Recursive backtracker iterativo -> laberinto perfecto. */
    private fun recursiveBacktracker(maze: Maze, rnd: Random) {
        val cols = maze.cols
        val rows = maze.rows
        val visited = BooleanArray(cols * rows)
        val stack = ArrayList<Int>(cols * rows)

        val startCell = rnd.nextInt(cols * rows)
        visited[startCell] = true
        stack.add(startCell)
        maze.setSolid((startCell % cols) * 2 + 1, (startCell / cols) * 2 + 1, false)

        val dirs = intArrayOf(0, 1, 2, 3)

        while (stack.isNotEmpty()) {
            val cur = stack[stack.size - 1]
            val cx = cur % cols
            val cy = cur / cols

            // Baraja direcciones (Fisher-Yates)
            for (i in 3 downTo 1) {
                val j = rnd.nextInt(i + 1)
                val t = dirs[i]; dirs[i] = dirs[j]; dirs[j] = t
            }

            var advanced = false
            for (d in dirs) {
                val nx = cx + DX[d]
                val ny = cy + DY[d]
                if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) continue
                val ni = ny * cols + nx
                if (visited[ni]) continue

                // Abre la celda destino y la pared intermedia.
                maze.setSolid(nx * 2 + 1, ny * 2 + 1, false)
                maze.setSolid(cx * 2 + 1 + DX[d], cy * 2 + 1 + DY[d], false)

                visited[ni] = true
                stack.add(ni)
                advanced = true
                break
            }
            if (!advanced) stack.removeAt(stack.size - 1)
        }
    }

    /** Abre paredes en callejones sin salida para crear bucles. Solo suma caminos. */
    private fun braidDeadEnds(maze: Maze, rnd: Random, chance: Float) {
        if (chance <= 0f) return
        val candidates = ArrayList<Int>()
        for (row in 0 until maze.rows) {
            for (col in 0 until maze.cols) {
                val gx = col * 2 + 1
                val gy = row * 2 + 1
                if (maze.isSolid(gx, gy)) continue
                var open = 0
                for (d in 0..3) if (maze.isOpen(gx + DX[d], gy + DY[d])) open++
                if (open <= 1) candidates.add(row * maze.cols + col)
            }
        }
        for (c in candidates) {
            if (rnd.nextFloat() > chance) continue
            val col = c % maze.cols
            val row = c / maze.cols
            val gx = col * 2 + 1
            val gy = row * 2 + 1
            // Paredes internas que se podrian abrir
            val walls = ArrayList<Int>(4)
            for (d in 0..3) {
                val wx = gx + DX[d]
                val wy = gy + DY[d]
                val bx = gx + DX[d] * 2
                val by = gy + DY[d] * 2
                if (!maze.inBounds(bx, by)) continue
                if (bx <= 0 || by <= 0 || bx >= maze.gw - 1 || by >= maze.gh - 1) continue
                if (maze.isSolid(wx, wy) && maze.isOpen(bx, by)) walls.add(d)
            }
            if (walls.isEmpty()) continue
            val d = walls[rnd.nextInt(walls.size)]
            maze.setSolid(gx + DX[d], gy + DY[d], false)
        }
    }

    /** Camaras rectangulares abiertas, ancladas siempre sobre pasillo existente. */
    private fun carveChambers(maze: Maze, rnd: Random, count: Int) {
        var made = 0
        var guard = 0
        while (made < count && guard < count * 40) {
            guard++
            val w = 3 + rnd.nextInt(3)   // 3..5
            val h = 3 + rnd.nextInt(3)
            if (maze.gw - 2 <= w + 2 || maze.gh - 2 <= h + 2) break
            val ox = 1 + rnd.nextInt(maze.gw - 2 - w)
            val oy = 1 + rnd.nextInt(maze.gh - 2 - h)

            // Debe tocar algo ya abierto para no crear una burbuja aislada.
            var touches = false
            outer@ for (y in oy until oy + h) {
                for (x in ox until ox + w) {
                    if (maze.isOpen(x, y)) { touches = true; break@outer }
                }
            }
            if (!touches) continue

            for (y in oy until oy + h) {
                for (x in ox until ox + w) maze.setSolid(x, y, false)
            }
            made++
        }
    }

    /** Suaviza roca que sobresale (bloques con 3+ vecinos abiertos). */
    private fun erode(maze: Maze, rnd: Random, chance: Float) {
        if (chance <= 0f) return
        val toOpen = ArrayList<Int>()
        for (y in 1 until maze.gh - 1) {
            for (x in 1 until maze.gw - 1) {
                if (!maze.isSolid(x, y)) continue
                var open = 0
                for (d in 0..3) if (maze.isOpen(x + DX[d], y + DY[d])) open++
                if (open >= 3 && rnd.nextFloat() < chance) toOpen.add(maze.index(x, y))
            }
        }
        for (i in toOpen) maze.setSolid(i % maze.gw, i / maze.gw, false)
    }

    /**
     * Inicio en una esquina abierta; salida en el punto ABIERTO mas lejano
     * (excentricidad por BFS) -> el recorrido siempre es largo de verdad.
     */
    private fun placeStartAndExit(maze: Maze) {
        // Inicio: celda logica abierta mas cercana a la esquina superior izquierda.
        var best = -1
        var bestScore = Int.MAX_VALUE
        for (row in 0 until maze.rows) {
            for (col in 0 until maze.cols) {
                if (maze.isSolid(col * 2 + 1, row * 2 + 1)) continue
                val score = col + row
                if (score < bestScore) { bestScore = score; best = row * maze.cols + col }
            }
        }
        if (best < 0) { // no deberia ocurrir: el backtracker abre todas las celdas
            maze.setSolid(1, 1, false)
            best = 0
        }
        maze.startCol = best % maze.cols
        maze.startRow = best / maze.cols

        val dist = maze.bfsDistances(maze.startGx, maze.startGy)
        var far = -1
        var farD = -1
        for (row in 0 until maze.rows) {
            for (col in 0 until maze.cols) {
                val gi = maze.index(col * 2 + 1, row * 2 + 1)
                val d = dist[gi]
                if (d > farD) { farD = d; far = row * maze.cols + col }
            }
        }
        if (far < 0 || farD <= 0) {
            maze.exitCol = maze.startCol
            maze.exitRow = maze.startRow
        } else {
            maze.exitCol = far % maze.cols
            maze.exitRow = far / maze.cols
        }
    }

    // --------------------------------------------------------------- poblado

    private fun populate(maze: Maze, level: Int, rnd: Random, theme: CaveTheme): Blueprint {
        val open = ArrayList<Int>()
        for (y in 1 until maze.gh - 1) {
            for (x in 1 until maze.gw - 1) if (maze.isOpen(x, y)) open.add(maze.index(x, y))
        }
        val startI = maze.index(maze.startGx, maze.startGy)
        val exitI = maze.index(maze.exitGx, maze.exitGy)
        val distFromStart = maze.bfsDistances(maze.startGx, maze.startGy)

        val reserved = HashSet<Int>()
        reserved.add(startI)
        reserved.add(exitI)
        // Zona franca alrededor del inicio: nada peligroso ahi.
        for (dy in -2..2) for (dx in -2..2) {
            val x = maze.startGx + dx
            val y = maze.startGy + dy
            if (maze.inBounds(x, y)) reserved.add(maze.index(x, y))
        }

        val pool = open.filter { it !in reserved }.toMutableList()
        pool.shuffle(rnd)
        var cursor = 0
        fun take(n: Int): List<Int> {
            val end = min(pool.size, cursor + n)
            val out = if (cursor >= end) emptyList() else ArrayList(pool.subList(cursor, end))
            cursor = end
            return out
        }

        val area = open.size
        val coinCount = (area * 0.085f).toInt().coerceIn(6, 140)
        val bigCount = (2 + level / 6).coerceIn(2, 12)
        // Vetagris: antes casi no aparecia (uno cada cinco niveles, y en el
        // resto un 22% de que hubiera UNO solo escondido en toda la cueva).
        // Bajar y no encontrar nunca la moneda cara no genera ganas de buscar,
        // genera la idea de que no existe. Ahora siempre hay al menos uno, los
        // niveles multiplo de cinco siguen siendo los buenos, y hay una chance
        // sana de que aparezca alguno de yapa.
        val crystalCount = 1 +
            (if (level % 5 == 0) 1 + level / 25 else 0) +
            (if (rnd.nextFloat() < 0.35f) 1 else 0)
        val chestCount = (1 + level / 8).coerceIn(1, 5)
        val trapCount = if (level < 3) 0 else (area * 0.012f * (1f + level / 45f)).toInt().coerceIn(1, 60)
        val torchCount = (area * 0.05f).toInt().coerceIn(4, 90)
        // Cada bioma se puebla distinto: en la mina hay entibado por todos
        // lados y casi ninguna estalagmita; en el bosque de esporas al reves.
        val densStalag = when (theme.biome) {
            Biome.MINA -> 0.40f
            Biome.RUINAS -> 0.55f
            Biome.TEMPLO -> 0.35f
            Biome.HONGOS -> 0.60f
            Biome.CUEVA -> 1f
        }
        val densViga = when (theme.biome) {
            Biome.MINA -> 2.6f
            Biome.RUINAS -> 0.35f
            Biome.TEMPLO -> 0.25f
            Biome.HONGOS -> 0.5f
            Biome.CUEVA -> 1f
        }
        val densHongo = when (theme.biome) {
            Biome.HONGOS -> 3.2f
            Biome.RUINAS -> 1.4f
            Biome.MINA -> 0.5f
            Biome.TEMPLO -> 0.3f
            Biome.CUEVA -> 1f
        }
        val densCristal = when (theme.biome) {
            Biome.TEMPLO -> 1.8f
            Biome.RUINAS -> 0.6f
            Biome.MINA -> 0.5f
            else -> 1f
        }
        val stalagCount = (area * 0.07f * densStalag).toInt().coerceIn(4, 130)

        val coins = take(coinCount)
        val bigCoins = take(bigCount)
        val crystals = take(crystalCount)
        val chests = take(chestCount)

        // Trampas: nunca sobre el unico camino si el laberinto es un pasillo puro,
        // y siempre a >=6 de distancia del inicio.
        val trapSpots = take(trapCount).filter { (distFromStart[it] >= 6) }
        val kinds = TrapKind.values()
        val traps = trapSpots.map {
            Trap(it % maze.gw, it / maze.gw, kinds[rnd.nextInt(kinds.size)])
        }

        // Antorchas: pegadas a pared para que se vean bien montadas.
        val torches = ArrayList<Int>(torchCount)
        val wallHugging = open.filter { gi ->
            val x = gi % maze.gw
            val y = gi / maze.gw
            var s = 0
            for (d in 0..3) if (maze.isSolid(x + DX[d], y + DY[d])) s++
            s in 1..3 && gi != startI && gi != exitI
        }.toMutableList()
        wallHugging.shuffle(rnd)
        for (i in 0 until min(torchCount, wallHugging.size)) torches.add(wallHugging[i])

        // Estalagmitas decorativas: en casillas libres que no bloqueen (son delgadas).
        val stalag = take(stalagCount)

        // --- ambientacion y estaciones
        // Las estaciones de carburo se reparten por el nivel: siempre hay al
        // menos una, y mas cuanto mas largo es el nivel, asi la linterna nunca
        // te deja tirado.
        val carbideCount = (1 + area / 260).coerceIn(1, 8)
        val carbide = take(carbideCount)
        val crystalClusters = take((area * 0.030f * densCristal).toInt().coerceIn(3, 70))
        val rocks = take((area * 0.045f).toInt().coerceIn(4, 90))
        val mushrooms = take((area * 0.025f * densHongo).toInt().coerceIn(3, 80))
        // Los marcos de madera solo entran en pasillos rectos: si no, quedan
        // clavados en el aire.
        val pasillos = open.filter { gi ->
            val x = gi % maze.gw
            val y = gi / maze.gw
            val horizontal = maze.isSolid(x, y - 1) && maze.isSolid(x, y + 1) &&
                maze.isOpen(x - 1, y) && maze.isOpen(x + 1, y)
            val vertical = maze.isSolid(x - 1, y) && maze.isSolid(x + 1, y) &&
                maze.isOpen(x, y - 1) && maze.isOpen(x, y + 1)
            (horizontal || vertical) && gi != startI && gi != exitI
        }.toMutableList()
        pasillos.shuffle(rnd)
        val beams = pasillos.take((area * 0.02f * densViga).toInt().coerceIn(2, 60))

        // --- bichos
        val enemies = spawnEnemies(maze, level, rnd, distFromStart, take((3 + level / 2).coerceAtMost(26)))

        return Blueprint(
            maze = maze,
            level = level,
            seed = 0L,
            theme = theme,
            coins = coins,
            bigCoins = bigCoins,
            crystals = crystals,
            traps = traps,
            chests = chests,
            torches = torches,
            stalagmites = stalag,
            carbide = carbide,
            crystalClusters = crystalClusters,
            rocks = rocks,
            mushrooms = mushrooms,
            beams = beams,
            enemies = enemies
        )
    }

    /**
     * Reparte los bichos. Nunca cerca del inicio (por eso el filtro de
     * distancia), y la mezcla se va poniendo mas fea con el nivel: primero
     * solo murcielagos, despues rastreros y al final guardianes.
     */
    private fun spawnEnemies(
        maze: Maze,
        level: Int,
        rnd: Random,
        distFromStart: IntArray,
        lugares: List<Int>
    ): List<EnemySpawn> {
        if (level < 3) return emptyList()
        val cuantos = when {
            level < 6 -> 1 + level / 3
            level < 14 -> 2 + level / 3
            else -> (3 + level / 3).coerceAtMost(15)
        }
        val libres = lugares.filter { distFromStart[it] >= 10 }
        val out = ArrayList<EnemySpawn>()
        for (gi in libres.take(cuantos)) {
            val r = rnd.nextFloat()
            val kind = when {
                level < 6 -> EnemyKind.MURCIELAGO
                level < 12 -> if (r < 0.62f) EnemyKind.MURCIELAGO else EnemyKind.RASTRERO
                level < 20 -> when {
                    r < 0.42f -> EnemyKind.MURCIELAGO
                    r < 0.86f -> EnemyKind.RASTRERO
                    else -> EnemyKind.GUARDIAN
                }
                else -> when {
                    r < 0.30f -> EnemyKind.MURCIELAGO
                    r < 0.70f -> EnemyKind.RASTRERO
                    else -> EnemyKind.GUARDIAN
                }
            }
            out.add(EnemySpawn(gi % maze.gw, gi / maze.gw, kind))
        }
        return out
    }

    private val DX = intArrayOf(1, -1, 0, 0)
    private val DY = intArrayOf(0, 0, 1, -1)
}
