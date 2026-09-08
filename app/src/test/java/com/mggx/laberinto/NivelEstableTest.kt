package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Un nivel tiene que ser SIEMPRE el mismo laberinto. Si cambiara en cada
 * intento, el boton "Reintentar" no reintentaria el nivel: te tiraria a otro.
 */
class NivelEstableTest {

    private fun perfil() = SaveData.fromStore(SaveData.memoryStore())

    @Test
    fun reintentarDaExactamenteElMismoNivel() {
        for (level in intArrayOf(1, 3, 12, 27, 44)) {
            val a = GameSession(perfil(), level)
            val b = GameSession(perfil(), level)
            assertTrue("el nivel $level cambio de forma", a.maze.solid.contentEquals(b.maze.solid))
            assertEquals("el nivel $level cambio de inicio", a.maze.startCol, b.maze.startCol)
            assertEquals("el nivel $level cambio de salida", a.maze.exitCol, b.maze.exitCol)
            assertEquals("el nivel $level cambio los ecos", a.blueprint.coins, b.blueprint.coins)
            assertEquals("el nivel $level cambio las trampas", a.blueprint.traps.size, b.blueprint.traps.size)
            assertEquals("el nivel $level cambio la orientacion inicial", a.yawDeg, b.yawDeg, 0.001f)
        }
    }

    @Test
    fun cadaNivelEsDistintoDelAnterior() {
        var iguales = 0
        for (level in 1..30) {
            val a = GameSession(perfil(), level)
            val b = GameSession(perfil(), level + 1)
            if (a.maze.gw == b.maze.gw && a.maze.gh == b.maze.gh &&
                a.maze.solid.contentEquals(b.maze.solid)
            ) iguales++
        }
        assertEquals("hay niveles seguidos con el mismo laberinto", 0, iguales)
    }

    @Test
    fun laSemillaPorNivelNoSeRepiteEnElRangoJugable() {
        val vistas = HashSet<Long>()
        for (level in 1..200) {
            assertTrue(
                "la semilla del nivel $level ya la usaba otro nivel",
                vistas.add(GameSession.seedForLevel(level))
            )
        }
    }
}
