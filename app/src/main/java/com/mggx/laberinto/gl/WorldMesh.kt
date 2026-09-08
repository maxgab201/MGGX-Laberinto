package com.mggx.laberinto.gl

import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.maze.Maze
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Convierte el laberinto en una malla unica de cueva.
 *
 * No son cajas: cada cara (piso, techo y pared) se subdivide en una grilla y se
 * desplaza con un ruido continuo del mundo, asi la roca queda abollada y
 * redondeada.
 *
 * La clave de que se vea como una cueva y no como un pasillo de cajas esta en
 * DONDE se apaga ese desplazamiento (ver [mascaraCampo]). Antes se apagaba en
 * los cuatro bordes de cada cara, y eso dejaba una arista dura cada [CELL]
 * metros: el tunel quedaba como una fila de almohadones cuadrados. Ahora se
 * apaga segun un campo de distancia a la roca, que al depender solo de la
 * posicion del mundo da el mismo valor desde las dos casillas que comparten
 * un borde: la roca corre larga e irregular sin una sola grieta.
 *
 * Encima del ruido va un perfil fijo que redondea la seccion del tunel: las
 * paredes se abren hacia afuera a media altura ([ENSANCHE_PARED]) y el techo
 * sube en el medio ([ARCO_TECHO]), los dos apagandose contra el piso y contra
 * las paredes. El resultado es una seccion de tunel ovalada en vez de un
 * rectangulo.
 *
 * Ademas respeta el relieve del laberinto: cada casilla tiene su altura de piso
 * y su alto libre, y entre casillas a distinta altura se levanta el escalon
 * correspondiente. Donde hay escalera se le agregan los travesanos.
 */
object WorldMesh {

    /** floats por vertice: pos(3) normal(3) uv(2) ao(1) layer(1) */
    const val STRIDE_FLOATS = 10
    const val STRIDE_BYTES = STRIDE_FLOATS * 4

    private const val CELL = GameSession.CELL
    private const val TEX_SCALE = 3.0f

    /** Cuanto se abolla la roca hacia adentro y hacia afuera, en metros. */
    private const val BULTO_PARED = 0.30f
    internal const val BULTO_PISO = 0.13f
    private const val BULTO_TECHO = 0.34f

    /**
     * Cuanto se abre la pared hacia AFUERA a media altura, en metros.
     *
     * Es un corrimiento fijo que se suma al ruido y usa la misma mascara, asi
     * que vale cero contra el piso y contra el techo y es maximo en el medio:
     * el tunel deja de ser un rectangulo y pasa a tener panza. Como siempre
     * empuja hacia la roca, ademas achica el bulto que podia meterse hacia
     * adentro (de 30cm a 8cm), que es justo lo que podia rozar al jugador.
     */
    private const val ENSANCHE_PARED = 0.22f

    /**
     * Cuanto sube el techo en el medio del tunel, en metros. Se apaga contra
     * las paredes, asi que arma un arco en vez de una tapa plana.
     *
     * Va acotado por el alto libre de la casilla ([FACTOR_ARCO]) porque en una
     * gatera el alto que se ve tiene que seguir pareciendo el que hay: el
     * agacharse automatico mira `ceilClearance`, no la malla.
     */
    private const val ARCO_TECHO = 0.20f
    private const val FACTOR_ARCO = 0.07f

    /** Capas del atlas de texturas. */
    private const val CAPA_PARED = 0f
    private const val CAPA_PISO = 1f
    private const val CAPA_TECHO = 2f

    class Mesh(val vertices: FloatArray, val indices: IntArray, val triangleCount: Int)

    private fun hash(x: Int, y: Int, z: Int): Float {
        var h = x * 374761393 + y * 668265263 + z * 1442695041
        h = (h xor (h shr 13)) * 1274126177
        h = h xor (h shr 16)
        return (h and 0x7FFFFFFF) / 2147483647.0f
    }

    private fun suave(t: Float): Float = t * t * (3f - 2f * t)

