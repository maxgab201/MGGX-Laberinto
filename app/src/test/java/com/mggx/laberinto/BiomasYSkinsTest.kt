package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.EffectType
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import com.mggx.laberinto.game.PlayerStats
import com.mggx.laberinto.maze.Biome
import com.mggx.laberinto.maze.CaveTheme
import com.mggx.laberinto.maze.MazeGenerator
import com.mggx.laberinto.maze.Patron
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * No todo es cueva: hay mina, ruinas, bosque de esporas y templo. Y el jugador
 * puede cambiar de piel y de traje.
 */
class BiomasYSkinsTest {

    // -------------------------------------------------------------- biomas

    @Test
    fun todosLosAmbientesSeAlcanzanJugando() {
        val vistos = HashSet<CaveTheme>()
        for (level in 1..400) vistos.add(CaveTheme.forLevel(level))
        val faltan = CaveTheme.entries.filter { it !in vistos }
        assertTrue("ambientes a los que no se llega nunca: $faltan", faltan.isEmpty())
    }

    @Test
    fun hayAmbientesQueNoSonCueva() {
        val noCueva = CaveTheme.entries.filter { it.biome != Biome.CUEVA }
        assertTrue("todo sigue siendo cueva", noCueva.size >= 4)
        val biomas = noCueva.map { it.biome }.toSet()
        assertTrue("faltan clases de lugar: $biomas", biomas.size >= 4)
        // Y se llega a todos antes del nivel 50, no en el infinito.
        for (b in Biome.entries) {
            val alcanzable = (1..50).any { CaveTheme.forLevel(it).biome == b }
            assertTrue("al bioma $b no se llega en los primeros 50 niveles", alcanzable)
        }
    }

    @Test
    fun cadaAmbienteTieneNombreYAspectoPropio() {
        val nombres = CaveTheme.entries.map { it.displayName }
        assertEquals("nombres de ambiente repetidos", nombres.size, nombres.toSet().size)
        val descripciones = CaveTheme.entries.map { it.description }
        assertEquals("descripciones repetidas", descripciones.size, descripciones.toSet().size)
        val semillas = CaveTheme.entries.map { it.textureSeed }
        assertEquals("dos ambientes comparten la semilla de textura", semillas.size, semillas.toSet().size)
    }

    @Test
    fun lasParedesLabradasUsanSuPropioPatron() {
        assertEquals(Patron.MADERA, CaveTheme.MINA.patron)
        assertEquals(Patron.SILLAR, CaveTheme.RUINAS.patron)
        assertEquals(Patron.SILLAR, CaveTheme.TEMPLO.patron)
        assertEquals(Patron.ORGANICO, CaveTheme.HONGOS.patron)
        // Las cuevas de verdad siguen siendo roca.
        for (t in CaveTheme.entries.filter { it.biome == Biome.CUEVA }) {
            assertEquals("${t.name} dejo de ser roca", Patron.ROCA, t.patron)
        }
    }

    @Test
    fun cadaBiomaSePueblaDistinto() {
        // La mina esta llena de entibado y casi no tiene estalagmitas; el
        // bosque de esporas al reves. Si los dos generaran lo mismo, el bioma
        // seria solo un cambio de color.
        fun conteo(theme: CaveTheme): Pair<Int, Int> {
            var vigas = 0
            var hongos = 0
            var n = 0
            for (level in 1..80) {
                if (CaveTheme.forLevel(level) != theme) continue
                val bp = MazeGenerator.generate(level, level * 131L)
                vigas += bp.beams.size
                hongos += bp.mushrooms.size
                n++
            }
            return if (n == 0) 0 to 0 else (vigas / n) to (hongos / n)
        }
        val (vigasMina, _) = conteo(CaveTheme.MINA)
        val (vigasHongos, hongosHongos) = conteo(CaveTheme.HONGOS)
        assertTrue("la mina no tiene mas entibado que el bosque", vigasMina > vigasHongos)
        assertTrue("el bosque de esporas no tiene hongos de mas", hongosHongos > 0)
    }

    @Test
    fun losNivelesDeCualquierBiomaSiguenSiendoPasables() {
        // Cambiar el bioma no puede romper la garantia de siempre.
        for (level in intArrayOf(10, 12, 20, 22, 30, 32, 40, 42, 60, 100)) {
            val bp = MazeGenerator.generate(level, level * 55L)
            assertTrue(
                "el nivel $level (${bp.theme.displayName}) no se puede terminar",
                bp.maze.solutionLength > 0
            )
        }
    }

    // --------------------------------------------------------------- skins

    private fun skins() = ItemCatalog.ofKind(ItemKind.COSMETICO)
        .filter { it.effect.type == EffectType.COS_PIEL }

    @Test
    fun haySeisSkinsYTodasSonDistintas() {
        val s = skins()
        assertTrue("pocas skins: ${s.size}", s.size >= 6)
        val colores = s.map { it.effect.color to it.effect.color2 }
        assertEquals("hay skins con los mismos colores", colores.size, colores.toSet().size)
        val estilos = s.map { it.effect.magnitude }
        assertEquals("hay skins con el mismo estilo", estilos.size, estilos.toSet().size)
        for (sk in s) {
            assertNotEquals("la skin ${sk.id} no tiene color de piel", 0L, sk.effect.color)
            assertNotEquals("la skin ${sk.id} no tiene color de traje", 0L, sk.effect.color2)
        }
    }

    @Test
    fun laSkinDeArranqueEsGratisYVienePuesta() {
        val save = SaveData.fromStore(SaveData.memoryStore())
        assertEquals("skin_minero", save.cosmeticSkin)
        assertTrue(save.isOwned("skin_minero"))
        assertEquals(0, ItemCatalog.require("skin_minero").basePrice)
    }

    @Test
    fun cambiarDeSkinCambiaLoQueSeVe() {
        val save = SaveData.fromStore(SaveData.memoryStore())
        repeat(60) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
        repeat(200) { save.addVetagris(1) }
        repeat(100) { save.addEcos(500) }

        val antes = PlayerStats(save)
        for (sk in skins().filter { it.id != "skin_minero" }) {
            assertEquals(
                "no se pudo comprar ${sk.id}",
                SaveData.BuyResult.OK, save.buy(sk.id)
            )
            assertEquals("comprar la skin tiene que equiparla", sk.id, save.cosmeticSkin)
            val ahora = PlayerStats(save)
            assertEquals(sk.effect.color, ahora.skinTint)
            assertEquals(sk.effect.color2, ahora.suitTint)
            assertNotEquals(
                "la skin ${sk.id} se ve igual que la de arranque",
                antes.skinTint to antes.suitTint, ahora.skinTint to ahora.suitTint
            )
        }
    }

    @Test
    fun laSkinYLosGuantesSonIndependientes() {
        val save = SaveData.fromStore(SaveData.memoryStore())
        repeat(60) { save.onLevelCompleted(save.maxLevel, 1000, 10) }
        repeat(100) { save.addEcos(500) }
        save.buy("skin_veterano")
        save.buy("cos_guantes_malla")
        val stats = PlayerStats(save)
        assertEquals("skin_veterano", save.cosmeticSkin)
        assertEquals("cos_guantes_malla", save.cosmeticGloves)
        assertTrue("el guante tiene que seguir teniendo su estilo", stats.gloveStyle > 0)
        assertEquals(1, stats.skinStyle)
    }
}
