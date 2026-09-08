package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.Currency
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import com.mggx.laberinto.game.PlayerStats
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los poderes son carisimos y para siempre: si alguno no hiciera nada seria
 * la peor estafa del juego. Aca se comprueba que cada uno cambia algo de
 * verdad en la partida.
 */
class PoderesTest {

    private fun perfil(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    /** Un perfil rico y avanzado, para poder comprar cualquier cosa. */
    private fun perfilRico(): SaveData {
        val save = perfil()
        repeat(80) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
        repeat(400) { save.addVetagris(1) }
        repeat(200) { save.addEcos(500) }
        return save
    }

    private fun comprar(save: SaveData, id: String) {
        val r = save.buy(id)
        assertTrue("no se pudo comprar $id: $r", r == SaveData.BuyResult.OK)
    }

    @Test
    fun hayOchoPoderesYTodosCuestanVetagris() {
        val poderes = ItemCatalog.ofKind(ItemKind.PODER)
        assertTrue("pocos poderes: ${poderes.size}", poderes.size >= 8)
        for (p in poderes) {
            assertEquals("el poder ${p.id} no cuesta Vetagris", Currency.VETAGRIS, p.currency)
            assertTrue("el poder ${p.id} es barato", p.basePrice >= 8)
            assertEquals("un poder no se sube de nivel: ${p.id}", 1, p.maxLevel)
        }
    }

    @Test
    fun cadaPoderCambiaAlgunaEstadistica() {
        // Para cada poder, comprarlo tiene que cambiar el resultado de
        // PlayerStats. Si no cambiara nada, el poder seria decorativo.
        for (p in ItemCatalog.ofKind(ItemKind.PODER)) {
            val save = perfilRico()
            val antes = firma(PlayerStats(save))
            comprar(save, p.id)
            val despues = firma(PlayerStats(save))
            assertTrue("comprar ${p.id} no cambia ninguna estadistica", antes != despues)
        }
    }

    private fun firma(s: PlayerStats): String = listOf(
        s.tieneLinterna, s.posturaLibre, s.saltoExtra, s.sinDanoDeCaida,
        s.verTesoros, s.picosGratis, s.mapaPersistente, s.regenPerSecond,
        s.sigilo, s.invisibleArrastrandose
    ).joinToString("|")

    // ------------------------------------------------------------ linterna

    @Test
    fun sinElPoderNoHayLinterna() {
        val s = GameSession(perfil(), 1, 777L)
        assertFalse(s.toggleLinterna())
        assertEquals(0f, s.carburo, 0.001f)
        assertEquals(0f, s.linternaFuerza(), 0.001f)
    }

    @Test
    fun laLinternaSePrendeGastaYSeApagaSola() {
        val save = perfilRico()
        comprar(save, "pod_linterna")
        val s = GameSession(save, 6, 999L)
        assertEquals("arranca con el tanque lleno", 1f, s.carburo, 0.001f)
        assertTrue(s.toggleLinterna())
        assertTrue("prendida tiene que alumbrar", s.linternaFuerza() > 0f)

        // Un minuto prendida gasta una parte del tanque, pero no todo.
        repeat(60 * 60) { s.update(1f / 60f, GameSession.Input()) }
        assertTrue("tendria que haber gastado carburo", s.carburo < 1f)
        assertTrue("no puede vaciarse en un minuto", s.carburo > 0.2f)

        // Y si se sigue gastando, se apaga sola.
        repeat(60 * 200) { s.update(1f / 60f, GameSession.Input()) }
        assertEquals(0f, s.carburo, 0.001f)
        assertFalse("sin carburo tiene que quedar apagada", s.linternaEncendida)
        assertEquals(0f, s.linternaFuerza(), 0.001f)
    }

    @Test
    fun laEstacionDeCarburoRecarga() {
        val save = perfilRico()
        comprar(save, "pod_linterna")
        val s = GameSession(save, 12, 4242L)
        val estacion = s.carbideStations.firstOrNull()
        assertNotNull("todo nivel tiene al menos una estacion", estacion)

        s.toggleLinterna()
        repeat(60 * 100) { s.update(1f / 60f, GameSession.Input()) }
        val gastado = s.carburo
        assertTrue("tendria que faltar carburo", gastado < 0.9f)

        // Se lo teletransporta a la estacion y se deja pasar un frame.
        val gi = estacion!!
        s.posX = (gi % s.maze.gw + 0.5f) * GameSession.CELL
        s.posZ = (gi / s.maze.gw + 0.5f) * GameSession.CELL
        s.posY = s.maze.floorY(gi % s.maze.gw, gi / s.maze.gw)
        s.update(1f / 60f, GameSession.Input())
        assertEquals("la estacion tiene que llenar el tanque", 1f, s.carburo, 0.001f)
        assertTrue("y quedar marcada como usada", s.estacionUsada(gi))
    }

    @Test
    fun todosLosNivelesTienenEstacionDeCarga() {
        for (level in 1..40) {
            val bp = MazeGenerator.generate(level, level * 313L)
            assertTrue(
                "el nivel $level se quedo sin estaciones de carburo",
                bp.carbide.isNotEmpty()
            )
        }
    }

    // ------------------------------------------------------------- efectos

    @Test
    fun elReptadorSacaLaPenalidadDeIrAgachado() {
        val save = perfilRico()
        val normal = GameSession(save, 2, 55L)
        comprar(save, "pod_reptador")
        val conPoder = GameSession(save, 2, 55L)

        fun avance(s: GameSession): Float {
            val x0 = s.posX; val z0 = s.posZ
            val input = GameSession.Input(moveY = 1f, agacharse = 2)
            repeat(90) { s.update(1f / 60f, input) }
            return kotlin.math.hypot(s.posX - x0, s.posZ - z0)
        }
        val d1 = avance(normal)
        val d2 = avance(conPoder)
        assertTrue("con Reptador tiene que avanzar mas ($d1 vs $d2)", d2 > d1 * 1.3f)
    }

    @Test
    fun losPiesDeCabraSaltanMasYNoDuelenLasCaidas() {
        val save = perfilRico()
        comprar(save, "pod_cabra")
        val s = GameSession(save, 2, 88L)
        s.posY = s.maze.floorY(s.maze.startGx, s.maze.startGy)
        val piso = s.posY

        s.update(1f / 60f, GameSession.Input(saltar = true))
        var maximo = s.posY
        repeat(180) {
            s.update(1f / 60f, GameSession.Input())
            if (s.posY > maximo) maximo = s.posY
        }
        assertTrue("el salto tiene que ser mas alto que el normal", maximo - piso > 1.2f)

        val vida = s.health
        s.posY = piso + 25f
        repeat(400) { s.update(1f / 60f, GameSession.Input()) }
        assertTrue("tendria que haber caido", s.enSuelo)
        assertEquals("con Pies de Cabra ninguna caida lastima", vida, s.health, 0.001f)
    }

    @Test
    fun elPicoEternoDaUnGolpeGratisPorNivel() {
        val save = perfilRico()
        val sinPoder = GameSession(save, 13, 71L)
        assertEquals(0, sinPoder.pickCharges)
        comprar(save, "pod_pico_eterno")
        val conPoder = GameSession(save, 13, 71L)
        assertEquals(1, conPoder.pickCharges)
        // Y se repone al bajar otra vez.
        val otroNivel = GameSession(save, 14, 72L)
        assertEquals(1, otroNivel.pickCharges)
    }

    @Test
    fun laMemoriaDeLaSimaGuardaElMapaDelNivel() {
        val save = perfilRico()
        comprar(save, "pod_memoria_sima")
        val primera = GameSession(save, 10, 3131L)
        // Se revela medio nivel a mano y se pierde la partida.
        var marcadas = 0
        for (i in primera.revealed.indices) {
            if (i % 2 == 0) { primera.revealed[i] = true; marcadas++ }
        }
        primera.forceLose()

        val segunda = GameSession(save, 10, 3131L)
        val recordadas = segunda.revealed.count { it }
        assertTrue(
            "el mapa explorado tendria que volver ($recordadas de $marcadas)",
            recordadas >= marcadas
        )

        // Pero en otro nivel no aplica: es el mapa de ESE nivel.
        val otro = GameSession(save, 11, 4141L)
        assertTrue(
            "el mapa de un nivel no puede colarse en otro",
            otro.revealed.count { it } < recordadas / 2
        )
    }

    @Test
    fun elOjoDeLaVetaMarcaLosTesoros() {
        val save = perfilRico()
        val sinPoder = GameSession(save, 20, 606L)
        comprar(save, "pod_veta")
        val conPoder = GameSession(save, 20, 606L)
        val tesoros = conPoder.pickups.filter {
            it.kind == GameSession.PickupKind.COFRE || it.kind == GameSession.PickupKind.VETAGRIS
        }
        assertTrue("el nivel de prueba no tiene tesoros", tesoros.isNotEmpty())
        for (t in tesoros) {
            assertTrue(
                "el Ojo de la Veta no marco el tesoro en (${t.gx},${t.gy})",
                conPoder.revealed[conPoder.maze.index(t.gx, t.gy)]
            )
        }
        assertTrue(
            "sin el poder no tendria que estar todo marcado",
            conPoder.revealed.count { it } > sinPoder.revealed.count { it }
        )
    }

    @Test
    fun elCorazonDeLaCuevaRegeneraVida() {
        val save = perfilRico()
        comprar(save, "pod_corazon_cueva")
        val s = GameSession(save, 4, 121L)
        s.applyDamage(40f)
        val herido = s.health
        repeat(60 * 12) { s.update(1f / 60f, GameSession.Input()) }
        assertTrue("la vida tendria que subir sola ($herido -> ${s.health})", s.health > herido + 5f)
    }
}