    /**
     * Ruido de valor continuo en el espacio del mundo, de -1 a 1. Como depende
     * solo de la posicion, dos caras que comparten un borde leen el mismo valor
     * y no aparece ningun salto.
     */
    private fun ruido(x: Float, y: Float, z: Float): Float {
        val ix = kotlin.math.floor(x).toInt()
        val iy = kotlin.math.floor(y).toInt()
        val iz = kotlin.math.floor(z).toInt()
        val fx = suave(x - ix)
        val fy = suave(y - iy)
        val fz = suave(z - iz)
        var acc = 0f
        for (dz in 0..1) for (dy in 0..1) for (dx in 0..1) {
            val w = (if (dx == 1) fx else 1f - fx) *
                (if (dy == 1) fy else 1f - fy) *
                (if (dz == 1) fz else 1f - fz)
            acc += w * hash(ix + dx, iy + dy, iz + dz)
        }
        return acc * 2f - 1f
    }

    /** Dos octavas: bultos grandes y grumos chicos. */
    internal fun roca(x: Float, y: Float, z: Float): Float =
        ruido(x * 0.62f, y * 0.62f, z * 0.62f) * 0.72f +
            ruido(x * 1.7f + 11f, y * 1.7f, z * 1.7f - 7f) * 0.28f

    // ------------------------------------------------------- continuidad
    //
    // Donde se apaga el desplazamiento es lo que decide si la cueva se ve
    // organica o cuadrada, y es tambien el unico lugar donde se puede abrir
    // una grieta. Antes se apagaba en los cuatro bordes de CADA cara: nunca
    // habia grietas, pero quedaba una arista dura cada [CELL] metros.
    //
    // Lo obvio seria apagar solo contra los bordes donde la superficie no
    // sigue, con una rampa por cada eje de la cara. NO ALCANZA, y el error es
    // sutil: la mascara de un eje depende de los vecinos del OTRO eje, que no
    // son los mismos para las dos casillas que comparten un borde. Con la
    // casilla de la diagonal distinta, una desplaza y la otra no, y ahi se
    // abre la grieta (lo agarro CuevaContinuaTest antes de que llegara a un
    // telefono).
    //
    // Por eso la mascara no se arma por cara sino como un CAMPO DE DISTANCIA
    // del mundo: cuanto falta hasta el borde mas cercano donde la superficie
    // deja de continuar. Al depender solo de la posicion y del laberinto, dos
    // caras vecinas calculan exactamente el mismo valor en el borde que
    // comparten, sin importar como esten sus diagonales. Y de yapa se ve
    // mejor: en un pasillo el bulto crece hacia el centro y corre parejo a lo
    // largo, que es justo como es una cueva.

    /** A que distancia de la roca la superficie ya llego a su bulto pleno. */
    private const val RADIO_APAGADO = CELL * 0.5f

    /** Cuantos rectangulos de borde puede tener una cara como mucho. */
    private const val MAX_BORDES = 8

    /**
     * Distancia de (x,z) al rectangulo de la casilla que arranca en
     * (minX,minZ). Cero si el punto esta adentro o justo en el canto.
     */
    private fun distanciaACasilla(x: Float, z: Float, minX: Float, minZ: Float): Float {
        val ax = kotlin.math.max(0f, kotlin.math.max(minX - x, x - (minX + CELL)))
        val az = kotlin.math.max(0f, kotlin.math.max(minZ - z, z - (minZ + CELL)))
        if (ax == 0f) return az
        if (az == 0f) return ax
        return sqrt(ax * ax + az * az)
    }

    /**
     * La mascara en si: 0 pegado al borde que corta, 1 a [RADIO_APAGADO] o
     * mas. Va por [suave] para llegar con pendiente cero a los dos extremos y
     * no dejar ninguna arista de sombreado.
     */
    private fun mascaraCampo(x: Float, z: Float, bordes: FloatArray, n: Int): Float {
        var d = RADIO_APAGADO
        for (k in 0 until n) {
            val dd = distanciaACasilla(x, z, bordes[k * 2], bordes[k * 2 + 1])
            if (dd < d) d = dd
            if (d == 0f) return 0f
        }
        return suave(d / RADIO_APAGADO)
    }

    private fun abierta(maze: Maze, gx: Int, gy: Int): Boolean =
        maze.inBounds(gx, gy) && !maze.isSolid(gx, gy)

    /** El piso sigue hacia el vecino: casilla abierta y a la misma altura. */
    internal fun pisoSigue(maze: Maze, gx: Int, gy: Int, vx: Int, vy: Int): Boolean =
        abierta(maze, vx, vy) && abs(maze.floorY(vx, vy) - maze.floorY(gx, gy)) < 1e-4f

