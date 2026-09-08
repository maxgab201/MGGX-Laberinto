package com.mggx.laberinto

import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.gl.CaveRenderer
import com.mggx.laberinto.gl.PropMeshes
import com.mggx.laberinto.gl.StructureMeshes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Objetos 3D que se veian raros por su proporcion, con numeros concretos:
 * las monedas eran el triple de grandes de lo esperado, las estalactitas
 * tenian forma de "gorro de fiesta" (corto y ancho) en vez de una punta
 * fina, y la llama de la antorcha era mas ancha que el poste y lo penetraba.
 *
 * Estos tests no dibujan nada (no hay GL en la JVM): solo protegen las
 * constantes numericas de CaveRenderer para que ninguno de estos tres
 * problemas vuelva sin querer.
 */
class PropsProporcionesTest {

    // -------------------------------------------------------------- monedas

    @Test
    fun lasMonedasSonBastanteMasChicasQueElJugador() {
        val radioJugador = GameSession.PLAYER_RADIUS
        assertTrue(
            "el ECO sigue siendo demasiado grande",
            CaveRenderer.ESCALA_ECO < radioJugador * 0.5f
        )
        assertTrue(
            "el VETAGRIS vuelve a medir lo mismo (o mas) que el jugador",
            CaveRenderer.ESCALA_VETAGRIS < radioJugador
        )
        // Y que seguimos manteniendo una jerarquia de tamanos entre las tres.
        assertTrue(CaveRenderer.ESCALA_ECO < CaveRenderer.ESCALA_ECO_GRANDE)
        assertTrue(CaveRenderer.ESCALA_ECO_GRANDE < CaveRenderer.ESCALA_VETAGRIS)
    }

    // --------------------------------------------------------- estalactitas

    @Test
    fun laEstalactitaEsUnaPuntaFinaNoUnGorroDeFiesta() {
        // Altura base del cono = 1 (ver PropMeshes.cone), asi que la relacion
        // altura:radio es directamente 1/radio. Antes era 1/0.42 = 2.4:1.
        val ratio = 1f / CaveRenderer.RADIO_ESTALACTITA
        assertTrue(
            "la estalactita/estalagmita sigue siendo corta y ancha (ratio=$ratio)",
            ratio >= 6f
        )
    }

    // ------------------------------------------------------------ antorcha

    @Test
    fun laLlamaSeApoyaEnElCuencoYNoSePuedeHundirEnElPoste() {
        // El modelo de la llama tiene la base en y=0. Eso es lo que hace
        // imposible que se hunda: se la apoya y listo. El octaedro de antes
        // tenia el centro en su posicion, asi que media figura quedaba
        // metida adentro del poste.
        val llama = StructureMeshes.llama()
        var minY = Float.MAX_VALUE
        var i = 1
        while (i < llama.vertices.size) {
            if (llama.vertices[i] < minY) minY = llama.vertices[i]
            i += 6
        }
        assertEquals("la llama ya no tiene la base en cero", 0f, minY, 1e-4f)
    }

    @Test
    fun laLlamaEntraEnElCuencoDeLaAntorcha() {
        // Se comparan anchos REALES en metros, sacados de las mallas mismas:
        // asi el test sigue valiendo si maniana se retoca cualquiera de las
        // dos formas, sin tener que acordarse de actualizar un numero.
        val anchoLlama = anchoDe(StructureMeshes.llama()) * CaveRenderer.ESCALA_LLAMA
        val anchoCuenco = StructureMeshes.RADIO_CUENCO * 2f * CaveRenderer.ALTO_ANTORCHA
        assertTrue(
            "la llama (${anchoLlama}m) no entra en el cuenco (${anchoCuenco}m)",
            anchoLlama <= anchoCuenco
        )
        // Y que la llama se vea: mas alta que el propio cuenco.
        assertTrue(
            "la llama quedo mas baja que el cuenco que la contiene",
            CaveRenderer.ESCALA_LLAMA > anchoCuenco * 0.5f
        )
    }

    @Test
    fun elBrazoDeLaAntorchaLlegaHastaLaPared() {
        // La antorcha se cuelga a SEPARACION_ANTORCHA del centro de la
        // casilla; lo que falta hasta la roca lo tiene que cubrir el brazo, si
        // no queda flotando despegada de la pared.
        val hastaLaPared = (0.5f - CaveRenderer.SEPARACION_ANTORCHA) * GameSession.CELL
        val brazo = StructureMeshes.LARGO_BRAZO * CaveRenderer.ALTO_ANTORCHA
        assertTrue(
            "el brazo mide ${brazo}m y hasta la pared hay ${hastaLaPared}m: queda flotando",
            brazo >= hastaLaPared
        )
    }

    private fun anchoDe(g: PropMeshes.Geometry): Float {
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        var i = 0
        while (i < g.vertices.size) {
            if (g.vertices[i] < min) min = g.vertices[i]
            if (g.vertices[i] > max) max = g.vertices[i]
            i += 6
        }
        return max - min
    }
}
