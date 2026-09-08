package com.mggx.laberinto.gl

import android.opengl.GLES30
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Mallas simples generadas por codigo para los objetos de la cueva. */
object PropMeshes {

    class Geometry(val vertices: FloatArray, val indices: IntArray)

    private fun norm(x: Float, y: Float, z: Float): FloatArray {
        val l = sqrt(x * x + y * y + z * z).coerceAtLeast(1e-6f)
        return floatArrayOf(x / l, y / l, z / l)
    }

    /** Octaedro: sirve de eco, cristal y gema. */
    fun octahedron(stretchY: Float = 1.4f): Geometry {
        val p = arrayOf(
            floatArrayOf(0f, stretchY, 0f),
            floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 0f, 1f),
            floatArrayOf(-1f, 0f, 0f), floatArrayOf(0f, 0f, -1f),
            floatArrayOf(0f, -stretchY, 0f)
        )
        val faces = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(0, 2, 3), intArrayOf(0, 3, 4), intArrayOf(0, 4, 1),
            intArrayOf(5, 2, 1), intArrayOf(5, 3, 2), intArrayOf(5, 4, 3), intArrayOf(5, 1, 4)
        )
        return flatFaces(p, faces)
    }

    /** Cono para estalagmitas y estalactitas. */
    fun cone(segments: Int = 8, height: Float = 1f, radius: Float = 1f, inverted: Boolean = false): Geometry {
        val v = FloatList()
        val idx = IntList()
        var n = 0
        val tipY = if (inverted) -height else height
        for (i in 0 until segments) {
            val a0 = i * 2.0 * PI / segments
            val a1 = (i + 1) * 2.0 * PI / segments
            val x0 = (cos(a0) * radius).toFloat(); val z0 = (sin(a0) * radius).toFloat()
            val x1 = (cos(a1) * radius).toFloat(); val z1 = (sin(a1) * radius).toFloat()
            // Cara lateral
            val nn = norm((x0 + x1) * 0.5f, radius * 0.55f, (z0 + z1) * 0.5f)
            v.add(0f, tipY, 0f, nn[0], nn[1], nn[2])
            v.add(x0, 0f, z0, nn[0], nn[1], nn[2])
            v.add(x1, 0f, z1, nn[0], nn[1], nn[2])
            if (inverted) { idx.add(n); idx.add(n + 1); idx.add(n + 2) }
            else { idx.add(n); idx.add(n + 2); idx.add(n + 1) }
            n += 3
            // Base
            v.add(0f, 0f, 0f, 0f, if (inverted) 1f else -1f, 0f)
            v.add(x1, 0f, z1, 0f, if (inverted) 1f else -1f, 0f)
            v.add(x0, 0f, z0, 0f, if (inverted) 1f else -1f, 0f)
            if (inverted) { idx.add(n); idx.add(n + 1); idx.add(n + 2) }
            else { idx.add(n); idx.add(n + 2); idx.add(n + 1) }
            n += 3
        }
        return Geometry(v.toArray(), idx.toArray())
    }

    /** Cilindro: mango de antorcha y columna de la salida. */
    fun cylinder(segments: Int = 8, height: Float = 1f, radius: Float = 1f): Geometry {
        val v = FloatList()
        val idx = IntList()
        var n = 0
        for (i in 0 until segments) {
            val a0 = i * 2.0 * PI / segments
            val a1 = (i + 1) * 2.0 * PI / segments
            val x0 = (cos(a0) * radius).toFloat(); val z0 = (sin(a0) * radius).toFloat()
            val x1 = (cos(a1) * radius).toFloat(); val z1 = (sin(a1) * radius).toFloat()
            val n0 = norm(x0, 0f, z0); val n1 = norm(x1, 0f, z1)
            v.add(x0, 0f, z0, n0[0], n0[1], n0[2])
            v.add(x1, 0f, z1, n1[0], n1[1], n1[2])
            v.add(x1, height, z1, n1[0], n1[1], n1[2])
            v.add(x0, height, z0, n0[0], n0[1], n0[2])
            idx.add(n); idx.add(n + 2); idx.add(n + 1)
            idx.add(n); idx.add(n + 3); idx.add(n + 2)
            n += 4
            // Tapa superior
            v.add(0f, height, 0f, 0f, 1f, 0f)
            v.add(x0, height, z0, 0f, 1f, 0f)
            v.add(x1, height, z1, 0f, 1f, 0f)
            idx.add(n); idx.add(n + 2); idx.add(n + 1)
            n += 3
        }
        return Geometry(v.toArray(), idx.toArray())
    }

    /** Caja: cofres y bloques. */
    fun box(sx: Float = 1f, sy: Float = 1f, sz: Float = 1f): Geometry {
        val v = FloatList()
        val idx = IntList()
        var n = 0
        val dirs = arrayOf(
            floatArrayOf(0f, 0f, 1f), floatArrayOf(0f, 0f, -1f),
            floatArrayOf(1f, 0f, 0f), floatArrayOf(-1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f), floatArrayOf(0f, -1f, 0f)
        )
        for (d in dirs) {
            // Dos ejes perpendiculares a la normal
            val up = if (kotlin.math.abs(d[1]) > 0.9f) floatArrayOf(0f, 0f, 1f) else floatArrayOf(0f, 1f, 0f)
            val t = norm(up[1] * d[2] - up[2] * d[1], up[2] * d[0] - up[0] * d[2], up[0] * d[1] - up[1] * d[0])
            val b = norm(d[1] * t[2] - d[2] * t[1], d[2] * t[0] - d[0] * t[2], d[0] * t[1] - d[1] * t[0])
            val corners = arrayOf(
                floatArrayOf(-1f, -1f), floatArrayOf(1f, -1f), floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f)
            )
            for (c in corners) {
                val x = (d[0] + t[0] * c[0] + b[0] * c[1]) * sx
                val y = (d[1] + t[1] * c[0] + b[1] * c[1]) * sy
                val z = (d[2] + t[2] * c[0] + b[2] * c[1]) * sz
                v.add(x * 0.5f, y * 0.5f, z * 0.5f, d[0], d[1], d[2])
            }
            idx.add(n); idx.add(n + 1); idx.add(n + 2)
            idx.add(n); idx.add(n + 2); idx.add(n + 3)
            n += 4
        }
        return Geometry(v.toArray(), idx.toArray())
    }

    /** Flecha de la brujula. */
    fun arrow(): Geometry {
        val p = arrayOf(
            floatArrayOf(0f, 0f, 1.6f),      // punta
            floatArrayOf(-0.7f, 0f, -0.4f),
            floatArrayOf(0f, 0.34f, -0.1f),
            floatArrayOf(0.7f, 0f, -0.4f),
            floatArrayOf(0f, -0.34f, -0.1f)
        )
        val faces = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(0, 2, 3), intArrayOf(0, 3, 4), intArrayOf(0, 4, 1),
            intArrayOf(1, 4, 3), intArrayOf(1, 3, 2)
        )
        return flatFaces(p, faces)
    }

    // ------------------------------------------------- herramientas de modelado
    //
    // El pipeline de instancias solo permite UNA escala (la misma en los tres
    // ejes) y giro en Y, asi que las proporciones de una figura no se pueden
    // estirar desde afuera: tienen que venir horneadas en la malla. Estas dos
    // funciones son las que permiten hacer eso sin escribir vertices a mano.

    /**
     * Cuerpo de revolucion: gira un perfil alrededor del eje Y.
     *
     * Cada punto del perfil es (radio, altura), del de abajo al de arriba. Un
     * radio 0 arriba o abajo cierra la figura en punta. Las normales salen de
     * la pendiente del propio perfil y se promedian entre tramos, asi que la
     * superficie se ve redonda y no facetada.
     */
    fun lathe(perfil: Array<FloatArray>, segmentos: Int = 10): Geometry {
        val v = FloatList()
        val idx = IntList()
        val filas = perfil.size

        // Normal 2D de cada tramo, en el plano (radio, altura).
        val nr = FloatArray(filas)
        val ny = FloatArray(filas)
        for (k in 0 until filas) {
            var ar = 0f; var ay = 0f
            for (t in intArrayOf(k - 1, k)) {
                if (t < 0 || t + 1 >= filas) continue
                val dr = perfil[t + 1][0] - perfil[t][0]
                val dy = perfil[t + 1][1] - perfil[t][1]
                val l = sqrt(dr * dr + dy * dy).coerceAtLeast(1e-6f)
                // (dy,-dr) apunta hacia afuera con el perfil de abajo a arriba.
                ar += dy / l; ay += -dr / l
            }
            val l = sqrt(ar * ar + ay * ay).coerceAtLeast(1e-6f)
            nr[k] = ar / l; ny[k] = ay / l
        }

        // Un anillo de vertices por fila del perfil.
        for (k in 0 until filas) {
            val r = perfil[k][0]
            val y = perfil[k][1]
            for (i in 0..segmentos) {
                val a = i * 2.0 * PI / segmentos
                val c = cos(a).toFloat(); val s = sin(a).toFloat()
                val n = norm(nr[k] * c, ny[k], nr[k] * s)
                v.add(r * c, y, r * s, n[0], n[1], n[2])
            }
        }

        // Mismo tejido que usa cylinder(): (A,C,B) y (A,D,C) con A abajo-i,
        // B abajo-i+1, C arriba-i+1, D arriba-i.
        val porFila = segmentos + 1
        for (k in 0 until filas - 1) {
            for (i in 0 until segmentos) {
                val a = k * porFila + i
                val b = k * porFila + i + 1
                val c = (k + 1) * porFila + i + 1
                val d = (k + 1) * porFila + i
                idx.add(a); idx.add(c); idx.add(b)
                idx.add(a); idx.add(d); idx.add(c)
            }
        }
        return Geometry(v.toArray(), idx.toArray())
    }

    /**
     * Extruye un contorno plano (en XY) a lo largo de Z, con tapa adelante,
     * tapa atras y el canto que las une. Sirve para las piezas chatas: alas,
     * placas, aletas.
     *
     * El sentido en que se escriba el contorno no importa: se mide su area con
     * signo y se lo da vuelta si hace falta. Dejarlo librado a quien lo
     * escriba seria pedir un error invisible, porque un contorno al reves
     * compila igual y recien se nota en el telefono, con la figura del reves.
     *
     * Tampoco hace falta que sea convexo: se triangula por recorte de orejas,
     * asi que las muescas (los dedos de un ala, por ejemplo) salen bien.
     */
    fun extruir(contornoDado: Array<FloatArray>, grosor: Float): Geometry {
        var area = 0f
        for (k in contornoDado.indices) {
            val p = contornoDado[k]
            val q = contornoDado[(k + 1) % contornoDado.size]
            area += p[0] * q[1] - q[0] * p[1]
        }
        val contorno = if (area < 0f) contornoDado.reversedArray() else contornoDado

        val v = FloatList()
        val idx = IntList()
        val m = contorno.size
        val h = grosor * 0.5f
        val tri = triangular(contorno)
        var n = 0

        // Tapa de adelante (+Z) y de atras (-Z). La de atras va con los
        // triangulos al reves, porque su normal mira para el otro lado.
        for (lado in 0..1) {
            val z = if (lado == 0) h else -h
            val nz = if (lado == 0) 1f else -1f
            val base = n
            for (p in contorno) { v.add(p[0], p[1], z, 0f, 0f, nz); n++ }
            var k = 0
            while (k < tri.size) {
                if (lado == 0) { idx.add(base + tri[k]); idx.add(base + tri[k + 1]); idx.add(base + tri[k + 2]) }
                else { idx.add(base + tri[k]); idx.add(base + tri[k + 2]); idx.add(base + tri[k + 1]) }
                k += 3
            }
        }

        // Canto: un quad por lado del contorno, con la normal hacia afuera.
        for (k in 0 until m) {
            val p = contorno[k]
            val q = contorno[(k + 1) % m]
            val dx = q[0] - p[0]; val dy = q[1] - p[1]
            // Con el contorno antihorario, (dy,-dx) mira hacia afuera.
            val nn = norm(dy, -dx, 0f)
            v.add(p[0], p[1], h, nn[0], nn[1], nn[2])
            v.add(q[0], q[1], h, nn[0], nn[1], nn[2])
            v.add(q[0], q[1], -h, nn[0], nn[1], nn[2])
            v.add(p[0], p[1], -h, nn[0], nn[1], nn[2])
            idx.add(n); idx.add(n + 2); idx.add(n + 1)
            idx.add(n); idx.add(n + 3); idx.add(n + 2)
            n += 4
        }
        return Geometry(v.toArray(), idx.toArray())
    }

    private fun giro(a: FloatArray, b: FloatArray, c: FloatArray): Float =
        (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])

    /**
     * Triangula un poligono simple ANTIHORARIO por recorte de orejas: busca un
     * vertice convexo cuyo triangulo no tape a ningun otro vertice, lo corta y
     * repite. Devuelve indices de a tres.
     *
     * Se usa esto y no un abanico desde el primer punto porque el abanico solo
     * sirve si todo el contorno se ve desde ahi, y las formas con muescas (un
     * ala con dedos) no cumplen eso: los triangulos que se van afuera salen
     * dados vuelta y la figura se ve agujereada.
     */
    private fun triangular(p: Array<FloatArray>): IntArray {
        val out = IntList()
        val quedan = p.indices.toMutableList()
        var vueltasSinCortar = 0
        while (quedan.size > 3 && vueltasSinCortar <= quedan.size) {
            var cortada = false
            for (k in quedan.indices) {
                val ia = quedan[(k - 1 + quedan.size) % quedan.size]
                val ib = quedan[k]
                val ic = quedan[(k + 1) % quedan.size]
                val a = p[ia]; val b = p[ib]; val c = p[ic]
                if (giro(a, b, c) <= 1e-9f) continue     // vertice concavo o plano
                var libre = true
                for (i in quedan) {
                    if (i == ia || i == ib || i == ic) continue
                    val q = p[i]
                    if (giro(a, b, q) >= 0f && giro(b, c, q) >= 0f && giro(c, a, q) >= 0f) {
                        libre = false; break
                    }
                }
                if (!libre) continue
                out.add(ia); out.add(ib); out.add(ic)
                quedan.removeAt(k)
                cortada = true
                break
            }
            if (cortada) vueltasSinCortar = 0 else vueltasSinCortar++
        }
        for (k in 1 until quedan.size - 1) {
            out.add(quedan[0]); out.add(quedan[k]); out.add(quedan[k + 1])
        }
        return out.toArray()
    }

    // ------------------------------------------------------ armar por piezas
    //
    // Con esto un modelo se escribe como lo que es ("un cuerpo, dos orejas y
    // una cola") en vez de como una lista de vertices, y queda UNA sola malla:
    // el bicho entero sale en una instancia y no en cinco.

    private fun mapear(g: Geometry, f: (Float, Float, Float, Boolean) -> FloatArray): Geometry {
        val v = g.vertices.copyOf()
        var i = 0
        while (i < v.size) {
            val p = f(v[i], v[i + 1], v[i + 2], false)
            val n = f(v[i + 3], v[i + 4], v[i + 5], true)
            v[i] = p[0]; v[i + 1] = p[1]; v[i + 2] = p[2]
            val l = sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]).coerceAtLeast(1e-6f)
            v[i + 3] = n[0] / l; v[i + 4] = n[1] / l; v[i + 5] = n[2] / l
            i += 6
        }
        return Geometry(v, g.indices.copyOf())
    }

    fun trasladar(g: Geometry, dx: Float, dy: Float, dz: Float): Geometry =
        mapear(g) { x, y, z, esNormal ->
            if (esNormal) floatArrayOf(x, y, z) else floatArrayOf(x + dx, y + dy, z + dz)
        }

    /**
     * Escala en cada eje por separado. Las normales van con la inversa (1/s):
     * si no, una figura achatada quedaria con la luz mal, como inflada.
     *
     * No se admiten factores negativos: darian vuelta las caras y la figura se
     * veria del reves. Para espejar, girar 180 grados.
     */
    fun escalar(g: Geometry, sx: Float, sy: Float, sz: Float): Geometry {
        require(sx > 0f && sy > 0f && sz > 0f) { "escalar() no admite factores negativos ni cero" }
        return mapear(g) { x, y, z, esNormal ->
            if (esNormal) floatArrayOf(x / sx, y / sy, z / sz)
            else floatArrayOf(x * sx, y * sy, z * sz)
        }
    }

    fun rotarX(g: Geometry, grados: Float): Geometry {
        val r = Math.toRadians(grados.toDouble())
        val c = cos(r).toFloat(); val s = sin(r).toFloat()
        return mapear(g) { x, y, z, _ -> floatArrayOf(x, y * c - z * s, y * s + z * c) }
    }

    fun rotarY(g: Geometry, grados: Float): Geometry {
        val r = Math.toRadians(grados.toDouble())
        val c = cos(r).toFloat(); val s = sin(r).toFloat()
        return mapear(g) { x, y, z, _ -> floatArrayOf(x * c + z * s, y, -x * s + z * c) }
    }

    fun rotarZ(g: Geometry, grados: Float): Geometry {
        val r = Math.toRadians(grados.toDouble())
        val c = cos(r).toFloat(); val s = sin(r).toFloat()
        return mapear(g) { x, y, z, _ -> floatArrayOf(x * c - y * s, x * s + y * c, z) }
    }

    fun combinar(vararg piezas: Geometry): Geometry {
        val v = FloatList()
        val idx = IntList()
        var base = 0
        for (g in piezas) {
            for (f in g.vertices) v.add(f)
            for (i in g.indices) idx.add(i + base)
            base += g.vertices.size / 6
        }
        return Geometry(v.toArray(), idx.toArray())
    }

    private fun flatFaces(p: Array<FloatArray>, faces: Array<IntArray>): Geometry {
        val v = FloatList()
        val idx = IntList()
        var n = 0
        for (f in faces) {
            val a = p[f[0]]; val b = p[f[1]]; val c = p[f[2]]
            val ux = b[0] - a[0]; val uy = b[1] - a[1]; val uz = b[2] - a[2]
            val vx = c[0] - a[0]; val vy = c[1] - a[1]; val vz = c[2] - a[2]
            val nn = norm(uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx)
            for (q in arrayOf(a, b, c)) v.add(q[0], q[1], q[2], nn[0], nn[1], nn[2])
            idx.add(n); idx.add(n + 1); idx.add(n + 2)
            n += 3
        }
        return Geometry(v.toArray(), idx.toArray())
    }
}

