package com.mggx.laberinto

import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MazeGeneratorTest {

    /** El requisito principal: TODO nivel generado tiene que ser resoluble. */
    @Test
    fun todoNivelEsResoluble() {
        for (level in 1..120) {
            for (s in 0 until 6) {
                val seed = level * 1_000_003L + s * 7919L
                val bp = MazeGenerator.generate(level, seed)
                assertTrue(
                    "Nivel $level semilla $seed no tiene solucion",
                    bp.maze.isSolvable()
                )
                assertTrue(
                    "Nivel $level semilla $seed tiene camino de largo 0",
                    bp.maze.solutionLength > 0
                )
            }
        }
    }

    @Test
    fun elCaminoReconstruidoEsValidoYContinuo() {
        for (level in intArrayOf(1, 5, 17, 40, 77)) {
            val bp = MazeGenerator.generate(level, level * 31L)
            val maze = bp.maze
            val path = maze.solutionPath
            assertTrue("camino vacio en nivel $level", path.size >= 2)
            assertEquals(maze.index(maze.startGx, maze.startGy), path.first())
            assertEquals(maze.index(maze.exitGx, maze.exitGy), path.last())
            for (i in path.indices) {
                val x = path[i] % maze.gw
                val y = path[i] / maze.gw
                assertFalse("el camino atraviesa roca en nivel $level", maze.isSolid(x, y))
                if (i > 0) {
                    val px = path[i - 1] % maze.gw
                    val py = path[i - 1] / maze.gw
                    val d = Math.abs(px - x) + Math.abs(py - y)
                    assertEquals("salto no adyacente en el camino", 1, d)
                }
            }
        }
    }

    @Test
    fun elLaberintoCreceConElNivel() {
        val (c1, r1) = MazeGenerator.cellsForLevel(1)
        val (c10, r10) = MazeGenerator.cellsForLevel(10)
        val (c40, r40) = MazeGenerator.cellsForLevel(40)
        assertTrue("nivel 10 no es mas grande que el 1", c10 * r10 > c1 * r1)
        assertTrue("nivel 40 no es mas grande que el 10", c40 * r40 > c10 * r10)
    }

    /** El recorrido tiene que hacerse mas largo a medida que se avanza. */
    @Test
    fun elRecorridoSeAlarga() {
        fun promedio(level: Int): Double {
            var t = 0.0
            for (s in 0 until 12) t += MazeGenerator.generate(level, s * 104729L + level).maze.solutionLength
            return t / 12.0
        }
        val a = promedio(2)
        val b = promedio(15)
        val c = promedio(45)
        assertTrue("nivel 15 ($b) no es mas largo que el 2 ($a)", b > a * 1.4)
        assertTrue("nivel 45 ($c) no es mas largo que el 15 ($b)", c > b * 1.2)
    }

    @Test
    fun elBordeExteriorSiempreEsSolido() {
        val bp = MazeGenerator.generate(23, 424242L)
        val m = bp.maze
        for (x in 0 until m.gw) {
            assertTrue(m.isSolid(x, 0)); assertTrue(m.isSolid(x, m.gh - 1))
        }
        for (y in 0 until m.gh) {
            assertTrue(m.isSolid(0, y)); assertTrue(m.isSolid(m.gw - 1, y))
        }
    }

    @Test
    fun inicioYSalidaSonDistintosYTransitables() {
        for (level in 1..60) {
            val m = MazeGenerator.generate(level, level * 977L).maze
            assertTrue(m.isOpen(m.startGx, m.startGy))
            assertTrue(m.isOpen(m.exitGx, m.exitGy))
            assertTrue(
                "inicio y salida coinciden en nivel $level",
                m.startCol != m.exitCol || m.startRow != m.exitRow
            )
        }
    }

    @Test
    fun objetosYTrampasCaenEnCasillasTransitables() {
        for (level in intArrayOf(1, 3, 12, 33, 58)) {
            val bp = MazeGenerator.generate(level, level * 7919L)
            val m = bp.maze
            val todos = bp.coins + bp.bigCoins + bp.crystals + bp.chests + bp.torches + bp.stalagmites
            for (i in todos) {
                assertTrue("objeto dentro de roca (nivel $level)", m.isOpen(i % m.gw, i / m.gw))
            }
            for (t in bp.traps) {
                assertTrue("trampa dentro de roca (nivel $level)", m.isOpen(t.gx, t.gy))
            }
        }
    }

    @Test
    fun noHayTrampasPegadasAlInicio() {
        for (level in 3..40) {
            val bp = MazeGenerator.generate(level, level * 5051L)
            val m = bp.maze
            for (t in bp.traps) {
                val d = Math.abs(t.gx - m.startGx) + Math.abs(t.gy - m.startGy)
                assertTrue("trampa demasiado cerca del inicio en nivel $level", d >= 3)
            }
        }
    }

    @Test
    fun laMismaSemillaDaElMismoLaberinto() {
        val a = MazeGenerator.generate(19, 123456789L)
        val b = MazeGenerator.generate(19, 123456789L)
        assertTrue(a.maze.solid.contentEquals(b.maze.solid))
        assertEquals(a.maze.exitCol, b.maze.exitCol)
        assertEquals(a.coins, b.coins)
    }
}
