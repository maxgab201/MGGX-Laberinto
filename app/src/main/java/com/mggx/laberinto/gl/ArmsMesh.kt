package com.mggx.laberinto.gl

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Brazos en primera persona generados por codigo: antebrazo conico, mano y
 * cinco dedos con dos falanges cada uno. El brazo se construye una vez mirando
 * hacia -Z y se duplica en espejo; el atributo aSide (-1 / +1) le dice al shader
 * que matriz usar.
 *
 * Formato de vertice: pos(3) normal(3) side(1) = 7 floats.
 */
object ArmsMesh {

    const val STRIDE_FLOATS = 7
    const val STRIDE_BYTES = STRIDE_FLOATS * 4

    /**
     * Coordenada X local del dedo mas cercano al centro del cuerpo (el
     * indice, primer elemento de `fingerX` en [buildArm]) y del pulgar.
     * Expuestas aparte para poder testear, sin reconstruir la malla entera,
     * que el pulgar quede de ESE lado y no del lado del menique.
     */
    const val INDICE_X = -0.034f
    const val PULGAR_X = -0.050f

    class Mesh(val vertices: FloatArray, val indices: IntArray)

    private class Builder {
        val v = FloatList(8192)
        val idx = IntList(8192)
        var n = 0

        fun vertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, side: Float): Int {
            v.add(x, y, z, nx, ny, nz, side)
            return n++
        }

