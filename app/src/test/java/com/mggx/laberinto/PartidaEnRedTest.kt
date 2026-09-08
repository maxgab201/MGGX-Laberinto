package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.net.MatchLink
import com.mggx.laberinto.net.NetProtocol
import com.mggx.laberinto.net.TransporteLocal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dos partidas de verdad, conectadas entre si.
 *
 * Los tests de MultijugadorTest prueban los mensajes y la sala por separado;
 * estos arman dos GameSession completas atadas por un transporte y las hacen
 * jugar, que es donde aparecen los problemas reales: que una moneda
 * desaparezca en los dos telefonos, que el caido no se muera solo, que el
 * segundo no gane si ya gano el primero.
 */
class PartidaEnRedTest {

    private fun perfil(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    /** Dos partidas del mismo nivel y semilla, ya conectadas. */
    private class Mesa(modo: NetProtocol.Modo, nivel: Int = 4, semilla: Long = 20260906L) {
        val t1 = TransporteLocal()
        val t2 = t1.companero()
        val uno: GameSession
        val dos: GameSession
        val redUno: MatchLink
        val redDos: MatchLink

        init {
            val p1 = SaveData.fromStore(SaveData.memoryStore())
            val p2 = SaveData.fromStore(SaveData.memoryStore())
            uno = GameSession(p1, nivel, semilla)
            dos = GameSession(p2, nivel, semilla)
            redUno = MatchLink(t1, "uno", "Maxi", "skin_minero", anfitrion = true)
            redDos = MatchLink(t2, "dos", "Colo", "skin_minero", anfitrion = false)
            uno.red = redUno
            dos.red = redDos
            redUno.arrancar(modo, nivel, semilla)
            // Los dos tienen que quedar en el mismo modo: el invitado se
            // entera por el mensaje del anfitrion.
            bombear()
        }

        /**
         * Un rato de juego con los dos quietos, para que los mensajes crucen.
         *
         * Ojo con el tiempo: GameSession.update recorta el dt a 0.05s (es su
         * techo anti-saltos tras un lag), asi que un paso nunca vale mas que
         * eso por mucho que se le pida. Por eso [segundos] cuenta pasos de
         * 0.05 y no del dt que uno quiera.
         */
        fun bombear(veces: Int = 4, dt: Float = 0.05f) {
            val quieto = GameSession.Input()
            repeat(veces) {
                uno.update(dt, quieto)
                dos.update(dt, quieto)
            }
        }

        /** Deja correr [s] segundos de partida, en pasos del tamano maximo. */
        fun segundos(s: Float) = bombear(veces = (s / 0.05f).toInt() + 1, dt = 0.05f)

        /**
         * Los separa para que no se levanten solos.
         *
         * Los dos arrancan en la misma casilla (misma cueva, mismo punto de
         * partida), asi que sin esto el companiero esta siempre en el radio
         * de levantada y no se puede probar el caido.
         */
        fun separar() {
            dos.posX = uno.posX + 40f
            dos.posZ = uno.posZ + 40f
        }
    }

    // --------------------------------------------------------- el mundo

    @Test
    fun laMonedaQueLevantaUnoDesapareceParaElOtro() {
        val mesa = Mesa(NetProtocol.Modo.CARRERA)
        val moneda = mesa.uno.pickups.first { !it.taken }
        val mismaEnElOtro = mesa.dos.pickups.first { it.gx == moneda.gx && it.gy == moneda.gy }

        // El uno se para encima y la levanta.
        mesa.uno.posX = (moneda.gx + 0.5f) * GameSession.CELL
        mesa.uno.posZ = (moneda.gy + 0.5f) * GameSession.CELL
        mesa.bombear()

        assertTrue("no la levanto ni el que la piso", moneda.taken)
        assertTrue("la moneda sigue estando para el otro", mismaEnElOtro.taken)
        // En carrera cada uno junta lo suyo: el que no la levanto no cobra.
        assertTrue(mesa.uno.ecosCollected > 0)
        assertEquals("el otro cobro una moneda que no levanto", 0, mesa.dos.ecosCollected)
    }

    @Test
    fun enCooperativoLoQueLevantaUnoLoCobranLosDos() {
        // Es lo que promete el modo ("comparten los ecos"), asi que tiene que
        // ser verdad y no solo un texto lindo en la pantalla de la sala.
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        mesa.separar()
        val moneda = mesa.uno.pickups.first { !it.taken }
        mesa.uno.posX = (moneda.gx + 0.5f) * GameSession.CELL
        mesa.uno.posZ = (moneda.gy + 0.5f) * GameSession.CELL
        mesa.bombear()

        assertTrue("no la levanto ni el que la piso", moneda.taken)
        assertTrue("el que la levanto no cobro", mesa.uno.ecosCollected > 0)
        assertTrue(
            "el companiero no cobro su parte en cooperativo",
            mesa.dos.ecosCollected > 0
        )
    }

    @Test
    fun laParedQueRompeUnoQuedaRotaParaElOtro() {
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        // Buscar una pared interior que se pueda romper de verdad.
        val m = mesa.uno.maze
        var gx = -1; var gy = -1
        outer@ for (y in 1 until m.gh - 1) {
            for (x in 1 until m.gw - 1) {
                if (m.isSolid(x, y)) { gx = x; gy = y; break@outer }
            }
        }
        assertTrue("el nivel no tiene ninguna pared interior", gx > 0)

        // Se lo hace a mano, que es lo que termina llamando breakWall.
        m.setSolid(gx, gy, false)
        mesa.uno.red!!.avisarRoto(gy * m.gw + gx)
        mesa.bombear()

        assertFalse("la pared sigue en pie para el otro", mesa.dos.maze.isSolid(gx, gy))
        assertTrue("el otro no se entero de que hay que rehacer la malla", mesa.dos.geometryDirty)
    }

    // ------------------------------------------------------------ carrera

    @Test
    fun enCarreraElQueLlegaPrimeroGanaYAlOtroSeLeTerminaLaPartida() {
        val mesa = Mesa(NetProtocol.Modo.CARRERA)
        // El uno se planta en la salida.
        mesa.uno.posX = mesa.uno.exitWorldX
        mesa.uno.posZ = mesa.uno.exitWorldZ
        mesa.bombear()

        assertEquals(GameSession.Phase.GANADO, mesa.uno.phase)
        assertEquals(
            "al que no llego no se le termino la partida",
            GameSession.Phase.PERDIDO, mesa.dos.phase
        )
        assertEquals("uno", mesa.dos.red!!.match.ganador()?.id)
    }

    @Test
    fun elQueYaGanoNoSeVuelveAtrasPorqueLlegueYoDespues() {
        val mesa = Mesa(NetProtocol.Modo.CARRERA)
        mesa.uno.posX = mesa.uno.exitWorldX
        mesa.uno.posZ = mesa.uno.exitWorldZ
        mesa.bombear()
        assertEquals(GameSession.Phase.GANADO, mesa.uno.phase)

        // Le llega la llegada del otro, mas rapida, pero el ya termino.
        mesa.uno.red!!.match.aplicar(NetProtocol.llegada("dos", 1L).codificar())
        mesa.bombear()
        assertEquals("perdio una partida que ya habia ganado", GameSession.Phase.GANADO, mesa.uno.phase)
    }

    // -------------------------------------------------------- cooperativo

    @Test
    fun enCooperativoElQueCaeQuedaEsperandoYNoPierdeLaPartida() {
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        mesa.separar()
        mesa.uno.applyDamage(99999f)
        mesa.bombear()

        assertTrue("no quedo caido", mesa.uno.caido)
        assertEquals(
            "se termino la partida por un solo caido",
            GameSession.Phase.JUGANDO, mesa.uno.phase
        )
        assertTrue("el companiero no se entero de la caida", mesa.dos.red!!.match.jugador("uno")!!.caido)
    }

    @Test
    fun alCaidoNoSeLeSigueDescontandoVida() {
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        mesa.separar()
        mesa.uno.applyDamage(99999f)
        mesa.bombear()
        assertTrue(mesa.uno.caido)

        // Un bicho que lo muerde estando en el piso no lo puede rematar.
        mesa.uno.applyDamage(50f)
        assertEquals(
            "el caido paso a PERDIDO por un golpe de mas",
            GameSession.Phase.JUGANDO, mesa.uno.phase
        )
    }

    @Test
    fun alCaidoHayQueEsperarloUnosSegundosAntesDeLevantarlo() {
        // Si se levantara en el mismo frame en que cae, dos que van pegados no
        // perderian nunca y el modo no tendria ninguna gracia.
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        mesa.separar()
        mesa.uno.applyDamage(99999f)
        mesa.bombear()
        assertTrue(mesa.uno.caido)
        assertTrue("no quedo tiempo de espera", mesa.uno.esperaParaLevantarte() > 0f)

        // El companiero llega enseguida, pero todavia no lo puede levantar.
        mesa.dos.posX = mesa.uno.posX
        mesa.dos.posZ = mesa.uno.posZ
        mesa.segundos(0.5f)
        assertTrue("lo levanto antes de tiempo", mesa.uno.caido)

        // Y despues de la espera, si.
        mesa.segundos(4f)
        assertFalse("no lo levanto ni despues de esperar", mesa.uno.caido)
    }

    @Test
    fun elCompanieroQueSeAcercaLoLevanta() {
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        mesa.separar()
        mesa.uno.applyDamage(99999f)
        mesa.bombear()
        assertTrue(mesa.uno.caido)

        // El dos se le pone al lado y le da el tiempo que hace falta.
        mesa.dos.posX = mesa.uno.posX
        mesa.dos.posZ = mesa.uno.posZ
        mesa.segundos(4f)

        assertFalse("no lo levanto teniendolo al lado", mesa.uno.caido)
        assertTrue("lo levanto sin vida", mesa.uno.health > 0f)
        assertFalse(
            "el que lo levanto lo sigue viendo tirado",
            mesa.dos.red!!.match.jugador("uno")!!.caido
        )
    }

    @Test
    fun siCaenLosDosSeTerminaParaTodos() {
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        mesa.separar()
        mesa.uno.applyDamage(99999f)
        mesa.dos.applyDamage(99999f)
        mesa.bombear()

        assertEquals(GameSession.Phase.PERDIDO, mesa.uno.phase)
        assertEquals(GameSession.Phase.PERDIDO, mesa.dos.phase)
    }

    @Test
    fun elCaidoNoSeMueveAunqueEmpujenElJoystick() {
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        mesa.separar()
        mesa.uno.applyDamage(99999f)
        mesa.bombear()
        val x = mesa.uno.posX
        val z = mesa.uno.posZ

        val corriendo = GameSession.Input(moveY = 1f, running = true)
        repeat(20) { mesa.uno.update(0.05f, corriendo) }

        assertEquals("el caido camino", x, mesa.uno.posX, 0.01f)
        assertEquals(z, mesa.uno.posZ, 0.01f)
    }

    @Test
    fun elCaidoNoSaleAunqueLoDejenEnLaPuertaDeSalida() {
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        mesa.separar()
        mesa.uno.applyDamage(99999f)
        mesa.bombear()
        mesa.uno.posX = mesa.uno.exitWorldX
        mesa.uno.posZ = mesa.uno.exitWorldZ
        mesa.bombear()

        assertNotEquals(
            "gano el nivel tirado en el piso",
            GameSession.Phase.GANADO, mesa.uno.phase
        )
    }

    // ------------------------------------------------------------- la red

    @Test
    fun elCompanieroSeVeDondeEstaYNoDondeEstaba() {
        val mesa = Mesa(NetProtocol.Modo.CARRERA)
        mesa.uno.posX += 6f
        mesa.uno.posZ += 3f
        mesa.segundos(2f)

        val visto = mesa.dos.red!!.match.jugador("uno")!!
        assertEquals(mesa.uno.posX, visto.x, 0.02f)
        assertEquals(mesa.uno.posZ, visto.z, 0.02f)
        // Y la posicion que se dibuja ya lo alcanzo, sin quedar atras.
        assertEquals("la posicion dibujada quedo colgada", visto.x, visto.dibX, 0.05f)
        assertEquals(visto.z, visto.dibZ, 0.05f)
    }

    @Test
    fun estandoQuietoNoSeMandanPosesAlPedo() {
        val mesa = Mesa(NetProtocol.Modo.CARRERA)
        mesa.bombear(veces = 40)
        // Contar lo que efectivamente sale al transporte de aca en mas.
        val espia = TransporteLocal()
        val companiero = espia.companero()
        val red = MatchLink(espia, "solo", "Quieto", "skin_minero", anfitrion = true)
        companiero.recibir()   // vaciar el JOIN

        repeat(60) { red.bombear(0.1f, 5f, 0f, 5f, 90f, 0) }
        val mandados = companiero.recibir()
        // En 6 segundos quieto: latidos y una pose de recuperacion cada 3s,
        // nunca las 60 poses de un jugador que camina.
        assertTrue(
            "manda mensajes de mas estando quieto: ${mandados.size}",
            mandados.size <= 5
        )
    }

    @Test
    fun moviendoseSiSeMandanPoses() {
        val espia = TransporteLocal()
        val companiero = espia.companero()
        val red = MatchLink(espia, "solo", "Andante", "skin_minero", anfitrion = true)
        companiero.recibir()

        var x = 5f
        repeat(20) { x += 0.5f; red.bombear(0.1f, x, 0f, 5f, 90f, 0) }
        val poses = companiero.recibir().count {
            NetProtocol.decodificar(it)?.tipo == NetProtocol.Tipo.POSE
        }
        assertTrue("no mando ninguna pose caminando", poses >= 15)
    }

    @Test
    fun enLaSalaNoSeMandanPosesInventadas() {
        // Antes de bajar a la cueva no hay ninguna posicion que mandar. Si se
        // mandara igual (el cero del mapa), el companiero te dibujaria un
        // instante en un rincon del laberinto antes de la primera pose real.
        val espia = TransporteLocal()
        val companiero = espia.companero()
        val red = MatchLink(espia, "solo", "Esperando", "skin_minero", anfitrion = true)
        companiero.recibir()

        repeat(100) { red.latir(0.1f) }
        val poses = companiero.recibir().count {
            NetProtocol.decodificar(it)?.tipo == NetProtocol.Tipo.POSE
        }
        assertEquals("mando poses estando en la sala", 0, poses)
    }

    @Test
    fun enLaSalaSeSigueLatiendoParaQueNoTeEchen() {
        // Lo otro que tiene que pasar en la sala: seguir dando senales de vida
        // mientras se espera a que entren los demas.
        val espia = TransporteLocal()
        val companiero = espia.companero()
        val red = MatchLink(espia, "solo", "Esperando", "skin_minero", anfitrion = true)
        companiero.recibir()

        // Bastante mas que el tiempo muerto de la sala. El latido es un JOIN
        // (dice "sigo aca" y ademas repite quien sos, para el que entro
        // despues y no vio el JOIN original).
        repeat(300) { red.latir(0.1f) }
        val latidos = companiero.recibir().count {
            NetProtocol.decodificar(it)?.tipo == NetProtocol.Tipo.UNIRSE
        }
        assertTrue("no mando ningun latido: lo echarian de la sala", latidos >= 5)
    }

    @Test
    fun enPausaLaSalaSigueViva() {
        // Abrir el mapa un rato no te puede sacar de la sala.
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        mesa.bombear()
        assertEquals(2, mesa.dos.red!!.match.cantidad())

        // El uno se queda en pausa (no llama update, solo late) mas tiempo del
        // que aguanta la sala antes de dar a alguien por ido.
        val vueltas = ((MatchLink.CADA_PING * 2f + 13f) / 0.05f).toInt()
        repeat(vueltas) {
            mesa.uno.latirRed(0.05f)
            mesa.dos.update(0.05f, GameSession.Input())
        }

        assertEquals(
            "al que estaba en pausa lo echaron de la sala",
            2, mesa.dos.red!!.match.cantidad()
        )
    }

    /**
     * Un relay que le devuelve a cada uno su propio mensaje.
     *
     * Firebase hace exactamente esto: al escuchar un nodo, tambien te llegan
     * los mensajes que publicaste vos. TransporteLocal no, asi que sin este
     * doble no habria forma de ver el problema en un test.
     */
    private class TransporteConEco : com.mggx.laberinto.net.Transporte {
        val bandeja = ArrayDeque<String>()
        override fun enviar(texto: String) { bandeja.addLast(texto) }
        override fun recibir(): List<String> {
            val out = bandeja.toList(); bandeja.clear(); return out
        }
        override fun cerrar() = Unit
    }

    @Test
    fun elEcoDelPropioMensajeNoTeSacaDeLaSala() {
        // El caso feo: te llega tu propio SALIR y te borras a vos mismo de la
        // lista de la sala, con lo cual dejas de existir para tu propia UI.
        val eco = TransporteConEco()
        val red = MatchLink(eco, "uno", "Maxi", "skin_minero", anfitrion = true)
        assertEquals(1, red.match.cantidad())

        // El JOIN propio vuelve por el eco.
        red.latir(0.1f)
        assertEquals("el eco del JOIN cambio la sala", 1, red.match.cantidad())

        // Y el SALIR de otro jugador si tiene que sacarlo.
        eco.bandeja.addLast(NetProtocol.unirse("dos", "Colo", "skin_minero").codificar())
        red.latir(0.1f)
        assertEquals(2, red.match.cantidad())
        eco.bandeja.addLast(NetProtocol.salir("dos").codificar())
        red.latir(0.1f)
        assertEquals(1, red.match.cantidad())
    }

    @Test
    fun elEcoDeLaPropiaLlegadaNoTerminaLaPartidaDeUno() {
        // En carrera, la partida se corta cuando gana OTRO. Si el eco de la
        // propia llegada se tomara como ajeno, el que gana veria "gano el
        // otro" y perderia su propia carrera.
        val eco = TransporteConEco()
        val s = GameSession(perfil(), 4, 20260906L)
        val red = MatchLink(eco, "uno", "Maxi", "skin_minero", anfitrion = true)
        s.red = red
        red.arrancar(NetProtocol.Modo.CARRERA, 4, 20260906L)

        s.posX = s.exitWorldX
        s.posZ = s.exitWorldZ
        repeat(20) { s.update(0.05f, GameSession.Input()) }

        assertEquals("se perdio su propia carrera por el eco", GameSession.Phase.GANADO, s.phase)
    }

    @Test
    fun agacharseSinCaminarTambienViaja() {
        // Si la postura no viajara, el companiero te seguiria dibujando
        // parado, atravesando el techo del tramo bajo en el que estas.
        val espia = TransporteLocal()
        val companiero = espia.companero()
        val red = MatchLink(espia, "solo", "Agachado", "skin_minero", anfitrion = true)
        companiero.recibir()

        // Quieto y de pie: manda la primera pose y despues se calla.
        repeat(30) { red.bombear(0.1f, 5f, 0f, 5f, 90f, 0) }
        companiero.recibir()

        // Se agacha sin moverse un centimetro.
        repeat(10) { red.bombear(0.1f, 5f, 0f, 5f, 90f, 1) }
        val posturas = companiero.recibir()
            .mapNotNull { NetProtocol.decodificar(it) }
            .filter { it.tipo == NetProtocol.Tipo.POSE }
            .map { it.entero(4) }
        assertTrue("agacharse quieto no viajo", posturas.contains(1))
    }

    @Test
    fun abandonarEnCooperativoAbandonaDeVerdad() {
        // El boton "Abandonar el nivel" no puede dejarte tirado esperando que
        // un companiero te levante: es una decision tuya, no una caida.
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        mesa.separar()
        mesa.uno.forceLose()

        assertEquals(GameSession.Phase.PERDIDO, mesa.uno.phase)
        assertFalse("quedo caido en vez de abandonar", mesa.uno.caido)
    }

    @Test
    fun elInvitadoNoSeLlevaVeinteNivelesDeRegalo() {
        // El anfitrion baja a un nivel altisimo y el invitado recien va por
        // el 5: ganar tiene que avanzarlo uno, no dejarlo en el 31.
        val perfilInvitado = perfil()
        repeat(4) { perfilInvitado.onLevelCompleted(perfilInvitado.maxLevel, 1000L, 10) }
        val antes = perfilInvitado.maxLevel

        val t = TransporteLocal()
        val s = GameSession(perfilInvitado, 30, 12345L)
        s.red = MatchLink(t, "yo", "Invitado", "skin_minero", anfitrion = false)
        s.posX = s.exitWorldX
        s.posZ = s.exitWorldZ
        repeat(10) { s.update(0.05f, GameSession.Input()) }
        assertEquals(GameSession.Phase.GANADO, s.phase)
        s.settle()

        assertEquals(
            "ganar de invitado le desbloqueo niveles que no jugo",
            antes + 1, perfilInvitado.maxLevel
        )
    }

    @Test
    fun jugandoTuPropioNivelEnSalaProgresasNormal() {
        // El otro lado de la moneda: si la sala baja a TU nivel, ganar tiene
        // que avanzarte igual que si jugaras solo.
        val p = perfil()
        val nivel = p.maxLevel
        val t = TransporteLocal()
        val s = GameSession(p, nivel, 999L)
        s.red = MatchLink(t, "yo", "Anfitrion", "skin_minero", anfitrion = true)
        s.posX = s.exitWorldX
        s.posZ = s.exitWorldZ
        repeat(10) { s.update(0.05f, GameSession.Input()) }
        s.settle()

        assertEquals("no avanzo jugando su propio nivel", nivel + 1, p.maxLevel)
    }

    @Test
    fun elQueSeQuedoEnLaSalaNoCongelaLaPartidaDeLosDemas() {
        // Alguien entra a la sala DESPUES de que arranco la partida: se queda
        // esperando arriba, sin bajar a la cueva. Si contara para el fin de
        // la partida, los que si estan abajo no podrian terminar nunca: no se
        // mueven, no se mueren y no pueden salir.
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        mesa.separar()
        // El de arriba solo se anuncia, nunca manda una pose.
        mesa.t1.enviar(NetProtocol.unirse("tarde", "Tardio", "skin_minero").codificar())
        mesa.bombear()

        mesa.uno.applyDamage(99999f)
        mesa.dos.applyDamage(99999f)
        mesa.bombear()

        assertEquals(
            "el que miraba desde la sala congelo la partida",
            GameSession.Phase.PERDIDO, mesa.uno.phase
        )
    }

    @Test
    fun elAnfitrionRepiteElArranqueParaElQueSeLoPerdio() {
        // El ARRANQUE se manda una sola vez. Si justo se pierde, el invitado
        // se quedaria mirando la sala mientras el anfitrion juega solo.
        val t = TransporteLocal()
        val invitado = t.companero()
        val red = MatchLink(t, "uno", "Maxi", "skin_minero", anfitrion = true)
        red.arrancar(NetProtocol.Modo.CARRERA, 7, 4242L)
        // Se pierde todo lo mandado hasta aca, arranque incluido.
        invitado.recibir()

        repeat(((MatchLink.CADA_PING + 1f) / 0.1f).toInt()) { red.latir(0.1f) }
        val arranques = invitado.recibir().mapNotNull { NetProtocol.decodificar(it) }
            .filter { it.tipo == NetProtocol.Tipo.ARRANQUE }
        assertTrue("el anfitrion no repitio nunca el arranque", arranques.isNotEmpty())
        assertEquals("repitio otra semilla", "4242", arranques.first().arg(2))
    }

    @Test
    fun unArranqueRepetidoNoReiniciaLaPartidaEnCurso() {
        // La otra cara de repetirlo: al que ya esta jugando no le puede
        // borrar lo que paso en la cueva.
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO, nivel = 7, semilla = 4242L)
        mesa.separar()
        mesa.uno.applyDamage(99999f)
        mesa.bombear()
        assertTrue(mesa.uno.caido)
        assertTrue(mesa.dos.red!!.match.jugador("uno")!!.caido)

        // Llega otra vez el mismo arranque.
        mesa.dos.red!!.match.aplicar(
            NetProtocol.arranque("uno", NetProtocol.Modo.COOPERATIVO, 7, 4242L).codificar()
        )
        assertTrue(
            "el arranque repetido levanto al caido de la nada",
            mesa.dos.red!!.match.jugador("uno")!!.caido
        )
    }

