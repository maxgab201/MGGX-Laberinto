package com.mggx.laberinto.net

import kotlin.random.Random

/**
 * El codigo con el que se invita a una sala, y el id con el que se identifica
 * cada jugador adentro.
 *
 * El codigo esta pensado para DICTARSE: uno se lo pasa al otro por teléfono o
 * gritando de una pieza a la otra, asi que no puede tener caracteres que se
 * confundan al escucharlos o al leerlos.
 */
object CodigoSala {

    /**
     * Alfabeto sin las que se confunden entre si: I con 1 con L, O con 0.
     * "Ele" y "uno" al oido son la misma cosa, y una sala a la que no podes
     * entrar porque escribiste un cero en vez de una O no sirve para nada.
     */
    const val LETRAS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    /** Cuantas letras tiene un codigo de sala. */
    const val LARGO = 4

    /** Un codigo nuevo, listo para dictar. */
    fun nuevo(rnd: Random = Random.Default): String =
        (1..LARGO).map { LETRAS[rnd.nextInt(LETRAS.length)] }.joinToString("")

    /**
     * Deja el codigo como lo espera la sala: en mayusculas, sin espacios y sin
     * las letras que no existen en el alfabeto. Asi da igual como lo escriba
     * el que entra ("a7k2", "A7-K2", "a 7 k 2" son la misma sala).
     */
    fun normalizar(escrito: String): String =
        escrito.uppercase().filter { LETRAS.contains(it) }.take(LARGO)

    /** Un codigo sirve cuando tiene las cuatro letras del alfabeto bueno. */
    fun valido(escrito: String): Boolean = normalizar(escrito).length == LARGO

    /**
     * Id corto y propio de cada jugador dentro de la sala.
     *
     * No es el nombre: el nombre lo elige uno y se puede repetir (dos "Maxi"
     * en la misma sala), y esto tiene que ser unico o los dos serian el mismo
     * jugador para el protocolo.
     */
    fun idDeJugador(rnd: Random = Random.Default): String =
        (1..6).map { LETRAS[rnd.nextInt(LETRAS.length)] }.joinToString("")
}
