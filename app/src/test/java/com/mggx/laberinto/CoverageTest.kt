package com.mggx.laberinto

import com.mggx.laberinto.game.EffectType
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import com.mggx.laberinto.ui.icons.IconId
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tests de cabos sueltos: que no quede ni un icono sin dibujar, ni un efecto
 * declarado que el juego no aplique, ni una mejora referida por un id que no exista.
 */
class CoverageTest {

    private fun fuente(rel: String): String {
        val candidatos = listOf(
            File("src/main/java/com/mggx/laberinto/$rel"),
            File("app/src/main/java/com/mggx/laberinto/$rel"),
            File("../app/src/main/java/com/mggx/laberinto/$rel")
        )
        val f = candidatos.firstOrNull { it.exists() }
            ?: error("no se encontro el archivo $rel (cwd=${File(".").absolutePath})")
        return f.readText()
    }

    @Test
    fun todosLosIconosEstanDibujados() {
        val dibujos = fuente("ui/icons/ItemIcons.kt") + fuente("ui/icons/UiIcons.kt")
        val faltantes = IconId.entries.filter { !dibujos.contains("IconId.${it.name} ->") }
        assertTrue("iconos declarados pero nunca dibujados: $faltantes", faltantes.isEmpty())
    }

    @Test
    fun noHayIconosDibujadosDeMas() {
        // Todo icono que se dibuja tiene que existir en el enum (esto lo garantiza
        // el compilador) y ademas tiene que usarse en algun lado del juego.
        val usos = buildString {
            append(fuente("game/ItemCatalog.kt"))
            File("src/main/java/com/mggx/laberinto/ui").walkTopDown()
                .plus(File("app/src/main/java/com/mggx/laberinto/ui").walkTopDown())
                .filter { it.isFile && it.extension == "kt" && !it.path.contains("icons") }
                .forEach { append(it.readText()) }
        }
        val sinUsar = IconId.entries.filter { !usos.contains("IconId.${it.name}") }
        assertTrue("iconos que no usa nadie: $sinUsar", sinUsar.isEmpty())
    }

    @Test
    fun todosLosEfectosDeConsumibleSeAplicanEnLaPartida() {
        val src = fuente("game/GameSession.kt")
        val bloque = src.substring(src.indexOf("private fun applyConsumable"))
        val tiposDeConsumible = ItemCatalog.ofKind(ItemKind.CONSUMIBLE).map { it.effect.type }.toSet()
        val faltantes = tiposDeConsumible.filter { !bloque.contains("EffectType.${it.name}") }
        assertTrue("consumibles cuyo efecto nunca se aplica: $faltantes", faltantes.isEmpty())
    }

    @Test
    fun todasLasMejorasSeLeenEnLasEstadisticas() {
        // La mayoria vive en PlayerStats; la de ranuras rapidas la lee el perfil.
        val src = fuente("game/PlayerStats.kt") + fuente("core/SaveData.kt")
        val faltantes = ItemCatalog.ofKind(ItemKind.MEJORA)
            .filter { !src.contains("\"${it.id}\"") }
            .map { it.id }
        assertTrue("mejoras compradas que no afectan nada: $faltantes", faltantes.isEmpty())
    }

    @Test
    fun todasLasReliquiasSeLeenEnLasEstadisticas() {
        val src = fuente("game/PlayerStats.kt")
        val faltantes = ItemCatalog.ofKind(ItemKind.RELIQUIA).filter { !src.contains("\"${it.id}\"") }
        assertTrue("reliquias equipables sin ningun efecto: $faltantes", faltantes.map { it.id }.isEmpty())
    }

    @Test
    fun losIdentificadoresQueUsaElCodigoExisten() {
        // Cualquier id de objeto escrito a mano en el codigo tiene que existir
        // en el catalogo, si no la mejora o reliquia no haria nada en silencio.
        val archivos = listOf(
            "game/PlayerStats.kt", "game/GameSession.kt", "core/SaveData.kt"
        )
        val idsValidos = ItemCatalog.all.map { it.id }.toSet()
        val patron = Regex("\"((?:up|rel|cos)_[a-z_]+)\"")
        val malos = mutableListOf<String>()
        for (a in archivos) {
            patron.findAll(fuente(a)).forEach { m ->
                val id = m.groupValues[1]
                if (id !in idsValidos) malos.add("$a -> $id")
            }
        }
        assertTrue("ids inexistentes usados en el codigo: $malos", malos.isEmpty())
    }

