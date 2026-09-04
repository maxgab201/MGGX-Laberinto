package com.mggx.laberinto.gl

import android.opengl.GLES30
import com.mggx.laberinto.maze.CaveTheme
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Texturas de cueva generadas por codigo. No hay ni un solo archivo de imagen:
 * la roca, el suelo, el techo y las vetas salen de ruido fractal calculado al vuelo.
 * Se generan 4 capas en un GL_TEXTURE_2D_ARRAY (albedo con mascara de veta en alfa,
 * mas su mapa de normales derivado del mismo campo de altura).
 */
object ProcTextures {

    const val LAYER_WALL = 0
    const val LAYER_FLOOR = 1
    const val LAYER_CEIL = 2
    const val LAYER_VEIN = 3
    const val LAYERS = 4

    // -------------------------------------------------------------- ruido

    private fun hash2(x: Int, y: Int, seed: Int): Float {
        var h = x * 374761393 + y * 668265263 + seed * 1274126177
        h = (h xor (h shr 13)) * 1274126177
        h = h xor (h shr 16)
        return (h and 0x7FFFFFFF) / 2147483647.0f
    }

    private fun smooth(t: Float) = t * t * (3f - 2f * t)

    /** Ruido de valor con repeticion exacta en [period] para que la textura sea continua. */
    private fun valueNoise(x: Float, y: Float, period: Int, seed: Int): Float {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        val xf = x - xi
        val yf = y - yi
        fun w(a: Int) = ((a % period) + period) % period
        val x0 = w(xi); val x1 = w(xi + 1)
        val y0 = w(yi); val y1 = w(yi + 1)
        val v00 = hash2(x0, y0, seed)
        val v10 = hash2(x1, y0, seed)
        val v01 = hash2(x0, y1, seed)
        val v11 = hash2(x1, y1, seed)
        val sx = smooth(xf)
        val sy = smooth(yf)
        val a = v00 + (v10 - v00) * sx
        val b = v01 + (v11 - v01) * sx
        return a + (b - a) * sy
    }

    private fun fbm(x: Float, y: Float, octaves: Int, basePeriod: Int, seed: Int): Float {
        var sum = 0f
        var amp = 0.5f
        var norm = 0f
        var freq = 1
        for (o in 0 until octaves) {
            sum += valueNoise(x * freq, y * freq, basePeriod * freq, seed + o * 101) * amp
            norm += amp
            amp *= 0.5f
            freq *= 2
        }
        return sum / norm
    }

    /** Ruido celular tipo Worley: da el aspecto de piedra picada. */
    private fun worley(x: Float, y: Float, period: Int, seed: Int): Float {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        var best = 10f
        for (dy in -1..1) for (dx in -1..1) {
            val cx = xi + dx
            val cy = yi + dy
            fun w(a: Int) = ((a % period) + period) % period
            val px = cx + hash2(w(cx), w(cy), seed)
            val py = cy + hash2(w(cx), w(cy), seed + 7717)
            val d = sqrt((px - x) * (px - x) + (py - y) * (py - y))
            if (d < best) best = d
        }
        return min(1f, best)
    }

    // ----------------------------------------------------------- campos

    /** Altura 0..1 de cada capa. De aca sale tambien el mapa de normales. */
    private fun height(layer: Int, u: Float, v: Float, per: Int, seed: Int, rough: Float): Float {
        return when (layer) {
            LAYER_WALL -> {
                val base = fbm(u * per, v * per, 5, per, seed)
                val cells = 1f - worley(u * per * 0.5f, v * per * 0.5f, per / 2, seed + 31)
                val strat = 0.5f + 0.5f * kotlin.math.sin((v * 26f + fbm(u * 4f, v * 4f, 2, 4, seed) * 5f).toDouble()).toFloat()
                (base * 0.55f + cells * 0.30f + strat * 0.15f).coerceIn(0f, 1f) * rough + (1f - rough) * 0.5f
            }
            LAYER_FLOOR -> {
                val base = fbm(u * per * 1.2f, v * per * 1.2f, 4, (per * 1.2f).toInt().coerceAtLeast(2), seed + 5)
                val pebbles = 1f - worley(u * per, v * per, per, seed + 77)
                (base * 0.6f + pebbles * 0.4f).coerceIn(0f, 1f)
            }
            LAYER_CEIL -> {
                val base = fbm(u * per * 0.8f, v * per * 0.8f, 5, (per * 0.8f).toInt().coerceAtLeast(2), seed + 11)
                val drips = worley(u * per * 0.7f, v * per * 0.7f, (per * 0.7f).toInt().coerceAtLeast(2), seed + 143)
                (base * 0.7f + (1f - drips) * 0.3f).coerceIn(0f, 1f)
            }
            else -> {
                val f = fbm(u * per * 1.6f, v * per * 1.6f, 4, (per * 1.6f).toInt().coerceAtLeast(2), seed + 23)
                f.coerceIn(0f, 1f)
            }
        }
    }

