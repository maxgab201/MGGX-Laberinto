package com.mggx.laberinto

import com.mggx.laberinto.gl.ProcTextures
import com.mggx.laberinto.maze.CaveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.Deflater
import kotlin.math.sqrt

/**
 * Controla que las texturas de roca generadas por codigo tengan contenido de
 * verdad: contraste, relieve y variacion. Una textura plana o casi negra
 * compila igual pero deja la cueva pareciendo carton pintado.
 *
 * De paso deja un PNG de muestra en build/texturas para poder mirarlas.
 */
class TexturasTest {

    private fun canal(px: ByteArray, i: Int): Int = px[i].toInt() and 0xFF

    private fun estadisticas(px: ByteArray, size: Int, capa: Int, canalOffset: Int): Pair<Float, Float> {
        val base = capa * size * size * 4
        var suma = 0.0
        var suma2 = 0.0
        val n = size * size
        for (i in 0 until n) {
            val v = canal(px, base + i * 4 + canalOffset) / 255.0
            suma += v
            suma2 += v * v
        }
        val media = suma / n
        val varianza = (suma2 / n) - media * media
        return Pair(media.toFloat(), sqrt(varianza.coerceAtLeast(0.0)).toFloat())
    }

    @Test
    fun laRocaTieneContrasteYNoEsUnColorPlano() {
        for (theme in CaveTheme.entries) {
            val p = ProcTextures.generate(theme, 2)
            for (capa in intArrayOf(ProcTextures.LAYER_WALL, ProcTextures.LAYER_FLOOR, ProcTextures.LAYER_CEIL)) {
                val (media, desvio) = estadisticas(p.albedo, p.size, capa, 1)
                assertTrue(
                    "la capa $capa de ${theme.name} quedo demasiado oscura (media $media)",
                    media > 0.06f
                )
                assertTrue(
                    "la capa $capa de ${theme.name} quedo quemada (media $media)",
                    media < 0.88f
                )
                // Se mide el contraste RELATIVO al brillo medio: un ambiente
                // oscuro como el magma tiene poca variacion absoluta aunque la
                // roca este igual de trabajada que en uno claro.
                val contraste = desvio / media
                assertTrue(
                    "la capa $capa de ${theme.name} es casi un color plano " +
                        "(contraste relativo $contraste)",
                    contraste > 0.13f
                )
            }
        }
    }

    @Test
    fun elMapaDeNormalesTieneRelieve() {
        val p = ProcTextures.generate(CaveTheme.ENTRADA, 2)
        // El canal rojo guarda la componente X de la normal: si no varia,
        // la superficie es un plano perfecto y no hay relieve.
        val (media, desvio) = estadisticas(p.normal, p.size, ProcTextures.LAYER_WALL, 0)
        assertEquals("la normal no esta centrada en 0.5", 0.5f, media, 0.08f)
        assertTrue("la pared no tiene relieve (desvio $desvio)", desvio > 0.03f)
    }

    @Test
    fun lasVetasSonFinasYNoTapanLaRoca() {
        for (theme in CaveTheme.entries) {
            val p = ProcTextures.generate(theme, 2)
            val base = ProcTextures.LAYER_WALL * p.size * p.size * 4
            var conVeta = 0
            for (i in 0 until p.size * p.size) {
                if (canal(p.albedo, base + i * 4 + 3) > 40) conVeta++
            }
            val fraccion = conVeta.toFloat() / (p.size * p.size)
            assertTrue(
                "las vetas de ${theme.name} tapan el ${(fraccion * 100).toInt()}% de la pared",
                fraccion < 0.12f
            )
        }
    }

    @Test
    fun cadaAmbienteTieneSuPropioAspecto() {
        val firmas = CaveTheme.entries.map { theme ->
            val p = ProcTextures.generate(theme, 1)
            val (m, d) = estadisticas(p.albedo, p.size, ProcTextures.LAYER_WALL, 0)
            theme.name to "%.3f/%.3f".format(m, d)
        }
        val repetidos = firmas.groupBy { it.second }.filter { it.value.size > 1 }
        assertTrue("hay ambientes con la roca identica: $repetidos", repetidos.isEmpty())
    }

