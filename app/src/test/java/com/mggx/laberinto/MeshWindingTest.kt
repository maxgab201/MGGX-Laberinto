package com.mggx.laberinto

import com.mggx.laberinto.gl.ArmsMesh
import com.mggx.laberinto.gl.EnemyMeshes
import com.mggx.laberinto.gl.PropMeshes
import com.mggx.laberinto.gl.WorldMesh
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * Verifica que las caras miren para donde dicen mirar.
 *
 * OpenGL descarta las caras traseras: si el orden de los vertices de un
 * triangulo no coincide con su normal, esa cara se vuelve invisible y se ve
 * "a traves" de la pared. Es un error que no da ningun sintoma al compilar,
 * asi que se controla aca.
 */
class MeshWindingTest {

    private fun cross(
        ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float
    ) = floatArrayOf(ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx)

    /**
     * Recorre los triangulos y compara la normal geometrica (por el orden de
     * los vertices) con la normal declarada. Devuelve cuantos no coinciden.
     */
    private fun contarInvertidos(
        vertices: FloatArray, indices: IntArray, stride: Int, normalOffset: Int
    ): Int {
        var malas = 0
        var i = 0
        while (i + 2 < indices.size) {
            val i0 = indices[i] * stride
            val i1 = indices[i + 1] * stride
            val i2 = indices[i + 2] * stride

            val ax = vertices[i1] - vertices[i0]
            val ay = vertices[i1 + 1] - vertices[i0 + 1]
            val az = vertices[i1 + 2] - vertices[i0 + 2]
            val bx = vertices[i2] - vertices[i0]
            val by = vertices[i2 + 1] - vertices[i0 + 1]
            val bz = vertices[i2 + 2] - vertices[i0 + 2]

            val g = cross(ax, ay, az, bx, by, bz)
            val len = sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2])
            if (len < 1e-9f) { i += 3; continue }   // triangulo degenerado

            // Normal declarada: promedio de los tres vertices
            var nx = 0f; var ny = 0f; var nz = 0f
            for (k in intArrayOf(i0, i1, i2)) {
                nx += vertices[k + normalOffset]
                ny += vertices[k + normalOffset + 1]
                nz += vertices[k + normalOffset + 2]
            }
            val nlen = sqrt(nx * nx + ny * ny + nz * nz)
            if (nlen < 1e-6f) { i += 3; continue }

