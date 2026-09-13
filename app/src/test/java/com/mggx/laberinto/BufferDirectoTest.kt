package com.mggx.laberinto

import com.mggx.laberinto.gl.BufferDirecto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El buffer directo que se reusa para subir datos a OpenGL cuadro a cuadro.
 *
 * Por que importa: `ByteBuffer.allocateDirect` no es una asignacion comun. Esa
 * memoria vive FUERA del monton de Java y el recolector la libera tarde, con su
 * propio mecanismo. Pedir una por cuadro son sesenta por segundo que nadie
 * devuelve hasta que el sistema empieza a apretar — y en un telefono eso
 * termina en tironeos y en la app cerrada de golpe.
 *
 * El renderer lo hacia en dos lugares de la ruta de dibujo (el polvo del aire y
 * las calcomanias del piso). Con el polvo en calidad ultra eran 5 KB por
 * cuadro: 300 KB por segundo de memoria nativa.
 */
class BufferDirectoTest {

    @Test
    fun noVuelveAAsignarSiLosDatosEntran() {
        val b = BufferDirecto(64)
        val capacidadInicial = b.capacidadActual
        val datos = FloatArray(64) { it.toFloat() }
        repeat(1000) { b.cargar(datos) }
        assertEquals(
            "volvio a asignar aunque los datos entraban",
            capacidadInicial, b.capacidadActual
        )
    }

    @Test
    fun devuelveSiempreElMismoBuffer() {
        // Es lo que lo hace util: si devolviera uno nuevo, no habriamos
        // ahorrado nada.
        val b = BufferDirecto(32)
        val datos = FloatArray(10)
        val primero = b.cargar(datos)
        repeat(50) {
            assertSame("devolvio un buffer distinto", primero, b.cargar(datos))
        }
    }

    @Test
    fun creceCuandoHaceFaltaYDejaDeCrecer() {
        val b = BufferDirecto(8)
        b.cargar(FloatArray(100))
        val capacidad = b.capacidadActual
        assertTrue("no crecio lo suficiente", capacidad >= 100)
        // Y una vez que crecio, se queda: mil cargas mas no vuelven a asignar.
        repeat(1000) { b.cargar(FloatArray(100)) }
        assertEquals("siguio creciendo de mas", capacidad, b.capacidadActual)
    }

    @Test
    fun cargaExactamenteLoQueSeLePide() {
        // Se le pasa el array entero pero solo una parte valida: subir de mas
        // mandaria basura a la placa (posiciones viejas del cuadro anterior).
        val b = BufferDirecto(16)
        val datos = FloatArray(16) { it.toFloat() }
        val buf = b.cargar(datos, cuantos = 5)
        assertEquals("no dejo listos exactamente los pedidos", 5, buf.remaining())
        for (i in 0 until 5) {
            assertEquals(i.toFloat(), buf.get(i), 0f)
        }
    }

    @Test
    fun cargarCeroNoRompe() {
        // Un cuadro sin polvo ni rastro: no hay nada que subir.
        val b = BufferDirecto(16)
        val buf = b.cargar(FloatArray(0))
        assertEquals(0, buf.remaining())
        // Y el siguiente con datos tiene que andar igual.
        assertEquals(3, b.cargar(FloatArray(3)).remaining())
    }

    @Test
    fun esDirectoYConElOrdenDeLaMaquina() {
        // Si no fuera directo, OpenGL no lo acepta. Y si el orden de bytes no
        // fuera el nativo, los floats llegarian dados vuelta: se veria como
        // geometria explotada, sin ningun error.
        val b = BufferDirecto(8)
        val buf = b.cargar(FloatArray(4) { it + 1f })
        assertTrue("el buffer no es directo: OpenGL no lo acepta", buf.isDirect)
        assertEquals(1f, buf.get(0), 0f)
        assertEquals(4f, buf.get(3), 0f)
    }

    @Test
    fun elContenidoViejoNoSeMezclaConElNuevo() {
        // Es el caso que importa cuadro a cuadro: en uno hay 200 motas y en el
        // siguiente 10. Si el buffer no se reiniciara, las 190 viejas seguirian
        // ahi y se dibujarian motas fantasma en posiciones del pasado.
        val b = BufferDirecto(256)
        b.cargar(FloatArray(200) { 99f })
        val buf = b.cargar(FloatArray(10) { it.toFloat() })
        assertEquals("quedaron datos del cuadro anterior", 10, buf.remaining())
        for (i in 0 until 10) assertEquals(i.toFloat(), buf.get(i), 0f)
    }
}
