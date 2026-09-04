package com.mggx.laberinto.gl

import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.maze.Maze
import kotlin.math.PI
import kotlin.math.sin

/**
 * Convierte el laberinto en una malla unica de cueva.
 *
 * En vez de cajas perfectas, cada esquina de la grilla tiene una altura propia
 * derivada de un hash determinista: el suelo ondula, el techo baja y sube y las
 * paredes acompañan. Como las alturas se calculan por ESQUINA (no por cara),
 * las caras vecinas encajan siempre y no quedan grietas.
 */
object WorldMesh {

    /** floats por vertice: pos(3) normal(3) uv(2) ao(1) layer(1) */
    const val STRIDE_FLOATS = 10
    const val STRIDE_BYTES = STRIDE_FLOATS * 4

    private const val CELL = GameSession.CELL
    private const val WALL_H = GameSession.WALL_HEIGHT
    private const val TEX_SCALE = 3.0f

    class Mesh(val vertices: FloatArray, val indices: IntArray, val triangleCount: Int)

    private fun hash(x: Int, y: Int, seed: Int): Float {
        var h = x * 374761393 + y * 668265263 + seed * 1442695041
        h = (h xor (h shr 13)) * 1274126177
        h = h xor (h shr 16)
        return (h and 0x7FFFFFFF) / 2147483647.0f
    }

    /** Altura del suelo en la esquina de grilla (cx, cy). */
    fun floorHeight(cx: Int, cy: Int): Float =
        -0.16f * hash(cx, cy, 17) - 0.03f * hash(cx, cy, 91)

    /** Altura del techo en la esquina de grilla (cx, cy). */
    fun ceilHeight(cx: Int, cy: Int): Float =
        WALL_H + 0.62f * hash(cx, cy, 53) - 0.22f

    /** Oclusion ambiental de una esquina: cuenta la roca que la rodea. */
    private fun cornerAo(maze: Maze, cx: Int, cy: Int): Float {
        var solid = 0
        if (maze.isSolid(cx - 1, cy - 1)) solid++
        if (maze.isSolid(cx, cy - 1)) solid++
        if (maze.isSolid(cx - 1, cy)) solid++
        if (maze.isSolid(cx, cy)) solid++
        return (1f - 0.155f * solid).coerceIn(0.42f, 1f)
    }

    /** Sombreado vertical: mas oscuro pegado al suelo y al techo. */
    private fun verticalAo(t: Float): Float = 0.58f + 0.42f * sin(PI * t.coerceIn(0f, 1f)).toFloat()