    @Test
    fun todosLosEfectosDeReliquiaSeConsultanEnLaPartida() {
        val session = fuente("game/GameSession.kt")
        val stats = fuente("game/PlayerStats.kt")
        // Cada campo publico de PlayerStats que viene de una reliquia tiene que
        // usarse en la partida o en el renderer.
        val renderer = fuente("gl/CaveRenderer.kt")
        val hud = File("src/main/java/com/mggx/laberinto/ui/screens/GameScreen.kt")
            .takeIf { it.exists() }?.readText()
            ?: File("app/src/main/java/com/mggx/laberinto/ui/screens/GameScreen.kt").readText()
        val todo = session + renderer + hud
        val campos = listOf(
            "criticalArmor", "ecoCritChance", "exitPingSeconds", "regenPerSecond",
            "threadMultiplier", "minAmbient", "startBurstSpeed", "consumableSaveChance",
            "vetagrisIncome", "heatImmune", "freeSonarSeconds", "trapSenseRange"
        )
        val sinUsar = campos.filter { !todo.contains(it) }
        assertTrue("efectos de reliquia calculados pero nunca usados: $sinUsar", sinUsar.isEmpty())
        assertTrue(stats.isNotEmpty())
    }

    @Test
    fun noQuedaNingunTodoNiCodigoSinTerminar() {
        val raiz = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull { it.exists() } ?: error("no se encontro el codigo fuente")
        val marcas = listOf("TODO(", "TODO:", "FIXME", "NotImplementedError", "XXX:")
        val encontrados = mutableListOf<String>()
        raiz.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { f ->
            val txt = f.readText()
            marcas.forEach { marca ->
                if (txt.contains(marca)) encontrados.add("${f.name}: $marca")
            }
        }
        assertTrue("hay codigo sin terminar: $encontrados", encontrados.isEmpty())
    }

    @Test
    fun todosLosSonidosDelJuegoTienenVoz() {
        val audio = fuente("core/CaveAudio.kt")
        val faltantes = com.mggx.laberinto.game.GameSession.Sfx.entries
            .filter { !audio.contains("Sfx.${it.name}") }
        assertTrue("sonidos sin sintetizar: $faltantes", faltantes.isEmpty())
    }

    @Test
    fun todosLosAjustesGuardadosSeUsanEnAlgunLado() {
        // Se excluye SaveData (donde se guardan) y SettingsScreen (donde se editan):
        // un ajuste tiene que HACER algo en el juego, no solo aparecer en el menu.
        val raiz = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull { it.exists() }!!
        val todo = raiz.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .filter { it.name != "SaveData.kt" && it.name != "SettingsScreen.kt" }
            .joinToString("\n") { it.readText() }
        val ajustes = listOf(
            "musicVolume", "sfxVolume", "masterVolume", "haptics", "invertY",
            "lookSensitivity", "gamepadSensitivity", "stickDeadzone", "leftHanded",
            "showMinimap", "minimapSize", "showArms", "headBob", "fovExtra",
            "quality", "targetFps", "showFps", "brightness", "fogIntensity",
            "torchFlicker", "autoRun", "joystickSize", "joystickOpacity",
            "buttonScale", "vibrateOnPickup", "subtitles", "compassAlwaysOn",
            "colorBlindMode", "uiScale", "renderScale", "tutorialDone"
        )
        val sinUsar = ajustes.filter { !todo.contains(it) }
        assertTrue("ajustes que se guardan pero no hacen nada: $sinUsar", sinUsar.isEmpty())
    }

    @Test
    fun todosLosEfectosDeclaradosEstanEnAlgunObjeto() {
        val usados = ItemCatalog.all.map { it.effect.type }.toSet()
        val sinObjeto = EffectType.entries.filter { it !in usados }
        assertTrue("efectos declarados sin ningun objeto: $sinObjeto", sinObjeto.isEmpty())
    }
}
