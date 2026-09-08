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
 * Igual que las demas, el cuerpo entero se apoya en y=0 y llega hasta y=1: el
 * `scale` que le pasa el renderer es directamente su altura en metros. Antes
 * era un apilado de cajas redondeadas sin brazos; ahora son cuerpos de
 * revolucion (piernas, abrigo y brazos colgando a los costados), con las
 * mismas herramientas que EnemyMeshes/StructureMeshes.
 */
object PlayerMeshes {

    /**
     * Una pierna con la bota puesta, de la suela a donde se esconde bajo el
     * ruedo del abrigo. Se usa dos veces (una por lado, ver [mineroCuerpo]).
     */
    private fun pierna(): Geometry = lathe(
        arrayOf(
            floatArrayOf(0.000f, 0.000f),   // suela, apoyada en el piso
            floatArrayOf(0.074f, 0.012f),
            floatArrayOf(0.068f, 0.080f),   // cuerpo de la bota
            floatArrayOf(0.070f, 0.160f),   // cana de la bota
            floatArrayOf(0.050f, 0.220f),   // tobillo
            floatArrayOf(0.048f, 0.340f),   // gemba
            floatArrayOf(0.058f, 0.430f),   // rodilla
            floatArrayOf(0.064f, 0.530f),   // muslo, con el pantalon holgado
            floatArrayOf(0.028f, 0.570f),
            floatArrayOf(0.000f, 0.610f)    // se cierra en punta bajo el abrigo
        ),
        segmentos = 10
    )

    /**
     * El abrigo puesto: de la cadera a los hombros y el cuello, mas ancho de
     * hombros que de cintura para que se lea como torso y no como un tubo.
     */
    private fun torso(): Geometry = lathe(
        arrayOf(
            floatArrayOf(0.000f, 0.430f),   // se cierra por encima de las piernas
            floatArrayOf(0.155f, 0.460f),   // ruedo del abrigo, abierto sobre la cadera
            floatArrayOf(0.128f, 0.560f),   // cintura entallada
            floatArrayOf(0.150f, 0.650f),   // pecho y bolsillos
            floatArrayOf(0.172f, 0.740f),   // hombros, lo mas ancho
            floatArrayOf(0.145f, 0.800f),
            floatArrayOf(0.072f, 0.840f),   // cuello
            floatArrayOf(0.000f, 0.860f)    // se cierra bajo donde va el casco
        ),
        segmentos = 12
    )

    /**
     * Perfil de un brazo colgando, en su propio eje local (de -0.5 el puno a
     * +0.5 el hombro). [mineroBrazo] lo escala a su largo real y lo ubica al
     * costado del cuerpo.
     */
    private fun brazoPerfil(): Geometry = lathe(
        arrayOf(
            floatArrayOf(0.000f, -0.50f),   // punta del puno
            floatArrayOf(0.050f, -0.42f),   // puno
            floatArrayOf(0.038f, -0.26f),   // muneca
            floatArrayOf(0.044f, -0.02f),   // manga del antebrazo
            floatArrayOf(0.050f, 0.24f),    // manga del brazo
            floatArrayOf(0.060f, 0.40f),    // hombrera
            floatArrayOf(0.000f, 0.50f)     // se cierra bajo el hombro del abrigo
        ),
        segmentos = 8
    )

    /** Un brazo ya ubicado: `lado` es -1f (izquierdo) o +1f (derecho). */
    private fun mineroBrazo(lado: Float): Geometry =
        trasladar(escalar(brazoPerfil(), 1f, 0.34f, 1f), 0.205f * lado, 0.610f, 0f)

    /**
     * Cuerpo completo: piernas, abrigo y los dos brazos colgando a los
     * costados. Antes no tenia brazos; ahora que el minero se ve de lejos en
     * los pasillos, sin ellos se leia como un maniqui.
     */
    fun mineroCuerpo(): Geometry {
        val unaPierna = pierna()
        return combinar(
            trasladar(unaPierna, -0.105f, 0f, 0f),
            trasladar(unaPierna, 0.105f, 0f, 0f),
            torso(),
            mineroBrazo(-1f),
            mineroBrazo(1f)
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
