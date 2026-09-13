package com.mggx.laberinto

import com.mggx.laberinto.gl.PropMeshes
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.Deflater
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Un rasterizador por software para MIRAR los modelos.
 *
 * Por que existe: todos los modelos del juego se escriben a mano, en codigo, y
 * hasta ahora se verificaban solo por numeros (que las caras miren para afuera,
 * que las proporciones entren en un rango). Eso agarra los errores graves, pero
 * no alcanza para decir si algo "se ve bien": un pico puede tener todas las
 * caras en orden y aun asi estar puesto como una cola, y eso paso de verdad.
 *
 * Con esto, cualquiera (o cualquier test) puede sacarle una foto a una malla y
 * abrirla. No necesita Android ni OpenGL: es un triangulo a la vez, con buffer
 * de profundidad, escrito a mano sobre un array de bytes.
 *
 * No es para que se vea lindo, es para que se ENTIENDA la forma: luz principal
 * en diagonal, relleno del lado opuesto para que la sombra no sea negra, y un
 * contorno claro para despegar la silueta del fondo.
 */
object VisorDeMallas {

    /** Donde van las fotos. Es carpeta de build: no se commitea. */
    val CARPETA: File = File("build/modelos")

    // ------------------------------------------------------------- lienzo

    class Lienzo(val ancho: Int, val alto: Int) {
        val rgb = ByteArray(ancho * alto * 3)
        val z = FloatArray(ancho * alto) { Float.MAX_VALUE }

        fun fondo(r: Int, g: Int, b: Int) {
            var i = 0
            while (i < rgb.size) {
                rgb[i] = r.toByte(); rgb[i + 1] = g.toByte(); rgb[i + 2] = b.toByte()
                i += 3
            }
        }

        fun pixel(x: Int, y: Int, prof: Float, r: Float, g: Float, b: Float) {
            if (x < 0 || y < 0 || x >= ancho || y >= alto) return
            val i = y * ancho + x
            if (prof >= z[i]) return
            z[i] = prof
            val j = i * 3
            rgb[j] = canal(r); rgb[j + 1] = canal(g); rgb[j + 2] = canal(b)
        }

        private fun canal(v: Float): Byte {
            val n = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            return (n and 0xFF).toByte()
        }
    }

    // -------------------------------------------------------------- camara

