package com.mggx.laberinto

import com.mggx.laberinto.gl.ArmsMesh
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El pulgar tiene que quedar del mismo lado que el dedo indice (el mas
 * cercano al centro del cuerpo, "hacia adentro"), no del lado del menique.
 *
 * Bug real que motiva este test: `buildArm()` se dibuja una sola vez
 * (mirando hacia -Z) y se espeja con `side` para armar el par de manos, asi
 * que un pulgar mal puesto en el modelo base queda mal puesto en las DOS
 * manos por igual — no es algo que el espejado por si solo pueda arreglar.
 */
class ArmsMeshTest {

    @Test
    fun elPulgarQuedaDelLadoDelIndice() {
        assertTrue(
            "el pulgar (${ArmsMesh.PULGAR_X}) tiene que tener el mismo signo " +
                "que el indice (${ArmsMesh.INDICE_X}): los dos del lado de adentro",
            ArmsMesh.PULGAR_X * ArmsMesh.INDICE_X > 0f
        )
    }

    @Test
    fun laMallaSigueSiendoValida() {
        // No repite el chequeo de orientacion de caras (eso ya lo cubre
        // MeshWindingTest); solo confirma que el cambio no rompio el build.
        val mesh = ArmsMesh.build()
        assertTrue(mesh.vertices.isNotEmpty())
        assertTrue(mesh.indices.isNotEmpty())
        assertTrue(mesh.indices.size % 3 == 0)
    }

    /**
     * Caja que ocupa la mano derecha en la franja de la palma.
     *
     * Se toma solo z entre -0.13 y -0.03: ahi esta la palma con el pulgar, y
     * quedan afuera el puno del guante (que va en z positivo) y los dedos
     * (que arrancan mas adelante de -0.14).
     */
    private fun cajaDeLaPalma(): FloatArray {
        val mesh = ArmsMesh.build()
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var i = 0
        while (i < mesh.vertices.size) {
            val x = mesh.vertices[i]
            val y = mesh.vertices[i + 1]
            val z = mesh.vertices[i + 2]
            val lado = mesh.vertices[i + 6]
            if (lado > 0.5f && lado < 1.5f && z in -0.13f..-0.03f) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
            i += ArmsMesh.STRIDE_FLOATS
        }
        return floatArrayOf(minX, maxX, minY, maxY)
    }

    @Test
    fun laManoVaVerticalYNoDePalmaParaAbajo() {
        // La mano se modela plana y despues se para. Si el giro no estuviera,
        // la palma quedaria mirando al piso: ancha en X y finita en Y, que es
        // exactamente como se veia antes.
        val (minX, maxX, minY, maxY) = cajaDeLaPalma().toList()
        val ancho = maxX - minX
        val alto = maxY - minY
        assertTrue(
            "la mano sigue de palma para abajo: mide $ancho de ancho y $alto de alto",
            alto > ancho * 1.4f
        )
    }

    @Test
    fun elPulgarQuedaArribaOSeaConLaPalmaMirandoAlCentro() {
        // Con la mano parada hay dos formas de ponerla: palma hacia el centro
        // del cuerpo (pulgar arriba) o hacia afuera (pulgar abajo). La que se
        // quiere es la primera, y el pulgar es justo lo que las distingue.
        val (_, _, minY, maxY) = cajaDeLaPalma().toList()
        assertTrue(
            "el pulgar tendria que asomar por arriba de la palma " +
                "(arriba llega a $maxY, abajo a $minY)",
            maxY > -minY * 1.2f
        )
    }

    @Test
    fun elArmaEquipadaSeAgregaALaMallaYVaAdelanteDeLaMano() {
        val sinArma = ArmsMesh.build()
        val conArma = ArmsMesh.build(ArmsMesh.Arma.GARROTE)
        assertTrue(
            "equipar un arma no agrego geometria",
            conArma.vertices.size > sinArma.vertices.size
        )

        // Todo lo marcado como arma tiene que estar delante de la mano, que es
        // donde se agarra: si quedara detras, se veria salir de la camara.
        var vertices = 0
        var zMin = Float.MAX_VALUE
        var zMax = -Float.MAX_VALUE
        var i = 0
        while (i < conArma.vertices.size) {
            if (conArma.vertices[i + 6] > 1.5f) {
                vertices++
                val z = conArma.vertices[i + 2]
                if (z < zMin) zMin = z
                if (z > zMax) zMax = z
            }
            i += ArmsMesh.STRIDE_FLOATS
        }
        assertTrue("el arma no quedo marcada con su propio tag", vertices > 50)
        assertTrue("el arma asoma demasiado por detras de la mano (z=$zMax)", zMax < 0.10f)
        assertTrue("el arma no sale para adelante (z=$zMin)", zMin < -0.35f)
    }
}
