package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.EffectType
import com.mggx.laberinto.game.GameSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * El cronometro de la partida.
 *
 * Importa mas de lo que parece: de ese numero salen el record personal, el
 * bonus de ecos por terminar rapido y, en una carrera, quien gano. Tiene que
 * medir lo mismo en un telefono que va a 60 cuadros que en uno que va a 90.
 */
class RelojTest {

    private fun sesion(level: Int = 1) =
        GameSession(SaveData.fromStore(SaveData.memoryStore()), level, 4242L)

    /** Deja pasar [segundos] de juego a [fps] cuadros por segundo, quieto. */
    private fun dejarPasar(s: GameSession, segundos: Float, fps: Int) {
        val dt = 1f / fps
        val input = GameSession.Input()
        repeat((segundos * fps).toInt()) { s.update(dt, input) }
    }

    @Test
    fun unMinutoDeJuegoEsUnMinutoDeReloj() {
        // Bug real: el reloj sumaba `(dt * 1000f).toLong()` por cuadro. A 60
        // cuadros eso es truncar 16,666 a 16, o sea correr al 96%: el minuto
        // marcaba 57,6 segundos.
        val s = sesion()
        dejarPasar(s, 60f, 60)
        val error = abs(s.elapsedMs - 60_000L)
        assertTrue(
            "el reloj marca ${s.elapsedMs} ms despues de un minuto real (se desvio $error ms)",
            error < 100L
        )
    }

    @Test
    fun elRelojMideIgualAsesentaQueANoventaCuadros() {
        // Este es el que duele en una carrera: dos telefonos con el mismo
        // recorrido tienen que dar el mismo tiempo aunque uno vaya mas fluido.
        val a = sesion(); dejarPasar(a, 30f, 60)
        val b = sesion(); dejarPasar(b, 30f, 90)
        val diferencia = abs(a.elapsedMs - b.elapsedMs)
        assertTrue(
            "a 60 cuadros marco ${a.elapsedMs} ms y a 90 marco ${b.elapsedMs} ms: $diferencia ms de diferencia",
            diferencia < 100L
        )
    }

    @Test
    fun elRelojNoSeAtrasaEnPartidasLargas() {
        // El error de redondeo se acumulaba: cuanto mas larga la partida, mas
        // se atrasaba. A diez minutos eran veinticuatro segundos de regalo.
        val s = sesion()
        dejarPasar(s, 600f, 60)
        val error = abs(s.elapsedMs - 600_000L)
        assertTrue("a los diez minutos marca ${s.elapsedMs} ms (se desvio $error ms)", error < 500L)
    }

    @Test
    fun congelarElRelojLoDetieneYDespuesSigueDondeEstaba() {
        val s = sesion()
        dejarPasar(s, 5f, 60)
        val antes = s.elapsedMs
        assertTrue("no arranco el reloj", antes > 4_000L)

        s.effects.apply(EffectType.CONGELAR_RELOJ, 1f, 10f, "rel_congelar")
        dejarPasar(s, 8f, 60)
        assertEquals("el reloj siguio corriendo congelado", antes, s.elapsedMs)

        // Y al descongelarse sigue contando desde donde estaba, no desde cero
        // ni recuperando el tiempo perdido.
        s.effects.clearAll()
        dejarPasar(s, 5f, 60)
        val error = abs((s.elapsedMs - antes) - 5_000L)
        assertTrue("despues de descongelar conto mal: ${s.elapsedMs - antes} ms (error $error)", error < 150L)
    }

    @Test
    fun enPausaElRelojNoCorre() {
        val s = sesion()
        dejarPasar(s, 3f, 60)
        val antes = s.elapsedMs
        s.pause()
        dejarPasar(s, 10f, 60)
        assertEquals("el reloj corrio en pausa", antes, s.elapsedMs)
    }
}
