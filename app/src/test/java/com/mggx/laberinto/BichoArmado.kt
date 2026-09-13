package com.mggx.laberinto

import com.mggx.laberinto.gl.ArmadoDeBichos
import com.mggx.laberinto.gl.PropMeshes
import com.mggx.laberinto.maze.MazeGenerator.EnemyKind

/**
 * Compone un bicho ENTERO en una sola malla, para poder mirarlo y medirlo.
 *
 * Es la pieza que faltaba. Hasta ahora se podian fotografiar las piezas
 * sueltas de un bicho (`EnemyMeshes`), pero el armado —que pieza va donde, con
 * que escala, con que giro— vivia adentro del renderer y no habia forma de
 * verlo. Y ahi estaban los errores.
 *
 * El bicho queda parado en el origen, con los pies en y=0 y mirando a +Z.
 *
 * Ojo con una cosa: las animaciones (aleteo, reptar) corren en el vertex
 * shader, asi que esto muestra la pose de REPOSO. Esta bien: lo que se esta
 * juzgando es el armado, no el movimiento.
 */
object BichoArmado {

    fun geometria(kind: EnemyKind, paso: Float = 0f, fase: Float = 0f): PropMeshes.Geometry {
        val piezas = ArmadoDeBichos.armar(kind, paso, fase).map { p ->
            val g = ArmadoDeBichos.geometria(p.malla)
            // El mismo orden que hace el shader: escala uniforme, giro en Y,
            // y recien despues la traslacion.
            PropMeshes.trasladar(
                PropMeshes.rotarY(
                    PropMeshes.escalar(g, p.escala, p.escala, p.escala),
                    Math.toDegrees(p.giro.toDouble()).toFloat()
                ),
                p.x, p.y, p.z
            )
        }
        return PropMeshes.combinar(*piezas.toTypedArray())
    }

    /** La caja que ocupa una malla: (minX, maxX, minY, maxY, minZ, maxZ). */
    fun caja(g: PropMeshes.Geometry): FloatArray {
        val out = floatArrayOf(
            Float.MAX_VALUE, -Float.MAX_VALUE,
            Float.MAX_VALUE, -Float.MAX_VALUE,
            Float.MAX_VALUE, -Float.MAX_VALUE
        )
        var i = 0
        while (i < g.vertices.size) {
            for (k in 0..2) {
                val v = g.vertices[i + k]
                if (v < out[k * 2]) out[k * 2] = v
                if (v > out[k * 2 + 1]) out[k * 2 + 1] = v
            }
            i += 6
        }
        return out
    }

    /** La caja de una sola pieza ya puesta en su lugar. */
    fun cajaDePieza(p: ArmadoDeBichos.Pieza): FloatArray = caja(
        PropMeshes.trasladar(
            PropMeshes.rotarY(
                PropMeshes.escalar(
                    ArmadoDeBichos.geometria(p.malla), p.escala, p.escala, p.escala
                ),
                Math.toDegrees(p.giro.toDouble()).toFloat()
            ),
            p.x, p.y, p.z
        )
    )
}