    /**
     * El techo sigue hacia el vecino. Se pide tambien el mismo alto libre
     * porque de el dependen la amplitud del bulto y la del arco: si no
     * coincidieran, el borde compartido no daria el mismo punto.
     */
    internal fun techoSigue(maze: Maze, gx: Int, gy: Int, vx: Int, vy: Int): Boolean {
        if (!abierta(maze, vx, vy)) return false
        val i = maze.index(gx, gy)
        val j = maze.index(vx, vy)
        return abs(maze.ceilY(vx, vy) - maze.ceilY(gx, gy)) < 1e-4f &&
            abs(maze.ceilClearance[j] - maze.ceilClearance[i]) < 1e-4f
    }

    /**
     * La pared del lado (sx,sy) sigue hacia el vecino (vx,vy): el vecino es
     * casilla abierta, tiene roca del mismo lado, y comparte piso y techo (si
     * no, el tramo vertical no seria el mismo y el borde no encajaria).
     */
    internal fun paredSigue(
        maze: Maze, gx: Int, gy: Int, vx: Int, vy: Int, sx: Int, sy: Int
    ): Boolean = abierta(maze, vx, vy) &&
        maze.isSolid(vx + sx, vy + sy) &&
        pisoSigue(maze, gx, gy, vx, vy) &&
        techoSigue(maze, gx, gy, vx, vy)

    /**
     * Junta los rectangulos de las casillas del anillo de alrededor contra las
     * que esta superficie tiene que volver a su plano teorico.
     *
     * Alcanza con el anillo de 1: una casilla mas lejos tiene su canto a
     * [CELL] metros, que ya es mas que [RADIO_APAGADO], asi que no puede
     * cambiar el resultado. Eso es justamente lo que hace que dos casillas
     * vecinas coincidan: los unicos bordes que les pueden importar en el
     * limite que comparten estan en el anillo de las dos.
     */
    private fun bordesDeCara(
        maze: Maze, gx: Int, gy: Int, out: FloatArray, sigue: (Int, Int) -> Boolean
    ): Int {
        var k = 0
        for (dy in -1..1) for (dx in -1..1) {
            if (dx == 0 && dy == 0) continue
            val vx = gx + dx
            val vy = gy + dy
            if (sigue(vx, vy)) continue
            out[k * 2] = vx * CELL
            out[k * 2 + 1] = vy * CELL
            k++
        }
        return k
    }

    /**
     * Lo mismo para una pared, pero mirando solo los dos vecinos a lo largo
     * de ella: la casilla de enfrente es la roca misma y su rectangulo toca
     * el plano de la pared, asi que si se la contara la mascara daria cero en
     * todos lados. Como los rectangulos que quedan contienen ese plano, la
     * distancia termina midiendose a lo largo de la pared, que es lo buscado.
     */
    private fun bordesDePared(
        maze: Maze, gx: Int, gy: Int, sx: Int, sy: Int, out: FloatArray
    ): Int {
        val ax = if (sx != 0) 0 else 1
        val ay = if (sx != 0) 1 else 0
        var k = 0
        for (signo in intArrayOf(-1, 1)) {
            val vx = gx + ax * signo
            val vy = gy + ay * signo
            if (paredSigue(maze, gx, gy, vx, vy, sx, sy)) continue
            out[k * 2] = vx * CELL
            out[k * 2 + 1] = vy * CELL
            k++
        }
        return k
    }

    /** Altura del piso en el centro de una casilla (para apoyar objetos). */
    fun floorHeight(maze: Maze, gx: Int, gy: Int): Float = maze.floorY(gx, gy)

    /**
     * Altura real del piso en un punto (x,z) cualquiera, con la misma
     * abolladura de ruido que dibuja [cara]. [floorHeight] da la altura
     * TEORICA y plana de la casilla; el piso que de verdad se ve puede estar
     * hasta [BULTO_PISO] por encima o por debajo de eso, maximo justo en el
     * centro de la casilla. Sirve para apoyar decals (marcas de tiza, rastro
     * de pisadas) sobre la roca real y que no queden enterrados.
     */
    fun realFloorHeight(maze: Maze, x: Float, z: Float): Float {
        val gx = (x / CELL).toInt().coerceIn(0, maze.gw - 1)
        val gy = (z / CELL).toInt().coerceIn(0, maze.gh - 1)
        val fy = maze.floorY(gx, gy)
        // La MISMA mascara que usa build() para el piso de esta casilla. Si se
        // desincronizara, las marcas de tiza volverian a enterrarse.
        val bordes = FloatArray(MAX_BORDES * 2)
        val n = bordesDeCara(maze, gx, gy, bordes) { vx, vy -> pisoSigue(maze, gx, gy, vx, vy) }
        return fy + roca(x, fy, z) * (-BULTO_PISO) * mascaraCampo(x, z, bordes, n)
    }

