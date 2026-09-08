package com.mggx.laberinto.gl

import com.mggx.laberinto.gl.PropMeshes.Geometry
import com.mggx.laberinto.gl.PropMeshes.combinar
import com.mggx.laberinto.gl.PropMeshes.escalar
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

