package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.net.MatchState
import com.mggx.laberinto.net.NetProtocol
import com.mggx.laberinto.net.TransporteLocal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El andamiaje del multijugador. Todavia no hay red de verdad, pero todo lo
 * que va ARRIBA de la red ya tiene que funcionar: los mensajes, la sala y,
 * sobre todo, que dos telefonos armen exactamente la misma cueva.
 */
class MultijugadorTest {

    private fun perfil(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    // ----------------------------------------------------------- protocolo

    @Test
    fun todoMensajeVaYVuelveIgual() {
        val casos = listOf(
            NetProtocol.unirse("a1", "Maxi", "skin_veterano"),
            NetProtocol.arranque("a1", NetProtocol.Modo.COOPERATIVO, 17, -998877665544L),
            NetProtocol.pose("b2", 12.34f, -0.5f, 98.06f, 271.5f, 2),
            NetProtocol.tomar("b2", 4021),
            NetProtocol.romper("a1", 77),
            NetProtocol.trampa("b2", 12),
            NetProtocol.llegada("a1", 123456L),
            NetProtocol.caido("b2"),
            NetProtocol.revivir("a1", "b2"),
            NetProtocol.salir("b2"),
            NetProtocol.ping("a1")
        )
        for (m in casos) {
            val vuelta = NetProtocol.decodificar(m.codificar())
            assertNotNull("no se pudo leer ${m.tipo}", vuelta)
            assertEquals(m.tipo, vuelta!!.tipo)
            assertEquals(m.de, vuelta.de)
            assertEquals("los campos de ${m.tipo} no vuelven iguales", m.args, vuelta.args)
        }
    }

    @Test
    fun lasPosicionesNegativasYLosCentesimosNoSePierden() {
        val m = NetProtocol.decodificar(
            NetProtocol.pose("x", -0.5f, 1.05f, -12.07f, 359.99f, 1).codificar()
        )!!
        assertEquals(-0.5f, m.num(0), 0.011f)
        assertEquals(1.05f, m.num(1), 0.011f)
        assertEquals(-12.07f, m.num(2), 0.011f)
        assertEquals(359.99f, m.num(3), 0.011f)
        assertEquals(1, m.entero(4))
    }

    @Test
    fun unMensajeRotoNoTumbaNada() {
        assertNull(NetProtocol.decodificar(""))
        assertNull(NetProtocol.decodificar("cualquier cosa"))
        assertNull(NetProtocol.decodificar("1|NOEXISTE|a"))
        assertNull(NetProtocol.decodificar("99|POSE|a|1|2|3|0|0"))
        // Y uno valido pero con campos de menos tampoco explota.
        val m = NetProtocol.decodificar("1|POSE|a")
        assertNotNull(m)
        assertEquals(0f, m!!.num(0), 0.001f)
    }

    @Test
    fun elNombreNoPuedeRomperElFormato() {
        // Alguien escribe su nombre con la barra separadora adentro.
        val m = NetProtocol.unirse("a1", "Ma|xi|tramposo", "skin_minero")
        val vuelta = NetProtocol.decodificar(m.codificar())!!
        assertEquals(NetProtocol.Tipo.UNIRSE, vuelta.tipo)
        assertEquals("skin_minero", vuelta.arg(1))
        assertFalse("la barra separadora se colo en el nombre", vuelta.arg(0).contains('|'))
    }

    // --------------------------------------------------------------- sala

    @Test
    fun dosJugadoresSeVenYSeSincronizan() {
        val t1 = TransporteLocal()
        val t2 = t1.companero()
        val m1 = MatchState("uno")
        val m2 = MatchState("dos")
        m1.entrarYo("Maxi", "skin_veterano")
        m2.entrarYo("Colo", "skin_tecnico")

        t1.enviar(NetProtocol.unirse("uno", "Maxi", "skin_veterano").codificar())
        t2.enviar(NetProtocol.unirse("dos", "Colo", "skin_tecnico").codificar())
        t2.recibir().forEach { m2.aplicar(it) }
        t1.recibir().forEach { m1.aplicar(it) }

        assertEquals(2, m1.cantidad())
        assertEquals(2, m2.cantidad())
        assertEquals("Colo", m1.jugador("dos")?.nombre)
        assertEquals("skin_veterano", m2.jugador("uno")?.skin)

        // Uno se mueve y el otro lo ve donde corresponde.
        t1.enviar(NetProtocol.pose("uno", 10.5f, 1.26f, 4.25f, 90f, 1).codificar())
        t2.recibir().forEach { m2.aplicar(it) }
        val visto = m2.jugador("uno")!!
        assertEquals(10.5f, visto.x, 0.011f)
        assertEquals(1.26f, visto.y, 0.011f)
        assertEquals(4.25f, visto.z, 0.011f)
        assertEquals(1, visto.postura)
    }

    @Test
    fun elArranqueRepartidoDaLaMismaCuevaEnLosDosTelefonos() {
        // Esto es lo que hace posible todo: nivel + semilla = misma cueva.
        val t1 = TransporteLocal()
        val t2 = t1.companero()
        val m2 = MatchState("dos")
        val semilla = 0x5EED_1234_5678L
        t1.enviar(NetProtocol.arranque("uno", NetProtocol.Modo.CARRERA, 23, semilla).codificar())
        t2.recibir().forEach { m2.aplicar(it) }

        assertTrue(m2.arrancada)
        assertEquals(23, m2.nivel)
        assertEquals(semilla, m2.semilla)

        val anfitrion = GameSession(perfil(), 23, semilla)
        val invitado = GameSession(perfil(), m2.nivel, m2.semilla)

        assertTrue("los mapas no son iguales", anfitrion.maze.solid.contentEquals(invitado.maze.solid))
        assertTrue("el relieve no es igual", anfitrion.maze.floorLevel.contentEquals(invitado.maze.floorLevel))
        assertTrue("las escaleras no son iguales", anfitrion.maze.ladder.contentEquals(invitado.maze.ladder))
        assertEquals("la salida no cae en el mismo lugar", anfitrion.maze.exitGx, invitado.maze.exitGx)
        assertEquals(anfitrion.maze.exitGy, invitado.maze.exitGy)
        assertEquals("el ambiente no coincide", anfitrion.theme, invitado.theme)
        assertEquals(
            "las monedas no caen en el mismo lugar",
            anfitrion.pickups.map { it.gx to it.gy },
            invitado.pickups.map { it.gx to it.gy }
        )
        assertEquals(
            "los bichos no nacen en el mismo lugar",
            anfitrion.enemies.map { it.nidoGx to it.nidoGy },
            invitado.enemies.map { it.nidoGx to it.nidoGy }
        )
    }

    @Test
    fun agarrarDosVecesLaMismaMonedaNoRompeNada() {
        val m = MatchState("uno")
        m.aplicar(NetProtocol.tomar("dos", 412).codificar())
        m.aplicar(NetProtocol.tomar("dos", 412).codificar())
        m.aplicar(NetProtocol.tomar("tres", 413).codificar())
        assertEquals(2, m.objetosTomados().size)
        assertTrue(m.objetosTomados().contains(412))
    }

    @Test
    fun laCarreraLaGanaElDeMenorTiempo() {
        val m = MatchState("uno")
        m.aplicar(NetProtocol.llegada("dos", 90_000L).codificar())
        m.aplicar(NetProtocol.llegada("tres", 61_500L).codificar())
        m.aplicar(NetProtocol.llegada("cuatro", 120_000L).codificar())
        assertEquals("tres", m.ganador()?.id)
        // Y el que ya llego no puede mejorar su tiempo mandando otro mensaje.
        m.aplicar(NetProtocol.llegada("dos", 1L).codificar())
        assertEquals(90_000L, m.jugador("dos")?.tiempoFinal)
    }

    @Test
    fun enCooperativoSePierdeReciénCuandoCaenTodos() {
        val m = MatchState("uno")
        m.entrarYo("Maxi", "skin_minero")
        m.aplicar(NetProtocol.unirse("dos", "Colo", "skin_minero").codificar())
        // La pose es lo que dice que bajo a la cueva de verdad: el que se
        // quedo en la pantalla de sala no cuenta para el fin de la partida.
        m.aplicar(NetProtocol.pose("dos", 5f, 0f, 5f, 0f, 0).codificar())
        m.aplicar(NetProtocol.caido("dos").codificar())
        assertFalse("con uno solo caido no se pierde", m.equipoCaido())
        m.aplicar(NetProtocol.caido("uno").codificar())
        assertTrue(m.equipoCaido())
        // Y levantar a uno saca al equipo de la lona. Ojo con quien manda el
        // mensaje: el que vale es el del PROPIO caido diciendo "ya estoy de
        // pie", porque es el unico que sabe si pudo levantarse.
        m.aplicar(NetProtocol.revivir("dos", "dos").codificar())
        assertFalse(m.equipoCaido())
    }

    @Test
    fun elPedidoDeLevantarANoEsLoMismoQueHaberseLevantado() {
        // "levantate" lo manda el que esta al lado; "ya estoy de pie" lo manda
        // el caido. Solo el segundo cambia el estado: si el pedido contara,
        // los dos telefonos quedarian contando cosas distintas cuando el
        // caido todavia no puede levantarse.
        val m = MatchState("uno")
        m.entrarYo("Maxi", "skin_minero")
        m.aplicar(NetProtocol.unirse("dos", "Colo", "skin_minero").codificar())
        m.aplicar(NetProtocol.caido("dos").codificar())
        assertTrue(m.jugador("dos")!!.caido)

        // Un tercero pide que lo levanten: sigue en el piso.
        m.aplicar(NetProtocol.revivir("uno", "dos").codificar())
        assertTrue("un pedido lo dio por levantado", m.jugador("dos")!!.caido)

        // El propio caido avisa que ya esta: ahora si.
        m.aplicar(NetProtocol.revivir("dos", "dos").codificar())
        assertFalse(m.jugador("dos")!!.caido)
    }

    @Test
    fun elQueSeCuelgaSeVaSolo() {
        val m = MatchState("uno")
        m.entrarYo("Maxi", "skin_minero")
        m.aplicar(NetProtocol.unirse("dos", "Colo", "skin_minero").codificar())
        assertEquals(2, m.cantidad())
        assertTrue(m.envejecer(5f).isEmpty())
        val idos = m.envejecer(10f)
        assertEquals(listOf("dos"), idos)
        assertEquals(1, m.cantidad())
        // Pero yo nunca me echo a mi mismo.
        m.envejecer(600f)
        assertEquals(1, m.cantidad())
    }

    @Test
    fun unaPartidaNuevaLimpiaLoDeLaAnterior() {
        val m = MatchState("uno")
        m.aplicar(NetProtocol.tomar("dos", 1).codificar())
        m.aplicar(NetProtocol.romper("dos", 2).codificar())
        m.aplicar(NetProtocol.trampa("dos", 3).codificar())
        m.aplicar(NetProtocol.caido("dos").codificar())
        m.aplicar(NetProtocol.llegada("dos", 5L).codificar())

        m.aplicar(NetProtocol.arranque("uno", NetProtocol.Modo.CARRERA, 4, 99L).codificar())
        assertTrue(m.objetosTomados().isEmpty())
        assertTrue(m.paredesRotas().isEmpty())
        assertTrue(m.trampasSaltadas().isEmpty())
        assertFalse(m.jugador("dos")!!.caido)
        assertEquals(0L, m.jugador("dos")!!.tiempoFinal)
    }

    @Test
    fun elQueSeVaDesaparecoDeLaLista() {
        val m = MatchState("uno")
        m.entrarYo("Maxi", "skin_minero")
        m.aplicar(NetProtocol.unirse("dos", "Colo", "skin_minero").codificar())
        assertEquals(1, m.otros().size)
        m.aplicar(NetProtocol.salir("dos").codificar())
        assertTrue(m.otros().isEmpty())
    }

    @Test
    fun elCompanieroSoloMueveLasPiernasCuandoAvanza() {
        // El reloj de la caminata son los metros caminados y no el tiempo:
        // es lo que hace que las piernas se queden quietas con el companiero
        // parado sin tener que mandar nada extra por la red.
        val j = com.mggx.laberinto.net.JugadorRemoto("dos", "Colo", "skin_minero")
        j.pose(0f, 0f, 0f, 0f, 0)
        repeat(20) { j.suavizar(0.35f) }
        assertEquals("el paso corre con el companiero parado", 0f, j.paso, 1e-4f)

        j.pose(5f, 0f, 0f, 0f, 0)
        repeat(60) { j.suavizar(0.35f) }
        assertTrue("el paso no avanzo caminando cinco metros", j.paso > 4f)
    }

    @Test
    fun hayDosModosYCadaUnoSeExplicaSolo() {
        val modos = NetProtocol.Modo.entries
        assertEquals(2, modos.size)
        for (mo in modos) {
            assertTrue("el modo ${mo.name} no tiene etiqueta", mo.etiqueta.isNotBlank())
            assertTrue("el modo ${mo.name} no se explica", mo.explicacion.length > 30)
        }
    }
}
