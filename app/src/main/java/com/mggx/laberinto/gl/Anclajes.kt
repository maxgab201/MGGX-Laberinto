package com.mggx.laberinto.gl

import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.maze.Maze

/**
 * Como se clavan a la roca las cosas que van colgadas de una pared.
 *
 * Esta aparte del renderer por una sola razon: aca no hay nada de OpenGL, asi
 * que se puede probar contra la malla de verdad en un test de JVM. Y hace
 * falta probarlo, porque el bug que arregla era invisible desde el codigo:
 *
 * el plano nominal de una pared es el borde de la casilla, pero la roca que se
 * VE no esta ahi. El ruido la corre hasta 30 cm para cualquiera de los dos
 * lados y la panza del tunel 22 cm mas hacia afuera, las dos cosas con su
 * maximo justo a media altura — o sea exactamente donde se cuelga una
 * antorcha. Colgarla a un corrimiento fijo desde el centro de la casilla la
 * dejaba flotando hasta medio metro en el aire.
 */
object Anclajes {

    /** Alto total de la antorcha en metros (el modelo mide 1 de alto). */
    const val ALTO_ANTORCHA = 0.48f

    /** A que altura del piso se clava la base de la antorcha, en metros. */
    const val ALTURA_ANTORCHA = 1.75f

    /**
     * Cuanto se hunde la placa de anclaje dentro de la roca, en metros.
     *
     * Un par de centimetros: lo justo para que no se vea una luz entre la
     * placa y la pared por mas que la roca este abollada.
     */
    const val HUNDIDO = 0.04f

    /** Alto de la llama en metros, antes del parpadeo. */
    const val ALTO_LLAMA = 0.22f

    /**
     * Lo que ocupa la antorcha por ENCIMA de su base: el poste hasta la boca
     * del cuenco, mas la llama. Es lo que tiene que entrar bajo el techo.
     */
    const val ALTO_TOTAL = ALTO_ANTORCHA * StructureMeshes.ALTURA_DEL_FUEGO + ALTO_LLAMA

    /** Aire que se le deja a la punta de la llama antes de la roca del techo. */
    const val MARGEN_TECHO = 0.12f

    /**
     * Lo mas cerca del centro de la casilla que se le permite quedar, en
     * fraccion de casilla.
     *
     * Es el tope por si la roca se abolla mucho hacia adentro: la antorcha la
     * sigue, pero no hasta meterse en el medio del pasillo.
     */
    const val MINIMO = 0.34f

    /**
     * Donde va clavada la antorcha de una casilla.
     *
     * Devuelve false si la casilla se quedo SIN pared (el jugador la rompio con
     * el pico). Una antorcha de pared sin pared no se dibuja: lo unico que
     * podria hacer es quedar colgada del aire.
     *
     * @param out recibe (x, y de la base, z, giro hacia la pared).
     */
    fun antorcha(m: Maze, gx: Int, gy: Int, out: FloatArray): Boolean {
        var sx = 0; var sy = 0
        if (m.isSolid(gx - 1, gy)) sx = -1
        else if (m.isSolid(gx + 1, gy)) sx = 1
        else if (m.isSolid(gx, gy - 1)) sy = -1
        else if (m.isSolid(gx, gy + 1)) sy = 1
        else return false

        val c = GameSession.CELL
        val cx = (gx + 0.5f) * c
        val cz = (gy + 0.5f) * c

        // Cuanto techo hay de verdad, ya descontada la panza que se abolla
        // hacia abajo. En una gatera no entra una antorcha a 1,75 m: quedaba
        // clavada ARRIBA del techo, con la llama del otro lado de la roca y la
        // luz saliendo de adentro de la pared. Es el otro caso de "antorcha
        // flotando", y era mas feo que el de la pared.
        val fy = WorldMesh.floorHeight(m, gx, gy)
        val libre = Maze.altoLibreReal(m.ceilY(gx, gy) - fy)
        if (libre < ALTO_TOTAL + MARGEN_TECHO + 0.08f) return false
        val baseY = fy + kotlin.math.min(ALTURA_ANTORCHA, libre - ALTO_TOTAL - MARGEN_TECHO)
        // Se mide la roca a la altura de la PLACA, que es la pieza que tiene
        // que tocarla, no a la del mastil ni a la de la llama.
        val yPlaca = baseY + ALTO_ANTORCHA * StructureMeshes.ALTURA_DE_LA_PLACA

        val enX = sx != 0
        val centro = if (enX) cx else cz
        val cara = WorldMesh.realWallFace(m, gx, gy, sx, sy, yPlaca, if (enX) cz else cx)
        val dir = (sx + sy).toFloat()

        // Desde la cara se vuelve hacia adentro lo que mide el brazo, menos lo
        // que se quiere hundir la placa.
        val retroceso = StructureMeshes.LARGO_BRAZO * ALTO_ANTORCHA - HUNDIDO
        val lejos = ((cara - centro) * dir - retroceso).coerceAtLeast(c * MINIMO)

        out[0] = if (enX) centro + dir * lejos else cx
        out[1] = baseY
        out[2] = if (enX) cz else centro + dir * lejos
        // El brazo del soporte esta modelado hacia -Z, asi que se gira para que
        // se clave en la pared contra la que quedo apoyada.
        out[3] = Math.atan2(sx.toDouble(), sy.toDouble()).toFloat() + Math.PI.toFloat()
        return true
    }

    /** Donde queda la placa de anclaje de una antorcha ya ubicada por [antorcha]. */
    fun placaDe(out: FloatArray, dentro: FloatArray) {
        val giro = out[3]
        // El brazo sale hacia -Z del modelo; girado por `giro` da esta direccion.
        val largo = StructureMeshes.LARGO_BRAZO * ALTO_ANTORCHA
        val dx = -Math.sin(giro.toDouble()).toFloat() * largo
        val dz = -Math.cos(giro.toDouble()).toFloat() * largo
        dentro[0] = out[0] + dx
        dentro[1] = out[1] + ALTO_ANTORCHA * StructureMeshes.ALTURA_DE_LA_PLACA
        dentro[2] = out[2] + dz
    }
}
