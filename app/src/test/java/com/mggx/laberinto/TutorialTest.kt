package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.game.Tutorial
import com.mggx.laberinto.maze.Maze
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El guion del nivel 1.
 *
 * La promesa es doble: que cada paso se apruebe HACIENDO la cosa (y no
 * mirando pasar el tiempo), y que ningun paso pueda dejar al jugador trabado
 * para siempre si no encuentra el boton.
 */
class TutorialTest {

    private fun perfil(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    /** Un cuadro en el que el jugador no hace absolutamente nada. */
    private fun quieto(t: Tutorial, dt: Float = 1f / 60f): String? = t.observar(
        dt, giroGrados = 0f, metros = 0f, corriendo = false, agachado = false,
        salto = false, golpe = false, junto = false, linterna = false, gano = false
    )

    @Test
    fun elTutorialSoloExisteEnElNivelUno() {
        assertNotNull("el nivel 1 tiene que traer tutorial", GameSession(perfil(), 1, 11L).tutorial)
        for (level in intArrayOf(2, 3, 7, 20)) {
            assertNull(
                "el nivel $level no tendria que traer tutorial",
                GameSession(perfil(), level, level * 13L).tutorial
            )
        }
    }

    @Test
    fun arrancaPidiendoQueMiresAlrededor() {
        val t = Tutorial(conLinterna = false)
        assertEquals(Tutorial.Clave.MIRAR, t.pasoActual?.clave)
        assertEquals("no tendria que arrancar con progreso", 0f, t.progreso, 0.001f)
    }

    @Test
    fun mirandoSeAvanzaYQuietoNo() {
        val t = Tutorial(conLinterna = false)
        // Un rato quieto: el paso de mirar no se mueve ni un poco.
        repeat(60) { quieto(t) }
        assertEquals(Tutorial.Clave.MIRAR, t.pasoActual?.clave)
        assertEquals(0f, t.progreso, 0.001f)

        // Y girando si.
        var msg: String? = null
        repeat(60) {
            val m = t.observar(
                1f / 60f, giroGrados = 5f, metros = 0f, corriendo = false, agachado = false,
                salto = false, golpe = false, junto = false, linterna = false, gano = false
            )
            if (m != null) msg = m
        }
        assertNotNull("mirar 300 grados no completo el paso", msg)
        assertEquals(Tutorial.Clave.CAMINAR, t.pasoActual?.clave)
    }

    @Test
    fun cadaPasoSeApruebaConSuPropiaAccion() {
        // Que cada paso mire SU accion y no cualquiera es lo que hace que el
        // tutorial ensene algo: si "agachate" se aprobara caminando, el
        // jugador nunca se enteraria de que existe el boton.
        val t = Tutorial(conLinterna = true)
        val hechos = ArrayList<Tutorial.Clave>()
        var vueltas = 0
        while (t.pasoActual != null && vueltas < 100000) {
            vueltas++
            val clave = t.pasoActual!!.clave
            val m = t.observar(
                1f / 60f,
                giroGrados = if (clave == Tutorial.Clave.MIRAR) 6f else 0f,
                metros = if (clave == Tutorial.Clave.CAMINAR ||
                    clave == Tutorial.Clave.CORRER
                ) 0.05f else 0f,
                corriendo = clave == Tutorial.Clave.CORRER,
                agachado = clave == Tutorial.Clave.AGACHARSE,
                salto = clave == Tutorial.Clave.SALTAR,
                golpe = clave == Tutorial.Clave.GOLPEAR,
                junto = clave == Tutorial.Clave.JUNTAR,
                linterna = clave == Tutorial.Clave.LINTERNA,
                gano = clave == Tutorial.Clave.SALIR
            )
            if (m != null) hechos.add(clave)
        }
        assertTrue("el tutorial no se termino nunca", t.terminado)
        assertEquals(
            "no se completaron todos los pasos, o se completaron de mas",
            t.pasos.map { it.clave }, hechos
        )
        // Ninguno tuvo que agotar su limite: todos se aprobaron haciendolos.
        assertTrue("tardo demasiado: se aprobaron por tiempo, no por accion", vueltas < 6000)
    }

    @Test
    fun ningunPasoTeDejaTrabado() {
        // Un jugador que no encuentra el boton no puede quedar encerrado en el
        // paso 3 para siempre. Todos vencen solos, menos el ultimo, que es
        // "busca la salida" y no tiene nada despues.
        val t = Tutorial(conLinterna = true)
        repeat(60 * 60 * 10) { quieto(t) }   // diez minutos sin tocar nada
        assertFalse("el tutorial no puede darse por terminado solo", t.terminado)
        assertEquals(
            "quieto tendria que quedar en el ultimo paso, el de la salida",
            Tutorial.Clave.SALIR, t.pasoActual?.clave
        )
    }

    @Test
    fun elPasoDeLaLinternaSoloApareceSiTenesLinterna() {
        val con = Tutorial(conLinterna = true).pasos.map { it.clave }
        val sin = Tutorial(conLinterna = false).pasos.map { it.clave }
        assertTrue(Tutorial.Clave.LINTERNA in con)
        assertFalse("no se puede ensenar a prender algo que no tenes", Tutorial.Clave.LINTERNA in sin)
    }

    @Test
    fun cadaPasoTieneTextoDeVerdad() {
        for (p in Tutorial(conLinterna = true).pasos) {
            assertTrue("${p.clave} sin titulo", p.titulo.length > 3)
            assertTrue("${p.clave} sin ayuda", p.ayuda.length > 12)
            assertTrue("${p.clave} sin mensaje de logro", p.logro.length > 8)
            assertTrue("${p.clave} se aprueba sola", p.objetivo > 0f)
        }
    }

    @Test
    fun elNivelUnoTieneUnTramoBajoParaPracticar() {
        // El tutorial dice "agachate, hay tramos bajos donde es la unica forma
        // de pasar". Si el nivel 1 fuera todo de techo alto, esa frase seria
        // mentira. Y tiene que estar SOBRE el camino a la salida, para que
        // haya que usarlo y no se pueda esquivar.
        val alturaDePie = com.mggx.laberinto.game.Postura.DE_PIE.alturaCuerpo
        for (semilla in 0L until 12L) {
            val m = MazeGenerator.generate(1, 1000L + semilla).maze
            val bajas = m.solutionPath.count {
                Maze.altoLibreReal(m.ceilClearance[it]) < alturaDePie
            }
            assertTrue(
                "el nivel 1 (semilla $semilla) no tiene ningun tramo bajo en el camino",
                bajas > 0
            )
            // Pero uno solo: el nivel 1 no es una carrera de obstaculos.
            assertTrue(
                "el nivel 1 (semilla $semilla) tiene demasiados tramos bajos: $bajas",
                bajas <= 4
            )
        }
    }

    @Test
    fun elTramoBajoDelNivelUnoSePasaAgachado() {
        // Bajo, pero no tanto como para que haya que arrastrarse: el paso del
        // tutorial ensena a agacharse, no a arrastrarse.
        val agachado = com.mggx.laberinto.game.Postura.AGACHADO.alturaCuerpo
        for (semilla in 0L until 12L) {
            val m = MazeGenerator.generate(1, 1000L + semilla).maze
            for (i in m.solutionPath) {
                val hueco = Maze.altoLibreReal(m.ceilClearance[i])
                if (hueco >= com.mggx.laberinto.game.Postura.DE_PIE.alturaCuerpo) continue
                assertTrue(
                    "el tramo bajo del nivel 1 no le entra al cuerpo agachado ($hueco)",
                    hueco >= agachado
                )
            }
        }
    }
}
