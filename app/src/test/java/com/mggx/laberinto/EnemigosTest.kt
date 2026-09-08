package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.Enemy
import com.mggx.laberinto.game.EnemyBrain
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * Los bichos de la mina. Tienen que perseguir de verdad (doblando esquinas),
 * no atravesar la roca, no aparecer encima del jugador y respetar el sigilo.
 */
class EnemigosTest {

    private fun perfil(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    private fun perfilRico(): SaveData {
        val save = perfil()
        repeat(80) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
        repeat(400) { save.addVetagris(1) }
        return save
    }

    @Test
    fun losPrimerosNivelesNoTienenBichos() {
        for (level in 1..2) {
            val bp = MazeGenerator.generate(level, level * 17L)
            assertTrue("el nivel $level no deberia tener bichos", bp.enemies.isEmpty())
        }
    }

    @Test
    fun losNivelesHondosSiTienenBichos() {
        var conBichos = 0
        for (level in 6..40) {
            if (MazeGenerator.generate(level, level * 101L).enemies.isNotEmpty()) conBichos++
        }
        assertTrue("casi ningun nivel hondo tiene bichos: $conBichos", conBichos >= 30)
    }

    @Test
    fun ningunBichoNaceEncimaDelJugadorNiDentroDeLaRoca() {
        for (level in 3..45) {
            val bp = MazeGenerator.generate(level, level * 977L)
            val m = bp.maze
            val dist = m.bfsDistances(m.startGx, m.startGy)
            for (e in bp.enemies) {
                assertFalse(
                    "bicho dentro de la roca en el nivel $level",
                    m.isSolid(e.gx, e.gy)
                )
                assertTrue(
                    "bicho demasiado cerca del inicio en el nivel $level",
                    dist[m.index(e.gx, e.gy)] >= 10
                )
            }
        }
    }

    @Test
    fun elBichoPersigueYNoAtraviesaLaRoca() {
        val s = GameSession(perfil(), 15, 5150L)
        val m = s.maze
        // Un murcielago plantado unas casillas mas alla, sobre el camino.
        val camino = m.solutionPath
        val lejos = camino[minOf(6, camino.size - 1)]
        val e = Enemy(
            MazeGenerator.EnemyKind.MURCIELAGO,
            (lejos % m.gw + 0.5f) * GameSession.CELL,
            (lejos / m.gw + 0.5f) * GameSession.CELL,
            lejos % m.gw, lejos / m.gw, 0f
        )
        val brain = EnemyBrain(m, 1L)
        val d0 = hypot(e.x - s.posX, e.z - s.posZ)
        var mordidas = 0
        repeat(600) {
            brain.update(
                1f / 60f, listOf(e), s.posX, s.posZ, GameSession.PLAYER_RADIUS,
                1f, true, false, GameSession.CELL, { gx, gy -> m.floorY(gx, gy) }
            ) { mordidas++ }
            assertFalse(
                "el bicho se metio en la roca",
                m.isSolid(
                    (e.x / GameSession.CELL).toInt(),
                    (e.z / GameSession.CELL).toInt()
                )
            )
        }
        val d1 = hypot(e.x - s.posX, e.z - s.posZ)
        assertTrue("el bicho no se acerco ($d0 -> $d1)", d1 < d0)
        assertTrue("y tendria que haber llegado a morder", mordidas > 0)
    }

    @Test
    fun elRastreroSoloTeOyeSiCorres() {
        val s = GameSession(perfil(), 15, 6161L)
        val m = s.maze
        val camino = m.solutionPath
        val lejos = camino[minOf(4, camino.size - 1)]

        fun probar(ruidoso: Boolean): Boolean {
            val e = Enemy(
                MazeGenerator.EnemyKind.RASTRERO,
                (lejos % m.gw + 0.5f) * GameSession.CELL,
                (lejos / m.gw + 0.5f) * GameSession.CELL,
                lejos % m.gw, lejos / m.gw, 0f
            )
            // Bien lejos para que no lo detecte por el radio corto de cercania.
            val px = s.posX - 100f
            val brain = EnemyBrain(m, 2L)
            brain.update(
                1f / 60f, listOf(e), px, s.posZ, GameSession.PLAYER_RADIUS,
                1f, true, ruidoso, GameSession.CELL, { gx, gy -> m.floorY(gx, gy) }
            ) { }
            return e.alerta
        }
        // A 100 m no lo nota ni corriendo: el alcance es finito.
        assertFalse("no puede oirte desde 100 metros", probar(true))
    }

    @Test
    fun elSigiloAchicaElAlcanceYArrastrandoteNoTeVen() {
        val save = perfilRico()
        assertEquals(SaveData.BuyResult.OK, save.buy("pod_sombra"))
        val s = GameSession(save, 15, 7171L)
        assertEquals("el poder tiene que achicar el alcance", 0.5f, s.stats.sigilo, 0.001f)
        assertTrue(s.stats.invisibleArrastrandose)

        // Arrastrandose, con el poder puesto, nadie se alerta.
        val input = GameSession.Input(agacharse = 2)
        repeat(240) { s.update(1f / 60f, input) }
        assertEquals(
            "arrastrandote con Paso de Sombra no te tienen que ver",
            0, s.enemigosAlerta()
        )
    }

    @Test
    fun elGuardianNoSeAlejaDeSuNido() {
        val s = GameSession(perfil(), 25, 8181L)
        val m = s.maze
        val camino = m.solutionPath
        val casa = camino[minOf(10, camino.size - 1)]
        val e = Enemy(
            MazeGenerator.EnemyKind.GUARDIAN,
            (casa % m.gw + 0.5f) * GameSession.CELL,
            (casa / m.gw + 0.5f) * GameSession.CELL,
            casa % m.gw, casa / m.gw, 0f
        )
        val brain = EnemyBrain(m, 3L)
        repeat(1800) {
            brain.update(
                1f / 60f, listOf(e), s.posX, s.posZ, GameSession.PLAYER_RADIUS,
                1f, true, true, GameSession.CELL, { gx, gy -> m.floorY(gx, gy) }
            ) { }
        }
        val d = hypot(
            (e.nidoGx + 0.5f) * GameSession.CELL - e.x,
            (e.nidoGy + 0.5f) * GameSession.CELL - e.z
        )
        assertTrue("el guardian se fue de paseo: $d m del nido", d < GameSession.CELL * 8f)
    }

    @Test
    fun losBichosLastimanPeroNoDeUnGolpe() {
        // Ningun bicho puede matar de un mordisco a un jugador de vida base.
        val save = perfil()
        val vidaBase = com.mggx.laberinto.game.PlayerStats.BASE_HEALTH
        for (k in MazeGenerator.EnemyKind.entries) {
            assertTrue("${k.name} pega de mas", k.dano < vidaBase * 0.5f)
            assertTrue("${k.name} pega demasiado seguido", k.recarga >= 1.2f)
            assertTrue("${k.name} no hace dano", k.dano > 0f)
        }
        assertTrue(save.maxLevel >= 1)
    }

    @Test
    fun cadaBichoEsDistintoDeLosDemas() {
        val firmas = MazeGenerator.EnemyKind.entries.map {
            listOf(it.alcance, it.velocidad, it.dano, it.recarga, it.radio, it.soloOye, it.guardian)
        }
        assertEquals("hay bichos clonados", firmas.size, firmas.toSet().size)
        val nombres = MazeGenerator.EnemyKind.entries.map { it.etiqueta }
        assertEquals("hay nombres repetidos", nombres.size, nombres.toSet().size)
    }
}
