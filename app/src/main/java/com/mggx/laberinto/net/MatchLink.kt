package com.mggx.laberinto.net

import kotlin.math.abs

/**
 * El puente entre una partida y la red.
 *
 * [MatchState] sabe QUE pasa en la sala y [Transporte] sabe COMO viajan los
 * mensajes; esto es lo que los ata y le pone el reloj: manda tu posicion
 * varias veces por segundo, avisa los hechos del mundo cuando ocurren, y
 * aplica todo lo que llega de los demas.
 *
 * No sabe nada de OpenGL ni de Compose, asi que se puede probar entero en
 * JVM con [TransporteLocal], sin red y sin telefono.
 */
class MatchLink(
    private val transporte: Transporte,
    val yo: String,
    val nombre: String,
    val skin: String,
    /** El que creo la sala reparte la partida (nivel, semilla y modo). */
    val anfitrion: Boolean
) {

    companion object {
        /** Cada cuanto se manda la posicion, en segundos. */
        const val CADA_POSE = 0.1f
        /**
         * Cada cuanto se manda un latido cuando no pasa nada.
         *
         * Tiene que ser bastante menos que [MatchState.TIEMPO_MUERTO], si no
         * te echan de la sala por callado justo cuando estas quieto leyendo
         * el mapa.
         */
        const val CADA_PING = 3f
        /**
         * Cada cuanto el anfitrion reparte donde estan los bichos.
         *
         * Mas espaciado que las poses a proposito: son muchos mas cuerpos y no
         * son la parte que el ojo mira de cerca. Con el suavizado del dibujo
         * alcanza para que se vean caminando y no dando saltos.
         */
        const val CADA_BICHOS = 0.15f
        /**
         * Cuantos bichos entran en un mensaje. Con veinte bichos son dos
         * mensajes por vuelta en vez de veinte: el plan gratis del relay se
         * mide en escrituras, no en bytes.
         */
        const val BICHOS_POR_MENSAJE = 12
        /**
         * Cuanto se mueve por segundo la posicion dibujada hacia la que
         * llego. Suaviza el salto entre mensaje y mensaje.
         */
        const val SUAVIZADO = 12f
    }

    val match = MatchState(yo)

    val errorConexion: String? get() = transporte.errorActual

    /** Se apago la conexion: no se manda ni se recibe nada mas. */
    var cerrado: Boolean = false
        private set

    private var desdePose = 0f
    private var desdePing = 0f
    private var desdeBichos = 0f

    /** Ultima pose mandada, para no repetir mensajes cuando estas quieto. */
    private var ultX = Float.NaN
    private var ultY = Float.NaN
    private var desdePoseCompleta = 0f
    private var ultZ = Float.NaN
    private var ultYaw = Float.NaN
    private var ultPostura = -1

    init {
        match.entrarYo(nombre, skin)
        enviar(NetProtocol.unirse(yo, nombre, skin))
    }

    // ------------------------------------------------------------- mandar

    private fun enviar(m: NetProtocol.Mensaje) {
        if (cerrado) return
        transporte.enviar(m.codificar())
    }

    /** Solo el anfitrion reparte la partida: el mismo nivel y semilla para todos. */
    fun arrancar(modo: NetProtocol.Modo, nivel: Int, semilla: Long) {
        if (!anfitrion) return
        val m = NetProtocol.arranque(yo, modo, nivel, semilla)
        enviar(m)
        // El anfitrion tambien se lo aplica a si mismo: si no, seria el unico
        // de la sala que no sabe con que semilla se juega.
        match.aplicar(m.codificar())
    }

    fun avisarTomado(indiceCasilla: Int) {
        match.marcarTomado(indiceCasilla)
        enviar(NetProtocol.tomar(yo, indiceCasilla))
    }

    fun avisarRoto(indiceCasilla: Int) {
        match.marcarRota(indiceCasilla)
        enviar(NetProtocol.romper(yo, indiceCasilla))
    }

    fun avisarTrampa(indiceCasilla: Int) {
        match.marcarTrampa(indiceCasilla)
        enviar(NetProtocol.trampa(yo, indiceCasilla))
    }

    fun avisarLlegada(milisegundos: Long) {
        val m = NetProtocol.llegada(yo, milisegundos)
        enviar(m)
        match.aplicar(m.codificar())
    }

    /**
     * True cuando toca repartir los bichos, y solo para el anfitrion.
     *
     * Se pregunta en vez de mandarse solo porque armar los cuadros cuesta
     * (hay que recorrer todos los bichos y redondear sus posiciones), y a
     * sesenta cuadros por segundo eso seria tirar trabajo a la basura nueve
     * de cada diez veces.
     */
    fun tocaRepartirBichos(): Boolean {
        if (!anfitrion || cerrado || desdeBichos < CADA_BICHOS) return false
        desdeBichos = 0f
        return true
    }

    /** Reparte donde estan los bichos. Lo ignora el que no es anfitrion. */
    fun repartirBichos(cuadros: List<String>) {
        if (!anfitrion || cerrado || cuadros.isEmpty()) return
        for (lote in cuadros.chunked(BICHOS_POR_MENSAJE)) {
            enviar(NetProtocol.bichos(yo, lote))
        }
    }

    fun avisarCaido() {
        val m = NetProtocol.caido(yo)
        enviar(m)
        match.aplicar(m.codificar())
    }

    /**
     * Pide levantar a un caido. Ojo: es un PEDIDO, no un hecho. El que decide
     * si se levanta es el propio caido (es el unico que sabe cuanto lleva
     * tirado), asi que aca no se toca el estado de nadie: se espera a que el
     * que se levanta lo confirme con [avisarDePie].
     */
    fun avisarRevivir(aQuien: String) {
        enviar(NetProtocol.revivir(yo, aQuien))
    }

    /** "Ya estoy de pie": lo manda el que se levanta, y todos lo dan por vivo. */
    fun avisarDePie() {
        val m = NetProtocol.revivir(yo, yo)
        enviar(m)
        match.aplicar(m.codificar())
    }

    // ------------------------------------------------------------ el reloj

    /**
     * Un paso de red. Se llama una vez por frame.
     *
     * Hace las tres cosas en orden: aplica lo que llego, manda tu posicion si
     * toca, y echa a los que dejaron de dar senales de vida. Devuelve los
     * mensajes que llegaron, para que la partida reaccione a los que le
     * cambian el mundo (una moneda que levanto otro, una pared que rompio).
     */
    fun bombear(
        dt: Float,
        x: Float, y: Float, z: Float, yaw: Float, postura: Int
    ): List<NetProtocol.Mensaje> = paso(dt) {
        // Si no cambio nada, no hace falta repetir la pose: el que la recibio
        // ya te tiene ahi. Asi una sala donde todos miran el mapa no gasta
        // mensajes al pedo.
        //
        // La postura entra en la cuenta aunque no muevas un dedo: agacharse
        // sin caminar tambien es un cambio, y si no viajara el companiero te
        // seguiria dibujando parado, atravesando el techo bajo.
        //
        // La altura tambien, y por el mismo motivo: saltar en el lugar no
        // cambia ni x, ni z, ni el angulo, ni la postura. Sin mirarla, un
        // salto quieto contaba como estar quieto y no se mandaba nada: la y
        // viajaba en el mensaje, pero el mensaje no salia, y el companiero te
        // veia pegado al piso todo el salto.
        val quieto = !ultX.isNaN() && postura == ultPostura &&
            abs(x - ultX) < 0.02f && abs(y - ultY) < 0.02f &&
            abs(z - ultZ) < 0.02f && abs(yaw - ultYaw) < 0.7f
        if (!quieto || desdePoseCompleta >= CADA_PING) {
            desdePoseCompleta = 0f
            ultY = y
            ultX = x; ultZ = z; ultYaw = yaw; ultPostura = postura
            enviar(NetProtocol.pose(yo, x, y, z, yaw, postura))
        }
    }

    /**
     * Un paso de red SIN mandar posicion, para cuando todavia no hay ninguna
     * que mandar: la pantalla de sala (nadie bajo a la cueva) y la pausa.
     *
     * En la sala es importante que no salga ninguna pose: seria una posicion
     * inventada (el cero del mapa), y el companiero te dibujaria un instante
     * ahi antes de que llegue la primera de verdad.
     *
     * Y en la pausa es importante que SI se sigan mandando latidos: si no, a
     * los [MatchState.TIEMPO_MUERTO] segundos los demas te dan por ido y te
     * sacan de la sala por estar mirando el mapa.
     */
    fun latir(dt: Float): List<NetProtocol.Mensaje> = paso(dt, null)

    private fun paso(dt: Float, mandarPose: (() -> Unit)?): List<NetProtocol.Mensaje> {
        if (cerrado) return emptyList()

        // --- lo que llego
        //
        // Los mensajes propios se descartan: hay relays que devuelven al que
        // publica su propio mensaje (Firebase es uno), y aplicarse el propio
        // SALIR se traduce en borrarse a uno mismo de la lista de la sala.
        // Todo lo que uno hace ya se aplico en el momento de hacerlo.
        val llegados = ArrayList<NetProtocol.Mensaje>()
        for (texto in transporte.recibir()) {
            // Se mira ANTES de aplicar: aplicar el propio SALIR y despues
            // descartarlo no sirve de nada, el borrado ya paso.
            val m = NetProtocol.decodificar(texto)
            if (m?.de == yo) continue
            // El anfitrion es el que reparte los bichos: si ademas se copiara
            // los que le mandan, dos cabezas estarian moviendo los mismos
            // cuerpos y se pelearian tironeandolos.
            if (anfitrion && m?.tipo == NetProtocol.Tipo.BICHOS) continue
            match.aplicar(texto)?.let { llegados.add(it) }
        }

        // --- lo mio
        desdePose += dt
        desdePing += dt
        desdePoseCompleta += dt
        desdeBichos += dt
        if (mandarPose != null && desdePose >= CADA_POSE) {
            desdePose = 0f
            mandarPose()
        }
        // El latido es un JOIN y no un PING: ademas de decir "sigo aca",
        // repite quien sos. Hace falta porque el que entra a una sala ya
        // empezada no recibe los mensajes anteriores (ver TransporteFirebase),
        // asi que se perderia el JOIN del que llego primero y lo veria con el
        // id crudo en vez del nombre. Asi, a los pocos segundos, todos saben
        // el nombre y la skin de todos.
        if (desdePing >= CADA_PING) {
            desdePing = 0f
            enviar(NetProtocol.unirse(yo, nombre, skin))
            // Y el anfitrion repite ademas el arranque. El ARRANQUE se manda
            // una sola vez, asi que si justo se pierde (o si alguien entro un
            // segundo tarde), el otro se quedaria esperando en la sala para
            // siempre mientras el anfitrion juega solo. Repetirlo no molesta:
            // el que ya arranco con esa misma semilla lo ignora.
            if (anfitrion && match.arrancada) {
                enviar(NetProtocol.arranque(yo, match.modo, match.nivel, match.semilla))
            }
        }

        // --- los que se fueron sin avisar
        match.envejecer(dt)

        // --- la posicion que se dibuja persigue a la que llego
        val k = (SUAVIZADO * dt).coerceIn(0f, 1f)
        for (j in match.otros()) j.suavizar(k)
        if (!anfitrion) match.suavizarBichos(k)

        return llegados
    }

    fun cerrar() {
        if (cerrado) return
        // Avisar antes de cortar: el que se va de la lista al toque es mucho
        // mejor que el fantasma que se queda 12 segundos hasta que lo echan.
        transporte.enviar(NetProtocol.salir(yo).codificar())
        cerrado = true
        transporte.cerrar()
    }
}

