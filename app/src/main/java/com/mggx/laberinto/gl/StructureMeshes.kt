package com.mggx.laberinto.gl

import com.mggx.laberinto.gl.PropMeshes.Geometry
import com.mggx.laberinto.gl.PropMeshes.combinar
import com.mggx.laberinto.gl.PropMeshes.escalar
import com.mggx.laberinto.gl.PropMeshes.extruir
import com.mggx.laberinto.gl.PropMeshes.lathe
import com.mggx.laberinto.gl.PropMeshes.rotarX
import com.mggx.laberinto.gl.PropMeshes.rotarY
import com.mggx.laberinto.gl.PropMeshes.rotarZ
import com.mggx.laberinto.gl.PropMeshes.trasladar

/**
 * Un modelo propio para cada estructura de la cueva.
 *
 * Igual que con los bichos, casi todas se armaban reusando las mismas cuatro
 * primitivas: la antorcha era un cilindro con un octaedro encima, el cofre dos
 * cajas, los cristales octaedros, el hongo un cilindro con un octaedro de
 * sombrero. Aca cada una tiene su forma.
 *
 * Convencion de escala: salvo que se diga otra cosa, cada modelo se apoya en
 * y=0 y llega hasta y=1, asi el `scale` que le pasa CaveRenderer es
 * directamente su altura en metros y se puede apoyar en el piso sin cuentas.
 */
object StructureMeshes {

    /**
     * Cilindro CERRADO en las dos puntas, centrado en el origen y de largo 1
     * sobre el eje Y.
     *
     * [PropMeshes.cylinder] solo lleva tapa arriba, asi que mirado desde abajo
     * se le ve el agujero. Da igual cuando la pieza se apoya en el piso, pero
     * no cuando cuelga a la altura de la cabeza (una antorcha de pared) o
     * queda al aire: ahi se nota.
     */
    private fun barra(radio: Float, segmentos: Int = 12): Geometry = lathe(
        arrayOf(
            floatArrayOf(0f, -0.5f),
            floatArrayOf(radio, -0.5f),
            floatArrayOf(radio, 0.5f),
            floatArrayOf(0f, 0.5f)
        ),
        segmentos
    )

    /**
     * Antorcha de pared: la escarpia que sale de la roca, el mastil y el
     * cuenco donde prende el fuego.
     *
     * El brazo apunta hacia -Z (hacia la pared en la que se clava), asi que
     * CaveRenderer la gira segun de que lado quedo la roca. Antes era un
     * cilindro suelto flotando, sin nada que la sujetara.
     */
    fun antorcha(): Geometry {
        val mastil = trasladar(escalar(barra(RADIO_MASTIL), 1f, 0.80f, 1f), 0f, 0.40f, 0f)
        // Brazo que la ata a la pared, con su placa de anclaje. Girado -90 en X
        // para que el eje apunte a -Z, o sea hacia la roca.
        val brazo = trasladar(
            rotarX(escalar(barra(0.05f, 5), 1f, LARGO_BRAZO, 1f), -90f),
            0f, 0.13f, -LARGO_BRAZO * 0.5f
        )
        val placa = trasladar(
            escalar(DetailMeshes.roundedBox(), 0.15f, 0.28f, 0.05f),
            0f, 0.15f, -LARGO_BRAZO
        )
        // Cuenco de arriba: se abre para contener la llama, y el perfil vuelve
        // para adentro y para abajo, que es la pared interior de la taza.
        val cuenco = trasladar(
            lathe(
                arrayOf(
                    floatArrayOf(0.00f, 0.00f),
                    floatArrayOf(0.09f, 0.01f),
                    floatArrayOf(0.13f, 0.10f),
                    floatArrayOf(RADIO_CUENCO, 0.20f),
                    floatArrayOf(0.17f, 0.19f),
                    floatArrayOf(0.10f, 0.09f),
                    floatArrayOf(0.00f, 0.08f)
                ),
                segmentos = 16
            ),
            0f, 0.80f, 0f
        )
        return combinar(mastil, brazo, placa, cuenco)
    }

