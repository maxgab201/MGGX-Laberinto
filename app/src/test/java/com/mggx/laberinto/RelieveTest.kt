package com.mggx.laberinto

import com.mggx.laberinto.maze.Maze
import com.mggx.laberinto.maze.MazeGenerator
import com.mggx.laberinto.maze.ReliefGenerator
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * El relieve le da altura a la cueva, pero no puede volver imposible un nivel.
 * Entre dos casillas vecinas el piso cambia como mucho un escalon (que se sube
 * caminando) o hay escalera. El techo puede obligar a agacharse, nunca a mas.
 */
class RelieveTest {

    private val dx = intArrayOf(1, -1, 0, 0)
    private val dy = intArrayOf(0, 0, 1, -1)

    @Test
    fun ningunDesnivelDejaSinSalidaEnNingunNivel() {
        for (level in 1..90) {
            val m = MazeGenerator.generate(level, level * 7717L).maze
            assertTrue(
                "el relieve del nivel $level corta el paso",
                ReliefGenerator.esTransitable(m)
            )
        }
    }

    @Test
    fun losDesnivelesGrandesSiempreTienenEscalera() {
        for (level in intArrayOf(8, 15, 27, 40, 66)) {
            val m = MazeGenerator.generate(level, level * 131L).maze
            for (gy in 0 until m.gh) for (gx in 0 until m.gw) {
                if (m.isSolid(gx, gy)) continue
                for (d in 0 until 4) {
                    val nx = gx + dx[d]
                    val ny = gy + dy[d]
                    if (!m.inBounds(nx, ny) || m.isSolid(nx, ny)) continue
                    val desnivel = abs(m.floorY(nx, ny) - m.floorY(gx, gy))
                    if (desnivel > Maze.SUBIDA_CAMINANDO) {
                        assertTrue(
                            "desnivel de $desnivel m sin escalera en el nivel $level",
                            m.hasLadder(gx, gy) || m.hasLadder(nx, ny)
                        )
                    }
                }
            }
        }
    }

    @Test
    fun elTechoNuncaBajaDeLoQueEntraArrastrandose() {
        for (level in 1..60) {
            val m = MazeGenerator.generate(level, level * 977L).maze
            for (i in m.ceilClearance.indices) {
                if (m.solid[i]) continue
                assertTrue(
                    "techo de ${m.ceilClearance[i]} m en el nivel $level",
                    m.ceilClearance[i] >= 0.70f
                )
            }
        }
    }

    @Test
    fun elInicioYLaSalidaSiempreSonDePie() {
        for (level in 1..60) {
            val m = MazeGenerator.generate(level, level * 311L).maze
            assertTrue(
                "el inicio del nivel $level obliga a agacharse",
                m.ceilClearance[m.index(m.startGx, m.startGy)] >= Maze.ALTO_NORMAL - 0.01f
            )
            assertTrue(
                "la salida del nivel $level obliga a agacharse",
                m.ceilClearance[m.index(m.exitGx, m.exitGy)] >= Maze.ALTO_NORMAL - 0.01f
            )
        }
    }

    @Test
    fun losPrimerosNivelesSonPlanosYSinTramosBajos() {
        for (level in 1..2) {
            val m = MazeGenerator.generate(level, level * 53L).maze
            assertTrue(
                "el nivel $level ya tiene desniveles",
                m.floorLevel.all { it == 0 }
            )
        }
        // Del 2 en adelante, y no del 1: el nivel 1 es el tutorial y tiene un
        // unico tramo bajo puesto a mano sobre el camino a la salida, para que
        // el paso "agachate" ensene algo de verdad en vez de hacer apretar un
        // boton al pedo. Ese tramo lo cuida TutorialTest.
        //
        // Se mide si se pasa DE PIE, no si el techo mide exactamente lo
        // normal: desde que la cueva tiene salones altos y galerias bajas,
        // "distinto de 3,40" ya no quiere decir "hay que agacharse". Una
        // galeria de 2,70 se cruza caminando igual que una de 5,40.
        for (level in 2..3) {
            val m = MazeGenerator.generate(level, level * 59L).maze
            for (i in m.ceilClearance.indices) {
                if (m.solid[i]) continue
                assertTrue(
                    "el nivel $level ya obliga a agacharse",
                    Maze.altoLibreReal(m.ceilClearance[i]) >= 1.9f
                )
            }
        }
    }

