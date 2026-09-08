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

    private val jugadores = LinkedHashMap<String, JugadorRemoto>()

    /** Casillas cuyo objeto ya agarro alguien. */
    val objetosTomados = HashSet<Int>()
    /** Paredes que alguien rompio con el pico. */
    val paredesRotas = HashSet<Int>()
    /** Trampas que alguien ya salto. */
    val trampasSaltadas = HashSet<Int>()

    fun otros(): List<JugadorRemoto> = jugadores.values.filter { it.id != yo }
    fun todos(): List<JugadorRemoto> = jugadores.values.toList()
    fun jugador(id: String): JugadorRemoto? = jugadores[id]
    fun cantidad(): Int = jugadores.size

    /** Quien va ganando la carrera: el primero que salio. */
    fun ganador(): JugadorRemoto? =
        jugadores.values.filter { it.tiempoFinal > 0L }.minByOrNull { it.tiempoFinal }

    /** En cooperativo se pierde recien cuando cayeron todos. */
    fun equipoCaido(): Boolean =
        jugadores.isNotEmpty() && jugadores.values.all { it.caido }

    fun aplicar(texto: String): NetProtocol.Mensaje? {
        val m = NetProtocol.decodificar(texto) ?: return null
        val p = jugadores.getOrPut(m.de) { JugadorRemoto(m.de, m.de, "skin_minero") }
        p.silencio = 0f

        when (m.tipo) {
            NetProtocol.Tipo.UNIRSE -> {
                p.nombre = m.arg(0).ifBlank { m.de }
                p.skin = m.arg(1).ifBlank { "skin_minero" }
            }
            NetProtocol.Tipo.ARRANQUE -> {
                modo = runCatching { NetProtocol.Modo.valueOf(m.arg(0)) }
                    .getOrDefault(NetProtocol.Modo.CARRERA)
                nivel = m.entero(1).coerceAtLeast(1)
                semilla = m.largo(2)
                arrancada = true
                // Una partida nueva empieza con el mundo limpio: si quedaran
                // las monedas de la anterior, aparecerian ya levantadas.
                objetosTomados.clear()
                paredesRotas.clear()
                trampasSaltadas.clear()
                for (j in jugadores.values) { j.caido = false; j.tiempoFinal = 0L }
            }
            NetProtocol.Tipo.POSE -> {
                p.x = m.num(0); p.y = m.num(1); p.z = m.num(2)
                p.yaw = m.num(3); p.postura = m.entero(4).coerceIn(0, 2)
            }
            NetProtocol.Tipo.TOMAR -> objetosTomados.add(m.entero(0))
            NetProtocol.Tipo.ROMPER -> paredesRotas.add(m.entero(0))
            NetProtocol.Tipo.TRAMPA -> trampasSaltadas.add(m.entero(0))
            NetProtocol.Tipo.LLEGADA -> if (p.tiempoFinal == 0L) p.tiempoFinal = m.largo(0)
            NetProtocol.Tipo.CAIDO -> p.caido = true
            NetProtocol.Tipo.REVIVIR -> jugadores[m.arg(0)]?.caido = false
            NetProtocol.Tipo.SALIR -> jugadores.remove(m.de)
            NetProtocol.Tipo.PING -> Unit
        }
        return m
    }

    /** Suma el tiempo y echa a los que dejaron de dar senales de vida. */
    fun envejecer(dt: Float): List<String> {
        val idos = ArrayList<String>()
        for (j in jugadores.values.toList()) {
            if (j.id == yo) continue
            j.silencio += dt
            if (j.silencio > TIEMPO_MUERTO) { jugadores.remove(j.id); idos.add(j.id) }
        }
        return idos
    }

    /** Me anoto a mi mismo en la sala. */
    fun entrarYo(nombre: String, skin: String) {
        val p = jugadores.getOrPut(yo) { JugadorRemoto(yo, nombre, skin) }
        p.nombre = NetProtocol.limpiar(nombre).ifBlank { yo }
        p.skin = skin
    }
}