    @Test
    fun elCaidoNoSeCuraSoloMientrasEspera() {
        val mesa = Mesa(NetProtocol.Modo.COOPERATIVO)
        mesa.separar()
        mesa.uno.applyDamage(99999f)
        mesa.bombear()
        assertTrue(mesa.uno.caido)

        mesa.segundos(8f)
        assertEquals(
            "se curo solo tirado en el piso",
            0f, mesa.uno.health, 0.01f
        )
    }

    @Test
    fun elQueSeVaAvisaAntesDeCortar() {
        val t = TransporteLocal()
        val otro = t.companero()
        val red = MatchLink(t, "uno", "Maxi", "skin_minero", anfitrion = true)
        otro.recibir()
        red.cerrar()

        val ultimo = otro.recibir().mapNotNull { NetProtocol.decodificar(it) }
        assertTrue(
            "se fue sin avisar: el otro lo ve 12 segundos como fantasma",
            ultimo.any { it.tipo == NetProtocol.Tipo.SALIR }
        )
        // Y despues de cerrar no manda nada mas.
        red.bombear(1f, 1f, 1f, 1f, 1f, 0)
        assertTrue(otro.recibir().isEmpty())
    }

    @Test
    fun soloElAnfitrionRepartePartida() {
        val t = TransporteLocal()
        val otro = t.companero()
        val invitado = MatchLink(t, "dos", "Colo", "skin_minero", anfitrion = false)
        otro.recibir()

        invitado.arrancar(NetProtocol.Modo.CARRERA, 9, 123L)
        val mandados = otro.recibir().mapNotNull { NetProtocol.decodificar(it) }
        assertTrue(
            "un invitado pudo repartir la partida",
            mandados.none { it.tipo == NetProtocol.Tipo.ARRANQUE }
        )
        assertFalse(invitado.match.arrancada)
    }

