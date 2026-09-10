package com.mggx.laberinto

import com.mggx.laberinto.ui.MinimapMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * La cuenta del minimapa que rota con la camara.
 *
 * Los primeros cuatro casos solo miran que el frente quede arriba, y con eso
 * NO alcanza: una rotacion con espejo tambien pone el frente arriba, y era
 * justo lo que estaba pasando (lo de la derecha se dibujaba a la izquierda).
 * Los casos de lateralidad de mas abajo son los que fijan eso.
 */
class MinimapMathTest {

    /**
     * La derecha del jugador en el mundo, en el plano XZ.
     *
     * Es la misma que arma `Matrix.setLookAtM` en CaveRenderer para la vista
     * en 3D: `frente x arriba`, con frente `(sin yaw, 0, cos yaw)` y arriba
     * `(0, 1, 0)`, o sea `(-cos yaw, 0, sin yaw)`.
     */
    private fun derechaDelJugador(yawDeg: Float): Pair<Float, Float> {
        val yaw = Math.toRadians(yawDeg.toDouble())
        return -cos(yaw).toFloat() to sin(yaw).toFloat()
    }

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
    fun laDerechaDelJuegoQuedaALaDerechaDelMapa() {
        // La invariante que faltaba. Lo que el jugador tiene a su derecha en
        // la vista en 3D tiene que caer a la derecha del minimapa, para
        // cualquier yaw. Sin esto, el mapa era un espejo: doblabas a la
        // derecha y el muñeco doblaba para el otro lado.
        for (yawDeg in floatArrayOf(0f, 30f, 90f, 143f, 180f, 250f, 300f, -70f, 415f)) {
            val (dx, dz) = derechaDelJugador(yawDeg)
            val (sx, sy) = MinimapMath.rotarHaciaArriba(dx, dz, yawDeg)
            assertEquals("yaw=$yawDeg sx", 1f, sx, 0.01f)
            assertEquals("yaw=$yawDeg sy", 0f, sy, 0.01f)
        }
    }

    @Test
    fun laIzquierdaDelJuegoQuedaALaIzquierdaDelMapa() {
        for (yawDeg in floatArrayOf(0f, 66f, 121f, 200f, 340f)) {
            val (dx, dz) = derechaDelJugador(yawDeg)
            val (sx, sy) = MinimapMath.rotarHaciaArriba(-dx, -dz, yawDeg)
            assertEquals("yaw=$yawDeg sx", -1f, sx, 0.01f)
            assertEquals("yaw=$yawDeg sy", 0f, sy, 0.01f)
        }
    }

    @Test
    fun noEsUnEspejo() {
        // Lo mismo dicho de una vez: el determinante de la transformacion
        // tiene que ser +1 (rotacion). Si diera -1 seria una rotacion con
        // espejo, que cumple lo del frente arriba pero da vuelta los
        // costados.
        for (yawDeg in floatArrayOf(0f, 23f, 90f, 177f, 260f, -40f)) {
            // Las columnas de la matriz: adonde van (1,0) y (0,1).
            val (axx, axy) = MinimapMath.rotarHaciaArriba(1f, 0f, yawDeg)
            val (azx, azy) = MinimapMath.rotarHaciaArriba(0f, 1f, yawDeg)
            val det = axx * azy - azx * axy
            assertEquals("yaw=$yawDeg: la transformacion espeja", 1f, det, 0.01f)
        }
    }

    @Test
    fun loQueEstaLejosSePegaAlBordeSinCambiarDeRumbo() {
        // La marca de la salida (con la Rosa de los Vientos) o de un tesoro
        // (con el Ojo de la Veta) tiene que seguir sirviendo cuando esta mas
        // lejos que el radar: se pega al borde, pero apuntando exactamente
        // para donde esta. Si el rumbo cambiara al pegarla, la marca mentiria.
        val (px, py, pegada) = MinimapMath.pegarAlBorde(30f, 40f, 10f)
        assertTrue("no la pego al borde", pegada)
        assertEquals("no quedo sobre el borde", 10f, kotlin.math.hypot(px, py), 0.001f)
        // Mismo rumbo: el producto cruzado con el original tiene que ser cero.
        assertEquals("le cambio el rumbo", 0f, 30f * py - 40f * px, 0.001f)
        assertTrue("la dio vuelta", px > 0f && py > 0f)
    }

    @Test
    fun loQueEntraEnElRadarNoSeToca() {
        val (px, py, pegada) = MinimapMath.pegarAlBorde(3f, -4f, 10f)
        assertTrue("pego al borde algo que entraba", !pegada)
        assertEquals(3f, px, 0.001f)
        assertEquals(-4f, py, 0.001f)
    }

    @Test
    fun laDistanciaSeConserva() {
        // Rotar no puede estirar ni encoger: mismo modulo antes y despues.
        val (sx, sy) = MinimapMath.rotarHaciaArriba(dx = 3f, dz = 4f, yawDeg = 57f)
        val distOriginal = kotlin.math.hypot(3f, 4f)
        val distRotada = kotlin.math.hypot(sx, sy)
        assertEquals(distOriginal, distRotada, 0.01f)
    }

    @Test
    fun elAnguloEntreDosPuntosNoCambia() {
        // Un espejo conserva las distancias pero da vuelta el signo de los
        // angulos. Este caso lo agarra aunque el de la distancia pase.
        val yawDeg = 37f
        val (ax, ay) = MinimapMath.rotarHaciaArriba(2f, 0f, yawDeg)
        val (bx, by) = MinimapMath.rotarHaciaArriba(0f, 2f, yawDeg)
        val cruz = ax * by - ay * bx
        assertTrue("los angulos salen dados vuelta: cruz=$cruz", cruz > 0f)
    }
}
