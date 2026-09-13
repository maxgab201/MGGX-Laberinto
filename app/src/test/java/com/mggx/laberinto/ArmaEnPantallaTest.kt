package com.mggx.laberinto

import com.mggx.laberinto.gl.ArmsMesh
import com.mggx.laberinto.gl.BrazoAnim
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Donde cae en la PANTALLA lo que llevas en la mano.
 *
 * Los demas tests de modelado miran la forma de cada pieza suelta: que las
 * caras apunten para afuera, que las proporciones cierren, que el mango cruce
 * el puno. Todo eso puede estar perfecto y el arma verse pesima igual, porque
 * lo que se ve no es la pieza: es su proyeccion, con la camara en el ojo, la
 * apertura del juego y el brazo en su pose de reposo.
 *
 * Eso es justo lo que estaba mal. El arma se modela hacia adelante y se
 * levantaba 55 grados, pero como apunta casi de frente a la camara el escorzo
 * se come el angulo: en pantalla quedaba PARADA, como un mastil en medio del
 * cuadro, y a las de cabeza grande (el hacha) se les cortaba la cabeza contra
 * el borde de arriba. Bajarle el cabeceo no cambiaba nada — lo que lo cambia
 * es ladearla.
 */
class ArmaEnPantallaTest {

    private val FOV = 62f

    /**
     * Proporcion de pantalla mas ANGOSTA que se soporta (una tablet 4:3). Es
     * el caso exigente para que no se corte nada por los costados: cuanto mas
     * ancho el telefono, mas lugar sobra.
     */
    private val ASPECTO_ANGOSTO = 4f / 3f

    class Huella(
        val minX: Float, val maxX: Float, val minY: Float, val maxY: Float,
        val tocaElCentro: Boolean, val puntos: Int
    )

    /**
     * Proyecta las piezas marcadas con [tag] a coordenadas de pantalla
     * normalizadas: x e y de -1 a 1, con (0,0) en la mira.
     */
    private fun huella(
        mesh: ArmsMesh.Mesh, tagOk: (Float) -> Boolean, side: Float,
        golpe: Float = 0f, aspecto: Float = ASPECTO_ANGOSTO
    ): Huella {
        val p = BrazoAnim.pose(side, golpe, 0f, 0f, 0f)
        val ry = Math.toRadians(p.rotYReposo.toDouble())
        val rx = Math.toRadians(p.rotXReposo.toDouble())
        val rz = Math.toRadians(p.rotZReposo.toDouble())
        val ty = tan(Math.toRadians(FOV * 0.5)).toFloat()
        val tx = ty * aspecto

        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var centro = false
        var n = 0
        val st = ArmsMesh.STRIDE_FLOATS
        var i = 0
        while (i < mesh.vertices.size) {
            if (!tagOk(mesh.vertices[i + 6])) { i += st; continue }
            var vx = mesh.vertices[i] * p.escala
            var vy = mesh.vertices[i + 1] * p.escala
            var vz = mesh.vertices[i + 2] * p.escala
            // El mismo orden que arma el renderer: Y, X, Z.
            var a = (vx * cos(ry) + vz * sin(ry)).toFloat()
            var b = (-vx * sin(ry) + vz * cos(ry)).toFloat()
            vx = a; vz = b
            a = (vy * cos(rx) - vz * sin(rx)).toFloat()
            b = (vy * sin(rx) + vz * cos(rx)).toFloat()
            vy = a; vz = b
            a = (vx * cos(rz) - vy * sin(rz)).toFloat()
            b = (vx * sin(rz) + vy * cos(rz)).toFloat()
            vx = a; vy = b
            vx += p.tx; vy += p.ty; vz += p.tz

            val prof = -vz
            if (prof > 0.02f) {
                val sx = vx / (prof * tx)
                val sy = vy / (prof * ty)
                if (sx < minX) minX = sx; if (sx > maxX) maxX = sx
                if (sy < minY) minY = sy; if (sy > maxY) maxY = sy
                // La ventanita de la mira: lo que cae ahi adentro te tapa
                // justo lo que estas apuntando.
                if (kotlin.math.abs(sx) < 0.16f && kotlin.math.abs(sy) < 0.22f) centro = true
                n++
            }
            i += st
        }
        return Huella(minX, maxX, minY, maxY, centro, n)
    }