    /** Radio del mastil, en unidades del modelo (el modelo mide 1 de alto). */
    const val RADIO_MASTIL = 0.075f

    /** Radio del borde del cuenco, que es lo que tiene que contener la llama. */
    const val RADIO_CUENCO = 0.19f

    /** Cuanto sale el brazo hacia la pared, en unidades del modelo. */
    const val LARGO_BRAZO = 0.26f

    /**
     * Altura del centro de la placa de anclaje, en unidades del modelo.
     *
     * Es la pieza que tiene que tocar la roca, asi que es a ESTA altura donde
     * hay que medir donde cae la pared de verdad (ver `WorldMesh.realWallFace`).
     */
    const val ALTURA_DE_LA_PLACA = 0.15f

    /** Altura a la que se apoya la llama, en unidades del modelo. */
    const val ALTURA_DEL_FUEGO = 0.86f

    /**
     * Llama: una gota, ancha abajo y afinada arriba, con la base justo en y=0.
     *
     * Que la base este en cero es lo que hace imposible que se hunda en el
     * poste: se la apoya en la punta y listo. El octaedro de antes tenia el
     * centro en su posicion, asi que media figura quedaba metida adentro.
     */
    fun llama(): Geometry = lathe(
        arrayOf(
            floatArrayOf(0.00f, 0.00f),
            floatArrayOf(0.16f, 0.10f),
            floatArrayOf(0.21f, 0.26f),   // la panza del fuego
            floatArrayOf(0.19f, 0.44f),
            floatArrayOf(0.12f, 0.64f),
            floatArrayOf(0.05f, 0.84f),
            floatArrayOf(0.00f, 1.00f)    // la punta
        ),
        segmentos = 16
    )

    /**
     * Cristal: un prisma de seis caras con la punta tallada, apoyado en la
     * base y apenas inclinado.
     *
     * La inclinacion es a proposito: como el shader solo gira en Y, si el
     * cristal fuera derecho todos saldrian identicos y parados. Inclinado, cada
     * giro lo hace caer para otro lado y el racimo se ve natural.
     */
    fun cristal(): Geometry = rotarZ(
        lathe(
            arrayOf(
                floatArrayOf(0.00f, 0.00f),
                floatArrayOf(0.15f, 0.05f),
                floatArrayOf(0.17f, 0.62f),   // el fuste
                floatArrayOf(0.13f, 0.80f),   // donde arrancan las facetas
                floatArrayOf(0.00f, 1.00f)
            ),
            segmentos = 6
        ),
        9f
    )

    /**
     * Cofre: cajon, tapa curva con los flejes, y la cerradura al frente.
     */
    fun cofre(): Geometry {
        val cajon = trasladar(escalar(DetailMeshes.roundedBox(), 0.86f, 0.52f, 0.62f), 0f, 0.26f, 0f)
        // Tapa: un barril acostado a lo ancho del cofre. Al girarlo 90 en Z el
        // eje queda sobre X, asi que el primer factor es el largo y los otros
        // dos el radio. La mitad de abajo queda escondida dentro del cajon.
        val barril = lathe(
            arrayOf(
                floatArrayOf(0.00f, -0.50f),
                floatArrayOf(0.22f, -0.47f),
                floatArrayOf(0.31f, -0.36f),
                floatArrayOf(0.31f, 0.36f),
                floatArrayOf(0.22f, 0.47f),
                floatArrayOf(0.00f, 0.50f)
            ),
            segmentos = 18
        )
        val tapa = trasladar(escalar(rotarZ(barril, 90f), 0.86f, 1f, 1f), 0f, 0.52f, 0f)
        val fleje = escalar(DetailMeshes.roundedBox(), 0.06f, 0.56f, 0.66f)
        val cerradura = trasladar(escalar(DetailMeshes.roundedBox(), 0.14f, 0.16f, 0.06f), 0f, 0.44f, 0.32f)
        return combinar(
            cajon, tapa,
            trasladar(fleje, -0.28f, 0.28f, 0f),
            trasladar(fleje, 0.28f, 0.28f, 0f),
            cerradura
        )
    }

