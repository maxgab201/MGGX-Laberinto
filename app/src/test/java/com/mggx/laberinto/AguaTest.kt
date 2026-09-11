package com.mggx.laberinto

import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.gl.WaterMesh
import com.mggx.laberinto.maze.AguaDeLaCueva
import com.mggx.laberinto.maze.Maze
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * El agua de la cueva.
 *
 * Las dos promesas que no se pueden romper: que nunca tape el paso y que nunca
 * moje ni el arranque ni la salida. Un charco que ahoga, o aparecer con los
 * pies en el agua, se lee como un bug aunque sea a proposito.
 */
class AguaTest {

    private fun perfil(): SaveData = SaveData.fromStore(SaveData.memoryStore())

    @Test
    fun losPrimerosNivelesEstanSecos() {
        for (level in 1 until AguaDeLaCueva.NIVEL_DESDE) {
            for (s in 0L until 6L) {
                val m = MazeGenerator.generate(level, level * 31L + s).maze
                assertEquals(
                    "el nivel $level no tendria que tener agua",
                    AguaDeLaCueva.SIN_AGUA, m.waterY, 0f
                )
            }
        }
    }

    @Test
    fun masAbajoHayMasCuevasConAgua() {
        fun conAgua(desde: Int, hasta: Int): Int {
            var n = 0
            for (level in desde..hasta) {
                for (s in 0L until 4L) {
                    if (MazeGenerator.generate(level, level * 77L + s).maze.waterY !=
                        AguaDeLaCueva.SIN_AGUA
                    ) n++
                }
            }
            return n
        }
        val arriba = conAgua(4, 14)
        val abajo = conAgua(40, 50)
        assertTrue("nunca aparece agua en ningun lado", abajo > 0)
        assertTrue("la humedad no aumenta al bajar ($arriba -> $abajo)", abajo > arriba)
    }

    @Test
    fun nuncaHayAguaEnElArranqueNiEnLaSalida() {
        // Aparecer chapoteando, o tener que terminar el nivel metido en el
        // agua, se lee como un error aunque este hecho a proposito.
        for (level in 4..60) {
            for (s in 0L until 3L) {
                val m = MazeGenerator.generate(level, level * 401L + s).maze
                if (m.waterY == AguaDeLaCueva.SIN_AGUA) continue
                assertFalse(
                    "el nivel $level arranca dentro del agua",
                    m.hayAgua(m.startGx, m.startGy)
                )
                assertFalse(
                    "la salida del nivel $level esta bajo el agua",
                    m.hayAgua(m.exitGx, m.exitGy)
                )
            }
        }
    }

    @Test
    fun elAguaNuncaTapaElPaso() {
        // Se vadea caminando: el agua se queda por debajo de lo que mide el
        // cuerpo agachado, asi que ni hay que nadar ni hay que saltar.
        val agachado = com.mggx.laberinto.game.Postura.AGACHADO.alturaCuerpo
        assertTrue(
            "el agua es mas honda que el cuerpo agachado",
            AguaDeLaCueva.PROFUNDIDAD < agachado
        )
        for (level in 4..60) {
            val m = MazeGenerator.generate(level, level * 909L).maze
            if (m.waterY == AguaDeLaCueva.SIN_AGUA) continue
            for (gy in 0 until m.gh) {
                for (gx in 0 until m.gw) {
                    val h = AguaDeLaCueva.hondura(m, m.waterY, gx, gy)
                    assertTrue(
                        "hay agua de $h m en el nivel $level: eso ya es nadar",
                        h <= AguaDeLaCueva.PROFUNDIDAD + 0.001f
                    )
                }
            }
        }
    }

    @Test
    fun elAguaSeJuntaEnLoHondoYNoEnLoAlto() {
        // Es una napa, no charcos puestos a dedo: si una casilla esta mojada,
        // cualquier casilla mas honda que ella tambien tiene que estarlo.
        for (level in 8..40) {
            val m = MazeGenerator.generate(level, level * 1313L).maze
            val nivel = m.waterY
            if (nivel == AguaDeLaCueva.SIN_AGUA) continue
            var masAltaMojada = -Float.MAX_VALUE
            var masBajaSeca = Float.MAX_VALUE
            for (gy in 0 until m.gh) {
                for (gx in 0 until m.gw) {
                    if (m.isSolid(gx, gy)) continue
                    val y = m.floorY(gx, gy)
                    if (m.hayAgua(gx, gy)) { if (y > masAltaMojada) masAltaMojada = y }
                    else if (y < masBajaSeca) masBajaSeca = y
                }
            }
            if (masAltaMojada == -Float.MAX_VALUE || masBajaSeca == Float.MAX_VALUE) continue
            assertTrue(
                "en el nivel $level hay una casilla seca mas honda que una mojada",
                masBajaSeca > masAltaMojada
            )
        }
    }

