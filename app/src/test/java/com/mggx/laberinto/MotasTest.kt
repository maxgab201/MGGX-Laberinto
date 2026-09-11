package com.mggx.laberinto

import com.mggx.laberinto.gl.Motas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El polvo que flota en el aire de la cueva.
 *
 * Lo que hace barato esto es que las motas NO se guardan ni se simulan: hay
 * una cantidad fija, su posicion es funcion del numero de mota y del tiempo, y
 * se envuelve en una caja centrada en el jugador. Eso solo funciona si la
 * envoltura es correcta, y de eso se trata casi todo este test.
 */
class MotasTest {

    private val p = FloatArray(3)

    @Test
    fun siempreQuedanAlrededorDelJugador() {
        // La prueba de fuego: camines a donde camines, incluso lejisimos del
        // origen y en negativo, todas las motas tienen que seguir estando en la
        // caja que te rodea. Si alguna se escapara, el polvo se quedaria atras
        // y la cueva volveria a verse vacia.
        val puntos = arrayOf(
            floatArrayOf(0f, 0f, 0f),
            floatArrayOf(137.4f, 2.1f, -88.9f),
            floatArrayOf(-500f, -12f, 500f),
            floatArrayOf(3.2f, 1.8f, 4.7f)
        )
        for (c in puntos) {
            for (t in floatArrayOf(0f, 3.7f, 120f, 5000f)) {
                for (i in 0 until 260) {
                    Motas.posicion(i, t, c[0], c[1], c[2], p)
                    for (eje in 0..2) {
                        val d = p[eje] - c[eje]
                        assertTrue(
                            "la mota $i se escapo de la caja en t=$t (eje $eje, distancia $d)",
                            kotlin.math.abs(d) <= Motas.LADO * 0.5f + 0.001f
                        )
                    }
                }
            }
        }
    }

    @Test
    fun envolverNoDejaHuecosNiEnNegativo() {
        // Con el resto de `%` en vez de `mod`, una mota detras del origen
        // saltaba al otro extremo de la caja: se veia como si el polvo
        // desapareciera de un lado de la galeria.
        for (centro in floatArrayOf(0f, 50f, -50f, 7.3f)) {
            for (v in floatArrayOf(-1000f, -7f, 0f, 3f, 1000f)) {
                val r = Motas.envolver(v, centro)
                assertTrue(
                    "envolver($v, $centro) = $r quedo fuera de la caja",
                    r >= centro - Motas.LADO * 0.5f - 0.001f &&
                        r <= centro + Motas.LADO * 0.5f + 0.001f
                )
            }
        }
    }

    @Test
    fun envolverNoMueveLoQueYaEstabaAdentro() {
        val centro = 20f
        for (d in floatArrayOf(-6f, -1f, 0f, 1f, 6f)) {
            assertEquals(centro + d, Motas.envolver(centro + d, centro), 0.001f)
        }
    }

    @Test
    fun lasMotasSeMuevenPeroDespacio() {
        // El aire de una cueva se mueve, pero despacio. Una mota que cruza la
        // pantalla en un segundo se lee como un bicho, no como polvo.
        var maxima = 0f
        for (i in 0 until 200) {
            Motas.posicion(i, 10f, 0f, 0f, 0f, p)
            val x0 = p[0]; val y0 = p[1]; val z0 = p[2]
            Motas.posicion(i, 11f, 0f, 0f, 0f, p)
            // Se saltea la que justo se envolvio en este segundo: ahi el salto
            // es del envoltorio, no del movimiento.
            val dx = kotlin.math.abs(p[0] - x0)
            val dy = kotlin.math.abs(p[1] - y0)
            val dz = kotlin.math.abs(p[2] - z0)
            if (dx > Motas.LADO * 0.5f || dy > Motas.LADO * 0.5f || dz > Motas.LADO * 0.5f) continue
            val v = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            if (v > maxima) maxima = v
        }
        assertTrue("las motas no se mueven nada", maxima > 0.01f)
        assertTrue("las motas van demasiado rapido ($maxima m/s)", maxima < 1.2f)
    }

    @Test
    fun noSeMuevenTodasIgual() {
        // Si todas siguieran el mismo camino se veria el patron enseguida:
        // parecerian una grilla flotando, no polvo.
        val desplazamientos = HashSet<Int>()
        for (i in 0 until 120) {
            Motas.posicion(i, 0f, 0f, 0f, 0f, p)
            val x0 = p[0]
            Motas.posicion(i, 4f, 0f, 0f, 0f, p)
            desplazamientos.add(Math.round((p[0] - x0) * 500f))
        }
        assertTrue("todas las motas se mueven igual", desplazamientos.size > 60)
    }

    @Test
    fun estanRepartidasPorTodaLaCaja() {
        // Amontonadas en una esquina no servirian de nada. Se parte la caja en
        // ocho y se controla que ninguna parte quede vacia.
        val octantes = IntArray(8)
        for (i in 0 until 260) {
            Motas.posicion(i, 0f, 0f, 0f, 0f, p)
            var idx = 0
            if (p[0] > 0f) idx = idx or 1
            if (p[1] > 0f) idx = idx or 2
            if (p[2] > 0f) idx = idx or 4
            octantes[idx]++
        }
        for ((i, n) in octantes.withIndex()) {
            assertTrue("el octante $i quedo vacio de polvo", n > 5)
        }
    }

    @Test
    fun enCalidadBajaNoHayPolvo() {
        // En calidad baja el problema es el telefono, no el ambiente.
        assertEquals(0, Motas.cuantas(0))
        assertTrue(Motas.cuantas(1) > 0)
        // Y a mas calidad, mas polvo.
        assertTrue(Motas.cuantas(2) > Motas.cuantas(1))
        assertTrue(Motas.cuantas(3) > Motas.cuantas(2))
    }

    @Test
    fun elTamanoYElBrilloSonRazonables() {
        for (i in 0 until 260) {
            val t = Motas.tamano(i)
            assertTrue("la mota $i mide $t: o no se ve o es una pelota", t in 0.004f..0.04f)
            for (tiempo in floatArrayOf(0f, 1.3f, 99f)) {
                val b = Motas.brillo(i, tiempo)
                assertTrue("brillo fuera de rango en la mota $i", b in 0f..1f)
            }
        }
    }

    @Test
    fun titilan() {
        // Una mota de polvo real gira sobre si misma y cambia de cara. Sin el
        // titileo se ven como puntos pegados en el aire.
        var cambia = 0
        for (i in 0 until 60) {
            if (kotlin.math.abs(Motas.brillo(i, 0f) - Motas.brillo(i, 0.9f)) > 0.05f) cambia++
        }
        assertTrue("las motas no titilan", cambia > 30)
    }

    @Test
    fun laMismaMotaEnElMismoInstanteEstaSiempreEnElMismoLugar() {
        // No hay estado guardado: la posicion es funcion pura. Si no lo fuera,
        // las motas saltarian de lugar entre cuadro y cuadro.
        val a = FloatArray(3)
        Motas.posicion(42, 7.5f, 1f, 2f, 3f, a)
        repeat(5) {
            Motas.posicion(42, 7.5f, 1f, 2f, 3f, p)
            assertEquals(a[0], p[0], 0f)
            assertEquals(a[1], p[1], 0f)
            assertEquals(a[2], p[2], 0f)
        }
    }
}
