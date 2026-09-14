package com.mggx.laberinto.maze

import kotlin.random.Random

/**
 * El nivel 999: la salida de verdad.
 *
 * Todos los demas niveles terminan en una columna de cristal que te baja un
 * piso mas. Este no. Este es una galeria corta que SUBE, y arriba de todo hay
 * una puerta de tablones que da al patio de tu casa. Ahi se termina la mina.
 *
 * Por eso el nivel es chico a proposito: no es un laberinto para resolver, es
 * el ultimo tramo. Lo unico que tiene que pasar es que subas, que la luz vaya
 * cambiando de la lampara de carburo al sol, y que al salir estes afuera.
 *
 * ## Como esta armado
 *
 * Un corredor en zigzag que va ganando altura escalon a escalon, con escaleras
 * en los tramos grandes, y un bloque de casillas al final marcadas como
 * [Maze.cielo]: ahi no se dibuja techo, y eso solo ya cambia todo — mirar para
 * arriba y que no haya piedra es la diferencia entre estar adentro y estar
 * afuera.
 *
 * Todo lo que arma esto es aritmetica sobre el [Maze], asi que se verifica
 * entero en un test de JVM (ver AscensoTest).
 */
object ElAscenso {

    /** El numero del nivel final. */
    const val NIVEL = 999

    /** Cuantas casillas de lado tiene el patio. */
    const val LADO_PATIO = 5

    /**
     * Cuantos escalones sube el ascenso en total.
     *
     * Son unos seis metros y medio: lo suficiente para que se sienta que
     * estabas hondo y saliste, sin convertirlo en una escalera eterna.
     */
    const val ESCALONES = 16

    /** Dimensiones logicas del nivel 999. Chico: es el ultimo tramo, no un laberinto. */
    fun celdas(): Pair<Int, Int> = 7 to 7

    /**
     * Talla el nivel: un corredor que sube y un patio arriba.
     *
     * Se hace a mano y no con el generador comun porque este nivel no es un
     * laberinto: no tiene que tener vueltas ni callejones, tiene que tener UNA
     * subida que se lea como una subida.
     */
    fun tallar(maze: Maze, rnd: Random) {
        // Todo solido, y se abre solo lo que hace falta.
        maze.solid.fill(true)

        val gw = maze.gw
        val gh = maze.gh
        // El patio ocupa el bloque de arriba (gy chico), centrado.
        val patioX0 = (gw - LADO_PATIO) / 2
        val patioY0 = 1
        // El ascenso arranca abajo del todo, en el medio.
        val startGx = gw / 2
        val startGy = gh - 2

        // --- el corredor: sube en zigzag desde el arranque hasta el patio
        val camino = ArrayList<Int>()
        var x = startGx
        var y = startGy
        val destinoY = patioY0 + LADO_PATIO
        var haciaLaDerecha = rnd.nextBoolean()
        while (y > destinoY) {
            // Un tramo recto hacia el costado, y despues sube.
            val ancho = 1 + rnd.nextInt(2)
            repeat(ancho) {
                val nx = (x + if (haciaLaDerecha) 1 else -1).coerceIn(1, gw - 2)
                if (nx != x) { x = nx; camino.add(maze.index(x, y)) }
            }
            haciaLaDerecha = !haciaLaDerecha
            y--
            camino.add(maze.index(x, y))
        }
        // Se abre el corredor entero, mas la casilla de arranque.
        camino.add(maze.index(startGx, startGy))
        for (i in camino) maze.solid[i] = false
        // Y se lo conecta con el patio por el medio.
        for (gy in destinoY downTo patioY0 + LADO_PATIO - 1) {
            maze.solid[maze.index(x, gy)] = false
        }

        // --- el patio
        for (dy in 0 until LADO_PATIO) {
            for (dx in 0 until LADO_PATIO) {
                maze.solid[maze.index(patioX0 + dx, patioY0 + dy)] = false
            }
        }
        // El corredor tiene que desembocar adentro del patio, no al lado.
        val bocaX = x.coerceIn(patioX0, patioX0 + LADO_PATIO - 1)
        for (gy in patioY0 + LADO_PATIO - 1..destinoY) {
            if (maze.inBounds(bocaX, gy)) maze.solid[maze.index(bocaX, gy)] = false
        }

        maze.startCol = (startGx - 1) / 2
        maze.startRow = (startGy - 1) / 2
        maze.exitCol = (patioX0 + LADO_PATIO / 2 - 1) / 2
        maze.exitRow = (patioY0 + LADO_PATIO / 2 - 1) / 2
    }

