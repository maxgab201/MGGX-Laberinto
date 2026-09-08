package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.Currency
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveDataTest {

    private fun nuevo(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    @Test
    fun arrancaComoCorresponde() {
        val s = nuevo()
        assertEquals(0, s.balance(Currency.ECOS))
        assertEquals(0, s.balance(Currency.VETAGRIS))
        assertEquals(1, s.maxLevel)
        assertEquals(1, s.currentLevel)
        // Los objetos de arranque ya vienen puestos
        ItemCatalog.defaultsOwned.forEach { assertTrue("falta $it", s.isOwned(it)) }
        assertEquals("cos_guantes_cuero", s.cosmeticGloves)
        assertEquals("cos_luz_calida", s.cosmeticLight)
    }

    @Test
    fun noSePuedeComprarSinPlata() {
        val s = nuevo()
        assertEquals(SaveData.BuyResult.SIN_FONDOS, s.buy("antorcha_sebo"))
        assertEquals(0, s.stockOf("antorcha_sebo"))
    }

    @Test
    fun comprarConsumibleDescuentaYSuma() {
        val s = nuevo()
        s.addEcos(500)
        val precio = ItemCatalog.require("antorcha_sebo").basePrice
        assertEquals(SaveData.BuyResult.OK, s.buy("antorcha_sebo"))
        assertEquals(500 - precio, s.balance(Currency.ECOS))
        assertEquals(1, s.stockOf("antorcha_sebo"))
        assertEquals(SaveData.BuyResult.OK, s.buy("antorcha_sebo"))
        assertEquals(2, s.stockOf("antorcha_sebo"))
    }

    @Test
    fun lasMejorasSubenDeNivelYSeFrenanEnElTope() {
        val s = nuevo()
        s.addEcos(200000)
        val item = ItemCatalog.require("up_botas")
        var gastadoAnterior = 0
        for (n in 1..item.maxLevel) {
            val antes = s.balance(Currency.ECOS)
            assertEquals(SaveData.BuyResult.OK, s.buy(item.id))
            val gastado = antes - s.balance(Currency.ECOS)
            assertTrue("el precio no sube", gastado > gastadoAnterior)
            gastadoAnterior = gastado
            assertEquals(n, s.ownedLevel(item.id))
        }
        assertEquals(SaveData.BuyResult.AL_MAXIMO, s.buy(item.id))
        assertEquals(item.maxLevel, s.ownedLevel(item.id))
    }

    @Test
    fun noSeCompraLoBloqueado() {
        val s = nuevo()
        s.addEcos(100000)
        val bloqueado = ItemCatalog.all.first { it.unlockLevel > 1 }
        assertEquals(SaveData.BuyResult.BLOQUEADO, s.buy(bloqueado.id))
    }

    @Test
    fun lasReliquiasRespetanLasTresRanuras() {
        val s = nuevo()
        s.addVetagris(100)
        // Se desbloquean todas subiendo de nivel
        repeat(40) { s.onLevelCompleted(s.maxLevel, 1000, 10) }
        val reliquias = ItemCatalog.ofKind(ItemKind.RELIQUIA).take(5)
        reliquias.forEach { assertEquals(SaveData.BuyResult.OK, s.buy(it.id)) }
        assertTrue(s.relics().size <= ItemCatalog.RELIC_SLOTS)
        // Equipar una quinta expulsa a la mas vieja, nunca supera el tope
        s.toggleRelic(reliquias[4].id)
        assertTrue(s.relics().size <= ItemCatalog.RELIC_SLOTS)
        assertTrue(s.isRelicEquipped(reliquias[4].id))
    }

    @Test
    fun lasRanurasRapidasCrecenConBolsillosProfundos() {
        val s = nuevo()
        assertEquals(2, s.loadoutSlots())
        s.addEcos(500000)
        s.buy("up_bolsillos")
        assertEquals(3, s.loadoutSlots())
        s.buy("up_bolsillos")
        assertEquals(4, s.loadoutSlots())
    }

    @Test
    fun consumirGastaYVaciaLaRanura() {
        val s = nuevo()
        s.grantConsumable("pan_cueva", 2)
        assertTrue(s.toggleLoadout("pan_cueva"))
        assertTrue(s.consume("pan_cueva"))
        assertEquals(1, s.stockOf("pan_cueva"))
        assertTrue(s.loadoutList().contains("pan_cueva"))
        assertTrue(s.consume("pan_cueva"))
        assertEquals(0, s.stockOf("pan_cueva"))
        assertFalse("la ranura quedo con un objeto inexistente",
            s.loadoutList().contains("pan_cueva"))
        assertFalse(s.consume("pan_cueva"))
    }

    @Test
    fun elProgresoSobreviveAlGuardadoYCarga() {
        val store = SaveData.memoryStore()
        val a = SaveData.fromStore(store)
        a.addEcos(1234)
        a.addVetagris(7)
        a.grantConsumable("bengala", 3)
        a.onLevelCompleted(1, 45_000, 250)
        a.settings.musicVolume = 0.33f
        a.settings.leftHanded = true
        a.settings.quality = 1
        a.save()

        val b = SaveData.fromStore(store)
        assertEquals(1234, b.balance(Currency.ECOS))
        assertEquals(7, b.balance(Currency.VETAGRIS))
        assertEquals(3, b.stockOf("bengala"))
        assertEquals(2, b.maxLevel)
        assertEquals(45_000, b.bestTimeMs)
        assertEquals(0.33f, b.settings.musicVolume, 0.001f)
        assertTrue(b.settings.leftHanded)
        assertEquals(1, b.settings.quality)
    }

    @Test
    fun unPerfilCorruptoNoRompeNada() {
        val store = object : SaveData.Store {
            var data: String? = "{{{ esto no es json"
            override fun read(): String? = data
            override fun write(json: String) { data = json }
        }
        val s = SaveData.fromStore(store)
        assertEquals(0, s.balance(Currency.ECOS))
        assertEquals(1, s.maxLevel)
        assertTrue(s.isOwned("cos_guantes_cuero"))
    }

    @Test
    fun borrarProgresoDejaTodoEnCero() {
        val s = nuevo()
        s.addEcos(9999); s.addVetagris(20)
        s.grantConsumable("bengala", 5)
        s.onLevelCompleted(1, 1000, 5)
        s.resetProgress()
        assertEquals(0, s.balance(Currency.ECOS))
        assertEquals(0, s.balance(Currency.VETAGRIS))
        assertEquals(1, s.maxLevel)
        assertEquals(0, s.stockOf("bengala"))
        assertEquals(0, s.totalWins)
        assertTrue(s.isOwned("cos_guantes_cuero"))
    }

    @Test
    fun completarNivelDesbloqueaElSiguiente() {
        val s = nuevo()
        s.onLevelCompleted(1, 30_000, 100)
        assertEquals(2, s.maxLevel)
        assertEquals(2, s.currentLevel)
        // Rejugar un nivel viejo no baja el maximo
        s.setCurrentLevel(1)
        s.onLevelCompleted(1, 20_000, 90)
        assertEquals(2, s.maxLevel)
    }

    @Test
    fun elNivelActualNuncaPasaElMaximo() {
        val s = nuevo()
        s.setCurrentLevel(50)
        assertEquals(1, s.currentLevel)
    }
}
