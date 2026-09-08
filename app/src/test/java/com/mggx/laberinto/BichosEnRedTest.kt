package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.Enemy
import com.mggx.laberinto.game.EnemyBrain
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.game.ObjetivoBicho
import com.mggx.laberinto.maze.MazeGenerator
import com.mggx.laberinto.net.MatchLink
import com.mggx.laberinto.net.NetProtocol
import com.mggx.laberinto.net.TransporteLocal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los bichos y el salto de a varios.
 *
 * Dos cosas distintas que se prueban juntas porque las dos son lo mismo desde
 * afuera: cosas que pasan en un telefono y el otro no llegaba a ver.
 */
class BichosEnRedTest {

    private val CELL = GameSession.CELL

    // ------------------------------------------------------------- el salto

    /**
     * El bug tal cual estaba: saltar sin moverse no cambia ni x, ni z, ni el
     * angulo, ni la postura, y el filtro de "no repitas si estas quieto" no
     * miraba la altura. La pose no salia y el companiero te veia en el piso.
     */
    @Test
    fun saltarEnElLugarLeLlegaAlCompanero() {
        val mio = TransporteLocal()
        val suyo = mio.companero()
        val link = MatchLink(mio, "AAAA", "Uno", "skin_minero", anfitrion = true)

        // La primera siempre sale (no hay con que comparar): se descarta.
        link.bombear(0.2f, 10f, 1f, 20f, 90f, 0)
        suyo.recibir()

        // Quieto del todo: no tiene que salir ninguna pose.
        link.bombear(0.2f, 10f, 1f, 20f, 90f, 0)
        assertTrue(
            "quieto no tendria que mandar pose",
            suyo.recibir().none { NetProtocol.decodificar(it)?.tipo == NetProtocol.Tipo.POSE }
        )

        // Mismo lugar, misma vuelta, misma postura: solo salto.
        link.bombear(0.2f, 10f, 1.9f, 20f, 90f, 0)
        val poses = suyo.recibir()
            .mapNotNull { NetProtocol.decodificar(it) }
            .filter { it.tipo == NetProtocol.Tipo.POSE }
        assertEquals("el salto tiene que viajar", 1, poses.size)
        assertEquals("y con la altura de arriba", 1.9f, poses[0].num(1), 0.01f)
    }

    // ---------------------------------------------------- el cuadro de bicho

    @Test
    fun elCuadroDeBichoVaYVuelveIgual() {
        val c = NetProtocol.cuadroDeBicho(7, 12.34f, 56.78f, alerta = true, vivo = false)
        val b = NetProtocol.leerCuadroDeBicho(c)
        assertNotNull("no se pudo leer '$c'", b)
        assertEquals(7, b!!.indice)
        assertEquals(12.34f, b.x, 0.01f)
        assertEquals(56.78f, b.z, 0.01f)
        assertTrue(b.alerta)
        assertTrue("tendria que venir volteado", !b.vivo)
    }

    /**
     * [NetProtocol.limpiar] recorta cada campo a 24 caracteres. Un cuadro mas
     * largo que eso llegaria cortado al medio de un numero, y el bicho
     * apareceria en cualquier lado.
     */
    @Test
    fun elCuadroDeBichoEntraEnUnCampo() {
        val peor = NetProtocol.cuadroDeBicho(999, 999.99f, 999.99f, alerta = true, vivo = false)
        assertTrue("el peor caso mide ${peor.length}: $peor", peor.length <= 24)
        assertEquals("y limpiar no lo tiene que tocar", peor, NetProtocol.limpiar(peor))
    }

    @Test
    fun unCuadroRotoNoRompeNada() {
        assertNull(NetProtocol.leerCuadroDeBicho(""))
        assertNull(NetProtocol.leerCuadroDeBicho("1,2,3"))
        assertNull(NetProtocol.leerCuadroDeBicho("hola,1.0,2.0,0"))
        assertNull(NetProtocol.leerCuadroDeBicho("-1,1.0,2.0,0"))
    }

    // ------------------------------------------------------ quien reparte

    @Test
    fun elAnfitrionReparteLosBichosYElOtroLosRecibe() {
        val delAnfitrion = TransporteLocal()
        val delOtro = delAnfitrion.companero()
        val anfitrion = MatchLink(delAnfitrion, "AAAA", "Uno", "skin_minero", anfitrion = true)
        val otro = MatchLink(delOtro, "BBBB", "Dos", "skin_minero", anfitrion = false)

        anfitrion.repartirBichos(
            listOf(
                NetProtocol.cuadroDeBicho(0, 5f, 6f, alerta = true, vivo = true),
                NetProtocol.cuadroDeBicho(3, 9f, 1f, alerta = false, vivo = false)
            )
        )
        otro.latir(0.02f)

        val b0 = otro.match.bicho(0)
        assertNotNull("no le llego el bicho 0", b0)
        assertEquals(5f, b0!!.x, 0.01f)
        assertEquals(6f, b0.z, 0.01f)
        assertTrue(b0.alerta)

        val b3 = otro.match.bicho(3)
        assertNotNull("no le llego el bicho 3", b3)
        assertTrue("el 3 venia volteado", !b3!!.vivo)
        assertNull("nadie mando el bicho 1", otro.match.bicho(1))
    }