    private fun soloArma(t: Float) = t > 1.5f
    private fun soloObjeto(t: Float) = t < -1.5f

    @Test
    fun ningunArmaSeSaleDelCuadro() {
        // Una cabeza de hacha cortada por el borde de arriba se ve como un
        // error de dibujo, no como un hacha.
        for (arma in ArmsMesh.Arma.entries) {
            val h = huella(ArmsMesh.build(arma = arma), ::soloArma, 1f)
            assertTrue("${arma.name}: no se ve nada", h.puntos > 20)
            assertTrue("${arma.name}: se corta arriba (y=${h.maxY})", h.maxY <= 0.97f)
            assertTrue("${arma.name}: se corta a la derecha (x=${h.maxX})", h.maxX <= 0.99f)
            assertTrue("${arma.name}: se corta a la izquierda (x=${h.minX})", h.minX >= -0.99f)
        }
    }

    @Test
    fun ningunArmaTapaLaMira() {
        // En reposo la mira tiene que quedar limpia: es donde mirás para pegar.
        for (arma in ArmsMesh.Arma.entries) {
            val h = huella(ArmsMesh.build(arma = arma), ::soloArma, 1f)
            assertTrue("${arma.name} tapa la mira en reposo", !h.tocaElCentro)
        }
    }

    @Test
    fun elArmaSeVeEnDiagonalYNoParadaComoUnMastil() {
        // El bug de fondo. Se mide la forma de la huella: un mastil deja una
        // mancha angosta y muy alta; un arma agarrada en diagonal deja una
        // mancha ancha. La proporcion alto/ancho lo dice sin tener que mirar.
        for (arma in ArmsMesh.Arma.entries) {
            val h = huella(ArmsMesh.build(arma = arma), ::soloArma, 1f)
            val ancho = h.maxX - h.minX
            val alto = h.maxY - h.minY
            assertTrue(
                "${arma.name} queda parada como un mastil (${alto / ancho} de alto por ancho)",
                alto / ancho < 2.2f
            )
        }
    }

    @Test
    fun elArmaVaAlaDerechaYElObjetoALaIzquierda() {
        for (arma in ArmsMesh.Arma.entries) {
            val h = huella(ArmsMesh.build(arma = arma), ::soloArma, 1f)
            assertTrue("${arma.name} se paso al lado izquierdo", h.minX > -0.05f)
        }
        for (obj in ArmsMesh.Objeto.entries) {
            val h = huella(ArmsMesh.build(objeto = obj), ::soloObjeto, -1f)
            assertTrue("${obj.name}: no se ve nada", h.puntos > 8)
            assertTrue("${obj.name} se paso al lado derecho", h.maxX < 0.05f)
            assertTrue("${obj.name} tapa la mira", !h.tocaElCentro)
            assertTrue("${obj.name} se corta arriba (y=${h.maxY})", h.maxY <= 0.97f)
        }
    }

    @Test
    fun elArmaOcupaUnPedazoRazonableDeLaPantalla() {
        // Ni una miniatura que no se entiende ni un cartel que tapa el juego.
        for (arma in ArmsMesh.Arma.entries) {
            val h = huella(ArmsMesh.build(arma = arma), ::soloArma, 1f, aspecto = 16f / 9f)
            val area = (h.maxX - h.minX) * 0.5f * (h.maxY - h.minY) * 0.5f
            assertTrue("${arma.name} ocupa solo el ${(area * 100).toInt()}% del cuadro", area > 0.03f)
            assertTrue("${arma.name} ocupa el ${(area * 100).toInt()}% del cuadro", area < 0.30f)
        }
    }

    @Test
    fun enElPicoDelGolpeElArmaSigueEnPantalla() {
        // Pegando SI puede cruzar la mira (ese es el punto), pero no puede
        // irse de cuadro: seria un golpe que no se ve.
        for (arma in ArmsMesh.Arma.entries) {
            val h = huella(ArmsMesh.build(arma = arma), ::soloArma, 1f, golpe = 1f)
            assertTrue("${arma.name} se fue de cuadro al pegar (y=${h.maxY})", h.maxY <= 1.15f)
            assertTrue("${arma.name} se fue por la derecha al pegar (x=${h.minX})", h.minX < 0.9f)
        }
    }
}