    /**
     * Salto medio entre dos filas (o columnas) de una capa. Se usa para
     * comparar la union de la baldosa contra el interior.
     */
    private fun salto(
        px: ByteArray, size: Int, capa: Int,
        filaA: Int, filaB: Int, porColumnas: Boolean
    ): Float {
        val base = capa * size * size * 4
        var suma = 0.0
        for (i in 0 until size) {
            val a = if (porColumnas) base + (i * size + filaA) * 4 else base + (filaA * size + i) * 4
            val b = if (porColumnas) base + (i * size + filaB) * 4 else base + (filaB * size + i) * 4
            for (c in 0 until 3) {
                suma += kotlin.math.abs(canal(px, a + c) - canal(px, b + c)) / 255.0
            }
        }
        return (suma / (size * 3)).toFloat()
    }

    /**
     * La textura se repite cada 3 metros de pared. Si el ruido no cierra en el
     * borde, aparece una linea marcada en cada union, y se ve como una reja a
     * lo largo de todo el pasillo.
     */
    @Test
    fun dondeHayHumedadLaParedSeVeDistintaQueDondeNoHay() {
        // El magma no tiene ni liquen ni chorreaduras (no crece nada ahi y el
        // agua no llega), y el bosque de esporas tiene las dos cosas al maximo.
        // Si las dos paredes salieran con el mismo relieve, la roca seria la
        // misma pintada de otro color y el bioma no significaria nada.
        fun rugosidad(tema: CaveTheme): Float {
            val p = ProcTextures.generate(tema, 1)
            val porCapa = p.size * p.size * 4
            // Desvio de la altura guardada en el alfa del mapa de normales.
            var suma = 0.0
            var suma2 = 0.0
            var n = 0
            var i = ProcTextures.LAYER_WALL * porCapa + 3
            while (i < (ProcTextures.LAYER_WALL + 1) * porCapa) {
                val v = (p.normal[i].toInt() and 0xFF) / 255.0
                suma += v; suma2 += v * v; n++
                i += 4
            }
            val media = suma / n
            return sqrt((suma2 / n - media * media)).toFloat()
        }
        val seco = rugosidad(CaveTheme.MAGMA)
        val humedo = rugosidad(CaveTheme.HONGOS)
        assertTrue("el bioma humedo no cambia el relieve de la pared", humedo != seco)
        assertTrue("la pared seca quedo sin relieve", seco > 0.05f)
        assertTrue("la pared humeda quedo sin relieve", humedo > 0.05f)
    }

    @Test
    fun elLiquenYLaHumedadSonCoherentesConCadaBioma() {
        // Los valores son una descripcion del lugar, no un numero decorativo:
        // si el magma tuviera verdin, la textura estaria contando una mentira
        // sobre donde estas parado.
        assertEquals("en el magma no puede crecer nada", 0f, CaveTheme.MAGMA.liquen, 0f)
        assertTrue(
            "las cisternas anegadas tendrian que ser lo mas humedo",
            CaveTheme.RUINAS.humedad >= 0.9f
        )
        assertTrue(
            "las galerias de musgo tendrian que ser lo mas cubierto de verdin",
            CaveTheme.MUSGO.liquen >= 0.9f
        )
        for (t in CaveTheme.entries) {
            assertTrue("${t.name} tiene un liquen fuera de rango", t.liquen in 0f..1f)
            assertTrue("${t.name} tiene una humedad fuera de rango", t.humedad in 0f..1f)
        }
    }

    @Test
    fun generarDaSiempreExactamenteLoMismo() {
        // Las cuatro capas se calculan en hilos distintos (ver
        // ProcTextures.generate). Eso es correcto solo si cada hilo escribe su
        // propio pedazo y ninguno lee el del otro: si se pisaran, el resultado
        // cambiaria de una corrida a la otra y la cueva se veria distinta cada
        // vez que bajas, o directamente con franjas de basura.
        //
        // Repetirlo varias veces es a proposito: una carrera entre hilos casi
        // nunca falla en el primer intento.
        val tema = com.mggx.laberinto.maze.CaveTheme.forLevel(9)
        val referencia = ProcTextures.generate(tema, 1)
        repeat(6) { intento ->
            val otra = ProcTextures.generate(tema, 1)
            assertEquals(referencia.size, otra.size)
            assertTrue(
                "el albedo salio distinto en el intento $intento: hay hilos pisandose",
                referencia.albedo.contentEquals(otra.albedo)
            )
            assertTrue(
                "el mapa de normales salio distinto en el intento $intento",
                referencia.normal.contentEquals(otra.normal)
            )
        }
    }

