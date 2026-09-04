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
            if (inverted) { idx.add(n); idx.add(n + 2); idx.add(n + 1) }
            else { idx.add(n); idx.add(n + 1); idx.add(n + 2) }
            n += 3
            // Base
            v.add(0f, 0f, 0f, 0f, if (inverted) 1f else -1f, 0f)
            v.add(x1, 0f, z1, 0f, if (inverted) 1f else -1f, 0f)
            v.add(x0, 0f, z0, 0f, if (inverted) 1f else -1f, 0f)
            if (inverted) { idx.add(n); idx.add(n + 2); idx.add(n + 1) }
            else { idx.add(n); idx.add(n + 1); idx.add(n + 2) }
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
            idx.add(n); idx.add(n + 1); idx.add(n + 2)
            idx.add(n); idx.add(n + 2); idx.add(n + 3)
            n += 4
            // Tapa superior
            v.add(0f, height, 0f, 0f, 1f, 0f)
            v.add(x0, height, z0, 0f, 1f, 0f)
            v.add(x1, height, z1, 0f, 1f, 0f)
            idx.add(n); idx.add(n + 1); idx.add(n + 2)
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
