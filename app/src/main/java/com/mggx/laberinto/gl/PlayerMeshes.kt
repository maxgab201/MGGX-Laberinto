package com.mggx.laberinto.gl

import com.mggx.laberinto.gl.PropMeshes.Geometry
import com.mggx.laberinto.gl.PropMeshes.combinar
import com.mggx.laberinto.gl.PropMeshes.escalar
import com.mggx.laberinto.gl.PropMeshes.lathe
import com.mggx.laberinto.gl.PropMeshes.trasladar

/**
 * El companiero de partida, visto desde afuera.
 *
 * A uno mismo nunca se lo dibuja entero (el juego es en primera persona y lo
 * unico propio que se ve son los brazos, ver ArmsMesh), asi que estas mallas
 * son solo para los OTROS de la sala.
 *
 * Van aparte de EnemyMeshes a proposito: un companiero no es un bicho, y
 * cuando se lo ve de lejos en un pasillo tiene que leerse en seguida como
 * "una persona", no como algo que ataca. Por eso la silueta es alta y
 * angosta, con el casco bien marcado.
 *
 * Igual que las demas, cada pieza se apoya en y=0 y llega hasta y=1: el
 * `scale` que le pasa el renderer es directamente su altura en metros.
 */
object PlayerMeshes {

    /**
     * Cuerpo con el abrigo puesto: botas, piernas, la panza del abrigo y los
     * hombros, rematado en el cuello.
     *
     * Es un cuerpo de revolucion (una sola pasada de lathe). De lejos, que es
     * como se lo ve casi siempre, se lee igual que una silueta modelada
     * hombro por hombro, y cuesta la decima parte de triangulos.
     */
    fun mineroCuerpo(): Geometry {
        fun pieza(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float) =
            trasladar(DetailMeshes.roundedBox(sx,sy,sz,minOf(sx,sy,sz)*0.28f),x,y,z)
        return combinar(
            pieza(-0.095f,0.055f,0.035f,0.14f,0.11f,0.24f),
            pieza(0.095f,0.055f,0.035f,0.14f,0.11f,0.24f),
            pieza(-0.09f,0.28f,0f,0.14f,0.37f,0.16f),
            pieza(0.09f,0.28f,0f,0.14f,0.37f,0.16f),
            pieza(0f,0.61f,0f,0.36f,0.32f,0.22f),
            pieza(0f,0.47f,0f,0.37f,0.05f,0.24f),
            pieza(-0.23f,0.59f,0f,0.12f,0.35f,0.14f),
            pieza(0.23f,0.59f,0f,0.12f,0.35f,0.14f),
            pieza(0f,0.835f,0.02f,0.17f,0.17f,0.18f),
            pieza(0f,0.63f,-0.16f,0.25f,0.25f,0.12f),
            pieza(-0.095f,0.61f,0.12f,0.09f,0.10f,0.025f),
            pieza(0.095f,0.61f,0.12f,0.09f,0.10f,0.025f)
        )
    }

    /**
     * Casco de minero con visera.
     *
     * La visera va adelante (hacia +Z, que es hacia donde mira el modelo) y
     * es lo que permite saber de una para donde esta mirando el companiero,
     * aun de lejos y sin verle la cara.
     */
    fun mineroCasco(): Geometry {
        val domo = lathe(
            arrayOf(
                floatArrayOf(0.00f, 0.00f),
                floatArrayOf(0.30f, 0.04f),   // el ala del casco
                floatArrayOf(0.27f, 0.16f),
                floatArrayOf(0.20f, 0.44f),
                floatArrayOf(0.11f, 0.72f),
                floatArrayOf(0.00f, 0.86f)
            ),
            segmentos = 16
        )
        // Visera: una placa fina que sobresale por delante del ala.
        val visera = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.34f, 0.07f, 0.26f),
            0f, 0.10f, 0.30f
        )
        return combinar(domo, visera)
    }
}

