package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.maze.ElAscenso
import com.mggx.laberinto.maze.Maze
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * El nivel 999: el unico que termina afuera.
 *
 * Todos los demas niveles bajan un piso mas. Este sube y sale al patio de tu
 * casa. Por eso es corto: no es un laberinto para resolver, es el ultimo
 * tramo.
 */
class AscensoTest {

    private fun nivel(semilla: Long) = MazeGenerator.generate(ElAscenso.NIVEL, semilla)

    @Test
    fun elUltimoNivelSeTerminaSiempre() {
        for (s in 0L until 40L) {
            val bp = nivel(s * 7919L)
            assertTrue("semilla $s: no se puede llegar a la salida", bp.maze.isSolvable())
        }
    }

    @Test
    fun esCortoDeVerdad() {
        // La promesa del nivel. Si fuera largo dejaria de ser un final y
        // volveria a ser un laberinto mas.
        val largos = (0L until 20L).map { nivel(it * 31L).maze.solutionLength }
        val peor = largos.max()
        assertTrue("el camino mas largo mide $peor casillas: dejo de ser corto", peor <= 26)
        val normal = MazeGenerator.generate(30, 1234L).maze.solutionLength
        assertTrue("no es mas corto que un nivel comun ($peor contra $normal)", peor < normal)
    }

    @Test
    fun sube() {
        // Lo que lo define: se entra abajo y se sale arriba.
        for (s in 0L until 20L) {
            val m = nivel(s * 101L).maze
            val arranque = m.floorY(m.startGx, m.startGy)
            val salida = m.floorY(m.exitGx, m.exitGy)
            assertTrue(
                "semilla $s: la salida esta a ${salida - arranque} m del arranque",
                salida - arranque > 5f
            )
        }
    }

    @Test
    fun elPatioEstaAlAireLibre() {
        for (s in 0L until 20L) {
            val bp = nivel(s * 13L)
            val m = bp.maze
            assertEquals(
                "el patio no mide lo que dice",
                ElAscenso.LADO_PATIO * ElAscenso.LADO_PATIO, bp.patio.size
            )
            for (i in bp.patio) {
                assertTrue("una casilla del patio quedo solida", !m.solid[i])
                assertTrue("una casilla del patio quedo con techo", m.cielo[i])
            }
            // Y la salida cae adentro del patio.
            assertTrue(
                "la salida no esta en el patio",
                m.index(m.exitGx, m.exitGy) in bp.patio
            )
        }
    }

    @Test
    fun elPatioEsPlanoYDeUnaPieza() {
        // Un patio con escalones adentro no es un patio.
        for (s in 0L until 20L) {
            val bp = nivel(s * 17L)
            val m = bp.maze
            val alturas = bp.patio.map { m.floorLevel[it] }.toSet()
            assertEquals("el patio tiene escalones adentro", 1, alturas.size)
        }
    }

    @Test
    fun ningunSaltoSeSubeSinEscalera() {
        // La garantia de siempre: donde el desnivel no se sube caminando, hay
        // escalera en las dos puntas.
        for (s in 0L until 20L) {
            val m = nivel(s * 23L).maze
            for (gy in 0 until m.gh) {
                for (gx in 0 until m.gw) {
                    if (m.isSolid(gx, gy)) continue
                    for (d in 0 until 4) {
                        val vx = gx + intArrayOf(0, 0, -1, 1)[d]
                        val vy = gy + intArrayOf(-1, 1, 0, 0)[d]
                        if (!m.inBounds(vx, vy) || m.isSolid(vx, vy)) continue
                        val salto = abs(m.floorY(gx, gy) - m.floorY(vx, vy))
                        if (salto > Maze.SUBIDA_CAMINANDO) {
                            assertTrue(
                                "salto de $salto m sin escalera en ($gx,$gy)",
                                m.hasLadder(gx, gy) && m.hasLadder(vx, vy)
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun noHayBichosNiTrampasNiMonedasEnElFinal() {
        // Es el final, no una prueba mas.
        val bp = nivel(555L)
        assertTrue(bp.enemies.isEmpty())
        assertTrue(bp.traps.isEmpty())
        assertTrue(bp.coins.isEmpty())
        assertTrue(bp.chests.isEmpty())
        assertTrue("sin antorchas el corredor queda a oscuras", bp.torches.isNotEmpty())
    }

    @Test
    fun sePuedeCaminarDesdeElArranqueHastaElPatio() {
        // La prueba de verdad: una partida que camina el camino minimo y sale.
        val save = SaveData.fromStore(SaveData.memoryStore())
        val s = GameSession(save, ElAscenso.NIVEL, 4242L)
        val c = GameSession.CELL
        val path = s.maze.solutionPath
        val dt = 1f / 60f
        var t = 0f
        var idx = 0
        val input = GameSession.Input()
        while (idx < path.size && t < 300f && s.phase == GameSession.Phase.JUGANDO) {
            val gi = path[idx]
            val tx = (gi % s.maze.gw + 0.5f) * c
            val tz = (gi / s.maze.gw + 0.5f) * c
            val dx = tx - s.posX
            val dz = tz - s.posZ
            if (hypot(dx, dz) < 0.45f) { idx++; continue }
            s.yawDeg = Math.toDegrees(Math.atan2(dx.toDouble(), dz.toDouble())).toFloat()
            input.moveY = 1f
            s.update(dt, input)
            t += dt
        }
        assertEquals("no se llego al patio", GameSession.Phase.GANADO, s.phase)
    }
}