    /** El que no es anfitrion no reparte: si los dos repartieran, temblarian. */
    @Test
    fun elQueNoEsAnfitrionNoReparteBichos() {
        val delOtro = TransporteLocal()
        val delAnfitrion = delOtro.companero()
        val otro = MatchLink(delOtro, "BBBB", "Dos", "skin_minero", anfitrion = false)
        val anfitrion = MatchLink(delAnfitrion, "AAAA", "Uno", "skin_minero", anfitrion = true)

        otro.repartirBichos(listOf(NetProtocol.cuadroDeBicho(0, 5f, 6f, true, true)))
        anfitrion.latir(0.02f)
        assertNull("el anfitrion no tiene que copiar bichos ajenos", anfitrion.match.bicho(0))
    }

    /** Y aunque le llegaran igual, el anfitrion los tira: el que manda es el. */
    @Test
    fun elAnfitrionIgnoraLosBichosQueLeMandan() {
        val delColado = TransporteLocal()
        val delAnfitrion = delColado.companero()
        val anfitrion = MatchLink(delAnfitrion, "AAAA", "Uno", "skin_minero", anfitrion = true)

        // Un mensaje de bichos crudo, como si otro se pusiera a repartir.
        delColado.enviar(
            NetProtocol.bichos("CCCC", listOf(NetProtocol.cuadroDeBicho(0, 5f, 6f, true, true)))
                .codificar()
        )
        anfitrion.latir(0.02f)
        assertNull(anfitrion.match.bicho(0))
    }

    @Test
    fun unNivelNuevoSeLlevaLosBichosDelAnterior() {
        val delOtro = TransporteLocal()
        val delAnfitrion = delOtro.companero()
        val otro = MatchLink(delOtro, "BBBB", "Dos", "skin_minero", anfitrion = false)
        val anfitrion = MatchLink(delAnfitrion, "AAAA", "Uno", "skin_minero", anfitrion = true)

        anfitrion.repartirBichos(listOf(NetProtocol.cuadroDeBicho(0, 5f, 6f, true, true)))
        otro.latir(0.02f)
        assertNotNull(otro.match.bicho(0))

        anfitrion.arrancar(NetProtocol.Modo.CARRERA, 9, 12345L)
        otro.latir(0.02f)
        assertNull("los bichos del nivel viejo tienen que irse", otro.match.bicho(0))
    }

    // -------------------------------------------------- a quien persiguen

    /** Un bicho suelto en una casilla abierta, bien lejos del arranque. */
    private fun bichoSuelto(s: GameSession): Enemy {
        val m = s.maze
        val camino = m.solutionPath
        val celda = camino[minOf(6, camino.size - 1)]
        return Enemy(
            MazeGenerator.EnemyKind.MURCIELAGO,
            (celda % m.gw + 0.5f) * CELL, (celda / m.gw + 0.5f) * CELL,
            celda % m.gw, celda / m.gw, 0f
        )
    }

    private fun corrida(
        s: GameSession, e: Enemy, brain: EnemyBrain,
        companieros: List<ObjetivoBicho>, vueltas: Int = 1
    ) {
        repeat(vueltas) {
            brain.update(
                1f / 60f, listOf(e),
                // Uno mismo, a mil kilometros: asi no compite por la presa y
                // lo unico que decide son los companieros del test.
                px = -5000f, pz = -5000f, pRadio = GameSession.PLAYER_RADIUS,
                alcanceEscala = 1f, detectable = true, ruidoso = false,
                cell = CELL, pisoDe = { gx, gy -> s.maze.floorY(gx, gy) },
                yoId = "YO", companieros = companieros, moverlos = false
            ) { }
        }
    }

    @Test
    fun persigueAlQueTieneMasCerca() {
        val s = GameSession(SaveData.fromStore(SaveData.memoryStore()), 15, 4242L)
        val e = bichoSuelto(s)
        val brain = EnemyBrain(s.maze, 7L)

        val lejos = ObjetivoBicho("LEJOS", e.x + 8f, e.z, detectable = true, ruidoso = false)
        val cerca = ObjetivoBicho("CERCA", e.x + 2f, e.z, detectable = true, ruidoso = false)
        // Se pasa el lejano primero para que el orden de la lista no sea lo
        // que decide: tiene que ganar la distancia.
        corrida(s, e, brain, listOf(lejos, cerca))

        assertEquals("tendria que ir por el mas cercano", "CERCA", e.objetivoId)
        assertTrue("y ponerse en guardia", e.alerta)
    }

