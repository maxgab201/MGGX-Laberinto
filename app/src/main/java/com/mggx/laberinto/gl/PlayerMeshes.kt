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
 * "una persona", no como algo que ataca.
 *
 * Igual que las demas, cada pieza se apoya en y=0 y llega hasta y=1: el
 * `scale` que le pasa el renderer es directamente su altura en metros.
 *
 * ---
 *
 * **LA REGLA QUE MAS IMPORTA ACA: un `lathe` da seccion CIRCULAR.**
 *
 * Un cuerpo de revolucion que mide 60 cm de hombro a hombro mide tambien 60 cm
 * de pecho a espalda, y eso no es una persona: es un barril. Una persona es
 * ancha y CHATA (unos 45 cm de ancho por 25 de fondo). Por eso todas las
 * piezas del cuerpo pasan por un `escalar(..., 1f, 1f, k)` con k menor que 1:
 * el torno da la silueta y el achatado la convierte en un cuerpo.
 *
 * Esto se descubrio mirando el modelo con VisorDeMallas (ver RetratosTest). Por
 * numeros estaba impecable —proporciones en rango, caras bien orientadas— y en
 * la foto era un globo con patas.
 */
object PlayerMeshes {

    /**
     * Cuanto se achata el cuerpo en Z (fondo) respecto del ancho.
     *
     * Un torso humano mide mas o menos 45 cm de ancho por 25 de fondo, o sea
     * un 55%. Es el numero que convierte el barril en persona.
     */
    private const val FONDO_CUERPO = 0.56f

    /** Lo mismo para la cabeza, que es al reves: mas honda que ancha. */
    private const val FONDO_CABEZA = 1.18f

    /**
     * Una pierna con la bota puesta, de la suela a donde se esconde bajo el
     * ruedo del abrigo. Se usa dos veces (una por lado, ver [mineroCuerpo]).
     *
     * La bota es lo unico mas ancho que la pierna: es lo que hace que se lea
     * como calzado y no como un tubo que termina en el piso.
     */
    private fun pierna(): Geometry = escalar(
        lathe(
            arrayOf(
                floatArrayOf(0.000f, 0.000f),   // suela, apoyada en el piso
                floatArrayOf(0.060f, 0.008f),
                floatArrayOf(0.072f, 0.030f),   // el ancho de la bota
                floatArrayOf(0.070f, 0.090f),
                floatArrayOf(0.062f, 0.140f),   // cana de la bota
                floatArrayOf(0.046f, 0.185f),   // tobillo, lo mas fino
                floatArrayOf(0.048f, 0.300f),   // pantorrilla
                floatArrayOf(0.054f, 0.360f),
                floatArrayOf(0.046f, 0.430f),   // rodilla
                floatArrayOf(0.058f, 0.500f),   // muslo
                floatArrayOf(0.062f, 0.560f),
                floatArrayOf(0.040f, 0.600f),
                floatArrayOf(0.000f, 0.620f)    // se cierra bajo el abrigo
            ),
            segmentos = 10
        ),
        1f, 1f, 0.88f   // el pie es un poco mas largo que ancho: casi redondo
    )

    /**
     * El abrigo puesto: de la cadera a los hombros y el cuello.
     *
     * El perfil tiene tres cosas que hacen que se lea como un torso y no como
     * un tubo: se ensancha en el ruedo (el abrigo cae abierto sobre la cadera),
     * se mete en la cintura, y arriba se ENSANCHA DE GOLPE y se corta casi
     * plano. Ese corte plano son los hombros; sin el, el cuerpo termina en
     * punta y parece un huevo.
     */
    private fun torso(): Geometry = escalar(
        lathe(
            arrayOf(
                floatArrayOf(0.000f, 0.400f),   // cierra por encima de las piernas
                floatArrayOf(0.120f, 0.410f),
                floatArrayOf(0.148f, 0.450f),   // ruedo del abrigo, abierto
                floatArrayOf(0.136f, 0.500f),
                floatArrayOf(0.120f, 0.560f),   // cintura, lo mas angosto
                floatArrayOf(0.132f, 0.620f),
                floatArrayOf(0.148f, 0.680f),   // pecho
                floatArrayOf(0.158f, 0.730f),
                floatArrayOf(0.162f, 0.780f),   // hombros, lo mas ancho
                floatArrayOf(0.150f, 0.815f),   // el corte de los hombros
                floatArrayOf(0.098f, 0.840f),   // trapecio bajando al cuello
                floatArrayOf(0.058f, 0.870f),   // cuello
                floatArrayOf(0.052f, 0.930f),
                floatArrayOf(0.000f, 0.950f)
            ),
            segmentos = 14
        ),
        1f, 1f, FONDO_CUERPO
    )

