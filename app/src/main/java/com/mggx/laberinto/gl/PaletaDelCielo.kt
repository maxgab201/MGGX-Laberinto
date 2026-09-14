package com.mggx.laberinto.gl

/**
 * Los colores del cielo del patio, en un solo lugar.
 *
 * Existe porque el cielo se dibuja en la GPU (`Shaders.CIELO_FS`) y eso no se
 * puede mirar desde un test: no hay OpenGL en la JVM. La unica forma de VER el
 * cielo antes de meterlo en el telefono es tener una copia de la cuenta en
 * Kotlin y rasterizarla a mano (ver `VisorDeCielo` en los tests).
 *
 * Con dos copias de la cuenta, lo minimo es que los NUMEROS sean uno solo: si
 * el visor tuviera su propia paleta, la foto que mira uno y el cielo que ve el
 * jugador podrian no tener nada que ver. Es la misma leccion que dejaron
 * `WorldMesh.realFloorHeight` y `Anclajes`: dos cuentas que tienen que dar lo
 * mismo se desincronizan solas, y lo unico que lo frena es que compartan todo
 * lo que se pueda compartir.
 *
 * Todo en lineal (0..1).
 */
object PaletaDelCielo {

    // Es media tarde y no mediodia a proposito: el sol bajo y calido es lo que
    // hace que salir de la mina se sienta un final y no una hora cualquiera.

    /** Arriba de todo. */
    const val CENIT_R = 0.085f
    const val CENIT_G = 0.245f
    const val CENIT_B = 0.740f

    /**
     * Contra el horizonte.
     *
     * La primera version tenia 0.74/0.76/0.82 y el cielo entero se veia
     * NUBLADO: despues del mapeo de tonos (`c/(c+0.85)`) eso queda en un gris
     * de 0.71 que se come el azul en la mitad de abajo de la pantalla, que es
     * justo donde mira el jugador al salir del tunel.
     */
    const val HORIZONTE_R = 0.52f
    const val HORIZONTE_G = 0.64f
    const val HORIZONTE_B = 0.86f

    /** Lo que se ve por debajo del horizonte: tierra lejana con bruma. */
    const val SUELO_R = 0.30f
    const val SUELO_G = 0.31f
    const val SUELO_B = 0.28f

    /**
     * Hacia donde esta el sol, ya normalizado.
     *
     * Adelante y a la izquierda de donde salis (el patio mira a +Z), y bajo.
     * Asi la casa recibe la luz de frente y lo que este parado en el patio
     * tira sombra hacia el camino, que es lo que le da profundidad al
     * encuadre.
     */
    const val SOL_X = -0.4410f
    const val SOL_Y = 0.3608f
    const val SOL_Z = 0.8218f

    const val SOL_R = 1.00f
    const val SOL_G = 0.88f
    const val SOL_B = 0.66f

    // ------------------------------------------------------------- ajustes
    //
    // Los numeros que deciden como se ve el cielo tambien viven aca y no
    // adentro del shader, por el mismo motivo que los colores: son los que uno
    // mueve mirando la foto, y si el shader tuviera los suyos habria que
    // acertarle dos veces a cada cambio.

    /**
     * Cuanto manda el azul del cenit contra lo pale del horizonte.
     *
     * Es el exponente de `pow(altura, k)`. Mas chico = el azul baja mas y solo
     * la franja pegada al horizonte se abre.
     */
    const val POTENCIA_ALTO = 0.32f

    /** El disco del sol: exponente y fuerza. */
    const val SOL_DISCO_POT = 900f
    const val SOL_DISCO_FUERZA = 14f

    /** El halo alrededor: exponente y fuerza. */
    const val SOL_HALO_POT = 5.5f
    const val SOL_HALO_FUERZA = 0.30f

    /** Cuanto se aclara el horizonte del lado del sol. */
    const val DERRAME_SOL = 0.16f

    /**
     * Las nubes.
     *
     * [NUBE_PROYECCION] es cuanto se abre el plano de nubes sobre la cabeza:
     * con el valor original (0.42) mirando para arriba se leia UNA SOLA celda
     * de ruido y el cenit quedaba sin una nube en toda la pantalla.
     */
    const val NUBE_PROYECCION = 0.90f
    const val NUBE_ESCALA = 2.2f
    // Con 0.46/0.74 la mitad de abajo del cielo quedaba tapada de nubes, y
    // justo esa es la franja que mira el jugador al salir del tunel: el
    // momento del juego tiene que ser "se abrio el cielo", no "esta nublado".
    const val NUBE_DESDE = 0.50f
    const val NUBE_HASTA = 0.80f
    const val NUBE_FUERZA = 0.90f
}
