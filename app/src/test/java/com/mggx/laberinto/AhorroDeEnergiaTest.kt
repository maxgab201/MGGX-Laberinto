package com.mggx.laberinto

import com.mggx.laberinto.core.AhorroDeEnergia
import com.mggx.laberinto.core.AhorroDeEnergia.Donde
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las decisiones de cuanto trabajo hace falta hacer segun donde este el
 * jugador. El juego corre en un telefono: dibujar 60 veces por segundo una
 * imagen que no cambia, o forzar la pantalla prendida mientras alguien mira la
 * tienda, es bateria tirada.
 */
class AhorroDeEnergiaTest {

    @Test
    fun jugandoSeRespetaLoQueElijioElJugador() {
        for (fps in intArrayOf(30, 45, 60, 90)) {
            assertEquals(fps, AhorroDeEnergia.fpsObjetivo(Donde.JUGANDO, fps))
        }
    }

    @Test
    fun enPausaSeDibujaMuchoMenos() {
        // La escena de atras esta congelada: no hay nada que actualizar.
        assertTrue(
            "en pausa tendria que dibujar mucho menos que jugando",
            AhorroDeEnergia.fpsObjetivo(Donde.PAUSA, 60) <
                AhorroDeEnergia.fpsObjetivo(Donde.JUGANDO, 60)
        )
        assertEquals(AhorroDeEnergia.FPS_PAUSA, AhorroDeEnergia.fpsObjetivo(Donde.PAUSA, 60))
    }

    @Test
    fun laVitrinaVaAMediaMaquina() {
        // Es un paseo lento de camara: a 30 se ve igual que a 60.
        assertEquals(AhorroDeEnergia.FPS_VITRINA, AhorroDeEnergia.fpsObjetivo(Donde.VITRINA, 60))
        assertEquals(AhorroDeEnergia.FPS_VITRINA, AhorroDeEnergia.fpsObjetivo(Donde.VITRINA, 90))
    }

    @Test
    fun nuncaSeSubePorArribaDeLoQueElijioElJugador() {
        // Si alguien puso 30 porque el telefono se le calienta, ni la vitrina
        // ni nada puede meterle mas.
        for (donde in Donde.entries) {
            assertTrue(
                "$donde se paso del techo elegido",
                AhorroDeEnergia.fpsObjetivo(donde, 30) <= 30
            )
        }
    }

    @Test
    fun enLosMenusNoSeDibujaNada() {
        // Ahi el hilo de GL esta parado: no hay 3D en pantalla.
        assertEquals(0, AhorroDeEnergia.fpsObjetivo(Donde.MENU, 60))
    }

    @Test
    fun laPantallaSeQuedaPrendidaJugandoYEnPausaPeroNoEnLosMenus() {
        // Jugando podes estar un rato largo caminando sin tocar la pantalla, y
        // en pausa podes estar leyendo el mapa. En la tienda no: ahi el
        // telefono se apaga solo, como con cualquier otra app. Antes la
        // bandera se prendia al arrancar y no se apagaba NUNCA.
        assertTrue(AhorroDeEnergia.mantenerPantallaPrendida(Donde.JUGANDO))
        assertTrue(AhorroDeEnergia.mantenerPantallaPrendida(Donde.PAUSA))
        assertFalse(
            "la pantalla no se puede quedar prendida sola en los menus",
            AhorroDeEnergia.mantenerPantallaPrendida(Donde.MENU)
        )
        assertFalse(AhorroDeEnergia.mantenerPantallaPrendida(Donde.VITRINA))
    }

    @Test
    fun elHudSeRefrescaMasLentoEnPausa() {
        assertTrue(
            "el HUD tendria que refrescarse mas lento con la partida congelada",
            AhorroDeEnergia.intervaloHudMs(Donde.PAUSA) >
                AhorroDeEnergia.intervaloHudMs(Donde.JUGANDO)
        )
    }

    @Test
    fun elIntervaloDeCuadroEsCoherente() {
        assertEquals(1_000_000_000L / 60L, AhorroDeEnergia.intervaloCuadroNs(60))
        assertEquals(0L, AhorroDeEnergia.intervaloCuadroNs(0))
        // Menos cuadros por segundo = mas tiempo entre cuadros.
        assertTrue(
            AhorroDeEnergia.intervaloCuadroNs(30) > AhorroDeEnergia.intervaloCuadroNs(60)
        )
    }

    @Test
    fun unTechoRotoNoDejaElJuegoSinDibujar() {
        // Un save viejo o corrupto con fps 0 o negativo no puede terminar en
        // una pantalla congelada: se cae al valor de siempre.
        assertTrue(AhorroDeEnergia.fpsObjetivo(Donde.JUGANDO, 0) > 0)
        assertTrue(AhorroDeEnergia.fpsObjetivo(Donde.JUGANDO, -5) > 0)
    }
}
