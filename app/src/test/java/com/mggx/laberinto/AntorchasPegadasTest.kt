package com.mggx.laberinto

import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.gl.Anclajes
import com.mggx.laberinto.gl.StructureMeshes
import com.mggx.laberinto.gl.WorldMesh
import com.mggx.laberinto.maze.Maze
import com.mggx.laberinto.maze.MazeGenerator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Que las antorchas esten pegadas a la roca, y no flotando.
 *
 * El bug: el plano nominal de una pared es el borde de la casilla, pero la roca
 * que se VE no esta ahi. El ruido la corre hasta 30 cm para cualquiera de los
 * dos lados y la panza del tunel 22 cm mas hacia afuera, y las dos cosas tienen
 * su maximo justo a media altura — o sea exactamente donde va colgada una
 * antorcha. Como se colgaban a un corrimiento FIJO desde el centro de la
 * casilla, la placa de anclaje podia quedar a medio metro de la pared.
 *
 * Nada de esto se ve leyendo el codigo: hay que medirlo contra la malla que se
 * dibuja de verdad. Eso es lo que hacen estos tests.
 */
class AntorchasPegadasTest {

    private fun nivel(n: Int, semilla: Long) = MazeGenerator.generate(n, semilla)

    /**
     * Que tan lejos del centro de la casilla esta la roca dibujada, alrededor
     * de un punto de una pared.
     *
     * Medir la distancia al vertice mas cercano NO sirve: la malla esta
     * teselada en 3 a 5 tramos por casilla, asi que un punto perfectamente
     * apoyado en la superficie igual puede tener el vertice mas cercano a
     * medio metro. Lo que importa es la distancia PERPENDICULAR: se juntan los
     * vertices de esa pared que caen cerca (a lo largo y en altura) y se mira
     * entre que valores se mueve la roca ahi.
     *
     * @return (minimo, maximo) de la distancia de la roca al centro de la
     *   casilla, o null si no hay vertices cerca.
     */
    private fun rocaAlrededor(
        vertices: FloatArray, m: Maze, gx: Int, gy: Int, enX: Boolean, dir: Float,
        alLargo: Float, altura: Float
    ): Pair<Float, Float>? {
        val c = GameSession.CELL
        val centro = if (enX) (gx + 0.5f) * c else (gy + 0.5f) * c
        val paso = WorldMesh.STRIDE_FLOATS
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        var i = 0
        while (i < vertices.size) {
            if (vertices[i + 9] == 0f) {      // capa de pared
                val vx = vertices[i]
                val vy = vertices[i + 1]
                val vz = vertices[i + 2]
                val perp = if (enX) vx else vz
                val largo = if (enX) vz else vx
                val lejos = (perp - centro) * dir
                // Del lado bueno de la casilla, y en la ventana que rodea al
                // punto. Sin el filtro de lado entraria la pared de enfrente.
                if (lejos > 0.8f && abs(largo - alLargo) < 0.6f && abs(vy - altura) < 0.55f) {
                    if (lejos < lo) lo = lejos
                    if (lejos > hi) hi = lejos
                }
            }
            i += paso
        }
        return if (lo > hi) null else lo to hi
    }

    @Test
    fun laFormulaDeLaCaraCoincideConLaMallaQueSeDibuja() {
        // Lo primero de todo: que `realWallFace` diga lo mismo que construye
        // `build`. Si se desincronizaran, todo lo demas seria mentira.
        val bp = nivel(9, 31415L)
        val m = bp.maze
        val malla = WorldMesh.build(m, 3)
        var revisadas = 0
        var peor = 0f

        for (gy in 1 until m.gh - 1) {
            for (gx in 1 until m.gw - 1) {
                if (revisadas > 60) break
                if (m.isSolid(gx, gy) || !m.isSolid(gx - 1, gy)) continue
                val y = m.floorY(gx, gy) + 1.8f
                if (y >= m.ceilY(gx, gy) - 0.3f) continue
                val z = (gy + 0.5f) * GameSession.CELL
                val centro = (gx + 0.5f) * GameSession.CELL
                val cara = (WorldMesh.realWallFace(m, gx, gy, -1, 0, y, z) - centro) * -1f
                val rango = rocaAlrededor(malla.vertices, m, gx, gy, true, -1f, z, y) ?: continue
                revisadas++
                // La formula tiene que caer dentro de lo que la malla dibuja
                // ahi al lado, con margen para la teselacion.
                val fuera = maxOf(rango.first - cara, cara - rango.second, 0f)
                if (fuera > peor) peor = fuera
            }
        }
        assertTrue("no se encontro ninguna pared para revisar", revisadas > 15)
        assertTrue("la formula se despego de la malla: $peor m fuera de rango", peor < 0.20f)
    }

