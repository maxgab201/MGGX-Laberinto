package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.net.CodigoSala
import com.mggx.laberinto.net.RelayFirebase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * El codigo con el que se invita a una sala.
 *
 * Es lo primero que toca el jugador del multijugador y lo unico que tiene que
 * transmitirle a su amigo, casi siempre dictandoselo en voz alta. Si se
 * confunde una letra, no entra: por eso el alfabeto y la normalizacion tienen
 * tests propios.
 */
class CodigoSalaTest {

    @Test
    fun elCodigoNoTieneLetrasQueSeConfundenAlDictarlo() {
        // I con 1 con L, y O con 0: al oido son lo mismo.
        for (mala in listOf('I', 'O', '0', '1', 'L')) {
            assertFalse(
                "el alfabeto tiene '$mala', que se confunde al dictar",
                CodigoSala.LETRAS.contains(mala)
            )
        }
    }

    @Test
    fun cadaCodigoNuevoTieneElLargoJustoYSoloLetrasBuenas() {
        val rnd = Random(12345)
        repeat(200) {
            val c = CodigoSala.nuevo(rnd)
            assertEquals(CodigoSala.LARGO, c.length)
            for (ch in c) assertTrue("'$ch' no esta en el alfabeto", CodigoSala.LETRAS.contains(ch))
            assertTrue("un codigo recien hecho no se acepta a si mismo", CodigoSala.valido(c))
        }
    }

    @Test
    fun dosCodigosSeguidosCasiNuncaSonElMismo() {
        // Con 31 letras y 4 posiciones hay casi un millon de combinaciones:
        // que salgan repetidos en 500 tiradas seria un generador roto.
        val rnd = Random(999)
        val vistos = HashSet<String>()
        repeat(500) { vistos.add(CodigoSala.nuevo(rnd)) }
        assertTrue("el generador repite demasiado: ${vistos.size} distintos", vistos.size > 490)
    }

    @Test
    fun daIgualComoLoEscribaElQueEntra() {
        // El que lo recibe lo puede escribir de cualquier forma.
        val esperado = "A7K2"
        for (escrito in listOf("A7K2", "a7k2", "a7-k2", "a 7 k 2", " A7K2 ", "a.7,k;2")) {
            assertEquals("'$escrito' no cae en la misma sala", esperado, CodigoSala.normalizar(escrito))
        }
    }

    @Test
    fun unCodigoIncompletoOVacioNoSirve() {
        assertFalse(CodigoSala.valido(""))
        assertFalse(CodigoSala.valido("A7K"))
        assertFalse(CodigoSala.valido("----"))
        assertTrue(CodigoSala.valido("A7K2"))
        // Y de mas tampoco: se recorta al largo justo.
        assertEquals("A7K2", CodigoSala.normalizar("A7K2XYZ"))
    }

    @Test
    fun elCodigoDeSalaSobreviveElViajeHastaElPathDeFirebase() {
        // El codigo se usa como nombre del nodo en la base: si el saneo de
        // Firebase lo cambiara, dos jugadores con el mismo codigo podrian
        // terminar en salas distintas.
        val rnd = Random(7)
        repeat(100) {
            val c = CodigoSala.nuevo(rnd)
            assertEquals(
                "el codigo $c cambia al armar el path",
                "salas/$c/msgs", RelayFirebase.pathDeSala(c)
            )
        }
    }

    @Test
    fun losIdsDeJugadorNoSeRepitenEnUnaSala() {
        val rnd = Random(4242)
        val vistos = HashSet<String>()
        repeat(2000) { vistos.add(CodigoSala.idDeJugador(rnd)) }
        assertTrue("hay ids repetidos: ${vistos.size} de 2000", vistos.size > 1990)
    }

    @Test
    fun elIdDeJugadorNoRompeElFormatoDeLosMensajes() {
        // El id va como campo en cada mensaje: si tuviera la barra separadora
        // adentro, correria todos los campos que siguen.
        val rnd = Random(1)
        repeat(100) {
            val id = CodigoSala.idDeJugador(rnd)
            assertFalse(id.contains('|'))
            assertEquals(id, com.mggx.laberinto.net.NetProtocol.limpiar(id))
        }
    }

    // ------------------------------------------------------------- el nombre

    @Test
    fun elNombreDeJugadorSeGuardaYVuelve() {
        val store = SaveData.memoryStore()
        val a = SaveData.fromStore(store)
        assertEquals("", a.nombreJugador)
        a.setNombreJugador("Maxi")
        assertEquals("Maxi", SaveData.fromStore(store).nombreJugador)
    }

    @Test
    fun elNombreSeLimpiaAntesDeGuardarlo() {
        val save = SaveData.fromStore(SaveData.memoryStore())
        // Con la barra adentro rompe el formato de los mensajes, asi que se
        // limpia al guardarlo y no al mandarlo: si no, tu nombre se veria de
        // una forma en tu pantalla y de otra en la del de al lado.
        save.setNombreJugador("Ma|xi")
        assertFalse(save.nombreJugador.contains('|'))
        save.setNombreJugador("Un nombre larguisimo que no entra en ningun lado")
        assertTrue("el nombre no se recorto", save.nombreJugador.length <= 24)
        assertNotEquals("", save.nombreJugador)
    }
}
