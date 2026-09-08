package com.mggx.laberinto.net

/**
 * Las cuentas de TransporteFirebase que NO dependen del SDK de Firebase.
 *
 * Separado a proposito: `TransporteFirebase.kt` importa `com.google.firebase.*`,
 * y en los tests de JVM (`unitTests.isReturnDefaultValues = true`, ver
 * `app/build.gradle.kts`) esas clases no hacen nada de verdad. Lo que si se
 * puede probar sin Firebase es esto: como se arma el path de una sala y cada
 * cuanto toca podar mensajes viejos.
 */
object RelayFirebase {

    /** Cada cuantos mensajes enviados conviene disparar una poda. */
    const val CADA_CUANTOS_ENVIOS_PODAR = 40L

    /**
     * Cuanto dura un mensaje antes de considerarse basura, en milisegundos.
     *
     * No hace falta Cloud Functions (esas piden tarjeta, aunque no se
     * cobren): el propio cliente que manda el mensaje 40 es el que barre lo
     * que quedo de mas de 30 segundos. Es plata de sobra: a 10 mensajes por
     * segundo (el ritmo de POSE), 40 mensajes son 4 segundos, muy por debajo
     * de los 30 que se tardan en volverse basura.
     */
    const val VIDA_MENSAJE_MS = 30_000L

    /** Path de Firebase Realtime Database donde viven los mensajes de una sala. */
    fun pathDeSala(codigoSala: String): String = "salas/${sanearCodigo(codigoSala)}/msgs"

    /**
     * Un segmento de path de Firebase no puede tener `. # $ [ ]` ni `/`. El
     * codigo de sala lo escribe/dicta cualquiera, asi que se filtra a solo
     * letras y numeros antes de usarlo, igual que `NetProtocol.limpiar` filtra
     * los nombres de jugador.
     */
    fun sanearCodigo(codigoSala: String): String =
        codigoSala.filter { it.isLetterOrDigit() }.uppercase().take(8).ifEmpty { "SALA" }

    /** True cuando, tras hacer [envios] envios en total, toca podar. */
    fun tocaPodar(envios: Long): Boolean = envios > 0 && envios % CADA_CUANTOS_ENVIOS_PODAR == 0L
}