    @Test
    fun elAnfitrionTambienSeAplicaSuPropioArranque() {
        val t = TransporteLocal()
        val red = MatchLink(t, "uno", "Maxi", "skin_minero", anfitrion = true)
        red.arrancar(NetProtocol.Modo.COOPERATIVO, 12, 777L)
        assertTrue("el anfitrion no sabe con que semilla se juega", red.match.arrancada)
        assertEquals(12, red.match.nivel)
        assertEquals(777L, red.match.semilla)
        assertEquals(NetProtocol.Modo.COOPERATIVO, red.match.modo)
    }

    @Test
    fun unaPartidaSolitariaNoSabeNadaDeRed() {
        // La red es opcional: sin ella nada de esto tiene que correr.
        val s = GameSession(perfil(), 3)
        val quieto = GameSession.Input()
        repeat(20) { s.update(0.05f, quieto) }
        assertEquals(GameSession.Phase.JUGANDO, s.phase)
        assertFalse(s.caido)
    }

    // ---------------------------------------- seguir con la sala al otro nivel

    /**
     * Estos dos prueban el mecanismo de bajo nivel que sostiene "seguir con
     * la sala" (MggxApp / MultiplayerScreen, que son Compose y no se pueden
     * testear con JUnit puro en este repo): `match.arrancada` nunca vuelve
     * a false, asi que la pantalla tiene que distinguir "es el mismo
     * arranque de siempre" de "el anfitrion reparto un nivel de verdad
     * nuevo" comparando (nivel, semilla) a mano.
     */
    @Test
    fun elAnfitrionPuedeRepartirUnNivelNuevoSobreLaMismaSalaSinConfundirloConElViejo() {
        val t = TransporteLocal()
        val otro = t.companero()
        val anfitrion = MatchLink(t, "uno", "Maxi", "skin_minero", anfitrion = true)
        val invitado = MatchLink(otro, "dos", "Colo", "skin_minero", anfitrion = false)

        anfitrion.arrancar(NetProtocol.Modo.COOPERATIVO, 5, 111L)
        invitado.bombear(0.1f, 0f, 0f, 0f, 0f, 0)
        val jugado = invitado.match.nivel to invitado.match.semilla
        assertEquals(5, jugado.first)

        // Termina el nivel: nadie cierra la sala, sigue viva de los dos lados.
        assertFalse(anfitrion.cerrado)
        assertFalse(invitado.cerrado)

        // El anfitrion reparte el SIGUIENTE nivel sobre la misma sala.
        anfitrion.arrancar(NetProtocol.Modo.COOPERATIVO, 6, 222L)
        invitado.bombear(0.1f, 0f, 0f, 0f, 0f, 0)

        assertTrue(invitado.match.arrancada)
        assertNotEquals(
            "el nivel nuevo no se distingue del que ya se jugo",
            jugado, invitado.match.nivel to invitado.match.semilla
        )
        assertEquals(6, invitado.match.nivel)
        assertEquals(222L, invitado.match.semilla)
    }

