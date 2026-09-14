package com.mggx.laberinto

import com.mggx.laberinto.gl.PaletaDelCielo
import java.io.File
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Una copia en Kotlin del shader del cielo, para poder MIRARLO.
 *
 * El cielo del patio se dibuja en la GPU (`Shaders.CIELO_FS`) y en la JVM no
 * hay OpenGL, asi que sin esto el cielo seria la unica cosa del juego que se
 * manda al telefono sin haberla visto nunca — justo lo que este proyecto viene
 * arreglando desde la 1.9.1.
 *
 * ## Que garantiza y que no
 *
 * Los NUMEROS son los mismos: los colores, el sol y el horizonte salen de
 * [PaletaDelCielo], que es el mismo objeto que le pasa los uniforms al shader.
 * Si alguien cambia la paleta, cambia en los dos lados.
 *
 * La CUENTA esta escrita dos veces, y eso no se puede evitar: una version
 * tiene que estar en GLSL. Es el mismo riesgo que tienen
 * `WorldMesh.realFloorHeight` contra `cara()` y `Anclajes` contra la pared
 * dibujada, y se maneja igual: la copia repite paso por paso lo que hace el
 * shader, en el mismo orden, y `CieloTest` comprueba que las dos versiones
 * sigan hablando de las mismas cosas.
 */
object VisorDeCielo {

    // ------------------------------------------------- las mismas funciones

    private fun fract(x: Float): Float = x - floor(x)

    /** `fract(sin(dot(p, (127.1, 311.7))) * 43758.5453)` */
    private fun hash(x: Float, y: Float): Float {
        val d = x * 127.1f + y * 311.7f
        return fract(sin(d.toDouble()).toFloat() * 43758.5453f)
    }

    private fun ruido(px: Float, py: Float): Float {
        val ix = floor(px); val iy = floor(py)
        var fx = px - ix; var fy = py - iy
        fx = fx * fx * (3f - 2f * fx)
        fy = fy * fy * (3f - 2f * fy)
        val a = hash(ix, iy)
        val b = hash(ix + 1f, iy)
        val c = hash(ix, iy + 1f)
        val d = hash(ix + 1f, iy + 1f)
        val ab = a + (b - a) * fx
        val cd = c + (d - c) * fx
        return ab + (cd - ab) * fy
    }

    private fun nubes(px0: Float, py0: Float): Float {
        var px = px0; var py = py0
        var v = 0f
        var amp = 0.5f
        for (i in 0 until 4) {
            v += ruido(px, py) * amp
            val nx = px * 2.03f + 1.7f
            val ny = py * 2.03f - 0.9f
            px = nx; py = ny
            amp *= 0.5f
        }
        return v
    }