    /**
     * Hongo: pie grueso que se afina y sombrero de domo, en una sola pieza.
     * Antes eran un cilindro y un octaedro que no se tocaban del todo.
     */
    fun hongo(): Geometry {
        val pie = lathe(
            arrayOf(
                floatArrayOf(0.00f, 0.00f),
                floatArrayOf(0.14f, 0.03f),
                floatArrayOf(0.10f, 0.30f),
                floatArrayOf(0.09f, 0.62f),
                floatArrayOf(0.00f, 0.66f)
            ),
            segmentos = 16
        )
        val sombrero = trasladar(
            lathe(
                arrayOf(
                    floatArrayOf(0.00f, 0.00f),
                    floatArrayOf(0.24f, 0.02f),
                    floatArrayOf(0.30f, 0.10f),   // el ala del sombrero
                    floatArrayOf(0.26f, 0.24f),
                    floatArrayOf(0.15f, 0.32f),
                    floatArrayOf(0.00f, 0.34f)
                ),
                segmentos = 18
            ),
            0f, 0.60f, 0f
        )
        return combinar(pie, sombrero)
    }

    /**
     * Pincho de trampa: hierro forjado con una rebaba a media altura, para que
     * se lea como algo que engancha y no como un cono liso.
     */
    fun pincho(): Geometry = lathe(
        arrayOf(
            floatArrayOf(0.00f, 0.00f),
            floatArrayOf(0.12f, 0.04f),
            floatArrayOf(0.10f, 0.28f),
            floatArrayOf(0.15f, 0.36f),   // la rebaba
            floatArrayOf(0.07f, 0.44f),
            floatArrayOf(0.05f, 0.72f),
            floatArrayOf(0.00f, 1.00f)
        ),
        segmentos = 7
    )

    /**
     * Estacion de carburo: pie de hierro, bidon panzon y el volante de la
     * valvula arriba. Los tres eran un cilindro, una caja y un octaedro.
     */
    fun estacionCarburo(): Geometry {
        val pie = trasladar(escalar(barra(0.1f, 6), 0.5f, 0.62f, 0.5f), 0f, 0.31f, 0f)
        val bidon = trasladar(
            lathe(
                arrayOf(
                    floatArrayOf(0.00f, 0.00f),
                    floatArrayOf(0.18f, 0.01f),
                    floatArrayOf(0.21f, 0.08f),
                    floatArrayOf(0.21f, 0.26f),
                    floatArrayOf(0.17f, 0.32f),
                    floatArrayOf(0.00f, 0.33f)
                ),
                segmentos = 18
            ),
            0f, 0.58f, 0f
        )
        // Volante: un aro chato hecho de radios, apoyado sobre el bidon.
        val radio = escalar(DetailMeshes.roundedBox(), 0.30f, 0.035f, 0.05f)
        return combinar(
            pie, bidon,
            trasladar(DetailMeshes.torus(0.135f, 0.018f), 0f, 0.95f, 0f),
            trasladar(radio, 0f, 0.95f, 0f),
            trasladar(rotarY(radio, 60f), 0f, 0.95f, 0f),
            trasladar(rotarY(radio, 120f), 0f, 0.95f, 0f),
            trasladar(escalar(barra(0.1f, 6), 0.6f, 0.10f, 0.6f), 0f, 0.95f, 0f)
        )
    }

