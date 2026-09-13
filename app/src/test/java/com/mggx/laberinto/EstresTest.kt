package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.maze.Maze
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Le pega a la partida con entradas al azar, mucho rato y en muchos niveles, y
 * controla que nada se salga de lo posible.
 *
 * Es lo mas parecido a "que lo jueguen mil personas raras a la vez" que se
 * puede hacer sin telefono. Un test normal recorre UN camino pensado de
 * antemano; este recorre caminos que a nadie se le hubieran ocurrido, que es
 * justo donde viven los bugs que aparecen despues de una hora jugando.
 *
 * Lo que se cuida no son detalles de diseno, son cosas que NO PUEDEN pasar
 * nunca: quedarse fuera del mapa, tener NaN en la posicion, atravesar la roca,
 * vida negativa. Cualquiera de esas rompe la partida de alguien.
 */
class EstresTest {

    private fun perfil(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    /** Un perfil con todo comprado, para que corran tambien los poderes. */
    private fun perfilRico(): SaveData {
        val save = perfil()
        repeat(120) { save.onLevelCompleted(save.maxLevel, 1200, 12) }
        repeat(600) { save.addVetagris(1) }
        repeat(60) { save.addEcos(9000) }
        return save
    }

    private fun entradaAlAzar(r: Random) = GameSession.Input(
        moveX = r.nextFloat() * 2f - 1f,
        moveY = r.nextFloat() * 2f - 1f,
        lookX = (r.nextFloat() * 2f - 1f) * 14f,
        lookY = (r.nextFloat() * 2f - 1f) * 9f,
        running = r.nextBoolean(),
        agacharse = r.nextInt(3),
        saltar = r.nextFloat() < 0.06f
    )

    /** Controla todo lo que no puede pasar nunca. Devuelve el motivo, o null. */
    private fun revisar(s: GameSession, nivel: Int, cuadro: Int): String? {
        val m = s.maze
        if (s.posX.isNaN() || s.posZ.isNaN() || s.posY.isNaN()) {
            return "posicion NaN en el nivel $nivel, cuadro $cuadro"
        }
        if (s.posX.isInfinite() || s.posZ.isInfinite() || s.posY.isInfinite()) {
            return "posicion infinita en el nivel $nivel, cuadro $cuadro"
        }
        if (s.yawDeg.isNaN() || s.pitchDeg.isNaN()) {
            return "camara NaN en el nivel $nivel, cuadro $cuadro"
        }
        // La mirada vertical esta limitada a proposito: si se pasa, la camara
        // se da vuelta y el juego se vuelve injugable.
        if (s.pitchDeg < -90f || s.pitchDeg > 90f) {
            return "la camara se dio vuelta (pitch=${s.pitchDeg}) en el nivel $nivel"
        }
        val gx = (s.posX / GameSession.CELL).toInt()
        val gy = (s.posZ / GameSession.CELL).toInt()
        if (!m.inBounds(gx, gy)) {
            return "el jugador se fue del mapa a ($gx,$gy) en el nivel $nivel, cuadro $cuadro"
        }
        if (m.isSolid(gx, gy)) {
            return "el jugador quedo DENTRO de la roca en ($gx,$gy), nivel $nivel, cuadro $cuadro"
        }
        if (s.health.isNaN() || s.health < 0f) {
            return "vida invalida (${s.health}) en el nivel $nivel, cuadro $cuadro"
        }
        if (s.stamina.isNaN() || s.stamina < -0.01f) {
            return "aguante invalido (${s.stamina}) en el nivel $nivel, cuadro $cuadro"
        }
        // No se puede estar hundido en el piso ni flotando a diez metros.
        val suelo = m.floorY(gx, gy)
        if (s.posY < suelo - 0.6f) {
            return "el jugador se hundio en el piso (y=${s.posY}, suelo=$suelo) en el nivel $nivel"
        }
        if (s.posY > suelo + 6f) {
            return "el jugador se fue por el techo (y=${s.posY}, suelo=$suelo) en el nivel $nivel"
        }
        return null
    }

    @Test
    fun aguantaCualquierCosaQueLeHagasConLosControles() {
        for (nivel in intArrayOf(1, 3, 7, 12, 20, 31, 44, 58)) {
            val s = GameSession(perfil(), nivel, nivel * 7919L)
            val r = Random(nivel * 31L)
            var input = entradaAlAzar(r)
            for (cuadro in 0 until 5400) {          // 90 segundos a 60 fps
                // Se cambia de rumbo cada tanto, no en cada cuadro: asi el
                // jugador de mentira de verdad CAMINA en vez de temblar en el
                // lugar, que es como se llega a los rincones raros del mapa.
                if (cuadro % 25 == 0) input = entradaAlAzar(r)
                s.update(1f / 60f, input)
                revisar(s, nivel, cuadro)?.let { throw AssertionError(it) }
                if (s.phase != GameSession.Phase.JUGANDO) break
            }
        }
    }

    @Test
    fun aguantaConTodosLosPoderesPuestos() {
        // Los poderes cambian el movimiento (saltar mas alto, atravesar,
        // deslizarse): es donde es mas facil que algo se escape del mapa.
        val save = perfilRico()
        for (id in listOf(
            "pod_salto_alto", "pod_sombra", "pod_reptador", "pod_memoria_sima",
            "pod_veta", "pod_pico_eterno"
        )) {
            save.buy(id)
        }
        for (nivel in intArrayOf(9, 18, 27, 36)) {
            val s = GameSession(save, nivel, nivel * 4441L)
            val r = Random(nivel * 97L)
            var input = entradaAlAzar(r)
            for (cuadro in 0 until 4200) {
                if (cuadro % 20 == 0) input = entradaAlAzar(r)
                s.update(1f / 60f, input)
                revisar(s, nivel, cuadro)?.let { throw AssertionError(it) }
                if (s.phase != GameSession.Phase.JUGANDO) break
            }
        }
    }

    @Test
    fun aguantaGolpearYUsarObjetosSinParar() {
        val save = perfilRico()
        for (item in com.mggx.laberinto.game.ItemCatalog.ofKind(
            com.mggx.laberinto.game.ItemKind.CONSUMIBLE
        )) {
            repeat(8) { save.buy(item.id) }
        }
        val s = GameSession(save, 22, 55555L)
        val r = Random(7L)
        val bolsa = save.bolsaDeMano()
        var input = entradaAlAzar(r)
        for (cuadro in 0 until 5000) {
            if (cuadro % 15 == 0) input = entradaAlAzar(r)
            if (r.nextFloat() < 0.15f) s.golpear()
            if (r.nextFloat() < 0.05f && bolsa.isNotEmpty()) {
                s.useItem(bolsa[r.nextInt(bolsa.size)])
            }
            s.update(1f / 60f, input)
            revisar(s, 22, cuadro)?.let { throw AssertionError(it) }
            if (s.phase != GameSession.Phase.JUGANDO) break
        }
    }

    @Test
    fun elCuadroLargoNoLoRompe() {
        // Un telefono que se traba (una notificacion, el recolector) entrega un
        // cuadro larguisimo de golpe. Si el movimiento no tuviera techo, en ese
        // cuadro el jugador avanzaria metros y cruzaria una pared entera.
        for (nivel in intArrayOf(5, 15, 40)) {
            val s = GameSession(perfil(), nivel, nivel * 131L)
            val r = Random(nivel.toLong())
            for (cuadro in 0 until 900) {
                val input = entradaAlAzar(r)
                // Mezcla de cuadros normales y trabones de hasta un segundo.
                val dt = if (r.nextFloat() < 0.25f) r.nextFloat() * 1.0f else 1f / 60f
                s.update(dt, input)
                revisar(s, nivel, cuadro)?.let { throw AssertionError(it) }
                if (s.phase != GameSession.Phase.JUGANDO) break
            }
        }
    }

    @Test
    fun elCuadroDeCeroNoLoRompe() {
        // dt = 0 pasa de verdad: dos cuadros en el mismo nanosegundo, o el
        // primer cuadro despues de volver de pausa. Cualquier division por dt
        // sin proteger revienta aca.
        val s = GameSession(perfil(), 14, 99L)
        val input = GameSession.Input(moveY = 1f, lookX = 3f)
        repeat(400) {
            s.update(0f, input)
            revisar(s, 14, it)?.let { e -> throw AssertionError(e) }
        }
        // Y con dt negativo, que no deberia pasar pero es barato cubrirlo.
        repeat(100) {
            s.update(-0.5f, input)
            revisar(s, 14, it)?.let { e -> throw AssertionError(e) }
        }
    }

    @Test
    fun todosLosNivelesSeGeneranSinRomperNada() {
        // Barrido largo del generador: es donde un indice fuera de rango o una
        // division por cero se esconde hasta que a alguien le toca la semilla.
        for (nivel in 1..120) {
            val bp = MazeGenerator.generate(nivel, nivel * 1_000_003L)
            val m = bp.maze
            assertTrue("el nivel $nivel no tiene solucion", m.isSolvable())
            for (i in m.floorLevel.indices) {
                assertTrue(
                    "altura de piso absurda en el nivel $nivel",
                    m.floorLevel[i] in -20..20
                )
                val c = m.ceilClearance[i]
                assertTrue("techo invalido en el nivel $nivel", !c.isNaN() && c > 0f && c < 20f)
            }
            for (t in bp.traps) {
                assertTrue("trampa fuera del mapa en el nivel $nivel", m.inBounds(t.gx, t.gy))
                assertFalse("trampa dentro de la roca en el nivel $nivel", m.isSolid(t.gx, t.gy))
            }
            for (e in bp.enemies) {
                assertFalse("bicho dentro de la roca en el nivel $nivel", m.isSolid(e.gx, e.gy))
            }
            for (lista in listOf(
                bp.coins, bp.bigCoins, bp.crystals, bp.chests, bp.torches,
                bp.stalagmites, bp.carbide, bp.crystalClusters, bp.rocks,
                bp.mushrooms, bp.beams
            )) {
                for (gi in lista) {
                    assertTrue(
                        "objeto fuera del mapa en el nivel $nivel (indice $gi)",
                        gi >= 0 && gi < m.gw * m.gh
                    )
                }
            }
        }
    }

    @Test
    fun elAguaNuncaDejaUnNivelImposible() {
        // El agua se calcula despues del relieve y no lo toca, pero conviene
        // barrerlo ancho: es lo ultimo que se agrego al generador.
        for (nivel in 1..90) {
            val m = MazeGenerator.generate(nivel, nivel * 7_777_777L).maze
            assertTrue(
                "el nivel $nivel quedo intransitable",
                com.mggx.laberinto.maze.ReliefGenerator.esTransitable(m)
            )
            if (m.waterY != com.mggx.laberinto.maze.AguaDeLaCueva.SIN_AGUA) {
                assertFalse(
                    "el nivel $nivel arranca dentro del agua",
                    m.hayAgua(m.startGx, m.startGy)
                )
            }
        }
    }

    @Test
    fun elPerfilAguantaGuardarYCargarCualquierCosa() {
        // El perfil se serializa a JSON y se vuelve a leer en cada arranque. Un
        // campo que se guarde mal no se nota hasta que alguien cierra el juego
        // y pierde el progreso.
        val store = SaveData.memoryStore()
        val a = SaveData.fromStore(store)
        val r = Random(4242L)
        repeat(300) {
            when (r.nextInt(6)) {
                0 -> a.addEcos(r.nextInt(5000))
                1 -> a.addVetagris(r.nextInt(9))
                2 -> a.onLevelCompleted(a.maxLevel, r.nextLong(100_000), r.nextInt(900))
                3 -> com.mggx.laberinto.game.ItemCatalog.all.randomOrNull(r)?.let { a.buy(it.id) }
                4 -> a.setNombreJugador("Minero ${r.nextInt(99)}")
                else -> a.settings.quality = r.nextInt(4)
            }
        }
        a.save()

        val b = SaveData.fromStore(store)
        assertTrue("los ecos no sobrevivieron al guardado", b.ecos == a.ecos)
        assertTrue("el vetagris no sobrevivio", b.vetagris == a.vetagris)
        assertTrue("el nivel maximo no sobrevivio", b.maxLevel == a.maxLevel)
        assertTrue("el nombre no sobrevivio", b.nombreJugador == a.nombreJugador)
        assertTrue("la calidad no sobrevivio", b.settings.quality == a.settings.quality)
        assertTrue("el nivel actual quedo fuera de rango", b.currentLevel in 1..b.maxLevel)
    }

    @Test
    fun unPerfilCorruptoNoTumbaElJuego() {
        // Un archivo a medio escribir (se quedo sin bateria guardando) no puede
        // dejar el juego sin arrancar: se cae al perfil nuevo y listo.
        for (basura in listOf(
            "", "   ", "{", "}", "no soy json", "[1,2,3]",
            """{"ecos":"muchos","maxLevel":-5}""",
            """{"ecos":99999999999999999999}""",
            """{"maxLevel":2147483647,"curLevel":2147483647}""",
            """{"settings":{"quality":999,"fps":-3}}"""
        )) {
            val store = SaveData.memoryStore()
            store.write(basura)
            val s = SaveData.fromStore(store)
            assertTrue("un perfil roto dejo el nivel maximo invalido", s.maxLevel >= 1)
            assertTrue("un perfil roto dejo el nivel actual invalido", s.currentLevel >= 1)
            assertTrue("un perfil roto dejo el nivel actual por arriba del maximo",
                s.currentLevel <= s.maxLevel)
            assertTrue("un perfil roto dejo los ecos en negativo", s.ecos >= 0)
            assertTrue("un perfil roto dejo el vetagris en negativo", s.vetagris >= 0)
            // Y tiene que poder jugarse.
            val g = GameSession(s, s.currentLevel, 1L)
            repeat(60) { g.update(1f / 60f, GameSession.Input(moveY = 1f)) }
        }
    }

    @Test
    fun elMazoNoSeQuedaSinSalidaAlRomperParedes() {
        // El pico rompe paredes. Romper SOLO abre roca, asi que la cueva no
        // puede quedar sin solucion, pero conviene cuidarlo: es el unico lugar
        // donde el jugador cambia el mapa.
        val save = perfilRico()
        save.buy("pod_pico_eterno")
        val s = GameSession(save, 26, 616161L)
        val m = s.maze
        val r = Random(11L)
        repeat(400) {
            val gx = r.nextInt(m.gw)
            val gy = r.nextInt(m.gh)
            if (m.isSolid(gx, gy)) m.setSolid(gx, gy, false)
        }
        m.refreshSolution()
        assertTrue("romper paredes dejo el nivel sin salida", m.isSolvable())
        assertTrue(
            "romper paredes rompio el borde de la cueva",
            (0 until m.gw).all { m.isSolid(it, 0) && m.isSolid(it, m.gh - 1) }
        )
    }

    @Test
    fun elMapaDeExploradoSobreviveAlIdaYVuelta() {
        // El mapa explorado se guarda comprimido en nibbles. Un error ahi se
        // ve como "volvi al nivel y el mapa esta todo mal", que es peor que
        // perderlo entero.
        val save = perfil()
        val m = MazeGenerator.generate(17, 17L).maze
        val original = BooleanArray(m.gw * m.gh)
        val r = Random(3L)
        for (i in original.indices) original[i] = r.nextFloat() < 0.4f
        save.rememberExplored(17, original)

        val vuelto = BooleanArray(original.size)
        assertTrue("no se pudo restaurar el mapa", save.restoreExplored(17, vuelto))
        for (i in original.indices) {
            assertTrue("la casilla $i volvio distinta", vuelto[i] == original[i])
        }
        // Y de otro nivel no tiene que devolver nada.
        val otro = BooleanArray(original.size)
        assertFalse("devolvio el mapa de otro nivel", save.restoreExplored(18, otro))
    }

    @Test
    fun unLaberintoMinimoNoRompeNada() {
        // El caso borde del generador: el nivel 1 es el mas chico que existe.
        // Un indice que solo falla cuando el mapa es chiquito es el peor de
        // encontrar, porque justo el nivel 1 lo juega todo el mundo.
        val m = Maze(7, 7)
        assertTrue(m.isSolid(0, 0))
        assertTrue("el borde se abrio", m.isSolid(3, 0))
        // Fuera de rango tiene que contestar sin reventar.
        assertTrue(m.isSolid(-1, -1))
        assertTrue(m.isSolid(9999, 9999))
        assertFalse(m.inBounds(-1, 0))
        assertFalse(m.hayAgua(-5, -5))
    }
}
