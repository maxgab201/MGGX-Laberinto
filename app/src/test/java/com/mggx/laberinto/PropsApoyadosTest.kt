package com.mggx.laberinto

import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.gl.WorldMesh
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Que las cosas del piso esten APOYADAS en el piso.
 *
 * `maze.floorY` da la altura teorica y plana de una casilla, pero la roca que
 * se dibuja esta hasta 13 cm mas arriba o mas abajo — y la abolladura es maxima
 * justo en el CENTRO de la casilla, que es exactamente donde se apoya cada
 * objeto. Apoyando todo en la altura teorica, la mitad del nivel flotaba un
 * palmo y la otra mitad quedaba medio enterrada.
 *
 * Es el mismo bug que el de las antorchas, pero en el piso y multiplicado por
 * todo: monedas, cristales, hongos, piedras, estalagmitas, cofres, trampas,
 * estaciones de carburo y la salida.
 */
class PropsApoyadosTest {

    @Test
    fun elCentroDeLaCasillaEsJustoDondeMasSeDesviaElPiso() {
        // Esto es lo que hace que el bug se note tanto: el error no es al azar,
        // cae siempre donde se apoyan las cosas.
        val bp = MazeGenerator.generate(12, 8080L)
        val m = bp.maze
        val c = GameSession.CELL
        var peor = 0f
        var cuantas = 0
        for (gi in bp.stalagmites + bp.rocks + bp.mushrooms + bp.crystalClusters + bp.coins) {
            val gx = gi % m.gw
            val gy = gi / m.gw
            val real = WorldMesh.realFloorHeight(m, (gx + 0.5f) * c, (gy + 0.5f) * c)
            val plano = WorldMesh.floorHeight(m, gx, gy)
            peor = maxOf(peor, abs(real - plano))
            cuantas++
        }
        assertTrue("no habia objetos que revisar", cuantas > 50)
        assertTrue(
            "el piso real coincide con el teorico: no habria nada que arreglar (peor caso $peor m)",
            peor > 0.05f
        )
    }

    @Test
    fun laAlturaDeApoyoCoincideConLaRocaQueSeDibuja() {
        // Lo que importa de verdad: que el numero con el que se apoyan los
        // objetos sea el mismo que el de la malla del piso.
        val bp = MazeGenerator.generate(12, 8080L)
        val m = bp.maze
        val malla = WorldMesh.build(m, 3)
        val c = GameSession.CELL
        val paso = WorldMesh.STRIDE_FLOATS
        var revisadas = 0
        var peor = 0f

        for (gi in bp.stalagmites.take(30)) {
            val gx = gi % m.gw
            val gy = gi / m.gw
            val x = (gx + 0.5f) * c
            val z = (gy + 0.5f) * c
            val apoyo = WorldMesh.realFloorHeight(m, x, z)

            // Vertices del PISO (capa 1) cercanos, para ver a que altura anda
            // la roca ahi.
            var lo = Float.MAX_VALUE
            var hi = -Float.MAX_VALUE
            var i = 0
            while (i < malla.vertices.size) {
                if (malla.vertices[i + 9] == 1f) {
                    val vx = malla.vertices[i]
                    val vz = malla.vertices[i + 2]
                    if (abs(vx - x) < 0.8f && abs(vz - z) < 0.8f) {
                        val vy = malla.vertices[i + 1]
                        if (vy < lo) lo = vy
                        if (vy > hi) hi = vy
                    }
                }
                i += paso
            }
            if (lo > hi) continue
            revisadas++
            val fuera = maxOf(lo - apoyo, apoyo - hi, 0f)
            if (fuera > peor) peor = fuera
        }
        assertTrue("no se encontro piso para revisar", revisadas > 8)
        assertTrue("la altura de apoyo no cae sobre la roca: $peor m fuera", peor < 0.06f)
    }
}
