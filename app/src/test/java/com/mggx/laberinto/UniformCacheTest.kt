package com.mggx.laberinto

import com.mggx.laberinto.gl.UniformCache
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La cache de posiciones de uniforms.
 *
 * El renderer hacia 62 `glGetUniformLocation` por cuadro —casi 3.700 por
 * segundo a 60 fps— preguntando siempre lo mismo: la posicion de un uniform
 * no cambia mientras el programa siga enlazado. Cada una de esas cruza a JNI y
 * le hace al driver una busqueda por string. Es trabajo tirado, y en un
 * telefono se paga en bateria.
 */
class UniformCacheTest {

    @Test
    fun preguntaUnaSolaVezPorNombre() {
        val c = UniformCache()
        var llamadas = 0
        val buscar = { _: Int, nombre: String -> llamadas++; nombre.length }

        repeat(500) {
            c.loc(1, "uViewProj", buscar)
            c.loc(1, "uCamPos", buscar)
            c.loc(1, "uTime", buscar)
        }
        assertEquals("le pregunto al driver de mas", 3, llamadas)
        assertEquals(3, c.consultasReales)
    }

    @Test
    fun devuelveSiempreLaMismaPosicion() {
        val c = UniformCache()
        val buscar = { _: Int, nombre: String -> nombre.hashCode() }
        val primera = c.loc(7, "uAmbient", buscar)
        repeat(50) {
            assertEquals("devolvio otra posicion", primera, c.loc(7, "uAmbient", buscar))
        }
    }

    @Test
    fun cadaProgramaTieneSuPropiaTabla() {
        // Dos programas distintos pueden tener el mismo uniform en posiciones
        // distintas: cachear por nombre sin mirar el programa pintaria con los
        // datos equivocados.
        val c = UniformCache()
        val buscar = { prog: Int, nombre: String -> prog * 100 + nombre.length }
        assertEquals(105, c.loc(1, "uTime", buscar))
        assertEquals(205, c.loc(2, "uTime", buscar))
        assertEquals(2, c.consultasReales)
    }

    @Test
    fun alPerderElContextoSeOlvidaTodo() {
        // Cuando se recrea el contexto de GL los programas se vuelven a crear
        // con ids nuevos. Si la cache siguiera contestando lo de antes, el
        // renderer escribiria uniforms en posiciones de un programa que ya no
        // existe: se ve como una pantalla negra o como colores rotos, y no da
        // ningun error.
        val c = UniformCache()
        var version = 0
        val buscar = { _: Int, _: String -> version }

        assertEquals(0, c.loc(1, "uViewProj", buscar))
        version = 99
        assertEquals("no tendria que volver a preguntar", 0, c.loc(1, "uViewProj", buscar))

        c.limpiar()
        assertEquals("despues de limpiar tiene que volver a preguntar", 99, c.loc(1, "uViewProj", buscar))
    }

    @Test
    fun unUniformQueNoExisteNoSePreguntaDosVeces() {
        // El driver devuelve -1 para un uniform que el compilador saco por no
        // usarse. Eso NO es un fallo de cache: si no se guardara, cada cuadro
        // se volveria a preguntar por los que nunca van a estar, que es
        // justamente el caso peor.
        val c = UniformCache()
        var llamadas = 0
        val buscar = { _: Int, _: String -> llamadas++; -1 }
        repeat(100) { c.loc(1, "uNoExiste", buscar) }
        assertEquals(1, llamadas)
        assertEquals(-1, c.loc(1, "uNoExiste", buscar))
    }
}
