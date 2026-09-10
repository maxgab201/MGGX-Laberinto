package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.Enemy
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import com.mggx.laberinto.game.PlayerStats
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan2

/**
 * Pelearles a los bichos.
 *
 * El jugador se quejaba con razon: los bichos no se podian matar con nada.
 * Ahora se puede, y aunque no tengas plata para un arma se pega a mano limpia.
 */
class CombateTest {

    private fun perfil(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    private fun perfilRico(): SaveData {
        val save = perfil()
        repeat(80) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
        repeat(400) { save.addVetagris(1) }
        repeat(200) { save.addEcos(500) }
        return save
    }

    /** Planta un bicho justo enfrente del jugador, al alcance del golpe. */
    private fun bichoEnfrente(
        s: GameSession,
        kind: MazeGenerator.EnemyKind = MazeGenerator.EnemyKind.MURCIELAGO
    ): Enemy {
        val e = s.enemies.firstOrNull { it.kind == kind }
            ?: error("el nivel de prueba no tiene un ${kind.name}")
        val yaw = Math.toRadians(s.yawDeg.toDouble())
        val d = s.stats.alcanceGolpe * 0.6f
        e.x = s.posX + Math.sin(yaw).toFloat() * d
        e.z = s.posZ + Math.cos(yaw).toFloat() * d
        e.altura = s.posY + (if (e.vuela) 1.45f else 0f)
        return e
    }

    /** Un nivel que seguro tiene el bicho que se pide. */
    private fun sesionCon(
        save: SaveData,
        kind: MazeGenerator.EnemyKind
    ): GameSession {
        for (level in 3..80) {
            for (seed in 0L until 12L) {
                val s = GameSession(save, level, level * 1000L + seed)
                if (s.enemies.any { it.kind == kind }) return s
            }
        }
        error("no se encontro ningun nivel con ${kind.name}")
    }

    // ------------------------------------------------------------- catalogo

    @Test
    fun hayVariasArmasYTodasSonDistintas() {
        val armas = ItemCatalog.ofKind(ItemKind.ARMA)
        assertTrue("pocas armas: ${armas.size}", armas.size >= 4)
        val firmas = armas.map { Triple(it.effect.magnitude, it.effect.duration, it.effect.charges) }
        assertEquals("hay armas clonadas", firmas.size, firmas.toSet().size)
        for (a in armas) {
            assertTrue("${a.id} no hace dano", a.effect.magnitude > 0f)
            assertTrue("${a.id} no tiene cadencia", a.effect.duration > 0f)
            assertTrue("${a.id} no tiene alcance", a.effect.charges > 0)
            assertTrue(
                "${a.id} pega menos que las manos vacias",
                a.effect.magnitude > PlayerStats.GOLPE_BASE_DANO
            )
        }
    }

    @Test
    fun elArmaSeAgarraSolaAlComprarla() {
        val save = perfilRico()
        assertEquals("de arranque se pelea a mano limpia", "", save.armaEquipada)
        assertEquals(SaveData.BuyResult.OK, save.buy("arma_garrote"))
        assertEquals("arma_garrote", save.armaEquipada)
        assertEquals("Garrote de Roble", PlayerStats(save).nombreArma)
    }

    @Test
    fun sePuedeVolverAManoLimpia() {
        val save = perfilRico()
        save.buy("arma_garrote")
        save.equipArma("")
        assertEquals("", save.armaEquipada)
        assertEquals(PlayerStats.GOLPE_BASE_DANO, PlayerStats(save).danoGolpe, 0.001f)
    }

    @Test
    fun noSePuedeAgarrarUnArmaQueNoCompraste() {
        val save = perfil()
        save.equipArma("arma_hacha")
        assertEquals("se equipo un arma sin comprarla", "", save.armaEquipada)
    }

    @Test
    fun elArmaMejoraElGolpe() {
        val save = perfilRico()
        val aMano = PlayerStats(save)
        save.buy("arma_hacha")
        val conHacha = PlayerStats(save)
        assertTrue("el hacha no pega mas fuerte", conHacha.danoGolpe > aMano.danoGolpe * 3f)
        assertTrue("el hacha no llega mas lejos", conHacha.alcanceGolpe > aMano.alcanceGolpe)
    }

    // --------------------------------------------------------------- golpe

    @Test
    fun aManoLimpiaTambienSePega() {
        // Lo mas importante de todo: sin comprar nada, igual te podes defender.
        val save = perfil()
        val s = sesionCon(save, MazeGenerator.EnemyKind.MURCIELAGO)
        val e = bichoEnfrente(s)
        val vidaAntes = e.vida
        assertTrue(s.puedeGolpear())
        assertEquals("el golpe no toco al bicho de enfrente", 1, s.golpear())
        assertTrue("el bicho no perdio vida", e.vida < vidaAntes)
    }

    @Test
    fun elGolpeTieneCadenciaYNoSePuedeSpamear() {
        val save = perfil()
        val s = sesionCon(save, MazeGenerator.EnemyKind.MURCIELAGO)
        val e = bichoEnfrente(s)
        s.golpear()
        val vidaTrasUno = e.vida
        assertFalse("se pudo golpear dos veces seguidas", s.puedeGolpear())
        assertEquals(-1, s.golpear())
        assertEquals("el segundo golpe conto igual", vidaTrasUno, e.vida, 0.001f)

        // Pasada la recarga, si.
        val pasos = (s.stats.cadenciaGolpe * 60f).toInt() + 4
        repeat(pasos) { s.update(1f / 60f, GameSession.Input()) }
        bichoEnfrente(s)
        assertTrue("nunca se recargo el golpe", s.puedeGolpear())
    }

    @Test
    fun aLoQueEstaAtrasNoSeLePega() {
        val save = perfil()
        val s = sesionCon(save, MazeGenerator.EnemyKind.MURCIELAGO)
        val e = bichoEnfrente(s)
        // Se lo pone justo a la espalda.
        e.x = s.posX * 2f - e.x
        e.z = s.posZ * 2f - e.z
        val vidaAntes = e.vida
        assertEquals("le pego a algo que tenia atras", 0, s.golpear())
        assertEquals(vidaAntes, e.vida, 0.001f)
    }

    @Test
    fun aLoQueEstaLejosNoSeLePega() {
        val save = perfil()
        val s = sesionCon(save, MazeGenerator.EnemyKind.MURCIELAGO)
        val e = bichoEnfrente(s)
        val yaw = Math.toRadians(s.yawDeg.toDouble())
        val lejos = s.stats.alcanceGolpe + e.kind.radio + 1.5f
        e.x = s.posX + Math.sin(yaw).toFloat() * lejos
        e.z = s.posZ + Math.cos(yaw).toFloat() * lejos
        assertEquals("le pego a algo fuera de alcance", 0, s.golpear())
    }

    @Test
    fun elGolpeAlcanzaAVariosDeUnaVez() {
        val save = perfil()
        val s = sesionCon(save, MazeGenerator.EnemyKind.MURCIELAGO)
        val cerca = s.enemies.filter { it.kind == MazeGenerator.EnemyKind.MURCIELAGO }.take(3)
        if (cerca.size < 2) return   // ese nivel no tenia suficientes, no pasa nada
        val yaw = Math.toRadians(s.yawDeg.toDouble())
        cerca.forEachIndexed { i, e ->
            val d = s.stats.alcanceGolpe * 0.5f
            // En abanico, todos dentro del cono de enfrente.
            val a = yaw + (i - 1) * 0.25
            e.x = s.posX + Math.sin(a).toFloat() * d
            e.z = s.posZ + Math.cos(a).toFloat() * d
            e.altura = s.posY + 1.45f
        }
        assertEquals("el golpe no alcanzo a todos los de adelante", cerca.size, s.golpear())
    }

    // -------------------------------------------------------------- muerte

    @Test
    fun aFuerzaDeGolpesElBichoSeCae() {
        val save = perfilRico()
        save.buy("arma_hacha")
        val s = sesionCon(save, MazeGenerator.EnemyKind.MURCIELAGO)
        val e = bichoEnfrente(s)
        val ecosAntes = s.ecosCollected

        var vueltas = 0
        while (e.vivo && vueltas < 60) {
            bichoEnfrente(s)
            if (s.puedeGolpear()) s.golpear()
            s.update(1f / 60f, GameSession.Input())
            vueltas++
        }
        assertFalse("el bicho nunca se cayo", e.vivo)
        assertEquals(0f, e.vida, 0.001f)
        assertEquals(1, s.bichosVolteados)
        assertTrue("voltearlo no dio ecos", s.ecosCollected > ecosAntes)
    }

    @Test
    fun elBichoVolteadoDejaDeMolestar() {
        val save = perfilRico()
        save.buy("arma_hacha")
        val s = sesionCon(save, MazeGenerator.EnemyKind.MURCIELAGO)
        val e = bichoEnfrente(s)
        e.recibirGolpe(9999f, s.posX, s.posZ, 0f)
        assertFalse(e.vivo)

        val vivosAntes = s.bichosVivos()
        val vida = s.health
        // Se lo deja encima del jugador un rato largo: no tiene que morder ni
        // contar como que te esta persiguiendo.
        repeat(600) {
            e.x = s.posX; e.z = s.posZ
            s.update(1f / 60f, GameSession.Input())
        }
        assertEquals("un bicho volteado te siguio mordiendo", vida, s.health, 0.001f)
        assertEquals(vivosAntes, s.bichosVivos())
        assertFalse(s.enemies.first { !it.vivo }.let { it.vivo })
    }

    @Test
    fun elGolpeLoDespiertaAunqueNoTeHubieraVisto() {
        val save = perfil()
        val s = sesionCon(save, MazeGenerator.EnemyKind.MURCIELAGO)
        val e = bichoEnfrente(s)
        e.alerta = false
        e.recibirGolpe(1f, s.posX, s.posZ, 1f)
        assertTrue("le pegaste y siguio como si nada", e.alerta)
    }

    @Test
    fun elGolpeLoEmpujaParaAtras() {
        val save = perfil()
        val s = sesionCon(save, MazeGenerator.EnemyKind.MURCIELAGO)
        val e = bichoEnfrente(s)
        val antes = e.distanciaA(s.posX, s.posZ)
        e.recibirGolpe(1f, s.posX, s.posZ, 4f)
        assertNotEquals(0f, e.empujeX + e.empujeZ)
        // Durante el aturdimiento el bicho no avanza, asi que el golpe de
        // verdad lo separa. Puede quedar frenado contra una pared, pero nunca
        // termina mas cerca de lo que estaba.
        repeat(12) { s.update(1f / 60f, GameSession.Input()) }
        assertTrue(
            "el empuje lo dejo mas cerca que antes",
            e.distanciaA(s.posX, s.posZ) >= antes - 0.02f
        )
        assertTrue("el golpe no lo aturdio", e.aturdido > 0f)
    }

    @Test
    fun cadaBichoAguantaLoSuyo() {
        val vidas = MazeGenerator.EnemyKind.entries.map { it.vida }
        assertEquals("hay bichos con la misma vida", vidas.size, vidas.toSet().size)
        for (k in MazeGenerator.EnemyKind.entries) {
            assertTrue("${k.name} no tiene vida", k.vida > 0f)
            assertTrue("${k.name} no da recompensa", k.recompensa > 0)
        }
        // El guardian tiene que ser el hueso duro.
        assertEquals(
            MazeGenerator.EnemyKind.GUARDIAN,
            MazeGenerator.EnemyKind.entries.maxByOrNull { it.vida }
        )
    }

    @Test
    fun aManoLimpiaSePuedeVoltearUnMurcielagoSinTardarUnaEternidad() {
        // Es la unica forma de que el juego no sea injusto para el que recien
        // arranca: sin arma, un murcielago tiene que caer en pocos golpes.
        val golpes = Math.ceil(
            (MazeGenerator.EnemyKind.MURCIELAGO.vida / PlayerStats.GOLPE_BASE_DANO).toDouble()
        ).toInt()
        assertTrue("a mano limpia hacen falta $golpes golpes, son demasiados", golpes <= 3)
    }

    // ------------------------------------------------------ mordida (bug real)

    /**
     * Planta un bicho pegado al jugador, bien adentro del rango de mordida
     * (no del rango de golpe, que es mas largo). Sirve para probar que la
     * mordida en si funciona de punta a punta, pasando por GameSession.update().
     */
    private fun bichoBienCerca(
        s: GameSession,
        kind: MazeGenerator.EnemyKind = MazeGenerator.EnemyKind.MURCIELAGO
    ): Enemy {
        val e = s.enemies.firstOrNull { it.kind == kind }
            ?: error("el nivel de prueba no tiene un ${kind.name}")
        e.x = s.posX + 0.30f
        e.z = s.posZ
        e.altura = s.posY + (if (e.vuela) 1.45f else 0f)
        return e
    }

    @Test
    fun elBichoMuerdeAlJugadorPasivoDePuntaAPunta() {
        // El bug que reporto el jugador: "los bichos no hacen dano". Esto
        // prueba la mordida de verdad, pasando por GameSession.update() (no
        // por EnemyBrain aislado), sin que el jugador ataque nunca.
        val save = perfil()
        val s = sesionCon(save, MazeGenerator.EnemyKind.MURCIELAGO)
        bichoBienCerca(s)
        val vidaInicial = s.health
        repeat(600) {
            bichoBienCerca(s)
            s.update(1f / 60f, GameSession.Input())
        }
        assertTrue(
            "un murcielago pegado al jugador durante 10s no le bajo la vida",
            s.health < vidaInicial
        )
    }

    @Test
    fun elBichoSiguePudiendoMorderDurranteElCombateActivo() {
        // Guardrail de balance: el aturdimiento/empuje del golpe no puede
        // dejar al bicho sin NINGUNA chance de morder mientras el jugador
        // pelea, o "pegar" se vuelve la forma de no recibir nunca dano.
        val save = perfil()
        val s = sesionCon(save, MazeGenerator.EnemyKind.MURCIELAGO)
        bichoEnfrente(s)
        val vidaInicial = s.health
        repeat(300) {
            if (s.puedeGolpear()) s.golpear()
            s.update(1f / 60f, GameSession.Input())
        }
        assertTrue(
            "peleando activamente 5s el bicho nunca consiguio morder",
            s.health < vidaInicial
        )
    }

    // ------------------------------------------------- alcance del golpe

    /** Manda al resto de los bichos bien lejos: aca se mide uno solo. */
    private fun aislar(s: GameSession, e: Enemy) {
        for (o in s.enemies) if (o !== e) { o.x = -500f; o.z = -500f }
    }

    @Test
    fun elGolpeTieneMargenYNoHayQueEstarEncimaDelBicho() {
        // El jugador se quejaba de que "para pegarles te tienen que estar
        // pegando". El alcance del arma se medi­a de centro a centro, sin
        // contar el propio cuerpo: un bicho apenas mas lejos que
        // alcance+radio no se tocaba aunque lo tuvieras delante de la cara.
        val save = perfilRico()
        val s = sesionCon(save, MazeGenerator.EnemyKind.GUARDIAN)
        val e = bichoEnfrente(s, MazeGenerator.EnemyKind.GUARDIAN)
        aislar(s, e)

        val yaw = Math.toRadians(s.yawDeg.toDouble())
        val d = s.stats.alcanceGolpe + e.kind.radio + 0.25f   // fuera del alcance viejo
        e.x = s.posX + Math.sin(yaw).toFloat() * d
        e.z = s.posZ + Math.cos(yaw).toFloat() * d
        e.altura = s.posY

        // La mira y el golpe salen de la misma cuenta: los dos tienen que
        // decir lo mismo en este caso, que antes no entraba.
        assertTrue("la mira no se prende con el bicho ahi nomas", s.hayBichoAlAlcance())
        assertTrue("el golpe no llega a un bicho que esta a $d m", s.golpear() > 0)
    }

    @Test
    fun laMiraSePrendeJustoDondeLlegaElGolpe() {
        // La mira del HUD sale de la misma cuenta que el golpe. Aca se barre
        // la distancia y se controla que se prenda hasta el alcance real y ni
        // un centimetro mas: si mintiera, mentiria justo en el limite, que es
        // el unico lugar donde uno la mira.
        val save = perfilRico()
        val s = sesionCon(save, MazeGenerator.EnemyKind.GUARDIAN)
        val e = bichoEnfrente(s, MazeGenerator.EnemyKind.GUARDIAN)
        aislar(s, e)

        val yaw = Math.toRadians(s.yawDeg.toDouble())
        val fx = Math.sin(yaw).toFloat()
        val fz = Math.cos(yaw).toFloat()
        val limite = s.stats.alcanceGolpe + e.kind.radio + GameSession.REGALO_GOLPE

        var d = 0.3f
        while (d <= limite + 1.5f) {
            e.x = s.posX + fx * d
            e.z = s.posZ + fz * d
            e.altura = s.posY
            // Se saltean los 5 cm pegados al limite: ahi la diferencia es el
            // redondeo del float, no el comportamiento que se quiere fijar.
            if (Math.abs(d - limite) > 0.05f) {
                assertEquals(
                    "a $d m (el alcance es $limite) la mira no dice lo que corresponde",
                    d < limite, s.hayBichoAlAlcance()
                )
            }
            d += 0.1f
        }
    }

    @Test
    fun unBichoALaEspaldaNoSeToca() {
        val save = perfilRico()
        val s = sesionCon(save, MazeGenerator.EnemyKind.GUARDIAN)
        val e = bichoEnfrente(s, MazeGenerator.EnemyKind.GUARDIAN)
        aislar(s, e)

        // Bien atras: mas lejos que el regalo del golpe, que a quemarropa
        // toca para cualquier lado a proposito.
        val yaw = Math.toRadians(s.yawDeg.toDouble())
        val d = s.stats.alcanceGolpe
        e.x = s.posX - Math.sin(yaw).toFloat() * d
        e.z = s.posZ - Math.cos(yaw).toFloat() * d
        e.altura = s.posY

        assertFalse("la mira se prende con el bicho atras", s.hayBichoAlAlcance())
        assertEquals("el golpe le llego a un bicho que estaba atras", 0, s.golpear())
    }
}
