package com.mggx.laberinto

import com.mggx.laberinto.gl.WorldMesh
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La cueva redondeada subdivide cada cara, asi que la malla crecio mucho.
 * Este test es el freno: si alguna vez se sube el detalle sin pensar, un
 * telefono modesto se queda sin memoria y el juego se cierra solo, que es la
 * clase de error que no aparece hasta que esta en el celular de alguien.
 */
class MallaTamanoTest {

    private fun megas(bytes: Long): Float = bytes / (1024f * 1024f)

    @Test
    fun laMallaDeUnNivelGrandeEntraEnUnTelefonoModesto() {
        // Se prueban los niveles mas grandes que arma el generador.
        for (level in intArrayOf(40, 60, 90, 120)) {
            val bp = MazeGenerator.generate(level, level * 4441L)
            val mesh = WorldMesh.build(bp.maze)
            val vertices = mesh.vertices.size / WorldMesh.STRIDE_FLOATS
            val bytes = mesh.vertices.size.toLong() * 4L + mesh.indices.size.toLong() * 4L
            assertTrue(
                "el nivel $level genera ${megas(bytes)} MB de malla, es demasiado",
                megas(bytes) < 40f
            )
            assertTrue("el nivel $level no genero nada", vertices > 1000)
            assertTrue(
                "el nivel $level pasa el limite de indices de 32 bits con holgura",
                vertices < 8_000_000
            )
        }
    }

    @Test
    fun laMallaCubreTodasLasCasillasAbiertas() {
        // Cada casilla abierta tiene por lo menos su piso y su techo, asi que
        // si faltara geometria (por ejemplo por una capacidad que se llena) el
        // conteo se caeria enseguida.
        val bp = MazeGenerator.generate(12, 1212L)
        val abiertas = bp.maze.solid.count { !it }
        val mesh = WorldMesh.build(bp.maze)
        assertTrue(
            "hay menos triangulos que los del piso y el techo de cada casilla",
            mesh.triangleCount >= abiertas * 4
        )
    }

    @Test
    fun elRelieveSeVeEnLaMalla() {
        // Si la malla ignorara la altura de cada casilla, todos los vertices de
        // piso estarian a la misma altura y no se veria ningun escalon.
        val bp = MazeGenerator.generate(30, 3030L)
        val mesh = WorldMesh.build(bp.maze)
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var i = 0
        while (i < mesh.vertices.size) {
            val y = mesh.vertices[i + 1]
            if (y < minY) minY = y
            if (y > maxY) maxY = y
            i += WorldMesh.STRIDE_FLOATS
        }
        assertTrue(
            "la malla no tiene relieve vertical (de $minY a $maxY)",
            maxY - minY > 4f
        )
    }
}
