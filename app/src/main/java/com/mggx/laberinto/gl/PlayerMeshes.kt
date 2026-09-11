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
     * La mochila que lleva a la espalda.
     *
     * De todo lo que se le puede agregar a un modelo que se ve de lejos, esto
     * es lo que mas rinde: no se distingue ni la cara ni la ropa a diez metros
     * en un pasillo oscuro, pero la SILUETA si. Un bulto en la espalda convierte
     * un tubo con patas en alguien que anda cargando cosas.
     *
     * Va hacia -Z porque el modelo mira hacia +Z (la visera del casco marca el
     * frente, ver [mineroCasco]).
     */
    private fun mochila(): Geometry {
        val cuerpo = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.22f, 0.28f, 0.15f),
            0f, 0.655f, -0.145f
        )
        // La tapa con la correa, un poco mas angosta.
        val tapa = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.20f, 0.06f, 0.13f),
            0f, 0.790f, -0.150f
        )
        // El rollo de soga atado arriba.
        val soga = trasladar(
            escalar(lathe(
                arrayOf(
                    floatArrayOf(0.00f, -0.50f),
                    floatArrayOf(0.45f, -0.30f),
                    floatArrayOf(0.50f, 0.00f),
                    floatArrayOf(0.45f, 0.30f),
                    floatArrayOf(0.00f, 0.50f)
                ),
                segmentos = 8
            ), 0.17f, 0.09f, 0.17f),
            0f, 0.845f, -0.150f
        )
        return combinar(cuerpo, tapa, soga)
    }

    /**
     * El pico colgado a la espalda.
     *
     * Es lo que dice "minero" de un vistazo, sin leer nada.
     *
     * La cabeza del pico va ATRAVESADA (a lo ancho, sobre los hombros) y no
     * apuntando hacia atras. Las dos cosas a la vez: es como se carga un pico
     * de verdad, y ademas no convierte al companiero en una silueta que
     * sobresale medio metro por la espalda. Puesta hacia atras medía 46 cm de
     * saliente en un cuerpo de 1,72 m, que de costado se veia como una cola.
     */
    private fun picoALaEspalda(): Geometry {
        val cabo = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.028f, 0.40f, 0.028f),
            0.090f, 0.650f, -0.205f
        )
        val cabeza = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.230f, 0.030f, 0.022f),
            0.075f, 0.845f, -0.205f
        )
        // La punta, del lado largo de la cabeza.
        val punta = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.060f, 0.018f, 0.016f),
            -0.065f, 0.845f, -0.205f
        )
        return combinar(cabo, cabeza, punta)
    }

    /** El cinturon, con la hebilla marcada adelante. */
    private fun cinturon(): Geometry {
        val tira = trasladar(
            escalar(lathe(
                arrayOf(
                    floatArrayOf(0.000f, -0.50f),
                    floatArrayOf(0.145f, -0.36f),
                    floatArrayOf(0.150f, 0.00f),
                    floatArrayOf(0.145f, 0.36f),
                    floatArrayOf(0.000f, 0.50f)
                ),
                segmentos = 12
            ), 1f, 0.062f, 1f),
            0f, 0.560f, 0f
        )
        val hebilla = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.058f, 0.048f, 0.030f),
            0f, 0.560f, 0.142f
        )
        return combinar(tira, hebilla)
    }

    /**
     * Cuerpo completo: piernas, abrigo, brazos, cinturon, mochila y el pico
     * cruzado a la espalda.
     *
     * Fue creciendo por etapas y cada una respondia a lo mismo: de lejos, en un
     * pasillo, lo unico que se lee es la silueta. Primero era un apilado de
     * cajas sin brazos (un maniqui); despues cuerpos de revolucion con brazos;
     * ahora ademas carga cosas, que es lo que lo vuelve un minero y no una
     * persona generica.
     */
    fun mineroCuerpo(): Geometry {
        val unaPierna = pierna()
        return combinar(
            trasladar(unaPierna, -0.105f, 0f, 0f),
            trasladar(unaPierna, 0.105f, 0f, 0f),
            torso(),
            mineroBrazo(-1f),
            mineroBrazo(1f),
            cinturon(),
            mochila(),
            picoALaEspalda()
        )
    }

    /**
     * La cabeza, en malla aparte del cuerpo.
     *
     * Va separada porque el renderer pinta cada instancia de UN color: con la
     * cabeza adentro del cuerpo, la cara saldria del color del abrigo. En malla
     * propia se dibuja con tono de piel y el companiero deja de ser un traje
     * con casco encima.
     *
     * Mide 1 de alto y se apoya en y=0, como todas: el renderer la escala y la
     * pone a la altura del cuello.
     */
    fun mineroCabeza(): Geometry {
        val craneo = lathe(
            arrayOf(
                floatArrayOf(0.000f, 0.000f),   // base del cuello
                floatArrayOf(0.230f, 0.060f),
                floatArrayOf(0.260f, 0.170f),   // mandibula
                floatArrayOf(0.330f, 0.380f),   // pomulos, lo mas ancho
                floatArrayOf(0.330f, 0.640f),
                floatArrayOf(0.250f, 0.860f),
                floatArrayOf(0.000f, 1.000f)    // coronilla
            ),
            segmentos = 12
        )
        // Nariz: es lo que le da frente a la cara. Sin ella, de lejos la
        // cabeza es una bola y no se sabe si viene o va. Tiene que sobresalir
        // de verdad del craneo: una nariz que no pasa el perfil de la cara no
        // se ve nunca, y era justo lo que pasaba con la primera version.
        val nariz = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.100f, 0.130f, 0.150f),
            0f, 0.470f, 0.320f
        )
        // Ceja: una barra sobre los ojos. En una cueva la luz viene casi
        // siempre de abajo o de costado, y esta sombra es lo que hace que la
        // cara tenga ojos en vez de ser una superficie lisa.
        val ceja = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.400f, 0.060f, 0.090f),
            0f, 0.590f, 0.275f
        )
        // Barba: el bulto de abajo de la cara. Un minero con barba se lee como
        // persona mucho antes que una cabeza lisa.
        val barba = trasladar(
            escalar(lathe(
                arrayOf(
                    floatArrayOf(0.000f, -0.50f),
                    floatArrayOf(0.380f, -0.20f),
                    floatArrayOf(0.420f, 0.10f),
                    floatArrayOf(0.300f, 0.40f),
                    floatArrayOf(0.000f, 0.50f)
                ),
                segmentos = 10
            ), 0.64f, 0.38f, 0.50f),
            0f, 0.230f, 0.090f
        )
        return combinar(craneo, ceja, nariz, barba)
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
        // La lampara montada al frente del casco. El brillo lo pone el
        // renderer aparte; esto es la carcasa, para que la luz salga de algo y
        // no de la nada.
        val farol = trasladar(
            escalar(lathe(
                arrayOf(
                    floatArrayOf(0.00f, -0.50f),
                    floatArrayOf(0.42f, -0.40f),
                    floatArrayOf(0.50f, 0.20f),
                    floatArrayOf(0.36f, 0.50f)
                ),
                segmentos = 10
            ), 0.22f, 0.20f, 0.22f),
            0f, 0.32f, 0.21f
        )
        return combinar(domo, visera, farol)
    }
}
