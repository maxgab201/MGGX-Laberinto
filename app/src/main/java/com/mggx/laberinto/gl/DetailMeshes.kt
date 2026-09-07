package com.mggx.laberinto.gl

import com.mggx.laberinto.gl.PropMeshes.Geometry
import kotlin.math.*

/** Mallas de detalle originales. Se generan una vez y se dibujan por instancias. */
object DetailMeshes {
    /** Caja con bisel real: conserva medidas, caras planas y normales continuas. */
    fun roundedBox(sx: Float = 1f, sy: Float = 1f, sz: Float = 1f, bevel: Float = 0.08f): Geometry {
        require(sx > 0f && sy > 0f && sz > 0f && bevel > 0f)
        val half = floatArrayOf(sx * 0.5f, sy * 0.5f, sz * 0.5f)
        val radius = min(bevel, minOf(sx, sy, sz) * 0.45f)
        val vertices = FloatList()
        val indices = IntList()
        // Edge samples cluster around the bevel, retaining a broad flat center.
        for (axis in 0..2) for (sign in intArrayOf(-1, 1)) {
            val u = (axis + 1) % 3
            val w = (axis + 2) % 3
            fun samples(h: Float) = floatArrayOf(-h, -h + radius * 0.30f, -h + radius,
                h - radius, h - radius * 0.30f, h)
            val us = samples(half[u]); val ws = samples(half[w])
            val base = vertices.size / 6
            for (j in ws.indices) for (i in us.indices) {
                val p = FloatArray(3)
                p[axis] = half[axis] * sign; p[u] = us[i]; p[w] = ws[j]
                val inner = FloatArray(3) { k -> p[k].coerceIn(-half[k] + radius, half[k] - radius) }
                val n = FloatArray(3) { k -> p[k] - inner[k] }
                val length = sqrt(n.sumOf { (it * it).toDouble() }).toFloat().coerceAtLeast(1e-8f)
                for (k in 0..2) n[k] /= length
                vertices.add(inner[0] + n[0] * radius, inner[1] + n[1] * radius,
                    inner[2] + n[2] * radius, n[0], n[1], n[2])
            }
            for (j in 0 until ws.size - 1) for (i in 0 until us.size - 1) {
                val a = base + j * us.size + i
                val b = a + 1; val c = b + us.size; val d = a + us.size
                if (sign > 0) {
                    indices.add(a); indices.add(b); indices.add(c)
                    indices.add(a); indices.add(c); indices.add(d)
                } else {
                    indices.add(a); indices.add(c); indices.add(b)
                    indices.add(a); indices.add(d); indices.add(c)
                }
            }
        }
        return Geometry(vertices.toArray(), indices.toArray())
    }

    /** Roca erosionada asimetrica, con normales recalculadas despues de deformar. */
    fun boulder(): Geometry {
        val g = roundedBox(1.72f, 1.28f, 1.55f, 0.50f)
        val v = g.vertices.copyOf()
        for (i in v.indices step 6) {
            val x = v[i]; val y = v[i + 1]; val z = v[i + 2]
            val scale = 1f + sin(x * 5.3f + z * 3.7f) * sin(y * 4.1f + 0.8f) * 0.11f
            v[i] *= scale; v[i + 1] *= scale; v[i + 2] *= scale
        }
        return recalculate(Geometry(v, g.indices))
    }

    /** Deposito mineral por anillos, con base ensanchada y punta afinada. */
    fun stalagmite(inverted: Boolean = false): Geometry {
        val g = PropMeshes.lathe(arrayOf(
            floatArrayOf(0f, 0f), floatArrayOf(0.13f, 0f),
            floatArrayOf(0.12f, 0.06f), floatArrayOf(0.09f, 0.22f),
            floatArrayOf(0.082f, 0.30f), floatArrayOf(0.06f, 0.46f),
            floatArrayOf(0.049f, 0.56f), floatArrayOf(0.034f, 0.73f),
            floatArrayOf(0.014f, 0.91f), floatArrayOf(0f, 1f)
        ), 16)
        return if (inverted) PropMeshes.rotarX(g, 180f) else g
    }

