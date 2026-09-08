package com.mggx.laberinto.net

/**
 * Por donde salen y entran los mensajes. Todavia no hay ninguna implementacion
 * de red de verdad: la que existe es la de memoria, que sirve para probar toda
 * la logica de sala sin necesitar internet ni un servidor.
 *
 * El dia que se enchufe un relay (ver docs/MULTIJUGADOR.md) alcanza con
 * escribir otra clase que cumpla esta interfaz. Nada mas del juego cambia.
 */
interface Transporte {
    /** Manda un mensaje ya codificado a los demas. */
    fun enviar(texto: String)
    /** Toma los mensajes que llegaron desde la ultima vez. */
    fun recibir(): List<String>
    fun cerrar()
}

/**
 * Transporte de mentira, todo en memoria: lo que uno envia lo reciben los
 * otros del mismo grupo. Es lo que permite probar la sala completa en un test
 * de JVM, sin red.
 */
class TransporteLocal(private val grupo: MutableList<TransporteLocal> = ArrayList()) : Transporte {

    private val bandeja = ArrayDeque<String>()
    private var abierto = true

    init { grupo.add(this) }

    /** Otro participante conectado al mismo grupo. */
    fun companero(): TransporteLocal = TransporteLocal(grupo)

    override fun enviar(texto: String) {
        if (!abierto) return
        for (t in grupo) if (t !== this && t.abierto) t.bandeja.addLast(texto)
    }

    override fun recibir(): List<String> {
        if (bandeja.isEmpty()) return emptyList()
        val out = bandeja.toList()
        bandeja.clear()
        return out
    }

    override fun cerrar() {
        abierto = false
        grupo.remove(this)
    }
}

/** Un jugador de la sala, tal como lo ven los demas. */
class JugadorRemoto(
    val id: String,
    var nombre: String,
    var skin: String
) {
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f
    var yaw: Float = 0f
    /** 0 de pie, 1 agachado, 2 arrastrandose. */
    var postura: Int = 0
    var caido: Boolean = false
    /** Milisegundos que tardo en salir, o 0 si todavia no salio. */
    var tiempoFinal: Long = 0L
    /** Segundos desde el ultimo mensaje suyo: sirve para echar a los colgados. */
    var silencio: Float = 0f

    /**
     * Donde se lo DIBUJA, que no es lo mismo que donde esta.
     *
     * Las poses llegan 10 veces por segundo y la pantalla dibuja 60: si se lo
     * pusiera en la posicion que llego, se veria teletransportarse. Estas van
     * corriendo atras de las de arriba, y el ojo lee eso como caminar.
     */
    var dibX: Float = 0f
    var dibY: Float = 0f
    var dibZ: Float = 0f
    var dibYaw: Float = 0f

    /** True hasta que llega su primera pose: antes de eso no hay que dibujarlo. */
    var sinPose: Boolean = true
        private set

    /** Guarda una pose recien llegada. */
    fun pose(nx: Float, ny: Float, nz: Float, nyaw: Float, npostura: Int) {
        x = nx; y = ny; z = nz; yaw = nyaw; postura = npostura
        if (sinPose) {
            // La primera no se suaviza: aparece donde esta, no viene volando
            // desde el cero del mapa.
            dibX = nx; dibY = ny; dibZ = nz; dibYaw = nyaw
            sinPose = false
        }
    }

    /** Acerca la posicion dibujada a la real. [k] es cuanto del camino recorre. */
    fun suavizar(k: Float) {
        if (sinPose) return
        dibX += (x - dibX) * k
        dibY += (y - dibY) * k
        dibZ += (z - dibZ) * k
        // El angulo se interpola por el lado corto: si no, al cruzar de 359 a
        // 1 grado el companiero pega un giro completo para el otro lado.
        var d = yaw - dibYaw
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        dibYaw += d * k
    }
}

/**
 * Un bicho, tal como lo reparte el anfitrion.
 *
 * Guarda aparte donde se lo dibuja, por el mismo motivo que [JugadorRemoto]:
 * las tandas llegan unas siete veces por segundo y la pantalla dibuja sesenta.
 */
class BichoRemoto(var x: Float, var z: Float) {
    var alerta: Boolean = false
    var vivo: Boolean = true
    var dibX: Float = x
    var dibZ: Float = z

    fun poner(nx: Float, nz: Float, nalerta: Boolean, nvivo: Boolean) {
        // Un salto grande no se suaviza: es un bicho que aparecio de nuevo o
        // que el anfitrion reubico, y arrastrarlo por media cueva se veria
        // peor que ponerlo donde esta.
        if (kotlin.math.hypot(nx - x, nz - z) > 4f) { dibX = nx; dibZ = nz }
        x = nx; z = nz; alerta = nalerta; vivo = nvivo
    }

