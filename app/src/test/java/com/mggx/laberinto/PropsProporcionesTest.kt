package com.mggx.laberinto

import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.gl.CaveRenderer
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
    fun laLlamaNoEsMuchoMasAnchaQueElPosteYNoLoPenetra() {
        // Diametro del poste real (ver el cyl.add de la antorcha): el
        // PropMeshes.cylinder base tiene radio 0.1, multiplicado por el
        // scale que ya usa CaveRenderer para el poste.
        val radioPoste = 0.1f * 0.42f // 0.1 (radio base del cilindro) * scale del poste
        val diametroPoste = radioPoste * 2f
        val diametroLlamaMax = CaveRenderer.ESCALA_LLAMA * 2f // fl <= 1.0
        assertTrue(
            "la llama sigue siendo demasiado ancha comparada con el poste " +
                "(llama=$diametroLlamaMax, poste=$diametroPoste)",
            diametroLlamaMax <= diametroPoste * 2.5f
        )

        // La llama tiene que apoyarse en la punta del poste, no penetrarlo:
        // con stretchY=1.5 (shapeGem), el semieje vertical maximo es
        // 1.5 * ESCALA_LLAMA, y el centro esta a ALTO_POSTE_ANTORCHA + 0.13.
        val semiejeMax = 1.5f * CaveRenderer.ESCALA_LLAMA
        val centroY = CaveRenderer.ALTO_POSTE_ANTORCHA + 0.13f
        val bordeInferior = centroY - semiejeMax
        assertTrue(
            "la llama penetra el poste (borde inferior=$bordeInferior, " +
                "punta del poste=${CaveRenderer.ALTO_POSTE_ANTORCHA})",
            bordeInferior >= CaveRenderer.ALTO_POSTE_ANTORCHA - 0.01f
        )
    }
}
