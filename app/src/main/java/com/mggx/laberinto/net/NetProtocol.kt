package com.mggx.laberinto.net

/**
 * Protocolo de multijugador de MGGX Laberinto.
 *
 * La idea de fondo, que es lo que hace que esto sea barato: el laberinto es
 * DETERMINISTA. Con el nivel y la semilla, dos telefonos generan exactamente
 * la misma cueva, con las mismas monedas, las mismas trampas y el mismo
 * relieve. Entonces por la red no viaja NUNCA el mapa: viaja solo
 *
 *   - quien esta en la sala y con que semilla se juega,
 *   - donde esta cada uno y como esta parado, muchas veces por segundo,
 *   - y los hechos que cambian el mundo (agarre esta moneda, rompi esta
 *     pared, pise esta trampa), que son pocos y se mandan de a uno.
 *
 * Con eso alcanza para las dos modalidades pensadas: Carrera y Cooperativo.
 *
 * Los mensajes son texto plano separado por barras verticales. Es feo de
 * mirar pero es corto, no necesita ninguna libreria y se puede leer en un log
 * cuando algo no anda, que es exactamente lo que uno quiere cuando esta
 * peleando con la red.
 */
object NetProtocol {

    /** Version del protocolo. Dos clientes con versiones distintas no juegan. */
    const val VERSION = 1

    /** Separador de campos. No aparece en ningun nombre de jugador (se filtra). */
    const val SEP = '|'

    enum class Tipo(val codigo: String) {
        /** Entro a la sala: nombre y skin. */
        UNIRSE("JOIN"),
        /** El anfitrion reparte la partida: modo, nivel y semilla. */
        ARRANQUE("START"),
        /** Posicion y postura de un jugador. Es el unico que va seguido. */
        POSE("POSE"),
        /** Alguien agarro un objeto del nivel. */
        TOMAR("TAKE"),
        /** Alguien rompio una pared con el pico. */
        ROMPER("BREAK"),
        /** Alguien salto una trampa. */
        TRAMPA("TRAP"),
        /** Alguien llego a la salida. */
        LLEGADA("FINISH"),
        /** Alguien se quedo sin vida (en cooperativo se puede levantar). */
        CAIDO("DOWN"),
        /** Levantaron a un caido. */
        REVIVIR("UP"),
        /** Se fue de la sala. */
        SALIR("LEAVE"),
        /**
         * Donde estan los bichos. Lo manda SOLO el anfitrion.
         *
         * Los bichos nacen iguales en los dos telefonos (la cueva es la misma)
         * pero de ahi en mas cada cabeza decide por su cuenta, asi que al rato
         * estaban en lugares distintos en cada pantalla. Con esto hay uno solo
         * que decide y los demas lo copian.
         */
        BICHOS("MOBS"),
        /** Latido para saber que sigue conectado. */
        PING("PING");

        companion object {
            private val porCodigo = entries.associateBy { it.codigo }
            fun de(codigo: String): Tipo? = porCodigo[codigo]
        }
    }

    /** Las dos modalidades pensadas. */
    enum class Modo(val etiqueta: String, val explicacion: String) {
        CARRERA(
            "Carrera",
            "Todos bajan al mismo laberinto al mismo tiempo. Gana el primero que sale."
        ),
        COOPERATIVO(
            "Cooperativo",
            "Bajan juntos, comparten los ecos y se pueden levantar entre ustedes."
        )
    }

    /**
     * Un mensaje ya armado. [de] es el id corto del jugador que lo manda y
     * [args] son los campos propios de cada tipo.
     */
    data class Mensaje(val tipo: Tipo, val de: String, val args: List<String>) {

        fun arg(i: Int): String = args.getOrElse(i) { "" }
        fun num(i: Int): Float = arg(i).toFloatOrNull() ?: 0f
        fun entero(i: Int): Int = arg(i).toIntOrNull() ?: 0
        fun largo(i: Int): Long = arg(i).toLongOrNull() ?: 0L

        fun codificar(): String = buildString {
            append(VERSION); append(SEP)
            append(tipo.codigo); append(SEP)
            append(limpiar(de))
            for (a in args) { append(SEP); append(limpiar(a)) }
        }
    }

    /**
     * Saca de un campo cualquier cosa que pueda romper el formato. Un nombre
     * con una barra vertical adentro correria todos los campos siguientes, y
     * eso lo puede escribir cualquiera desde la pantalla de sala.
     */
    fun limpiar(s: String): String =
        s.replace(SEP, '/').replace('\n', ' ').replace('\r', ' ').trim().take(24)