    @Test
    fun todasLasCapasSeLlenan() {
        // Si un hilo se colgara o escribiera en el rango equivocado, alguna
        // capa quedaria en ceros y en el juego se veria como una superficie
        // negra lisa. Un array de ceros no da ningun error.
        val p = ProcTextures.generate(com.mggx.laberinto.maze.CaveTheme.forLevel(3), 1)
        val porCapa = p.size * p.size * 4
        for (capa in 0 until ProcTextures.LAYERS) {
            var noCero = 0
            for (i in capa * porCapa until (capa + 1) * porCapa) {
                if (p.albedo[i].toInt() != 0) noCero++
            }
            assertTrue("la capa $capa quedo vacia", noCero > porCapa / 2)
        }
    }

    @Test
    fun laTexturaCierraSinCostura() {
        for (theme in CaveTheme.entries) {
            val p = ProcTextures.generate(theme, 2)
            for (capa in 0 until 3) {
                // Referencia: cuanto cambia de una fila a la siguiente adentro.
                var interior = 0f
                val muestras = 12
                for (k in 1..muestras) {
                    val f = k * p.size / (muestras + 2)
                    interior += salto(p.albedo, p.size, capa, f, f + 1, false)
                    interior += salto(p.albedo, p.size, capa, f, f + 1, true)
                }
                interior /= (muestras * 2)

                val costuraH = salto(p.albedo, p.size, capa, p.size - 1, 0, false)
                val costuraV = salto(p.albedo, p.size, capa, p.size - 1, 0, true)

                // Se tolera el doble del salto normal: la union nunca tiene que
                // ser un escalon visible respecto del resto de la textura.
                val tope = interior * 2.5f + 0.01f
                assertTrue(
                    "${theme.name} capa $capa: costura horizontal $costuraH contra " +
                        "un salto normal de $interior",
                    costuraH < tope
                )
                assertTrue(
                    "${theme.name} capa $capa: costura vertical $costuraV contra " +
                        "un salto normal de $interior",
                    costuraV < tope
                )
            }
        }
    }

    @Test
    fun dejaUnaMuestraParaMirar() {
        val dir = File("build/texturas").apply { mkdirs() }
        for (theme in listOf(CaveTheme.ENTRADA, CaveTheme.CRISTAL, CaveTheme.MAGMA)) {
            val p = ProcTextures.generate(theme, 2)
            val ancho = p.size * 3
            val rgb = ByteArray(ancho * p.size * 3)
            for (capa in 0 until 3) {
                val base = capa * p.size * p.size * 4
                for (y in 0 until p.size) for (x in 0 until p.size) {
                    val o = base + (y * p.size + x) * 4
                    val d = (y * ancho + capa * p.size + x) * 3
                    rgb[d] = p.albedo[o]
                    rgb[d + 1] = p.albedo[o + 1]
                    rgb[d + 2] = p.albedo[o + 2]
                }
            }
            File(dir, "${theme.name.lowercase()}.png").writeBytes(png(rgb, ancho, p.size))
        }
        assertTrue(File(dir, "entrada.png").exists())
    }

    // ------------------------------------------------------------------ PNG
    // El classpath de tests de Android no trae javax.imageio, asi que el PNG
    // se arma a mano. Son cuatro bloques con su CRC y los datos en zlib.

    private fun png(rgb: ByteArray, ancho: Int, alto: Int): ByteArray {
        val crudo = ByteArrayOutputStream()
        for (y in 0 until alto) {
            crudo.write(0)                                  // filtro "ninguno"
            crudo.write(rgb, y * ancho * 3, ancho * 3)
        }
        val salida = ByteArrayOutputStream()
        salida.write(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))

        val ihdr = ByteArrayOutputStream()
        ihdr.write(entero(ancho)); ihdr.write(entero(alto))
        ihdr.write(8); ihdr.write(2); ihdr.write(0); ihdr.write(0); ihdr.write(0)
        salida.write(bloque("IHDR", ihdr.toByteArray()))
        salida.write(bloque("IDAT", comprimir(crudo.toByteArray())))
        salida.write(bloque("IEND", ByteArray(0)))
        return salida.toByteArray()
    }

    private fun entero(v: Int) = byteArrayOf(
        (v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte()
    )

    private fun bloque(tipo: String, datos: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(entero(datos.size))
        val cuerpo = tipo.toByteArray(Charsets.US_ASCII) + datos
        out.write(cuerpo)
        val crc = CRC32().apply { update(cuerpo) }
        out.write(entero(crc.value.toInt()))
        return out.toByteArray()
    }

    private fun comprimir(datos: ByteArray): ByteArray {
        val d = Deflater()
        d.setInput(datos); d.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16384)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        d.end()
        return out.toByteArray()
    }
}