    @Test
    fun losNivelesAvanzadosTienenRelieveDeVerdad() {
        var conDesnivel = 0
        var conTramoBajo = 0
        var conEscalera = 0
        for (level in 15..45) {
            val m = MazeGenerator.generate(level, level * 89L).maze
            if (m.floorLevel.any { it != 0 }) conDesnivel++
            if (m.ceilClearance.any { !it.isNaN() && it < Maze.ALTO_NORMAL - 0.01f }) conTramoBajo++
            if (m.ladder.any { it }) conEscalera++
        }
        assertTrue("casi ningun nivel tiene desniveles ($conDesnivel)", conDesnivel > 25)
        assertTrue("casi ningun nivel tiene tramos bajos ($conTramoBajo)", conTramoBajo > 25)
        assertTrue("casi ningun nivel tiene escaleras ($conEscalera)", conEscalera > 15)
    }

    @Test
    fun elRelieveEsEstablePorNivel() {
        for (level in intArrayOf(9, 22, 37)) {
            val a = MazeGenerator.generate(level, level * 1000L).maze
            val b = MazeGenerator.generate(level, level * 1000L).maze
            assertTrue(a.floorLevel.contentEquals(b.floorLevel))
            assertTrue(a.ceilClearance.contentEquals(b.ceilClearance))
            assertTrue(a.ladder.contentEquals(b.ladder))
        }
    }

    // ------------------------------------------------ mirar el relieve
    //
    // Lo de arriba comprueba que el relieve no ROMPA el nivel. Lo de aca abajo
    // mira si el relieve es BUENO, que es otra cosa y hasta ahora no se medía.

    private fun nivel(l: Int, semilla: Long = l * 7919L) =
        MazeGenerator.generate(l, semilla).maze

    @Test
    fun retratarElRelieveDeVariosNiveles() {
        val niveles = listOf(1, 6, 15, 30, 55).map { "nivel $it" to nivel(it) }
        val f = VisorDeRelieve.hoja(niveles, "relieve")
        println("FOTO: ${f.absolutePath}")
        for ((nombre, m) in niveles) {
            val s = VisorDeRelieve.saltosEntreVecinas(m)
            println(
                "$nombre: saltos " + s.take(6).joinToString(" ") +
                    "  techos " + resumenDeTechos(m) +
                    "  gatera %.0f%%".format(VisorDeRelieve.fraccionDeGatera(m) * 100) +
                    "  desnivel-de-pared %.2f".format(VisorDeRelieve.desnivelEntreVecinasDePared(m))
            )
        }
        assertTrue(f.exists() && f.length() > 1000)
    }

    private fun resumenDeTechos(m: Maze): String {
        var min = Float.MAX_VALUE; var max = -Float.MAX_VALUE; var suma = 0.0; var n = 0
        for (i in 0 until m.gw * m.gh) {
            if (m.solid[i]) continue
            val c = m.ceilClearance[i]
            if (c < min) min = c
            if (c > max) max = c
            suma += c; n++
        }
        return "min %.2f med %.2f max %.2f".format(min, suma / n, max)
    }

    @Test
    fun todosLosNivelesSiguenSiendoTransitables() {
        for (level in intArrayOf(1, 2, 5, 9, 14, 21, 33, 48, 60, 77)) {
            for (s in 0 until 12) {
                val m = nivel(level, level * 131L + s)
                assertTrue(
                    "nivel $level semilla $s quedo con una transicion imposible",
                    ReliefGenerator.esTransitable(m)
                )
            }
        }
    }

