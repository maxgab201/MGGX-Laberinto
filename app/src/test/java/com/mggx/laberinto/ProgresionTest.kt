package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.maze.AguaDeLaCueva
import com.mggx.laberinto.maze.CaveTheme
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Que bajar un nivel mas se NOTE.
 *
 * No alcanza con que el codigo tenga un parametro por nivel: lo que se
 * verifica aca es que el resultado cambie de verdad de arriba abajo, en todo
 * lo que hace a la sensacion de ir mas hondo. Un juego donde el nivel 40 se
 * juega igual que el 4 no tiene progresion, tiene un contador.
 */
class ProgresionTest {

    private fun perfil(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    @Test
    fun laCuevaCreceAlBajar() {
        var anterior = 0
        for (level in intArrayOf(1, 5, 10, 20, 35, 55)) {
            val (c, r) = MazeGenerator.cellsForLevel(level)
            val area = c * r
            assertTrue("el nivel $level no es mas grande que el anterior", area > anterior)
            anterior = area
        }
    }

    @Test
    fun elTamanoTieneTecho() {
        // Sin techo, el nivel 400 seria un laberinto que no entra en memoria y
        // que ademas nadie termina nunca.
        val (c1, r1) = MazeGenerator.cellsForLevel(200)
        val (c2, r2) = MazeGenerator.cellsForLevel(20000)
        assertEquals("el tamano no tiene techo", c1 * r1, c2 * r2)
    }

    @Test
    fun cadaTramoDeNivelesTieneSuBioma() {
        // Doce biomas repartidos en los primeros cincuenta y pico de niveles:
        // eso es lo que hace que bajar se sienta como un viaje y no como la
        // misma cueva repintada.
        val vistos = LinkedHashSet<CaveTheme>()
        for (level in 1..52) vistos.add(CaveTheme.forLevel(level))
        assertTrue("hay pocos biomas en el camino hasta el 52: ${vistos.size}", vistos.size >= 11)
        // Y ninguno aparece por un solo nivel: un bioma que ves una vez y no
        // volves a ver no llega a registrarse.
        for (t in vistos) {
            val cuantos = (1..52).count { CaveTheme.forLevel(it) == t }
            assertTrue("el bioma ${t.name} dura un solo nivel", cuantos >= 3)
        }
    }

    @Test
    fun despuesDelCincuentaYDosSiguenRotando() {
        // No puede quedarse clavado en el ultimo bioma para siempre.
        val vistos = HashSet<CaveTheme>()
        for (level in 53..90) vistos.add(CaveTheme.forLevel(level))
        assertTrue("despues del 52 deja de haber variedad", vistos.size >= 6)
    }

    @Test
    fun elVetagrisEsRaroYCaeEnLosNivelesRedondos() {
        // Es el bioma premio: si saliera en cualquier nivel dejaria de ser un
        // premio, y si no saliera nunca seria trabajo tirado.
        for (level in 55..120 step 5) {
            assertEquals(
                "el nivel $level tendria que ser Vetagris",
                CaveTheme.VETAGRIS, CaveTheme.forLevel(level)
            )
        }
        val antesDel53 = (1..52).count { CaveTheme.forLevel(it) == CaveTheme.VETAGRIS }
        assertEquals("el Vetagris no tendria que aparecer en la bajada inicial", 0, antesDel53)
    }

    @Test
    fun losPrimerosNivelesSonTranquilos() {
        // La curva de entrada: el 1 es el tutorial, y hasta el 2 no hay bichos
        // ni trampas. Alguien que abre el juego por primera vez tiene que poder
        // aprender a caminar sin que lo maten.
        assertNotNull("el nivel 1 tiene que ser el tutorial", GameSession(perfil(), 1, 1L).tutorial)
        for (level in 1..2) {
            val bp = MazeGenerator.generate(level, level * 7L)
            assertTrue("el nivel $level tiene bichos", bp.enemies.isEmpty())
            assertTrue("el nivel $level tiene trampas", bp.traps.isEmpty())
            assertEquals(
                "el nivel $level tiene agua",
                AguaDeLaCueva.SIN_AGUA, bp.maze.waterY, 0f
            )
        }
    }

    @Test
    fun loQueEsNuevoVaApareciendoDeAPoco() {
        // Cada cosa entra en su momento y no todas juntas: primero caminar,
        // despues las trampas, despues los bichos, despues el agua y el
        // relieve. Asi cada tanda de niveles ensena una cosa.
        fun primerNivelCon(predicado: (Int) -> Boolean): Int {
            for (level in 1..60) if (predicado(level)) return level
            return 999
        }
        val conTrampas = primerNivelCon { l ->
            (0L until 4L).any { MazeGenerator.generate(l, l * 31L + it).traps.isNotEmpty() }
        }
        val conBichos = primerNivelCon { l ->
            (0L until 4L).any { MazeGenerator.generate(l, l * 41L + it).enemies.isNotEmpty() }
        }
        val conAgua = primerNivelCon { l ->
            (0L until 6L).any {
                MazeGenerator.generate(l, l * 51L + it).maze.waterY != AguaDeLaCueva.SIN_AGUA
            }
        }
        assertTrue("las trampas aparecen demasiado pronto ($conTrampas)", conTrampas >= 3)
        assertTrue("los bichos aparecen demasiado pronto ($conBichos)", conBichos >= 3)
        assertTrue("el agua aparece demasiado pronto ($conAgua)", conAgua >= AguaDeLaCueva.NIVEL_DESDE)
        assertTrue("nunca aparecen trampas", conTrampas < 20)
        assertTrue("nunca aparecen bichos", conBichos < 20)
        assertTrue("nunca aparece agua", conAgua < 30)
    }

    @Test
    fun masAbajoHayMasPeligro() {
        fun peligro(desde: Int, hasta: Int): Int {
            var n = 0
            for (level in desde..hasta) {
                val bp = MazeGenerator.generate(level, level * 97L)
                n += bp.enemies.size + bp.traps.size
            }
            return n
        }
        val arriba = peligro(5, 15)
        val abajo = peligro(40, 50)
        assertTrue("bajar no trae mas peligro ($arriba -> $abajo)", abajo > arriba * 2)
    }

    @Test
    fun elCaminoSeHaceMasLargo() {
        var anterior = 0
        for (level in intArrayOf(2, 10, 25, 45)) {
            var suma = 0
            for (s in 0L until 3L) suma += MazeGenerator.generate(level, level * 13L + s).maze.solutionLength
            assertTrue("el camino del nivel $level no es mas largo", suma > anterior)
            anterior = suma
        }
    }

    @Test
    fun cadaNivelSiempreSePuedeTerminar() {
        // La promesa que no se rompe nunca, con todo lo nuevo encima (relieve,
        // agua, tramo bajo del tutorial): si un nivel no tiene salida, el juego
        // se termina ahi para ese jugador.
        for (level in 1..70) {
            val bp = MazeGenerator.generate(level, level * 613L)
            assertTrue("el nivel $level no tiene solucion", bp.maze.isSolvable())
            assertTrue(
                "el nivel $level quedo intransitable",
                com.mggx.laberinto.maze.ReliefGenerator.esTransitable(bp.maze)
            )
        }
    }

    @Test
    fun laEtiquetaDeDificultadAcompana() {
        val vistas = LinkedHashSet<String>()
        for (level in 1..60) vistas.add(MazeGenerator.difficultyLabel(level))
        assertTrue("la dificultad muestra siempre lo mismo", vistas.size >= 4)
    }
}
