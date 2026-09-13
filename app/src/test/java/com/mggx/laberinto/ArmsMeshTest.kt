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
        //
        // Antes esto se media solo como "se estira mas en Y que en Z". Servia
        // mientras el arma unicamente se levantaba, pero se rompio al empezar a
        // LADEARLA (que es lo que la saco de verse como un mastil en pantalla,
        // ver ArmaEnPantallaTest): ladeada, parte del largo se va en X y el
        // alto en Y baja sin que el agarre tenga nada de malo. Lo que importa
        // es que cruce el antebrazo, vaya para arriba o para el costado.
        for (arma in ArmsMesh.Arma.entries) {
            val v = verticesDelArma(arma)
            val ancho = v.maxOf { it[0] } - v.minOf { it[0] }
            val alto = v.maxOf { it[1] } - v.minOf { it[1] }
            val largo = v.maxOf { it[2] } - v.minOf { it[2] }
            assertTrue(
                "${arma.name} se estira mas a lo largo del antebrazo que cruzandolo " +
                    "(cruza ${maxOf(ancho, alto)}, a lo largo $largo): sigue atravesando los dedos",
                maxOf(ancho, alto) > largo
            )
            val maxY = v.maxOf { it[1] }
            val maxZ = v.maxOf { it[2] }
            val minZ = v.minOf { it[2] }
            assertTrue("${arma.name} no levanta la cabeza (maxY=$maxY)", maxY > 0.2f)
            // El puno esta en z=-0.11 y el antebrazo se va hacia z=+0.62. Un
            // par de centimetros del extremo del mango metidos ahi quedan
            // tapados por el propio antebrazo; lo que no puede es asomar de
            // verdad por atras de la mano, hacia la cara del jugador.
            assertTrue("${arma.name} sale para atras de la mano (maxZ=$maxZ)", maxZ < 0.06f)
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
        // El mango tiene que ocupar el hueco del puno: el espacio entre la
        // palma (z = -0.075) y las falanges (z = -0.17), o sea el punto
        // (0, 0, -0.11). Si no hubiera mango ahi, el arma estaria flotando al
        // lado de la mano en vez de agarrada.
        //
        // Se mide contra las ARISTAS de la malla, no contra los vertices. Un
        // tubo solo tiene vertices en los anillos de las puntas, que pueden
        // estar a diez o quince centimetros uno de otro: preguntar por el
        // vertice mas cercano da numeros enormes aunque el palo pase justo por
        // el medio del puno. La arista si esta ahi.
        //
        // (La version anterior de este test interpolaba entre los vertices de
        // arriba y de abajo filtrando por |x| chico. Dejo de funcionar al
        // ladear el arma, porque el palo ahora tambien se corre en X mientras
        // sube y arriba del puno ya no queda ningun vertice centrado.)
        for (arma in ArmsMesh.Arma.entries) {
            val d = distanciaALaMalla(ArmsMesh.build(arma), { it > 1.5f }, 0f, 0f, -0.11f)
            assertTrue(
                "${arma.name} no tiene mango en el puno: lo mas cerca que pasa es a $d m",
                d < 0.045f
            )
        }
    }

    /** Distancia de un punto a la arista mas cercana de las piezas marcadas. */
    private fun distanciaALaMalla(
        mesh: ArmsMesh.Mesh, tagOk: (Float) -> Boolean, px: Float, py: Float, pz: Float
    ): Float {
        val st = ArmsMesh.STRIDE_FLOATS
        var mejor = Float.MAX_VALUE

        fun aSegmento(ai: Int, bi: Int) {
            val ax = mesh.vertices[ai * st]; val ay = mesh.vertices[ai * st + 1]; val az = mesh.vertices[ai * st + 2]
            val bx = mesh.vertices[bi * st]; val by = mesh.vertices[bi * st + 1]; val bz = mesh.vertices[bi * st + 2]
            val ux = bx - ax; val uy = by - ay; val uz = bz - az
            val len2 = ux * ux + uy * uy + uz * uz
            val t = if (len2 < 1e-12f) 0f
            else (((px - ax) * ux + (py - ay) * uy + (pz - az) * uz) / len2).coerceIn(0f, 1f)
            val dx = px - (ax + ux * t); val dy = py - (ay + uy * t); val dz = pz - (az + uz * t)
            val d = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            if (d < mejor) mejor = d
        }

        var i = 0
        while (i + 2 < mesh.indices.size) {
            val a = mesh.indices[i]; val b = mesh.indices[i + 1]; val c = mesh.indices[i + 2]
            i += 3
            if (!tagOk(mesh.vertices[a * st + 6])) continue
            aSegmento(a, b); aSegmento(b, c); aSegmento(c, a)
        }
        return mejor
    }
}