    fun suavizar(k: Float) {
        dibX += (x - dibX) * k
        dibZ += (z - dibZ) * k
    }
}

/**
 * El estado compartido de una partida en red.
 *
 * No dibuja ni juega: recibe mensajes y deja el estado prolijo para que la
 * partida los pinte. Esta escrito aparte justamente para poder probarlo en
 * JVM, que es donde se van a encontrar los errores de verdad de la red.
 */
class MatchState(val yo: String) {

    /** Cuantos segundos sin noticias antes de dar por ido a alguien. */
    val TIEMPO_MUERTO = 12f

    var modo: NetProtocol.Modo = NetProtocol.Modo.CARRERA
        private set
    var nivel: Int = 1
        private set
    var semilla: Long = 0L
        private set
    var arrancada: Boolean = false
        private set

    /**
     * La gente de la sala.
     *
     * Se toca desde DOS hilos: el de OpenGL (que corre la partida y aplica lo
     * que llega) y el de la interfaz (que pinta la lista de la sala y el
     * cartel de caido). Sin el candado, que a alguien se le corte el internet
     * justo mientras se dibuja la lista revienta la app con una
     * ConcurrentModificationException.
     */
    private val jugadores = LinkedHashMap<String, JugadorRemoto>()
    private val candado = Any()

    /**
     * Donde dijo el anfitrion que estan los bichos, por indice en la lista.
     *
     * El indice sirve de nombre porque los bichos nacen de la misma semilla en
     * los dos telefonos y nunca se sacan de la lista al morir (se quedan con
     * vida en cero), asi que el numero de cada uno es el mismo de los dos
     * lados de la sala.
     */
    private val bichos = HashMap<Int, BichoRemoto>()

    /** Copia de los bichos repartidos. Vacio si nadie los reparte todavia. */
    fun bichos(): Map<Int, BichoRemoto> = synchronized(candado) { HashMap(bichos) }
    fun bicho(indice: Int): BichoRemoto? = synchronized(candado) { bichos[indice] }
    fun hayBichos(): Boolean = synchronized(candado) { bichos.isNotEmpty() }

    /** Acerca los bichos dibujados a donde dijo el anfitrion que estan. */
    fun suavizarBichos(k: Float) {
        synchronized(candado) { for (b in bichos.values) b.suavizar(k) }
    }

    /** Casillas cuyo objeto ya agarro alguien. */
    val objetosTomados = HashSet<Int>()
    /** Paredes que alguien rompio con el pico. */
    val paredesRotas = HashSet<Int>()
    /** Trampas que alguien ya salto. */
    val trampasSaltadas = HashSet<Int>()

    // Todo lo que recorre la lista devuelve una COPIA: asi el que la recibe
    // la puede recorrer tranquilo aunque mientras tanto entre o salga alguien.
    fun otros(): List<JugadorRemoto> =
        synchronized(candado) { jugadores.values.filter { it.id != yo } }
    fun todos(): List<JugadorRemoto> = synchronized(candado) { jugadores.values.toList() }
    fun jugador(id: String): JugadorRemoto? = synchronized(candado) { jugadores[id] }
    fun cantidad(): Int = synchronized(candado) { jugadores.size }

    /** Quien va ganando la carrera: el primero que salio. */
    fun ganador(): JugadorRemoto? = synchronized(candado) {
        jugadores.values.filter { it.tiempoFinal > 0L }.minByOrNull { it.tiempoFinal }
    }

    /**
     * En cooperativo se pierde recien cuando cayeron todos los que estan
     * jugando.
     *
     * "Los que estan jugando" son los que ya mandaron una pose, o sea los que
     * de verdad bajaron a la cueva. El que se queda en la pantalla de sala
     * (entro tarde, cuando la partida ya habia arrancado) no manda poses, y
     * si contara aca nunca estaria caido: los que si estan abajo quedarian
     * congelados para siempre, sin poder moverse ni terminar la partida.
     */
    fun equipoCaido(): Boolean = synchronized(candado) {
        // Uno mismo cuenta siempre: las poses propias viajan a los demas, no
        // a la copia local, asi que la entrada de uno nunca tiene pose. Sin
        // esta excepcion, el que juega solo en cooperativo se quedaria caido
        // para siempre esperando a un companiero que no existe.
        val enLaCueva = jugadores.values.filter { !it.sinPose || it.id == yo }
        enLaCueva.isNotEmpty() && enLaCueva.all { it.caido }
    }