    @Test
    fun laRetransmisionDelMismoArranqueNoSeConfundeConUnNivelNuevo() {
        // El anfitrion repite su ultimo ARRANQUE cada pocos segundos (para
        // el que se lo perdio). Comparar (nivel, semilla) tiene que dar
        // IGUAL en ese caso, o la pantalla dispararia un nivel "nuevo" que
        // en realidad es el mismo de siempre, una y otra vez.
        val t = TransporteLocal()
        val otro = t.companero()
        val anfitrion = MatchLink(t, "uno", "Maxi", "skin_minero", anfitrion = true)
        val invitado = MatchLink(otro, "dos", "Colo", "skin_minero", anfitrion = false)

        anfitrion.arrancar(NetProtocol.Modo.CARRERA, 9, 4242L)
        invitado.bombear(0.1f, 0f, 0f, 0f, 0f, 0)
        val jugado = invitado.match.nivel to invitado.match.semilla

        // Retransmision: mismo nivel, misma semilla, de nuevo.
        invitado.match.aplicar(NetProtocol.arranque("uno", NetProtocol.Modo.CARRERA, 9, 4242L).codificar())

        assertEquals(
            "la retransmision del mismo nivel se veria como uno nuevo",
            jugado, invitado.match.nivel to invitado.match.semilla
        )
    }

    @Test
    fun enSolitarioSeSiguePerdiendoDeLaFormaDeSiempre() {
        // Sin red, quedarse sin vida es perder, no quedar caido esperando a
        // un companiero que no existe.
        val s = GameSession(perfil(), 3)
        s.applyDamage(99999f)
        assertEquals(GameSession.Phase.PERDIDO, s.phase)
        assertFalse(s.caido)
    }
}

