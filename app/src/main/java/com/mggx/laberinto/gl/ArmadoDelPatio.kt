package com.mggx.laberinto.gl

import com.mggx.laberinto.maze.ElAscenso
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Como esta amueblado el patio de tu casa.
 *
 * El patio es el final del juego (ver [ElAscenso]) y hay UNO solo, asi que no
 * se genera al azar: esta puesto a mano, pieza por pieza, como se pone un
 * escenario. Lo unico que sale de la semilla son los detalles que dan vida
 * (donde cae cada mata de pasto, que tan torcida esta cada maceta).
 *
 * Como todo el armado de este juego, vive afuera del renderer para que se
 * pueda mirar y medir en un test sin OpenGL (ver RetratosDelPatioTest y
 * PatioArmadoTest).
 *
 * ## La idea del lugar
 *
 * Saliste de una mina. Lo primero que tiene que pasar al pisar el pasto es que
 * se entienda, sin un solo cartel, que **llegaste**. Para eso el patio esta
 * ordenado alrededor de tres cosas que se leen de lejos:
 *
 *  1. **La casa**, ocupando un lado entero, con la puerta al frente de donde
 *     salis. Es a donde mira el jugador cuando sale del tunel.
 *  2. **El camino de losas**, que va de la boca de la galeria a esa puerta.
 *     No es decoracion: es la flecha.
 *  3. **El arbol y la sombra debajo**, en la esquina opuesta, que es lo que
 *     hace que el patio tenga un lugar "lejos" y no sea una caja.
 *
 * El resto —la ropa tendida, el balde de la mina apoyado en la pared, las
 * macetas— es lo que dice que aca vive alguien.
 */
object ArmadoDelPatio {

    /** Cada cosa que se planta en el patio. */
    enum class Malla {
        PASTO, LOSA, CASA, TEJAS, CHIMENEA, PUERTA, VENTANA,
        CERCO_TABLA, CERCO_TRAVESANO, BOCA_MINA, CERRO, ARBOL_LEJOS, FAROL,
        TRONCO, COPA, BANCO, MESA, SILLA, MACETA, ROPA, BALDE
    }

    /**
     * Una pieza puesta, en METROS y en coordenadas del mundo relativas a la
     * esquina del patio (la casilla de arriba a la izquierda del bloque).
     *
     * Solo geometria: el color lo pone el renderer, igual que con los bichos.
     */
    class Pieza(
        val malla: Malla,
        val x: Float, val y: Float, val z: Float,
        val escala: Float,
        val giro: Float
    )

    fun geometria(m: Malla): PropMeshes.Geometry = when (m) {
        Malla.PASTO -> PatioMeshes.mataDePasto()
        Malla.LOSA -> PatioMeshes.losaDePiedra()
        Malla.CASA -> PatioMeshes.moduloDeCasa()
        Malla.TEJAS -> PatioMeshes.tejasDelModulo()
        Malla.CHIMENEA -> PatioMeshes.chimeneaDeCasa()
        Malla.PUERTA -> PatioMeshes.puertaDeCasa()
        Malla.VENTANA -> PatioMeshes.ventanaDeCasa()
        Malla.CERCO_TABLA -> PatioMeshes.tablaDeCerco()
        Malla.CERCO_TRAVESANO -> PatioMeshes.travesanoDeCerco()
        Malla.BOCA_MINA -> PatioMeshes.bocaDeMina()
        Malla.FAROL -> PatioMeshes.farolDePuerta()
        Malla.CERRO -> PatioMeshes.cerroLejano()
        Malla.ARBOL_LEJOS -> PatioMeshes.arbolLejano()
        Malla.TRONCO -> PatioMeshes.troncoDeArbol()
        Malla.COPA -> PatioMeshes.copaDeArbol()
        Malla.BANCO -> PatioMeshes.bancoDeMadera()
        Malla.MESA -> PatioMeshes.mesaDePatio()
        Malla.SILLA -> PatioMeshes.sillaDePatio()
        Malla.MACETA -> PatioMeshes.macetaConPlanta()
        Malla.ROPA -> PatioMeshes.ropaTendida()
        Malla.BALDE -> PatioMeshes.baldeDeMina()
    }

    private const val MEDIA = PI.toFloat()

    /**
     * Las mallas que estan LEJOS, fuera del patio.
     *
     * El renderer las trata distinto en dos cosas: no las descarta por
     * distancia (estan lejos por definicion, descartarlas seria no dibujarlas
     * nunca) y las pinta con perspectiva aerea. Y los tests que comprueban que
     * nada se salga del patio tienen que saltearlas por el mismo motivo.
     */
    fun esDelFondo(m: Malla): Boolean = m == Malla.CERRO || m == Malla.ARBOL_LEJOS

