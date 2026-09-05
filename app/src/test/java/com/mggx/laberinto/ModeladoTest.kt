package com.mggx.laberinto

import com.mggx.laberinto.gl.EnemyMeshes
import com.mggx.laberinto.gl.PropMeshes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Las herramientas con las que se modelan los bichos y las estructuras.
 *
 * Son la base de todos los modelos nuevos, asi que un error aca se multiplica
 * por todos lados y encima es invisible: una malla mal armada compila igual y
 * recien se ve en el telefono.
 */
class ModeladoTest {

    private fun areaDelContorno(p: Array<FloatArray>): Float {
        var a = 0f
        for (k in p.indices) {
            val q = p[(k + 1) % p.size]
            a += p[k][0] * q[1] - q[0] * p[k][1]
        }
        return abs(a) * 0.5f
    }

    /** Suma del area de los triangulos de una tapa (los que miran a +Z). */
    private fun areaDeLaTapa(g: PropMeshes.Geometry): Float {
        var total = 0f
        var i = 0
        while (i < g.indices.size) {
            val a = g.indices[i] * 6; val b = g.indices[i + 1] * 6; val c = g.indices[i + 2] * 6
            // Solo la tapa de adelante: normal (0,0,1)
            if (g.vertices[a + 5] > 0.99f && g.vertices[a + 3] == 0f) {
                val ux = g.vertices[b] - g.vertices[a]
                val uy = g.vertices[b + 1] - g.vertices[a + 1]
                val vx = g.vertices[c] - g.vertices[a]
                val vy = g.vertices[c + 1] - g.vertices[a + 1]
                total += abs(ux * vy - uy * vx) * 0.5f
            }
            i += 3
        }
        return total
    }

    @Test
    fun elTrianguladorCubreTodoElContornoYNoSeSaltaNada() {
        // El recorte de orejas puede quedarse sin orejas y abandonar en la
        // mitad. Si eso pasara, la figura saldria con un pedazo faltante y
        // nadie se enteraria: por eso se compara area contra area.
        val contornos = mapOf(
            "cuadrado" to arrayOf(
                floatArrayOf(0f, 0f), floatArrayOf(1f, 0f),
                floatArrayOf(1f, 1f), floatArrayOf(0f, 1f)
            ),
            "concavo en L" to arrayOf(
                floatArrayOf(0f, 0f), floatArrayOf(2f, 0f), floatArrayOf(2f, 1f),
                floatArrayOf(1f, 1f), floatArrayOf(1f, 2f), floatArrayOf(0f, 2f)
            ),
            "ala con dedos" to arrayOf(
                floatArrayOf(0.00f, 0.00f), floatArrayOf(0.30f, 0.16f),
                floatArrayOf(0.66f, 0.24f), floatArrayOf(1.00f, 0.20f),
                floatArrayOf(0.86f, 0.02f), floatArrayOf(0.74f, 0.10f),
                floatArrayOf(0.58f, -0.06f), floatArrayOf(0.46f, 0.04f),
                floatArrayOf(0.30f, -0.12f), floatArrayOf(0.18f, -0.02f),
                floatArrayOf(0.06f, -0.14f)
            )
        )
        for ((nombre, c) in contornos) {
            val esperada = areaDelContorno(c)
            val obtenida = areaDeLaTapa(PropMeshes.extruir(c, 0.1f))
            assertEquals(
                "la tapa de '$nombre' no cubre todo el contorno",
                esperada, obtenida, esperada * 0.001f
            )
        }
    }

    @Test
    fun elContornoAlRevesDaLaMismaFigura() {
        // extruir() normaliza el sentido, asi que escribirlo al reves no puede
        // cambiar nada.
        val c = arrayOf(
            floatArrayOf(0f, 0f), floatArrayOf(1f, 0.3f),
            floatArrayOf(0.8f, -0.4f), floatArrayOf(0.2f, -0.5f)
        )
        val a = PropMeshes.extruir(c, 0.1f)
        val b = PropMeshes.extruir(c.reversedArray(), 0.1f)
        assertEquals(a.indices.size, b.indices.size)
        assertEquals(areaDeLaTapa(a), areaDeLaTapa(b), 1e-5f)
    }

    @Test
    fun escalarNoAdmiteFactoresQueDenVueltaLaFigura() {
        // Un factor negativo espeja, y espejar da vuelta todas las caras: la
        // figura se veria del reves. Mejor que reviente aca y no en pantalla.
        val g = PropMeshes.box(1f, 1f, 1f)
        for (malo in listOf(
            Triple(-1f, 1f, 1f), Triple(1f, -1f, 1f), Triple(1f, 1f, 0f)
        )) {
            var tiro = false
            try { PropMeshes.escalar(g, malo.first, malo.second, malo.third) }
            catch (_: IllegalArgumentException) { tiro = true }
            assertTrue("escalar acepto el factor invalido $malo", tiro)
        }
    }

    @Test
    fun cadaBichoEsUnSoloModeloDeUnTamanoRazonable() {
        // Todos los modelos se disenan de mas o menos 1 unidad en su lado
        // largo: asi el `scale` que les pasa el renderer se lee directo como
        // metros y no hay que adivinar.
        val modelos = mapOf(
            "cuerpo de murcielago" to EnemyMeshes.murcielagoCuerpo(),
            "ala de murcielago" to EnemyMeshes.murcielagoAla(),
            "segmento de rastrero" to EnemyMeshes.rastreroSegmento(),
            "pata de rastrero" to EnemyMeshes.rastreroPata(),
            "torso de guardian" to EnemyMeshes.guardianTorso(),
            "cabeza de guardian" to EnemyMeshes.guardianCabeza(),
            "brazo de guardian" to EnemyMeshes.guardianBrazo()
        )
        for ((nombre, g) in modelos) {
            var lado = 0f
            for (eje in 0..2) {
                var min = Float.MAX_VALUE; var max = -Float.MAX_VALUE
                var i = eje
                while (i < g.vertices.size) {
                    val v = g.vertices[i]
                    assertTrue("$nombre tiene un vertice invalido", !v.isNaN() && !v.isInfinite())
                    if (v < min) min = v
                    if (v > max) max = v
                    i += 6
                }
                if (max - min > lado) lado = max - min
            }
            assertTrue("$nombre mide $lado, se fue de escala", lado in 0.5f..1.6f)
        }
    }
}
