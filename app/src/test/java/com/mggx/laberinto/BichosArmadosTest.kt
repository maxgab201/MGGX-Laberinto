package com.mggx.laberinto

import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.gl.ArmadoDeBichos
import com.mggx.laberinto.maze.Maze
import com.mggx.laberinto.maze.MazeGenerator.EnemyKind
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Las medidas de cada bicho ARMADO.
 *
 * Hasta ahora se podian medir las piezas sueltas, pero no el bicho entero: el
 * armado vivia adentro del renderer. Con [ArmadoDeBichos] afuera, esto se
 * puede medir, y lo primero que aparecio al medirlo es que los numeros no
 * cerraban.
 *
 * Y no es un problema estetico. `EnemyKind.alto` y `EnemyKind.radio` son lo
 * que decide **por que huecos entra un bicho y a que distancia te muerde**
 * (ver `Enemy.kt`: el techo se calcula como `clearance - alto`, y la mordida
 * como `radio + PLAYER_RADIUS + 0.18`). Si lo que se dibuja no coincide con lo
 * que se declara, estas esquivando una caja que no podes ver.
 */
class BichosArmadosTest {

    private fun caja(kind: EnemyKind) = BichoArmado.caja(BichoArmado.geometria(kind))

    /**
     * La caja del CUERPO, sin alas, patas ni ojos.
     *
     * Es lo que tienen que describir `alto` y `radio`: un ala de murcielago no
     * es parte de lo que chocas ni de lo que le pegas, una pata de arana
     * tampoco, y una pala de topo menos. Medir la silueta entera contra el
     * radio de colision compararia dos cosas distintas.
     */
    /** Alas, patas, palas de cavar y ojos: lo que NO es cuerpo. */
    private val MIEMBROS = setOf(
        ArmadoDeBichos.Malla.ALA,
        ArmadoDeBichos.Malla.PATA,
        ArmadoDeBichos.Malla.PALA,
        ArmadoDeBichos.Malla.GEMA
    )

    private fun cajaDelCuerpo(kind: EnemyKind): FloatArray {
        val out = floatArrayOf(9f, -9f, 9f, -9f, 9f, -9f)
        for (p in ArmadoDeBichos.armar(kind, 0f, 0f)) {
            if (p.malla in MIEMBROS) continue
            val c = BichoArmado.cajaDePieza(p)
            for (k in 0..2) {
                out[k * 2] = minOf(out[k * 2], c[k * 2])
                out[k * 2 + 1] = max(out[k * 2 + 1], c[k * 2 + 1])
            }
        }
        return out
    }

    @Test
    fun elAltoQueSeVeEsElAltoQueSeDeclara() {
        for (kind in EnemyKind.entries) {
            val c = cajaDelCuerpo(kind)
            val alto = c[3] - c[2]
            val error = abs(alto - kind.alto) / kind.alto
            assertTrue(
                "${kind.name} se ve de ${"%.2f".format(alto)} m pero el juego lo trata " +
                    "como de ${kind.alto} m (${(error * 100).toInt()}% de diferencia)",
                error < 0.18f
            )
        }
    }

    @Test
    fun elRadioDeColisionCaeAdentroDelCuerpo() {
        // `radio` es un circulo, y un bicho no lo es: el rastrero es largo y
        // angosto, el guardian ancho de hombros y fino de perfil. Pedir que el
        // circulo sea igual al ancho seria pedir algo falso.
        //
        // Lo que si tiene que valer: que el circulo caiga ADENTRO de la huella
        // del cuerpo. Ni mas chico que el lado angosto (le pegarias al aire
        // alrededor y no al bicho), ni mas grande que el lado largo (te
        // morderia desde mas lejos de lo que se ve).
        for (kind in EnemyKind.entries) {
            val c = cajaDelCuerpo(kind)
            val a = (c[1] - c[0]) * 0.5f
            val b = (c[5] - c[4]) * 0.5f
            val angosto = minOf(a, b)
            val largo = max(a, b)
            assertTrue(
                "${kind.name}: le pegas con radio ${kind.radio} m y el cuerpo mide de " +
                    "${"%.2f".format(angosto)} a ${"%.2f".format(largo)} m de medio ancho",
                kind.radio >= angosto * 0.85f && kind.radio <= largo * 1.15f
            )
        }
    }