    /**
     * La boca del pozo: un brocal de piedra rota y, adentro, la oscuridad.
     *
     * Antes el pozo era una CAJA negra puesta bajo el piso; como la caja mide
     * lo mismo en los tres ejes, al escalarla para que tapara la casilla
     * tambien crecia para arriba y quedaba medio metro de cubo negro apoyado
     * en el suelo. Se veia un baul, no un agujero. No lo agarro nadie en
     * meses: el armado de las trampas no se podia mirar (ver
     * [ArmadoDeEstructuras]).
     *
     * Un agujero se lee por dos cosas: el BORDE, que tiene que ser irregular y
     * estar a ras del piso, y el adentro, que tiene que perderse hacia abajo.
     */
    fun bocaDePozo(): Geometry = lathe(
        // El perfil va de ARRIBA hacia ABAJO a proposito: escrito asi, lathe()
        // deja las normales y el tejido mirando para ADENTRO, que es lo unico
        // que sirve para un agujero. Escrito al reves se veria un cono macizo
        // desde afuera y, con el descarte de caras de atras prendido, desde
        // arriba no se veria nada.
        //
        // Y es un cuenco MUY chato, casi un disco, no un embudo hondo. El piso
        // de la cueva es una malla maciza: no tiene agujero, asi que todo lo
        // que se dibuje por debajo de el queda tapado por la roca y no se ve
        // nunca. Un embudo de 1,30 m de hondo se veria exactamente igual que
        // este disco — con la diferencia de que el disco no miente.
        arrayOf(
            floatArrayOf(0.50f, 0.000f),    // el borde
            floatArrayOf(0.46f, -0.012f),
            floatArrayOf(0.34f, -0.022f),
            floatArrayOf(0.00f, -0.028f)    // el fondo, que no se ve
        ),
        segmentos = 13
    )

    /**
     * El brocal: las piedras sueltas del borde del pozo.
     *
     * Va aparte del embudo por dos motivos. Lleva OTRO color — el agujero
     * tiene que quedarse negro y la piedra del borde tiene que verse piedra —
     * y ademas es lo que TAPA el escalon: la boca del pozo se levanta unos
     * centimetros sobre la roca (ver [ArmadoDeEstructuras.LEVANTE_POZO]) y sin
     * el brocal ese escalon se veria como un disco negro flotando.
     */
    fun brocalDePozo(): Geometry {
        val piedras = ArrayList<Geometry>()
        for (k in 0 until 11) {
            val a = k * 32.7f + (k % 3) * 6f
            val r = 0.495f + (k % 4) * 0.020f
            val piedra = escalar(
                DetailMeshes.boulder(),
                0.11f + (k % 3) * 0.030f, 0.075f + (k % 2) * 0.022f, 0.15f
            )
            piedras.add(rotarY(trasladar(piedra, r, -0.020f, 0f), a))
        }
        return combinar(piedras[0], *piedras.drop(1).toTypedArray())
    }

    /**
     * La losa rajada que tapa una trampa de pinches.
     *
     * Antes eran dos barras cruzadas, que se leian como un durmiente de vias.
     * Una losa partida al medio dice "esto se hunde cuando lo pises".
     */
    fun losaRajada(): Geometry {
        // La grieta va en ZIGZAG, no recta. Con el corte recto las dos mitades
        // se leian como dos baldosas puestas una al lado de la otra; lo que
        // dice "esto se partio" es que los dos bordes encastren.
        fun mitad(signo: Float) = extruir(
            arrayOf(
                floatArrayOf(signo * 0.03f, -0.50f),
                floatArrayOf(signo * 0.50f, -0.44f),
                floatArrayOf(signo * 0.50f, 0.42f),
                floatArrayOf(signo * 0.06f, 0.50f),
                floatArrayOf(signo * 0.12f, 0.27f),
                floatArrayOf(signo * 0.02f, 0.05f),
                floatArrayOf(signo * 0.13f, -0.19f)
            ),
            0.075f
        )
        // Acostadas, y la de la izquierda hundida y ladeada: ya cedio de ese
        // lado. Una tapa partida con las dos mitades a nivel se ve entera.
        return combinar(
            rotarX(mitad(1f), -90f),
            trasladar(rotarZ(rotarX(mitad(-1f), -90f), 4.5f), 0f, -0.030f, 0f)
        )
    }

