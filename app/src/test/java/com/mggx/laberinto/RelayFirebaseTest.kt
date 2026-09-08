package com.mggx.laberinto

import com.mggx.laberinto.net.RelayFirebase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La parte de TransporteFirebase que no necesita el SDK de Firebase (que en
 * los tests de JVM es un stub que no hace nada, ver
 * `unitTests.isReturnDefaultValues` en `app/build.gradle.kts`): el path de la
 * sala, el saneo del codigo y cada cuanto toca podar mensajes viejos.
 */
class RelayFirebaseTest {

    @Test
    fun elCodigoDeSalaSoloDejaLetrasYNumeros() {
        // Lo escribe o dicta cualquiera: no puede tener nada que rompa un
        // path de Firebase (. # $ [ ] /).
        assertEquals("SALA7K", RelayFirebase.sanearCodigo("sala-7k!"))
        assertEquals("A7K2", RelayFirebase.sanearCodigo("A7K2"))
        assertEquals("ABCDEF", RelayFirebase.sanearCodigo("a.b#c\$d[e]f/"))
    }

    @Test
    fun unCodigoVacioOSoloSimbolosCaeEnUnDefault() {
        assertEquals("SALA", RelayFirebase.sanearCodigo(""))
        assertEquals("SALA", RelayFirebase.sanearCodigo("###"))
    }

    @Test
    fun elCodigoSeRecortaAOchoCaracteres() {
        assertEquals("ABCDEFGH", RelayFirebase.sanearCodigo("abcdefghijk"))
    }

    @Test
    fun elPathDeSalaUsaElCodigoYaSaneado() {
        assertEquals("salas/A7K2/msgs", RelayFirebase.pathDeSala("a7k2"))
        assertEquals("salas/SALA/msgs", RelayFirebase.pathDeSala("###"))
    }

    @Test
    fun tocaPodarCadaTantosEnvios() {
        // Ni antes ni despues: justo en el multiplo, y nunca en cero.
        assertFalse(RelayFirebase.tocaPodar(0))
        for (i in 1 until RelayFirebase.CADA_CUANTOS_ENVIOS_PODAR) {
            assertFalse("no deberia podar en el envio $i", RelayFirebase.tocaPodar(i))
        }
        assertTrue(RelayFirebase.tocaPodar(RelayFirebase.CADA_CUANTOS_ENVIOS_PODAR))
        assertTrue(RelayFirebase.tocaPodar(RelayFirebase.CADA_CUANTOS_ENVIOS_PODAR * 3))
    }

    @Test
    fun laVidaDelMensajeAlcanzaDeSobraParaLosEnviosEntrePodas() {
        // A 10 mensajes por segundo (el ritmo de POSE), CADA_CUANTOS_ENVIOS_PODAR
        // mensajes tardan unos pocos segundos en acumularse: tiene que ser
        // bastante menos que VIDA_MENSAJE_MS, si no un mensaje se podaria
        // antes de que todos lo hayan podido leer.
        val segundosEntrePodas = RelayFirebase.CADA_CUANTOS_ENVIOS_PODAR / 10.0
        assertTrue(
            "la poda es mas seguida que la vida del mensaje",
            segundosEntrePodas * 1000 < RelayFirebase.VIDA_MENSAJE_MS
        )
    }
}
