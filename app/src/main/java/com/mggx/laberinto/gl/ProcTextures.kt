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

    /** Capas de sedimento por baldosa. Entero: si no, la banda se corta. */
    private const val ESTRATOS_POR_BALDOSA = 4.0

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
    /**
     * Distancia entre la celda de Worley mas cercana y la segunda. Cerca de
     * cero justo en el borde entre dos celdas, asi que sirve para dibujar las
     * juntas entre placas de roca.
     */
    private fun worleyEdge(x: Float, y: Float, period: Int, seed: Int): Float {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        var f1 = 10f
        var f2 = 10f
        for (dy in -1..1) for (dx in -1..1) {
            val cx = xi + dx
            val cy = yi + dy
            fun w(a: Int) = ((a % period) + period) % period
            val px = cx + hash2(w(cx), w(cy), seed)
            val py = cy + hash2(w(cx), w(cy), seed + 7717)
            val d = sqrt((px - x) * (px - x) + (py - y) * (py - y))
            if (d < f1) { f2 = f1; f1 = d } else if (d < f2) f2 = d
        }
        return (f2 - f1).coerceIn(0f, 1f)
    }

    /**
     * Periodo entero derivado del base.
     *
     * La textura solo cierra sin costura si la ESCALA de la coordenada y el
     * periodo de repeticion son EL MISMO ENTERO. Escribir `u * per * 1.7f` con
     * periodo `(per * 1.7f).toInt()` deja los dos desfasados y aparece una
     * linea marcada en el borde de cada baldosa, o sea cada 3 metros de pared.
     */
    private fun periodo(per: Int, factor: Float): Int =
        Math.round(per * factor).coerceAtLeast(2)

    private fun smoothstep(borde0: Float, borde1: Float, x: Float): Float {
        val t = ((x - borde0) / (borde1 - borde0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Estira el rango alrededor del medio: sube el contraste sin desplazar. */
    private fun contraste(h: Float, cuanto: Float): Float =
        (0.5f + (h - 0.5f) * cuanto).coerceIn(0f, 1f)

    /**
     * Grietas: ruido "ridged" recortado en la cresta, que deja fisuras finas y
     * ramificadas en vez de manchones.
     */
    private fun crackMask(u: Float, v: Float, per: Int, seed: Int): Float {
        val p = periodo(per, 0.85f)
        val n = fbm(u * p, v * p, 3, p, seed + 907)
        val ridged = 1f - abs(n * 2f - 1f)
        return ((ridged - 0.90f) / 0.10f).coerceIn(0f, 1f)
    }

    private fun height(layer: Int, u: Float, v: Float, per: Int, seed: Int, rough: Float): Float {
        return when (layer) {
            LAYER_WALL -> {
                val base = fbm(u * per, v * per, 5, per, seed)
                // Bloques grandes de roca y, encima, grano mas chico.
                val pPlacas = periodo(per, 0.5f)
                val cells = 1f - worley(u * pPlacas, v * pPlacas, pPlacas, seed + 31)
                val pGrano = periodo(per, 1.7f)
                val grain = 1f - worley(u * pGrano, v * pGrano, pGrano, seed + 61)

                // Estratos de sedimento: bandas horizontales onduladas, con la
                // curva suavizada para que se lean como capas y no como olas.
                // Tienen que ser un numero ENTERO de ciclos por baldosa, si no
                // la banda queda cortada justo en la union.
                val onda = 0.5f + 0.5f * kotlin.math.sin(
                    v * ESTRATOS_POR_BALDOSA * 2.0 * Math.PI +
                        fbm(u * 3f, v * 3f, 2, 3, seed + 13) * 11.0
                ).toFloat()
                val strat = smoothstep(0.18f, 0.82f, onda)

                var h = base * 0.36f + cells * 0.20f + grain * 0.14f + strat * 0.30f
                // Junta entre placas: una linea hundida justo en el borde de
                // cada celda, que es lo que le da canto a la piedra.
                val junta = 1f - smoothstep(
                    0f, 0.062f, worleyEdge(u * pPlacas, v * pPlacas, pPlacas, seed + 31)
                )
                h -= junta * 0.24f
                // Grietas: ruido "ridged" muy fino que hunde la superficie.
                h -= crackMask(u, v, per, seed) * 0.30f
                contraste(h.coerceIn(0f, 1f), 0.90f + rough * 0.80f)
            }
            LAYER_FLOOR -> {
                val pBase = periodo(per, 1.2f)
                val base = fbm(u * pBase, v * pBase, 4, pBase, seed + 5)
                // Dos tamanos de canto rodado: piedras grandes y ripio.
                val pebbles = 1f - worley(u * per, v * per, per, seed + 77)
                val pRipio = periodo(per, 2.3f)
                val ripio = 1f - worley(u * pRipio, v * pRipio, pRipio, seed + 131)
                var h = base * 0.46f + pebbles * 0.34f + ripio * 0.20f
                h -= crackMask(u, v, per, seed + 400) * 0.18f
                contraste(h.coerceIn(0f, 1f), 1.20f)
            }
            LAYER_CEIL -> {
                val pBase = periodo(per, 0.8f)
                val base = fbm(u * pBase, v * pBase, 5, pBase, seed + 11)
                val pGotas = periodo(per, 0.7f)
                val drips = worley(u * pGotas, v * pGotas, pGotas, seed + 143)
                contraste((base * 0.7f + (1f - drips) * 0.3f).coerceIn(0f, 1f), 1.15f)
            }
            else -> {
                val pVeta = periodo(per, 1.6f)
                fbm(u * pVeta, v * pVeta, 4, pVeta, seed + 23).coerceIn(0f, 1f)
            }
        }
    }

    /** Mascara de veta de mineral: filamentos finos siguiendo el ruido. */
    private fun veinMask(u: Float, v: Float, per: Int, seed: Int): Float {
        val p = periodo(per, 0.6f)
        val n = fbm(u * p, v * p, 4, p, seed + 900)
        val ridged = 1f - abs(n * 2f - 1f)
        // Umbral alto y curva cubica: quedan hilos finos y separados, no una
        // telarana que tapa toda la roca.
        val m = ((ridged - 0.945f) / 0.055f).coerceIn(0f, 1f)
        return m * m * m
    }

    // ------------------------------------------------------------ subida

    class Result(val albedoTex: Int, val normalTex: Int, val size: Int)

    /** Pixeles ya calculados, listos para subir. Sin nada de OpenGL adentro. */
    class Pixels(val size: Int, val albedo: ByteArray, val normal: ByteArray)

    /** Genera y sube las dos texturas array. Se llama en el hilo de GL. */
    fun build(theme: CaveTheme, quality: Int): Result = upload(generate(theme, quality))

    /**
     * Calcula los pixeles de las dos texturas. Es CPU pura, sin OpenGL: se
     * puede correr fuera del hilo de GL y se puede mirar en un test.
     */
    fun generate(theme: CaveTheme, quality: Int): Pixels {
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

        val albedo = ByteArray(size * size * 4 * LAYERS)
        val normal = ByteArray(size * size * 4 * LAYERS)
        var ai = 0
        var ni = 0

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
                    val shade = 0.46f + hv * 0.92f
                    val grain = (hash2(x, y, seed + 3) - 0.5f) * 0.06f
                    val vm = if (layer == LAYER_VEIN) 0.85f else veinMask(u, v, per, seed)
                    var r = cr * shade + grain
                    var g = cg * shade + grain
                    var b = cb * shade + grain
                    // La veta tine apenas el albedo de alrededor
                    r = r * (1f - vm * 0.28f) + theme.veinR * vm * 0.28f
                    g = g * (1f - vm * 0.28f) + theme.veinG * vm * 0.28f
                    b = b * (1f - vm * 0.28f) + theme.veinB * vm * 0.28f
                    albedo[ai++] = toByte(r); albedo[ai++] = toByte(g)
                    albedo[ai++] = toByte(b); albedo[ai++] = toByte(vm)
                }
            }

            // 3) mapa de normales por diferencias centrales (con envolvente)
            val strength = when (layer) {
                LAYER_WALL -> 3.3f
                LAYER_FLOOR -> 2.1f
                LAYER_CEIL -> 2.7f
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
                    normal[ni++] = toByte(nx * 0.5f + 0.5f)
                    normal[ni++] = toByte(ny * 0.5f + 0.5f)
                    normal[ni++] = toByte(nz * 0.5f + 0.5f)
                    normal[ni++] = toByte(h[y * size + x])   // altura: la usa el especular
                }
            }
        }

        return Pixels(size, albedo, normal)
    }

    /** Sube los pixeles ya calculados a dos GL_TEXTURE_2D_ARRAY. */
    private fun upload(p: Pixels): Result {
        val ids = IntArray(2)
        GLES30.glGenTextures(2, ids, 0)
        uploadArray(ids[0], p.size, directBuffer(p.albedo))
        uploadArray(ids[1], p.size, directBuffer(p.normal))
        return Result(ids[0], ids[1], p.size)
    }

    private fun directBuffer(data: ByteArray): ByteBuffer =
        ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder()).apply {
            put(data); position(0)
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