    @Test
    fun unaCuevaPlanaNoSeInunda() {
        // Sin pozos, la napa taparia la cueva entera o ninguna casilla, y las
        // dos cosas son peores que no tener agua.
        val m = MazeGenerator.generate(30, 55L).maze
        m.floorLevel.fill(0)
        assertEquals(
            AguaDeLaCueva.SIN_AGUA,
            AguaDeLaCueva.nivelDeAgua(m, 30, Random(1)),
            0f
        )
    }

    @Test
    fun laMallaDelAguaCubreExactamenteLasCasillasInundadas() {
        var probadas = 0
        for (level in 6..50) {
            val m = MazeGenerator.generate(level, level * 641L).maze
            val malla = WaterMesh.build(m)
            if (m.waterY == AguaDeLaCueva.SIN_AGUA) {
                assertEquals("hay malla de agua en una cueva seca", 0, malla.triangleCount)
                continue
            }
            val casillas = WaterMesh.casillasInundadas(m)
            assertTrue("hay agua pero ninguna casilla inundada", casillas > 0)
            assertTrue("la cueva se inundo entera", casillas < m.solid.count { !it })
            assertTrue("no se armo ni un triangulo de agua", malla.triangleCount >= casillas)
            for (v in malla.vertices) {
                assertTrue("hay un valor invalido en la malla del agua", !v.isNaN() && !v.isInfinite())
            }
            val vertices = malla.vertices.size / WaterMesh.STRIDE_FLOATS
            for (i in malla.indices) {
                assertTrue("indice fuera de rango en la malla del agua", i in 0 until vertices)
            }
            probadas++
        }
        assertTrue("no se probo ninguna cueva con agua", probadas > 0)
    }

    @Test
    fun elEspejoEstaTodoALaMismaAltura() {
        // Es un plano horizontal: si algun vertice quedara mas alto o mas bajo,
        // se veria como una rampa de agua.
        for (level in 6..50) {
            val m = MazeGenerator.generate(level, level * 88L).maze
            if (m.waterY == AguaDeLaCueva.SIN_AGUA) continue
            val malla = WaterMesh.build(m)
            if (malla.triangleCount == 0) continue
            val y0 = malla.vertices[1]
            var i = 1
            while (i < malla.vertices.size) {
                assertEquals("el espejo de agua no es plano", y0, malla.vertices[i], 1e-5f)
                i += WaterMesh.STRIDE_FLOATS
            }
            assertTrue("el espejo no quedo por debajo de la napa", y0 <= m.waterY)
        }
    }

    @Test
    fun elAguaFrenaPeroNoTanto() {
        // Es ambiente y un estorbo chico, no un castigo: si frenara de verdad,
        // cruzar un charco con un bicho atras seria una muerte cantada.
        assertEquals("en seco no puede frenar", 1f, AguaDeLaCueva.frenoPorAgua(0f), 0f)
        val freno = AguaDeLaCueva.frenoPorAgua(AguaDeLaCueva.PROFUNDIDAD)
        assertTrue("el agua no frena nada", freno < 1f)
        assertTrue("el agua frena demasiado: $freno", freno >= 0.70f)
    }

    @Test
    fun chapotearHaceRuidoPeroQuedarseQuietoNo() {
        assertTrue(AguaDeLaCueva.haceRuido(AguaDeLaCueva.PROFUNDIDAD, seEstaMoviendo = true))
        assertFalse(
            "quieto en el agua no puede hacer ruido",
            AguaDeLaCueva.haceRuido(AguaDeLaCueva.PROFUNDIDAD, seEstaMoviendo = false)
        )
        assertFalse(
            "caminar en seco no es chapotear",
            AguaDeLaCueva.haceRuido(0f, seEstaMoviendo = true)
        )
    }

    @Test
    fun laSesionSabeSiEstasEnElAgua() {
        // Arrancar siempre en seco es parte del contrato (ver el test de
        // arranque y salida), asi que esto vale para cualquier nivel.
        for (level in intArrayOf(1, 12, 30)) {
            val s = GameSession(perfil(), level, level * 3L)
            assertFalse("el nivel $level arranca en el agua", s.enElAgua())
            assertEquals(0f, s.honduraDelAgua(), 0f)
        }
    }

    @Test
    fun elAguaNoRompeElRelieveQueYaEstaba() {
        // El agua se calcula DESPUES del relieve y no lo toca: si alterara las
        // alturas podria dejar una transicion imposible de pasar.
        for (level in 10..40) {
            val bp = MazeGenerator.generate(level, level * 17L)
            assertTrue(
                "el nivel $level quedo intransitable",
                com.mggx.laberinto.maze.ReliefGenerator.esTransitable(bp.maze)
            )
            assertTrue("el nivel $level dejo de tener solucion", bp.maze.isSolvable())
        }
    }

    @Test
    fun sinAguaNoHayNadaQueDibujar() {
        val m = Maze(9, 9)
        assertEquals(AguaDeLaCueva.SIN_AGUA, m.waterY, 0f)
        assertEquals(0, WaterMesh.build(m).triangleCount)
        assertEquals(0, WaterMesh.casillasInundadas(m))
    }
}
