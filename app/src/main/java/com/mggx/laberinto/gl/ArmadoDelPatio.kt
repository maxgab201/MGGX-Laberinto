package com.mggx.laberinto.gl

import com.mggx.laberinto.maze.ElAscenso
import kotlin.math.PI
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
        PASTO, LOSA, PARED_CASA, PUERTA, VENTANA, ALERO,
        CERCO_TABLA, CERCO_TRAVESANO, BOCA_MINA,
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
        Malla.PARED_CASA -> PatioMeshes.paredDeCasa()
        Malla.PUERTA -> PatioMeshes.puertaDeCasa()
        Malla.VENTANA -> PatioMeshes.ventanaDeCasa()
        Malla.ALERO -> PatioMeshes.aleroDeTejas()
        Malla.CERCO_TABLA -> PatioMeshes.tablaDeCerco()
        Malla.CERCO_TRAVESANO -> PatioMeshes.travesanoDeCerco()
        Malla.BOCA_MINA -> PatioMeshes.bocaDeMina()
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
        // Se planta en una grilla con desorden: en linea se veria sembrado, y
        // el pasto de un patio no esta sembrado en linea.
        val paso = 0.62f
        var gz = paso * 0.5f
        while (gz < lado) {
            var gx = paso * 0.5f
            while (gx < lado) {
                val px = gx + (rnd.nextFloat() - 0.5f) * paso * 0.8f
                val pz = gz + (rnd.nextFloat() - 0.5f) * paso * 0.8f
                // Ni arriba del camino ni pegado a la casa.
                // Ni sobre el camino, ni pegado a la casa, ni justo en la
                // boca de la galeria: ahi el pasto queda a un palmo de la
                // camara y tapa el primer plano con una pared de hojas.
                val enLaBoca = pz > lado - 1.3f && kotlin.math.abs(px - bocaX) < 1.4f
                if (!sobreElCamino(px, pz, bocaX, lado) && pz > 0.9f && !enLaBoca) {
                    out.add(
                        Pieza(
                            Malla.PASTO, px, 0f, pz,
                            0.15f + rnd.nextFloat() * 0.10f,
                            rnd.nextFloat() * 6.283f
                        )
                    )
                }
                gx += paso
            }
            gz += paso
        }

        // ----------------------------------------------------------- camino
        //
        // De la boca de la galeria (abajo) a la puerta de la casa (arriba).
        // Es lo primero que se ve al salir y es lo que dice para donde ir.
        var t = 0f
        while (t <= 1.0001f) {
            val px = bocaX + (medio - bocaX) * t
            val pz = lado - 0.35f - (lado - 1.5f) * t
            out.add(Pieza(Malla.LOSA, px, 0.015f, pz, 0.74f, rnd.nextFloat() * 0.5f - 0.25f))
            // Una losita al costado cada tanto, para que el camino no sea una
            // fila perfecta de baldosas.
            if (rnd.nextFloat() < 0.4f) {
                out.add(
                    Pieza(
                        Malla.LOSA, px + (if (rnd.nextBoolean()) 0.55f else -0.55f), 0.012f,
                        pz + 0.18f, 0.42f, rnd.nextFloat() * 3f
                    )
                )
            }
            t += 1f / 7f
        }

        // ------------------------------------------------------------- casa
        //
        // Ocupa el lado de arriba entero (z = 0), mirando hacia el patio (+Z).
        val altoCasa = 3.1f
        // Un pano mide lo que dice su escala: repartirlos cada 1 m dibujaba
        // tres paredes encima de la misma pared.
        var px = -0.4f
        while (px < lado + 0.4f) {
            out.add(Pieza(Malla.PARED_CASA, px, 0f, 0.18f, altoCasa, 0f))
            px += altoCasa * 0.94f
        }
        // Alero corrido arriba de todo.
        px = 0.5f
        while (px < lado + 0.5f) {
            out.add(Pieza(Malla.ALERO, px, altoCasa - 0.05f, 0.05f, 1.15f, 0f))
            px += 1.05f
        }
        // La puerta va enfrentada al camino.
        out.add(Pieza(Malla.PUERTA, medio, 0f, 0.30f, 2.15f, 0f))
        // Y una ventana a cada lado, si hay lugar.
        out.add(Pieza(Malla.VENTANA, medio - 1.85f, 1.25f, 0.28f, 1.15f, 0f))
        out.add(Pieza(Malla.VENTANA, medio + 1.85f, 1.25f, 0.28f, 1.15f, 0f))

        // ------------------------------------------------------------ cerco
        //
        // Los otros tres lados. Deja ver que hay mundo del otro lado sin que
        // el patio se desarme.
        val altoCerco = 1.25f
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
        // El lado de atras NO lleva cerco: del otro lado no hay vecino, hay
        // el cerro del que saliste. Lleva el marco de tablones de la boca de
        // la mina, que ademas es la misma pieza que sostiene las galerias de
        // abajo: lo ultimo que ves de la mina es lo que mas viste adentro.
        out.add(Pieza(Malla.BOCA_MINA, bocaX, 0f, lado - 0.35f, 2.55f, 0f))

        // ------------------------------------------------------------ arbol
        //
        // Al costado de la casa, no en la esquina de atras. Estaba atras y era
        // un error de encuadre: quedaba a un metro del jugador al salir del
        // tunel, negro y enorme, tapando justo la casa que uno acaba de subir
        // a ver. Adelante y al costado, en cambio, la enmarca.
        val arbolX = lado - 1.6f
        val arbolZ = 2.75f
        out.add(Pieza(Malla.TRONCO, arbolX, 0f, arbolZ, 2.55f, 0.6f))
        out.add(Pieza(Malla.COPA, arbolX, 2.30f, arbolZ, 2.35f, 1.1f))

        // ---------------------------------------------------------- muebles
        //
        // La mesa con dos sillas bajo el arbol, y el banco contra la pared de
        // la casa mirando al patio: los dos lugares donde uno se sentaria.
        out.add(Pieza(Malla.MESA, arbolX - 0.55f, 0f, arbolZ + 1.45f, 0.78f, 0f))
        out.add(Pieza(Malla.SILLA, arbolX - 1.25f, 0f, arbolZ + 1.45f, 0.92f, MEDIA * 0.5f))
        out.add(Pieza(Malla.SILLA, arbolX + 0.10f, 0f, arbolZ + 1.60f, 0.92f, -MEDIA * 0.55f))
        out.add(Pieza(Malla.BANCO, 1.45f, 0f, 1.25f, 1.35f, MEDIA))

        // Macetas contra la pared de la casa, a los lados de la puerta.
        for (d in floatArrayOf(-0.95f, 0.95f)) {
            out.add(Pieza(Malla.MACETA, medio + d, 0f, 0.62f, 0.62f, rnd.nextFloat() * 3f))
        }
        out.add(Pieza(Malla.MACETA, 0.75f, 0f, 0.85f, 0.50f, rnd.nextFloat() * 3f))

        // El balde de la mina, apoyado contra la pared al lado de la puerta.
        // Es el guino: lo trajiste de abajo.
        out.add(Pieza(Malla.BALDE, medio - 1.35f, 0f, 0.55f, 0.46f, 0.7f))

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
        val t = ((lado - 0.35f - z) / (lado - 1.5f)).coerceIn(0f, 1f)
        val cx = bocaX + (medio - bocaX) * t
        return kotlin.math.abs(x - cx) < 0.55f
    }
}
