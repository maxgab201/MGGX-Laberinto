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
 * El patio de tu casa: lo unico del juego que pasa afuera.
 *
 * Es el final del nivel 999 y de la mina entera (ver [com.mggx.laberinto.maze.ElAscenso]).
 * Todo lo demas del juego es piedra, humedad y oscuridad; esto tiene que
 * leerse, en un segundo y sin que nadie lo explique, como **llegaste**.
 *
 * Por eso cada cosa de aca tiene su propia malla en vez de salir de las
 * primitivas genericas. Una silla hecha con cuatro cajas es una silla; una
 * silla con el respaldo de listones y las patas que se afinan es TU silla, la
 * de tu patio. La diferencia es todo el punto del lugar.
 *
 * Convencion, la misma que el resto de los props: cada modelo se apoya en
 * `y = 0`, mide mas o menos 1 en su lado largo, y mira hacia +Z. El tamano
 * real lo pone la escala de la instancia, que se lee de una.
 */
object PatioMeshes {

    // ------------------------------------------------------------- el suelo

    /**
     * Una mata de pasto: unas hojas que salen del mismo punto y se abren.
     *
     * Es la pieza que mas trabaja de todo el patio. Una superficie verde y lisa
     * se lee como una alfombra; lo que la convierte en pasto es que tenga
     * PELO — cosas finitas y verticales que se repiten. Se planta mucha,
     * chiquita y con giros distintos.
     */
    fun mataDePasto(): Geometry {
        val hojas = ArrayList<Geometry>()
        // Cada hoja es una lengueta ancha en la base que se afina y se DOBLA
        // hacia afuera. El doblez es lo que la hace pasto: una hoja recta es
        // una aguja, y una mata de agujas se lee como un erizo.
        val angulos = floatArrayOf(0f, 51f, 103f, 148f, 199f, 252f, 304f)
        val alturas = floatArrayOf(1.00f, 0.74f, 0.90f, 0.62f, 0.86f, 0.70f, 0.95f)
        for (k in angulos.indices) {
            val h = alturas[k]
            // El contorno se va corriendo en x a medida que sube: eso es el
            // doblez, y sale gratis porque ya se dibuja el perfil a mano.
            val hoja = extruir(
                arrayOf(
                    floatArrayOf(-0.115f, 0f),
                    floatArrayOf(0.115f, 0f),
                    floatArrayOf(0.135f, h * 0.34f),
                    floatArrayOf(0.125f, h * 0.68f),
                    floatArrayOf(0.115f, h),           // la punta, caida
                    floatArrayOf(0.045f, h * 0.99f),
                    floatArrayOf(0.015f, h * 0.66f),
                    floatArrayOf(-0.035f, h * 0.32f)
                ),
                0.030f
            )
            hojas.add(rotarY(rotarZ(hoja, -16f - (k % 3) * 9f), angulos[k]))
        }
        return combinar(*hojas.toTypedArray())
    }