    private fun mezcla(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun suave(bordeA: Float, bordeB: Float, x: Float): Float {
        val t = ((x - bordeA) / (bordeB - bordeA)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /**
     * El color del cielo para una direccion de mirada, ya en sRGB (0..1).
     *
     * @param afuera cuanto salis (0 adentro del tunel, 1 en el patio).
     */
    fun color(dx: Float, dy: Float, dz: Float, tiempo: Float, afuera: Float, out: FloatArray) {
        val l = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-6f)
        val x = dx / l; val y = dy / l; val z = dz / l

        val alto = y.coerceIn(0f, 1f)
        val t = alto.toDouble().pow(PaletaDelCielo.POTENCIA_ALTO.toDouble()).toFloat()
        var r = mezcla(PaletaDelCielo.HORIZONTE_R, PaletaDelCielo.CENIT_R, t)
        var g = mezcla(PaletaDelCielo.HORIZONTE_G, PaletaDelCielo.CENIT_G, t)
        var b = mezcla(PaletaDelCielo.HORIZONTE_B, PaletaDelCielo.CENIT_B, t)

        val haciaElSol = (x * PaletaDelCielo.SOL_X + y * PaletaDelCielo.SOL_Y +
            z * PaletaDelCielo.SOL_Z).coerceAtLeast(0f)
        val disco = haciaElSol.toDouble().pow(PaletaDelCielo.SOL_DISCO_POT.toDouble())
            .toFloat() * PaletaDelCielo.SOL_DISCO_FUERZA
        val halo = haciaElSol.toDouble().pow(PaletaDelCielo.SOL_HALO_POT.toDouble())
            .toFloat() * PaletaDelCielo.SOL_HALO_FUERZA
        r += PaletaDelCielo.SOL_R * (disco + halo)
        g += PaletaDelCielo.SOL_G * (disco + halo)
        b += PaletaDelCielo.SOL_B * (disco + halo)
        val abre = haciaElSol.toDouble().pow(2.0).toFloat() * (1f - alto)
        r = mezcla(r, r + PaletaDelCielo.SOL_R * PaletaDelCielo.DERRAME_SOL, abre)
        g = mezcla(g, g + PaletaDelCielo.SOL_G * PaletaDelCielo.DERRAME_SOL, abre)
        b = mezcla(b, b + PaletaDelCielo.SOL_B * PaletaDelCielo.DERRAME_SOL, abre)

        if (y > 0.006f) {
            val ux = x / y * PaletaDelCielo.NUBE_PROYECCION + tiempo * 0.0055f
            val uy = z / y * PaletaDelCielo.NUBE_PROYECCION + tiempo * 0.0021f
            val n = nubes(ux * PaletaDelCielo.NUBE_ESCALA, uy * PaletaDelCielo.NUBE_ESCALA)
            val tapa = suave(PaletaDelCielo.NUBE_DESDE, PaletaDelCielo.NUBE_HASTA, n) *
                suave(0f, 0.13f, y)
            val calor = haciaElSol.toDouble().pow(3.0).toFloat() * 0.6f
            val lr = mezcla(0.86f, PaletaDelCielo.SOL_R * 1.30f, calor)
            val lg = mezcla(0.88f, PaletaDelCielo.SOL_G * 1.30f, calor)
            val lb = mezcla(0.94f, PaletaDelCielo.SOL_B * 1.30f, calor)
            r = mezcla(r, lr, tapa * PaletaDelCielo.NUBE_FUERZA)
            g = mezcla(g, lg, tapa * PaletaDelCielo.NUBE_FUERZA)
            b = mezcla(b, lb, tapa * PaletaDelCielo.NUBE_FUERZA)
        } else {
            val k = suave(0.006f, -0.30f, y)
            r = mezcla(r, PaletaDelCielo.SUELO_R, k)
            g = mezcla(g, PaletaDelCielo.SUELO_G, k)
            b = mezcla(b, PaletaDelCielo.SUELO_B, k)
        }

        // Adentro del tunel el fondo sigue siendo el de la cueva. Se usa el
        // color de niebla de ENTRADA, que es el tema del nivel 999.
        r = mezcla(NIEBLA_R * 0.55f, r, afuera)
        g = mezcla(NIEBLA_G * 0.55f, g, afuera)
        b = mezcla(NIEBLA_B * 0.55f, b, afuera)

        out[0] = tono(r); out[1] = tono(g); out[2] = tono(b)
    }

    private const val NIEBLA_R = 0.055f
    private const val NIEBLA_G = 0.052f
    private const val NIEBLA_B = 0.048f

    /** `c/(c+0.85)` y gamma, igual que el shader. */
    private fun tono(c: Float): Float =
        (c / (c + 0.85f)).toDouble().pow(1.0 / 2.2).toFloat()

    // ------------------------------------------------------------- la foto

    /**
     * Saca una foto del cielo con la misma camara del juego.
     *
     * @param mirandoY grados hacia arriba (0 al horizonte).
     */
    fun foto(
        ancho: Int, alto: Int, rumboGrados: Float, mirandoY: Float,
        tiempo: Float = 0f, afuera: Float = 1f, fovGrados: Float = 62f
    ): VisorDeMallas.Lienzo {
        val l = VisorDeMallas.Lienzo(ancho, alto)
        val yaw = Math.toRadians(rumboGrados.toDouble())
        val pitch = Math.toRadians(mirandoY.toDouble())
        val cp = kotlin.math.cos(pitch).toFloat()
        val fx = (kotlin.math.sin(yaw) * cp).toFloat()
        val fy = kotlin.math.sin(pitch).toFloat()
        val fz = (kotlin.math.cos(yaw) * cp).toFloat()
        // Los mismos ejes que arma el renderer.
        val rl = sqrt(fz * fz + fx * fx).coerceAtLeast(1e-5f)
        val rx = -fz / rl; val ry = 0f; val rz = fx / rl
        val ux = ry * fz - rz * fy
        val uy = rz * fx - rx * fz
        val uz = rx * fy - ry * fx
        val tan = kotlin.math.tan(Math.toRadians(fovGrados * 0.5).toFloat())
        val aspecto = ancho.toFloat() / alto
        val c = FloatArray(3)
        for (py in 0 until alto) {
            val ny = 1f - 2f * (py + 0.5f) / alto
            for (px in 0 until ancho) {
                val nx = 2f * (px + 0.5f) / ancho - 1f
                val dx = fx + rx * tan * aspecto * nx + ux * tan * ny
                val dy = fy + ry * tan * aspecto * nx + uy * tan * ny
                val dz = fz + rz * tan * aspecto * nx + uz * tan * ny
                color(dx, dy, dz, tiempo, afuera, c)
                l.pixel(px, py, -1f, c[0], c[1], c[2])
            }
        }
        return l
    }

    /** Una hoja con varias miradas del cielo. */
    fun hoja(nombre: String): File {
        val vistas = listOf(
            Triple("saliendo del tunel", 0f, 6f),
            Triple("mirando arriba", 0f, 52f),
            Triple("hacia el sol", -28f, 16f),
            Triple("de espaldas al sol", 152f, 16f)
        )
        val ancho = 320; val alto = 200
        val hoja = VisorDeMallas.Lienzo(ancho * 2, alto * 2)
        for ((i, v) in vistas.withIndex()) {
            val celda = foto(ancho, alto, v.second, v.third)
            val ox = (i % 2) * ancho
            val oy = (i / 2) * alto
            for (y in 0 until alto) {
                System.arraycopy(
                    celda.rgb, y * ancho * 3,
                    hoja.rgb, ((oy + y) * hoja.ancho + ox) * 3, ancho * 3
                )
            }
        }
        return VisorDeMallas.guardar(hoja, nombre)
    }

    /** Cuanto se despega el color de dos direcciones, para medir en un test. */
    fun distancia(a: FloatArray, b: FloatArray): Float =
        abs(a[0] - b[0]) + abs(a[1] - b[1]) + abs(a[2] - b[2])
}
