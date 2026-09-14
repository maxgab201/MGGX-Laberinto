package com.mggx.laberinto

import com.mggx.laberinto.gl.PaletaDelCielo
import com.mggx.laberinto.gl.Shaders
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El cielo del patio, mirado y medido.
 *
 * Se dibuja en la GPU, asi que lo que se mira aca es la copia en Kotlin
 * ([VisorDeCielo]). Ver ahi por que existe esa copia y que garantiza.
 */
class CieloTest {

    @Test
    fun retratarElCielo() {
        val f = VisorDeCielo.hoja("cielo")
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists() && f.length() > 1000)
    }

    @Test
    fun elCieloEsMasOscuroArribaQueEnElHorizonte() {
        // Es lo que hace que un cielo se lea como cielo y no como una pared
        // celeste: el azul se junta arriba y el horizonte se abre.
        val cenit = FloatArray(3)
        val horizonte = FloatArray(3)
        VisorDeCielo.color(0f, 1f, 0f, 0f, 1f, cenit)
        VisorDeCielo.color(0f, 0.02f, 1f, 0f, 1f, horizonte)
        assertTrue(
            "el cenit (${"%.2f".format(cenit[2])}) no es mas azul que el horizonte",
            cenit[2] - cenit[0] > horizonte[2] - horizonte[0] + 0.15f
        )
        assertTrue("el horizonte no aclara", horizonte[0] > cenit[0] + 0.15f)
    }

    @Test
    fun elSolEstaDondeDiceLaPaleta() {
        // Mirando justo al sol tiene que quemar, y a 20 grados ya no.
        val enElSol = FloatArray(3)
        val alLado = FloatArray(3)
        VisorDeCielo.color(
            PaletaDelCielo.SOL_X, PaletaDelCielo.SOL_Y, PaletaDelCielo.SOL_Z, 0f, 1f, enElSol
        )
        // 20 grados a un costado, en el plano horizontal.
        val a = Math.toRadians(20.0)
        val cx = PaletaDelCielo.SOL_X * Math.cos(a).toFloat() - PaletaDelCielo.SOL_Z * Math.sin(a).toFloat()
        val cz = PaletaDelCielo.SOL_X * Math.sin(a).toFloat() + PaletaDelCielo.SOL_Z * Math.cos(a).toFloat()
        VisorDeCielo.color(cx, PaletaDelCielo.SOL_Y, cz, 0f, 1f, alLado)
        assertTrue("el disco del sol no se ve", enElSol[0] > 0.93f && enElSol[1] > 0.9f)
        assertTrue(
            "el sol ilumina todo el cielo por igual: no es un sol, es un filtro",
            enElSol[0] - alLado[0] > 0.1f
        )
    }

    @Test
    fun porDebajoDelHorizonteNoHayCielo() {
        // Si no, mirando para abajo por un hueco del patio se veria celeste,
        // que es la unica forma de que un cielo se vea mal de verdad.
        val abajo = FloatArray(3)
        VisorDeCielo.color(0f, -1f, 0f, 0f, 1f, abajo)
        assertTrue("mirando al piso se ve celeste", abajo[2] < abajo[0] + 0.05f)
    }

    @Test
    fun adentroDelTunelElFondoSigueSiendoElDeLaCueva() {
        // El cielo aparece con luzDeAfuera, no de golpe al cruzar una linea.
        val adentro = FloatArray(3)
        val afuera = FloatArray(3)
        VisorDeCielo.color(0f, 0.4f, 1f, 0f, 0f, adentro)
        VisorDeCielo.color(0f, 0.4f, 1f, 0f, 1f, afuera)
        assertTrue("adentro del tunel ya se ve el cielo", adentro[0] < 0.25f && adentro[2] < 0.25f)
        assertTrue("afuera no se abre", afuera[2] > 0.5f)
    }

    @Test
    fun lasNubesSeMuevenYNoSonUnaManchaFija() {
        var distintos = 0
        val a = FloatArray(3)
        val b = FloatArray(3)
        for (k in 0 until 40) {
            val ang = k * 0.157f
            val dx = kotlin.math.cos(ang.toDouble()).toFloat()
            val dz = kotlin.math.sin(ang.toDouble()).toFloat()
            VisorDeCielo.color(dx, 0.35f, dz, 0f, 1f, a)
            VisorDeCielo.color(dx, 0.35f, dz, 600f, 1f, b)
            if (VisorDeCielo.distancia(a, b) > 0.02f) distintos++
        }
        assertTrue("el cielo es el mismo a los 10 minutos: las nubes no se mueven", distintos > 8)
    }

    @Test
    fun elShaderYLaCopiaHablanDeLasMismasCosas() {
        // No prueba que la cuenta sea identica —eso no se puede sin una GPU—
        // pero si que el shader siga teniendo las piezas que la copia imita.
        // Si alguien saca las nubes del shader y se olvida de la copia, la
        // foto que uno mira deja de ser la que ve el jugador.
        val fs = Shaders.CIELO_FS
        for (pieza in listOf("uCenit", "uHorizonte", "uSuelo", "uSolDir", "uSolColor",
                             "uAfuera", "uTiempo", "nubes(", "uSolPar", "uNubePar", "uCieloPar", "0.85")) {
            assertTrue("el shader del cielo ya no tiene '$pieza'", fs.contains(pieza))
        }
    }
}
