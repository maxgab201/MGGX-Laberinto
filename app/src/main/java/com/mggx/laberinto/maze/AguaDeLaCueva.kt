package com.mggx.laberinto.maze

import kotlin.random.Random

/**
 * Donde se junta el agua en una cueva.
 *
 * La idea es la de una napa: hay una altura y el agua llena TODO lo que quede
 * por debajo. No se eligen charcos a dedo; se elige un nivel y la cueva decide
 * sola donde hay agua, que es lo que hace que los charcos caigan justo en los
 * pozos y en los tramos hondos, como pasaria de verdad.
 *
 * Dos reglas que no se negocian, y por eso el nivel se calcula aca y no a ojo:
 *
 *  - **Nunca tapa el paso.** El agua se queda [PROFUNDIDAD] metros por arriba
 *    del piso mas hondo de la cueva y nada mas. A esa altura se vadea
 *    caminando: no hay que nadar, no hay que saltar y no se puede ahogar nadie.
 *  - **Nunca moja el arranque ni la salida.** Aparecer con los pies en el agua,
 *    o tener que terminar el nivel chapoteando, se lee como un bug aunque sea a
 *    proposito.
 *
 * Aritmetica pura sobre el [Maze]: se verifica entera en un test de JVM.
 */
object AguaDeLaCueva {

    /**
     * Cuanto sube el agua por encima del piso mas hondo, en metros.
     *
     * Un escalon mide 0,42 m ([Maze.ESCALON]), asi que esto es menos de un
     * escalon: el agua llena el fondo de los pozos y las terrazas bajas, y en
     * cuanto el piso sube un escalon ya se sale del agua. Subirlo convertiria
     * la cueva en una pileta y habria que rehacer el movimiento entero.
     */
    const val PROFUNDIDAD = 0.30f

    /** Marca de "esta cueva esta seca". */
    const val SIN_AGUA = Float.NEGATIVE_INFINITY

    /** Desde que nivel puede haber agua. Los primeros son secos y tranquilos. */
    const val NIVEL_DESDE = 4

    /**
     * Altura del espejo de agua, o [SIN_AGUA] si esta cueva esta seca.
     *
     * Devuelve [SIN_AGUA] tambien cuando el agua no serviria de nada: si el
     * piso es todo plano, la napa taparia la cueva entera o ninguna casilla, y
     * las dos cosas son peores que no tener agua.
     */
    fun nivelDeAgua(maze: Maze, level: Int, rnd: Random): Float {
        if (level < NIVEL_DESDE) return SIN_AGUA
        // Mas hondo, mas humedo. Nunca llega a ser siempre.
        val chance = (0.20f + level * 0.012f).coerceAtMost(0.62f)
        if (rnd.nextFloat() >= chance) return SIN_AGUA

        var masHondo = Int.MAX_VALUE
        var masAlto = Int.MIN_VALUE
        for (i in maze.floorLevel.indices) {
            if (maze.solid[i]) continue
            val f = maze.floorLevel[i]
            if (f < masHondo) masHondo = f
            if (f > masAlto) masAlto = f
        }
        // Cueva plana: no hay pozo donde se junte nada.
        if (masHondo == Int.MAX_VALUE || masHondo == masAlto) return SIN_AGUA

        val y = masHondo * Maze.ESCALON + PROFUNDIDAD

        // El arranque y la salida tienen que quedar en seco.
        if (maze.floorY(maze.startGx, maze.startGy) < y) return SIN_AGUA
        if (maze.floorY(maze.exitGx, maze.exitGy) < y) return SIN_AGUA
        return y
    }

    /** Si en esa casilla hay agua con el espejo a [nivelY]. */
    fun hayAgua(maze: Maze, nivelY: Float, gx: Int, gy: Int): Boolean {
        if (nivelY == SIN_AGUA) return false
        if (!maze.inBounds(gx, gy) || maze.isSolid(gx, gy)) return false
        return maze.floorY(gx, gy) < nivelY
    }

    /**
     * Cuanto te tapa el agua estando parado en esa casilla, en metros. Cero si
     * estas en seco.
     */
    fun hondura(maze: Maze, nivelY: Float, gx: Int, gy: Int): Float {
        if (!hayAgua(maze, nivelY, gx, gy)) return 0f
        return (nivelY - maze.floorY(gx, gy)).coerceAtMost(PROFUNDIDAD)
    }

    /**
     * Cuanto frena el agua, como multiplicador de velocidad.
     *
     * Frena, pero poco: el agua es ambiente y un obstaculo chico, no un
     * castigo. Si frenara de verdad, cruzar un charco largo con un bicho atras
     * seria una muerte cantada por algo que ni se ve venir.
     */
    fun frenoPorAgua(hondura: Float): Float =
        if (hondura <= 0f) 1f else (1f - hondura * 0.55f).coerceAtLeast(0.75f)

    /**
     * Chapotear hace ruido: caminando por el agua te oyen igual que si
     * corrieras.
     *
     * Es lo que le da sentido al agua mas alla de lo visual: un tramo inundado
     * es un tramo donde el sigilo no te sirve, y eso cambia por donde elegis
     * ir.
     */
    fun haceRuido(hondura: Float, seEstaMoviendo: Boolean): Boolean =
        seEstaMoviendo && hondura > 0.05f
}
