package com.mggx.laberinto

import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.gl.PropMeshes
import com.mggx.laberinto.maze.MazeGenerator.EnemyKind
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las fotos de cada bicho ENTERO, que hasta ahora no se podian sacar.
 *
 * `./gradlew :app:testDebugUnitTest --tests '*RetratosDeBichosTest*'` y mirar
 * `build/modelos/`.
 */
class RetratosDeBichosTest {

    @Test
    fun retratarLosBichosArmados() {
        val fotos = EnemyKind.entries.map { it.name to BichoArmado.geometria(it) }
        val f = VisorDeMallas.hoja(fotos, "bichos-armados", lado = 260)
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists() && f.length() > 1000)
    }

    @Test
    fun retratarLosBichosComoLosVeElJugador() {
        // La foto que importa: el bicho a la distancia a la que lo vas a ver,
        // que es la distancia a la que te muerde, y a la altura de tus ojos.
        for (kind in EnemyKind.entries) {
            val g = BichoArmado.geometria(kind)
            val distancia = kind.radio + GameSession.PLAYER_RADIUS + 0.18f + 1.2f
            val alturaOjos = 1.62f
            // La camara mira a -Z desde el origen, asi que el bicho se pone
            // adelante y se baja a la altura de los ojos. El que vuela va a su
            // altura de vuelo: si no, el murcielago quedaba abajo del cuadro y
            // la foto salia negra.
            val puesto = PropMeshes.trasladar(g, 0f, kind.vuelaA - alturaOjos, -distancia)
            val f = VisorDeMallas.primeraPersona(puesto, "bicho-ojo-${kind.name}", ancho = 460, alto = 260)
            println("FOTO: ${f.absolutePath}")
            assertTrue(f.exists())
        }
    }
}
