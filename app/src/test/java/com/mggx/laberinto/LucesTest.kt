package com.mggx.laberinto

import com.mggx.laberinto.gl.Shaders
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * La cueva ahora tiene luz propia: las antorchas, los cristales, los hongos y
 * la salida iluminan la roca de verdad, no solo brillan ellos.
 */
class LucesTest {

    private fun fuente(rel: String): String {
        val candidatos = listOf(
            File("src/main/java/com/mggx/laberinto/$rel"),
            File("app/src/main/java/com/mggx/laberinto/$rel")
        )
        return (candidatos.firstOrNull { it.exists() }
            ?: error("no se encontro $rel")).readText()
    }

    @Test
    fun elTopeDeLucesDeKotlinYElDelShaderSonElMismo() {
        // Si se desincronizan, glUniform4fv manda mas datos de los que el
        // uniforme aguanta y el driver lo descarta sin decir nada: la cueva se
        // apagaria sola y no habria ningun error para seguir.
        val enKotlin = com.mggx.laberinto.gl.CaveRenderer.MAX_LUCES
        val enShader = Regex("#define MAX_LUCES (\\d+)")
            .find(Shaders.WORLD_FS)?.groupValues?.get(1)?.toInt()
        assertEquals("el tope de luces del shader no coincide con el de Kotlin", enKotlin, enShader)

        val enProps = Regex("#define MAX_LUCES (\\d+)")
            .find(Shaders.PROP_FS)?.groupValues?.get(1)?.toInt()
        assertEquals("el shader de objetos usa otro tope", enKotlin, enProps)
    }

    @Test
    fun losDosShadersUsanLasLucesDeLaCueva() {
        for (nombre in listOf("WORLD_FS", "PROP_FS")) {
            val src = if (nombre == "WORLD_FS") Shaders.WORLD_FS else Shaders.PROP_FS
            assertTrue(
                "$nombre declara las luces pero no las usa",
                src.contains("lucesDeLaCueva(")
            )
        }
    }

    @Test
    fun elRendererArmaLasLucesAlPrepararElNivel() {
        // El armado tiene que colgar de prepareLevel: si no, al cambiar de
        // nivel quedarian las luces del nivel anterior flotando en el aire.
        val src = fuente("gl/CaveRenderer.kt")
        val bloque = src.substring(src.indexOf("private fun prepareLevel"))
        assertTrue(
            "prepareLevel no rearma las luces del nivel",
            bloque.take(400).contains("armarFaroles(")
        )
    }

    @Test
    fun todoNivelTieneFuentesDeLuzPropias() {
        // Si un nivel no tuviera ninguna, se veria igual de plano que antes.
        for (level in 1..40) {
            val bp = MazeGenerator.generate(level, level * 727L)
            val fuentes = bp.torches.size + bp.crystalClusters.size +
                bp.mushrooms.size + bp.carbide.size
            assertTrue(
                "el nivel $level no tiene ninguna luz propia",
                fuentes >= 8
            )
        }
    }
}