    /** Altura del techo en el centro de una casilla. */
    fun ceilHeight(maze: Maze, gx: Int, gy: Int): Float = maze.ceilY(gx, gy)

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

    // ------------------------------------------------------------- armado

    private class Builder {
        val v = FloatList(1 shl 17)
        val idx = IntList(1 shl 16)
        var count = 0

        fun push(
            x: Float, y: Float, z: Float,
            nx: Float, ny: Float, nz: Float,
            u: Float, vv: Float, ao: Float, layer: Float
        ): Int {
            v.add(x, y, z, nx, ny, nz, u, vv, ao, layer)
            return count++
        }

        /**
         * El anillo a,b,c,d viene en sentido HORARIO visto desde el lado al que
         * apunta la normal; OpenGL quiere antihorario, por eso va invertido.
         */
        fun quad(a: Int, b: Int, c: Int, d: Int) {
            idx.add(a); idx.add(c); idx.add(b)
            idx.add(a); idx.add(d); idx.add(c)
        }
    }

    /**
     * Como se apaga el desplazamiento de una cara contra sus bordes.
     *
     * [CONTORNO] es el modo viejo, que vuelve al plano teorico en los cuatro
     * bordes de la cara: lo siguen usando los escalones y los travesanos, que
     * son piezas sueltas y no tienen que empalmar con nadie.
     *
     * [CAMPO] y [PARED] usan el campo de distancia (ver arriba). La diferencia
     * es que la pared, ademas, se apaga siempre contra el piso y contra el
     * techo, que es donde se encuentra con caras de otra normal.
     */
    private object Modo {
        const val CONTORNO = 0
        const val CAMPO = 1
        const val PARED = 2
    }

