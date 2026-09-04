package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lo que se compra tiene que poder usarse.
 *
 * Solo se puede usar lo que esta en una ranura rapida, y antes eso habia que
 * hacerlo a mano en otra pantalla: se compraba un objeto y no aparecia nunca.
 */
class TiendaUsableTest {

    private fun perfil(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    @Test
    fun loCompradoQuedaListoParaUsar() {
        val s = perfil()
        s.addEcos(100000)
        assertEquals(SaveData.BuyResult.OK, s.buy("antorcha_sebo"))
        assertTrue(
            "lo comprado no quedo en una ranura rapida",
            s.loadoutList().contains("antorcha_sebo")
        )
        // Y de verdad se puede usar en una partida
        val partida = GameSession(s, 1)
        assertTrue("no se pudo usar lo recien comprado", partida.useItem("antorcha_sebo"))
    }

    @Test
    fun seLlenanLasRanurasEnOrdenYNoSePisanEntreSi() {
        val s = perfil()
        s.addEcos(100000)
        val ranuras = s.loadoutSlots()
        val compras = ItemCatalog.ofKind(ItemKind.CONSUMIBLE)
            .filter { it.unlockLevel <= 1 }
            .take(ranuras + 2)
        assertTrue("hacen falta mas consumibles iniciales", compras.size > ranuras)

        compras.forEach { s.buy(it.id) }
        assertEquals("se paso del numero de ranuras", ranuras, s.loadoutList().size)
        // Las primeras compras son las que quedaron cargadas, no las ultimas.
        assertEquals(compras.take(ranuras).map { it.id }, s.loadoutList())
        assertTrue(s.ranurasLlenas())
    }

    @Test
    fun comprarDeNuevoAlgoQueYaEstaCargadoNoOcupaOtraRanura() {
        val s = perfil()
        s.addEcos(100000)
        s.buy("pan_cueva")
        s.buy("pan_cueva")
        s.buy("pan_cueva")
        assertEquals(3, s.stockOf("pan_cueva"))
        assertEquals(1, s.loadoutList().count { it == "pan_cueva" })
    }

    @Test
    fun loQueSaleDeUnCofreTambienQuedaListo() {
        val s = perfil()
        s.grantConsumable("bengala", 1)
        assertTrue(
            "lo del cofre no quedo en una ranura",
            s.loadoutList().contains("bengala")
        )
    }

    @Test
    fun conLasRanurasLlenasNoSePisaLoQueElJugadorEligio() {
        val s = perfil()
        s.addEcos(100000)
        val ranuras = s.loadoutSlots()
        val compras = ItemCatalog.ofKind(ItemKind.CONSUMIBLE)
            .filter { it.unlockLevel <= 1 }.take(ranuras)
        compras.forEach { s.buy(it.id) }
        val antes = s.loadoutList()

        val otro = ItemCatalog.ofKind(ItemKind.CONSUMIBLE)
            .first { it.unlockLevel <= 1 && it.id !in antes }
        assertEquals(SaveData.BuyResult.OK, s.buy(otro.id))

        assertEquals("le pisaron una ranura al jugador", antes, s.loadoutList())
        assertFalse(s.estaEnRanuraRapida(otro.id))
        assertTrue("igual se compro", s.stockOf(otro.id) > 0)
    }

    @Test
    fun masRanurasPermitenCargarMasCosas() {
        val s = perfil()
        s.addEcos(1000000)
        repeat(ItemCatalog.require("up_bolsillos").maxLevel) { s.buy("up_bolsillos") }
        val ranuras = s.loadoutSlots()
        assertTrue("la mejora no dio mas ranuras", ranuras > 2)
        // A nivel 1 hay menos consumibles desbloqueados que ranuras, asi que se
        // compara contra lo que realmente se puede comprar.
        val disponibles = ItemCatalog.ofKind(ItemKind.CONSUMIBLE).filter { it.unlockLevel <= 1 }
        val esperadas = minOf(ranuras, disponibles.size)
        disponibles.take(esperadas).forEach { s.buy(it.id) }
        assertEquals(esperadas, s.loadoutList().size)
    }
}
