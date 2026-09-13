package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El aguante: correr lo gasta, caminar lo recupera.
 *
 * Lo que se prueba aca no es la formula sino el ESCALON. Antes la condicion
 * para correr era `stamina > 1f` a secas, y en el fondo de la barra eso se
 * convertia en un interruptor a sesenta hertz: un cuadro corrias (gastando 22
 * por segundo) y al siguiente no (recuperando 16), asi que volvias a cruzar el
 * umbral y otra vez. Dos cosas salian mal:
 *
 *  - se veia y se escuchaba el tironeo, porque la velocidad y el cabeceo
 *    cambiaban cuadro por medio;
 *  - y la barra no llegaba nunca al cero, porque el equilibrio se para justo
 *    arriba del umbral. Con casi la mitad de los cuadros corriendo, se cruzaba
 *    la cueva entera a paso de trote sin aguante y gratis.
 */
class AguanteTest {

    private val DT = 1f / 60f

    private fun sesion() =
        GameSession(SaveData.fromStore(SaveData.memoryStore()), 1, 999L)

    /** Corre de frente [segundos] y devuelve, cuadro a cuadro, si corrio. */
    private fun correrDeFrente(s: GameSession, segundos: Float): List<Boolean> {
        val input = GameSession.Input(moveY = 1f, running = true)
        val out = ArrayList<Boolean>()
        repeat((segundos / DT).toInt()) {
            s.update(DT, input)
            out.add(s.corriendo)
        }
        return out
    }

    @Test
    fun correrSinPararVaciaLaBarraHastaElCero() {
        val s = sesion()
        val input = GameSession.Input(moveY = 1f, running = true)
        var minimo = 1f
        repeat((30f / DT).toInt()) {
            s.update(DT, input)
            if (s.staminaFraction() < minimo) minimo = s.staminaFraction()
        }
        assertTrue(
            "la barra nunca bajo del ${(minimo * 100).toInt()}%: se queda clavada en el umbral",
            minimo < 0.01f
        )
    }

    @Test
    fun alQuedarseSinAguanteSeDejaDeCorrerDeVerdad() {
        // Lo que importa es el momento justo despues de vaciarse: ahi no se
        // corre ni un cuadro mas, por mas que sigas apretando. Antes se seguia
        // corriendo uno de cada dos.
        val s = sesion()
        val input = GameSession.Input(moveY = 1f, running = true)
        var cuadros = 0
        var arranco = false
        while (cuadros < 60 * 60) {
            s.update(DT, input); cuadros++
            if (s.corriendo) arranco = true else if (arranco) break
        }
        assertTrue("nunca arranco a correr", arranco)
        assertTrue("nunca dejo de correr en un minuto entero de sprint", cuadros < 60 * 60)
        assertTrue(
            "dejo de correr con la barra en el ${(s.staminaFraction() * 100).toInt()}%: no se vacio",
            s.staminaFraction() < 0.01f
        )

        val despues = correrDeFrente(s, 1.5f)
        assertTrue(
            "corrio ${despues.count { it }} cuadros de ${despues.size} con la barra en cero",
            despues.none { it }
        )
    }

    @Test
    fun elSprintNoSeEnciendeYApagaCuadroPorMedio() {
        // El sintoma que se ve y se escucha. Que haya un vaiven es esperable
        // (se corre un tramo y se recupera el aire); lo que no puede haber son
        // tramos de dos o tres cuadros.
        val s = sesion()
        val cuadros = correrDeFrente(s, 40f)
        var largo = 1
        var masCorto = Int.MAX_VALUE
        for (i in 1 until cuadros.size) {
            if (cuadros[i] == cuadros[i - 1]) largo++
            else { masCorto = minOf(masCorto, largo); largo = 1 }
        }
        assertTrue(
            "hubo un tramo de solo $masCorto cuadros: eso es tironeo, no ritmo",
            masCorto >= 30
        )
    }

    @Test
    fun despuesDeDescansarSePuedeVolverACorrer() {
        // Que el escalon no se convierta en un castigo eterno: soltando el
        // boton un rato, el aguante vuelve y se corre igual que al principio.
        val s = sesion()
        correrDeFrente(s, 30f)
        val quieto = GameSession.Input()
        repeat((10f / DT).toInt()) { s.update(DT, quieto) }
        assertTrue("no recupero aguante descansando", s.staminaFraction() > 0.9f)
        val despues = correrDeFrente(s, 2f)
        assertTrue("no pudo volver a correr", despues.count { it } > 100)
    }

    @Test
    fun quietoNoSeGastaAguanteAunqueTengasApretadoCorrer() {
        val s = sesion()
        val input = GameSession.Input(running = true)
        repeat((5f / DT).toInt()) { s.update(DT, input) }
        assertTrue("gasto aguante estando parado", s.staminaFraction() > 0.99f)
        assertFalse("dice que corre estando parado", s.corriendo)
    }
}