    @Test
    fun laAlturaSigueAlEspacioYNoALosPasillos() {
        // LA medida de esta tanda.
        //
        // Antes la altura se propagaba por BFS desde el inicio, o sea que
        // viajaba por los PASILLOS. Dos galerias separadas por una sola pared
        // podian quedar a varios metros de desnivel, porque el relieve habia
        // llegado a cada una por un camino distinto. En planta se veian fideos
        // de colores; adentro se sentia que la cueva no tenia forma.
        //
        // Se mide el desnivel medio entre casillas abiertas que estan a dos
        // casillas de distancia pero NO son vecinas directas: justamente las
        // que el BFS no tenia obligacion de parecer. Con el campo de terreno,
        // esas parejas casi siempre comparten altura.
        for (level in intArrayOf(8, 16, 29, 44, 61)) {
            var suma = 0f
            var n = 0
            for (s in 0 until 6) {
                suma += VisorDeRelieve.desnivelEntreVecinasDePared(nivel(level, level * 311L + s))
                n++
            }
            // El umbral sale de medir el generador VIEJO con esta misma
            // cuenta: daba entre 1,58 y 2,36 escalones. El de ahora da entre
            // 0,48 y 0,77. Se deja en 1,00 para que agarre una vuelta atras
            // sin depender de la semilla.
            val medio = suma / n
            assertTrue(
                "nivel $level: dos casillas pegadas por la pared se llevan %.2f escalones "
                    .format(medio) + "de desnivel: la altura viaja por los pasillos, no por el mapa",
                medio < 1.00f
            )
        }
    }

    @Test
    fun laGateraEsUnaExcepcionYNoLaMitadDeLaCueva() {
        // Un tramo bajo se siente porque es raro. Si media cueva obliga a
        // agacharse, agacharse deja de ser un momento y pasa a ser la forma
        // normal de caminar.
        for (level in intArrayOf(12, 25, 40, 58)) {
            for (s in 0 until 4) {
                val f = VisorDeRelieve.fraccionDeGatera(nivel(level, level * 641L + s))
                assertTrue(
                    "nivel $level semilla $s: el ${(f * 100).toInt()}% de la cueva es gatera",
                    f < 0.22f
                )
            }
        }
    }

    @Test
    fun laCuevaTieneSalonesAltosYGaleriasBajas() {
        // Todas las galerias median exactamente lo mismo (ALTO_NORMAL) salvo
        // los tramos bajos sueltos. Un techo constante es lo que mas hace que
        // una cueva se sienta un pasillo de oficina: adentro de una cueva de
        // verdad el techo sube y baja todo el tiempo.
        for (level in intArrayOf(7, 18, 31, 52)) {
            val m = nivel(level, level * 97L)
            var altos = 0
            var normales = 0
            var n = 0
            for (i in 0 until m.gw * m.gh) {
                if (m.solid[i]) continue
                n++
                if (m.ceilClearance[i] > Maze.ALTO_NORMAL + 0.4f) altos++
                if (abs(m.ceilClearance[i] - Maze.ALTO_NORMAL) < 0.01f) normales++
            }
            assertTrue(
                "nivel $level: ninguna casilla tiene el techo mas alto que lo normal",
                altos > n / 20
            )
            assertTrue(
                "nivel $level: el ${(normales * 100 / n)}% de la cueva mide exactamente lo mismo",
                normales < n * 0.7f
            )
        }
    }

    @Test
    fun losTramosBajosSiguenSiendoPasables() {
        // El alto que se promete tiene que existir DESPUES de descontar la
        // panza del techo. Si no, el juego te deja pasar por un lugar donde la
        // roca dibujada no te da y la camara termina adentro de la piedra.
        for (level in 1..60 step 7) {
            for (s in 0 until 8) {
                val m = nivel(level, level * 53L + s)
                for (i in 0 until m.gw * m.gh) {
                    if (m.solid[i]) continue
                    assertTrue(
                        "nivel $level: una casilla promete ${m.ceilClearance[i]} y deja " +
                            "${Maze.altoLibreReal(m.ceilClearance[i])} de hueco real",
                        Maze.altoLibreReal(m.ceilClearance[i]) >= 0.70f
                    )
                }
            }
        }
    }
}
