package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.gl.WorldMesh
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Que los bichos caminen sobre la roca que se dibuja.
 *
 * `maze.floorY` da la altura teorica y plana de una casilla; la roca de verdad
 * se abolla hasta 13 cm, con el maximo justo en el centro de la casilla. Los
 * bichos usaban el plano teorico, asi que flotaban o se hundian un palmo.
 *
 * Se destapo con la sombra de contacto de la 1.9.3: la sombra si va sobre la
 * roca real, asi que el bicho y su propia sombra quedaban separados en
 * vertical. Un bicho despegado de su sombra se ve peor que uno sin sombra.
 */
class BichosEnElPisoTest {

    private fun sesion(level: Int = 9, semilla: Long = 4242L) =
        GameSession(SaveData.fromStore(SaveData.memoryStore()), level, semilla)

    @Test
    fun elBichoCaminaDondeSeDibujaLaRocaYNoSobreElPlanoTeorico() {
        val s = sesion()
        val m = s.maze
        val c = GameSession.CELL
        // La misma tabla que arma el renderer para apoyar todo lo demas.
        s.alturaDeApoyo = { gx, gy ->
            WorldMesh.realFloorHeight(m, (gx + 0.5f) * c, (gy + 0.5f) * c)
        }

        val quieto = GameSession.Input()
        repeat(30) { s.update(0.05f, quieto) }

        var revisados = 0
        var peor = 0f
        for (e in s.enemies) {
            if (!e.vivo || e.kind.vuelaA > 0f) continue
            val gx = (e.x / c).toInt().coerceIn(0, m.gw - 1)
            val gy = (e.z / c).toInt().coerceIn(0, m.gh - 1)
            val roca = WorldMesh.realFloorHeight(m, (gx + 0.5f) * c, (gy + 0.5f) * c)
            peor = maxOf(peor, abs(e.altura - roca))
            revisados++
        }
        assertTrue("no habia bichos de piso que revisar", revisados > 0)
        assertTrue("un bicho quedo a $peor m de la roca", peor < 0.01f)
    }

    @Test
    fun sinRendererSeUsaElMapaPlanoYNoRevienta() {
        // Un test de JVM no tiene renderer que le arme la tabla. Tiene que
        // seguir andando con el piso teorico.
        val s = sesion()
        val quieto = GameSession.Input()
        repeat(30) { s.update(0.05f, quieto) }
        for (e in s.enemies) {
            if (!e.vivo || e.kind.vuelaA > 0f) continue
            val c = GameSession.CELL
            val gx = (e.x / c).toInt().coerceIn(0, s.maze.gw - 1)
            val gy = (e.z / c).toInt().coerceIn(0, s.maze.gh - 1)
            assertEquals(s.maze.floorY(gx, gy), e.altura, 0.001f)
        }
    }

    @Test
    fun laDiferenciaEntreLosDosPisosEsGrandeComoParaVerse() {
        // Si el piso teorico y el dibujado dieran lo mismo, esto no seria un
        // bug y el arreglo no serviria de nada.
        val s = sesion()
        val m = s.maze
        val c = GameSession.CELL
        var peor = 0f
        for (e in s.enemies) {
            val gx = (e.x / c).toInt().coerceIn(0, m.gw - 1)
            val gy = (e.z / c).toInt().coerceIn(0, m.gh - 1)
            val roca = WorldMesh.realFloorHeight(m, (gx + 0.5f) * c, (gy + 0.5f) * c)
            peor = maxOf(peor, abs(roca - m.floorY(gx, gy)))
        }
        assertTrue("los dos pisos coinciden: no habria nada que arreglar ($peor m)", peor > 0.04f)
    }
}