    /**
     * La costra mineral que rodea una fisura de vapor.
     *
     * Antes la fisura era un CILINDRO con una CAJA encima. En la foto se veia
     * un cubo blanco de tres cuartos de metro apoyado en el piso — un
     * lavarropas, no una grieta. El vapor sale de una RAJA en la roca, y una
     * raja se lee por los dos labios de costra que le crecen a los costados.
     */
    fun costraDeFisura(): Geometry {
        val piezas = ArrayList<Geometry>()
        // Los dos labios, LARGOS y juntos. Con los labios cortos y separados
        // la costra se leia como un nido redondo: lo que dice "grieta" es que
        // sea mucho mas larga que ancha.
        for (lado in intArrayOf(-1, 1)) {
            for (k in 0 until 8) {
                val x = -0.66f + k * 0.19f
                val z = lado * (0.21f + (k % 2) * 0.020f)
                val c = escalar(
                    DetailMeshes.boulder(),
                    0.26f + (k % 3) * 0.045f, 0.105f + (k % 2) * 0.025f, 0.125f
                )
                piezas.add(trasladar(rotarY(c, k * 23f * lado), x, 0f, z))
            }
        }
        // Los dos remates de las puntas, que cierran la raja.
        for (sg in intArrayOf(-1, 1)) {
            piezas.add(
                trasladar(escalar(DetailMeshes.boulder(), 0.14f, 0.095f, 0.21f), sg * 0.765f, 0f, 0f)
            )
        }
        return combinar(piezas[0], *piezas.drop(1).toTypedArray())
    }

    /**
     * La raja en si: la chapa oscura de donde sale el vapor.
     *
     * Va aparte de la costra por el mismo motivo que el pozo va aparte de su
     * brocal: lleva otro color. Adentro de una grieta no hay nada que ver.
     */
    fun bocaDeFisura(): Geometry = rotarX(
        extruir(
            arrayOf(
                floatArrayOf(-0.50f, 0.00f),
                floatArrayOf(-0.28f, 0.10f),
                floatArrayOf(0.08f, 0.14f),
                floatArrayOf(0.46f, 0.07f),
                floatArrayOf(0.50f, -0.03f),
                floatArrayOf(0.10f, -0.13f),
                floatArrayOf(-0.26f, -0.11f)
            ),
            0.022f
        ),
        -90f
    )

    /**
     * Una bocanada de vapor: un bollo blando, no un cristal.
     *
     * El vapor se dibujaba con la misma malla que las gemas, que es un
     * octaedro facetado. Se veia una torre de diamantes saliendo del piso.
     */
    fun nubeDeVapor(): Geometry {
        fun bollo(r: Float) = lathe(
            arrayOf(
                floatArrayOf(0.00f, -r * 0.9f),
                floatArrayOf(r * 0.70f, -r * 0.55f),
                floatArrayOf(r * 1.00f, 0.00f),
                floatArrayOf(r * 0.78f, r * 0.60f),
                floatArrayOf(0.00f, r * 0.95f)
            ),
            segmentos = 8
        )
        return combinar(
            bollo(0.50f),
            trasladar(bollo(0.34f), 0.34f, 0.20f, 0.12f),
            trasladar(bollo(0.28f), -0.30f, 0.14f, -0.16f)
        )
    }

    /**
     * Un tramo de via de vagoneta: los durmientes y los dos rieles.
     *
     * Antes la via eran dos [DetailMeshes.roundedBox] largos puestos en cruz,
     * uno mas largo que el otro. En una foto eso se lee como un durmiente
     * suelto, no como una via: lo que hace que el ojo diga "por aca pasaba una
     * vagoneta" son los DOS rieles paralelos, y eso con dos piezas cruzadas no
     * se puede decir.
     *
     * Se apoya en y=0, corre a lo largo de X y mide 1 de largo, asi que el
     * `scale` que le pase el renderer es directamente su largo en metros.
     */
    fun viaDeMina(): Geometry {
        val piezas = ArrayList<Geometry>()
        // Cinco durmientes cruzados, ninguno perfectamente derecho: la via
        // esta abandonada.
        for (k in 0 until 5) {
            val x = -0.40f + k * 0.20f
            val d = escalar(DetailMeshes.roundedBox(1f, 0.10f, 0.10f, 0.014f), 0.62f, 0.62f, 0.62f)
            piezas.add(trasladar(rotarY(d, 90f + (k % 3 - 1) * 4f), x, 0.031f, 0f))
        }
        // Los dos rieles, finitos y de otro material.
        for (sg in intArrayOf(-1, 1)) {
            val r = escalar(DetailMeshes.roundedBox(1f, 0.05f, 0.05f, 0.008f), 1f, 1f, 1f)
            piezas.add(trasladar(r, 0f, 0.083f, sg * 0.125f))
        }
        return combinar(piezas[0], *piezas.drop(1).toTypedArray())
    }

