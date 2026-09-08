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
        for (level in 1..3) {
            val m = MazeGenerator.generate(level, level * 59L).maze
            for (i in m.ceilClearance.indices) {
                if (m.solid[i]) continue
                assertTrue(
                    "el nivel $level ya obliga a agacharse",
                    m.ceilClearance[i] >= Maze.ALTO_NORMAL - 0.01f
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
}
