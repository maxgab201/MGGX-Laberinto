package com.mggx.laberinto

import com.mggx.laberinto.game.Currency
import com.mggx.laberinto.game.EffectType
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemCatalogTest {

    @Test
    fun hayAlMenosCincuentaObjetos() {
        assertTrue("solo hay ${ItemCatalog.all.size} objetos", ItemCatalog.all.size >= 50)
    }

    @Test
    fun losIdentificadoresSonUnicos() {
        val ids = ItemCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun losNombresSonUnicos() {
        val nombres = ItemCatalog.all.map { it.name }
        assertEquals(
            "hay nombres repetidos: " + nombres.groupBy { it }.filter { it.value.size > 1 }.keys,
            nombres.size, nombres.toSet().size
        )
    }

    @Test
    fun ningunObjetoTieneElMismoEfectoQueOtro() {
        // Cada objeto es unico en (tipo, magnitud, duracion, cargas, color).
        val firmas = ItemCatalog.all.map {
            listOf(it.effect.type, it.effect.magnitude, it.effect.duration, it.effect.charges, it.effect.color)
        }
        val repetidos = firmas.groupBy { it }.filter { it.value.size > 1 }
        assertTrue("efectos duplicados: $repetidos", repetidos.isEmpty())
    }

    @Test
    fun todosTienenTextoYIcono() {
        for (i in ItemCatalog.all) {
            assertTrue("nombre vacio en ${i.id}", i.name.isNotBlank())
            assertTrue("descripcion corta en ${i.id}", i.desc.length >= 20)
            assertNotNull("sin icono: ${i.id}", i.icon)
        }
    }

    @Test
    fun losPreciosSonCoherentes() {
        for (i in ItemCatalog.all) {
            assertTrue("precio negativo en ${i.id}", i.basePrice >= 0)
            if (i.id !in ItemCatalog.defaultsOwned) {
                assertTrue("objeto gratis no inicial: ${i.id}", i.basePrice > 0)
            }
            if (i.isLeveled) {
                // El precio siempre tiene que subir de un nivel al siguiente.
                for (l in 0 until i.maxLevel - 1) {
                    assertTrue(
                        "el precio no sube en ${i.id} nivel $l",
                        i.priceAt(l + 1) > i.priceAt(l)
                    )
                }
            }
        }
    }

    @Test
    fun cadaCategoriaTieneContenido() {
        for (k in ItemKind.entries) {
            assertTrue("categoria vacia: $k", ItemCatalog.ofKind(k).isNotEmpty())
        }
        assertTrue(ItemCatalog.ofKind(ItemKind.CONSUMIBLE).size >= 20)
        assertTrue(ItemCatalog.ofKind(ItemKind.MEJORA).size >= 12)
        assertTrue(ItemCatalog.ofKind(ItemKind.RELIQUIA).size >= 10)
    }

    @Test
    fun losConsumiblesHacenAlgo() {
        for (i in ItemCatalog.ofKind(ItemKind.CONSUMIBLE)) {
            val e = i.effect
            val hace = e.duration > 0f || e.magnitude > 0f || e.charges > 0
            assertTrue("consumible sin efecto: ${i.id}", hace)
        }
    }

    @Test
    fun lasMejorasTienenNivelesYMagnitud() {
        for (i in ItemCatalog.ofKind(ItemKind.MEJORA)) {
            assertTrue("mejora sin niveles: ${i.id}", i.maxLevel >= 2)
            assertTrue("mejora sin magnitud: ${i.id}", i.effect.magnitude > 0f)
        }
    }

    @Test
    fun losCosmeticosNoDanVentaja() {
        for (i in ItemCatalog.ofKind(ItemKind.COSMETICO)) {
            assertTrue(
                "cosmetico con efecto de juego: ${i.id}",
                i.effect.type == EffectType.COS_GUANTES || i.effect.type == EffectType.COS_TINTE_LUZ
            )
        }
    }

    @Test
    fun losObjetosDeArranqueExistenYSonGratis() {
        for (id in ItemCatalog.defaultsOwned) {
            val i = ItemCatalog.get(id)
            assertNotNull("objeto inicial inexistente: $id", i)
            assertEquals(0, i!!.basePrice)
        }
    }

    @Test
    fun elDesbloqueoEsAlcanzable() {
        for (i in ItemCatalog.all) {
            assertTrue("desbloqueo invalido en ${i.id}", i.unlockLevel >= 1)
            assertTrue("desbloqueo inalcanzable en ${i.id}", i.unlockLevel <= 40)
        }
    }

    @Test
    fun soloSeUsanLasDosMonedasDelJuego() {
        for (i in ItemCatalog.all) {
            assertTrue(i.currency == Currency.ECOS || i.currency == Currency.VETAGRIS)
        }
        assertTrue(ItemCatalog.all.any { it.currency == Currency.VETAGRIS })
    }

    /** Todo EffectType de juego declarado tiene que estar usado por algun objeto. */
    @Test
    fun noHayEfectosDeclaradosSinUsar() {
        val usados = ItemCatalog.all.map { it.effect.type }.toSet()
        val sinUsar = EffectType.entries.filter { it !in usados }
        assertTrue("efectos declarados pero sin objeto: $sinUsar", sinUsar.isEmpty())
    }
}
