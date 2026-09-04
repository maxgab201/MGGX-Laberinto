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
            return floatArrayOf(cx + x * side, cy + ry, cz + rz)
        }
        fun tn(x: Float, y: Float, z: Float): FloatArray {
            val ry = y * cp - z * sp
            val rz = y * sp + z * cp
            return floatArrayOf(x * side, ry, rz)
        }
        val corners = arrayOf(
            floatArrayOf(-hx, -hy, -hz), floatArrayOf(hx, -hy, -hz),
            floatArrayOf(hx, hy, -hz), floatArrayOf(-hx, hy, -hz),
            floatArrayOf(-hx, -hy, hz), floatArrayOf(hx, -hy, hz),
            floatArrayOf(hx, hy, hz), floatArrayOf(-hx, hy, hz)
        )
        val faces = arrayOf(
            intArrayOf(4, 5, 6, 7) to floatArrayOf(0f, 0f, 1f),
            intArrayOf(1, 0, 3, 2) to floatArrayOf(0f, 0f, -1f),
            intArrayOf(5, 1, 2, 6) to floatArrayOf(1f, 0f, 0f),
            intArrayOf(0, 4, 7, 3) to floatArrayOf(-1f, 0f, 0f),
            intArrayOf(3, 7, 6, 2) to floatArrayOf(0f, 1f, 0f),
            intArrayOf(0, 1, 5, 4) to floatArrayOf(0f, -1f, 0f)
        )
        val flip = side < 0f
        for ((f, nrm) in faces) {
            val nn = tn(nrm[0], nrm[1], nrm[2])
            val ids = IntArray(4)
            for (i in 0 until 4) {
                val c = corners[f[i]]
                val p = tx(c[0], c[1], c[2])
                ids[i] = b.vertex(p[0], p[1], p[2], nn[0], nn[1], nn[2], side)
            }
            b.quad(ids[0], ids[1], ids[2], ids[3], flip)
        }
    }

    /** Tubo conico a lo largo de Z, de z0 a z1 con radios r0 y r1. */
    private fun taperedTube(
        b: Builder, side: Float,
        cx: Float, cy: Float,
        z0: Float, z1: Float, r0: Float, r1: Float,
        segments: Int = 10, flatten: Float = 0.78f
    ) {
        val flip = side < 0f
        for (i in 0 until segments) {
            val a0 = i * 2.0 * PI / segments
            val a1 = (i + 1) * 2.0 * PI / segments
            val c0 = cos(a0).toFloat(); val s0 = sin(a0).toFloat()
            val c1 = cos(a1).toFloat(); val s1 = sin(a1).toFloat()

            fun p(cc: Float, ss: Float, z: Float, r: Float): FloatArray =
                floatArrayOf((cx + cc * r) * side, cy + ss * r * flatten, z)
            fun nrm(cc: Float, ss: Float): FloatArray {
                val nx = cc; val ny = ss / flatten
                val l = sqrt(nx * nx + ny * ny).coerceAtLeast(1e-5f)
                return floatArrayOf(nx / l * side, ny / l, 0f)
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
        taperedTube(b, side, 0f, 0f, 0.62f, 0.02f, 0.088f, 0.055f, 12, 0.80f)
        // Puno del guante (un anillo mas grueso)
        taperedTube(b, side, 0f, 0f, 0.10f, 0.03f, 0.072f, 0.066f, 12, 0.85f)
        // Palma
        box(b, side, 0f, 0f, -0.085f, 0.052f, 0.026f, 0.075f, 0f)
        // Nudillos
        box(b, side, 0f, 0.006f, -0.158f, 0.050f, 0.024f, 0.014f, 0f)

        // Cuatro dedos, cada uno con dos falanges y curvatura creciente.
        val fingerX = floatArrayOf(-0.033f, -0.011f, 0.011f, 0.033f)
        val fingerLen = floatArrayOf(0.048f, 0.056f, 0.052f, 0.040f)
        val curl = floatArrayOf(0.30f, 0.24f, 0.28f, 0.38f)
        for (i in 0 until 4) {
            val x = fingerX[i]
            val l1 = fingerLen[i]
            val l2 = l1 * 0.78f
            // Falange proximal
            box(b, side, x, -0.004f, -0.172f - l1 * 0.5f, 0.0105f, 0.0115f, l1 * 0.5f, curl[i] * 0.45f)
            // Falange distal, mas curvada
            val z2 = -0.172f - l1 - l2 * 0.45f
            box(b, side, x, -0.004f - l1 * 0.20f, z2, 0.0098f, 0.0105f, l2 * 0.5f, curl[i] * 1.15f)
        }

        // Pulgar: sale del costado y apunta hacia adentro.
        box(b, side, 0.055f, -0.012f, -0.086f, 0.014f, 0.014f, 0.036f, 0.15f)
        box(b, side, 0.062f, -0.020f, -0.145f, 0.012f, 0.012f, 0.030f, 0.55f)
    }

    fun build(): Mesh {
        val b = Builder()
        buildArm(b, -1f)   // izquierdo
        buildArm(b, 1f)    // derecho
        return Mesh(b.v.toArray(), b.idx.toArray())
    }
}
