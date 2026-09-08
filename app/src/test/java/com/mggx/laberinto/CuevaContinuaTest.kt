package com.mggx.laberinto

import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.gl.WorldMesh
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La cueva se veia cuadrada porque el desplazamiento de la roca se apagaba en
 * los CUATRO bordes de cada cara: quedaba una arista dura cada 3 metros, como
 * una fila de almohadones. Ahora solo se apaga contra un borde donde la
 * superficie no sigue del otro lado.
 *
 * Eso trae un riesgo nuevo y concreto: si las dos casillas vecinas no se ponen
 * de acuerdo en si el borde que comparten se apaga o no, una desplaza y la
 * otra no, y se abre una GRIETA por la que se ve el vacio. Estos tests son el
 * seguro contra eso, y son la unica forma de verificarlo sin un dispositivo:
 * atacan la propiedad que garantiza que no puede pasar, que es que los
 * predicados de continuidad sean simetricos.
 */
class CuevaContinuaTest {

    private val CELL = GameSession.CELL

    private fun mazes() = (1L..6L).map { MazeGenerator.generate(18, it * 911L).maze }

    // ------------------------------------------------- simetria (anti grieta)

    @Test
    fun elPisoSeContinuaIgualMiradoDesdeLasDosCasillas() {
        for (m in mazes()) {
            for (gy in 0 until m.gh) for (gx in 0 until m.gw) {
                if (m.isSolid(gx, gy)) continue
                for (d in arrayOf(intArrayOf(1, 0), intArrayOf(0, 1))) {
                    val vx = gx + d[0]; val vy = gy + d[1]
                    if (!m.inBounds(vx, vy) || m.isSolid(vx, vy)) continue
                    assertEquals(
                        "el piso entre ($gx,$gy) y ($vx,$vy) se continua desde un lado " +
                            "pero no desde el otro: eso abre una grieta",
                        WorldMesh.pisoSigue(m, gx, gy, vx, vy),
                        WorldMesh.pisoSigue(m, vx, vy, gx, gy)
                    )
                }
            }
        }
    }

    @Test
    fun elTechoSeContinuaIgualMiradoDesdeLasDosCasillas() {
        for (m in mazes()) {
            for (gy in 0 until m.gh) for (gx in 0 until m.gw) {
                if (m.isSolid(gx, gy)) continue
                for (d in arrayOf(intArrayOf(1, 0), intArrayOf(0, 1))) {
                    val vx = gx + d[0]; val vy = gy + d[1]
                    if (!m.inBounds(vx, vy) || m.isSolid(vx, vy)) continue
                    assertEquals(
                        "el techo entre ($gx,$gy) y ($vx,$vy) no coincide en los dos sentidos",
                        WorldMesh.techoSigue(m, gx, gy, vx, vy),
                        WorldMesh.techoSigue(m, vx, vy, gx, gy)
                    )
                }
            }
        }
    }

    @Test
    fun laParedSeContinuaIgualMiradaDesdeLasDosCasillas() {
        // Solo tiene sentido donde las DOS casillas tienen pared de ese lado:
        // ahi es donde las dos emiten cara y comparten un borde vertical.
        val lados = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
        var comprobados = 0
        for (m in mazes()) {
            for (gy in 0 until m.gh) for (gx in 0 until m.gw) {
                if (m.isSolid(gx, gy)) continue
                for (s in lados) {
                    if (!m.isSolid(gx + s[0], gy + s[1])) continue
                    // Vecinos a lo largo de esa pared: los perpendiculares al lado.
                    val avance = if (s[0] != 0) intArrayOf(0, 1) else intArrayOf(1, 0)
                    for (signo in intArrayOf(-1, 1)) {
                        val vx = gx + avance[0] * signo
                        val vy = gy + avance[1] * signo
                        if (!m.inBounds(vx, vy) || m.isSolid(vx, vy)) continue
                        if (!m.isSolid(vx + s[0], vy + s[1])) continue
                        comprobados++
                        assertEquals(
                            "la pared del lado (${s[0]},${s[1]}) entre ($gx,$gy) y ($vx,$vy) " +
                                "se continua desde un lado pero no desde el otro",
                            WorldMesh.paredSigue(m, gx, gy, vx, vy, s[0], s[1]),
                            WorldMesh.paredSigue(m, vx, vy, gx, gy, s[0], s[1])
                        )
                    }
                }
            }
        }
        assertTrue("el laberinto de prueba no tenia ningun tramo de pared seguido", comprobados > 100)
    }

