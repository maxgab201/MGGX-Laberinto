package com.mggx.laberinto

import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.gl.WorldMesh
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La tiza (y el rastro de pisadas) se enterraban en el piso: se apoyaban en
 * la altura TEORICA y plana de la casilla, pero el piso real tiene una
 * abolladura de ruido de hasta [WorldMesh.BULTO_PISO] metros. Estos tests
 * verifican, con numeros reales del propio ruido del juego, que la altura
 * real del piso puede diferir bastante de la teorica (confirmando que el
 * offset fijo viejo de 2.2cm no alcanzaba) y que la nueva funcion se
 * mantiene dentro de limites sanos.
 */
class PisoDecalTest {

    @Test
    fun elOffsetFijoViejoNoAlcanzabaParaCubrirElBultoDelPiso() {
        var maxDiff = 0f
        for (seed in 1L..10L) {
            val m = MazeGenerator.generate(20, seed * 777L).maze
            for (gy in 0 until m.gh) for (gx in 0 until m.gw) {
                if (m.isSolid(gx, gy)) continue
                val x = (gx + 0.5f) * GameSession.CELL
                val z = (gy + 0.5f) * GameSession.CELL
                val diff = kotlin.math.abs(
                    WorldMesh.realFloorHeight(m, x, z) - WorldMesh.floorHeight(m, gx, gy)
                )
                if (diff > maxDiff) maxDiff = diff
            }
        }
        assertTrue(
            "el piso real nunca se aparto mas de 2.2cm del teorico (max=$maxDiff), " +
                "el offset viejo no era realmente un problema",
            maxDiff > 0.022f
        )
    }

    @Test
    fun laAlturaRealNuncaSeVaMasAlaDeLaAmplitudDeclarada() {
        for (seed in 1L..8L) {
            val m = MazeGenerator.generate(15, seed * 331L).maze
            for (gy in 0 until m.gh) for (gx in 0 until m.gw) {
                if (m.isSolid(gx, gy)) continue
                // Varios puntos dentro de la casilla, no solo el centro.
                for (fx in floatArrayOf(0.2f, 0.5f, 0.8f)) {
                    for (fz in floatArrayOf(0.2f, 0.5f, 0.8f)) {
                        val x = (gx + fx) * GameSession.CELL
                        val z = (gy + fz) * GameSession.CELL
                        val diff = kotlin.math.abs(
                            WorldMesh.realFloorHeight(m, x, z) - WorldMesh.floorHeight(m, gx, gy)
                        )
                        assertTrue(
                            "en ($gx,$gy) fx=$fx fz=$fz el bulto se paso de la amplitud declarada: $diff",
                            diff <= WorldMesh.BULTO_PISO + 1e-4f
                        )
                    }
                }
            }
        }
    }

    @Test
    fun enElBordeDeLaCasillaElRuidoSeApagaYCoincideConElTeorico() {
        // cara() apaga el desplazamiento en los bordes (sin(PI*0)=0), y
        // realFloorHeight tiene que respetar exactamente lo mismo, si no las
        // marcas quedarian mal alineadas justo en el limite entre celdas.
        val m = MazeGenerator.generate(10, 5050L).maze
        var encontrado = false
        for (gy in 0 until m.gh) for (gx in 0 until m.gw) {
            if (m.isSolid(gx, gy)) continue
            encontrado = true
            val x0 = gx * GameSession.CELL
            val z0 = gy * GameSession.CELL
            assertEquals(
                WorldMesh.floorHeight(m, gx, gy),
                WorldMesh.realFloorHeight(m, x0, z0),
                1e-4f
            )
        }
        assertTrue("el laberinto de prueba no tenia ninguna casilla abierta", encontrado)
    }
}