    /**
     * Lo que pidio el usuario: si al que persigue se le va lejos, el bicho
     * mira quien le quedo cerca y se va por ese.
     */
    @Test
    fun siElQuePersigueSeLeVaLejosCambiaAlMasCercano() {
        val s = GameSession(SaveData.fromStore(SaveData.memoryStore()), 15, 4242L)
        val e = bichoSuelto(s)
        val brain = EnemyBrain(s.maze, 7L)
        val alcance = e.kind.alcance

        // Primero se engancha con A, que es el unico que hay.
        corrida(s, e, brain, listOf(ObjetivoBicho("A", e.x + 2f, e.z, true, false)))
        assertEquals("A", e.objetivoId)

        // A se va bien lejos (mas alla del aguante) y aparece B al lado.
        corrida(
            s, e, brain,
            listOf(
                ObjetivoBicho("A", e.x + alcance * 3f, e.z, true, false),
                ObjetivoBicho("B", e.x + 2f, e.z, true, false)
            )
        )
        assertEquals("tendria que haber soltado a A por B", "B", e.objetivoId)
    }

    /**
     * Pero mientras lo tenga a tiro no lo suelta. Sin esto, dos que corren
     * juntos le hacen girar la cabeza sin avanzar hacia ninguno.
     */
    @Test
    fun noCambiaDePresaPorUnMetroDeDiferencia() {
        val s = GameSession(SaveData.fromStore(SaveData.memoryStore()), 15, 4242L)
        val e = bichoSuelto(s)
        val brain = EnemyBrain(s.maze, 7L)

        corrida(s, e, brain, listOf(ObjetivoBicho("A", e.x + 3f, e.z, true, false)))
        assertEquals("A", e.objetivoId)

        // B se le pone un poco mas cerca que A, pero A sigue ahi nomas.
        corrida(
            s, e, brain,
            listOf(
                ObjetivoBicho("A", e.x + 3f, e.z, true, false),
                ObjetivoBicho("B", e.x + 2f, e.z, true, false)
            )
        )
        assertEquals("no tendria que cambiar de presa por tan poco", "A", e.objetivoId)
    }

    /** Al caido no se lo persigue: ya esta en el piso. */
    @Test
    fun noPersigueAUnCaido() {
        val s = GameSession(SaveData.fromStore(SaveData.memoryStore()), 15, 4242L)
        val e = bichoSuelto(s)
        val brain = EnemyBrain(s.maze, 7L)

        corrida(
            s, e, brain,
            listOf(
                ObjetivoBicho("CAIDO", e.x + 1f, e.z, true, false, enPie = false),
                ObjetivoBicho("EN_PIE", e.x + 4f, e.z, true, false, enPie = true)
            )
        )
        assertEquals("EN_PIE", e.objetivoId)
    }

    /** Si no hay nadie a la vista, se olvida del asunto y vuelve a lo suyo. */
    @Test
    fun sinNadieCercaSeOlvidaDeLaPresa() {
        val s = GameSession(SaveData.fromStore(SaveData.memoryStore()), 15, 4242L)
        val e = bichoSuelto(s)
        val brain = EnemyBrain(s.maze, 7L)

        corrida(s, e, brain, listOf(ObjetivoBicho("A", e.x + 2f, e.z, true, false)))
        assertEquals("A", e.objetivoId)

        // Todos se fueron. El interes dura unos segundos, asi que se le da
        // tiempo de sobra para que se apague.
        corrida(s, e, brain, emptyList(), vueltas = 60 * 8)
        assertNull("tendria que haberse olvidado", e.objetivoId)
        assertTrue("y bajar la guardia", !e.alerta)
    }

    /** El que copia no mueve los bichos: los pone donde le dicen. */
    @Test
    fun elQueCopiaNoMueveLosBichos() {
        val s = GameSession(SaveData.fromStore(SaveData.memoryStore()), 15, 4242L)
        val e = bichoSuelto(s)
        val brain = EnemyBrain(s.maze, 7L)
        val x0 = e.x
        val z0 = e.z

        // Con una presa pegada al lado y muchas vueltas: si se moviera, se
        // movia.
        corrida(s, e, brain, listOf(ObjetivoBicho("A", e.x + 2f, e.z, true, false)), vueltas = 300)
        assertEquals("no se tendria que haber movido", x0, e.x, 0.001f)
        assertEquals("no se tendria que haber movido", z0, e.z, 0.001f)
        assertTrue("pero si tiene que saber a quien persigue", e.objetivoId == "A")
    }

    /** Copiar una muerte ajena no paga ecos: los cobra el que le pego. */
    @Test
    fun darPorVolteadoNoRepiteNiPremia() {
        val s = GameSession(SaveData.fromStore(SaveData.memoryStore()), 15, 4242L)
        val e = bichoSuelto(s)
        assertTrue(e.vivo)
        e.darPorVolteado()
        assertTrue("tendria que quedar volteado", !e.vivo)
        // Y de nuevo no cambia nada: es lo que evita el premio en cada cuadro.
        e.darPorVolteado()
        assertTrue(!e.vivo)
    }
}