    /**
     * Dibuja [g] mirandola desde [gradosY] alrededor y [gradosX] de altura.
     *
     * La camara se acomoda sola al tamano del modelo: se mira el centro de su
     * caja y se retrocede lo necesario para que entre entero. Asi la misma
     * llamada sirve para un pan de 10 cm y para un minero de 1,72 m.
     */
    fun dibujar(
        lienzo: Lienzo,
        g: PropMeshes.Geometry,
        gradosY: Float,
        gradosX: Float = 18f,
        margen: Float = 1.22f
    ) {
        if (g.vertices.isEmpty()) return

        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        var i = 0
        while (i < g.vertices.size) {
            val x = g.vertices[i]; val y = g.vertices[i + 1]; val z = g.vertices[i + 2]
            if (x < minX) minX = x; if (x > maxX) maxX = x
            if (y < minY) minY = y; if (y > maxY) maxY = y
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
            i += 6
        }
        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        val radio = max(
            max(maxX - minX, maxY - minY), maxZ - minZ
        ) * 0.5f * margen + 1e-4f

        val ay = Math.toRadians(gradosY.toDouble())
        val ax = Math.toRadians(gradosX.toDouble())
        val dist = radio * 2.9f

        // Base de la camara: mira al centro desde (ay, ax) a distancia dist.
        val dirX = (cos(ax) * sin(ay)).toFloat()
        val dirY = sin(ax).toFloat()
        val dirZ = (cos(ax) * cos(ay)).toFloat()
        val ojoX = cx + dirX * dist
        val ojoY = cy + dirY * dist
        val ojoZ = cz + dirZ * dist
        // Adelante = del ojo al centro.
        val fX = -dirX; val fY = -dirY; val fZ = -dirZ
        // Derecha = adelante x arriba(0,1,0), normalizado.
        //
        // `f x (0,1,0)` da `(-fz, 0, fx)`. Con el signo al reves (que fue el
        // primer intento) la base entera queda espejada y el modelo sale
        // cabeza abajo: el casco se veia como un cono y el minero con las
        // piernas para arriba. Como el visor existe justamente para confiar en
        // lo que se ve, un error aca envenena todo lo demas.
        var rX = -fZ; var rZ = fX
        val rl = sqrt(rX * rX + rZ * rZ).coerceAtLeast(1e-5f)
        rX /= rl; rZ /= rl
        val rY = 0f
        // Arriba = derecha x adelante.
        val uX = rY * fZ - rZ * fY
        val uY = rZ * fX - rX * fZ
        val uZ = rX * fY - rY * fX

        // Luz principal en diagonal desde arriba-izquierda-adelante, en el
        // espacio de la camara: asi la forma se lee igual desde cualquier angulo.
        val lx = -0.45f; val ly = 0.62f; val lz = 0.64f
        val ll = sqrt(lx * lx + ly * ly + lz * lz)

        val escala = min(lienzo.ancho, lienzo.alto) * 0.5f / radio * 0.92f

        fun proyectar(vx: Float, vy: Float, vz: Float): FloatArray {
            val dx = vx - ojoX; val dy = vy - ojoY; val dz = vz - ojoZ
            val ex = dx * rX + dy * rY + dz * rZ
            val ey = dx * uX + dy * uY + dz * uZ
            val ez = dx * fX + dy * fY + dz * fZ    // profundidad, positiva adelante
            if (ez < 1e-3f) return floatArrayOf(0f, 0f, -1f)
            // Proyeccion debil: casi ortografica, con un toque de perspectiva
            // para que se entienda el volumen sin deformar las proporciones.
            val k = escala * dist / (dist + (ez - dist) * 0.35f)
            return floatArrayOf(
                lienzo.ancho * 0.5f + ex * k,
                lienzo.alto * 0.5f - ey * k,
                ez
            )
        }

        fun normalCamara(nx: Float, ny: Float, nz: Float): FloatArray = floatArrayOf(
            nx * rX + ny * rY + nz * rZ,
            nx * uX + ny * uY + nz * uZ,
            nx * fX + ny * fY + nz * fZ
        )

        var t = 0
        while (t + 2 < g.indices.size) {
            val i0 = g.indices[t] * 6
            val i1 = g.indices[t + 1] * 6
            val i2 = g.indices[t + 2] * 6
            t += 3

            val p0 = proyectar(g.vertices[i0], g.vertices[i0 + 1], g.vertices[i0 + 2])
            val p1 = proyectar(g.vertices[i1], g.vertices[i1 + 1], g.vertices[i1 + 2])
            val p2 = proyectar(g.vertices[i2], g.vertices[i2 + 1], g.vertices[i2 + 2])
            if (p0[2] < 0f || p1[2] < 0f || p2[2] < 0f) continue

            // Normal promedio del triangulo, pasada al espacio de la camara.
            val nx = (g.vertices[i0 + 3] + g.vertices[i1 + 3] + g.vertices[i2 + 3]) / 3f
            val ny = (g.vertices[i0 + 4] + g.vertices[i1 + 4] + g.vertices[i2 + 4]) / 3f
            val nz = (g.vertices[i0 + 5] + g.vertices[i1 + 5] + g.vertices[i2 + 5]) / 3f
            val nl = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-6f)
            val n = normalCamara(nx / nl, ny / nl, nz / nl)

            // La normal apunta hacia la camara cuando su Z de camara es
            // NEGATIVA (adelante es +Z de profundidad, pero la normal mira al
            // ojo). Se descartan las de atras, como hace OpenGL.
            val haciaAdelante = -n[2]
            if (haciaAdelante <= 0.02f) continue

            val difusa = max(0f, (n[0] * lx + n[1] * ly + n[2] * lz) / ll)
            // Relleno desde el lado opuesto, para que la sombra no sea negra.
            val relleno = max(0f, (-n[0] * lx - n[1] * ly + n[2] * lz) / ll) * 0.22f
            // Contorno: los bordes del modelo se aclaran y despegan del fondo.
            val borde = Math.pow((1f - haciaAdelante).toDouble(), 2.2).toFloat() * 0.55f
            val luz = (0.16f + difusa * 0.78f + relleno + borde).coerceIn(0f, 1.35f)

            rellenarTriangulo(lienzo, p0, p1, p2, luz)
        }
    }

    /**
     * La foto que importa para todo lo que va en la mano: lo que ve el
     * jugador.
     *
     * Las demas vistas de este visor orbitan alrededor del modelo y lo encuadran
     * solas, que sirve para juzgar la FORMA de una pieza. Pero un arma o un
     * objeto de mano no se juzga por su forma suelta: se juzga por donde cae en
     * la pantalla, con la camara en el ojo, la misma apertura del juego (62
     * grados) y la misma proporcion de pantalla. Un pico impecable puede estar
     * tapando media vista, o quedar fuera de cuadro, y ninguna de las otras
     * fotos lo muestra.
     *
     * La camara esta en el origen mirando hacia -Z, que es el espacio en el que
     * ya vienen los brazos.
     */
    fun primeraPersona(
        g: PropMeshes.Geometry,
        nombre: String,
        ancho: Int = 480,
        alto: Int = 280,
        fovGrados: Float = 62f
    ): File {
        val l = Lienzo(ancho, alto)
        // Media pantalla de alto en el plano z = -1.
        val k = alto * 0.5f / kotlin.math.tan(Math.toRadians(fovGrados * 0.5)).toFloat()

        fun proyectar(vx: Float, vy: Float, vz: Float): FloatArray {
            val prof = -vz                       // positiva adelante
            if (prof < 1e-3f) return floatArrayOf(0f, 0f, -1f)
            return floatArrayOf(
                ancho * 0.5f + vx * k / prof,
                alto * 0.5f - vy * k / prof,
                prof
            )
        }

        val lx = -0.45f; val ly = 0.62f; val lz = 0.64f
        val ll = sqrt(lx * lx + ly * ly + lz * lz)

        // La cruz de la mira, para tener referencia de donde esta el centro.
        for (x in ancho / 2 - 9..ancho / 2 + 9) l.pixel(x, alto / 2, 1e9f, 0.30f, 0.30f, 0.34f)
        for (y in alto / 2 - 9..alto / 2 + 9) l.pixel(ancho / 2, y, 1e9f, 0.30f, 0.30f, 0.34f)

        var t = 0
        while (t + 2 < g.indices.size) {
            val i0 = g.indices[t] * 6
            val i1 = g.indices[t + 1] * 6
            val i2 = g.indices[t + 2] * 6
            t += 3
            val p0 = proyectar(g.vertices[i0], g.vertices[i0 + 1], g.vertices[i0 + 2])
            val p1 = proyectar(g.vertices[i1], g.vertices[i1 + 1], g.vertices[i1 + 2])
            val p2 = proyectar(g.vertices[i2], g.vertices[i2 + 1], g.vertices[i2 + 2])
            if (p0[2] < 0f || p1[2] < 0f || p2[2] < 0f) continue

            val nx = (g.vertices[i0 + 3] + g.vertices[i1 + 3] + g.vertices[i2 + 3]) / 3f
            val ny = (g.vertices[i0 + 4] + g.vertices[i1 + 4] + g.vertices[i2 + 4]) / 3f
            val nz = (g.vertices[i0 + 5] + g.vertices[i1 + 5] + g.vertices[i2 + 5]) / 3f
            val nl = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-6f)
            val cnz = nz / nl
            if (cnz <= 0.02f) continue          // cara de atras
            val difusa = max(0f, (nx / nl * lx + ny / nl * ly + cnz * lz) / ll)
            val borde = Math.pow((1f - cnz).toDouble(), 2.2).toFloat() * 0.55f
            val luz = (0.16f + difusa * 0.78f + borde).coerceIn(0f, 1.35f)
            rellenarTriangulo(l, p0, p1, p2, luz)
        }

        CARPETA.mkdirs()
        val f = File(CARPETA, "$nombre.png")
        f.writeBytes(png(l.rgb, l.ancho, l.alto))
        return f
    }

    private fun rellenarTriangulo(
        l: Lienzo, a: FloatArray, b: FloatArray, c: FloatArray, luz: Float
    ) {
        val minX = max(0, min(min(a[0], b[0]), c[0]).toInt() - 1)
        val maxX = min(l.ancho - 1, max(max(a[0], b[0]), c[0]).toInt() + 1)
        val minY = max(0, min(min(a[1], b[1]), c[1]).toInt() - 1)
        val maxY = min(l.alto - 1, max(max(a[1], b[1]), c[1]).toInt() + 1)
        if (minX > maxX || minY > maxY) return

        val area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])
        if (abs(area) < 1e-7f) return

        // El color es plano por triangulo: lo que interesa es la FORMA, y un
        // sombreado plano la muestra mejor que uno suave, porque cada faceta
        // se distingue de la de al lado.
        val r = luz * 0.86f
        val g = luz * 0.84f
        val bl = luz * 0.80f

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val px = x + 0.5f
                val py = y + 0.5f
                var w0 = (b[0] - a[0]) * (py - a[1]) - (b[1] - a[1]) * (px - a[0])
                var w1 = (c[0] - b[0]) * (py - b[1]) - (c[1] - b[1]) * (px - b[0])
                var w2 = (a[0] - c[0]) * (py - c[1]) - (a[1] - c[1]) * (px - c[0])
                if (area < 0f) { w0 = -w0; w1 = -w1; w2 = -w2 }
                if (w0 < 0f || w1 < 0f || w2 < 0f) continue

                val suma = (w0 + w1 + w2).coerceAtLeast(1e-7f)
                // Los pesos estan rotados respecto de los vertices: w1 pesa a,
                // w2 pesa b y w0 pesa c.
                val prof = (w1 * a[2] + w2 * b[2] + w0 * c[2]) / suma
                l.pixel(x, y, prof, r, g, bl)
            }
        }
    }

    // -------------------------------------------------------- hoja de fotos

    /**
     * Una tira de fotos del mismo modelo girando, guardada en un PNG.
     *
     * Cuatro angulos porque con uno solo no se ve nada: un modelo puede estar
     * perfecto de frente y ser un desastre de costado, que es justo el error
     * que este visor tiene que agarrar.
     */
    fun retrato(
        g: PropMeshes.Geometry,
        nombre: String,
        lado: Int = 260,
        angulos: FloatArray = floatArrayOf(0f, 90f, 180f, 270f),
        elevacion: Float = 18f
    ): File {
        CARPETA.mkdirs()
        val hoja = Lienzo(lado * angulos.size, lado)
        hoja.fondo(26, 27, 30)

        for ((k, ang) in angulos.withIndex()) {
            val celda = Lienzo(lado, lado)
            celda.fondo(26, 27, 30)
            dibujar(celda, g, ang, elevacion)
            // Se pega la celda en la hoja.
            for (y in 0 until lado) {
                val origen = y * lado * 3
                val destino = (y * hoja.ancho + k * lado) * 3
                System.arraycopy(celda.rgb, origen, hoja.rgb, destino, lado * 3)
            }
            // Una linea separadora entre vistas.
            if (k > 0) {
                for (y in 0 until lado) {
                    val j = (y * hoja.ancho + k * lado) * 3
                    hoja.rgb[j] = 60; hoja.rgb[j + 1] = 62; hoja.rgb[j + 2] = 68
                }
            }
        }

        val f = File(CARPETA, "$nombre.png")
        f.writeBytes(png(hoja.rgb, hoja.ancho, hoja.alto))
        return f
    }

    /** Junta varios modelos en una sola hoja, uno por fila. */
    fun hoja(
        modelos: List<Pair<String, PropMeshes.Geometry>>,
        nombre: String,
        lado: Int = 230,
        angulos: FloatArray = floatArrayOf(0f, 90f, 180f, 270f)
    ): File {
        CARPETA.mkdirs()
        val hoja = Lienzo(lado * angulos.size, lado * modelos.size)
        hoja.fondo(26, 27, 30)

        for ((fila, par) in modelos.withIndex()) {
            for ((col, ang) in angulos.withIndex()) {
                val celda = Lienzo(lado, lado)
                celda.fondo(26, 27, 30)
                dibujar(celda, par.second, ang, 18f)
                for (y in 0 until lado) {
                    val origen = y * lado * 3
                    val destino = ((fila * lado + y) * hoja.ancho + col * lado) * 3
                    System.arraycopy(celda.rgb, origen, hoja.rgb, destino, lado * 3)
                }
            }
            // Linea horizontal entre modelos.
            if (fila > 0) {
                val y = fila * lado
                for (x in 0 until hoja.ancho) {
                    val j = (y * hoja.ancho + x) * 3
                    hoja.rgb[j] = 70; hoja.rgb[j + 1] = 72; hoja.rgb[j + 2] = 80
                }
            }
        }
        val f = File(CARPETA, "$nombre.png")
        f.writeBytes(png(hoja.rgb, hoja.ancho, hoja.alto))
        return f
    }

    // ------------------------------------------------------------------ png

    private fun png(rgb: ByteArray, ancho: Int, alto: Int): ByteArray {
        val crudo = ByteArrayOutputStream()
        for (y in 0 until alto) {
            crudo.write(0)
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
        val crc = CRC32(); crc.update(cuerpo)
        out.write(entero(crc.value.toInt()))
        return out.toByteArray()
    }

    private fun comprimir(datos: ByteArray): ByteArray {
        val d = Deflater()
        d.setInput(datos); d.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        d.end()
        return out.toByteArray()
    }
}
