package com.mggx.laberinto

import com.mggx.laberinto.gl.PlayerMeshes
import com.mggx.laberinto.gl.PropMeshes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El companiero de sala, visto desde afuera.
 *
 * Todo lo que se controla aca sale de la misma idea: en un pasillo oscuro, a
 * varios metros, de una persona no se ve la cara ni la ropa. Se ve la
 * SILUETA. Por eso importan las proporciones, que cargue cosas y que se sepa
 * para donde mira, y no el detalle fino.
 */
class PersonajeTest {

    private class Caja(
        val minX: Float, val maxX: Float,
        val minY: Float, val maxY: Float,
        val minZ: Float, val maxZ: Float
    ) {
        val ancho get() = maxX - minX
        val alto get() = maxY - minY
        val fondo get() = maxZ - minZ
    }

    private fun caja(g: PropMeshes.Geometry, filtro: (Float, Float, Float) -> Boolean = { _, _, _ -> true }): Caja {
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        var i = 0
        while (i < g.vertices.size) {
            val x = g.vertices[i]; val y = g.vertices[i + 1]; val z = g.vertices[i + 2]
            if (filtro(x, y, z)) {
                if (x < minX) minX = x; if (x > maxX) maxX = x
                if (y < minY) minY = y; if (y > maxY) maxY = y
                if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
            }
            i += 6
        }
        return Caja(minX, maxX, minY, maxY, minZ, maxZ)
    }

    @Test
    fun elCuerpoSeApoyaEnElPisoYMideUnaUnidad() {
        // El renderer lo escala por su altura en metros, asi que el modelo
        // tiene que medir 1: si midiera 0.8, un companiero de 1,72 m se
        // dibujaria de 1,38 y con los pies hundidos en la roca.
        val c = caja(PlayerMeshes.mineroCuerpo())
        assertEquals("no se apoya en cero: se hundiria en el piso", 0f, c.minY, 1e-4f)
        assertTrue("no mide una unidad de alto (mide ${c.maxY})", c.maxY in 0.85f..1.0f)
    }

    @Test
    fun tieneProporcionDePersonaYNoDeBulto() {
        val c = caja(PlayerMeshes.mineroCuerpo())
        assertTrue("es mas ancho que una persona (${c.ancho})", c.ancho < c.maxY * 0.75f)
        assertTrue("es mas hondo que una persona (${c.fondo})", c.fondo < c.maxY * 0.75f)
        // Y no al reves: un palo tampoco es una persona.
        assertTrue("es demasiado flaco (${c.ancho})", c.ancho > c.maxY * 0.30f)
    }

    @Test
    fun cargaCosasEnLaEspalda() {
        // La mochila y el pico son lo que lo vuelve un MINERO y no una persona
        // generica, y es lo unico de todo eso que se lee de lejos.
        //
        // El modelo mira hacia +Z (la visera del casco marca el frente), asi
        // que la carga tiene que estar del lado -Z.
        val c = caja(PlayerMeshes.mineroCuerpo())
        val soloTorso = caja(PlayerMeshes.mineroCuerpo()) { _, y, _ -> y in 0.45f..0.86f }
        assertTrue(
            "no lleva nada a la espalda: el cuerpo no sobresale para atras",
            soloTorso.minZ < -0.20f
        )
        assertTrue(
            "la carga sobresale mas para atras que para adelante, como tiene que ser",
            -c.minZ > c.maxZ
        )
    }

    @Test
    fun loQueLlevaALaEspaldaNoLoConvierteEnUnaCola() {
        // El pico se lleva ATRAVESADO sobre los hombros. Apuntando hacia atras
        // medía 46 cm de saliente en un cuerpo de 1,72 m y de costado se veia
        // como si arrastrara algo.
        val c = caja(PlayerMeshes.mineroCuerpo())
        assertTrue(
            "sobresale demasiado por la espalda (${-c.minZ} de un alto de ${c.maxY})",
            -c.minZ < c.maxY * 0.30f
        )
    }

    @Test
    fun tieneCabezaYVaAparteDelCuerpo() {
        // Va en malla propia porque el renderer pinta cada instancia de UN
        // color: adentro del cuerpo, la cara saldria del color del abrigo y el
        // companiero se veria como un traje vacio con un casco encima.
        val cabeza = PlayerMeshes.mineroCabeza()
        assertTrue("la cabeza esta vacia", cabeza.indices.size > 60)
        val c = caja(cabeza)
        assertEquals("la cabeza no se apoya en cero", 0f, c.minY, 1e-4f)
        assertTrue("la cabeza no mide una unidad", c.maxY in 0.9f..1.0f)
        // Una cabeza es mas alta que ancha.
        assertTrue("la cabeza es mas ancha que alta", c.ancho < c.maxY)
    }

    @Test
    fun laCaraMiraParaAdelante() {
        // De lejos no se ve una cara, pero SI se ve para donde apunta. La
        // nariz y la barba son lo que lo dice: sin ellas la cabeza es una bola
        // y no se sabe si viene o va.
        val c = caja(PlayerMeshes.mineroCabeza())
        assertTrue(
            "la cabeza es simetrica en Z: no se sabe para donde mira",
            c.maxZ > -c.minZ + 0.04f
        )
    }

    @Test
    fun elCascoSeSabeParaDondeApunta() {
        // La visera es lo que permite saber de una para donde esta mirando el
        // companiero, aun de lejos y sin verle la cara.
        val c = caja(PlayerMeshes.mineroCasco())
        assertTrue("el casco es simetrico: no se sabe para donde mira", c.maxZ > -c.minZ + 0.10f)
    }

    @Test
    fun elCascoEntraSobreLaCabeza() {
        // El renderer dibuja la cabeza a 0.155 del alto y el casco a 0.30. Si
        // el casco fuera mas angosto que el craneo, la cabeza lo atravesaria.
        val cabeza = caja(PlayerMeshes.mineroCabeza())
        val casco = caja(PlayerMeshes.mineroCasco())
        val anchoCabezaReal = cabeza.ancho * 0.155f
        val anchoCascoReal = casco.ancho * 0.30f
        assertTrue(
            "el casco ($anchoCascoReal) es mas angosto que la cabeza ($anchoCabezaReal)",
            anchoCascoReal > anchoCabezaReal
        )
    }

    @Test
    fun ningunaPiezaTieneValoresRotos() {
        for ((nombre, g) in mapOf(
            "cuerpo" to PlayerMeshes.mineroCuerpo(),
            "cabeza" to PlayerMeshes.mineroCabeza(),
            "casco" to PlayerMeshes.mineroCasco()
        )) {
            for (v in g.vertices) {
                assertTrue("hay un valor invalido en $nombre", !v.isNaN() && !v.isInfinite())
            }
            val vertices = g.vertices.size / 6
            for (i in g.indices) {
                assertTrue("indice fuera de rango en $nombre", i in 0 until vertices)
            }
            assertEquals("$nombre no cierra en triangulos", 0, g.indices.size % 3)
        }
    }
}