    /** Devuelve null si el texto no es un mensaje valido de esta version. */
    fun decodificar(texto: String): Mensaje? {
        val partes = texto.split(SEP)
        if (partes.size < 3) return null
        if (partes[0].toIntOrNull() != VERSION) return null
        val tipo = Tipo.de(partes[1]) ?: return null
        return Mensaje(tipo, partes[2], partes.drop(3))
    }

    // ------------------------------------------------------------ armadores
    //
    // Estan aca y no sueltos por el codigo para que haya un solo lugar donde
    // mirar cuando el orden de un campo no cierra.

    fun unirse(id: String, nombre: String, skin: String) =
        Mensaje(Tipo.UNIRSE, id, listOf(nombre, skin))

    fun arranque(id: String, modo: Modo, nivel: Int, semilla: Long) =
        Mensaje(Tipo.ARRANQUE, id, listOf(modo.name, nivel.toString(), semilla.toString()))

    fun pose(id: String, x: Float, y: Float, z: Float, yaw: Float, postura: Int) =
        Mensaje(
            Tipo.POSE, id,
            listOf(fmt(x), fmt(y), fmt(z), fmt(yaw), postura.toString())
        )

    fun tomar(id: String, indiceCasilla: Int) =
        Mensaje(Tipo.TOMAR, id, listOf(indiceCasilla.toString()))

    fun romper(id: String, indiceCasilla: Int) =
        Mensaje(Tipo.ROMPER, id, listOf(indiceCasilla.toString()))

    fun trampa(id: String, indiceCasilla: Int) =
        Mensaje(Tipo.TRAMPA, id, listOf(indiceCasilla.toString()))

    fun llegada(id: String, milisegundos: Long) =
        Mensaje(Tipo.LLEGADA, id, listOf(milisegundos.toString()))

    /**
     * Donde estan los bichos, varios por mensaje.
     *
     * Cada bicho ocupa un campo suelto en vez de un mensaje propio: veinte
     * mensajes por vuelta serian veinte escrituras en el relay, y el plan
     * gratis se mide justamente en eso.
     */
    fun bichos(id: String, cuadros: List<String>) = Mensaje(Tipo.BICHOS, id, cuadros)

    /** Un bicho de la sala, tal como viaja adentro de un [bichos]. */
    data class Bicho(
        val indice: Int, val x: Float, val z: Float,
        val alerta: Boolean, val vivo: Boolean
    )

    /**
     * Escribe un bicho en un solo campo.
     *
     * Va separado por comas, no por la barra vertical, porque la barra ya
     * separa los campos del mensaje. Sale de 19 caracteres en el peor caso
     * (indice de tres cifras y coordenadas de tres), y [limpiar] recorta a 24:
     * entra con lugar de sobra, y BichosTest lo deja clavado.
     */
    fun cuadroDeBicho(indice: Int, x: Float, z: Float, alerta: Boolean, vivo: Boolean): String {
        val banderas = (if (alerta) 1 else 0) or (if (vivo) 0 else 2)
        return "$indice,${fmt(x)},${fmt(z)},$banderas"
    }

    /** Lee un campo escrito por [cuadroDeBicho], o null si viene roto. */
    fun leerCuadroDeBicho(campo: String): Bicho? {
        val p = campo.split(',')
        if (p.size != 4) return null
        val i = p[0].toIntOrNull() ?: return null
        if (i < 0) return null
        val x = p[1].toFloatOrNull() ?: return null
        val z = p[2].toFloatOrNull() ?: return null
        val b = p[3].toIntOrNull() ?: return null
        return Bicho(i, x, z, alerta = (b and 1) != 0, vivo = (b and 2) == 0)
    }

    fun caido(id: String) = Mensaje(Tipo.CAIDO, id, emptyList())
    fun revivir(id: String, aQuien: String) = Mensaje(Tipo.REVIVIR, id, listOf(aQuien))
    fun salir(id: String) = Mensaje(Tipo.SALIR, id, emptyList())
    fun ping(id: String) = Mensaje(Tipo.PING, id, emptyList())

    /**
     * Dos decimales alcanzan de sobra y ahorran la mitad de los bytes.
     * El signo y el relleno del centesimo van a mano: con division entera
     * "-0,5" se escribia "0.50" (perdia el signo) y "1,05" se escribia "1.5".
     */
    private fun fmt(v: Float): String {
        val r = Math.round(v * 100f)
        val negativo = r < 0
        val a = if (negativo) -r else r
        val dec = (a % 100).toString().padStart(2, '0')
        return (if (negativo) "-" else "") + "${a / 100}.$dec"
    }
}
