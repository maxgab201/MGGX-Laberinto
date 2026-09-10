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
     *
     * OJO CON LOS SIGNOS. Que el frente quede arriba no alcanza: hay dos
     * transformaciones que lo cumplen, la rotacion (bien) y la rotacion con
     * espejo (mal). La version anterior de esta funcion usaba
     * `sx = dx * cosY - dz * sinY`, que arma la matriz
     * `[[cos, -sin], [-sin, -cos]]`, de determinante -1: un espejo. El
     * frente daba arriba igual, pero lo que estaba a la derecha del jugador
     * se dibujaba a la izquierda del mapa y al reves. Se ve en el telefono
     * como "el mapa esta al revés" y es facil de leer mal como un problema
     * de yaw.
     *
     * La invariante que lo fija, y que el test cuida, es la lateralidad: la
     * derecha del jugador en el juego, que en XZ es `(-cos(yaw), sin(yaw))`
     * (o sea `frente x arriba`), tiene que mapear a `(+1, 0)`: la derecha de
     * la pantalla. Con el menos de abajo el determinante da +1 y las dos
     * cosas valen a la vez.
     */
    fun rotarHaciaArriba(dx: Float, dz: Float, yawDeg: Float): Pair<Float, Float> {
        val yaw = Math.toRadians(yawDeg.toDouble())
        val cosY = cos(yaw).toFloat()
        val sinY = sin(yaw).toFloat()
        val sx = -(dx * cosY - dz * sinY)
        val sy = -(dx * sinY + dz * cosY)
        return sx to sy
    }

    /**
     * Deja un punto dentro del circulo del radar, empujandolo al borde si se
     * fue afuera.
     *
     * Sirve para las marcas que valen aunque esten lejos (la salida con la
     * Rosa de los Vientos, los tesoros con el Ojo de la Veta): en vez de
     * desaparecer del radar apenas te alejas, quedan pegadas al borde
     * apuntando para donde estan. Si el punto ya entraba, vuelve igual.
     *
     * Devuelve el punto y si hubo que pegarlo (para dibujarlo distinto: la
     * marca pegada al borde dice "esta para alla", no "esta aca").
     */
    fun pegarAlBorde(sx: Float, sy: Float, radio: Float): Triple<Float, Float, Boolean> {
        val d = kotlin.math.hypot(sx, sy)
        if (d <= radio || d < 1e-5f) return Triple(sx, sy, false)
        val k = radio / d
        return Triple(sx * k, sy * k, true)
    }
}