    /**
     * Le pone el relieve: el corredor sube, el patio esta arriba y al aire
     * libre.
     *
     * Se hace aparte de [ReliefGenerator] porque aca el relieve no es
     * decoracion: es el contenido del nivel. Tiene que subir de forma pareja y
     * monotona, y el generador comun reparte terrazas al azar.
     */
    fun relieve(maze: Maze) {
        val gw = maze.gw
        val gh = maze.gh
        val patioX0 = (gw - LADO_PATIO) / 2
        val patioY0 = 1
        val altoPatio = ESCALONES

        // La altura se reparte por la fila: abajo del todo cero, arriba el
        // alto del patio. Asi todo el corredor sube parejo sin importar por
        // donde zigzaguee.
        val filaBase = (gh - 2).toFloat()
        val filaTope = (patioY0 + LADO_PATIO).toFloat()
        for (gy in 0 until gh) {
            for (gx in 0 until gw) {
                val i = maze.index(gx, gy)
                if (maze.solid[i]) continue
                val enPatio = gx >= patioX0 && gx < patioX0 + LADO_PATIO &&
                    gy >= patioY0 && gy < patioY0 + LADO_PATIO
                if (enPatio) {
                    maze.floorLevel[i] = altoPatio
                    maze.cielo[i] = true
                    // Techo altisimo: no se dibuja igual (es cielo), pero la
                    // camara y los bichos leen este numero.
                    maze.ceilClearance[i] = Maze.ALTO_NORMAL * 2.2f
                    continue
                }
                val t = ((filaBase - gy) / (filaBase - filaTope)).coerceIn(0f, 1f)
                maze.floorLevel[i] = Math.round(t * altoPatio)
                maze.ceilClearance[i] = Maze.ALTO_NORMAL
            }
        }

        // Escaleras donde el salto entre vecinas no se sube caminando.
        for (gy in 0 until gh) {
            for (gx in 0 until gw) {
                val i = maze.index(gx, gy)
                if (maze.solid[i]) continue
                for (d in 0 until 4) {
                    val vx = gx + DX[d]
                    val vy = gy + DY[d]
                    if (!maze.inBounds(vx, vy) || maze.isSolid(vx, vy)) continue
                    val j = maze.index(vx, vy)
                    val salto = kotlin.math.abs(maze.floorLevel[i] - maze.floorLevel[j])
                    if (salto * Maze.ESCALON > Maze.SUBIDA_CAMINANDO) {
                        maze.ladder[i] = true
                        maze.ladder[j] = true
                    }
                }
            }
        }
    }

    /** Las casillas del patio, para que el renderer sepa donde amueblar. */
    fun casillasDelPatio(maze: Maze): List<Int> {
        val out = ArrayList<Int>(LADO_PATIO * LADO_PATIO)
        val patioX0 = (maze.gw - LADO_PATIO) / 2
        for (dy in 0 until LADO_PATIO) {
            for (dx in 0 until LADO_PATIO) {
                out.add(maze.index(patioX0 + dx, 1 + dy))
            }
        }
        return out
    }

    private val DX = intArrayOf(0, 0, -1, 1)
    private val DY = intArrayOf(-1, 1, 0, 0)
}
