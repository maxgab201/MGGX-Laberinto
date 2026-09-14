package com.mggx.laberinto

import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.gl.ArmadoDelPatio
import com.mggx.laberinto.gl.PropMeshes
import com.mggx.laberinto.maze.ElAscenso
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El patio entero, armado y mirado.
 *
 * Es el final del juego y hay uno solo, asi que se lo puede revisar de verdad:
 * componerlo completo, sacarle fotos y medirlo.
 */
class PatioArmadoTest {

    private val LADO = ElAscenso.LADO_PATIO * GameSession.CELL

    private fun geometria(): PropMeshes.Geometry {
        val piezas = ArmadoDelPatio.armar(LADO, LADO * 0.5f, 7L).map { p ->
            PropMeshes.trasladar(
                PropMeshes.rotarY(
                    PropMeshes.escalar(
                        ArmadoDelPatio.geometria(p.malla), p.escala, p.escala, p.escala
                    ),
                    Math.toDegrees(p.giro.toDouble()).toFloat()
                ),
                p.x, p.y, p.z
            )
        }
        return PropMeshes.combinar(*piezas.toTypedArray())
    }

    @Test
    fun retratarElPatioEntero() {
        val g = geometria()
        val f = VisorDeMallas.retrato(
            g, "patio-armado", lado = 420,
            angulos = floatArrayOf(0f, 55f, 180f, 270f), elevacion = 34f
        )
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists())
    }

    @Test
    fun comoSeVeAlSalirDelTunel() {
        // La foto que importa: parado en la boca de la galeria, mirando la
        // casa. Es el ultimo plano del juego.
        val g = geometria()
        // El patio se arma con la casa en z=0 y la boca de la galeria en
        // z=lado. La camara esta en el origen mirando a -Z, asi que alcanza con
        // correr el patio para atras: la boca queda en la camara y la casa,
        // enfrente. Sin giros — girarlo fue el primer intento y dejaba al
        // jugador adentro de la pared.
        val puesto = PropMeshes.trasladar(g, -LADO * 0.5f, -1.62f, -LADO + 1.2f)
        val f = VisorDeMallas.primeraPersona(puesto, "patio-desde-el-tunel", ancho = 620, alto = 300)
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists())
    }

    @Test
    fun todoLoDelPatioEstaApoyadoEnElPasto() {
        // Nada flotando y nada enterrado: el mismo criterio que se le exige a
        // los props de la cueva y a los bichos.
        for (p in ArmadoDelPatio.armar(LADO, LADO * 0.5f, 7L)) {
            if (p.malla == ArmadoDelPatio.Malla.ROPA ||
                p.malla == ArmadoDelPatio.Malla.COPA ||
                p.malla == ArmadoDelPatio.Malla.CHIMENEA ||
                p.malla == ArmadoDelPatio.Malla.VENTANA ||
                p.malla == ArmadoDelPatio.Malla.CERCO_TRAVESANO
            ) continue      // estos cuelgan o van montados, a proposito
            assertTrue("${p.malla} quedo enterrada (y=${p.y})", p.y > -0.02f)
            assertTrue("${p.malla} quedo flotando (y=${p.y})", p.y < 0.10f)
        }
    }

    @Test
    fun nadaSeSaleDelPatio() {
        for (p in ArmadoDelPatio.armar(LADO, LADO * 0.5f, 7L)) {
            assertTrue("${p.malla} se salio por x=${p.x}", p.x > -0.6f && p.x < LADO + 0.6f)
            assertTrue("${p.malla} se salio por z=${p.z}", p.z > -0.6f && p.z < LADO + 0.6f)
        }
    }

    @Test
    fun elPatioTieneLoQueTieneQueTener() {
        val piezas = ArmadoDelPatio.armar(LADO, LADO * 0.5f, 7L)
        val hay = piezas.map { it.malla }.toSet()
        for (m in ArmadoDelPatio.Malla.entries) {
            assertTrue("falta $m en el patio", m in hay)
        }
        // Y el pasto tiene que ser MUCHO: es lo que hace que se lea como pasto.
        val pasto = piezas.count { it.malla == ArmadoDelPatio.Malla.PASTO }
        assertTrue("solo $pasto matas de pasto: se va a ver pelado", pasto > 60)
    }

    @Test
    fun elCaminoVaDeLaBocaALaPuerta() {
        // El camino es la flecha del nivel: tiene que arrancar donde salis y
        // terminar en la puerta.
        val bocaX = LADO * 0.3f
        val piezas = ArmadoDelPatio.armar(LADO, bocaX, 7L)
        val losas = piezas.filter { it.malla == ArmadoDelPatio.Malla.LOSA }
        val cerca = losas.minByOrNull { it.z }!!
        val lejos = losas.maxByOrNull { it.z }!!
        assertTrue("el camino no llega a la puerta (z=${cerca.z})", cerca.z < 1.8f)
        assertTrue(
            "el camino no arranca en la boca (x=${lejos.x}, boca=$bocaX)",
            kotlin.math.abs(lejos.x - bocaX) < 1.0f
        )
    }
}
