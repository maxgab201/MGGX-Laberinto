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

    /**
     * Caja orientada: centro, medidas, giro alrededor de X (para curvar los
     * dedos) y giro alrededor de Z (para poner la mano vertical).
     *
     * [tag] es lo que se guarda en el atributo `aSide` del vertice. Por
     * defecto es el propio lado, que es lo que usa el shader para elegir la
     * matriz del brazo; el arma lo usa para marcarse como material aparte
     * ([TAG_ARMA]) sin dejar de viajar con la mano derecha.
     */
    private fun box(
        b: Builder, side: Float,
        cx: Float, cy: Float, cz: Float,
        hx: Float, hy: Float, hz: Float,
        pitch: Float,
        roll: Float = 0f,
        tag: Float = side
    ) {
        val cp = cos(pitch.toDouble()).toFloat()
        val sp = sin(pitch.toDouble()).toFloat()
        val cr = cos(roll.toDouble()).toFloat()
        val sr = sin(roll.toDouble()).toFloat()
        fun tx(x: Float, y: Float, z: Float): FloatArray {
            val ry = y * cp - z * sp
            val rz = y * sp + z * cp
            // El roll gira la pieza YA UBICADA alrededor del eje del antebrazo,
            // asi que la mano entera (palma, dedos y pulgar) gira junta en vez
            // de girar cada pieza sobre si misma.
            val px = cx + x
            val py = cy + ry
            // El centro tambien se refleja: si no, el pulgar del brazo
            // izquierdo termina del lado equivocado de la mano.
            return floatArrayOf((px * cr - py * sr) * side, px * sr + py * cr, cz + rz)
        }
        fun tn(x: Float, y: Float, z: Float): FloatArray {
            val ry = y * cp - z * sp
            val rz = y * sp + z * cp
            return floatArrayOf((x * cr - ry * sr) * side, x * sr + ry * cr, rz)
        }
        val g = DetailMeshes.roundedBox(hx * 2f, hy * 2f, hz * 2f, minOf(hx,hy,hz) * 0.65f)
        val offset = b.n
        for (i in g.vertices.indices step 6) {
            val p = tx(g.vertices[i], g.vertices[i+1], g.vertices[i+2])
            val n = tn(g.vertices[i+3], g.vertices[i+4], g.vertices[i+5])
            b.vertex(p[0],p[1],p[2],n[0],n[1],n[2],tag)
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
        segments: Int = 10, flatten: Float = 0.78f,
        tag: Float = side
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
            val i0 = b.vertex(v0[0], v0[1], v0[2], n0[0], n0[1], n0[2], tag)
            val i1 = b.vertex(v1[0], v1[1], v1[2], n1[0], n1[1], n1[2], tag)
            val i2 = b.vertex(v2[0], v2[1], v2[2], n1[0], n1[1], n1[2], tag)
            val i3 = b.vertex(v3[0], v3[1], v3[2], n0[0], n0[1], n0[2], tag)
            b.quad(i0, i1, i2, i3, flip)
        }
    }

    /**
     * Cuanto se gira la mano alrededor del eje del antebrazo, en radianes.
     *
     * La mano se modela plana (palma mirando para abajo, que es como se
     * dibujaba antes) y despues se para con este giro. Un cuarto de vuelta
     * NEGATIVO deja la palma mirando hacia el centro del cuerpo, que es como
     * uno lleva la mano cuando camina o cuando agarra algo; en positivo
     * quedaba mirando para afuera. Como `buildArm` se dibuja una vez y se
     * espeja con `side`, el mismo valor sirve para las dos manos: al
     * espejarse, la palma sigue apuntando al medio.
     */
    private val ROLL_MANO = (-PI / 2.0).toFloat()

    /** Marca de vertice del arma: viaja con la mano derecha pero es otro material. */
    const val TAG_ARMA = 2f

    private fun buildArm(b: Builder, side: Float) {
        // Antebrazo: entra desde atras de la camara y se afina hacia la muneca.
        taperedTube(b, side, 0f, 0f, 0.62f, 0.05f, 0.074f, 0.047f, 14, 0.92f)
        // Puno del guante: un anillo mas grueso que marca donde termina la tela.
        taperedTube(b, side, 0f, 0f, 0.13f, 0.042f, 0.062f, 0.055f, 14, 0.94f)
        val r = ROLL_MANO
        // Palma, un poco mas angosta en la muneca que en los nudillos.
        box(b, side, 0f, 0f, -0.075f, 0.046f, 0.021f, 0.055f, 0.02f, r)
        box(b, side, 0.002f, 0.002f, -0.128f, 0.050f, 0.020f, 0.014f, 0.05f, r)

        // Cuatro dedos separados, cada uno con dos falanges y su propia curva.
        // Estan repartidos a lo ancho de la palma; con la mano ya parada, eso
        // los deja apilados en vertical, el indice arriba y el menique abajo.
        val fingerX = floatArrayOf(INDICE_X, -0.0115f, 0.0115f, 0.034f)
        val l1 = floatArrayOf(0.052f, 0.060f, 0.056f, 0.043f)
        val curl = floatArrayOf(0.34f, 0.26f, 0.30f, 0.42f)
        for (i in 0 until 4) {
            val x = fingerX[i]
            val a = l1[i]
            val bLen = a * 0.80f
            // Falange proximal
            box(b, side, x, 0.001f, -0.142f - a * 0.5f, 0.0092f, 0.0105f, a * 0.5f, curl[i] * 0.40f, r)
            // Falange distal, mas curvada hacia la palma
            box(
                b, side, x, 0.001f - a * 0.22f, -0.142f - a - bLen * 0.42f,
                0.0086f, 0.0096f, bLen * 0.5f, curl[i] * 1.20f, r
            )
        }

        // Pulgar: sale del costado de la palma y apunta hacia adentro (mismo
        // lado que fingerX[0], el dedo mas cercano al centro del cuerpo).
        // Con signo positivo quedaba del lado del menique: la mano se leia
        // invertida en las dos manos por igual, porque buildArm() se dibuja
        // una sola vez y despues se espeja con `side`.
        box(b, side, PULGAR_X, -0.010f, -0.072f, 0.0125f, 0.0125f, 0.032f, 0.12f, r)
        box(b, side, PULGAR_X - 0.006f, -0.019f, -0.124f, 0.0108f, 0.0108f, 0.027f, 0.50f, r)
    }

    // ------------------------------------------------------------------ armas

    /**
     * Lo que se ve en la mano derecha segun con que estes peleando.
     *
     * Va en la MISMA malla que los brazos, marcado con [TAG_ARMA]: asi viaja
     * con la matriz de la mano derecha sin ningun trabajo extra, incluido el
     * envion del golpe, y el shader lo pinta con su propio material.
     */
    enum class Arma(val idTienda: String) {
        GARROTE("arma_garrote"),
        PICO("arma_pico"),
        AGUIJON("arma_aguijon"),
        MAZA("arma_maza"),
        HACHA("arma_hacha");

        companion object {
            fun por(id: String): Arma? = entries.firstOrNull { it.idTienda == id }
        }
    }

    /**
     * El arma agarrada en el puno, saliendo hacia adelante (-Z).
     *
     * El mango arranca un poco por detras de la mano (asoma por abajo del
     * puno, como cuando se agarra algo de verdad) y la cabeza va en la punta.
     */
    private fun buildArma(b: Builder, arma: Arma) {
        val t = TAG_ARMA
        val s = 1f       // siempre en la mano derecha: no se espeja
        when (arma) {
            Arma.GARROTE -> {
                taperedTube(b, s, 0f, 0f, 0.02f, -0.30f, 0.019f, 0.024f, 10, 1f, t)
                taperedTube(b, s, 0f, 0f, -0.30f, -0.60f, 0.031f, 0.046f, 10, 1f, t)
                // Nudos de la madera en la punta.
                box(b, s, 0.030f, 0.014f, -0.53f, 0.012f, 0.012f, 0.020f, 0f, 0f, t)
                box(b, s, -0.028f, -0.016f, -0.45f, 0.011f, 0.011f, 0.018f, 0f, 0f, t)
            }
            Arma.PICO -> {
                taperedTube(b, s, 0f, 0f, 0.02f, -0.52f, 0.017f, 0.020f, 8, 1f, t)
                // Cabeza cruzada: la punta para un lado y la pala para el otro.
                box(b, s, 0.10f, 0f, -0.50f, 0.105f, 0.017f, 0.020f, 0f, 0f, t)
                box(b, s, -0.10f, 0f, -0.50f, 0.105f, 0.017f, 0.020f, 0f, 0f, t)
                box(b, s, 0.215f, 0f, -0.50f, 0.038f, 0.011f, 0.013f, 0f, 0f, t)
            }
            Arma.AGUIJON -> {
                taperedTube(b, s, 0f, 0f, 0.02f, -0.16f, 0.018f, 0.021f, 8, 1f, t)
                // Guarda y hoja larga y fina, que se afina hasta la punta.
                box(b, s, 0f, 0f, -0.17f, 0.052f, 0.013f, 0.012f, 0f, 0f, t)
                taperedTube(b, s, 0f, 0f, -0.18f, -0.66f, 0.026f, 0.004f, 6, 0.34f, t)
            }
            Arma.MAZA -> {
                taperedTube(b, s, 0f, 0f, 0.02f, -0.40f, 0.019f, 0.022f, 8, 1f, t)
                // Bloque de basalto en la punta, con las esquinas comidas.
                box(b, s, 0f, 0f, -0.50f, 0.058f, 0.058f, 0.085f, 0f, 0f, t)
                box(b, s, 0f, 0f, -0.60f, 0.040f, 0.040f, 0.030f, 0f, 0f, t)
            }
            Arma.HACHA -> {
                taperedTube(b, s, 0f, 0f, 0.02f, -0.50f, 0.018f, 0.021f, 8, 1f, t)
                // Hoja ancha de un solo filo, montada al costado del cabo.
                box(b, s, 0.085f, 0f, -0.46f, 0.085f, 0.014f, 0.062f, 0f, 0f, t)
                box(b, s, 0.165f, 0f, -0.46f, 0.020f, 0.006f, 0.072f, 0f, 0f, t)
                box(b, s, -0.030f, 0f, -0.46f, 0.030f, 0.020f, 0.030f, 0f, 0f, t)
            }
        }
    }

    /** Los dos brazos, con el arma en la mano derecha si hay alguna equipada. */
    fun build(arma: Arma? = null): Mesh {
        val b = Builder()
        buildArm(b, -1f)   // izquierdo
        buildArm(b, 1f)    // derecho
        if (arma != null) buildArma(b, arma)
        return Mesh(b.v.toArray(), b.idx.toArray())
    }
}

