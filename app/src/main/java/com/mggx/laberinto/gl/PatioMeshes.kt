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
import kotlin.math.pow

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
     * Una mata de pasto: hojas finas que salen juntas y se van acostando.
     *
     * ## Que hace que se lea como pasto
     *
     * La primera version tenia siete lenguetas ANCHAS —tres centimetros de
     * ancho por veinte de alto— apenas inclinadas. En la foto del patio se
     * veia un yuyal de puas: la proporcion estaba mal por un orden de
     * magnitud (una hoja de pasto de verdad es 1 a 50, no 1 a 5) y la
     * inclinacion rigida las dejaba a todas igual de tiesas.
     *
     * Lo que hace pasto es el ARCO: la hoja sale vertical, se afina y se
     * acuesta cada vez mas rapido hasta caer de punta. Aca la linea media de
     * cada hoja se recorre paso a paso con una parabola y el ancho se apaga
     * con ella, asi que la hoja nace gruesa y termina en punta sin ningun
     * corte.
     */
    fun mataDePasto(): Geometry {
        val hojas = ArrayList<Geometry>()
        val alturas = floatArrayOf(1.00f, 0.72f, 0.88f, 0.58f, 0.94f, 0.66f, 0.82f, 0.50f, 0.76f)
        val curvas = floatArrayOf(0.26f, 0.44f, 0.16f, 0.52f, 0.31f, 0.40f, 0.21f, 0.58f, 0.36f)
        for (k in alturas.indices) {
            hojas.add(
                rotarY(
                    hojaDePasto(alturas[k], curvas[k], 0.055f + (k % 3) * 0.012f),
                    k * 41.3f + (k % 2) * 17f
                )
            )
        }
        return combinar(hojas[0], *hojas.drop(1).toTypedArray())
    }

    /**
     * Una sola hoja: una cinta que sube por una parabola y se va afinando.
     *
     * @param alto cuanto llega a subir.
     * @param curva cuanto se acuesta al llegar a la punta.
     * @param ancho cuanto mide en la base.
     */
    private fun hojaDePasto(alto: Float, curva: Float, ancho: Float): Geometry {
        val pasos = 5
        val izq = ArrayList<FloatArray>()
        val der = ArrayList<FloatArray>()
        for (i in 0..pasos) {
            val t = i.toFloat() / pasos
            // La punta cae: por eso el alto no crece lineal sino frenando.
            val y = alto * t * (1.12f - 0.12f * t) * (1f - 0.18f * t * t)
            val x = curva * t * t
            // El ancho se apaga hacia la punta, pero no de golpe.
            val w = ancho * (1f - t).toDouble().pow(0.62).toFloat()
            izq.add(floatArrayOf(x - w, y))
            der.add(floatArrayOf(x + w, y))
        }
        val contorno = ArrayList<FloatArray>()
        contorno.addAll(der)
        for (i in pasos downTo 0) contorno.add(izq[i])
        return extruir(contorno.toTypedArray(), 0.016f)
    }

    /**
     * Una losa del camino: piedra plana, con el canto gastado.
     *
     * El camino va de la boca de la galeria a la puerta de la casa. No es
     * decoracion: es lo que le dice al jugador, sin un solo cartel, para donde
     * tiene que ir.
     */
    fun losaDePiedra(): Geometry {
        // Una lasca de piedra con SIETE lados desparejos, no un cuadrado
        // redondeado. Un camino hecho de cuadrados iguales se lee como una
        // vereda de baldosas de fabrica; lo que dice "esto lo puso alguien con
        // lo que habia" es que ninguna losa sea igual a la de al lado, y con
        // una sola malla eso solo se puede conseguir en la FORMA.
        val contorno = arrayOf(
            floatArrayOf(-0.50f, -0.28f),
            floatArrayOf(-0.22f, -0.50f),
            floatArrayOf(0.30f, -0.46f),
            floatArrayOf(0.50f, -0.06f),
            floatArrayOf(0.38f, 0.40f),
            floatArrayOf(-0.06f, 0.50f),
            floatArrayOf(-0.44f, 0.24f)
        )
        return rotarX(extruir(contorno, 0.085f), -90f)
    }

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

    /**
     * El farol de la puerta.
     *
     * Es la unica cosa del patio que EMITE luz, y por eso vale por diez
     * adornos: una casa con la luz de la entrada prendida dice "te estaban
     * esperando" sin una sola palabra. Ademas hace de ancla para el ojo — el
     * camino te lleva a la puerta y el farol te dice cual es la puerta.
     *
     * Normalizado: colgado del brazo en y=1, mirando a +Z.
     */
    fun farolDePuerta(): Geometry {
        val piezas = ArrayList<Geometry>()
        // El brazo que lo sostiene, y su soporte contra la pared.
        piezas.add(
            trasladar(escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.02f), 0.09f, 0.22f, 0.07f),
                0f, 0.90f, -0.02f)
        )
        piezas.add(
            trasladar(escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.02f), 0.06f, 0.06f, 0.42f),
                0f, 0.96f, 0.20f)
        )
        // La caja de vidrio: cuatro montantes y el vidrio adentro.
        for (sx in intArrayOf(-1, 1)) for (sz in intArrayOf(-1, 1)) {
            piezas.add(
                trasladar(escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.01f), 0.035f, 0.42f, 0.035f),
                    sx * 0.135f, 0.62f, 0.38f + sz * 0.135f)
            )
        }
        piezas.add(
            trasladar(escalar(DetailMeshes.roundedBox(1f, 1f, 1f, bevel = 0.02f), 0.30f, 0.05f, 0.30f),
                0f, 0.415f, 0.38f)
        )
        // El sombrerete, a cuatro aguas.
        piezas.add(
            trasladar(
                lathe(
                    arrayOf(
                        floatArrayOf(0.215f, 0.00f),
                        floatArrayOf(0.195f, 0.04f),
                        floatArrayOf(0.105f, 0.13f),
                        floatArrayOf(0.000f, 0.18f)
                    ),
                    segmentos = 4
                ),
                0f, 0.835f, 0.38f
            )
        )
        // La llama: una gota adentro del vidrio. El renderer la pinta emisiva.
        piezas.add(
            trasladar(
                lathe(
                    arrayOf(
                        floatArrayOf(0.000f, -0.09f),
                        floatArrayOf(0.055f, -0.02f),
                        floatArrayOf(0.045f, 0.07f),
                        floatArrayOf(0.000f, 0.13f)
                    ),
                    segmentos = 6
                ),
                0f, 0.62f, 0.38f
            )
        )
        return combinar(piezas[0], *piezas.drop(1).toTypedArray())
    }

    // ------------------------------------------------------------- lo lejos

    /**
     * Un cerro del fondo.
     *
     * ## Por que hace falta
     *
     * Hasta ahora, al otro lado del cerco no habia NADA: el mundo se terminaba
     * en la ultima tabla. Eso hace dos cosas malas a la vez. La obvia es que
     * se ve el vacio. La que no es obvia, y pesa mas, es que sin nada lejos el
     * patio no tiene ESCALA: todo lo que se ve esta a la misma distancia, y el
     * ojo no tiene con que medir cuan grande es la casa ni cuan lejos queda el
     * arbol.
     *
     * Son masas grandes y simples a proposito. A cuarenta metros nadie va a
     * mirarles la forma; lo unico que importa es la silueta contra el cielo y
     * que el borde de arriba no sea una curva limpia, porque un cerro con el
     * lomo liso se lee como una carpa.
     *
     * Normalizado: se apoya en y=0 y llega a y=1.
     */
    fun cerroLejano(): Geometry {
        val piezas = ArrayList<Geometry>()
        // MUY ancho y bajo: 2,4 de radio por 1 de alto, o sea casi cinco a
        // uno. La primera version era 1 a 1 —una campana— y en la foto se veia
        // una carpa de circo detras de la casa. Un cerro visto de lejos es
        // casi todo horizontal; lo que lo hace cerro es la pendiente suave,
        // no la punta.
        val r = 2.40f
        piezas.add(
            lathe(
                arrayOf(
                    floatArrayOf(r, 0.00f),
                    floatArrayOf(r * 0.93f, 0.12f),
                    floatArrayOf(r * 0.74f, 0.36f),
                    floatArrayOf(r * 0.50f, 0.64f),
                    floatArrayOf(r * 0.25f, 0.87f),
                    floatArrayOf(0.00f, 1.00f)
                ),
                segmentos = 13
            )
        )
        // Estribaciones: lomas laterales que le rompen la simetria. Sin ellas
        // el cerro se lee como una curva de libro de texto.
        val jorobas = arrayOf(
            floatArrayOf(1.30f, -1.30f, 0.06f, 0.70f),
            floatArrayOf(1.05f, 1.45f, 0.04f, -0.60f),
            floatArrayOf(0.80f, 0.30f, 0.16f, 1.35f),
            floatArrayOf(0.66f, -0.75f, 0.10f, -1.15f)
        )
        for (j in jorobas) {
            val m = lathe(
                arrayOf(
                    floatArrayOf(j[0], 0.00f),
                    floatArrayOf(j[0] * 0.80f, j[0] * 0.22f),
                    floatArrayOf(j[0] * 0.44f, j[0] * 0.38f),
                    floatArrayOf(0.00f, j[0] * 0.46f)
                ),
                segmentos = 9
            )
            piezas.add(trasladar(m, j[1], j[2], j[3]))
        }
        return combinar(piezas[0], *piezas.drop(1).toTypedArray())
    }

    /**
     * Un arbol del fondo: tronco fino y una copa cerrada.
     *
     * Mucho mas simple que [copaDeArbol] porque a treinta metros la unica
     * diferencia que se nota entre los dos es el tamano. Lo que SI importa es
     * que la copa no sea una esfera: una fila de esferas identicas contra el
     * cielo se lee como una hilera de globos.
     *
     * Normalizado: se apoya en y=0 y llega a y=1.
     */
    fun arbolLejano(): Geometry {
        val tronco = lathe(
            arrayOf(
                floatArrayOf(0.055f, 0.00f),
                floatArrayOf(0.036f, 0.20f),
                floatArrayOf(0.030f, 0.42f),
                floatArrayOf(0.000f, 0.48f)
            ),
            segmentos = 6
        )
        fun masa(r: Float) = lathe(
            arrayOf(
                floatArrayOf(0.00f, -r * 0.88f),
                floatArrayOf(r * 0.72f, -r * 0.54f),
                floatArrayOf(r * 1.00f, r * 0.04f),
                floatArrayOf(r * 0.70f, r * 0.62f),
                floatArrayOf(0.00f, r * 0.98f)
            ),
            segmentos = 7
        )
        return combinar(
            tronco,
            trasladar(masa(0.26f), 0.00f, 0.70f, 0.00f),
            trasladar(masa(0.20f), -0.17f, 0.52f, 0.08f),
            trasladar(masa(0.18f), 0.16f, 0.58f, -0.10f),
            trasladar(masa(0.14f), 0.04f, 0.90f, 0.06f)
        )
    }

    // -------------------------------------------------------------- el cerco

    /**
     * Una tabla del cerco, con la punta en pico.
     *
     * La punta importa: un cerco de tablas rectas parece una pared de madera.
     * En pico se lee como cerco de patio a veinte metros.
     */
    fun tablaDeCerco(): Geometry = extruir(
        // La tabla no es un rectangulo con punta: tiene la veta comida, los
        // cantos desparejos y la punta corrida del medio. Como todas las
        // tablas del cerco salen de la MISMA malla, lo unico que las puede
        // diferenciar es que la malla ya traiga desprolijidad y que el armado
        // las gire; una tabla perfectamente simetrica repetida cincuenta veces
        // se lee como una reja de fabrica.
        arrayOf(
            floatArrayOf(-0.50f, 0.000f),
            floatArrayOf(0.50f, 0.000f),
            floatArrayOf(0.47f, 0.430f),
            floatArrayOf(0.50f, 0.790f),
            floatArrayOf(0.09f, 1.000f),      // la punta, corrida
            floatArrayOf(-0.47f, 0.815f),
            floatArrayOf(-0.50f, 0.410f)
        ),
        0.12f
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
        // Un ramillete de lobulos, no tres bolas.
        //
        // La version anterior eran tres esferas superpuestas y en la foto se
        // leia una ROCA apoyada sobre un palo. Lo que separa una copa de una
        // piedra es el contorno: una copa es mas ancha que alta, tiene el
        // borde mordido y no cierra en ningun lado con una curva limpia. Por
        // eso son ocho masas de tamanos bien distintos, cuatro grandes que
        // arman el volumen y cuatro chicas colgadas afuera que le rompen la
        // silueta.
        fun masa(r: Float) = lathe(
            arrayOf(
                floatArrayOf(0.00f, -r * 0.86f),
                floatArrayOf(r * 0.66f, -r * 0.64f),
                floatArrayOf(r * 1.00f, -r * 0.08f),
                floatArrayOf(r * 0.84f, r * 0.52f),
                floatArrayOf(r * 0.40f, r * 0.84f),
                floatArrayOf(0.00f, r * 0.94f)
            ),
            segmentos = 8
        )
        val piezas = ArrayList<Geometry>()
        // Diez masas de tamanos PARECIDOS y bien encimadas.
        //
        // Con cuatro grandes y cuatro chicas la copa se leia como un racimo de
        // uvas: cada masa se distinguia de la de al lado y ninguna se fundia
        // con el resto. Lo que hace follaje es que las masas se toquen tanto
        // que el ojo deje de contarlas y solo le quede el contorno mordido.
        val puestos = arrayOf(
            floatArrayOf(0.42f, 0.00f, 0.46f, 0.00f),
            floatArrayOf(0.38f, -0.34f, 0.40f, 0.12f),
            floatArrayOf(0.37f, 0.33f, 0.43f, -0.14f),
            floatArrayOf(0.35f, 0.04f, 0.38f, 0.34f),
            floatArrayOf(0.34f, -0.08f, 0.42f, -0.32f),
            floatArrayOf(0.31f, -0.24f, 0.66f, -0.14f),
            floatArrayOf(0.30f, 0.26f, 0.68f, 0.12f),
            floatArrayOf(0.29f, 0.02f, 0.74f, 0.00f),
            floatArrayOf(0.28f, 0.48f, 0.26f, 0.20f),
            floatArrayOf(0.27f, -0.47f, 0.24f, -0.18f)
        )
        for (p in puestos) piezas.add(trasladar(masa(p[0]), p[1], p[2], p[3]))
        return combinar(piezas[0], *piezas.drop(1).toTypedArray())
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
