package com.mggx.laberinto

import com.mggx.laberinto.gl.ArmsMesh
import org.junit.Assert.assertNotNull
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

    /** Los vertices marcados como arma (tag [ArmsMesh.TAG_ARMA]). */
    private fun verticesDelArma(arma: ArmsMesh.Arma): List<FloatArray> {
        val mesh = ArmsMesh.build(arma)
        val out = ArrayList<FloatArray>()
        var i = 0
        while (i < mesh.vertices.size) {
            if (mesh.vertices[i + 6] > 1.5f) {
                out.add(
                    floatArrayOf(mesh.vertices[i], mesh.vertices[i + 1], mesh.vertices[i + 2])
                )
            }
            i += ArmsMesh.STRIDE_FLOATS
        }
        return out
    }

    @Test
    fun elArmaEquipadaSeAgregaALaMalla() {
        val sinArma = ArmsMesh.build()
        for (arma in ArmsMesh.Arma.entries) {
            val conArma = ArmsMesh.build(arma)
            assertTrue(
                "equipar ${arma.name} no agrego geometria",
                conArma.vertices.size > sinArma.vertices.size
            )
            assertTrue(
                "${arma.name} no quedo marcada con su propio tag",
                verticesDelArma(arma).size > 50
            )
        }
    }

    @Test
    fun elArmaVaAgarradaEnElPunoYNoCruzandoLosDedos() {
        // El bug que motiva este test: el arma se modelaba a lo largo de -Z,
        // o sea paralela al antebrazo, saliendo derecho para adelante. Pero
        // los dedos se cierran a lo ANCHO de la palma, y con la mano ya
        // parada ese tunel quedo en VERTICAL: un palo horizontal no esta
        // agarrado, esta atravesando los dedos.
        for (arma in ArmsMesh.Arma.entries) {
            val v = verticesDelArma(arma)
            val maxY = v.maxOf { it[1] }
            val minY = v.minOf { it[1] }
            val minZ = v.minOf { it[2] }
            val maxZ = v.maxOf { it[2] }
            assertTrue(
                "${arma.name} se estira mas para adelante que para arriba " +
                    "(alto ${maxY - minY}, largo ${maxZ - minZ}): sigue cruzada",
                (maxY - minY) > (maxZ - minZ)
            )
            assertTrue("${arma.name} no levanta la cabeza (maxY=$maxY)", maxY > 0.2f)
            assertTrue("${arma.name} sale para atras de la mano (maxZ=$maxZ)", maxZ < 0.02f)
            assertTrue("${arma.name} no asoma para adelante (minZ=$minZ)", minZ < -0.15f)
        }
    }

    @Test
    fun elMangoAsomaPorAbajoDelPuno() {
        // Un pico agarrado tiene un cacho de mango que sobra por abajo de la
        // mano. Sin eso el arma parece pegada a los nudillos.
        for (arma in ArmsMesh.Arma.entries) {
            val minY = verticesDelArma(arma).minOf { it[1] }
            assertTrue(
                "${arma.name} no tiene mango abajo del puno (minY=$minY)",
                minY < -0.05f
            )
        }
    }

    @Test
    fun elMangoPasaPorDondeSeCierranLosDedos() {
        // El eje del mango tiene que cruzar el hueco del puno: el espacio
        // entre la palma (z = -0.075) y las falanges (z = -0.17), o sea
        // z ~ -0.11 a la altura de la muneca (y = 0). Si no pasara por ahi,
        // el arma quedaria flotando al lado de la mano en vez de agarrada.
        //
        // El mango no tiene vertices JUSTO en y=0 (los tubos solo ponen
        // anillos en las puntas), asi que se mide donde cruza: se toman los
        // vertices del palo (los que estan sobre el eje, |x| chico), el mas
        // cercano por abajo y el mas cercano por arriba, y se interpola.
        for (arma in ArmsMesh.Arma.entries) {
            val palo = verticesDelArma(arma).filter { kotlin.math.abs(it[0]) < 0.05f }
            val abajo = palo.filter { it[1] < 0f }.maxByOrNull { it[1] }
            val arriba = palo.filter { it[1] >= 0f }.minByOrNull { it[1] }
            assertNotNull("${arma.name} no tiene mango por abajo del puno", abajo)
            assertNotNull("${arma.name} no tiene mango por arriba del puno", arriba)
            val a = abajo!!; val b = arriba!!
            val t = (0f - a[1]) / (b[1] - a[1])
            val z = a[2] + t * (b[2] - a[2])
            assertTrue(
                "${arma.name} no cruza el puno: a la altura de la muneca el " +
                    "mango esta en z=$z, y el hueco del puno esta en -0.11",
                kotlin.math.abs(z - (-0.11f)) < 0.04f
            )
        }
    }
}