    /**
     * Una losa del camino: piedra plana, con el canto gastado.
     *
     * El camino va de la boca de la galeria a la puerta de la casa. No es
     * decoracion: es lo que le dice al jugador, sin un solo cartel, para donde
     * tiene que ir.
     */
    fun losaDePiedra(): Geometry = escalar(
        DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.22f),
        1f, 0.10f, 1f
    )

    // -------------------------------------------------------------- la casa

    // --------------------------------------------------------------- la casa

    /**
     * Cuanto sobresale el alero, en unidades del modulo. Lo usa el armado para
     * saber donde termina el techo.
     */
    const val VUELO_ALERO = 0.470f

    /** A que altura queda el alero, en unidades del modulo. */
    const val ALTURA_ALERO = 0.560f

    /** Media profundidad del cuerpo de la casa, en unidades del modulo. */
    const val FONDO_CASA = 0.330f

    /**
     * Un modulo de casa: un metro de casa, con cuerpo, alero y techo a dos
     * aguas con sus tejas.
     *
     * ## Por que en modulos
     *
     * Antes la casa era un PANEL plano repetido: una losa de revoque con un
     * zocalo. Desde la boca del tunel —que es de donde se la mira— se leia una
     * pared de hormigon de quince metros, no una casa. Le faltaba lo unico que
     * hace que una casa sea una casa vista de lejos: el TECHO.
     *
     * No se puede modelar la casa entera de una pieza porque el pipeline de
     * instancias solo admite escala UNIFORME (ver `InstancedShape.add`): una
     * casa de 3,2 m de alto mediria 3,2 m de ancho, y el patio tiene quince.
     * Entonces se modela un metro de casa —un corte transversal extruido— y se
     * encadenan. Como la extrusion trae sus dos tapas, el primer y el ultimo
     * modulo cierran solos con su propio hastial; no hace falta una pieza
     * aparte para las puntas.
     *
     * Normalizado: se apoya en y=0, la cumbrera llega a y=1 y mide 1 de largo,
     * asi que el `scale` es la altura en metros Y el paso entre modulos.
     */
    fun moduloDeCasa(): Geometry {
        // El corte va en XY y se extruye a lo largo de Z; despues se gira 90
        // en Y para que el largo corra por X y el corte quede en ZY.
        val corte = arrayOf(
            floatArrayOf(-0.355f, 0.000f),
            floatArrayOf(-0.355f, 0.062f),
            floatArrayOf(-FONDO_CASA, 0.080f),      // zocalo
            floatArrayOf(-FONDO_CASA, ALTURA_ALERO),
            floatArrayOf(-VUELO_ALERO, ALTURA_ALERO),   // el alero vuela
            floatArrayOf(-0.452f, 0.615f),
            floatArrayOf(0.000f, 1.000f),            // cumbrera
            floatArrayOf(0.452f, 0.615f),
            floatArrayOf(VUELO_ALERO, ALTURA_ALERO),
            floatArrayOf(FONDO_CASA, ALTURA_ALERO),
            floatArrayOf(FONDO_CASA, 0.080f),
            floatArrayOf(0.355f, 0.062f),
            floatArrayOf(0.355f, 0.000f)
        )
        return rotarY(extruir(corte, 1f), 90f)
    }

    /**
     * Las tejas de un modulo: dos hileras de medias canas, una por agua.
     *
     * Van en una malla APARTE del cuerpo por una sola razon: llevan otro
     * color. El revoque es claro y la teja es terracota, y si fueran una sola
     * pieza habria que elegir uno de los dos.
     *
     * Pero se dibujan en el MISMO sitio y con la MISMA escala que el modulo, y
     * estan medidas en el mismo marco normalizado, asi que se apoyan sobre el
     * plano del techo por construccion y no por haberle acertado. Esa es la
     * diferencia con el pico-mastil de la 1.9.3: ahi la pieza se ponia a un
     * corrimiento fijo que alguien habia estimado.
     *
     * El paso de 0,10 divide justo el metro del modulo, asi que la hilera
     * sigue de un modulo al siguiente sin juntarse ni abrirse.
     */
    fun tejasDelModulo(): Geometry {
        // Pendiente del agua: del alero (z, y) a la cumbrera (0, 1).
        val zAlero = 0.452f
        val yAlero = 0.615f
        val dz = zAlero
        val dy = 1.000f - yAlero
        val largo = kotlin.math.sqrt(dz * dz + dy * dy) + 0.045f
        // Cuanto hay que girar en X para que el cilindro (que nace mirando a
        // +Y) apunte hacia arriba de la pendiente.
        val grados = -Math.toDegrees(kotlin.math.atan2(dz.toDouble(), dy.toDouble())).toFloat()
        val radio = 0.052f
        // La normal del agua, para apoyar la teja encima del plano y no dentro.
        val ln = kotlin.math.sqrt(dz * dz + dy * dy)
        val nz = dy / ln
        val ny = dz / ln

        val piezas = ArrayList<Geometry>()
        for (lado in intArrayOf(1, -1)) {
            for (k in 0 until 10) {
                val x = -0.45f + k * 0.10f
                val teja = rotarX(PropMeshes.cylinder(5, largo, radio), grados * lado)
                piezas.add(
                    trasladar(
                        if (lado == 1) teja else rotarY(teja, 180f),
                        x,
                        yAlero + ny * radio * 0.55f - 0.012f,
                        lado * (zAlero + nz * radio * 0.55f)
                    )
                )
            }
        }
        return combinar(piezas[0], *piezas.drop(1).toTypedArray())
    }

    /**
     * La chimenea: el detalle que termina de decir "aca vive alguien".
     *
     * Es la unica cosa de la casa que rompe la linea del techo, y por eso vale
     * mas que cualquier otro adorno: una silueta con chimenea se lee como casa
     * desde el otro lado del patio, sin que haga falta distinguir la puerta.
     *
     * Normalizada a 1 m de alto, apoyada en y=0.
     */
    fun chimeneaDeCasa(): Geometry {
        val cano = escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.02f), 0.30f, 0.86f, 0.30f)
        val remate = escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.02f), 0.40f, 0.09f, 0.40f)
        // Cuatro ladrillos salidos, para que no sea un tubo liso.
        val ladrillos = ArrayList<Geometry>()
        for (k in 0 until 4) {
            val y = 0.14f + k * 0.19f
            val b = escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.01f), 0.33f, 0.045f, 0.33f)
            ladrillos.add(trasladar(b, 0f, y, 0f))
        }
        return combinar(
            trasladar(cano, 0f, 0.43f, 0f),
            trasladar(remate, 0f, 0.915f, 0f),
            *ladrillos.toTypedArray()
        )
    }

    /**
     * La puerta de tu casa: marco, hoja de tablones, picaporte y escalon.
     *
     * Esta cerrada a proposito. Una puerta abierta pide que entres y el juego
     * termina afuera; una puerta cerrada dice "es tu casa, llegaste" y deja
     * que el patio sea el final.
     */
    fun puertaDeCasa(): Geometry {
        val piezas = ArrayList<Geometry>()
        // Hoja: cinco tablones verticales con una junta entre ellos.
        for (k in 0 until 5) {
            val x = (k - 2) * 0.152f
            piezas.add(
                trasladar(
                    escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.05f), 0.140f, 0.86f, 0.055f),
                    x, 0.45f, 0f
                )
            )
        }
        // Dos travesanos que las atan, como una puerta de tablas de verdad.
        for (y in floatArrayOf(0.20f, 0.70f)) {
            piezas.add(
                trasladar(
                    escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.05f), 0.78f, 0.075f, 0.070f),
                    0f, y, 0.008f
                )
            )
        }
        // Marco: jambas y dintel.
        piezas.add(trasladar(escalar(DetailMeshes.roundedBox(), 0.075f, 0.95f, 0.11f), -0.435f, 0.475f, 0f))
        piezas.add(trasladar(escalar(DetailMeshes.roundedBox(), 0.075f, 0.95f, 0.11f), 0.435f, 0.475f, 0f))
        piezas.add(trasladar(escalar(DetailMeshes.roundedBox(), 0.95f, 0.080f, 0.12f), 0f, 0.930f, 0f))
        // Picaporte y su roseta.
        piezas.add(trasladar(escalar(PropMeshes.cylinder(8, 1f, 1f), 0.032f, 0.030f, 0.032f), 0.30f, 0.46f, 0.052f))
        piezas.add(
            trasladar(
                rotarX(escalar(PropMeshes.cylinder(6, 1f, 1f), 0.018f, 0.075f, 0.018f), 90f),
                0.30f, 0.46f, 0.085f
            )
        )
        // Escalon de entrada.
        piezas.add(trasladar(escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.10f), 1.10f, 0.085f, 0.34f), 0f, 0.042f, 0.19f))
        return combinar(*piezas.toTypedArray())
    }

    /**
     * La ventana: marco, cruceta, alfeizar y dos postigos abiertos contra la
     * pared.
     *
     * Los postigos abiertos son el detalle que dice que la casa esta VIVIDA.
     * Una ventana pelada podria ser de cualquier lado.
     */
    fun ventanaDeCasa(): Geometry {
        val piezas = ArrayList<Geometry>()
        // Marco.
        piezas.add(trasladar(escalar(DetailMeshes.roundedBox(), 0.72f, 0.065f, 0.10f), 0f, 0.47f, 0f))
        piezas.add(trasladar(escalar(DetailMeshes.roundedBox(), 0.72f, 0.065f, 0.10f), 0f, -0.47f, 0f))
        piezas.add(trasladar(escalar(DetailMeshes.roundedBox(), 0.065f, 1.0f, 0.10f), -0.33f, 0f, 0f))
        piezas.add(trasladar(escalar(DetailMeshes.roundedBox(), 0.065f, 1.0f, 0.10f), 0.33f, 0f, 0f))
        // Cruceta.
        piezas.add(escalar(DetailMeshes.roundedBox(), 0.045f, 0.94f, 0.06f))
        piezas.add(escalar(DetailMeshes.roundedBox(), 0.66f, 0.045f, 0.06f))
        // Vidrio: una lamina apenas hundida, que el renderer pinta clara.
        piezas.add(trasladar(escalar(PropMeshes.box(), 0.62f, 0.90f, 0.02f), 0f, 0f, -0.03f))
        // Alfeizar que sobresale.
        piezas.add(trasladar(escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.14f), 0.88f, 0.055f, 0.20f), 0f, -0.51f, 0.05f))
        // Postigos abiertos, uno a cada lado, con sus listones.
        for (lado in intArrayOf(-1, 1)) {
            val listones = ArrayList<Geometry>()
            for (k in 0 until 4) {
                listones.add(
                    trasladar(
                        escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.06f), 0.26f, 0.19f, 0.030f),
                        0f, (k - 1.5f) * 0.225f, 0f
                    )
                )
            }
            val postigo = combinar(*listones.toTypedArray())
            piezas.add(trasladar(rotarY(postigo, lado * 72f), lado * 0.47f, 0f, 0.09f))
        }
        return trasladar(combinar(*piezas.toTypedArray()), 0f, 0.5f, 0f)
    }

    // -------------------------------------------------------------- el cerco

    /**
     * Una tabla del cerco, con la punta en pico.
     *
     * La punta importa: un cerco de tablas rectas parece una pared de madera.
     * En pico se lee como cerco de patio a veinte metros.
     */
    fun tablaDeCerco(): Geometry = extruir(
        arrayOf(
            floatArrayOf(-0.5f, 0.00f),
            floatArrayOf(0.5f, 0.00f),
            floatArrayOf(0.5f, 0.84f),
            floatArrayOf(0.0f, 1.00f),
            floatArrayOf(-0.5f, 0.84f)
        ),
        0.14f
    )

    /** El travesano horizontal que ata las tablas del cerco. */
    fun travesanoDeCerco(): Geometry =
        escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.06f), 1f, 0.10f, 0.09f)

    /**
     * La boca de la mina: el marco de tablones por el que acabas de salir.
     *
     * Va del lado por el que llegas, en vez de cerco. Un cerco ahi seria una
     * mentira —del otro lado no hay vecino, hay el cerro del que saliste— y
     * ademas te tapa la cara al salir del tunel, que fue exactamente lo que
     * mostro la primera foto del patio armado.
     *
     * Es el mismo marco de madera que sostiene las galerias abajo, y esa
     * repeticion es el punto: lo ultimo que ves de la mina es la misma pieza
     * que veias a cada rato adentro.
     */
    fun bocaDeMina(): Geometry {
        val piezas = ArrayList<Geometry>()
        // Dos montantes gruesos y un dintel, con la madera mordida.
        for (sx in intArrayOf(-1, 1)) {
            piezas.add(
                trasladar(
                    lathe(
                        arrayOf(
                            floatArrayOf(0.000f, 0.00f),
                            floatArrayOf(0.105f, 0.03f),
                            floatArrayOf(0.088f, 0.30f),
                            floatArrayOf(0.095f, 0.72f),
                            floatArrayOf(0.082f, 0.96f),
                            floatArrayOf(0.000f, 1.00f)
                        ),
                        segmentos = 7
                    ),
                    sx * 0.46f, 0f, 0f
                )
            )
        }
        piezas.add(
            trasladar(
                escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.05f), 1.14f, 0.13f, 0.20f),
                0f, 0.99f, 0f
            )
        )
        // Una tabla cruzada arriba, de refuerzo, y el cartel colgado.
        piezas.add(
            trasladar(
                rotarZ(escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.06f), 0.42f, 0.055f, 0.09f), 34f),
                -0.30f, 0.86f, 0.09f
            )
        )
        piezas.add(
            trasladar(
                rotarZ(escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.06f), 0.42f, 0.055f, 0.09f), -34f),
                0.30f, 0.86f, 0.09f
            )
        )
        return combinar(*piezas.toTypedArray())
    }

    // -------------------------------------------------------------- el arbol

    /** Tronco con la base ensanchada, como los que levantan la vereda. */
    fun troncoDeArbol(): Geometry = lathe(
        arrayOf(
            floatArrayOf(0.00f, 0.00f),
            floatArrayOf(0.185f, 0.02f),   // la raiz que se abre
            floatArrayOf(0.115f, 0.16f),
            floatArrayOf(0.095f, 0.52f),
            floatArrayOf(0.088f, 0.86f),
            floatArrayOf(0.060f, 1.00f)
        ),
        segmentos = 9
    )

    /**
     * La copa: tres masas de hoja de distinto tamano, no una pelota.
     *
     * Una esfera arriba de un palo es un arbol de maqueta. Tres masas que se
     * pisan entre si dan la silueta irregular que hace que se lea como follaje.
     */
    fun copaDeArbol(): Geometry {
        fun masa(r: Float) = lathe(
            arrayOf(
                floatArrayOf(0.00f, -r),
                floatArrayOf(r * 0.62f, -r * 0.72f),
                floatArrayOf(r * 0.98f, -r * 0.12f),
                floatArrayOf(r * 0.88f, r * 0.48f),
                floatArrayOf(r * 0.44f, r * 0.86f),
                floatArrayOf(0.00f, r)
            ),
            segmentos = 9
        )
        return combinar(
            trasladar(masa(0.50f), 0f, 0.50f, 0f),
            trasladar(masa(0.34f), -0.34f, 0.34f, 0.12f),
            trasladar(masa(0.30f), 0.32f, 0.40f, -0.14f)
        )
    }

    // ------------------------------------------------------------- muebles

    /**
     * Banco de madera de tres tablas, con las patas en A.
     *
     * Mira hacia +Z, como todo: el respaldo queda atras.
     */
    fun bancoDeMadera(): Geometry {
        val piezas = ArrayList<Geometry>()
        // Asiento: tres tablas con junta.
        for (k in 0 until 3) {
            piezas.add(
                trasladar(
                    escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.06f), 1f, 0.055f, 0.145f),
                    0f, 0.44f, (k - 1) * 0.165f
                )
            )
        }
        // Respaldo: dos tablas inclinadas.
        for (k in 0 until 2) {
            piezas.add(
                trasladar(
                    rotarX(escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.06f), 1f, 0.115f, 0.045f), 12f),
                    0f, 0.72f + k * 0.17f, -0.24f
                )
            )
        }
        // Patas: cuatro, afinadas hacia abajo.
        for (sx in intArrayOf(-1, 1)) {
            for (sz in intArrayOf(-1, 1)) {
                piezas.add(
                    trasladar(
                        lathe(
                            arrayOf(
                                floatArrayOf(0.000f, 0.00f),
                                floatArrayOf(0.030f, 0.02f),
                                floatArrayOf(0.026f, 0.38f),
                                floatArrayOf(0.034f, 0.44f),
                                floatArrayOf(0.000f, 0.46f)
                            ),
                            segmentos = 6
                        ),
                        sx * 0.42f, 0f, sz * 0.17f
                    )
                )
            }
        }
        // Los montantes del respaldo, que lo atan al asiento.
        for (sx in intArrayOf(-1, 1)) {
            piezas.add(
                trasladar(
                    rotarX(escalar(DetailMeshes.roundedBox(), 0.05f, 0.52f, 0.045f), 12f),
                    sx * 0.42f, 0.66f, -0.235f
                )
            )
        }
        return combinar(*piezas.toTypedArray())
    }

    /** Mesa redonda de patio, con el pie de tres patas. */
    fun mesaDePatio(): Geometry {
        val tapa = lathe(
            arrayOf(
                floatArrayOf(0.00f, 0.86f),
                floatArrayOf(0.46f, 0.88f),
                floatArrayOf(0.50f, 0.92f),
                floatArrayOf(0.47f, 1.00f),
                floatArrayOf(0.00f, 1.00f)
            ),
            segmentos = 14
        )
        val pie = lathe(
            arrayOf(
                floatArrayOf(0.000f, 0.00f),
                floatArrayOf(0.055f, 0.06f),
                floatArrayOf(0.042f, 0.50f),
                floatArrayOf(0.050f, 0.86f),
                floatArrayOf(0.000f, 0.88f)
            ),
            segmentos = 8
        )
        val patas = ArrayList<Geometry>()
        for (k in 0 until 3) {
            val a = k * 120f
            patas.add(
                rotarY(
                    trasladar(
                        rotarZ(escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.1f), 0.30f, 0.045f, 0.075f), -16f),
                        0.16f, 0.055f, 0f
                    ),
                    a
                )
            )
        }
        return combinar(tapa, pie, *patas.toTypedArray())
    }

    /** Silla de patio, con el respaldo de listones. */
    fun sillaDePatio(): Geometry {
        val piezas = ArrayList<Geometry>()
        piezas.add(trasladar(escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.07f), 0.90f, 0.06f, 0.86f), 0f, 0.50f, 0f))
        for (k in 0 until 3) {
            piezas.add(
                trasladar(
                    escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.07f), 0.84f, 0.115f, 0.05f),
                    0f, 0.66f + k * 0.16f, -0.40f
                )
            )
        }
        for (sx in intArrayOf(-1, 1)) {
            piezas.add(trasladar(escalar(DetailMeshes.roundedBox(), 0.055f, 0.62f, 0.055f), sx * 0.38f, 0.79f, -0.40f))
            for (sz in intArrayOf(-1, 1)) {
                piezas.add(trasladar(escalar(DetailMeshes.roundedBox(), 0.055f, 0.52f, 0.055f), sx * 0.38f, 0.25f, sz * 0.36f))
            }
        }
        return combinar(*piezas.toTypedArray())
    }

    /** Maceta de barro con una planta de hojas anchas. */
    fun macetaConPlanta(): Geometry {
        val maceta = lathe(
            arrayOf(
                floatArrayOf(0.00f, 0.00f),
                floatArrayOf(0.26f, 0.02f),
                floatArrayOf(0.33f, 0.40f),
                floatArrayOf(0.37f, 0.46f),   // el labio
                floatArrayOf(0.33f, 0.50f),
                floatArrayOf(0.28f, 0.44f),
                floatArrayOf(0.00f, 0.42f)    // la tierra adentro
            ),
            segmentos = 12
        )
        val hojas = ArrayList<Geometry>()
        for (k in 0 until 6) {
            val hoja = extruir(
                arrayOf(
                    floatArrayOf(-0.055f, 0f),
                    floatArrayOf(0.055f, 0f),
                    floatArrayOf(0.085f, 0.30f),
                    floatArrayOf(0.000f, 0.52f)
                ),
                0.018f
            )
            hojas.add(rotarY(trasladar(rotarZ(hoja, 18f + (k % 3) * 11f), 0.03f, 0.40f, 0f), k * 61f))
        }
        return combinar(maceta, *hojas.toTypedArray())
    }

    /**
     * Una prenda tendida, con la pinza arriba.
     *
     * De todo el patio, esto es lo que mas dice "aca vive alguien". Una cuerda
     * pelada podria ser cualquier cosa; una camisa colgada al sol no.
     */
    fun ropaTendida(): Geometry {
        val tela = extruir(
            arrayOf(
                floatArrayOf(-0.34f, 1.00f),
                floatArrayOf(0.34f, 1.00f),
                floatArrayOf(0.40f, 0.62f),
                floatArrayOf(0.30f, 0.58f),
                floatArrayOf(0.28f, 0.06f),
                floatArrayOf(0.00f, 0.00f),
                floatArrayOf(-0.28f, 0.06f),
                floatArrayOf(-0.30f, 0.58f),
                floatArrayOf(-0.40f, 0.62f)
            ),
            0.020f
        )
        val pinza = trasladar(escalar(DetailMeshes.roundedBox(), 0.045f, 0.075f, 0.035f), -0.20f, 1.01f, 0f)
        val pinza2 = trasladar(escalar(DetailMeshes.roundedBox(), 0.045f, 0.075f, 0.035f), 0.20f, 1.01f, 0f)
        return combinar(tela, pinza, pinza2)
    }

    /** El balde de la mina, apoyado contra la pared. Es el guino al lugar del que saliste. */
    fun baldeDeMina(): Geometry {
        val cuerpo = lathe(
            arrayOf(
                floatArrayOf(0.00f, 0.00f),
                floatArrayOf(0.30f, 0.03f),
                floatArrayOf(0.37f, 0.72f),
                floatArrayOf(0.40f, 0.80f),
                floatArrayOf(0.36f, 0.84f),
                floatArrayOf(0.32f, 0.78f),
                floatArrayOf(0.00f, 0.74f)
            ),
            segmentos = 12
        )
        // Asa: media argolla parada.
        val asa = rotarX(DetailMeshes.torus(0.40f, 0.026f, rings = 14, sides = 5), 90f)
        return combinar(cuerpo, trasladar(asa, 0f, 0.84f, 0f))
    }
}
