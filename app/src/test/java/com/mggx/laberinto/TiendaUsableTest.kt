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
 * Dos veces se rompio esto: primero habia que cargar todo a mano en otra
 * pantalla, y despues, con las ranuras llenas, lo comprado se sumaba al stock
 * pero no aparecia en la partida. Ahora las ranuras deciden el ORDEN del bolso,
 * no si algo se puede usar.
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
        // La antorcha queda afuera a proposito: es la que se compra ultima, y
        // su efecto no depende del estado del jugador, asi que sirve para
        // probar de punta a punta que lo comprado se puede usar igual.
        val compras = ItemCatalog.ofKind(ItemKind.CONSUMIBLE)
            .filter { it.unlockLevel <= 1 && it.id != "antorcha_sebo" }.take(ranuras)
        assertEquals("no alcanzan los consumibles para llenar las ranuras", ranuras, compras.size)
        compras.forEach { s.buy(it.id) }
        val antes = s.loadoutList()
        assertTrue(s.ranurasLlenas())

        assertEquals(SaveData.BuyResult.OK, s.buy("antorcha_sebo"))

        assertEquals("le pisaron una ranura al jugador", antes, s.loadoutList())
        assertFalse(s.estaEnRanuraRapida("antorcha_sebo"))
        assertTrue("igual se compro", s.stockOf("antorcha_sebo") > 0)
        // Y, sobre todo, igual se puede usar: antes esto quedaba pago y sin
        // ninguna forma de sacarlo del bolso durante la partida.
        assertTrue("lo comprado quedo inusable", s.bolsaDeMano().contains("antorcha_sebo"))
        assertTrue(GameSession(s, 1).useItem("antorcha_sebo"))
        assertEquals("no se descontó al usarlo", 0, s.stockOf("antorcha_sebo"))
    }

    @Test
    fun todoLoQueTenesEnElBolsoSePuedeUsarEnLaPartida() {
        // Este es el bug que reporto el jugador: compraba dos panes con las
        // ranuras llenas, los veia en la tienda y no los podia usar nunca.
        val s = perfil()
        s.addEcos(100000)
        val disponibles = ItemCatalog.ofKind(ItemKind.CONSUMIBLE).filter { it.unlockLevel <= 1 }
        assertTrue("hacen falta mas consumibles", disponibles.size > s.loadoutSlots())
        disponibles.forEach { s.buy(it.id) }

        val bolso = s.bolsaDeMano()
        for (i in disponibles) {
            assertTrue(
                "${i.id} se compro pero no aparece en el bolso",
                bolso.contains(i.id)
            )
            assertTrue("${i.id} no se puede usar", s.sePuedeUsarEnPartida(i.id))
        }
        // Las ranuras elegidas van primero, para que el orden no cambie solo.
        assertEquals(s.loadoutList(), bolso.take(s.loadoutList().size))
    }

    @Test
    fun loQueSeAcabaSaleDelBolso() {
        val s = perfil()
        s.addEcos(100000)
        s.buy("pan_cueva")
        assertTrue(s.bolsaDeMano().contains("pan_cueva"))
        assertTrue(s.consume("pan_cueva"))
        assertEquals(0, s.stockOf("pan_cueva"))
        assertFalse("un objeto agotado no puede seguir en el bolso",
            s.bolsaDeMano().contains("pan_cueva"))
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
