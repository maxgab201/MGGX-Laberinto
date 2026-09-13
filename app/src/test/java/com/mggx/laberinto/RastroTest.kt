package com.mggx.laberinto

import com.mggx.laberinto.game.Rastro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El rastro de pisadas del piso, y sobre todo el Hilo de Ariadna.
 *
 * El objeto promete dos cosas en la tienda: "deja un rastro brillante en el
 * piso por donde ya pasaste" y "dura 90 segundos". Las dos se rompieron, cada
 * una por su lado, y ninguna se notaba jugando poquito:
 *
 *  - el rastro dejaba de anotar al llegar al tope de memoria, asi que en un
 *    nivel grande el hilo se cortaba a mitad de camino;
 *  - y los puntos del hilo sobrevivian un cuarto de hora despues de que el
 *    efecto se apagara.
 */
class RastroTest {

    private val VIDA_NORMAL = 12f
    private val DT = 1f / 60f

    /** Camina en linea recta dejando rastro, y devuelve la distancia final. */
    private fun caminar(r: Rastro, metros: Float, hilo: Boolean, paso: Float = 0.05f): Float {
        var x = 0f
        while (x < metros) {
            x += paso
            r.update(DT, x, 0f, hilo, VIDA_NORMAL)
        }
        return x
    }

    @Test
    fun conElHiloPuestoElRastroSigueAnotandoDespuesDelTope() {
        // Este es el bug gordo. El tope son 900 puntos = 675 metros. Caminamos
        // 900 y miramos si el ultimo punto quedo donde estamos parados o si el
        // rastro se congelo en el kilometro 675.
        val r = Rastro()
        val x = caminar(r, 900f, hilo = true)
        val ultimo = r.puntos.last()
        assertTrue(
            "el rastro dejo de anotar: el ultimo punto quedo en ${ultimo.x} m y el jugador esta en $x m",
            ultimo.x > x - 2f
        )
    }

    @Test
    fun elRastroNuncaSePasaDelTopeDeMemoria() {
        // El otro lado: que "no dejar de anotar" no se convierta en una lista
        // que crece para siempre. Son objetos vivos que se recorren cuadro a
        // cuadro para dibujar.
        val r = Rastro(maxPuntos = 50)
        caminar(r, 400f, hilo = true)
        assertEquals("se paso del tope", 50, r.puntos.size)
    }

    @Test
    fun alLlenarseTiraElMasViejoYNoElMasNuevo() {
        val r = Rastro(maxPuntos = 10)
        caminar(r, 60f, hilo = true)
        val xs = r.puntos.map { it.x }
        assertEquals("quedaron en desorden", xs.sorted(), xs)
        assertTrue("el primero tendria que ser un punto reciente, no el del arranque", xs.first() > 40f)
    }

    @Test
    fun mientrasElHiloEstaPuestoNoSeApagaNingunPunto() {
        val r = Rastro()
        caminar(r, 20f, hilo = true)
        val cuantos = r.puntos.size
        assertTrue("no anoto nada", cuantos > 10)
        // Parado en el lugar, tres minutos: el hilo dura 90 s, asi que si algo
        // se apagara aca, se apagaria en plena partida.
        repeat(60 * 180) { r.update(DT, 20f, 0f, true, VIDA_NORMAL) }
        assertEquals("se apago el hilo antes de que lo apaguen", cuantos, r.puntos.size)
    }

