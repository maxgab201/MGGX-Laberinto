package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fija la convencion de ejes de la camara y del movimiento.
 *
 * La camara se arma con Matrix.setLookAtM, que calcula el vector lateral como
 * "frente x arriba". Con frente = (sin yaw, 0, cos yaw) y arriba = (0, 1, 0),
 * la derecha de la pantalla es (-cos yaw, 0, sin yaw). Todo el movimiento
 * horizontal y el giro de camara tienen que respetar eso: si se invierte el
 * signo, el juego se maneja al reves y no hay nada que avise.
 */
class EjesTest {

    private fun sesion(level: Int = 1) =
        GameSession(SaveData.fromStore(SaveData.memoryStore()), level, 12345L)

    /** El mismo vector lateral que arma setLookAtM: frente x arriba. */
    private fun derechaDePantalla(yawDeg: Float): Pair<Float, Float> {
        val r = Math.toRadians(yawDeg.toDouble())
        val fx = sin(r).toFloat()
        val fz = cos(r).toFloat()
        return Pair(-fz, fx)
    }

    /** Un paso chiquito desde el centro de una celda nunca llega a la pared. */
    private fun pasoCorto(s: GameSession, moveX: Float, moveY: Float) {
        s.update(1f / 120f, GameSession.Input(moveX = moveX, moveY = moveY))
    }

    @Test
    fun caminarDeCostadoVaHaciaLaDerechaDeLaPantalla() {
        for (yaw in intArrayOf(0, 45, 90, 135, 180, 225, 270, 315)) {
            val s = sesion()
            s.yawDeg = yaw.toFloat()
            pasoCorto(s, moveX = 1f, moveY = 0f)

            val (rx, rz) = derechaDePantalla(yaw.toFloat())
            val proyeccion = s.velX * rx + s.velZ * rz
            assertTrue(
                "con yaw=$yaw empujar a la derecha movio para el otro lado " +
                    "(vel=${s.velX},${s.velZ} derecha=$rx,$rz)",
                proyeccion > 0.1f
            )
        }
    }

    @Test
    fun caminarDeCostadoALaIzquierdaVaAlOtroLado() {
        val s = sesion()
        s.yawDeg = 0f
        pasoCorto(s, moveX = -1f, moveY = 0f)
        val (rx, rz) = derechaDePantalla(0f)
        assertTrue("empujar a la izquierda no fue a la izquierda", s.velX * rx + s.velZ * rz < -0.1f)
    }

    @Test
    fun caminarDeFrenteVaHaciaAdelante() {
        for (yaw in intArrayOf(0, 90, 180, 270)) {
            val s = sesion()
            s.yawDeg = yaw.toFloat()
            pasoCorto(s, moveX = 0f, moveY = 1f)
            val r = Math.toRadians(yaw.toDouble())
            val fx = sin(r).toFloat()
            val fz = cos(r).toFloat()
            assertTrue(
                "con yaw=$yaw caminar de frente no fue hacia adelante",
                s.velX * fx + s.velZ * fz > 0.1f
            )
        }
    }

    @Test
    fun arrastrarHaciaLaDerechaGiraALaDerecha() {
        for (yaw in intArrayOf(0, 90, 180, 270)) {
            val s = sesion()
            s.yawDeg = yaw.toFloat()
            val (rx, rz) = derechaDePantalla(yaw.toFloat())

            s.update(1f / 60f, GameSession.Input(lookX = 25f))

            val r = Math.toRadians(s.yawDeg.toDouble())
            val nuevoFx = sin(r).toFloat()
            val nuevoFz = cos(r).toFloat()
            assertTrue(
                "con yaw=$yaw arrastrar a la derecha giro para el otro lado " +
                    "(quedo en ${s.yawDeg})",
                nuevoFx * rx + nuevoFz * rz > 0.1f
            )
        }
    }

    @Test
    fun arrastrarHaciaLaIzquierdaGiraALaIzquierda() {
        val s = sesion()
        s.yawDeg = 0f
        val (rx, rz) = derechaDePantalla(0f)
        s.update(1f / 60f, GameSession.Input(lookX = -25f))
        val r = Math.toRadians(s.yawDeg.toDouble())
        assertTrue(
            "arrastrar a la izquierda no giro a la izquierda",
            sin(r).toFloat() * rx + cos(r).toFloat() * rz < -0.1f
        )
    }

    @Test
    fun mirarHaciaArribaSubeLaCamara() {
        val s = sesion()
        val antes = s.pitchDeg
        s.update(1f / 60f, GameSession.Input(lookY = 20f))
        assertTrue("lookY positivo no levanto la mirada", s.pitchDeg > antes)
    }

    @Test
    fun laBrujulaMarcaDerechaConSignoPositivo() {
        val s = sesion(7)
        // Mirando de frente a la salida el rumbo es cero.
        val dx = s.exitWorldX - s.posX
        val dz = s.exitWorldZ - s.posZ
        s.yawDeg = Math.toDegrees(Math.atan2(dx.toDouble(), dz.toDouble())).toFloat()
        assertEquals(0f, s.bearingToExit(), 1.5f)

        // Si giro a la izquierda, la salida me queda a la derecha (positivo).
        val (rx, rz) = derechaDePantalla(s.yawDeg)
        s.update(1f / 60f, GameSession.Input(lookX = -40f))
        assertTrue(
            "tras girar a la izquierda la brujula no marca la salida a la derecha",
            s.bearingToExit() > 5f
        )
        assertTrue(rx * rx + rz * rz > 0.5f)
    }
}
