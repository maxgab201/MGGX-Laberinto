package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.EffectType
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.hypot

class GameSessionTest {

    private fun perfil(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    private fun sesion(level: Int = 1, seed: Long = 12345L, save: SaveData = perfil()) =
        GameSession(save, level, seed)

    /** Camina de verdad por el laberinto siguiendo el camino minimo. */
    private fun caminarHastaLaSalida(s: GameSession, maxSegundos: Float = 900f): Boolean {
        val C = GameSession.CELL
        val path = s.maze.solutionPath
        val dt = 1f / 60f
        var tiempo = 0f
        var idx = 0
        val input = GameSession.Input()
        while (idx < path.size && tiempo < maxSegundos && s.phase == GameSession.Phase.JUGANDO) {
            val gi = path[idx]
            val tx = (gi % s.maze.gw + 0.5f) * C
            val tz = (gi / s.maze.gw + 0.5f) * C
            val dx = tx - s.posX
            val dz = tz - s.posZ
            val d = hypot(dx, dz)
            if (d < 0.45f) { idx++; continue }
            // Apunta al objetivo y camina de frente
            s.yawDeg = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
            input.moveX = 0f
            input.moveY = 1f
            input.lookX = 0f
            input.lookY = 0f
            s.update(dt, input)
            tiempo += dt
        }
        return s.phase == GameSession.Phase.GANADO
    }


    /** Avanza unos segundos siguiendo el camino minimo, para salir del punto de partida. */
    private fun avanzarUnPoco(s: GameSession, segundos: Float = 4f) {
        val C = GameSession.CELL
        val path = s.maze.solutionPath
        val dt = 1f / 60f
        var t = 0f
        var idx = 0
        val input = GameSession.Input(moveY = 1f)
        while (idx < path.size && t < segundos && s.phase == GameSession.Phase.JUGANDO) {
            val gi = path[idx]
            val tx = (gi % s.maze.gw + 0.5f) * C
            val tz = (gi / s.maze.gw + 0.5f) * C
            val d = hypot(tx - s.posX, tz - s.posZ)
            if (d < 0.45f) { idx++; continue }
            s.yawDeg = Math.toDegrees(atan2((tx - s.posX).toDouble(), (tz - s.posZ).toDouble())).toFloat()
            s.update(dt, input)
            t += dt
        }
    }

    @Test
    fun sePuedeLlegarCaminandoHastaLaSalida() {
        for (level in intArrayOf(1, 2, 6, 14, 30)) {
            val save = perfil()
            // Vida alta para que ninguna trampa corte la prueba
            repeat(40) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
            val s = GameSession(save, level, level * 7919L)
            s.health = 100000f
            assertTrue(
                "no se pudo llegar a la salida en el nivel $level",
                caminarHastaLaSalida(s)
            )
        }
    }

    @Test
    fun nuncaSeArrancaMirandoAUnaPared() {
        for (level in 1..60) {
            val s = sesion(level, level * 137L)
            // A un paso y medio hacia adelante tiene que haber aire, no roca.
            val rad = Math.toRadians(s.yawDeg.toDouble())
            val fx = Math.sin(rad).toFloat()
            val fz = Math.cos(rad).toFloat()
            val x = s.posX + fx * GameSession.CELL * 1.2f
            val z = s.posZ + fz * GameSession.CELL * 1.2f
            val gx = (x / GameSession.CELL).toInt()
            val gy = (z / GameSession.CELL).toInt()
            assertFalse(
                "en el nivel $level se arranca de frente a la roca (yaw=${s.yawDeg})",
                s.maze.isSolid(gx, gy)
            )
        }
    }

    @Test
    fun elJugadorArrancaEnUnLugarLibreYNoAtraviesaLaRoca() {
        for (level in 1..40) {
            val s = sesion(level, level * 31L)
            assertFalse("arranca dentro de la roca en el nivel $level", s.collides(s.posX, s.posZ))
        }
    }

    @Test
    fun noSePuedeCaminarAtravesandoParedes() {
        val s = sesion(3, 99L)
        val dt = 1f / 60f
        val input = GameSession.Input(moveY = 1f)
        // Da vueltas en todas las direcciones empujando contra lo que haya
        for (paso in 0 until 4000) {
            s.yawDeg = (paso * 37f) % 360f
            s.update(dt, input)
            assertFalse(
                "el jugador termino dentro de una pared (paso $paso)",
                s.collides(s.posX, s.posZ)
            )
        }
    }

    @Test
    fun elCronometroCorreYSeCongelaConElReloj() {
        val save = perfil()
        save.grantConsumable("reloj_arena", 1)
        repeat(10) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
        val s = GameSession(save, 6, 5L)
        val input = GameSession.Input()
        repeat(60) { s.update(1f / 60f, input) }
        val t1 = s.elapsedMs
        assertTrue("el reloj no avanza", t1 > 800)

        assertTrue(s.useItem("reloj_arena"))
        repeat(60) { s.update(1f / 60f, input) }
        assertEquals("el reloj siguio corriendo estando congelado", t1, s.elapsedMs)
    }

    @Test
    fun todosLosConsumiblesSePuedenUsarSinRomperNada() {
        val save = perfil()
        repeat(60) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
        for (item in ItemCatalog.ofKind(ItemKind.CONSUMIBLE)) {
            val s = GameSession(save, 20, item.id.hashCode().toLong())
            // Condiciones para que el uso tenga sentido
            // Se aleja del punto de partida: hay objetos (como la Semilla de
            // Retorno) que con razon no hacen nada si no te moviste.
            avanzarUnPoco(s, 5f)
            s.health = s.stats.maxHealth * 0.4f
            s.stamina = s.stats.maxStamina * 0.3f
            save.grantConsumable(item.id, 2)
            val antes = save.stockOf(item.id)
            val ok = s.useItem(item.id)
            assertTrue("no se pudo usar ${item.id}", ok)
            assertTrue(
                "usar ${item.id} no gasto ni conservo el objeto de forma valida",
                save.stockOf(item.id) == antes - 1 || save.stockOf(item.id) == antes
            )
            // Y el juego sigue andando despues de usarlo
            repeat(20) { s.update(1f / 60f, GameSession.Input()) }
            assertNotEquals(GameSession.Phase.PERDIDO, s.phase)
        }
    }

    @Test
    fun usarUnObjetoQueNoTenesNoHaceNada() {
        val s = sesion()
        assertFalse(s.useItem("bengala"))
        assertFalse(s.useItem("no_existe_este_objeto"))
        // Y las mejoras y reliquias no son usables como consumible
        assertFalse(s.useItem("up_botas"))
        assertFalse(s.useItem("rel_ambar"))
    }

    @Test
    fun losEfectosTemporalesSeApaganSolos() {
        val save = perfil()
        save.grantConsumable("pocion_zancada", 1)
        val s = GameSession(save, 1, 7L)
        assertTrue(s.useItem("pocion_zancada"))
        assertTrue(s.effects.isActive(EffectType.VELOCIDAD))
        val dur = ItemCatalog.require("pocion_zancada").effect.duration
        val pasos = ((dur + 1f) * 60f).toInt()
        repeat(pasos) { s.update(1f / 60f, GameSession.Input()) }
        assertFalse("el efecto no se apago", s.effects.isActive(EffectType.VELOCIDAD))
    }

    @Test
    fun elMapaSeRevelaCaminandoYConElMapaCompleto() {
        val save = perfil()
        repeat(12) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
        save.grantConsumable("mapa_completo", 1)
        val s = GameSession(save, 10, 42L)
        val reveladoInicial = s.revealed.count { it }
        assertTrue("no se revela nada alrededor del inicio", reveladoInicial > 0)
        assertTrue("arranca con el mapa entero", reveladoInicial < s.revealed.size)
        assertTrue(s.useItem("mapa_completo"))
        assertEquals(s.revealed.size, s.revealed.count { it })
    }

    @Test
    fun elDanoBajaLaVidaYPuedeTerminarLaPartida() {
        val s = sesion(1, 3L)
        val vidaInicial = s.health
        s.applyDamage(10f)
        assertTrue(s.health < vidaInicial)
        s.applyDamage(100000f)
        assertEquals(GameSession.Phase.PERDIDO, s.phase)
    }

    @Test
    fun laCuerdaDeRescateTeDaUnaVidaMas() {
        val save = perfil()
        save.addEcos(0); save.addVetagris(50)
        repeat(20) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
        assertEquals(SaveData.BuyResult.OK, save.buy("up_cuerda"))
        val s = GameSession(save, 10, 11L)
        assertEquals(1, s.livesLeft)
        s.applyDamage(100000f)
        assertEquals("deberia haberse salvado con la cuerda", GameSession.Phase.JUGANDO, s.phase)
        assertEquals(0, s.livesLeft)
        s.applyDamage(100000f)
        assertEquals(GameSession.Phase.PERDIDO, s.phase)
    }

    @Test
    fun elPicoAbreLaParedDeAdelanteYNoRompeElBorde() {
        val save = perfil()
        repeat(12) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
        save.grantConsumable("pico_mano", 1)
        val s = GameSession(save, 10, 77L)
        assertTrue(s.useItem("pico_mano"))

        // Se busca una casilla libre con una pared INTERIOR al lado y se para ahi.
        val m = s.maze
        var libreX = -1; var libreY = -1; var paredX = -1; var paredY = -1
        outer@ for (gy in 2 until m.gh - 2) {
            for (gx in 2 until m.gw - 2) {
                if (m.isSolid(gx, gy)) continue
                val vecinos = arrayOf(gx + 1 to gy, gx - 1 to gy, gx to gy + 1, gx to gy - 1)
                for ((nx, ny) in vecinos) {
                    if (m.isSolid(nx, ny) && nx > 0 && ny > 0 && nx < m.gw - 1 && ny < m.gh - 1) {
                        libreX = gx; libreY = gy; paredX = nx; paredY = ny
                        break@outer
                    }
                }
            }
        }
        assertTrue("el laberinto no tiene ninguna pared interior", paredX >= 0)

        s.posX = (libreX + 0.5f) * GameSession.CELL
        s.posZ = (libreY + 0.5f) * GameSession.CELL
        s.yawDeg = Math.toDegrees(
            atan2((paredX - libreX).toDouble(), (paredY - libreY).toDouble())
        ).toFloat()

        assertEquals(paredX to paredY, s.wallAhead())
        assertTrue(s.canBreakWall())
        assertTrue(s.breakWall())
        assertFalse("la pared sigue solida", m.isSolid(paredX, paredY))
        assertTrue("hay que rehacer la malla", s.geometryDirty)
        assertEquals("gasto mas cargas de las debidas", 0, s.pickCharges)
        assertFalse("rompio sin tener cargas", s.breakWall())

        // El borde exterior sigue intacto y el nivel sigue teniendo solucion
        for (x in 0 until m.gw) { assertTrue(m.isSolid(x, 0)); assertTrue(m.isSolid(x, m.gh - 1)) }
        for (y in 0 until m.gh) { assertTrue(m.isSolid(0, y)); assertTrue(m.isSolid(m.gw - 1, y)) }
        assertTrue(m.isSolvable())
    }

    @Test
    fun elPicoNuncaRompeLaParedExterior() {
        val save = perfil()
        save.grantConsumable("pico_mano", 3)
        val s = GameSession(save, 1, 3L)
        assertTrue(s.useItem("pico_mano"))
        val m = s.maze
        // Parado pegado al borde y mirandolo de frente
        s.posX = 1.5f * GameSession.CELL
        s.posZ = 1.5f * GameSession.CELL
        s.yawDeg = 270f    // hacia -X, donde esta el borde
        assertEquals(null, s.wallAhead())
        assertFalse(s.canBreakWall())
        assertFalse(s.breakWall())
        for (y in 0 until m.gh) assertTrue(m.isSolid(0, y))
    }

    @Test
    fun laTizaDejaMarcasYSeAgota() {
        val save = perfil()
        save.grantConsumable("tiza_luminosa", 1)
        val s = GameSession(save, 2, 5L)
        assertFalse("no deberia poder marcar sin tiza", s.dropChalk())
        assertTrue(s.useItem("tiza_luminosa"))
        val cargas = ItemCatalog.require("tiza_luminosa").effect.charges
        repeat(cargas) { assertTrue(s.dropChalk()) }
        assertEquals(cargas, s.marks.size)
        assertFalse("dejo mas marcas de las que tenia", s.dropChalk())
    }

    @Test
    fun elImanSeLlevaTodosLosEcos() {
        val save = perfil()
        repeat(10) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
        save.grantConsumable("piedra_iman", 1)
        val s = GameSession(save, 8, 21L)
        val antes = s.ecosCollected
        assertTrue(s.useItem("piedra_iman"))
        // Los ecos vuelan hasta el jugador
        repeat(60 * 12) { s.update(1f / 60f, GameSession.Input()) }
        assertTrue("el iman no junto nada", s.ecosCollected > antes)
    }

    @Test
    fun laRecompensaSeAcreditaUnaSolaVez() {
        val save = perfil()
        val s = GameSession(save, 1, 9L)
        s.health = 100000f
        assertTrue(caminarHastaLaSalida(s))
        val r1 = s.settle()
        assertTrue(r1 != null && r1.totalEcos > 0)
        val saldo = save.balance(com.mggx.laberinto.game.Currency.ECOS)
        assertEquals("se acredito dos veces", null, s.settle())
        assertEquals(saldo, save.balance(com.mggx.laberinto.game.Currency.ECOS))
        assertEquals(2, save.maxLevel)
    }

    @Test
    fun abandonarDaLaMitadYNoDesbloqueaNivel() {
        val save = perfil()
        val s = GameSession(save, 1, 13L)
        repeat(200) { s.update(1f / 60f, GameSession.Input(moveY = 1f)) }
        s.forceLose()
        val r = s.settle()!!
        assertEquals(1, save.maxLevel)
        assertTrue(r.totalEcos <= s.ecosCollected)
        assertEquals(1, save.totalDeaths)
    }

    @Test
    fun laBrujulaApuntaALaSalida() {
        val save = perfil()
        repeat(8) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
        val s = GameSession(save, 7, 4L)
        // Mirando justo hacia la salida el rumbo tiene que dar casi cero
        val dx = s.exitWorldX - s.posX
        val dz = s.exitWorldZ - s.posZ
        s.yawDeg = Math.toDegrees(atan2(dx.toDouble(), dz.toDouble())).toFloat()
        assertTrue("la brujula no apunta bien", Math.abs(s.bearingToExit()) < 1.5f)
        // Girando 180 grados tiene que marcar el lado opuesto
        s.yawDeg = (s.yawDeg + 180f) % 360f
        assertTrue(Math.abs(Math.abs(s.bearingToExit()) - 180f) < 2f)
    }

    @Test
    fun laPausaFrenaElJuego() {
        val s = sesion(1, 2L)
        val x0 = s.posX; val z0 = s.posZ
        s.pause()
        repeat(120) { s.update(1f / 60f, GameSession.Input(moveY = 1f)) }
        assertEquals(x0, s.posX, 0.0001f)
        assertEquals(z0, s.posZ, 0.0001f)
        assertEquals(0L, s.elapsedMs)
        s.resume()
        repeat(120) { s.update(1f / 60f, GameSession.Input(moveY = 1f)) }
        assertTrue("no se movio al reanudar", hypot(s.posX - x0, s.posZ - z0) > 0.5f)
    }

    @Test
    fun lasMejorasCambianDeVerdadLasEstadisticas() {
        val base = perfil()
        val s0 = com.mggx.laberinto.game.PlayerStats(base)
        val mejorado = perfil()
        mejorado.addEcos(1_000_000)
        repeat(40) { mejorado.onLevelCompleted(mejorado.maxLevel, 1000, 10) }
        listOf("up_botas", "up_corazon", "up_pulmones", "up_farol", "up_iman", "up_vista")
            .forEach { id -> repeat(ItemCatalog.require(id).maxLevel) { mejorado.buy(id) } }
        val s1 = com.mggx.laberinto.game.PlayerStats(mejorado)

        assertTrue("las botas no aceleran", s1.walkSpeed > s0.walkSpeed)
        assertTrue("el corazon no da vida", s1.maxHealth > s0.maxHealth)
        assertTrue("los pulmones no dan aguante", s1.maxStamina > s0.maxStamina)
        assertTrue("el farol no alumbra mas", s1.lightRadius > s0.lightRadius)
        assertTrue("el iman no llega mas lejos", s1.pickupRadius > s0.pickupRadius)
        assertTrue("la vista no abre el campo", s1.fovBonus > s0.fovBonus)
    }

    @Test
    fun elNivelSiempreTieneObjetosParaJuntar() {
        for (level in intArrayOf(1, 5, 17, 33, 55)) {
            val s = sesion(level, level * 101L)
            assertTrue("el nivel $level no tiene ecos", s.pickups.isNotEmpty())
            assertTrue("el nivel $level no tiene cofres",
                s.pickups.any { it.kind == GameSession.PickupKind.COFRE })
        }
    }
}