    /** Cristal tallado con cintura y varias facetas, dentro del radio original. */
    fun gem(): Geometry {
        val g = PropMeshes.lathe(arrayOf(
            floatArrayOf(0f, -1.5f), floatArrayOf(0.68f, -0.52f),
            floatArrayOf(1f, -0.06f), floatArrayOf(1f, 0.08f),
            floatArrayOf(0.65f, 0.56f), floatArrayOf(0f, 1.5f)
        ), 8)
        return flat(g)
    }

    /** Aro de valvula, con radios suaves y sin aristas de una caja. */
    fun torus(radius: Float, tube: Float, rings: Int = 24, sides: Int = 8): Geometry {
        val v = FloatList(); val idx = IntList()
        for (j in 0..rings) for (i in 0..sides) {
            val a = j * 2.0 * PI / rings; val b = i * 2.0 * PI / sides
            val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
            val cb = cos(b).toFloat(); val sb = sin(b).toFloat()
            v.add((radius + tube * cb) * ca, tube * sb, (radius + tube * cb) * sa,
                cb * ca, sb, cb * sa)
        }
        for (j in 0 until rings) for (i in 0 until sides) {
            val a = j * (sides + 1) + i; val b = a + sides + 1
            idx.add(a); idx.add(a + 1); idx.add(b + 1)
            idx.add(a); idx.add(b + 1); idx.add(b)
        }
        return Geometry(v.toArray(), idx.toArray())
    }

    private fun recalculate(g: Geometry): Geometry {
        val v = g.vertices.copyOf()
        for (i in v.indices step 6) { v[i + 3] = 0f; v[i + 4] = 0f; v[i + 5] = 0f }
        for (i in g.indices.indices step 3) {
            val a = g.indices[i] * 6; val b = g.indices[i + 1] * 6; val c = g.indices[i + 2] * 6
            val ux = v[b]-v[a]; val uy = v[b+1]-v[a+1]; val uz = v[b+2]-v[a+2]
            val vx = v[c]-v[a]; val vy = v[c+1]-v[a+1]; val vz = v[c+2]-v[a+2]
            for (k in intArrayOf(a,b,c)) {
                v[k+3] += uy*vz-uz*vy; v[k+4] += uz*vx-ux*vz; v[k+5] += ux*vy-uy*vx
            }
        }
        for (i in v.indices step 6) {
            val l = sqrt(v[i+3]*v[i+3]+v[i+4]*v[i+4]+v[i+5]*v[i+5]).coerceAtLeast(1e-8f)
            for (k in 3..5) v[i+k] /= l
        }
        return Geometry(v,g.indices)
    }

    private fun flat(g: Geometry): Geometry {
        val v = FloatList(); val indices = IntList()
        for (i in g.indices.indices step 3) {
            val a = g.indices[i]*6; val b = g.indices[i+1]*6; val c = g.indices[i+2]*6
            val p = g.vertices
            val ux=p[b]-p[a]; val uy=p[b+1]-p[a+1]; val uz=p[b+2]-p[a+2]
            val vx=p[c]-p[a]; val vy=p[c+1]-p[a+1]; val vz=p[c+2]-p[a+2]
            val nx=uy*vz-uz*vy; val ny=uz*vx-ux*vz; val nz=ux*vy-uy*vx
            val len=sqrt(nx*nx+ny*ny+nz*nz)
            if (len < 1e-8f) continue
            for (k in intArrayOf(a,b,c)) {
                indices.add(v.size/6)
                v.add(p[k],p[k+1],p[k+2],nx/len,ny/len,nz/len)
            }
        }
        return Geometry(v.toArray(),indices.toArray())
    }
}
