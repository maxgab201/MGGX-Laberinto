package com.mggx.laberinto.core

/**
 * Las decisiones de cuanto trabajo hace falta hacer segun donde este el
 * jugador. Aritmetica pura: nada de Android adentro, asi se verifica en un
 * test de JVM.
 *
 * El problema que resuelve: el juego dibujaba SIEMPRE al maximo de cuadros por
 * segundo y mantenia la pantalla prendida a la fuerza SIEMPRE, incluso con el
 * menu de pausa abierto sobre una escena congelada o con el jugador leyendo la
 * tienda. Un telefono no tiene enchufe: dibujar 60 veces por segundo una
 * imagen que no cambia es gastar bateria para nada.
 */
object AhorroDeEnergia {

    /** Donde esta el jugador ahora mismo. */
    enum class Donde {
        /** Jugando de verdad: la cueva se mueve y el toque tiene que responder ya. */
        JUGANDO,

        /** El menu de pausa, o ganaste/perdiste: la escena esta congelada detras. */
        PAUSA,

        /** La vitrina del lobby: la camara pasea sola, despacio. */
        VITRINA,

        /** Tienda, ajustes, multijugador: no hay nada de 3D dibujandose. */
        MENU
    }

    /** Cuadros por segundo con la escena quieta detras de un menu. */
    const val FPS_PAUSA = 10

    /** Cuadros por segundo del paseo del lobby. */
    const val FPS_VITRINA = 30

    /**
     * Cuantos cuadros por segundo hace falta dibujar.
     *
     * [fpsElegido] es lo que el jugador puso en Ajustes; nunca se sube por
     * arriba de eso, solo se baja. Si alguien eligio 30 porque su telefono se
     * calienta, la vitrina no le va a meter 30 igual: le va a meter 30 como
     * techo y la pausa 10.
     */
    fun fpsObjetivo(donde: Donde, fpsElegido: Int): Int {
        val techo = if (fpsElegido > 0) fpsElegido else 60
        return when (donde) {
            Donde.JUGANDO -> techo
            Donde.PAUSA -> minOf(techo, FPS_PAUSA)
            Donde.VITRINA -> minOf(techo, FPS_VITRINA)
            // En los menus no se dibuja 3D: el hilo de GL esta parado.
            Donde.MENU -> 0
        }
    }

    /**
     * Si hay que forzar la pantalla a quedarse prendida.
     *
     * Jugando si: en un juego de primera persona podes estar un rato largo sin
     * tocar la pantalla (caminando con el joystick sostenido, o mirando) y que
     * se apague seria insoportable. En pausa tambien, porque leer el mapa o el
     * inventario no es estar inactivo.
     *
     * En los menus NO. Ahi el telefono se apaga solo como con cualquier otra
     * app, que es lo que espera cualquiera. Antes la bandera se prendia al
     * arrancar y no se apagaba nunca: dejabas el juego abierto en la tienda y
     * la pantalla se quedaba encendida hasta que se acababa la bateria.
     */
    fun mantenerPantallaPrendida(donde: Donde): Boolean =
        donde == Donde.JUGANDO || donde == Donde.PAUSA

    /**
     * Cada cuantos milisegundos refrescar el HUD (vida, aguante, minimapa).
     *
     * Con la partida congelada detras del menu de pausa no cambia ni una barra,
     * asi que rehacer el dibujo 30 veces por segundo es trabajo tirado.
     */
    fun intervaloHudMs(donde: Donde): Long =
        if (donde == Donde.JUGANDO) 33L else 125L

    /** Nanosegundos entre cuadros para [fps]. 0 si no hay techo. */
    fun intervaloCuadroNs(fps: Int): Long =
        if (fps <= 0) 0L else 1_000_000_000L / fps
}