    @Test
    fun laPlacaDeCadaAntorchaTocaLaRoca() {
        var peorNuevo = 0f
        var peorViejo = 0f
        var cuantas = 0

        for (nivelN in intArrayOf(2, 7, 14, 23)) {
            val bp = nivel(nivelN, nivelN * 9931L)
            val m = bp.maze
            val malla = WorldMesh.build(m, 3)
            val ancla = FloatArray(4)
            val placa = FloatArray(3)
            val c = GameSession.CELL

            for (gi in bp.torches) {
                val gx = gi % m.gw
                val gy = gi / m.gw
                if (!Anclajes.antorcha(m, gx, gy, ancla)) continue

                // De que lado quedo, para saber que eje mirar.
                var sx = 0; var sy = 0
                if (m.isSolid(gx - 1, gy)) sx = -1
                else if (m.isSolid(gx + 1, gy)) sx = 1
                else if (m.isSolid(gx, gy - 1)) sy = -1
                else sy = 1
                val enX = sx != 0
                val dir = (sx + sy).toFloat()
                val centro = if (enX) (gx + 0.5f) * c else (gy + 0.5f) * c
                val alLargo = if (enX) (gy + 0.5f) * c else (gx + 0.5f) * c

                Anclajes.placaDe(ancla, placa)
                val rango = rocaAlrededor(
                    malla.vertices, m, gx, gy, enX, dir, alLargo, placa[1]
                ) ?: continue
                cuantas++

                // Cuanto le falta a la placa para llegar a la roca mas cercana
                // que hay ahi. Positivo = flotando en el aire.
                val placaLejos = ((if (enX) placa[0] else placa[2]) - centro) * dir
                peorNuevo = maxOf(peorNuevo, rango.first - placaLejos)

                // La misma antorcha con la cuenta vieja, en la misma malla,
                // para tener con que comparar.
                val viejaLejos = 0.46f * c + StructureMeshes.LARGO_BRAZO * Anclajes.ALTO_ANTORCHA
                peorViejo = maxOf(peorViejo, rango.first - viejaLejos)
            }
        }

        assertTrue("no habia antorchas que revisar", cuantas > 40)
        assertTrue(
            "la peor antorcha quedo a $peorNuevo m de la roca (con la cuenta vieja, a $peorViejo m)",
            peorNuevo < 0.12f
        )
        assertTrue(
            "el arreglo no mejoro nada: antes $peorViejo m en el aire, ahora $peorNuevo m",
            peorNuevo < peorViejo - 0.20f
        )
    }

    @Test
    fun unaAntorchaQueSeQuedoSinParedNoSeDibuja() {
        // Si el jugador rompe con el pico la pared de la que colgaba, no hay
        // donde clavarla: antes se dibujaba igual, colgada del aire.
        val bp = nivel(5, 777L)
        val m = bp.maze
        val gi = bp.torches.first()
        val gx = gi % m.gw
        val gy = gi / m.gw
        val ancla = FloatArray(4)
        assertTrue("no tenia pared de entrada", Anclajes.antorcha(m, gx, gy, ancla))

        for (d in 0..3) {
            val vx = gx + intArrayOf(-1, 1, 0, 0)[d]
            val vy = gy + intArrayOf(0, 0, -1, 1)[d]
            if (m.inBounds(vx, vy)) m.setSolid(vx, vy, false)
        }
        assertFalse("sin pared alrededor, sigue diciendo que la puede clavar",
            Anclajes.antorcha(m, gx, gy, ancla))
    }