    fun aplicar(texto: String): NetProtocol.Mensaje? {
        val m = NetProtocol.decodificar(texto) ?: return null
        val p = synchronized(candado) {
            jugadores.getOrPut(m.de) { JugadorRemoto(m.de, m.de, "skin_minero") }
        }
        p.silencio = 0f

        when (m.tipo) {
            NetProtocol.Tipo.UNIRSE -> {
                p.nombre = m.arg(0).ifBlank { m.de }
                p.skin = m.arg(1).ifBlank { "skin_minero" }
            }
            NetProtocol.Tipo.ARRANQUE -> {
                val nuevoNivel = m.entero(1).coerceAtLeast(1)
                val nuevaSemilla = m.largo(2)
                // Un ARRANQUE repetido con el mismo nivel y la misma semilla
                // es una RETRANSMISION, no una partida nueva. El anfitrion lo
                // repite mientras juega para enganchar al que se lo perdio, y
                // volver a aplicarlo aca borraria lo que ya paso en la cueva
                // (quien esta caido, quien salio y en cuanto tiempo).
                if (arrancada && nuevoNivel == nivel && nuevaSemilla == semilla) return m
                modo = runCatching { NetProtocol.Modo.valueOf(m.arg(0)) }
                    .getOrDefault(NetProtocol.Modo.CARRERA)
                nivel = nuevoNivel
                semilla = nuevaSemilla
                arrancada = true
                // Una partida nueva empieza con el mundo limpio: si quedaran
                // las monedas de la anterior, aparecerian ya levantadas.
                objetosTomados.clear()
                paredesRotas.clear()
                trampasSaltadas.clear()
                // Los bichos de la cueva anterior no tienen nada que ver con
                // los de esta: dejarlos poblaria el nivel nuevo de fantasmas
                // parados donde estaban los del anterior.
                synchronized(candado) { bichos.clear() }
                for (j in todos()) { j.caido = false; j.tiempoFinal = 0L }
            }
            NetProtocol.Tipo.POSE ->
                p.pose(m.num(0), m.num(1), m.num(2), m.num(3), m.entero(4).coerceIn(0, 2))
            NetProtocol.Tipo.BICHOS -> synchronized(candado) {
                for (campo in m.args) {
                    val b = NetProtocol.leerCuadroDeBicho(campo) ?: continue
                    bichos.getOrPut(b.indice) { BichoRemoto(b.x, b.z) }
                        .poner(b.x, b.z, b.alerta, b.vivo)
                }
            }
            NetProtocol.Tipo.TOMAR -> objetosTomados.add(m.entero(0))
            NetProtocol.Tipo.ROMPER -> paredesRotas.add(m.entero(0))
            NetProtocol.Tipo.TRAMPA -> trampasSaltadas.add(m.entero(0))
            NetProtocol.Tipo.LLEGADA -> if (p.tiempoFinal == 0L) p.tiempoFinal = m.largo(0)
            NetProtocol.Tipo.CAIDO -> p.caido = true
            NetProtocol.Tipo.REVIVIR -> {
                // Ojo con quien lo manda: un REVIVIR de OTRO es un pedido
                // ("levantate"), y el caido lo puede ignorar si todavia lleva
                // poco tiempo en el piso. Solo cuenta como hecho el que manda
                // el propio caido ("ya estoy de pie").
                //
                // Sin esta distincion los dos telefonos terminan contando
                // cosas distintas: uno lo da por levantado y el otro lo sigue
                // viendo tirado, y entonces equipoCaido() no se cumple nunca
                // y la partida no puede terminar.
                if (m.arg(0) == m.de) p.caido = false
            }
            NetProtocol.Tipo.SALIR -> synchronized(candado) { jugadores.remove(m.de) }
            NetProtocol.Tipo.PING -> Unit
        }
        return m
    }

    /** Suma el tiempo y echa a los que dejaron de dar senales de vida. */
    fun envejecer(dt: Float): List<String> {
        val idos = ArrayList<String>()
        for (j in todos()) {
            if (j.id == yo) continue
            j.silencio += dt
            if (j.silencio > TIEMPO_MUERTO) {
                synchronized(candado) { jugadores.remove(j.id) }
                idos.add(j.id)
            }
        }
        return idos
    }

    /** Me anoto a mi mismo en la sala. */
    fun entrarYo(nombre: String, skin: String) {
        val p = synchronized(candado) {
            jugadores.getOrPut(yo) { JugadorRemoto(yo, nombre, skin) }
        }
        p.nombre = NetProtocol.limpiar(nombre).ifBlank { yo }
        p.skin = skin
    }
}
