package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.game.Postura
import com.mggx.laberinto.maze.Maze
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Agacharse, saltar y subir escaleras.
 *
 * Todo esto se prueba en JVM sin OpenGL: la sesion es solo estado y numeros.
 */
class MovimientoTest {

    private fun perfil(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    private fun sesion(level: Int = 1, seed: Long = 4242L) =
        GameSession(perfil(), level, seed)

    /** Deja al jugador parado en el centro exacto de una casilla. */
    private fun ponerEn(s: GameSession, gx: Int, gy: Int) {
        s.posX = (gx + 0.5f) * GameSession.CELL
        s.posZ = (gy + 0.5f) * GameSession.CELL
        s.posY = s.maze.floorY(gx, gy)
    }

    /** Un vecino abierto de la casilla de arranque, para armar el escenario. */
    private fun vecinoAbierto(m: Maze, gx: Int, gy: Int): Pair<Int, Int>? {
        val dx = intArrayOf(1, -1, 0, 0)
        val dy = intArrayOf(0, 0, 1, -1)
        for (d in 0 until 4) {
            val nx = gx + dx[d]; val ny = gy + dy[d]
            if (m.inBounds(nx, ny) && !m.isSolid(nx, ny)) return nx to ny
        }
        return null
    }

    private fun quieto() = GameSession.Input()

    // --------------------------------------------------------------- postura

    @Test
    fun elTechoBajoObligaAAgacharseSolo() {
        val s = sesion()
        val gx = s.maze.startGx; val gy = s.maze.startGy
        ponerEn(s, gx, gy)
        s.maze.ceilClearance[s.maze.index(gx, gy)] = 1.30f
        s.update(1f / 60f, quieto())
        assertEquals(
            "con 1,30 m de alto libre el jugador tiene que ir agachado",
            Postura.AGACHADO, s.postura
        )

        s.maze.ceilClearance[s.maze.index(gx, gy)] = 0.85f
        s.update(1f / 60f, quieto())
        assertEquals(
            "con 0,85 m solo se pasa arrastrandose",
            Postura.ARRASTRANDOSE, s.postura
        )
    }

    @Test
    fun agacharseNoGastaAguante() {
        val s = sesion()
        s.stamina = s.stats.maxStamina * 0.5f
        val antes = s.stamina
        val input = GameSession.Input(moveY = 1f, agacharse = 2, running = true)
        repeat(120) { s.update(1f / 60f, input) }
        assertTrue(
            "arrastrandose el aguante tiene que regenerar, no bajar (antes=$antes ahora=${s.stamina})",
            s.stamina > antes
        )
    }

    @Test
    fun correrSiGastaAguante() {
        val s = sesion()
        val antes = s.stamina
        val input = GameSession.Input(moveY = 1f, running = true)
        repeat(60) { s.update(1f / 60f, input) }
        assertTrue("corriendo de pie el aguante tiene que bajar", s.stamina < antes)
    }

    @Test
    fun laCamaraBajaAlAgacharse() {
        val s = sesion()
        ponerEn(s, s.maze.startGx, s.maze.startGy)
        repeat(30) { s.update(1f / 60f, quieto()) }
        val dePie = s.alturaCamara()
        val input = GameSession.Input(agacharse = 1)
        repeat(90) { s.update(1f / 60f, input) }
        val agachado = s.alturaCamara()
        assertTrue(
            "agachado el ojo tiene que quedar mas abajo ($dePie -> $agachado)",
            agachado < dePie - 0.4f
        )
    }

    // -------------------------------------------------------------- gateras

    @Test
    fun entrarEnLaGateraAgachaSolo() {
        // La cueva nunca te frena en seco: al meterte en un tramo bajo el
        // personaje se agacha solo, y por eso el nivel sigue siendo pasable.
        val s = sesion()
        val gx = s.maze.startGx; val gy = s.maze.startGy
        ponerEn(s, gx, gy)
        val (vx, vy) = vecinoAbierto(s.maze, gx, gy)!!
        s.maze.ceilClearance[s.maze.index(vx, vy)] = 1.25f
        val destinoX = (vx + 0.5f) * GameSession.CELL
        val destinoZ = (vy + 0.5f) * GameSession.CELL

        s.update(1f / 60f, quieto())
        assertEquals(Postura.DE_PIE, s.postura)
        assertFalse("la gatera nunca puede quedar cerrada", s.bloqueado(destinoX, destinoZ))

        // Se lo pone dentro de la gatera: tiene que agacharse solo.
        ponerEn(s, vx, vy)
        s.update(1f / 60f, quieto())
        assertEquals("dentro de la gatera va agachado", Postura.AGACHADO, s.postura)
    }

    @Test
    fun elBotonDeAgacharseSigueMandandoHaciaAbajo() {
        // El automatismo agacha cuando hace falta; el boton permite agacharse
        // MAS de lo necesario, nunca menos.
        val s = sesion()
        val gx = s.maze.startGx; val gy = s.maze.startGy
        ponerEn(s, gx, gy)
        s.maze.ceilClearance[s.maze.index(gx, gy)] = 1.30f
        repeat(5) { s.update(1f / 60f, GameSession.Input(agacharse = 2)) }
        assertEquals(Postura.ARRASTRANDOSE, s.postura)
        repeat(5) { s.update(1f / 60f, GameSession.Input(agacharse = 0)) }
        assertEquals("el techo bajo manda por encima del boton", Postura.AGACHADO, s.postura)
    }

    // ------------------------------------------------------------ escaleras

    @Test
    fun elEscalonGrandeSoloSeSubeConEscalera() {
        val s = sesion()
        val gx = s.maze.startGx; val gy = s.maze.startGy
        ponerEn(s, gx, gy)
        val (vx, vy) = vecinoAbierto(s.maze, gx, gy)!!
        val j = s.maze.index(vx, vy)
        // Cuatro escalones arriba: no se sube caminando.
        s.maze.floorLevel[j] = s.maze.floorLevel[s.maze.index(gx, gy)] + 4
        s.maze.ladder[j] = false
        val destinoX = (vx + 0.5f) * GameSession.CELL
        val destinoZ = (vy + 0.5f) * GameSession.CELL

        s.update(1f / 60f, quieto())
        assertTrue("sin escalera un desnivel grande frena", s.bloqueado(destinoX, destinoZ))

        s.maze.ladder[j] = true
        assertFalse("con escalera se puede subir", s.bloqueado(destinoX, destinoZ))
    }

    @Test
    fun elEscalonChicoSeSubeCaminando() {
        val s = sesion()
        val gx = s.maze.startGx; val gy = s.maze.startGy
        ponerEn(s, gx, gy)
        val (vx, vy) = vecinoAbierto(s.maze, gx, gy)!!
        s.maze.floorLevel[s.maze.index(vx, vy)] = s.maze.floorLevel[s.maze.index(gx, gy)] + 1
        val destinoX = (vx + 0.5f) * GameSession.CELL
        val destinoZ = (vy + 0.5f) * GameSession.CELL
        s.update(1f / 60f, quieto())
        assertFalse("un escalon de 42 cm se sube caminando", s.bloqueado(destinoX, destinoZ))
    }

    @Test
    fun subirLaEscaleraLevantaAlJugador() {
        val s = sesion()
        val gx = s.maze.startGx; val gy = s.maze.startGy
        val (vx, vy) = vecinoAbierto(s.maze, gx, gy)!!
        val i = s.maze.index(gx, gy)
        val j = s.maze.index(vx, vy)
        s.maze.floorLevel[j] = s.maze.floorLevel[i] + 4
        s.maze.ladder[i] = true
        s.maze.ladder[j] = true
        // Se lo planta directamente en la casilla de arriba, a la altura de abajo.
        ponerEn(s, vx, vy)
        s.posY = s.maze.floorY(gx, gy)
        val altoDestino = s.maze.floorY(vx, vy)

        repeat(180) { s.update(1f / 60f, quieto()) }
        assertEquals(
            "la escalera tiene que terminar de subirlo",
            altoDestino, s.posY, 0.02f
        )
    }

    // --------------------------------------------------------------- salto

    @Test
    fun saltarSubeYVuelveAlPiso() {
        val s = sesion()
        val gx = s.maze.startGx; val gy = s.maze.startGy
        ponerEn(s, gx, gy)
        val piso = s.posY
        s.update(1f / 60f, GameSession.Input(saltar = true))
        assertTrue("apenas salta ya tiene que estar en el aire", !s.enSuelo)

        var maximo = s.posY
        repeat(120) {
            s.update(1f / 60f, quieto())
            if (s.posY > maximo) maximo = s.posY
        }
        assertTrue("el salto tiene que levantar al menos medio metro", maximo - piso > 0.45f)
        assertTrue("el salto no puede levantar mas de un metro", maximo - piso < 1.0f)
        assertTrue("y despues tiene que volver al piso", s.enSuelo)
        assertEquals(piso, s.posY, 0.001f)
    }

    @Test
    fun noSePuedeSaltarAgachado() {
        val s = sesion()
        ponerEn(s, s.maze.startGx, s.maze.startGy)
        val piso = s.posY
        repeat(30) { s.update(1f / 60f, GameSession.Input(agacharse = 1)) }
        repeat(30) { s.update(1f / 60f, GameSession.Input(agacharse = 1, saltar = true)) }
        assertEquals("agachado no se salta", piso, s.posY, 0.001f)
        assertTrue(s.enSuelo)
    }

    @Test
    fun elSaltoNoAlcanzaParaSaltearUnPozo() {
        // Un pozo de 3 escalones (1,26 m) es mas de lo que sube el salto: por eso
        // siempre hay escalera y el salto no rompe el diseño del nivel.
        val s = sesion()
        ponerEn(s, s.maze.startGx, s.maze.startGy)
        val piso = s.posY
        s.update(1f / 60f, GameSession.Input(saltar = true))
        var maximo = s.posY
        repeat(120) {
            s.update(1f / 60f, quieto())
            if (s.posY > maximo) maximo = s.posY
        }
        assertTrue(
            "el salto no puede llegar a los 3 escalones de un pozo",
            maximo - piso < 3 * Maze.ESCALON
        )
    }

    // ------------------------------------------------------------- caidas

    @Test
    fun caerDeUnPozoNormalNoHaceDano() {
        val s = sesion()
        val gx = s.maze.startGx; val gy = s.maze.startGy
        ponerEn(s, gx, gy)
        val vida = s.health
        // El pozo mas hondo que arma el generador es de 5 escalones.
        s.posY = s.maze.floorY(gx, gy) + 5 * Maze.ESCALON
        repeat(120) { s.update(1f / 60f, quieto()) }
        assertTrue("tiene que haber aterrizado", s.enSuelo)
        assertEquals("un pozo del generador no puede lastimar", vida, s.health, 0.001f)
    }

    @Test
    fun unaCaidaEnormeSiLastima() {
        val s = sesion()
        val gx = s.maze.startGx; val gy = s.maze.startGy
        ponerEn(s, gx, gy)
        val vida = s.health
        s.posY = s.maze.floorY(gx, gy) + 14f
        repeat(240) { s.update(1f / 60f, quieto()) }
        assertTrue("caer 14 m tiene que doler", s.health < vida)
    }

    // --------------------------------------------------- el relieve se juega

    @Test
    fun elJugadorArrancaApoyadoEnElPisoDeSuCasilla() {
        for (level in intArrayOf(1, 5, 12, 30, 60)) {
            val s = GameSession(perfil(), level, level * 991L)
            val esperado = s.maze.floorY(s.maze.startGx, s.maze.startGy)
            assertTrue(
                "en el nivel $level el jugador no arranca apoyado (${s.posY} vs $esperado)",
                abs(s.posY - esperado) < 0.001f
            )
            assertTrue("la camara tiene que estar sobre el piso", s.alturaCamara() > s.posY)
        }
    }
}