    /**
     * Una cara subdividida y abollada.
     *
     * Los cuatro puntos vienen en el orden del anillo HORARIO visto desde el
     * lado al que apunta la normal (la misma convencion que pide [Builder.quad]).
     * La superficie es la interpolacion bilineal de esas esquinas mas un
     * desplazamiento a lo largo de la normal, apagado segun [modo] para que
     * las caras vecinas encajen sin grietas.
     */
    private fun cara(
        b: Builder,
        n: Int,
        p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
        nx: Float, ny: Float, nz: Float,
        amplitud: Float,
        perfil: Float,
        layer: Float,
        modo: Int,
        bordes: FloatArray?, nBordes: Int,
        ao0: Float, ao1: Float, ao2: Float, ao3: Float
    ) {
        val mueve = amplitud != 0f || perfil != 0f

        // p(s,t): s va de p0 a p1, t va de p0 a p3.
        fun base(s: Float, t: Float, out: FloatArray) {
            for (k in 0..2) {
                val a = p0[k] + (p1[k] - p0[k]) * s
                val c = p3[k] + (p2[k] - p3[k]) * s
                out[k] = a + (c - a) * t
            }
        }

        fun desplazado(s: Float, t: Float, out: FloatArray) {
            base(s, t, out)
            if (mueve) {
                val apaga = when (modo) {
                    Modo.CAMPO -> mascaraCampo(out[0], out[2], bordes!!, nBordes)
                    Modo.PARED -> mascaraCampo(out[0], out[2], bordes!!, nBordes) *
                        sin(PI * t.toDouble()).toFloat()
                    else -> sin(PI * s.toDouble()).toFloat() * sin(PI * t.toDouble()).toFloat()
                }
                // El perfil fijo (panza de la pared, arco del techo) va por la
                // misma mascara que el ruido: asi se apaga exactamente en los
                // mismos bordes y no puede abrir una grieta.
                val d = (roca(out[0], out[1], out[2]) * amplitud + perfil) * apaga
                out[0] += nx * d; out[1] += ny * d; out[2] += nz * d
            }
        }

        // La textura se proyecta sobre el plano dominante de la normal. Al
        // depender solo de la posicion del mundo, no hay costura entre caras.
        val ejeY = abs(ny) >= abs(nx) && abs(ny) >= abs(nz)
        val ejeX = !ejeY && abs(nx) >= abs(nz)

        val ids = IntArray((n + 1) * (n + 1))
        val p = FloatArray(3)
        val q = FloatArray(3)
        val pa = FloatArray(3)
        val pb = FloatArray(3)
        val h = 0.5f / n
        for (j in 0..n) {
            val t = j.toFloat() / n
            for (i in 0..n) {
                val s = i.toFloat() / n
                desplazado(s, t, p)
                base(s, t, q)

                var vnx = nx; var vny = ny; var vnz = nz
                if (mueve) {
                    val s0 = (s - h).coerceIn(0f, 1f); val s1 = (s + h).coerceIn(0f, 1f)
                    val t0 = (t - h).coerceIn(0f, 1f); val t1 = (t + h).coerceIn(0f, 1f)
                    desplazado(s0, t, pa); desplazado(s1, t, pb)
                    val ux = pb[0] - pa[0]; val uy = pb[1] - pa[1]; val uz = pb[2] - pa[2]
                    desplazado(s, t0, pa); desplazado(s, t1, pb)
                    val wx = pb[0] - pa[0]; val wy = pb[1] - pa[1]; val wz = pb[2] - pa[2]
                    // Con el anillo horario visto desde la normal, (dp/dt) x (dp/ds)
                    // apunta para el mismo lado que la normal declarada.
                    var cx = wy * uz - wz * uy
                    var cy = wz * ux - wx * uz
                    var cz = wx * uy - wy * ux
                    val len = sqrt(cx * cx + cy * cy + cz * cz)
                    if (len > 1e-5f) {
                        cx /= len; cy /= len; cz /= len
                        // El ruido nunca puede dar vuelta la cara.
                        if (cx * nx + cy * ny + cz * nz > 0.2f) { vnx = cx; vny = cy; vnz = cz }
                    }
                }

                val u = when {
                    ejeY -> q[0] / TEX_SCALE
                    ejeX -> q[2] / TEX_SCALE
                    else -> q[0] / TEX_SCALE
                }
                val vv = if (ejeY) q[2] / TEX_SCALE else q[1] / TEX_SCALE

                val ao = ao0 * (1f - s) * (1f - t) + ao1 * s * (1f - t) +
                    ao2 * s * t + ao3 * (1f - s) * t
                ids[j * (n + 1) + i] = b.push(p[0], p[1], p[2], vnx, vny, vnz, u, vv, ao, layer)
            }
        }

        for (j in 0 until n) {
            for (i in 0 until n) {
                val a = ids[j * (n + 1) + i]
                val bb = ids[j * (n + 1) + i + 1]
                val c = ids[(j + 1) * (n + 1) + i + 1]
                val d = ids[(j + 1) * (n + 1) + i]
                b.quad(a, bb, c, d)
            }
        }
    }

    private fun pt(x: Float, y: Float, z: Float) = floatArrayOf(x, y, z)

    /**
     * Cuantas veces se subdivide cada cara.
     *
     * Ahora que el ruido corre continuo entre casillas, subdividir de mas se
     * nota mucho mas que antes: con pocas divisiones el bulto queda como una
     * sola loma suave por casilla. Pero la malla crece con el CUADRADO de
     * este numero y se dibuja entera cada cuadro, asi que se ata a la calidad
     * elegida y al tamano del nivel para no fundir un telefono modesto.
     */
    internal fun subdivisiones(abiertas: Int, calidad: Int): Int {
        val grande = abiertas > 2200
        return when {
            calidad <= 0 -> if (grande) 2 else 3
            calidad >= 3 -> if (grande) 4 else 5
            else -> if (grande) 3 else 4
        }
    }

