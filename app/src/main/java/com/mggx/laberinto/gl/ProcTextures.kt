package com.mggx.laberinto.gl

import android.opengl.GLES30
import com.mggx.laberinto.maze.CaveTheme
import com.mggx.laberinto.maze.Patron
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Texturas de cueva generadas por codigo. No hay ni un solo archivo de imagen:
 * la roca, el suelo, el techo y las vetas salen de ruido fractal calculado al vuelo.
 * Se generan 4 capas en un GL_TEXTURE_2D_ARRAY (albedo con mascara de veta en alfa,
 * mas su mapa de normales derivado del mismo campo de altura).
 */
object ProcTextures {

    const val LAYER_WALL = 0
    const val LAYER_FLOOR = 1
    const val LAYER_CEIL = 2
    const val LAYER_VEIN = 3
    const val LAYERS = 4

    /** Capas de sedimento por baldosa. Entero: si no, la banda se corta. */
    private const val ESTRATOS_POR_BALDOSA = 4.0

    // -------------------------------------------------------------- ruido

    private fun hash2(x: Int, y: Int, seed: Int): Float {
        var h = x * 374761393 + y * 668265263 + seed * 1274126177
        h = (h xor (h shr 13)) * 1274126177
        h = h xor (h shr 16)
        return (h and 0x7FFFFFFF) / 2147483647.0f
    }

    private fun smooth(t: Float) = t * t * (3f - 2f * t)

    /** Ruido de valor con repeticion exacta en [period] para que la textura sea continua. */
    private fun valueNoise(x: Float, y: Float, period: Int, seed: Int): Float {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        val xf = x - xi
        val yf = y - yi
        fun w(a: Int) = ((a % period) + period) % period
        val x0 = w(xi); val x1 = w(xi + 1)
        val y0 = w(yi); val y1 = w(yi + 1)
        val v00 = hash2(x0, y0, seed)
        val v10 = hash2(x1, y0, seed)
        val v01 = hash2(x0, y1, seed)
        val v11 = hash2(x1, y1, seed)
        val sx = smooth(xf)
        val sy = smooth(yf)
        val a = v00 + (v10 - v00) * sx
        val b = v01 + (v11 - v01) * sx
        return a + (b - a) * sy
    }

    private fun fbm(x: Float, y: Float, octaves: Int, basePeriod: Int, seed: Int): Float {
        var sum = 0f
        var amp = 0.5f
        var norm = 0f
        var freq = 1
        for (o in 0 until octaves) {
            sum += valueNoise(x * freq, y * freq, basePeriod * freq, seed + o * 101) * amp
            norm += amp
            amp *= 0.5f
            freq *= 2
        }
        return sum / norm
    }

    /** Ruido celular tipo Worley: da el aspecto de piedra picada. */
    private fun worley(x: Float, y: Float, period: Int, seed: Int): Float {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        var best = 10f
        for (dy in -1..1) for (dx in -1..1) {
            val cx = xi + dx
            val cy = yi + dy
            fun w(a: Int) = ((a % period) + period) % period
            val px = cx + hash2(w(cx), w(cy), seed)
            val py = cy + hash2(w(cx), w(cy), seed + 7717)
            val d = sqrt((px - x) * (px - x) + (py - y) * (py - y))
            if (d < best) best = d
        }
        return min(1f, best)
    }

    // ----------------------------------------------------------- campos

    /** Altura 0..1 de cada capa. De aca sale tambien el mapa de normales. */
    /**
     * Distancia entre la celda de Worley mas cercana y la segunda. Cerca de
     * cero justo en el borde entre dos celdas, asi que sirve para dibujar las
     * juntas entre placas de roca.
     */
    private fun worleyEdge(x: Float, y: Float, period: Int, seed: Int): Float {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        var f1 = 10f
        var f2 = 10f
        for (dy in -1..1) for (dx in -1..1) {
            val cx = xi + dx
            val cy = yi + dy
            fun w(a: Int) = ((a % period) + period) % period
            val px = cx + hash2(w(cx), w(cy), seed)
            val py = cy + hash2(w(cx), w(cy), seed + 7717)
            val d = sqrt((px - x) * (px - x) + (py - y) * (py - y))
            if (d < f1) { f2 = f1; f1 = d } else if (d < f2) f2 = d
        }
        return (f2 - f1).coerceIn(0f, 1f)
    }

