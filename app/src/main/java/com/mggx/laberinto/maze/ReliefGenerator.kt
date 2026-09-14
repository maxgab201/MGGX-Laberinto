package com.mggx.laberinto.maze

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Le da alturas a la cueva: lomas, hondonadas, barrancas con escalera, salones
 * altos y galerias bajas donde hay que agacharse.
 *
 * ## Por que el relieve sale de un CAMPO y no de un paseo al azar
 *
 * Antes la altura se propagaba por BFS desde el inicio y en cada paso tiraba
 * una moneda: se quedaba igual, subia un escalon o bajaba uno. Eso garantizaba
 * que el nivel siguiera siendo pasable, y en eso funcionaba bien. El problema
 * es lo que producia, y recien se vio cuando se pudo MIRAR el relieve entero
 * con `VisorDeRelieve`: la altura viajaba por los pasillos, no por el espacio.
 * Dos galerias separadas por una sola pared podian quedar a dos metros de
 * desnivel, porque el BFS habia llegado a cada una por un camino distinto.
 *
 * En planta se veia clarisimo: fideos de colores. Cada pasillo su altura, sin
 * ninguna relacion con el de al lado. Adentro eso se siente como que la cueva
 * no tiene forma: subis, bajas, volves a subir, y nunca estas "en la parte
 * alta" de ningun lado, porque no hay parte alta.
 *
 * Ahora la altura sale de un campo de ruido suave sobre la GRILLA, igual que
 * un mapa de terreno. Dos casillas que estan cerca en el espacio tienen
 * alturas parecidas aunque haya una pared en el medio, porque las dos leen el
 * mismo campo. Asi aparecen zonas: un ala alta, una hondonada, una pendiente
 * larga. Y el agua, que se llena hasta una cota, deja de ser charcos sueltos y
 * pasa a ser un lago en la parte baja.
 *
 * ## Por que esto sigue siendo pasable
 *
 * El campo es suave: entre dos casillas vecinas cambia mucho menos de un
 * escalon, asi que casi todo el mapa se recorre caminando. Donde SI hay un
 * salto grande —las barrancas, que se ponen a proposito— la ultima pasada
 * marca escalera en las dos puntas, sin excepcion. [esTransitable] lo
 * comprueba, y el generador vuelve a tirar el nivel si no da.
 */
object ReliefGenerator {

    /**
     * Cada cuantas casillas se repite el dibujo grande del terreno.
     *
     * Es lo que decide el tamano de las zonas. Con un numero chico salen
     * lomitas de dos casillas (ruido otra vez); con uno muy grande el nivel
     * entero queda en pendiente. Veinte casillas son unos sesenta metros: un
     * ala del mapa.
     */
    private const val ESCALA_TERRENO = 20f

    /** Lo mismo para el techo, que va en salones mas chicos que las lomas. */
    private const val ESCALA_TECHO = 11f

    /**
     * Los altos libres que puede tener una galeria, de menor a mayor.
     *
     * Van en una lista corta y no en un valor continuo por una razon de
     * dibujo: `WorldMesh` solo puede continuar el techo entre dos casillas si
     * tienen EXACTAMENTE el mismo alto libre. Con un techo que cambia de a
     * poco en cada casilla, ninguna pareja coincidiria y el techo (y las
     * paredes, que dependen de el) quedaria partido en parches de tres metros
     * con un canto duro en cada borde. Con una lista corta, en cambio, salen
     * salones enteros de un mismo alto y el canto aparece solo donde cambia de
     * verdad, que es donde tiene que estar.
     *
     * El mas bajo sigue siendo comodo para andar de pie (el minero mide 1,72).
     */
    private val ALTOS = floatArrayOf(2.70f, 3.40f, 4.35f, 5.40f)

    // ------------------------------------------------------------ el campo

    private fun hash2(x: Int, y: Int, semilla: Int): Float {
        var h = x * 374761393 + y * 668265263 + semilla * 1442695041
        h = (h xor (h shr 13)) * 1274126177
        h = h xor (h shr 16)
        return (h and 0x7FFFFFFF) / 2147483647.0f
    }

    private fun suave(t: Float): Float = t * t * (3f - 2f * t)

    /** Ruido de valor continuo, de -1 a 1. */
    private fun valor(x: Float, y: Float, semilla: Int): Float {
        val ix = floor(x).toInt()
        val iy = floor(y).toInt()
        val fx = suave(x - ix)
        val fy = suave(y - iy)
        val a = hash2(ix, iy, semilla)
        val b = hash2(ix + 1, iy, semilla)
        val c = hash2(ix, iy + 1, semilla)
        val d = hash2(ix + 1, iy + 1, semilla)
        val ab = a + (b - a) * fx
        val cd = c + (d - c) * fx
        return (ab + (cd - ab) * fy) * 2f - 1f
    }

    /**
     * Cuanto se estira el campo antes de usarlo.
     *
     * Dos octavas de ruido de valor sumadas no llegan nunca a los extremos: el
     * 90% de las casillas cae entre -0,60 y +0,53, asi que sin estirarlo el
     * campo desperdicia la mitad de su rango. Medido, no estimado: sin esto,
     * tres cuartos de la cueva caian en el mismo alto de techo.
     */
    private const val GANANCIA = 1.55f