    /**
     * El fuste de una columna partida, con su basa.
     *
     * Antes era una caja de 0,72 con un cilindro encima, y el cilindro se
     * escalaba a `alto * 2.6`: hasta 4,76 m. En una galeria de 3,40 la columna
     * salia por el techo, y eso paso desapercibido todo este tiempo porque el
     * armado vivia suelto adentro del renderer.
     *
     * Ahora es un modelo solo, normalizado: se apoya en y=0 y llega a y=1, asi
     * que el `scale` es su altura en metros y el renderer puede recortarla
     * contra el alto libre de la casilla sin hacer cuentas.
     */
    fun fusteRoto(): Geometry {
        // Basa escalonada.
        val basa = combinar(
            escalar(DetailMeshes.roundedBox(), 0.46f, 0.07f, 0.46f),
            trasladar(escalar(DetailMeshes.roundedBox(), 0.38f, 0.05f, 0.38f), 0f, 0.055f, 0f)
        )
        // Fuste apenas conico, como cualquier columna de verdad.
        val fuste = lathe(
            arrayOf(
                floatArrayOf(0.170f, 0.000f),
                floatArrayOf(0.163f, 0.180f),
                floatArrayOf(0.152f, 0.520f),
                floatArrayOf(0.146f, 0.820f),
                floatArrayOf(0.150f, 0.880f)
            ),
            segmentos = 12
        )
        // Estrias: ocho medias canas pegadas al fuste. Son lo que separa una
        // columna tallada de un cano.
        val estrias = ArrayList<Geometry>()
        for (k in 0 until 8) {
            val e = escalar(
                PropMeshes.cylinder(5, 1f, 0.1f), 0.26f, 0.83f, 0.26f
            )
            estrias.add(rotarY(trasladar(e, 0.152f, 0.03f, 0f), k * 45f))
        }
        // El quiebre de arriba: la columna esta PARTIDA, no cortada a escuadra.
        //
        // Va en un arco de medio giro y no en un anillo entero, y siempre por
        // DENTRO del radio del fuste: con las piedras repartidas en toda la
        // vuelta y asomando por el borde, la rotura se leia como un capitel,
        // que es lo contrario de lo que tiene que decir.
        val quiebre = ArrayList<Geometry>()
        val alturas = floatArrayOf(0.905f, 0.870f, 0.845f, 0.882f, 0.838f)
        val radios = floatArrayOf(0.035f, 0.085f, 0.110f, 0.062f, 0.100f)
        for (k in 0 until 5) {
            val t = escalar(DetailMeshes.boulder(), 0.115f, 0.075f, 0.100f)
            quiebre.add(rotarY(trasladar(t, radios[k], alturas[k], 0f), 24f + k * 47f))
        }
        return combinar(
            trasladar(basa, 0f, 0.035f, 0f),
            trasladar(fuste, 0f, 0.085f, 0f),
            *estrias.toTypedArray(),
            *quiebre.toTypedArray()
        )
    }

    /**
     * Columna de la salida: un obelisco de cristal facetado, ancho abajo y
     * rematado en punta, para que se distinga de lejos de cualquier otra cosa.
     */
    fun obeliscoSalida(): Geometry = lathe(
        arrayOf(
            floatArrayOf(0.00f, 0.00f),
            floatArrayOf(0.16f, 0.02f),
            floatArrayOf(0.13f, 0.20f),
            floatArrayOf(0.10f, 0.55f),
            floatArrayOf(0.12f, 0.62f),   // el collar de la mitad
            floatArrayOf(0.08f, 0.70f),
            floatArrayOf(0.06f, 0.90f),
            floatArrayOf(0.00f, 1.00f)
        ),
        segmentos = 6
    )
}