    /**
     * Periodo entero derivado del base.
     *
     * La textura solo cierra sin costura si la ESCALA de la coordenada y el
     * periodo de repeticion son EL MISMO ENTERO. Escribir `u * per * 1.7f` con
     * periodo `(per * 1.7f).toInt()` deja los dos desfasados y aparece una
     * linea marcada en el borde de cada baldosa, o sea cada 3 metros de pared.
     */
    private fun periodo(per: Int, factor: Float): Int =
        Math.round(per * factor).coerceAtLeast(2)

    private fun smoothstep(borde0: Float, borde1: Float, x: Float): Float {
        val t = ((x - borde0) / (borde1 - borde0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Estira el rango alrededor del medio: sube el contraste sin desplazar. */
    private fun contraste(h: Float, cuanto: Float): Float =
        (0.5f + (h - 0.5f) * cuanto).coerceIn(0f, 1f)

    /**
     * Grietas: ruido "ridged" recortado en la cresta, que deja fisuras finas y
     * ramificadas en vez de manchones.
     */
    private fun crackMask(u: Float, v: Float, per: Int, seed: Int): Float {
        val p = periodo(per, 0.85f)
        val n = fbm(u * p, v * p, 3, p, seed + 907)
        val ridged = 1f - abs(n * 2f - 1f)
        return ((ridged - 0.90f) / 0.10f).coerceIn(0f, 1f)
    }

    /**
     * Pared de sillares: bloques rectangulares trabados, con la junta de
     * mortero hundida y cada bloque con su propio desgaste.
     *
     * Para que cierre sin costura el numero de FILAS tiene que ser par: si no,
     * la traba de media pieza no coincide al dar la vuelta a la baldosa.
     */
    private fun sillar(u: Float, v: Float, per: Int, seed: Int): Float {
        var filas = periodo(per, 0.8f)
        if (filas % 2 != 0) filas++
        val cols = periodo(per, 0.45f)
        val fy = v * filas
        val fila = floor(fy).toInt()
        val traba = if (((fila % 2) + 2) % 2 == 0) 0f else 0.5f
        val fx = u * cols + traba
        val dx = fx - floor(fx)
        val dy = fy - floor(fy)
        val borde = min(min(dx, 1f - dx), min(dy, 1f - dy))
        val mortero = 1f - smoothstep(0.028f, 0.105f, borde)

        val cx = ((floor(fx).toInt() % cols) + cols) % cols
        val cy = ((fila % filas) + filas) % filas
        val pieza = hash2(cx, cy, seed + 313)
        val desgaste = fbm(u * per, v * per, 4, per, seed + 41)
        var h = 0.58f + pieza * 0.17f + desgaste * 0.25f
        h -= mortero * 0.44f
        h -= crackMask(u, v, per, seed + 88) * 0.16f
        return h.coerceIn(0f, 1f)
    }

    /** Entibado de madera: tablas horizontales con veta y juntas marcadas. */
    private fun madera(u: Float, v: Float, per: Int, seed: Int): Float {
        val tablas = periodo(per, 0.55f)
        val fy = v * tablas
        val idx = floor(fy).toInt()
        val dy = fy - floor(fy)
        val junta = 1f - smoothstep(0f, 0.075f, min(dy, 1f - dy))

        // La veta corre a lo largo de la tabla; tiene que dar un numero entero
        // de ciclos por baldosa o se corta en la union.
        val ciclos = periodo(per, 2.4f)
        val onda = 0.5f + 0.5f * kotlin.math.sin(
            u * ciclos * 2.0 * Math.PI +
                fbm(u * per, v * per, 3, per, seed + idx * 17) * 9.0
        ).toFloat()
        val nudo = 1f - worley(u * periodo(per, 1.1f), v * periodo(per, 1.1f), periodo(per, 1.1f), seed + 611)
        // Cada tabla arranca de un tono propio: es lo que hace que un entibado
        // no se lea como una pared lisa.
        val tono = hash2(0, ((idx % tablas) + tablas) % tablas, seed + 733)
        var h = 0.34f + tono * 0.26f + onda * 0.30f + nudo * 0.20f
        h -= junta * 0.52f
        return contraste(h.coerceIn(0f, 1f), 1.30f)
    }

    /** Pared viva: bultos redondeados y poros, sin ninguna arista recta. */
    private fun organico(u: Float, v: Float, per: Int, seed: Int): Float {
        val pBultos = periodo(per, 0.55f)
        val bultos = 1f - worley(u * pBultos, v * pBultos, pBultos, seed + 55)
        val pPoros = periodo(per, 1.8f)
        val poros = worley(u * pPoros, v * pPoros, pPoros, seed + 91)
        val suave = fbm(u * periodo(per, 0.9f), v * periodo(per, 0.9f), 4, periodo(per, 0.9f), seed + 7)
        // Los poros se hunden de verdad: si solo se suman, la pared queda plana.
        var h = bultos * 0.52f + suave * 0.28f + 0.20f
        h -= smoothstep(0.62f, 0.95f, poros) * 0.34f
        return contraste(h.coerceIn(0f, 1f), 1.25f)
    }

    /**
     * Chorreadura de agua: regueros verticales por la pared.
     *
     * Es la unica marca de la roca que tiene una direccion OBLIGADA, porque la
     * hace la gravedad. Una pared con chorreadura horizontal se lee mal aunque
     * nadie sepa decir por que.
     *
     * Se consigue estirando el ruido en V (la coordenada vertical): el mismo
     * campo, muy comprimido en X y muy estirado en Y, da rayas finas que caen.
     */
    private fun chorreado(u: Float, v: Float, per: Int, seed: Int): Float {
        val pAncho = periodo(per, 2.2f)
        val pLargo = periodo(per, 0.35f)
        // El ruido se muestrea con mucha frecuencia horizontal y poca vertical:
        // eso es lo que alarga las manchas para abajo.
        val n = fbm(u * pAncho, v * pLargo, 3, pAncho, seed + 1201)
        // Umbral alto: pocos regueros y separados, no una pared entera mojada.
        val reguero = ((n - 0.56f) / 0.20f).coerceIn(0f, 1f)
        // Se desvanece hacia abajo, como el agua que se va secando al caer.
        val desvanece = smoothstep(0f, 0.45f, v)
        return reguero * reguero * (0.35f + 0.65f * desvanece)
    }

    /**
     * Liquen y verdin: manchones esponjosos que crecen en las juntas.
     *
     * Crece SOLO donde la roca esta hundida ([altura] baja): en las grietas y
     * las juntas, que es donde se junta la humedad. Repartido al azar por toda
     * la pared se veria como pintura salpicada, y es justo lo que no es.
     */
    private fun liquen(u: Float, v: Float, per: Int, seed: Int, altura: Float): Float {
        val pMancha = periodo(per, 0.7f)
        val mancha = fbm(u * pMancha, v * pMancha, 4, pMancha, seed + 1307)
        val pGrumo = periodo(per, 2.6f)
        val grumo = fbm(u * pGrumo, v * pGrumo, 2, pGrumo, seed + 1409)
        // Donde hay manchon Y la roca esta hundida.
        val donde = smoothstep(0.50f, 0.78f, mancha) * (1f - smoothstep(0.30f, 0.68f, altura))
        return (donde * (0.55f + 0.45f * grumo)).coerceIn(0f, 1f)
    }

    private fun height(
        layer: Int, u: Float, v: Float, per: Int, seed: Int, rough: Float,
        patron: Patron = Patron.ROCA
    ): Float {
        // Las paredes labradas tienen su propio relieve; el piso, el techo y las
        // vetas siguen siendo roca en todos los biomas.
        if (layer == LAYER_WALL && patron != Patron.ROCA) {
            return when (patron) {
                Patron.SILLAR -> sillar(u, v, per, seed)
                Patron.MADERA -> madera(u, v, per, seed)
                else -> organico(u, v, per, seed)
            }
        }
        return when (layer) {
            LAYER_WALL -> {
                val base = fbm(u * per, v * per, 5, per, seed)
                // Bloques grandes de roca y, encima, grano mas chico.
                val pPlacas = periodo(per, 0.5f)
                val cells = 1f - worley(u * pPlacas, v * pPlacas, pPlacas, seed + 31)
                val pGrano = periodo(per, 1.7f)
                val grain = 1f - worley(u * pGrano, v * pGrano, pGrano, seed + 61)

                // Estratos de sedimento: bandas horizontales onduladas, con la
                // curva suavizada para que se lean como capas y no como olas.
                // Tienen que ser un numero ENTERO de ciclos por baldosa, si no
                // la banda queda cortada justo en la union.
                val onda = 0.5f + 0.5f * kotlin.math.sin(
                    v * ESTRATOS_POR_BALDOSA * 2.0 * Math.PI +
                        fbm(u * 3f, v * 3f, 2, 3, seed + 13) * 11.0
                ).toFloat()
                val strat = smoothstep(0.18f, 0.82f, onda)

                var h = base * 0.36f + cells * 0.20f + grain * 0.14f + strat * 0.30f
                // Junta entre placas: una linea hundida justo en el borde de
                // cada celda, que es lo que le da canto a la piedra.
                val junta = 1f - smoothstep(
                    0f, 0.062f, worleyEdge(u * pPlacas, v * pPlacas, pPlacas, seed + 31)
                )
                h -= junta * 0.24f
                // Grietas: ruido "ridged" muy fino que hunde la superficie.
                h -= crackMask(u, v, per, seed) * 0.30f
                contraste(h.coerceIn(0f, 1f), 0.90f + rough * 0.80f)
            }
            LAYER_FLOOR -> {
                val pBase = periodo(per, 1.2f)
                val base = fbm(u * pBase, v * pBase, 4, pBase, seed + 5)
                // Dos tamanos de canto rodado: piedras grandes y ripio.
                val pebbles = 1f - worley(u * per, v * per, per, seed + 77)
                val pRipio = periodo(per, 2.3f)
                val ripio = 1f - worley(u * pRipio, v * pRipio, pRipio, seed + 131)
                var h = base * 0.46f + pebbles * 0.34f + ripio * 0.20f
                h -= crackMask(u, v, per, seed + 400) * 0.18f
                contraste(h.coerceIn(0f, 1f), 1.20f)
            }
            LAYER_CEIL -> {
                val pBase = periodo(per, 0.8f)
                val base = fbm(u * pBase, v * pBase, 5, pBase, seed + 11)
                val pGotas = periodo(per, 0.7f)
                val drips = worley(u * pGotas, v * pGotas, pGotas, seed + 143)
                contraste((base * 0.7f + (1f - drips) * 0.3f).coerceIn(0f, 1f), 1.15f)
            }
            else -> {
                val pVeta = periodo(per, 1.6f)
                fbm(u * pVeta, v * pVeta, 4, pVeta, seed + 23).coerceIn(0f, 1f)
            }
        }
    }

    /** Mascara de veta de mineral: filamentos finos siguiendo el ruido. */
    private fun veinMask(u: Float, v: Float, per: Int, seed: Int): Float {
        val p = periodo(per, 0.6f)
        val n = fbm(u * p, v * p, 4, p, seed + 900)
        val ridged = 1f - abs(n * 2f - 1f)
        // Umbral alto y curva cubica: quedan hilos finos y separados, no una
        // telarana que tapa toda la roca.
        val m = ((ridged - 0.945f) / 0.055f).coerceIn(0f, 1f)
        return m * m * m
    }

    // ------------------------------------------------------------ subida

    class Result(val albedoTex: Int, val normalTex: Int, val size: Int)

    /** Pixeles ya calculados, listos para subir. Sin nada de OpenGL adentro. */
    class Pixels(val size: Int, val albedo: ByteArray, val normal: ByteArray)

    /** Genera y sube las dos texturas array. Se llama en el hilo de GL. */
    fun build(theme: CaveTheme, quality: Int): Result = upload(generate(theme, quality))

    /**
     * Calcula los pixeles de las dos texturas. Es CPU pura, sin OpenGL: se
     * puede correr fuera del hilo de GL y se puede mirar en un test.
     */
    fun generate(theme: CaveTheme, quality: Int): Pixels {
        // Mas pixeles por baldosa en las calidades altas: el detalle chico (las
        // juntas entre placas, las grietas, la veta de la madera) es lo primero
        // que se pierde cuando la baldosa es corta, y es justo lo que hace que
        // la piedra se vea piedra de cerca. Las calidades bajas no se tocan:
        // ahi el problema es el telefono, no el detalle.
        val size = when (quality) {
            0 -> 96
            1 -> 160
            2 -> 320
            else -> 448
        }
        // OJO: [per] no se toca a la ligera. Cambiar el detalle es subir `size`;
        // `per` cambia los periodos de TODOS los patrones a la vez, y no todos
        // cierran igual de bien con cualquier valor. Probado: pasarlo de 12 a
        // 14 abre una costura visible en el entibado de la Mina, que
        // TexturasTest agarra. Si alguna vez se cambia, tiene que ser mirando
        // ese test, no de memoria.
        val per = when (quality) {
            0 -> 6
            1 -> 8
            2 -> 12
            else -> 16
        }
        val seed = theme.textureSeed * 7919

        val albedo = ByteArray(size * size * 4 * LAYERS)
        val normal = ByteArray(size * size * 4 * LAYERS)

        // Las cuatro capas se calculan EN PARALELO.
        //
        // Esto no es micro-optimizacion: medido en JVM de escritorio, generar
        // las texturas tardaba 358 ms en calidad alta y 722 ms en ultra, y en
        // un telefono es varias veces eso. Peor todavia, corre en el HILO DE
        // OPENGL al preparar el nivel, asi que ese tiempo es la pantalla
        // congelada al bajar a una cueva de bioma nuevo.
        //
        // Las capas son independientes entre si: cada una escribe su propio
        // pedazo de los arrays y no lee el de las otras, asi que repartirlas no
        // necesita ningun candado y el resultado es byte por byte el mismo que
        // secuencial (lo verifica TexturasTest).
        val hilos = ArrayList<Thread>(LAYERS)
        for (layer in 0 until LAYERS) {
            hilos.add(
                Thread {
                    capa(layer, size, per, seed, theme, albedo, normal)
                }.apply { start() }
            )
        }
        for (t in hilos) t.join()

        return Pixels(size, albedo, normal)
    }

    /**
     * Calcula una capa entera (campo de altura, albedo y mapa de normales) y la
     * escribe en su pedazo de [albedo] y [normal].
     *
     * Los indices se calculan a partir de [layer] en vez de llevar un contador
     * corrido: es lo que permite que cuatro hilos escriban a la vez sin
     * pisarse, porque cada uno toca un rango distinto y ninguno lee el del otro.
     */
    private fun capa(
        layer: Int, size: Int, per: Int, seed: Int, theme: CaveTheme,
        albedo: ByteArray, normal: ByteArray
    ) {
        val base = layer * size * size * 4
        val h = FloatArray(size * size)

        // La pared es la unica capa donde crece liquen y chorrea el agua: en el
        // piso el liquen lo pisarias y en el techo el agua no chorrea, gotea.
        val conVida = layer == LAYER_WALL
        val fuerzaLiquen = if (conVida) theme.liquen else 0f
        val fuerzaHumedad = if (conVida) theme.humedad else 0f
        // El liquen y el chorreado por pixel, guardados aparte: se calculan una
        // vez y los usan tanto la altura como el color. Volver a calcularlos en
        // el paso del albedo seria hacer el doble de ruido para lo mismo.
        val liq = if (fuerzaLiquen > 0f) FloatArray(size * size) else null
        val moj = if (fuerzaHumedad > 0f) FloatArray(size * size) else null

        // 1) campo de altura de la capa
        for (y in 0 until size) {
            val v = y.toFloat() / size
            for (x in 0 until size) {
                val u = x.toFloat() / size
                var hv = height(layer, u, v, per, seed, theme.roughness, theme.patron)

                if (moj != null) {
                    // El agua que chorrea PULE la roca: la deja mas lisa y mas
                    // baja. Eso hace dos cosas gratis en el shader, que ya lee
                    // la altura para decidir la rugosidad: la zona mojada se ve
                    // mas oscura y ademas devuelve mas brillo.
                    val c = chorreado(u, v, per, seed) * fuerzaHumedad
                    moj[y * size + x] = c
                    hv -= c * 0.16f
                }
                if (liq != null) {
                    // El liquen es esponjoso: SUMA altura. Si solo cambiara el
                    // color quedaria como una mancha pintada sobre piedra lisa,
                    // que es exactamente lo que no es.
                    val l = liquen(u, v, per, seed, hv) * fuerzaLiquen
                    liq[y * size + x] = l
                    hv += l * 0.13f
                }
                h[y * size + x] = hv.coerceIn(0f, 1f)
            }
        }

        // 2) albedo tenido por el tema
        val (cr, cg, cb) = when (layer) {
            LAYER_WALL -> Triple(theme.rockR, theme.rockG, theme.rockB)
            LAYER_FLOOR -> Triple(theme.floorR, theme.floorG, theme.floorB)
            LAYER_CEIL -> Triple(theme.rockR * 0.78f, theme.rockG * 0.78f, theme.rockB * 0.8f)
            else -> Triple(theme.veinR * 0.5f, theme.veinG * 0.5f, theme.veinB * 0.5f)
        }
        var ai = base
        for (y in 0 until size) {
            val v = y.toFloat() / size
            for (x in 0 until size) {
                val u = x.toFloat() / size
                val hv = h[y * size + x]
                // Variacion de tono ligada a la altura: hondo = mas oscuro
                val shade = 0.46f + hv * 0.92f
                val grain = (hash2(x, y, seed + 3) - 0.5f) * 0.06f
                val vm = if (layer == LAYER_VEIN) 0.85f else veinMask(u, v, per, seed)

                // Manchones grandes de tono, del tamano de varias baldosas
                // juntas. La roca de verdad no es de un solo color parejo:
                // sin esto, una pared larga se lee como una unica lamina
                // repetida, que es lo que hacia que la cueva se viera
                // "de plastico" aunque el relieve estuviera bien.
                val mancha = fbm(u * 2f, v * 2f, 3, 2, seed + 97)
                val tinte = 0.84f + mancha * 0.34f
                // Ademas de aclarar y oscurecer, los manchones tiran un
                // poco hacia el color de la veta del bioma: es lo que le da
                // aire de mineral y no de cemento pintado.
                val hacia = ((mancha - 0.5f) * 0.22f).coerceIn(0f, 0.22f)

                var r = (cr * (1f - hacia) + theme.veinR * hacia) * shade * tinte + grain
                var g = (cg * (1f - hacia) + theme.veinG * hacia) * shade * tinte + grain
                var b = (cb * (1f - hacia) + theme.veinB * hacia) * shade * tinte + grain
                // La veta tine apenas el albedo de alrededor
                r = r * (1f - vm * 0.28f) + theme.veinR * vm * 0.28f
                g = g * (1f - vm * 0.28f) + theme.veinG * vm * 0.28f
                b = b * (1f - vm * 0.28f) + theme.veinB * vm * 0.28f

                // Lo mojado se oscurece, como cualquier cosa mojada: el agua
                // llena los poros y la superficie deja de dispersar la luz.
                if (moj != null) {
                    val k = 1f - moj[y * size + x] * 0.42f
                    r *= k; g *= k; b *= k
                }
                // El liquen tira al color de la veta del bioma, no a un verde
                // cualquiera: asi el verdin de la mina y el del bosque de
                // esporas no son la misma mancha pegada sobre dos paredes
                // distintas.
                val l = liq?.get(y * size + x) ?: 0f
                if (l > 0.002f) {
                    val lr = 0.20f + theme.veinR * 0.16f
                    val lg = 0.38f + theme.veinG * 0.22f
                    val lb = 0.17f + theme.veinB * 0.14f
                    r = r * (1f - l) + lr * l
                    g = g * (1f - l) + lg * l
                    b = b * (1f - l) + lb * l
                }

                albedo[ai++] = toByte(r); albedo[ai++] = toByte(g)
                albedo[ai++] = toByte(b); albedo[ai++] = toByte(vm)
            }
        }

        // 3) mapa de normales por diferencias centrales (con envolvente)
        val strength = when (layer) {
            LAYER_WALL -> 3.3f
            LAYER_FLOOR -> 2.1f
            LAYER_CEIL -> 2.7f
            else -> 1.2f
        }
        var ni = base
        for (y in 0 until size) {
            for (x in 0 until size) {
                val xm = (x - 1 + size) % size
                val xp = (x + 1) % size
                val ym = (y - 1 + size) % size
                val yp = (y + 1) % size
                val dx = (h[y * size + xp] - h[y * size + xm]) * strength
                val dy = (h[yp * size + x] - h[ym * size + x]) * strength
                var nx = -dx
                var ny = -dy
                var nz = 1f
                val len = sqrt(nx * nx + ny * ny + nz * nz)
                nx /= len; ny /= len; nz /= len
                normal[ni++] = toByte(nx * 0.5f + 0.5f)
                normal[ni++] = toByte(ny * 0.5f + 0.5f)
                normal[ni++] = toByte(nz * 0.5f + 0.5f)
                normal[ni++] = toByte(h[y * size + x])   // altura: la usa el especular
            }
        }
    }

    /** Sube los pixeles ya calculados a dos GL_TEXTURE_2D_ARRAY. */
    private fun upload(p: Pixels): Result {
        val ids = IntArray(2)
        GLES30.glGenTextures(2, ids, 0)
        uploadArray(ids[0], p.size, directBuffer(p.albedo))
        uploadArray(ids[1], p.size, directBuffer(p.normal))
        return Result(ids[0], ids[1], p.size)
    }

    private fun directBuffer(data: ByteArray): ByteBuffer =
        ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder()).apply {
            put(data); position(0)
        }

    private fun uploadArray(tex: Int, size: Int, data: ByteBuffer) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D_ARRAY, tex)
        val levels = max(1, (Math.log(size.toDouble()) / Math.log(2.0)).toInt() + 1)
        GLES30.glTexStorage3D(GLES30.GL_TEXTURE_2D_ARRAY, levels, GLES30.GL_RGBA8, size, size, LAYERS)
        GLES30.glTexSubImage3D(
            GLES30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0,
            size, size, LAYERS, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, data
        )
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D_ARRAY)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D_ARRAY, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
        GLUtil.checkError("uploadArray")
    }

    private fun toByte(f: Float): Byte {
        val v = (f.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (v and 0xFF).toByte()
    }
}
