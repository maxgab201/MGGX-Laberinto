package com.mggx.laberinto.gl

import com.mggx.laberinto.maze.AguaDeLaCueva
import com.mggx.laberinto.maze.Maze

/**
 * El espejo de agua de la cueva: un plano horizontal, a la altura de la napa,
 * sobre cada casilla inundada.
 *
 * Va en su propia malla y no adentro de [WorldMesh] a proposito. El agua se
 * dibuja DESPUES de todo lo opaco, mezclada por transparencia y sin escribir
 * profundidad: si viajara en la malla del mundo habria que ordenar triangulos
 * transparentes contra opacos dentro del mismo dibujo, que es exactamente el
 * problema que se evita separando las dos pasadas.
 *
 * Formato de vertice: pos(3) + hondura(1) = 4 floats. La hondura (cuantos
 * metros de agua hay debajo de ese punto) es lo que le permite al shader teñir
 * mas fuerte el fondo de los pozos y dejar el borde casi transparente, que es
 * lo que hace que un charco se lea como un charco y no como un vidrio azul.
 */
object WaterMesh {

    /** floats por vertice: pos(3) hondura(1) */
    const val STRIDE_FLOATS = 4
    const val STRIDE_BYTES = STRIDE_FLOATS * 4

    class Mesh(val vertices: FloatArray, val indices: IntArray, val triangleCount: Int)

    val VACIA = Mesh(FloatArray(0), IntArray(0), 0)

    /**
     * Cuantas veces se parte cada casilla de agua.
     *
     * El agua es plana, asi que la geometria no necesita divisiones para la
     * forma: las necesita para que la ONDA del shader (que se calcula por
     * vertice donde conviene y por pixel donde importa) y la iluminacion
     * especular no se vean en escalones. Con 2 alcanza y son 8 triangulos por
     * casilla.
     */
    private const val DIVISIONES = 2

    /**
     * Cuanto se hunde el espejo respecto de la napa teorica, en metros.
     *
     * Sin esto, el agua queda EXACTAMENTE a la altura del piso de las casillas
     * que apenas se inundan, y las dos superficies pelean por el mismo pixel:
     * aparecen manchas que parpadean al mover la camara (z-fighting). Bajarlo
     * un milimetro escaso lo resuelve y no se nota.
     */
    private const val HUNDIDO = 0.004f

    fun build(maze: Maze): Mesh {
        val nivel = maze.waterY
        if (nivel == AguaDeLaCueva.SIN_AGUA) return VACIA

        val v = FloatList(4096)
        val idx = IntList(4096)
        var n = 0
        val cell = com.mggx.laberinto.game.GameSession.CELL
        val d = DIVISIONES
        val y = nivel - HUNDIDO

        for (gy in 0 until maze.gh) {
            for (gx in 0 until maze.gw) {
                if (!AguaDeLaCueva.hayAgua(maze, nivel, gx, gy)) continue
                val hondura = (nivel - maze.floorY(gx, gy)).coerceAtLeast(0f)
                val x0 = gx * cell
                val z0 = gy * cell
                val base = n

                for (j in 0..d) {
                    for (i in 0..d) {
                        val x = x0 + cell * i / d
                        val z = z0 + cell * j / d
                        v.add(x, y, z, hondura)
                        n++
                    }
                }
                // Visto desde arriba (la normal es +Y), el anillo horario en
                // pantalla es el orden antihorario en el plano XZ.
                for (j in 0 until d) {
                    for (i in 0 until d) {
                        val a = base + j * (d + 1) + i
                        val b = base + j * (d + 1) + i + 1
                        val c = base + (j + 1) * (d + 1) + i + 1
                        val e = base + (j + 1) * (d + 1) + i
                        idx.add(a); idx.add(e); idx.add(c)
                        idx.add(a); idx.add(c); idx.add(b)
                    }
                }
            }
        }

        val indices = idx.toArray()
        return Mesh(v.toArray(), indices, indices.size / 3)
    }

    /** Cuantas casillas quedaron bajo el agua. Lo mira el test. */
    fun casillasInundadas(maze: Maze): Int {
        val nivel = maze.waterY
        if (nivel == AguaDeLaCueva.SIN_AGUA) return 0
        var n = 0
        for (gy in 0 until maze.gh) {
            for (gx in 0 until maze.gw) {
                if (AguaDeLaCueva.hayAgua(maze, nivel, gx, gy)) n++
            }
        }
        return n
    }
}