            val dot = (g[0] / len) * (nx / nlen) + (g[1] / len) * (ny / nlen) + (g[2] / len) * (nz / nlen)
            if (dot < 0.05f) malas++
            i += 3
        }
        return malas
    }

    @Test
    fun lasParedesDelLaberintoMiranParaAdentro() {
        for (level in intArrayOf(1, 4, 12, 30, 55)) {
            val maze = MazeGenerator.generate(level, level * 991L).maze
            val mesh = WorldMesh.build(maze)
            assertTrue("la malla del nivel $level esta vacia", mesh.triangleCount > 100)
            val malas = contarInvertidos(mesh.vertices, mesh.indices, WorldMesh.STRIDE_FLOATS, 3)
            assertTrue(
                "el nivel $level tiene $malas triangulos dados vuelta de ${mesh.triangleCount}",
                malas == 0
            )
        }
    }

    @Test
    fun losBrazosMiranParaAfuera() {
        val mesh = ArmsMesh.build()
        assertTrue(mesh.indices.size > 300)
        val malas = contarInvertidos(mesh.vertices, mesh.indices, ArmsMesh.STRIDE_FLOATS, 3)
        assertTrue("los brazos tienen $malas triangulos dados vuelta", malas == 0)
    }

    @Test
    fun losObjetosDeLaCuevaMiranParaAfuera() {
        val formas = mapOf(
            "octaedro" to PropMeshes.octahedron(1.5f),
            "cono" to PropMeshes.cone(8, 1f, 0.4f, false),
            "cono invertido" to PropMeshes.cone(8, 1f, 0.4f, true),
            "cilindro" to PropMeshes.cylinder(8, 1f, 0.2f),
            "caja" to PropMeshes.box(1f, 1f, 1f),
            "flecha" to PropMeshes.arrow()
        )
        for ((nombre, g) in formas) {
            val malas = contarInvertidos(g.vertices, g.indices, 6, 3)
            assertTrue("$nombre tiene $malas triangulos dados vuelta", malas == 0)
        }
    }

    @Test
    fun losBichosMiranParaAfuera() {
        // Cada bicho tiene malla propia, armada con lathe/extruir/combinar. Un
        // error de orientacion ahi no da ningun sintoma al compilar: el bicho
        // simplemente se ve del reves o transparente en el telefono.
        val formas = mapOf(
            "cuerpo de murcielago" to EnemyMeshes.murcielagoCuerpo(),
            "ala de murcielago" to EnemyMeshes.murcielagoAla(),
            "segmento de rastrero" to EnemyMeshes.rastreroSegmento(),
            "pata de rastrero" to EnemyMeshes.rastreroPata(),
            "torso de guardian" to EnemyMeshes.guardianTorso(),
            "cabeza de guardian" to EnemyMeshes.guardianCabeza(),
            "brazo de guardian" to EnemyMeshes.guardianBrazo()
        )
        for ((nombre, g) in formas) {
            assertTrue("$nombre esta vacio", g.indices.size > 30)
            val malas = contarInvertidos(g.vertices, g.indices, 6, 3)
            assertTrue("$nombre tiene $malas triangulos dados vuelta", malas == 0)
        }
    }

    @Test
    fun lasHerramientasDeModeladoNoDanVueltaLasCaras() {
        // Las transformaciones son la base de todos los modelos nuevos: si
        // alguna invirtiera la orientacion, romperia todo lo que se arme
        // encima y seria dificil de rastrear.
        val base = PropMeshes.lathe(
            arrayOf(
                floatArrayOf(0f, -0.5f), floatArrayOf(0.4f, -0.2f),
                floatArrayOf(0.5f, 0.2f), floatArrayOf(0f, 0.5f)
            ),
            segmentos = 8
        )
        val casos = mapOf(
            "lathe" to base,
            "trasladar" to PropMeshes.trasladar(base, 1f, -2f, 0.5f),
            "escalar" to PropMeshes.escalar(base, 0.3f, 2f, 1.4f),
            "rotarX" to PropMeshes.rotarX(base, 37f),
            "rotarY" to PropMeshes.rotarY(base, -110f),
            "rotarZ" to PropMeshes.rotarZ(base, 64f),
            "combinar" to PropMeshes.combinar(base, PropMeshes.trasladar(base, 2f, 0f, 0f)),
            "extruir" to PropMeshes.extruir(
                arrayOf(
                    floatArrayOf(0f, 0f), floatArrayOf(1f, 0.3f),
                    floatArrayOf(0.8f, -0.4f), floatArrayOf(0.2f, -0.5f)
                ),
                0.1f
            )
        )
        for ((nombre, g) in casos) {
            val malas = contarInvertidos(g.vertices, g.indices, 6, 3)
            assertTrue("$nombre da vuelta $malas triangulos", malas == 0)
        }
    }

    @Test
    fun laMallaNoTieneNiIndicesFueraDeRangoNiValoresRotos() {
        val maze = MazeGenerator.generate(9, 4242L).maze
        val mesh = WorldMesh.build(maze)
        val vertices = mesh.vertices.size / WorldMesh.STRIDE_FLOATS
        for (i in mesh.indices) {
            assertTrue("indice fuera de rango: $i de $vertices", i in 0 until vertices)
        }
        for (v in mesh.vertices) {
            assertTrue("hay un valor invalido en la malla", !v.isNaN() && !v.isInfinite())
        }
        // Las capas de textura son 0 (pared), 1 (suelo) o 2 (techo)
        var i = 9
        while (i < mesh.vertices.size) {
            val capa = mesh.vertices[i].toInt()
            assertTrue("capa de textura desconocida: $capa", capa in 0..2)
            i += WorldMesh.STRIDE_FLOATS
        }
    }
}