    @Test
    fun ningunaAntorchaSeMeteEnElMedioDelPasillo() {
        // El otro extremo: cuando la roca se abolla hacia ADENTRO, la antorcha
        // la sigue, pero no hasta pararse en el medio del camino.
        val c = GameSession.CELL
        for (nivelN in intArrayOf(3, 11, 19)) {
            val bp = nivel(nivelN, nivelN * 71L)
            val m = bp.maze
            val ancla = FloatArray(4)
            for (gi in bp.torches) {
                val gx = gi % m.gw
                val gy = gi / m.gw
                if (!Anclajes.antorcha(m, gx, gy, ancla)) continue
                val dx = abs(ancla[0] - (gx + 0.5f) * c)
                val dz = abs(ancla[2] - (gy + 0.5f) * c)
                val lejos = maxOf(dx, dz)
                assertTrue(
                    "una antorcha quedo a $lejos m del centro: se metio en el pasillo",
                    lejos >= c * Anclajes.MINIMO - 0.001f
                )
            }
        }
    }
    @Test
    fun enUnaGateraLaAntorchaEntraBajoElTecho() {
        // El otro caso de "antorcha flotando", y el mas feo: clavada siempre a
        // 1,75 m del piso, en una gatera de un metro de alto quedaba del OTRO
        // LADO de la roca. Se veia la punta de la llama saliendo del techo y la
        // luz alumbrando desde adentro de la pared.
        //
        // Se fuerza el techo bajo a mano en vez de buscar un nivel que lo
        // tenga: ahora el generador esquiva esas casillas (ver el test de
        // abajo), asi que hay que armar el caso a proposito para probar que el
        // renderer igual lo resuelve si le toca.
        val bp = MazeGenerator.generate(6, 123L)
        val m = bp.maze
        val ancla = FloatArray(4)

        for (alto in floatArrayOf(3.4f, 2.4f, 1.6f, 1.05f)) {
            val gi = bp.torches.first { Anclajes.antorcha(m, it % m.gw, it / m.gw, ancla) }
            val gx = gi % m.gw
            val gy = gi / m.gw
            m.ceilClearance[gi] = alto
            assertTrue("con $alto m de techo dejo de poder clavarla", Anclajes.antorcha(m, gx, gy, ancla))

            val fy = m.floorY(gx, gy)
            val libre = Maze.altoLibreReal(m.ceilY(gx, gy) - fy)
            val punta = ancla[1] + Anclajes.ALTO_TOTAL
            assertTrue(
                "con $alto m de techo la llama llega a ${punta - fy} m: atraviesa la roca",
                punta <= fy + libre + 0.001f
            )
            assertTrue("quedo enterrada en el piso", ancla[1] >= fy)
            m.ceilClearance[gi] = 3.4f
        }
    }

    @Test
    fun unTechoImposibleDejaALaAntorchaSinDibujar() {
        // Si no entra ni bajandola, no se dibuja: es mejor que no haya
        // antorcha a que haya media antorcha adentro de la roca.
        val bp = MazeGenerator.generate(6, 123L)
        val m = bp.maze
        val ancla = FloatArray(4)
        val gi = bp.torches.first { Anclajes.antorcha(m, it % m.gw, it / m.gw, ancla) }
        m.ceilClearance[gi] = 0.55f
        assertFalse(
            "dice que la puede clavar en un hueco de medio metro",
            Anclajes.antorcha(m, gi % m.gw, gi / m.gw, ancla)
        )
    }

    @Test
    fun casiTodasLasAntorchasCaenDondeHayAlturaDeSobra() {
        // El generador prefiere casillas con techo: bajarla para que entre es
        // el premio consuelo, no el caso normal.
        var total = 0
        var comodas = 0
        for (nivelN in intArrayOf(6, 14, 23, 31)) {
            val bp = MazeGenerator.generate(nivelN, nivelN * 4441L)
            val m = bp.maze
            for (gi in bp.torches) {
                val gx = gi % m.gw
                val gy = gi / m.gw
                total++
                if (Maze.altoLibreReal(m.ceilY(gx, gy) - m.floorY(gx, gy)) >= 2.6f) comodas++
            }
        }
        assertTrue("no habia antorchas", total > 40)
        val fraccion = comodas.toFloat() / total
        assertTrue("solo el ${(fraccion * 100).toInt()}% cayo en casillas con techo", fraccion > 0.9f)
    }

}
