package com.mggx.laberinto

import com.mggx.laberinto.game.ActiveEffects
import com.mggx.laberinto.game.EffectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los buffs temporales de la partida.
 *
 * La regla de fondo: un mismo tipo de efecto esta activo UNA sola vez. Volver
 * a usarlo refresca, no apila — si apilara, con cinco pociones de velocidad
 * cruzarias la cueva atravesando paredes.
 */
class EfectosTest {

    @Test
    fun elEfectoSeApagaCuandoSeLeAcabaElTiempo() {
        val e = ActiveEffects()
        e.apply(EffectType.VELOCIDAD, 1.5f, 2f, "pocion_zancada")
        assertTrue(e.isActive(EffectType.VELOCIDAD))
        e.update(1f)
        assertTrue("se apago antes de tiempo", e.isActive(EffectType.VELOCIDAD))
        e.update(1.1f)
        assertFalse("no se apago nunca", e.isActive(EffectType.VELOCIDAD))
        assertEquals("quedo colgado en la lista", 0, e.visible.size)
    }

    @Test
    fun usarDosVecesRefrescaPeroNoApila() {
        val e = ActiveEffects()
        e.apply(EffectType.VELOCIDAD, 1.5f, 10f, "a")
        e.apply(EffectType.VELOCIDAD, 1.5f, 10f, "a")
        assertEquals("se apilaron dos veces el mismo efecto", 1, e.visible.size)
        assertEquals("se sumaron las duraciones", 10f, e.remaining(EffectType.VELOCIDAD), 0.01f)
    }

    @Test
    fun sequedaConLaMagnitudMasAlta() {
        val e = ActiveEffects()
        e.apply(EffectType.VELOCIDAD, 2.0f, 10f, "fuerte")
        e.apply(EffectType.VELOCIDAD, 1.2f, 30f, "flojo_pero_largo")
        assertEquals(
            "perdio la magnitud buena al refrescar con una peor",
            2.0f, e.magnitude(EffectType.VELOCIDAD), 0.01f
        )
        assertEquals(
            "perdio el tiempo largo",
            30f, e.remaining(EffectType.VELOCIDAD), 0.01f
        )
    }

    @Test
    fun alRefrescarConAlgoMasLargoCambiaElObjetoQueSeMuestra() {
        // Bug real: `apply` refrescaba el tiempo pero NO el sourceId. Usabas
        // una pocion corta y despues una larga, y el HUD te seguia mostrando
        // el icono de la corta. El efecto era el de la larga, pero el cartel
        // mentia sobre cual era.
        val e = ActiveEffects()
        e.apply(EffectType.LUZ_RADIO, 1.4f, 20f, "antorcha_sebo")
        e.apply(EffectType.LUZ_RADIO, 1.4f, 90f, "antorcha_fosforo")
        assertEquals(
            "el HUD seguiria mostrando el objeto viejo",
            "antorcha_fosforo", e.visible.first().sourceId
        )
    }

    @Test
    fun laBarritaDelHudNuncaSePasaDelCien() {
        // El otro lado del mismo bug: `total` tampoco se actualizaba, asi que
        // la barra dividia el tiempo NUEVO (largo) por el total VIEJO (corto) y
        // se iba por arriba del 100%.
        val e = ActiveEffects()
        e.apply(EffectType.LUZ_RADIO, 1f, 10f, "corta")
        e.apply(EffectType.LUZ_RADIO, 1f, 120f, "larga")
        val a = e.visible.first()
        assertTrue(
            "la barra se pasa del 100% (${a.remaining} de ${a.total})",
            a.remaining <= a.total + 0.001f
        )
        // Y sigue teniendo sentido a lo largo de toda la duracion.
        repeat(100) {
            e.update(1f)
            for (x in e.visible) {
                assertTrue("la barra quedo fuera de rango", x.remaining / x.total in 0f..1.0001f)
            }
        }
    }

    @Test
    fun refrescarConAlgoMasCortoNoAcortaLoQueYaTenias() {
        // Si usaras una antorcha larga y despues una corta, seria una estafa
        // que la corta te recortara el tiempo que ya tenias pago.
        val e = ActiveEffects()
        e.apply(EffectType.LUZ_RADIO, 1f, 90f, "larga")
        e.apply(EffectType.LUZ_RADIO, 1f, 10f, "corta")
        assertEquals(90f, e.remaining(EffectType.LUZ_RADIO), 0.01f)
        assertEquals("le cambio el objeto por uno peor", "larga", e.visible.first().sourceId)
    }

    @Test
    fun losMultiplicadoresValenUnoCuandoNoHayEfecto() {
        // Es lo que hace que el resto del codigo pueda multiplicar sin
        // preguntar. Si devolviera cero, no tener el buff te dejaria quieto.
        val e = ActiveEffects()
        for (t in EffectType.entries) {
            assertEquals("${t.name} no vale 1 apagado", 1f, e.multiplier(t), 0f)
        }
    }

    @Test
    fun sePuedenTenerVariosEfectosDistintosALaVez() {
        val e = ActiveEffects()
        e.apply(EffectType.VELOCIDAD, 1.4f, 20f, "a")
        e.apply(EffectType.LUZ_RADIO, 1.6f, 20f, "b")
        e.apply(EffectType.RESISTENCIA, 1.3f, 20f, "c")
        assertEquals(3, e.visible.size)
        e.clear(EffectType.LUZ_RADIO)
        assertEquals(2, e.visible.size)
        e.clearAll()
        assertEquals(0, e.visible.size)
    }

    @Test
    fun unaDuracionCeroNoPrendeNada() {
        val e = ActiveEffects()
        e.apply(EffectType.VELOCIDAD, 2f, 0f, "x")
        e.apply(EffectType.VELOCIDAD, 2f, -5f, "x")
        assertFalse(e.isActive(EffectType.VELOCIDAD))
        assertEquals(0, e.visible.size)
    }

    @Test
    fun unCuadroLarguisimoApagaTodoDeUnaYNoDejaNegativos() {
        val e = ActiveEffects()
        e.apply(EffectType.VELOCIDAD, 2f, 5f, "a")
        e.apply(EffectType.LUZ_RADIO, 2f, 5f, "b")
        e.update(999f)
        assertEquals(0, e.visible.size)
        assertEquals(0f, e.remaining(EffectType.VELOCIDAD), 0f)
    }
}