    /** Mascara de veta de mineral: filamentos finos siguiendo el ruido. */
    private fun veinMask(u: Float, v: Float, per: Int, seed: Int): Float {
        val n = fbm(u * per * 0.6f, v * per * 0.6f, 4, (per * 0.6f).toInt().coerceAtLeast(2), seed + 900)
        val ridged = 1f - abs(n * 2f - 1f)
        val m = ((ridged - 0.86f) / 0.14f).coerceIn(0f, 1f)
        return m * m
    }

    // ------------------------------------------------------------ subida

    class Result(val albedoTex: Int, val normalTex: Int, val size: Int)

    /**
     * Genera y sube las dos texturas array. Se llama en el hilo de GL,
     * durante la pantalla de carga.
     */
    fun build(theme: CaveTheme, quality: Int): Result {
        val size = when (quality) {
            0 -> 96
            1 -> 160
            2 -> 256
            else -> 384
        }
        val per = when (quality) {
            0 -> 6
            1 -> 8
            2 -> 12
            else -> 16
        }
        val seed = theme.textureSeed * 7919

        val albedo = ByteBuffer.allocateDirect(size * size * 4 * LAYERS).order(ByteOrder.nativeOrder())
        val normal = ByteBuffer.allocateDirect(size * size * 4 * LAYERS).order(ByteOrder.nativeOrder())

        val h = FloatArray(size * size)

        for (layer in 0 until LAYERS) {
            // 1) campo de altura de la capa
            for (y in 0 until size) {
                val v = y.toFloat() / size
                for (x in 0 until size) {
                    val u = x.toFloat() / size
                    h[y * size + x] = height(layer, u, v, per, seed, theme.roughness)
                }
            }

            // 2) albedo tenido por el tema
            val (cr, cg, cb) = when (layer) {
                LAYER_WALL -> Triple(theme.rockR, theme.rockG, theme.rockB)
                LAYER_FLOOR -> Triple(theme.floorR, theme.floorG, theme.floorB)
                LAYER_CEIL -> Triple(theme.rockR * 0.78f, theme.rockG * 0.78f, theme.rockB * 0.8f)
                else -> Triple(theme.veinR * 0.5f, theme.veinG * 0.5f, theme.veinB * 0.5f)
            }
            for (y in 0 until size) {
                val v = y.toFloat() / size
                for (x in 0 until size) {
                    val u = x.toFloat() / size
                    val hv = h[y * size + x]
                    // Variacion de tono ligada a la altura: hondo = mas oscuro
                    val shade = 0.55f + hv * 0.75f
                    val grain = (hash2(x, y, seed + 3) - 0.5f) * 0.06f
                    val vm = if (layer == LAYER_VEIN) 0.85f else veinMask(u, v, per, seed)
                    var r = cr * shade + grain
                    var g = cg * shade + grain
                    var b = cb * shade + grain
                    // La veta tine ligeramente el albedo alrededor
                    r = r * (1f - vm * 0.5f) + theme.veinR * vm * 0.5f
                    g = g * (1f - vm * 0.5f) + theme.veinG * vm * 0.5f
                    b = b * (1f - vm * 0.5f) + theme.veinB * vm * 0.5f
                    albedo.put(toByte(r)); albedo.put(toByte(g)); albedo.put(toByte(b)); albedo.put(toByte(vm))
                }
            }

            // 3) mapa de normales por diferencias centrales (con envolvente)
            val strength = when (layer) {
                LAYER_WALL -> 2.6f
                LAYER_FLOOR -> 1.7f
                LAYER_CEIL -> 2.2f
                else -> 1.2f
            }
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val xm = (x - 1 + size) % size
                    val xp = (x + 1) % size
                    val ym = (y - 1 + size) % size
                    val yp = (y + 1) % size
                    val dx = (h[y * size + xp] - h[y * size + xm]) * strength
                    val dy = (h[yp * size + x] - h[ym * size + x]) * strength
                    var nx = -dx
                    var ny = -dy
                    var nz = 1f
                    val len = sqrt(nx * nx + ny * ny + nz * nz)
                    nx /= len; ny /= len; nz /= len
                    normal.put(toByte(nx * 0.5f + 0.5f))
                    normal.put(toByte(ny * 0.5f + 0.5f))
                    normal.put(toByte(nz * 0.5f + 0.5f))
                    normal.put(toByte(h[y * size + x]))   // altura, por si sirve despues
                }
            }
        }

        albedo.position(0)
        normal.position(0)

        val ids = IntArray(2)
        GLES30.glGenTextures(2, ids, 0)
        uploadArray(ids[0], size, albedo)
        uploadArray(ids[1], size, normal)
        return Result(ids[0], ids[1], size)
    }

    private fun uploadArray(tex: Int, size: Int, data: ByteBuffer) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY, tex)
        val levels = max(1, (Math.log(size.toDouble()) / Math.log(2.0)).toInt() + 1)
        GLES30.glTexStorage3D(GLES30.GL_TEXTURE_2D_ARRAY, levels, GLES30.GL_RGBA8, size, size, LAYERS)
        GLES30.glTexSubImage3D(
            GLES30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0,
            size, size, LAYERS, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, data
        )
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D_ARRAY)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
        GLUtil.checkError("uploadArray")
    }

    private fun toByte(f: Float): Byte {
        val v = (f.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (v and 0xFF).toByte()
    }
}