    fun build(maze: Maze): Mesh {
        val v = FloatList(1 shl 16)
        val idx = IntList(1 shl 15)
        var vertCount = 0

        fun push(
            x: Float, y: Float, z: Float,
            nx: Float, ny: Float, nz: Float,
            u: Float, vv: Float, ao: Float, layer: Float
        ): Int {
            v.add(x, y, z, nx, ny, nz, u, vv, ao, layer)
            return vertCount++
        }

        fun quad(a: Int, b: Int, c: Int, d: Int) {
            idx.add(a); idx.add(b); idx.add(c)
            idx.add(a); idx.add(c); idx.add(d)
        }

        for (gy in 0 until maze.gh) {
            for (gx in 0 until maze.gw) {
                if (maze.isSolid(gx, gy)) continue

                val x0 = gx * CELL
                val x1 = (gx + 1) * CELL
                val z0 = gy * CELL
                val z1 = (gy + 1) * CELL

                val f00 = floorHeight(gx, gy)
                val f10 = floorHeight(gx + 1, gy)
                val f11 = floorHeight(gx + 1, gy + 1)
                val f01 = floorHeight(gx, gy + 1)
                val c00 = ceilHeight(gx, gy)
                val c10 = ceilHeight(gx + 1, gy)
                val c11 = ceilHeight(gx + 1, gy + 1)
                val c01 = ceilHeight(gx, gy + 1)

                val a00 = cornerAo(maze, gx, gy)
                val a10 = cornerAo(maze, gx + 1, gy)
                val a11 = cornerAo(maze, gx + 1, gy + 1)
                val a01 = cornerAo(maze, gx, gy + 1)

                val u0 = x0 / TEX_SCALE
                val u1 = x1 / TEX_SCALE
                val w0 = z0 / TEX_SCALE
                val w1 = z1 / TEX_SCALE

                // --------------------------------------------------- suelo
                run {
                    val i0 = push(x0, f00, z0, 0f, 1f, 0f, u0, w0, a00, 1f)
                    val i1 = push(x1, f10, z0, 0f, 1f, 0f, u1, w0, a10, 1f)
                    val i2 = push(x1, f11, z1, 0f, 1f, 0f, u1, w1, a11, 1f)
                    val i3 = push(x0, f01, z1, 0f, 1f, 0f, u0, w1, a01, 1f)
                    quad(i0, i1, i2, i3)
                }

                // --------------------------------------------------- techo
                run {
                    val t = 0.82f
                    val i0 = push(x0, c00, z0, 0f, -1f, 0f, u0, w0, a00 * t, 2f)
                    val i1 = push(x0, c01, z1, 0f, -1f, 0f, u0, w1, a01 * t, 2f)
                    val i2 = push(x1, c11, z1, 0f, -1f, 0f, u1, w1, a11 * t, 2f)
                    val i3 = push(x1, c10, z0, 0f, -1f, 0f, u1, w0, a10 * t, 2f)
                    quad(i0, i1, i2, i3)
                }

                // -------------------------------------------------- paredes
                // Cara -X (roca a la izquierda)
                if (maze.isSolid(gx - 1, gy)) {
                    val fb = f00; val fbz = f01
                    val ct = c00; val ctz = c01
                    val aB = a00; val aT = a01
                    val i0 = push(x0, fb, z0, 1f, 0f, 0f, w0, fb / TEX_SCALE, aB * verticalAo(0f), 0f)
                    val i1 = push(x0, fbz, z1, 1f, 0f, 0f, w1, fbz / TEX_SCALE, aT * verticalAo(0f), 0f)
                    val i2 = push(x0, ctz, z1, 1f, 0f, 0f, w1, ctz / TEX_SCALE, aT * verticalAo(1f), 0f)
                    val i3 = push(x0, ct, z0, 1f, 0f, 0f, w0, ct / TEX_SCALE, aB * verticalAo(1f), 0f)
                    quad(i0, i1, i2, i3)
                }
                // Cara +X
                if (maze.isSolid(gx + 1, gy)) {
                    val i0 = push(x1, f10, z0, -1f, 0f, 0f, w0, f10 / TEX_SCALE, a10 * verticalAo(0f), 0f)
                    val i1 = push(x1, c10, z0, -1f, 0f, 0f, w0, c10 / TEX_SCALE, a10 * verticalAo(1f), 0f)
                    val i2 = push(x1, c11, z1, -1f, 0f, 0f, w1, c11 / TEX_SCALE, a11 * verticalAo(1f), 0f)
                    val i3 = push(x1, f11, z1, -1f, 0f, 0f, w1, f11 / TEX_SCALE, a11 * verticalAo(0f), 0f)
                    quad(i0, i1, i2, i3)
                }
                // Cara -Z
                if (maze.isSolid(gx, gy - 1)) {
                    val i0 = push(x0, f00, z0, 0f, 0f, 1f, u0, f00 / TEX_SCALE, a00 * verticalAo(0f), 0f)
                    val i1 = push(x0, c00, z0, 0f, 0f, 1f, u0, c00 / TEX_SCALE, a00 * verticalAo(1f), 0f)
                    val i2 = push(x1, c10, z0, 0f, 0f, 1f, u1, c10 / TEX_SCALE, a10 * verticalAo(1f), 0f)
                    val i3 = push(x1, f10, z0, 0f, 0f, 1f, u1, f10 / TEX_SCALE, a10 * verticalAo(0f), 0f)
                    quad(i0, i1, i2, i3)
                }
                // Cara +Z
                if (maze.isSolid(gx, gy + 1)) {
                    val i0 = push(x0, f01, z1, 0f, 0f, -1f, u0, f01 / TEX_SCALE, a01 * verticalAo(0f), 0f)
                    val i1 = push(x1, f11, z1, 0f, 0f, -1f, u1, f11 / TEX_SCALE, a11 * verticalAo(0f), 0f)
                    val i2 = push(x1, c11, z1, 0f, 0f, -1f, u1, c11 / TEX_SCALE, a11 * verticalAo(1f), 0f)
                    val i3 = push(x0, c01, z1, 0f, 0f, -1f, u0, c01 / TEX_SCALE, a01 * verticalAo(1f), 0f)
                    quad(i0, i1, i2, i3)
                }
            }
        }

        val indices = idx.toArray()
        return Mesh(v.toArray(), indices, indices.size / 3)
    }
}
