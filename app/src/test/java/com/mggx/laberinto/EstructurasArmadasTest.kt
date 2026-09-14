package com.mggx.laberinto

import com.mggx.laberinto.gl.ArmadoDeEstructuras
import com.mggx.laberinto.gl.PropMeshes
import com.mggx.laberinto.maze.MazeGenerator.TrapKind
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max

/**
 * Las trampas y la salida, armadas y miradas.
 *
 * Mismo tratamiento que los bichos, y por el mismo motivo: el armado vivia
 * adentro del renderer y no se podia ver. Ese punto ciego ya dejo pasar el
 * pico parado como un mastil y el guardian que era un busto.
 */
class EstructurasArmadasTest {

    private fun componer(piezas: List<ArmadoDeEstructuras.Pieza>): PropMeshes.Geometry =
        PropMeshes.combinar(*piezas.map { p ->
            PropMeshes.trasladar(
                PropMeshes.rotarY(
                    PropMeshes.escalar(
                        ArmadoDeEstructuras.geometria(p.malla), p.escala, p.escala, p.escala
                    ),
                    Math.toDegrees(p.giro.toDouble()).toFloat()
                ),
                p.x, p.y, p.z
            )
        }.toTypedArray())

    @Test
    fun retratarLasTrampasYLaSalida() {
        val fotos = TrapKind.entries.map { it.name to componer(ArmadoDeEstructuras.trampa(it, 3, 5, 0f)) } +
            listOf("SALIDA" to componer(ArmadoDeEstructuras.salida(0f)))
        val f = VisorDeMallas.hoja(fotos, "estructuras-armadas", lado = 250)
        println("FOTO: ${f.absolutePath}")
        assertTrue(f.exists() && f.length() > 1000)
    }

    @Test
    fun ningunaTrampaSeSaleDeSuCasilla() {
        // Una trampa que asoma en la casilla de al lado te pega desde donde no
        // la ves. El aviso que late esta atado a SU casilla.
        val c = com.mggx.laberinto.game.GameSession.CELL
        for (kind in TrapKind.entries) {
            val caja = BichoArmado.caja(componer(ArmadoDeEstructuras.trampa(kind, 3, 5, 0f)))
            val ancho = max(caja[1] - caja[0], caja[5] - caja[4])
            assertTrue(
                "$kind mide ${"%.2f".format(ancho)} m y la casilla ${c} m",
                ancho <= c
            )
        }
    }

    @Test
    fun lasTrampasNoSeLevantanComoParaTropezarlasSinVerlas() {
        // Son trampas de piso: si sobresalieran mucho serian un obstaculo
        // visible, y dejarian de ser trampas.
        for (kind in TrapKind.entries) {
            // El chorro de vapor no cuenta: es un efecto que sube, no un
            // pedazo de trampa con el que uno se tropieza.
            val solido = ArmadoDeEstructuras.trampa(kind, 3, 5, 0f)
                .filter { it.malla != ArmadoDeEstructuras.Malla.VAPOR }
            val caja = BichoArmado.caja(componer(solido))
            assertTrue("$kind se levanta ${"%.2f".format(caja[3])} m del piso", caja[3] < 1.1f)
        }
    }

    /** La caja de UNA sola pieza del armado, para medirla sola. */
    private fun cajaDe(
        piezas: List<ArmadoDeEstructuras.Pieza>, malla: ArmadoDeEstructuras.Malla
    ): FloatArray = BichoArmado.caja(componer(piezas.filter { it.malla == malla }))

    @Test
    fun elPozoEsUnAgujeroYNoUnBaul() {
        // El pozo era una CAJA negra de escala 1,35 puesta en y=-0.28. Como el
        // pipeline de instancias solo admite escala UNIFORME, al agrandarla
        // para tapar la casilla tambien crecia para arriba y quedaban 39 cm de
        // cubo negro apoyados en el piso: se veia un baul.
        //
        // Un agujero es ANCHO y CHATO. Esa es toda la diferencia, y es lo que
        // este test fija.
        val piezas = ArmadoDeEstructuras.trampa(TrapKind.PITFALL, 3, 5, 0f)
        val caja = cajaDe(piezas, ArmadoDeEstructuras.Malla.BOCA_POZO)
        val ancho = maxOf(caja[1] - caja[0], caja[5] - caja[4])
        val alto = caja[3] - caja[2]
        assertTrue("la boca del pozo mide ${"%.2f".format(ancho)} m de ancho", ancho > 1.8f)
        assertTrue("la boca del pozo se levanta ${"%.2f".format(alto)} m", alto < 0.12f)
    }