    // ------------------------------- el piso real, que es el que se puede medir

    @Test
    fun elPisoRealNoDaUnSaltoAlCruzarAUnaCasillaQueSigue() {
        // La prueba de fuego contra las grietas, con la funcion publica: si las
        // dos casillas no coincidieran en la mascara, cruzar el borde daria un
        // salto de hasta BULTO_PISO metros.
        val eps = 1e-3f
        var cruces = 0
        for (m in mazes()) {
            for (gy in 0 until m.gh) for (gx in 0 until m.gw) {
                if (m.isSolid(gx, gy)) continue
                // Solo bordes donde el piso sigue: los otros se apagan a
                // proposito y ahi el salto no existe porque las dos dan fy.
                if (WorldMesh.pisoSigue(m, gx, gy, gx + 1, gy)) {
                    val xb = (gx + 1) * CELL
                    val z = (gy + 0.5f) * CELL
                    val a = WorldMesh.realFloorHeight(m, xb - eps, z)
                    val b = WorldMesh.realFloorHeight(m, xb + eps, z)
                    cruces++
                    assertTrue(
                        "cruzando en x=$xb (casillas ($gx,$gy)-(${gx + 1},$gy)) el piso salta " +
                            "${kotlin.math.abs(a - b)} m",
                        kotlin.math.abs(a - b) < 0.01f
                    )
                }
                if (WorldMesh.pisoSigue(m, gx, gy, gx, gy + 1)) {
                    val zb = (gy + 1) * CELL
                    val x = (gx + 0.5f) * CELL
                    val a = WorldMesh.realFloorHeight(m, x, zb - eps)
                    val b = WorldMesh.realFloorHeight(m, x, zb + eps)
                    cruces++
                    assertTrue(
                        "cruzando en z=$zb (casillas ($gx,$gy)-($gx,${gy + 1})) el piso salta " +
                            "${kotlin.math.abs(a - b)} m",
                        kotlin.math.abs(a - b) < 0.01f
                    )
                }
            }
        }
        assertTrue("no se probo ningun cruce entre casillas", cruces > 200)
    }

    @Test
    fun elRuidoYaNoSeApagaEnLosBordesDondeElPisoSigue() {
        // Esta es la mejora en si: antes el piso volvia SIEMPRE a la altura
        // teorica en cada borde de casilla, y eso era la arista cada 3 metros.
        var maxEnBorde = 0f
        for (m in mazes()) {
            for (gy in 0 until m.gh) for (gx in 0 until m.gw) {
                if (m.isSolid(gx, gy)) continue
                if (!WorldMesh.pisoSigue(m, gx, gy, gx + 1, gy)) continue
                val xb = (gx + 1) * CELL - 1e-3f
                val z = (gy + 0.5f) * CELL
                val d = kotlin.math.abs(
                    WorldMesh.realFloorHeight(m, xb, z) - WorldMesh.floorHeight(m, gx, gy)
                )
                if (d > maxEnBorde) maxEnBorde = d
            }
        }
        assertTrue(
            "en los bordes que siguen el piso vuelve a la altura teorica " +
                "(max=$maxEnBorde): la arista cada casilla sigue ahi",
            maxEnBorde > 0.03f
        )
    }

    @Test
    fun contraLaRocaElPisoSiSigueApagandose() {
        // Y al reves: contra una pared el desplazamiento TIENE que valer cero,
        // porque ahi el piso se encuentra con una cara de otra normal.
        var comprobados = 0
        for (m in mazes()) {
            for (gy in 0 until m.gh) for (gx in 0 until m.gw) {
                if (m.isSolid(gx, gy)) continue
                if (!m.isSolid(gx - 1, gy)) continue
                val x = gx * CELL
                val z = (gy + 0.5f) * CELL
                comprobados++
                assertEquals(
                    "contra la roca en ($gx,$gy) el piso no se apago",
                    WorldMesh.floorHeight(m, gx, gy),
                    WorldMesh.realFloorHeight(m, x, z),
                    1e-4f
                )
            }
        }
        assertTrue("no habia ninguna pared para probar", comprobados > 50)
    }
}