    @Test
    fun losQueCaminanPisanElPiso() {
        for (kind in EnemyKind.entries) {
            if (kind.vuelaA > 0f) continue
            val c = caja(kind)
            assertTrue(
                "${kind.name} flota ${"%.3f".format(c[2])} m sobre el piso",
                abs(c[2]) < 0.04f
            )
        }
    }

    @Test
    fun ningunaPiezaQuedaEnterrada() {
        for (kind in EnemyKind.entries) {
            if (kind.vuelaA > 0f) continue
            for (p in ArmadoDeBichos.armar(kind, 0f, 0f)) {
                val c = BichoArmado.cajaDePieza(p)
                assertTrue(
                    "${kind.name}: una pieza ${p.malla} se hunde ${"%.3f".format(-c[2])} m en la roca",
                    c[2] > -0.05f
                )
            }
        }
    }

    @Test
    fun ningunaPiezaQuedaSueltaEnElAire() {
        // Cada pieza tiene que tocar a otra. Es lo que agarra al ala despegada
        // del cuerpo o a la pata que quedo colgando en el aire.
        for (kind in EnemyKind.entries) {
            val piezas = ArmadoDeBichos.armar(kind, 0f, 0f)
            val cajas = piezas.map { BichoArmado.cajaDePieza(it) }
            for (i in piezas.indices) {
                val tocaAAlguna = piezas.indices.any { j -> j != i && seTocan(cajas[i], cajas[j]) }
                assertTrue(
                    "${kind.name}: la pieza ${piezas[i].malla} quedo suelta, sin tocar nada",
                    tocaAAlguna
                )
            }
        }
    }

    /** Dos cajas que se pisan o se rozan (con unos milimetros de tolerancia). */
    private fun seTocan(a: FloatArray, b: FloatArray): Boolean {
        val h = 0.02f
        for (k in 0..2) {
            if (a[k * 2] > b[k * 2 + 1] + h || b[k * 2] > a[k * 2 + 1] + h) return false
        }
        return true
    }

    @Test
    fun cadaBichoEntraPorDondeElJuegoDiceQueEntra() {
        val c = GameSession.CELL
        for (kind in EnemyKind.entries) {
            val caja = caja(kind)
            val ancho = max(caja[1] - caja[0], caja[5] - caja[4])
            // El pasillo mide una casilla de lado; con la panza de las paredes
            // el hueco util es menor, asi que se pide que entre con holgura.
            assertTrue(
                "${kind.name} mide ${"%.2f".format(ancho)} m de ancho y el pasillo ${c} m",
                ancho < c * 0.85f
            )
            // Y si su `alto` promete que pasa por una gatera, el dibujo tiene
            // que caber ahi de verdad.
            val alto = caja[3] - caja[2] + kind.vuelaA
            assertTrue(
                "${kind.name} dibujado llega a ${"%.2f".format(alto)} m y no entra en el alto normal",
                alto < Maze.altoLibreReal(Maze.ALTO_NORMAL)
            )
        }
    }

    @Test
    fun losQueVuelanNoSeQuedanColgadosDeLaNada() {
        // Un bicho que vuela tiene que tener algo que lo explique: alas que
        // bate, o patas de araña que lo sostienen. Lo que no puede es ser un
        // bulto suspendido en el aire sin nada.
        for (kind in EnemyKind.entries) {
            if (kind.vuelaA <= 0f) continue
            val piezas = ArmadoDeBichos.armar(kind, 0f, 0f)
            val alas = piezas.count {
                it.malla == ArmadoDeBichos.Malla.ALA || it.malla == ArmadoDeBichos.Malla.PATA
            }
            assertTrue("${kind.name} vuela sin alas ni patas que lo expliquen", alas >= 2)
        }
    }

    @Test
    fun elBichoEsMasAnchoQueLargoOAlReves_peroNuncaUnaAguja() {
        // Una proporcion sana: nada tan finito que de lejos sea una raya.
        for (kind in EnemyKind.entries) {
            val c = caja(kind)
            val dx = c[1] - c[0]; val dy = c[3] - c[2]; val dz = c[5] - c[4]
            val mayor = max(dx, max(dy, dz))
            val menor = minOf(dx, dy, dz)
            assertTrue(
                "${kind.name} es una aguja (${"%.2f".format(mayor)} x ${"%.2f".format(menor)})",
                menor > mayor * 0.14f
            )
            assertTrue("${kind.name} no tiene volumen", sqrt(dx * dy * dz) > 0.05f)
        }
    }
}
