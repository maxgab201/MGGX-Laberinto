package com.mggx.laberinto.net

/**
 * Decide si se puede abrir una sala, y si no, POR QUE no.
 *
 * Aritmetica pura, sin Android ni Firebase adentro, para poder verificar los
 * casos en un test de JVM. Quien la llama le pasa los tres datos que hacen
 * falta; conseguirlos si depende del telefono.
 *
 * Por que existe: el juego entero anda sin internet (la cueva, las texturas,
 * la musica y los efectos se generan por codigo, y el perfil vive en el
 * telefono), pero el multijugador NO puede: necesita el relay. El problema es
 * que Firebase Realtime Database, sin red, no falla: guarda lo que le pedis en
 * una cola local y lo manda "cuando se pueda". Para el jugador eso se ve como
 * una sala que se queda en "esperando" para siempre, sin ningun cartel y sin
 * ningun error. Peor que un mensaje de error es un juego que no contesta.
 */
object PuedeJugarEnRed {

    /** Que falta para poder abrir una sala. [OK] es que no falta nada. */
    enum class Motivo { OK, SIN_INTERNET, SIN_CONFIGURACION, FIREBASE_NO_ARRANCO }

    /**
     * @param hayInternet el telefono tiene una red utilizable ahora mismo.
     * @param configEnElApk el APK trae la configuracion de Firebase.
     * @param firebaseArranco el SDK de Firebase quedo iniciado.
     */
    fun evaluar(
        hayInternet: Boolean,
        configEnElApk: Boolean,
        firebaseArranco: Boolean
    ): Motivo = when {
        // Primero la red: es lo mas comun y lo unico que el jugador puede
        // arreglar solo. Preguntarlo despues de lo otro haria que alguien sin
        // datos leyera un cartel sobre recompilar el APK.
        !hayInternet -> Motivo.SIN_INTERNET
        !configEnElApk -> Motivo.SIN_CONFIGURACION
        !firebaseArranco -> Motivo.FIREBASE_NO_ARRANCO
        else -> Motivo.OK
    }

    /** El cartel que se le muestra al jugador. */
    fun mensaje(motivo: Motivo): String = when (motivo) {
        Motivo.OK -> ""
        Motivo.SIN_INTERNET ->
            "El multijugador necesita internet y ahora no hay. " +
                "Prendé los datos o el wifi y probá de nuevo. " +
                "El resto del juego anda igual sin conexion."
        Motivo.SIN_CONFIGURACION ->
            "Este APK se compilo sin la configuracion de Firebase " +
                "(falta google-services.json). Hay que compilarlo de nuevo con " +
                "el archivo puesto: el multijugador no puede andar."
        Motivo.FIREBASE_NO_ARRANCO ->
            "La configuracion de Firebase esta en el APK, pero el SDK no " +
                "arranco igual. Probá cerrar la app del todo y volver a abrirla."
    }
}
