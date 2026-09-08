package com.mggx.laberinto

import com.mggx.laberinto.ui.MinimapMath
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class MinimapMathTest {

    @Test
    fun conYawCeroElFrenteEsMasZQuedaArriba() {
        // Con yaw=0, el frente del jugador es (sin 0, cos 0) = (0, 1): mas Z.
        val (sx, sy) = MinimapMath.rotarHaciaArriba(dx = 0f, dz = 1f, yawDeg = 0f)
        assertEquals(0f, sx, 0.001f)
        assertEquals(-1f, sy, 0.001f)
    }

    @Test
    fun elFrenteDelJugadorQuedaArribaParaCualquierYaw() {
        for (yawDeg in floatArrayOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, -35f, 400f)) {
            val yaw = Math.toRadians(yawDeg.toDouble())
            val dx = sin(yaw).toFloat()
            val dz = cos(yaw).toFloat()
            val (sx, sy) = MinimapMath.rotarHaciaArriba(dx, dz, yawDeg)
            assertEquals("yaw=$yawDeg sx", 0f, sx, 0.01f)
            assertEquals("yaw=$yawDeg sy", -1f, sy, 0.01f)
        }
    }

    @Test
    fun elDorsoDelJugadorQuedaAbajo() {
        // Detras del jugador (dz negativo con yaw=0) tiene que quedar abajo.
        val (_, sy) = MinimapMath.rotarHaciaArriba(dx = 0f, dz = -1f, yawDeg = 0f)
        assertEquals(1f, sy, 0.001f)
    }

    @Test
    fun conYaw90ElEsteQuedaArriba() {
        // Mirando al este (yaw=90, frente=(1,0)), lo que esta al este del
        // jugador (dx=1) tiene que quedar arriba en pantalla.
        val (sx, sy) = MinimapMath.rotarHaciaArriba(dx = 1f, dz = 0f, yawDeg = 90f)
        assertEquals(0f, sx, 0.01f)
        assertEquals(-1f, sy, 0.01f)
    }

    @Test
    fun laDistanciaSeConserva() {
        // Rotar no puede estirar ni encoger: mismo modulo antes y despues.
        val (sx, sy) = MinimapMath.rotarHaciaArriba(dx = 3f, dz = 4f, yawDeg = 57f)
        val distOriginal = kotlin.math.hypot(3f, 4f)
        val distRotada = kotlin.math.hypot(sx, sy)
        assertEquals(distOriginal, distRotada, 0.01f)
    }
}