    fun build(maze: Maze, calidad: Int = 2): Mesh {
        val b = Builder()
        val abiertas = maze.solid.count { !it }
        val n = subdivisiones(abiertas, calidad)
        // Se reusan casilla a casilla: los bordes son los mismos para toda la
        // cara, asi que se juntan una vez y no por vertice.
        val bordes = FloatArray(MAX_BORDES * 2)
        val bordesTecho = FloatArray(MAX_BORDES * 2)
        val bordesPared = FloatArray(MAX_BORDES * 2)

        for (gy in 0 until maze.gh) {
            for (gx in 0 until maze.gw) {
                if (maze.isSolid(gx, gy)) continue
                val i = maze.index(gx, gy)

                val x0 = gx * CELL
                val x1 = (gx + 1) * CELL
                val z0 = gy * CELL
                val z1 = (gy + 1) * CELL
                val fy = maze.floorY(gx, gy)
                val cy = fy + maze.ceilClearance[i]

                val a00 = cornerAo(maze, gx, gy)
                val a10 = cornerAo(maze, gx + 1, gy)
                val a11 = cornerAo(maze, gx + 1, gy + 1)
                val a01 = cornerAo(maze, gx, gy + 1)

                // --------------------------------------------------- piso
                val nPiso = bordesDeCara(maze, gx, gy, bordes) { vx, vy ->
                    pisoSigue(maze, gx, gy, vx, vy)
                }
                cara(
                    b, n,
                    pt(x0, fy, z0), pt(x1, fy, z0), pt(x1, fy, z1), pt(x0, fy, z1),
                    0f, 1f, 0f, -BULTO_PISO, 0f, CAPA_PISO,
                    Modo.CAMPO, bordes, nPiso,
                    a00, a10, a11, a01
                )

                // --------------------------------------------------- techo
                // Anillo al reves porque la normal mira para abajo.
                val t = 0.82f
                // En una gatera el techo casi no se abolla: si no, deja de
                // parecer un tramo bajo.
                val bultoTecho = -kotlin.math.min(BULTO_TECHO, maze.ceilClearance[i] * 0.11f)
                // El arco va con signo negativo porque la normal del techo
                // mira para abajo: asi el techo SUBE en el medio del tunel.
                val arco = -kotlin.math.min(ARCO_TECHO, maze.ceilClearance[i] * FACTOR_ARCO)
                val nTecho = bordesDeCara(maze, gx, gy, bordesTecho) { vx, vy ->
                    techoSigue(maze, gx, gy, vx, vy)
                }
                cara(
                    b, n,
                    pt(x0, cy, z0), pt(x0, cy, z1), pt(x1, cy, z1), pt(x1, cy, z0),
                    0f, -1f, 0f, bultoTecho, arco, CAPA_TECHO,
                    Modo.CAMPO, bordesTecho, nTecho,
                    a00 * t, a01 * t, a11 * t, a10 * t
                )

                // -------------------------------------------------- paredes
                // En las cuatro, t va del piso al techo, asi que arriba y
                // abajo SIEMPRE se apagan: ahi la pared se encuentra con dos
                // superficies de otra normal. Lo que cambia es el eje s, que
                // corre a lo largo de la pared, y ahi solo se apaga si la
                // pared no continua en la casilla vecina.

                // Cara -X: la normal mira hacia +X (hacia adentro de la casilla).
                // s va de z0 a z1.
                if (maze.isSolid(gx - 1, gy)) {
                    cara(
                        b, n,
                        pt(x0, fy, z0), pt(x0, fy, z1), pt(x0, cy, z1), pt(x0, cy, z0),
                        1f, 0f, 0f, BULTO_PARED, -ENSANCHE_PARED, CAPA_PARED,
                        Modo.PARED, bordesPared,
                        bordesDePared(maze, gx, gy, -1, 0, bordesPared),
                        a00 * verticalAo(0f), a01 * verticalAo(0f),
                        a01 * verticalAo(1f), a00 * verticalAo(1f)
                    )
                } else emitirDesniveles(b, maze, gx, gy, gx - 1, gy, n)

                // Cara +X: s va de z1 a z0 (al reves que la de -X).
                if (maze.isSolid(gx + 1, gy)) {
                    cara(
                        b, n,
                        pt(x1, fy, z1), pt(x1, fy, z0), pt(x1, cy, z0), pt(x1, cy, z1),
                        -1f, 0f, 0f, BULTO_PARED, -ENSANCHE_PARED, CAPA_PARED,
                        Modo.PARED, bordesPared,
                        bordesDePared(maze, gx, gy, 1, 0, bordesPared),
                        a11 * verticalAo(0f), a10 * verticalAo(0f),
                        a10 * verticalAo(1f), a11 * verticalAo(1f)
                    )
                } else emitirDesniveles(b, maze, gx, gy, gx + 1, gy, n)

                // Cara -Z: s va de x1 a x0.
                if (maze.isSolid(gx, gy - 1)) {
                    cara(
                        b, n,
                        pt(x1, fy, z0), pt(x0, fy, z0), pt(x0, cy, z0), pt(x1, cy, z0),
                        0f, 0f, 1f, BULTO_PARED, -ENSANCHE_PARED, CAPA_PARED,
                        Modo.PARED, bordesPared,
                        bordesDePared(maze, gx, gy, 0, -1, bordesPared),
                        a10 * verticalAo(0f), a00 * verticalAo(0f),
                        a00 * verticalAo(1f), a10 * verticalAo(1f)
                    )
                } else emitirDesniveles(b, maze, gx, gy, gx, gy - 1, n)

                // Cara +Z: s va de x0 a x1.
                if (maze.isSolid(gx, gy + 1)) {
                    cara(
                        b, n,
                        pt(x0, fy, z1), pt(x1, fy, z1), pt(x1, cy, z1), pt(x0, cy, z1),
                        0f, 0f, -1f, BULTO_PARED, -ENSANCHE_PARED, CAPA_PARED,
                        Modo.PARED, bordesPared,
                        bordesDePared(maze, gx, gy, 0, 1, bordesPared),
                        a01 * verticalAo(0f), a11 * verticalAo(0f),
                        a11 * verticalAo(1f), a01 * verticalAo(1f)
                    )
                } else emitirDesniveles(b, maze, gx, gy, gx, gy + 1, n)
            }
        }

        val indices = b.idx.toArray()
        return Mesh(b.v.toArray(), indices, indices.size / 3)
    }

