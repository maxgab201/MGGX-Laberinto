package com.mggx.laberinto.ui

import kotlin.math.cos
import kotlin.math.sin

/**
 * La cuenta que hace que el minimapa rote con la camara ("arriba" siempre
 * es hacia donde mira el jugador, no el norte fijo).
 *
 * Separada del Canvas de Compose a proposito, igual que BrazoPose.kt: asi se
 * puede verificar con aritmetica pura, sin tener que confiar a ojo en el
 * signo de una rotacion dibujada en pantalla.
 */
object MinimapMath {

    /**
     * Punto del mundo, relativo al jugador (`dx`, `dz` en metros), rotado a
     * coordenadas de pantalla donde el frente del jugador queda siempre
     * arriba (Y negativo).
     *
     * Coincide con la convencion de yaw del juego: el frente del jugador es
     * el vector `(sin(yaw), cos(yaw))` en el plano XZ (ver GameSession /
     * CaveRenderer). Un punto exactamente en esa direccion tiene que mapear
     * a `(0, -1)`: arriba, porque en un Canvas de Compose el eje Y crece
     * hacia abajo.
     */
    fun rotarHaciaArriba(dx: Float, dz: Float, yawDeg: Float): Pair<Float, Float> {
        val yaw = Math.toRadians(yawDeg.toDouble())
        val cosY = cos(yaw).toFloat()
        val sinY = sin(yaw).toFloat()
        val sx = dx * cosY - dz * sinY
        val sy = -(dx * sinY + dz * cosY)
        return sx to sy
    }
}