    /**
     * Arma el patio entero.
     *
     * @param lado cuantos metros de lado tiene el patio.
     * @param bocaX en que metro del lado de abajo desemboca la galeria. La
     *   casa y el camino se acomodan a eso: el camino tiene que arrancar donde
     *   salis, no en un punto fijo.
     */
    fun armar(lado: Float, bocaX: Float, semilla: Long): List<Pieza> {
        val out = ArrayList<Pieza>(256)
        val rnd = Random(semilla)
        val medio = lado * 0.5f

        // ------------------------------------------------------------ pasto
        //
        // Matas GRANDES y en grupos, no una siembra pareja de matitas.
        //
        // La primera version plantaba una mata cada 62 cm, todas del mismo
        // tamano, en toda la superficie: en la foto se veia un yuyal parejo de
        // punta a punta, y un pasto parejo no se lee como pasto sino como una
        // alfombra con pelos. Lo que hace patio es que haya MATAS y haya
        // pelado entre medio — y ademas la textura del piso ya trae su propio
        // pasto corto, asi que estas matas son lo que sobresale, no el pasto
        // entero.
        val paso = 1.25f
        var gz = paso * 0.5f
        while (gz < lado) {
            var gx = paso * 0.5f
            while (gx < lado) {
                val px = gx + (rnd.nextFloat() - 0.5f) * paso * 0.9f
                val pz = gz + (rnd.nextFloat() - 0.5f) * paso * 0.9f
                // Grupos: el sorteo va por zonas, asi que las matas se juntan
                // en manchones en vez de repartirse parejo.
                val zona = ((px * 0.37f).toInt() + (pz * 0.41f).toInt() * 3) and 3
                val salteo = if (zona == 0) 0.62f else 0.24f
                // Ni sobre el camino, ni pegado a la casa, ni justo en la boca
                // de la galeria: ahi el pasto queda a un palmo de la camara y
                // tapa el primer plano con una pared de hojas.
                val enLaBoca = pz > lado - 1.8f && kotlin.math.abs(px - bocaX) < 1.8f
                if (rnd.nextFloat() >= salteo &&
                    !sobreElCamino(px, pz, bocaX, lado) && pz > 2.4f && !enLaBoca
                ) {
                    out.add(
                        Pieza(
                            Malla.PASTO, px, 0f, pz,
                            0.38f + rnd.nextFloat() * 0.26f,
                            rnd.nextFloat() * 6.283f
                        )
                    )
                }
                gx += paso
            }
            gz += paso
        }

        // ------------------------------------------------------------- casa
        //
        // Ocupa el lado de arriba entero, mirando hacia el patio (+Z), con el
        // fondo metido en el cerro del que acabas de salir.
        //
        // Se arma encadenando modulos de un metro de casa (ver
        // [PatioMeshes.moduloDeCasa]): el pipeline de instancias solo admite
        // escala UNIFORME, asi que una casa de una sola pieza mediria tan
        // ancho como alto. El paso entre modulos es exactamente su escala,
        // porque el modulo mide 1 de largo — asi el techo sale corrido y sin
        // junta.
        // El alto sale de que los modulos entren JUSTO en el ancho del patio.
        //
        // Como el modulo mide 1 de largo y 1 de alto, su escala es las dos
        // cosas a la vez: elegir el alto es elegir el paso. Si se elige un
        // numero redondo, la hilera termina en cualquier lado y hay que
        // pasarse de largo metiendo modulos adentro de la roca para que no
        // quede un hueco en la esquina. Sacandolo del ancho del patio, la casa
        // arranca en una punta y termina en la otra, exacto.
        //
        // Y la casa NO ocupa el lado entero. Cuando lo ocupaba, veintiun
        // metros de largo por cuatro de alto se leian como un galpon: la
        // proporcion de una casa no es esa. Ocupa poco mas de la mitad del
        // fondo, centrada, y a los costados sigue el cerco — que ademas deja
        // ver que hay mundo detras.
        val anchoCasa = lado * 0.58f
        val modulos = (anchoCasa / 4.1f).roundToInt().coerceAtLeast(3)
        val ALTO_CASA = anchoCasa / modulos
        // La casa va ADENTRO del patio, no metida en el borde.
        //
        // Antes su fondo quedaba abajo del cero, o sea adentro del bloque de
        // roca, y no molestaba porque el borde del patio media siete metros y
        // medio y la tapaba entera. Ahora el borde mide 2,45: la mitad de
        // atras de la casa quedaria asomando por encima de la roca, flotando.
        val zCasa = ALTO_CASA * 0.40f
        val casaX0 = medio - anchoCasa * 0.5f
        /** Donde queda la cara de adelante de la pared. */
        val frente = zCasa + PatioMeshes.FONDO_CASA * ALTO_CASA
        val alero = PatioMeshes.ALTURA_ALERO * ALTO_CASA

        var px = casaX0 + ALTO_CASA * 0.5f
        for (k in 0 until modulos) {
            out.add(Pieza(Malla.CASA, px, 0f, zCasa, ALTO_CASA, 0f))
            // Mismo sitio y misma escala que el modulo: las tejas van aparte
            // solo porque llevan otro color, no porque se ubiquen aparte.
            out.add(Pieza(Malla.TEJAS, px, 0f, zCasa, ALTO_CASA, 0f))
            px += ALTO_CASA
        }
        // La chimenea, a caballo de la cumbrera y corrida del medio: centrada
        // se leeria como un adorno simetrico, y lo que tiene que parecer es
        // que la casa la fue creciendo alguien.
        out.add(Pieza(Malla.CHIMENEA, medio - ALTO_CASA * 0.58f, ALTO_CASA - 0.30f, zCasa, 1.30f, 0f))

        // La puerta va enfrentada al camino.
        out.add(Pieza(Malla.PUERTA, medio, 0f, frente + 0.02f, 2.15f, 0f))
        // Y el farol prendido al costado: es lo unico del patio que da luz, y
        // lo que dice que te estaban esperando.
        out.add(Pieza(Malla.FAROL, medio + 0.95f, 0.72f, frente, 1.35f, 0f))
        // Y una ventana a cada lado. El alto sale de donde esta el alero: una
        // ventana que lo cruza se ve metida en el techo.
        val altoVentana = alero * 0.61f
        // Colgada del alero y no apoyada en un numero fijo: una ventana que
        // cruza el alero se ve metida adentro del techo.
        val yVentana = alero - altoVentana - 0.14f
        out.add(Pieza(Malla.VENTANA, medio - 2.35f, yVentana, frente + 0.02f, altoVentana, 0f))
        out.add(Pieza(Malla.VENTANA, medio + 2.35f, yVentana, frente + 0.02f, altoVentana, 0f))

        // ----------------------------------------------------------- camino
        //
        // De la boca de la galeria (abajo) a la puerta de la casa (arriba). Es
        // lo primero que se ve al salir y es lo que dice para donde ir: no es
        // decoracion, es la flecha.
        //
        // Va DESPUES de la casa en el codigo porque termina en la puerta, y la
        // puerta esta donde este el frente de la pared. Con el camino armado
        // antes, el ultimo metro apuntaba a un punto fijo que no tenia nada
        // que ver con la casa.
        //
        // Y es ANCHO: dos losas por escalon con su desorden, mas alguna suelta
        // al costado. La version anterior era una sola fila de losas de 74 cm
        // y de lejos se leia como una hilera de piedritas, no como un camino.
        val pasos = 11
        for (k in 0..pasos) {
            val t = k.toFloat() / pasos
            val cx = bocaX + (medio - bocaX) * t
            val cz = lado - 0.5f - (lado - 0.5f - (frente + 0.75f)) * t
            for (lado2 in intArrayOf(-1, 1)) {
                out.add(
                    Pieza(
                        Malla.LOSA,
                        cx + lado2 * (0.40f + rnd.nextFloat() * 0.10f),
                        0.02f,
                        cz + (rnd.nextFloat() - 0.5f) * 0.22f,
                        0.86f + rnd.nextFloat() * 0.22f,
                        rnd.nextFloat() * 6.283f
                    )
                )
            }
            // Alguna losa suelta desbordando el borde, como si el camino se
            // hubiera ido ensanchando con el uso.
            if (rnd.nextFloat() < 0.45f) {
                out.add(
                    Pieza(
                        Malla.LOSA,
                        cx + (if (rnd.nextBoolean()) 1f else -1f) * (0.95f + rnd.nextFloat() * 0.35f),
                        0.015f,
                        cz + (rnd.nextFloat() - 0.5f) * 0.5f,
                        0.46f + rnd.nextFloat() * 0.22f,
                        rnd.nextFloat() * 6.283f
                    )
                )
            }
        }

        // ------------------------------------------------------------ cerco
        //
        // Los otros tres lados. Deja ver que hay mundo del otro lado sin que
        // el patio se desarme.
        val altoCerco = 1.05f
        fun cerco(x0: Float, z0: Float, x1: Float, z1: Float, giro: Float) {
            val largo = kotlin.math.hypot(x1 - x0, z1 - z0)
            val n = (largo / 0.30f).toInt().coerceAtLeast(2)
            for (k in 0..n) {
                val f = k.toFloat() / n
                out.add(
                    Pieza(
                        Malla.CERCO_TABLA, x0 + (x1 - x0) * f, 0f, z0 + (z1 - z0) * f,
                        altoCerco, giro
                    )
                )
            }
            // Dos travesanos que las atan, hechos de tramos cortos.
            //
            // NO se puede poner uno solo y estirarlo: la escala de una
            // instancia es uniforme en los tres ejes (ver InstancedShape.add),
            // asi que un riel "estirado" a 14 metros de largo tambien mide 14
            // de alto y de ancho. Quedaban dos losas negras gigantes tapando
            // las esquinas del patio — y no se veia en el codigo, se vio en la
            // primera foto del patio armado.
            val tramo = 0.34f
            val cuantos = (largo / tramo).toInt().coerceAtLeast(1)
            for (y in floatArrayOf(0.34f, 0.86f)) {
                for (k in 0 until cuantos) {
                    val f = (k + 0.5f) / cuantos
                    out.add(
                        Pieza(
                            Malla.CERCO_TRAVESANO, x0 + (x1 - x0) * f, y, z0 + (z1 - z0) * f,
                            tramo * 1.10f, giro
                        )
                    )
                }
            }
        }
        cerco(0.18f, 0.6f, 0.18f, lado - 0.2f, MEDIA * 0.5f)          // lado izquierdo
        cerco(lado - 0.18f, 0.6f, lado - 0.18f, lado - 0.2f, MEDIA * 0.5f)  // derecho
        // Y los dos tramos del fondo que la casa no tapa. Sin ellos el patio
        // se abria a los costados de la casa y se veia el vacio.
        cerco(0.30f, 0.30f, casaX0 - 0.10f, 0.30f, 0f)
        cerco(casaX0 + anchoCasa + 0.10f, 0.30f, lado - 0.30f, 0.30f, 0f)
        // El lado de atras NO lleva cerco: del otro lado no hay vecino, hay
        // el cerro del que saliste. Lleva el marco de tablones de la boca de
        // la mina, que ademas es la misma pieza que sostiene las galerias de
        // abajo: lo ultimo que ves de la mina es lo que mas viste adentro.
        out.add(Pieza(Malla.BOCA_MINA, bocaX, 0f, lado - 0.35f, 2.55f, 0f))

        // ------------------------------------------------------------- lejos
        //
        // Cerros y arboles del otro lado del cerco. Sin esto el mundo se
        // termina en la ultima tabla, y ademas el patio se queda sin ESCALA:
        // si todo lo que se ve esta a la misma distancia, el ojo no tiene con
        // que medir cuan grande es la casa.
        //
        // Van en coordenadas del patio igual que todo lo demas, nada mas que
        // con numeros que se salen de el. El renderer los pinta con
        // perspectiva aerea (ver colorDelPatio).
        // Tres anillos de cerros a distinta distancia. Que haya CAPAS es lo
        // que da hondura: con todos los cerros a la misma distancia se ve un
        // telon pintado, por bien dibujado que este.
        //
        // Y van lejos de verdad. La primera version los ponia a cuarenta
        // metros y en la foto se veian como penascos apoyados contra el cerco:
        // un cerro de treinta metros a cuarenta de distancia te tapa un tercio
        // del cielo.
        val cerros = arrayOf(
            //           x        z      alto
            floatArrayOf(-92f, -76f, 15f),
            floatArrayOf(-16f, -124f, 21f),
            floatArrayOf(68f, -104f, 14f),
            floatArrayOf(136f, -40f, 18f),
            floatArrayOf(156f, 68f, 13f),
            floatArrayOf(104f, 148f, 16f),
            floatArrayOf(-4f, 172f, 19f),
            floatArrayOf(-88f, 116f, 14f),
            floatArrayOf(-124f, 20f, 16f),
            // El anillo de en medio.
            floatArrayOf(-58f, -46f, 8.5f),
            floatArrayOf(52f, -58f, 7.5f),
            floatArrayOf(86f, 26f, 7f),
            floatArrayOf(-64f, 62f, 8f)
        )
        for (c in cerros) {
            out.add(Pieza(Malla.CERRO, lado * 0.5f + c[0], 0f, lado * 0.5f + c[1], c[2], c[0] * 0.13f))
        }
        // Arboles sueltos y en grupitos, mas cerca que los cerros.
        val rndLejos = Random(semilla xor 0x5EED)
        for (k in 0 until 32) {
            val a = k * 0.2417f * 6.283f + rndLejos.nextFloat() * 0.6f
            val r = 34f + rndLejos.nextFloat() * 40f
            val x = lado * 0.5f + kotlin.math.cos(a.toDouble()).toFloat() * r * 1.25f
            val z = lado * 0.5f + kotlin.math.sin(a.toDouble()).toFloat() * r
            // Nada adentro del patio ni pegado al cerco.
            if (x > -8f && x < lado + 8f && z > -8f && z < lado + 8f) continue
            out.add(
                Pieza(
                    Malla.ARBOL_LEJOS, x, 0f, z,
                    6.5f + rndLejos.nextFloat() * 5.5f,
                    rndLejos.nextFloat() * 6.283f
                )
            )
        }

        // ------------------------------------------------------------ arbol
        //
        // Al costado de la casa, no en la esquina de atras. Estaba atras y era
        // un error de encuadre: quedaba a un metro del jugador al salir del
        // tunel, negro y enorme, tapando justo la casa que uno acaba de subir
        // a ver. Adelante y al costado, en cambio, la enmarca.
        // Grande: un arbol chico al costado no enmarca nada. Este tiene que
        // competir en altura con la casa, que es lo que hace que el patio
        // tenga dos cosas y no una sola.
        val arbolX = lado - 5.0f
        val arbolZ = frente + 4.4f
        out.add(Pieza(Malla.TRONCO, arbolX, 0f, arbolZ, 3.40f, 0.6f))
        out.add(Pieza(Malla.COPA, arbolX, 2.80f, arbolZ, 3.90f, 1.1f))

        // ---------------------------------------------------------- muebles
        //
        // La mesa con dos sillas bajo el arbol, y el banco contra la pared de
        // la casa mirando al patio: los dos lugares donde uno se sentaria.
        out.add(Pieza(Malla.MESA, arbolX - 0.65f, 0f, arbolZ + 1.75f, 0.78f, 0f))
        out.add(Pieza(Malla.SILLA, arbolX - 1.40f, 0f, arbolZ + 1.75f, 0.92f, MEDIA * 0.5f))
        out.add(Pieza(Malla.SILLA, arbolX + 0.10f, 0f, arbolZ + 1.95f, 0.92f, -MEDIA * 0.55f))
        out.add(Pieza(Malla.BANCO, 1.45f, 0f, frente + 0.85f, 1.35f, MEDIA))

        // Macetas contra la pared de la casa, a los lados de la puerta.
        for (d in floatArrayOf(-0.95f, 0.95f)) {
            out.add(Pieza(Malla.MACETA, medio + d, 0f, frente + 0.34f, 0.62f, rnd.nextFloat() * 3f))
        }
        out.add(Pieza(Malla.MACETA, 0.75f, 0f, frente + 0.55f, 0.50f, rnd.nextFloat() * 3f))

        // El balde de la mina, apoyado contra la pared al lado de la puerta.
        // Es el guino: lo trajiste de abajo.
        out.add(Pieza(Malla.BALDE, medio - 1.35f, 0f, frente + 0.30f, 0.46f, 0.7f))

        // ------------------------------------------------------- la ropa
        //
        // Tendida entre el arbol y la casa, cruzando el patio. De todo lo que
        // hay aca, es lo que mas dice que la casa esta habitada.
        val ropaZ = lado - 0.95f
        for (k in 0 until 4) {
            val f = (k + 1) / 5f
            out.add(
                Pieza(
                    Malla.ROPA, 1.1f + f * (lado - 2.6f), 1.62f, ropaZ,
                    0.68f + (k % 2) * 0.12f, if (k % 2 == 0) 0f else 0.25f
                )
            )
        }

        return out
    }

    /** Si un punto cae sobre el camino de losas (para no plantar pasto ahi). */
    private fun sobreElCamino(x: Float, z: Float, bocaX: Float, lado: Float): Boolean {
        val medio = lado * 0.5f
        val t = ((lado - 0.5f - z) / (lado - 2.6f)).coerceIn(0f, 1f)
        val cx = bocaX + (medio - bocaX) * t
        // Mas ancho que el camino dibujado: el pasto no tiene que crecer
        // pegado al borde de las losas, tiene que dejarle aire.
        return kotlin.math.abs(x - cx) < 1.15f
    }
}