    /**
     * El campo de terreno en una casilla, de -1 a 1.
     *
     * Dos octavas: el dibujo grande manda, y la chica le saca la lisura de
     * pileta sin llegar a ponerle dientes.
     */
    internal fun campo(gx: Int, gy: Int, semilla: Int, escala: Float): Float {
        val crudo = valor(gx / escala, gy / escala, semilla) * 0.76f +
            valor(gx / (escala * 0.41f) + 5.5f, gy / (escala * 0.41f) - 3.5f, semilla + 977) * 0.24f
        return (crudo * GANANCIA).coerceIn(-1f, 1f)
    }

    // ------------------------------------------------------------- relieve

    fun apply(maze: Maze, level: Int, rnd: Random) {
        val n = maze.gw * maze.gh
        val semillaPiso = rnd.nextInt()
        val semillaTecho = rnd.nextInt()

        // Cuanto relieve tiene el nivel: los primeros son planos y tranquilos.
        val amplitud = when {
            level <= 2 -> 0f
            level <= 6 -> 2.2f
            level <= 14 -> 3.6f
            else -> 5.0f
        }
        // Cuantas barrancas (mesetas y hondonadas de borde cortado, con
        // escalera) se agregan encima del terreno suave.
        val barrancas = when {
            level <= 5 -> 0
            level <= 12 -> 1 + rnd.nextInt(2)
            else -> 2 + rnd.nextInt(3)
        }

        // ---------------------------------------------------------- el piso
        for (i in 0 until n) {
            if (maze.solid[i]) { maze.floorLevel[i] = 0; continue }
            val gx = i % maze.gw
            val gy = i / maze.gw
            maze.floorLevel[i] = (campo(gx, gy, semillaPiso, ESCALA_TERRENO) * amplitud).roundToInt()
        }

        // Las barrancas: un disco de casillas que sube o baja de golpe. Es lo
        // unico del relieve que corta: el campo suave nunca hace un escalon
        // que no se pueda subir caminando, y una cueva sin ningun corte se
        // vuelve una loma sin sorpresas.
        val inicio = maze.index(maze.startGx, maze.startGy)
        val salida = maze.index(maze.exitGx, maze.exitGy)
        for (k in 0 until barrancas) {
            val cx = 2 + rnd.nextInt((maze.gw - 4).coerceAtLeast(1))
            val cy = 2 + rnd.nextInt((maze.gh - 4).coerceAtLeast(1))
            val radio = 2 + rnd.nextInt(3)
            val salto = (3 + rnd.nextInt(3)) * (if (rnd.nextBoolean()) 1 else -1)
            for (gy in (cy - radio)..(cy + radio)) {
                for (gx in (cx - radio)..(cx + radio)) {
                    if (!maze.inBounds(gx, gy)) continue
                    val dx = gx - cx; val dy = gy - cy
                    if (dx * dx + dy * dy > radio * radio) continue
                    val i = maze.index(gx, gy)
                    if (maze.solid[i] || i == inicio || i == salida) continue
                    maze.floorLevel[i] += salto
                }
            }
        }

        // El inicio a cota cero, para que los numeros sean chicos y para que
        // "altura cero" quiera decir algo.
        val base = maze.floorLevel[inicio]
        for (i in 0 until n) {
            if (maze.solid[i]) continue
            maze.floorLevel[i] = (maze.floorLevel[i] - base).coerceIn(-14, 14)
        }

        // Escaleras: cualquier transicion que no se suba caminando lleva
        // escalera en las DOS puntas. Sin excepciones y sin depender del orden
        // en que se recorra: es lo unico que garantiza que el nivel se pueda
        // terminar.
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

        techos(maze, level, rnd, semillaTecho, inicio, salida)
        if (level == 1) tramoDeTutorial(maze, zonaFranca(maze, inicio, salida))
    }

    /** Las casillas alrededor del inicio y de la salida, donde nada estorba. */
    private fun zonaFranca(maze: Maze, inicio: Int, salida: Int): Set<Int> {
        val libres = HashSet<Int>()
        for (centro in intArrayOf(inicio, salida)) {
            val cx = centro % maze.gw
            val cy = centro / maze.gw
            for (dy in -2..2) for (dx in -2..2) {
                if (maze.inBounds(cx + dx, cy + dy)) libres.add(maze.index(cx + dx, cy + dy))
            }
        }
        return libres
    }