    /**
     * Entre dos casillas abiertas a distinta altura hay que cerrar el hueco.
     * Para no dibujarlo dos veces, el escalon de piso lo pone la casilla mas
     * ALTA y el de techo la mas BAJA.
     */
    private fun emitirDesniveles(
        b: Builder, maze: Maze,
        gx: Int, gy: Int, vx: Int, vy: Int,
        n: Int
    ) {
        if (!maze.inBounds(vx, vy) || maze.isSolid(vx, vy)) return
        val i = maze.index(gx, gy)
        val j = maze.index(vx, vy)
        val fy = maze.floorY(gx, gy)
        val fv = maze.floorY(vx, vy)
        val cy = fy + maze.ceilClearance[i]
        val cv = fv + maze.ceilClearance[j]

        val dx = vx - gx
        val dz = vy - gy
        // Plano del borde compartido y su recorrido.
        val ex: Float; val ez0: Float; val ez1: Float
        val ez: Float; val ex0: Float; val ex1: Float
        if (dx != 0) {
            ex = (if (dx > 0) gx + 1 else gx) * CELL
            ez0 = gy * CELL; ez1 = (gy + 1) * CELL
            ez = 0f; ex0 = 0f; ex1 = 0f
        } else {
            ez = (if (dz > 0) gy + 1 else gy) * CELL
            ex0 = gx * CELL; ex1 = (gx + 1) * CELL
            ex = 0f; ez0 = 0f; ez1 = 0f
        }

        val ao = cornerAo(maze, gx, gy) * 0.9f

        // ------------------------------------------------------ escalon de piso
        if (fy - fv > 0.01f) {
            // La normal mira hacia el vecino, que esta mas abajo.
            val nx = dx.toFloat(); val nz = dz.toFloat()
            if (dx != 0) {
                val za = if (dx > 0) ez0 else ez1
                val zb = if (dx > 0) ez1 else ez0
                cara(
                    b, n,
                    pt(ex, fv, za), pt(ex, fv, zb), pt(ex, fy, zb), pt(ex, fy, za),
                    nx, 0f, 0f, 0.10f, 0f, CAPA_PARED, Modo.CONTORNO, null, 0, ao, ao, ao, ao
                )
            } else {
                val xa = if (dz > 0) ex1 else ex0
                val xb = if (dz > 0) ex0 else ex1
                cara(
                    b, n,
                    pt(xa, fv, ez), pt(xb, fv, ez), pt(xb, fy, ez), pt(xa, fy, ez),
                    0f, 0f, nz, 0.10f, 0f, CAPA_PARED, Modo.CONTORNO, null, 0, ao, ao, ao, ao
                )
            }
            if (maze.hasLadder(gx, gy) || maze.hasLadder(vx, vy)) {
                escalera(b, gx, gy, dx, dz, fv, fy)
            }
        }

        // ------------------------------------------------------ escalon de techo
        if (cv - cy > 0.01f) {
            val nx = dx.toFloat(); val nz = dz.toFloat()
            if (dx != 0) {
                val za = if (dx > 0) ez0 else ez1
                val zb = if (dx > 0) ez1 else ez0
                cara(
                    b, n,
                    pt(ex, cy, za), pt(ex, cy, zb), pt(ex, cv, zb), pt(ex, cv, za),
                    nx, 0f, 0f, 0.10f, 0f, CAPA_TECHO, Modo.CONTORNO, null, 0, ao, ao, ao, ao
                )
            } else {
                val xa = if (dz > 0) ex1 else ex0
                val xb = if (dz > 0) ex0 else ex1
                cara(
                    b, n,
                    pt(xa, cy, ez), pt(xb, cy, ez), pt(xb, cv, ez), pt(xa, cv, ez),
                    0f, 0f, nz, 0.10f, 0f, CAPA_TECHO, Modo.CONTORNO, null, 0, ao, ao, ao, ao
                )
            }
        }
    }