    @Test
    fun elPozoNoQuedaMedioEnterradoEnLaRocaAbollada() {
        // La roca del piso esta abollada por ruido hasta BULTO_PISO, con el
        // maximo en el CENTRO de la casilla — que es justo donde se mide la
        // altura de apoyo. O sea que en el resto de la casilla la piedra
        // dibujada puede estar hasta 13 cm mas arriba que el punto de apoyo.
        //
        // Un disco de 2,2 m puesto a ras quedaria medio adentro de la piedra y
        // se veria una media luna negra. Es el mismo bug que dejaba las
        // antorchas flotando y los props enterrados, del otro lado.
        val piezas = ArmadoDeEstructuras.trampa(TrapKind.PITFALL, 3, 5, 0f)
        val caja = cajaDe(piezas, ArmadoDeEstructuras.Malla.BOCA_POZO)
        assertTrue(
            "lo mas bajo del pozo esta a ${"%.3f".format(caja[2])} m y la roca sube hasta " +
                "${com.mggx.laberinto.gl.WorldMesh.BULTO_PISO}",
            caja[2] >= com.mggx.laberinto.gl.WorldMesh.BULTO_PISO
        )
    }

    @Test
    fun elBrocalTapaElEscalonDelPozo() {
        // El disco negro se levanta sobre la roca. Si no hubiera piedras
        // alrededor, ese escalon se veria: un disco negro flotando a 20 cm del
        // piso no se lee como un pozo, se lee como un error.
        val piezas = ArmadoDeEstructuras.trampa(TrapKind.PITFALL, 3, 5, 0f)
        val pozo = cajaDe(piezas, ArmadoDeEstructuras.Malla.BOCA_POZO)
        val brocal = cajaDe(piezas, ArmadoDeEstructuras.Malla.BROCAL)
        assertTrue(
            "el brocal llega a ${"%.3f".format(brocal[3])} y el borde del pozo a " +
                "${"%.3f".format(pozo[3])}",
            brocal[3] >= pozo[3]
        )
        assertTrue(
            "el brocal arranca en ${"%.3f".format(brocal[2])} y el pozo en " +
                "${"%.3f".format(pozo[2])}: queda el escalon a la vista",
            brocal[2] <= pozo[2]
        )
        // Y tiene que rodearlo, no taparlo.
        assertTrue("el brocal es mas angosto que el pozo", brocal[1] - brocal[0] >= pozo[1] - pozo[0])
    }

    @Test
    fun laTapaDeLosPinchesNoEsUnDurmienteDeVias() {
        // Eran dos barras cruzadas en equis. En la foto se leia un durmiente
        // de vias. Una tapa es una LOSA: ancha en los dos ejes, no una cruz.
        val piezas = ArmadoDeEstructuras.trampa(TrapKind.SPIKES, 3, 5, 0f)
        val caja = cajaDe(piezas, ArmadoDeEstructuras.Malla.LOSA)
        val ancho = caja[1] - caja[0]
        val largo = caja[5] - caja[4]
        assertTrue("la tapa mide ${"%.2f".format(ancho)} x ${"%.2f".format(largo)} m", ancho > 1.2f)
        assertTrue("la tapa mide ${"%.2f".format(ancho)} x ${"%.2f".format(largo)} m", largo > 1.2f)
    }

    @Test
    fun laSalidaSeVeDeLejos() {
        // Es el objetivo del nivel. Una salida que hay que encontrar por
        // casualidad no es un objetivo, es una loteria.
        val caja = BichoArmado.caja(componer(ArmadoDeEstructuras.salida(0f)))
        val alto = caja[3] - caja[2]
        assertTrue("la salida mide ${"%.2f".format(alto)} m: no se ve por encima de nada", alto > 2f)
    }

    @Test
    fun elVaporSeMueveConElTiempoYLoDemasNo() {
        // El vapor es lo unico animado de las trampas. Si algo mas se moviera,
        // una trampa desarmada se veria viva.
        for (kind in TrapKind.entries) {
            val a = ArmadoDeEstructuras.trampa(kind, 3, 5, 0f)
            val b = ArmadoDeEstructuras.trampa(kind, 3, 5, 0.5f)
            val cambio = a.indices.count { i -> a[i].y != b[i].y || a[i].escala != b[i].escala }
            if (kind == TrapKind.STEAM) assertTrue("el vapor no se mueve", cambio > 0)
            else assertTrue("$kind se mueve sola", cambio == 0)
        }
    }
}