    /**
     * Los techos: salones altos, galerias comunes y tramos donde hay que
     * agacharse.
     *
     * Antes el techo era una constante. TODA la cueva media exactamente 3,40 m
     * de alto libre salvo los tramos bajos sueltos, y eso es lo que mas la
     * hacia sentir un pasillo de oficina en vez de una cueva: adentro de una
     * cueva de verdad el techo sube y baja todo el tiempo, y ese cambio es
     * justamente lo que te dice que pasaste de una galeria a un salon.
     */
    private fun techos(
        maze: Maze, level: Int, rnd: Random, semilla: Int, inicio: Int, salida: Int
    ) {
        val n = maze.gw * maze.gh
        for (i in 0 until n) {
            if (maze.solid[i]) { maze.ceilClearance[i] = Maze.ALTO_NORMAL; continue }
            val gx = i % maze.gw
            val gy = i / maze.gw
            val t = (campo(gx, gy, semilla, ESCALA_TECHO) * 0.5f + 0.5f).coerceIn(0f, 1f)
            maze.ceilClearance[i] = ALTOS[(t * (ALTOS.size - 1)).roundToInt()]
        }

        // Alrededor del inicio y de la salida se anda de pie y con lugar: son
        // los dos unicos puntos del nivel donde el jugador tiene que poder
        // orientarse sin agacharse.
        val libres = zonaFranca(maze, inicio, salida)
        for (i in libres) {
            if (!maze.solid[i] && maze.ceilClearance[i] < Maze.ALTO_NORMAL) {
                maze.ceilClearance[i] = Maze.ALTO_NORMAL
            }
        }

        // Cuantos arranques de tramo bajo se tiran por casilla.
        //
        // Cada acierto escribe una corrida de 3 a 5 casillas, asi que la
        // cobertura final es varias veces este numero. Con 0,17 (lo que habia)
        // casi un tercio de la cueva obligaba a agacharse, y un tramo bajo se
        // siente porque es RARO: si agacharse es la forma normal de caminar,
        // deja de ser un momento. Medido con `VisorDeRelieve.fraccionDeGatera`,
        // no estimado.
        val probTechoBajo = when {
            level <= 3 -> 0f
            level <= 10 -> 0.030f
            else -> 0.055f
        }
        if (probTechoBajo <= 0f) return

        for (gy in 1 until maze.gh - 1) {
            for (gx in 1 until maze.gw - 1) {
                val i = maze.index(gx, gy)
                if (maze.solid[i] || i in libres || maze.ladder[i]) continue
                if (rnd.nextFloat() >= probTechoBajo) continue

                // Los tramos bajos van de a varias casillas seguidas, como una
                // gatera de verdad y no un bache suelto: cuando eran de dos,
                // encontrar por donde se pasaba era cuestion de acertarle a un
                // huequito.
                //
                // Las alturas estan elegidas para que el hueco REAL (ya
                // descontada la panza del techo, ver Maze.altoLibreReal) le
                // entre a la postura que corresponde. Con los valores de
                // antes, un tramo "para agacharse" de 1,26 dejaba 1,12 de
                // hueco real y el cuerpo agachado mide 1,20: el juego te
                // dejaba pasar por un lugar donde la roca dibujada no te daba,
                // y la camara terminaba adentro de la piedra.
                val arrastrarse = rnd.nextFloat() < 0.3f
                val alto = if (arrastrarse) 0.88f + rnd.nextFloat() * 0.16f
                else 1.42f + rnd.nextFloat() * 0.22f
                val horizontal = rnd.nextBoolean()
                val largo = 3 + rnd.nextInt(3)
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

    /**
     * El unico tramo bajo del nivel 1, puesto a mano sobre el camino a la
     * salida.
     *
     * El nivel 1 es plano y tranquilo a proposito (probTechoBajo = 0), pero el
     * tutorial le dice al jugador "agachate, hay tramos bajos donde es la
     * unica forma de pasar". Si el nivel no tuviera ninguno, esa frase seria
     * mentira y el jugador aprenderia a apretar un boton sin entender para que
     * sirve. Va SOBRE el camino a la salida justamente para que tenga que
     * usarlo, no para que lo esquive.
     *
     * La altura es la misma que usa un tramo "para agacharse" del resto del
     * juego: deja hueco real de sobra para el cuerpo agachado (1,20 m) y
     * ninguno para el cuerpo de pie.
     */
    private fun tramoDeTutorial(maze: Maze, libres: Set<Int>) {
        val camino = maze.solutionPath
        if (camino.size < 12) return
        val desde = camino.size / 2
        var puestas = 0
        for (k in desde until camino.size - 3) {
            val i = camino[k]
            if (maze.solid[i] || i in libres || maze.ladder[i]) {
                if (puestas > 0) break     // se corto: mejor un tramo corto que dos sueltos
                continue
            }
            maze.ceilClearance[i] = 1.50f
            puestas++
            if (puestas >= 4) break
        }
    }

    /** Chequeo defensivo: ninguna transicion puede quedar imposible de pasar. */
    fun esTransitable(maze: Maze): Boolean {
        for (gy in 0 until maze.gh) {
            for (gx in 0 until maze.gw) {
                if (maze.isSolid(gx, gy)) continue
                val i = maze.index(gx, gy)
                // Ninguna casilla puede tener menos alto del que entra
                // arrastrandose. Se mide el hueco REAL: el teorico miente
                // hacia arriba, y una casilla que promete 0,71 y de verdad
                // tiene 0,63 es una casilla por la que no se pasa.
                if (Maze.altoLibreReal(maze.ceilClearance[i]) < 0.70f) return false
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
