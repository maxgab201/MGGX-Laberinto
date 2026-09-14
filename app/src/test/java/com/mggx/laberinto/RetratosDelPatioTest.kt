package com.mggx.laberinto

import com.mggx.laberinto.gl.PatioMeshes
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las fotos de cada cosa del patio.
 *
 * `./gradlew :app:testDebugUnitTest --tests '*RetratosDelPatioTest*'` y mirar
 * `build/modelos/patio.png`.
 */
class RetratosDelPatioTest {

    @Test
    fun retratarElPatio() {
        val f = VisorDeMallas.hoja(
            listOf(
                "pasto" to PatioMeshes.mataDePasto(),
                "puerta" to PatioMeshes.puertaDeCasa(),
                "ventana" to PatioMeshes.ventanaDeCasa(),
                "alero" to PatioMeshes.aleroDeTejas(),
                "cerco" to PatioMeshes.tablaDeCerco(),
                "tronco" to PatioMeshes.troncoDeArbol(),
                "copa" to PatioMeshes.copaDeArbol()
            ),
            "patio", lado = 230
        )
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists() && f.length() > 1000)
    }

    @Test
    fun retratarLosMuebles() {
        val f = VisorDeMallas.hoja(
            listOf(
                "banco" to PatioMeshes.bancoDeMadera(),
                "mesa" to PatioMeshes.mesaDePatio(),
                "silla" to PatioMeshes.sillaDePatio(),
                "maceta" to PatioMeshes.macetaConPlanta(),
                "ropa" to PatioMeshes.ropaTendida(),
                "balde" to PatioMeshes.baldeDeMina(),
                "pared" to PatioMeshes.paredDeCasa()
            ),
            "patio-muebles", lado = 230
        )
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists() && f.length() > 1000)
    }
}