/**
 * Forma dibujada con instancias. Un solo draw call para todos los ecos,
 * todas las estalagmitas, etc.
 */
class InstancedShape(geo: PropMeshes.Geometry, maxInstances: Int) {

    companion object { const val INSTANCE_FLOATS = 12 }

    private val vao = IntArray(1)
    private val vbo = IntArray(1)
    private val ebo = IntArray(1)
    private val ibo = IntArray(1)
    val indexCount = geo.indices.size
    private val instanceData = FloatArray(maxInstances * INSTANCE_FLOATS)
    var instanceCount = 0
        private set
    private val capacity = maxInstances

    init {
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(1, vbo, 0)
        GLES30.glGenBuffers(1, ebo, 0)
        GLES30.glGenBuffers(1, ibo, 0)

        GLES30.glBindVertexArray(vao[0])

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        val vb = GLUtil.floatBuffer(geo.vertices)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, geo.vertices.size * 4, vb, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 24, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 24, 12)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo[0])
        val ib = GLUtil.intBuffer(geo.indices)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, geo.indices.size * 4, ib, GLES30.GL_STATIC_DRAW)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, ibo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, capacity * INSTANCE_FLOATS * 4, null, GLES30.GL_DYNAMIC_DRAW)
        val stride = INSTANCE_FLOATS * 4
        for (i in 0 until 3) {
            val loc = 2 + i
            GLES30.glEnableVertexAttribArray(loc)
            GLES30.glVertexAttribPointer(loc, 4, GLES30.GL_FLOAT, false, stride, i * 16)
            GLES30.glVertexAttribDivisor(loc, 1)
        }

        GLES30.glBindVertexArray(0)
        GLUtil.checkError("InstancedShape init")
    }

    fun begin() { instanceCount = 0 }

    fun add(
        x: Float, y: Float, z: Float, scale: Float,
        r: Float, g: Float, b: Float, emissive: Float,
        rotation: Float, phase: Float, kind: Float, alpha: Float
    ) {
        if (instanceCount >= capacity) return
        val o = instanceCount * INSTANCE_FLOATS
        instanceData[o] = x; instanceData[o + 1] = y; instanceData[o + 2] = z; instanceData[o + 3] = scale
        instanceData[o + 4] = r; instanceData[o + 5] = g; instanceData[o + 6] = b; instanceData[o + 7] = emissive
        instanceData[o + 8] = rotation; instanceData[o + 9] = phase; instanceData[o + 10] = kind; instanceData[o + 11] = alpha
        instanceCount++
    }

    fun draw() {
        if (instanceCount == 0) return
        GLES30.glBindVertexArray(vao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, ibo[0])
        val buf = GLUtil.floatBuffer(instanceData.copyOf(instanceCount * INSTANCE_FLOATS))
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, instanceCount * INSTANCE_FLOATS * 4, buf)
        GLES30.glDrawElementsInstanced(
            GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0, instanceCount
        )
        GLES30.glBindVertexArray(0)
    }

    fun release() {
        GLES30.glDeleteVertexArrays(1, vao, 0)
        GLES30.glDeleteBuffers(1, vbo, 0)
        GLES30.glDeleteBuffers(1, ebo, 0)
        GLES30.glDeleteBuffers(1, ibo, 0)
    }
}