    /**
     * Perfil de un brazo colgando, en su propio eje local (de -0.5 el puno a
     * +0.5 el hombro). [mineroBrazo] lo escala a su largo real y lo ubica al
     * costado del cuerpo.
     *
     * El puno se ensancha: es lo unico que distingue un brazo con mano de un
     * palo, y a la distancia a la que se ve esto, es suficiente.
     */
    private fun brazoPerfil(): Geometry = lathe(
        arrayOf(
            floatArrayOf(0.000f, -0.500f),
            floatArrayOf(0.038f, -0.470f),
            floatArrayOf(0.050f, -0.420f),   // el puno, mas gordo que la muneca
            floatArrayOf(0.046f, -0.380f),
            floatArrayOf(0.034f, -0.330f),   // muneca, lo mas fino
            floatArrayOf(0.040f, -0.240f),
            floatArrayOf(0.044f, -0.020f),   // antebrazo
            floatArrayOf(0.040f, 0.060f),    // codo
            floatArrayOf(0.050f, 0.240f),    // biceps
            floatArrayOf(0.062f, 0.400f),    // hombrera
            floatArrayOf(0.054f, 0.460f),
            floatArrayOf(0.000f, 0.500f)
        ),
        segmentos = 8
    )

    /**
     * Un brazo ya ubicado: `lado` es -1f (izquierdo) o +1f (derecho).
     *
     * Va METIDO bajo la linea del hombro (y no colgando de un punto mas abajo)
     * para que el brazo salga del cuerpo en vez de estar apoyado al lado.
     */
    private fun mineroBrazo(lado: Float): Geometry =
        trasladar(escalar(brazoPerfil(), 1f, 0.40f, 1f), 0.196f * lado, 0.600f, 0.010f)

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
            escalar(DetailMeshes.roundedBox(), 0.200f, 0.250f, 0.120f),
            0f, 0.665f, -0.128f
        )
        // La tapa, un poco mas chica y con su propia sombra.
        val tapa = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.185f, 0.050f, 0.105f),
            0f, 0.782f, -0.132f
        )
        // El rollo de soga atado arriba.
        val soga = trasladar(
            escalar(
                lathe(
                    arrayOf(
                        floatArrayOf(0.00f, -0.50f),
                        floatArrayOf(0.42f, -0.32f),
                        floatArrayOf(0.50f, 0.00f),
                        floatArrayOf(0.42f, 0.32f),
                        floatArrayOf(0.00f, 0.50f)
                    ),
                    segmentos = 8
                ),
                0.150f, 0.080f, 0.150f
            ),
            0f, 0.828f, -0.132f
        )
        // Las dos correas que bajan por el pecho: cierran la lectura de
        // "mochila puesta" en vez de "caja flotando detras".
        val correaIzq = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.026f, 0.230f, 0.026f),
            -0.072f, 0.700f, 0.082f
        )
        val correaDer = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.026f, 0.230f, 0.026f),
            0.072f, 0.700f, 0.082f
        )
        return combinar(cuerpo, tapa, soga, correaIzq, correaDer)
    }

    /**
     * El pico colgado a la espalda.
     *
     * Es lo que dice "minero" de un vistazo, sin leer nada.
     *
     * La cabeza del pico va ATRAVESADA (a lo ancho, sobre los hombros) y no
     * apuntando hacia atras: es como se carga un pico de verdad, y ademas no
     * convierte al companiero en una silueta que sobresale medio metro por la
     * espalda. Puesta hacia atras medía 46 cm de saliente en un cuerpo de
     * 1,72 m y de costado se veia como una cola.
     */
    private fun picoALaEspalda(): Geometry {
        val cabo = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.026f, 0.370f, 0.026f),
            0.082f, 0.660f, -0.192f
        )
        // La cabeza: el lado largo es la punta, el corto es la pala.
        val punta = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.180f, 0.026f, 0.020f),
            0.010f, 0.838f, -0.192f
        )
        val pala = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.070f, 0.040f, 0.022f),
            0.150f, 0.838f, -0.192f
        )
        return combinar(cabo, punta, pala)
    }

    /** El cinturon, con la hebilla marcada adelante. */
    private fun cinturon(): Geometry {
        val tira = escalar(
            trasladar(
                escalar(
                    lathe(
                        arrayOf(
                            floatArrayOf(0.000f, -0.50f),
                            floatArrayOf(0.128f, -0.34f),
                            floatArrayOf(0.132f, 0.00f),
                            floatArrayOf(0.128f, 0.34f),
                            floatArrayOf(0.000f, 0.50f)
                        ),
                        segmentos = 14
                    ),
                    1f, 0.055f, 1f
                ),
                0f, 0.562f, 0f
            ),
            1f, 1f, FONDO_CUERPO
        )
        val hebilla = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.052f, 0.044f, 0.026f),
            0f, 0.562f, 0.072f
        )
        return combinar(tira, hebilla)
    }

    /**
     * Cuerpo completo: piernas, abrigo, brazos, cinturon, mochila y el pico
     * cruzado a la espalda.
     */
    fun mineroCuerpo(): Geometry {
        val unaPierna = pierna()
        return combinar(
            trasladar(unaPierna, -0.082f, 0f, 0f),
            trasladar(unaPierna, 0.082f, 0f, 0f),
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
     * cabeza adentro del cuerpo, la cara saldria del color del abrigo y el
     * companiero seria un traje con casco encima.
     *
     * Mide 1 de alto y se apoya en y=0, como todas.
     *
     * El perfil termina en una coronilla REDONDEADA y no en punta: los ultimos
     * tres anillos se van cerrando de a poco. Con el perfil viejo, que saltaba
     * de 0.25 a 0.00 de una, la cabeza salia como un huevo terminado en pico.
     */
    fun mineroCabeza(): Geometry {
        val craneo = escalar(
            lathe(
                arrayOf(
                    floatArrayOf(0.000f, 0.000f),   // base del cuello
                    floatArrayOf(0.150f, 0.020f),
                    floatArrayOf(0.155f, 0.110f),   // cuello
                    floatArrayOf(0.235f, 0.185f),   // maxilar
                    floatArrayOf(0.290f, 0.290f),   // mandibula
                    floatArrayOf(0.330f, 0.430f),   // pomulos
                    floatArrayOf(0.345f, 0.560f),   // sienes, lo mas ancho
                    floatArrayOf(0.340f, 0.680f),
                    floatArrayOf(0.310f, 0.790f),
                    floatArrayOf(0.255f, 0.880f),   // la curva de la coronilla
                    floatArrayOf(0.175f, 0.945f),
                    floatArrayOf(0.092f, 0.985f),
                    floatArrayOf(0.000f, 1.000f)
                ),
                segmentos = 14
            ),
            1f, 1f, FONDO_CABEZA
        )
        // Nariz: una cuna, no un ladrillo. Tres anillos que se afinan hacia
        // afuera y hacia abajo. Es lo que le da FRENTE a la cara: sin ella, de
        // lejos la cabeza es una bola y no se sabe si viene o va.
        val nariz = trasladar(
            escalar(
                lathe(
                    arrayOf(
                        floatArrayOf(0.000f, -0.50f),
                        floatArrayOf(0.34f, -0.36f),
                        floatArrayOf(0.50f, -0.05f),
                        floatArrayOf(0.40f, 0.28f),
                        floatArrayOf(0.000f, 0.50f)
                    ),
                    segmentos = 8
                ),
                0.105f, 0.175f, 0.160f
            ),
            // OJO: al achatar el craneo en Z (FONDO_CABEZA lo estira a 1.18)
            // el perfil de la cara se corrio hacia adelante, y una nariz puesta
            // a 0.33 quedaba ADENTRO del craneo. Una nariz que no pasa el
            // perfil no se ve nunca, y es justo lo unico que dice para donde
            // mira la cabeza.
            0f, 0.470f, 0.400f
        )
        // Ceja: una barra baja sobre los ojos. En una cueva la luz viene casi
        // siempre de abajo o de costado, y esta sombra es lo que hace que la
        // cara tenga ojos en vez de ser una superficie lisa.
        val ceja = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.330f, 0.045f, 0.075f),
            0f, 0.590f, 0.360f
        )
        // Barba: el bulto de abajo de la cara. Un minero con barba se lee como
        // persona mucho antes que una cabeza lisa.
        val barba = trasladar(
            escalar(
                lathe(
                    arrayOf(
                        floatArrayOf(0.000f, -0.50f),
                        floatArrayOf(0.300f, -0.30f),
                        floatArrayOf(0.440f, -0.05f),
                        floatArrayOf(0.430f, 0.18f),
                        floatArrayOf(0.280f, 0.40f),
                        floatArrayOf(0.000f, 0.50f)
                    ),
                    segmentos = 12
                ),
                0.620f, 0.420f, 0.560f
            ),
            0f, 0.265f, 0.085f
        )
        // Orejas: dos discos chatos a los costados. Cuestan doce triangulos y
        // son lo que termina de sacarle el aire de maniqui.
        val oreja = escalar(
            lathe(
                arrayOf(
                    floatArrayOf(0.000f, -0.50f),
                    floatArrayOf(0.34f, -0.28f),
                    floatArrayOf(0.42f, 0.10f),
                    floatArrayOf(0.000f, 0.50f)
                ),
                segmentos = 6
            ),
            0.070f, 0.150f, 0.110f
        )
        return combinar(
            craneo, ceja, nariz, barba,
            trasladar(oreja, -0.330f, 0.490f, 0.020f),
            trasladar(oreja, 0.330f, 0.490f, 0.020f)
        )
    }

    /**
     * Casco de minero: una copa redondeada con el ala alrededor.
     *
     * Dos cosas que estaban mal y se vieron recien al mirarlo con el visor:
     *
     *  - **Era un cono.** El perfil viejo iba de 0.30 de radio a 0.00 en linea
     *    casi recta, y un cono no es un casco. Ahora la copa se mantiene ancha
     *    hasta media altura y recien ahi curva, que es lo que la vuelve un domo.
     *  - **Medía 52 cm de alto** en un cuerpo de 1,72 m (el renderer lo escalaba
     *    a 0.30 de la altura del cuerpo). Un casco mide unos 16 cm. Ahora el
     *    modelo esta pensado para escalarse a ~0.105 del alto del companiero.
     *
     * La visera va adelante (hacia +Z, que es hacia donde mira el modelo) y es
     * lo que permite saber de una para donde esta mirando el companiero, aun de
     * lejos y sin verle la cara.
     */
    fun mineroCasco(): Geometry {
        // OJO CON LAS PROPORCIONES. La malla mide 1 de ALTO y el renderer la
        // escala por esa altura, asi que el ancho se escribe en unidades de
        // altura: un radio de 0.47 da un casco tan ancho como alto, y eso es
        // una bala, no un casco. Un casco de minero mide unos 28 cm de ancho
        // por 16 de alto, o sea que el radio tiene que ser ~0.85 de la altura.
        val copa = lathe(
            arrayOf(
                floatArrayOf(0.000f, 0.000f),   // cierra el ala por abajo
                floatArrayOf(0.760f, 0.020f),   // el ala, que sobresale
                floatArrayOf(0.860f, 0.090f),   // canto del ala, lo mas ancho
                floatArrayOf(0.800f, 0.150f),
                floatArrayOf(0.690f, 0.210f),   // donde arranca la copa
                floatArrayOf(0.670f, 0.330f),   // la copa se mantiene ancha...
                floatArrayOf(0.648f, 0.470f),
                floatArrayOf(0.600f, 0.610f),
                floatArrayOf(0.520f, 0.740f),   // ...y recien aca curva
                floatArrayOf(0.400f, 0.860f),
                floatArrayOf(0.255f, 0.945f),
                floatArrayOf(0.130f, 0.990f),
                floatArrayOf(0.000f, 1.000f)    // coronilla redondeada
            ),
            segmentos = 16
        )
        // La cresta del casco, de adelante hacia atras: es el detalle que hace
        // que se lea como casco de obra y no como una olla dada vuelta.
        val cresta = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.130f, 0.140f, 1.000f),
            0f, 0.800f, 0f
        )
        // Visera: una placa que sobresale por delante del ala.
        val visera = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.740f, 0.090f, 0.460f),
            0f, 0.095f, 0.760f
        )
        // La lampara montada al frente. El brillo lo pone el renderer aparte;
        // esto es la carcasa, para que la luz salga de algo y no de la nada.
        val farol = trasladar(
            escalar(
                lathe(
                    arrayOf(
                        floatArrayOf(0.00f, -0.50f),
                        floatArrayOf(0.40f, -0.42f),
                        floatArrayOf(0.50f, 0.15f),
                        floatArrayOf(0.38f, 0.50f)
                    ),
                    segmentos = 10
                ),
                0.400f, 0.320f, 0.400f
            ),
            0f, 0.400f, 0.610f
        )
        return combinar(copa, cresta, visera, farol)
    }
}