    /**
     * Travesanos de madera apoyados contra el escalon, para que se vea de lejos
     * por donde se sube. Cada travesano es una caja de seis caras planas.
     */
    private fun escalera(
        b: Builder, gx: Int, gy: Int,
        dx: Int, dz: Int, yBajo: Float, yAlto: Float
    ) {
        val cxw = (gx + 0.5f) * CELL
        val czw = (gy + 0.5f) * CELL
        // Centro del borde compartido, corrido un poquito hacia el vacio.
        val bx = cxw + dx * (CELL * 0.5f + 0.06f)
        val bz = czw + dz * (CELL * 0.5f + 0.06f)
        val ancho = 0.78f
        val grosor = 0.07f
        val salida = 0.13f
        var y = yBajo + 0.24f
        var puestos = 0
        while (y < yAlto - 0.05f && puestos < 24) {
            // El travesano es perpendicular al sentido de subida.
            val ax = if (dx != 0) 0f else ancho * 0.5f
            val az = if (dx != 0) ancho * 0.5f else 0f
            val px = if (dx != 0) salida * 0.5f else 0f
            val pz = if (dx != 0) 0f else salida * 0.5f
            caja(
                b,
                bx - ax - px, y - grosor, bz - az - pz,
                bx + ax + px, y + grosor, bz + az + pz
            )
            y += 0.36f
            puestos++
        }
    }

    /** Caja alineada a los ejes, con las seis caras bien orientadas hacia afuera. */
    private fun caja(b: Builder, x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float) {
        val ao = 0.72f
        // +Y
        cara(b, 1, pt(x0, y1, z0), pt(x1, y1, z0), pt(x1, y1, z1), pt(x0, y1, z1), 0f, 1f, 0f, 0f, 0f, CAPA_PARED, Modo.CONTORNO, null, 0, ao, ao, ao, ao)
        // -Y
        cara(b, 1, pt(x0, y0, z0), pt(x0, y0, z1), pt(x1, y0, z1), pt(x1, y0, z0), 0f, -1f, 0f, 0f, 0f, CAPA_PARED, Modo.CONTORNO, null, 0, ao, ao, ao, ao)
        // -X (normal hacia -X)
        cara(b, 1, pt(x0, y0, z1), pt(x0, y0, z0), pt(x0, y1, z0), pt(x0, y1, z1), -1f, 0f, 0f, 0f, 0f, CAPA_PARED, Modo.CONTORNO, null, 0, ao, ao, ao, ao)
        // +X
        cara(b, 1, pt(x1, y0, z0), pt(x1, y0, z1), pt(x1, y1, z1), pt(x1, y1, z0), 1f, 0f, 0f, 0f, 0f, CAPA_PARED, Modo.CONTORNO, null, 0, ao, ao, ao, ao)
        // -Z
        cara(b, 1, pt(x0, y0, z0), pt(x1, y0, z0), pt(x1, y1, z0), pt(x0, y1, z0), 0f, 0f, -1f, 0f, 0f, CAPA_PARED, Modo.CONTORNO, null, 0, ao, ao, ao, ao)
        // +Z
        cara(b, 1, pt(x1, y0, z1), pt(x0, y0, z1), pt(x0, y1, z1), pt(x1, y1, z1), 0f, 0f, 1f, 0f, 0f, CAPA_PARED, Modo.CONTORNO, null, 0, ao, ao, ao, ao)
    }

}