    @Test
    fun cuandoSeAcabaElHiloElRastroSeApagaEnLoQueDuraUnRastroNormal() {
        // Bug real: los puntos del hilo nacian con 9999 segundos de vida y
        // nadie se los recortaba al terminar el efecto. Resultado: el hilo
        // seguia dibujado un cuarto de hora despues de que el HUD dijera que se
        // habia terminado.
        val r = Rastro()
        caminar(r, 20f, hilo = true)
        assertTrue(r.puntos.isNotEmpty())

        // Primer cuadro sin hilo: ningun punto puede seguir con la vida larga.
        r.update(DT, 20f, 0f, false, VIDA_NORMAL)
        val masVivo = r.puntos.maxOf { it.life }
        assertTrue(
            "quedo un punto con $masVivo segundos de vida: son ${(masVivo / 60f).toInt()} minutos de hilo de regalo",
            masVivo <= VIDA_NORMAL + 0.001f
        )

        // Y esperando lo que dura un rastro comun, el camino recorrido se
        // termina de borrar. (Siempre queda un punto abajo de los pies: estas
        // parado ahi, esa pisada es de recien. Lo que no puede quedar es el
        // camino de atras.)
        repeat(((VIDA_NORMAL + 1f) / DT).toInt()) { r.update(DT, 20f, 0f, false, VIDA_NORMAL) }
        val delCamino = r.puntos.filter { it.x < 19f }
        assertEquals(
            "quedaron ${delCamino.size} puntos del hilo dibujados por el camino viejo",
            0, delCamino.size
        )
    }

    @Test
    fun sinHiloElRastroSeApagaSolo() {
        val r = Rastro()
        caminar(r, 20f, hilo = false)
        assertTrue("no anoto nada", r.puntos.isNotEmpty())
        repeat(((VIDA_NORMAL + 1f) / DT).toInt()) { r.update(DT, 20f, 0f, false, VIDA_NORMAL) }
        assertEquals("quedo dibujado el camino de atras", 0, r.puntos.count { it.x < 19f })
    }

    @Test
    fun quedarseQuietoNoLlenaElRastroDePuntosEncimados() {
        val r = Rastro()
        repeat(60 * 60) { r.update(DT, 5f, 5f, true, VIDA_NORMAL) }
        assertEquals("dejo un monton de puntos en el mismo lugar", 1, r.puntos.size)
    }

    @Test
    fun losPuntosGuardanLaSeparacionPedida() {
        // Si fueran mas juntos se verian como una mancha y costarian el doble
        // de dibujar; si fueran mas lejos, el hilo quedaria punteado.
        val r = Rastro()
        caminar(r, 60f, hilo = false, paso = 0.02f)
        val xs = r.puntos.map { it.x }
        for (i in 1 until xs.size) {
            val d = xs[i] - xs[i - 1]
            assertTrue("dos puntos a $d m, muy encimados", d > 0.7f)
            assertTrue("dos puntos a $d m, el rastro queda punteado", d < 1.2f)
        }
    }

    @Test
    fun prenderYApagarElHiloVariasVecesNoDejaPuntosEternos() {
        // Tres pociones seguidas. Lo que importa es que despues de la ultima el
        // rastro se termine de apagar igual que siempre.
        val r = Rastro()
        var x = 0f
        repeat(3) {
            repeat(300) { x += 0.05f; r.update(DT, x, 0f, true, VIDA_NORMAL) }
            repeat(60) { x += 0.05f; r.update(DT, x, 0f, false, VIDA_NORMAL) }
        }
        repeat(((VIDA_NORMAL + 1f) / DT).toInt()) { r.update(DT, x, 0f, false, VIDA_NORMAL) }
        assertEquals(
            "sobrevivio algun punto de los hilos viejos",
            0, r.puntos.count { it.x < x - 1f }
        )
        assertTrue("quedo un punto con la vida larga del hilo", r.puntos.all { it.life <= VIDA_NORMAL + 0.001f })
    }

    @Test
    fun limpiarDejaElRastroComoNuevo() {
        val r = Rastro()
        caminar(r, 20f, hilo = true)
        r.limpiar()
        assertEquals(0, r.puntos.size)
        // Y el estado interno del hilo tambien: si hubiera quedado "el hilo
        // estaba puesto", el primer cuadro sin hilo recortaria de gusto.
        r.update(DT, 0f, 0f, false, VIDA_NORMAL)
        assertEquals(1, r.puntos.size)
        assertEquals(VIDA_NORMAL - DT, r.puntos[0].life, 0.001f)
    }
}