        fun quad(a: Int, b: Int, c: Int, d: Int, flip: Boolean) {
            if (flip) {
                idx.add(a); idx.add(c); idx.add(b)
                idx.add(a); idx.add(d); idx.add(c)
            } else {
                idx.add(a); idx.add(b); idx.add(c)
                idx.add(a); idx.add(c); idx.add(d)
            }
        }
    }

    /** Caja orientada: centro, medidas y giro alrededor de X (para curvar los dedos). */
    private fun box(
        b: Builder, side: Float,
        cx: Float, cy: Float, cz: Float,
        hx: Float, hy: Float, hz: Float,
        pitch: Float
    ) {
        val cp = cos(pitch.toDouble()).toFloat()
        val sp = sin(pitch.toDouble()).toFloat()
        fun tx(x: Float, y: Float, z: Float): FloatArray {
            val ry = y * cp - z * sp
            val rz = y * sp + z * cp
            // El centro tambien se refleja: si no, el pulgar del brazo
            // izquierdo termina del lado equivocado de la mano.
            return floatArrayOf((cx + x) * side, cy + ry, cz + rz)
        }
        fun tn(x: Float, y: Float, z: Float): FloatArray {
            val ry = y * cp - z * sp
            val rz = y * sp + z * cp
            return floatArrayOf(x * side, ry, rz)
        }
        val g = DetailMeshes.roundedBox(hx * 2f, hy * 2f, hz * 2f, minOf(hx,hy,hz) * 0.65f)
        val offset = b.n
        for (i in g.vertices.indices step 6) {
            val p = tx(g.vertices[i], g.vertices[i+1], g.vertices[i+2])
            val n = tn(g.vertices[i+3], g.vertices[i+4], g.vertices[i+5])
            b.vertex(p[0],p[1],p[2],n[0],n[1],n[2],side)
        }
        for (i in g.indices.indices step 3) {
            b.idx.add(offset + g.indices[i])
            b.idx.add(offset + g.indices[i + if (side < 0f) 2 else 1])
            b.idx.add(offset + g.indices[i + if (side < 0f) 1 else 2])
        }
    }

    /** Tubo conico a lo largo de Z, de z0 a z1 con radios r0 y r1. */
    private fun taperedTube(
        b: Builder, side: Float,
        cx: Float, cy: Float,
        z0: Float, z1: Float, r0: Float, r1: Float,
        segments: Int = 10, flatten: Float = 0.78f
    ) {
        // El anillo se recorre en sentido contrario al de las cajas, asi que
        // aca la condicion del giro va al reves (lo verifica MeshWindingTest).
        val flip = side > 0f
        for (i in 0 until segments) {
            val a0 = i * 2.0 * PI / segments
            val a1 = (i + 1) * 2.0 * PI / segments
            val c0 = cos(a0).toFloat(); val s0 = sin(a0).toFloat()
            val c1 = cos(a1).toFloat(); val s1 = sin(a1).toFloat()

            fun p(cc: Float, ss: Float, z: Float, r: Float): FloatArray =
                floatArrayOf((cx + cc * r) * side, cy + ss * r * flatten, z)
            fun nrm(cc: Float, ss: Float): FloatArray {
                val nx = cc; val ny = ss / flatten
                val nz = -(r1 - r0) / (z1 - z0)
                val l = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
                return floatArrayOf(nx / l * side, ny / l, nz / l)
            }

            val n0 = nrm(c0, s0); val n1 = nrm(c1, s1)
            val v0 = p(c0, s0, z0, r0); val v1 = p(c1, s1, z0, r0)
            val v2 = p(c1, s1, z1, r1); val v3 = p(c0, s0, z1, r1)
            val i0 = b.vertex(v0[0], v0[1], v0[2], n0[0], n0[1], n0[2], side)
            val i1 = b.vertex(v1[0], v1[1], v1[2], n1[0], n1[1], n1[2], side)
            val i2 = b.vertex(v2[0], v2[1], v2[2], n1[0], n1[1], n1[2], side)
            val i3 = b.vertex(v3[0], v3[1], v3[2], n0[0], n0[1], n0[2], side)
            b.quad(i0, i1, i2, i3, flip)
        }
    }

    private fun buildArm(b: Builder, side: Float) {
        // Antebrazo: entra desde atras de la camara y se afina hacia la muneca.
        taperedTube(b, side, 0f, 0f, 0.62f, 0.05f, 0.074f, 0.047f, 14, 0.92f)
        // Puno del guante: un anillo mas grueso que marca donde termina la tela.
        taperedTube(b, side, 0f, 0f, 0.13f, 0.042f, 0.062f, 0.055f, 14, 0.94f)
        // Palma, un poco mas angosta en la muneca que en los nudillos.
        box(b, side, 0f, 0f, -0.075f, 0.046f, 0.021f, 0.055f, 0.02f)
        box(b, side, 0.002f, 0.002f, -0.128f, 0.050f, 0.020f, 0.014f, 0.05f)

        // Cuatro dedos separados, cada uno con dos falanges y su propia curva.
        val fingerX = floatArrayOf(INDICE_X, -0.0115f, 0.0115f, 0.034f)
        val l1 = floatArrayOf(0.052f, 0.060f, 0.056f, 0.043f)
        val curl = floatArrayOf(0.34f, 0.26f, 0.30f, 0.42f)
        for (i in 0 until 4) {
            val x = fingerX[i]
            val a = l1[i]
            val bLen = a * 0.80f
            // Falange proximal
            box(b, side, x, 0.001f, -0.142f - a * 0.5f, 0.0092f, 0.0105f, a * 0.5f, curl[i] * 0.40f)
            // Falange distal, mas curvada hacia abajo
            box(
                b, side, x, 0.001f - a * 0.22f, -0.142f - a - bLen * 0.42f,
                0.0086f, 0.0096f, bLen * 0.5f, curl[i] * 1.20f
            )
        }

        // Pulgar: sale del costado de la palma y apunta hacia adentro (mismo
        // lado que fingerX[0], el dedo mas cercano al centro del cuerpo).
        // Con signo positivo quedaba del lado del menique: la mano se leia
        // invertida en las dos manos por igual, porque buildArm() se dibuja
        // una sola vez y despues se espeja con `side`.
        box(b, side, PULGAR_X, -0.010f, -0.072f, 0.0125f, 0.0125f, 0.032f, 0.12f)
        box(b, side, PULGAR_X - 0.006f, -0.019f, -0.124f, 0.0108f, 0.0108f, 0.027f, 0.50f)
    }

    fun build(): Mesh {
        val b = Builder()
        buildArm(b, -1f)   // izquierdo
        buildArm(b, 1f)    // derecho
        return Mesh(b.v.toArray(), b.idx.toArray())
    }
}

