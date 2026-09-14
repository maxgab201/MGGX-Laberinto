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

    /**
     * @param conElFondo si se incluyen los cerros y arboles lejanos.
     *   La vista de arriba va SIN ellos: el visor encuadra sola la caja
     *   entera, y con cerros a ciento veinte metros el patio queda del tamano
     *   de un sello. La vista desde el tunel si los lleva, porque son la mitad
     *   de lo que se ve desde ahi.
     */
    private fun geometria(conElFondo: Boolean = true): PropMeshes.Geometry {
        val piezas = ArmadoDelPatio.armar(LADO, LADO * 0.5f, 7L)
            .filter { conElFondo || !ArmadoDelPatio.esDelFondo(it.malla) }
            .map { p ->
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
        val g = geometria(conElFondo = false)
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
                p.malla == ArmadoDelPatio.Malla.FAROL ||
                ArmadoDelPatio.esDelFondo(p.malla) ||
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
            // Los cerros y los arboles del fondo estan AFUERA a proposito: son
            // lo que se ve por encima del cerco.
            if (ArmadoDelPatio.esDelFondo(p.malla)) continue
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
        // Se mide contra la PUERTA de verdad y no contra un numero fijo: la
        // casa se corrio cuando dejo de ser un panel plano, y un umbral
        // escrito a mano se habria quedado apuntando a donde estaba antes.
        val puerta = piezas.first { it.malla == ArmadoDelPatio.Malla.PUERTA }
        assertTrue(
            "el camino se corta a ${"%.2f".format(cerca.z - puerta.z)} m de la puerta",
            cerca.z - puerta.z < 1.4f
        )
        assertTrue(
            "el camino se mete adentro de la casa (z=${cerca.z}, puerta=${puerta.z})",
            cerca.z > puerta.z - 0.2f
        )
        // El arranque se mide con el PROMEDIO de las losas del primer tramo,
        // no con una sola: el camino tiene dos losas por escalon mas alguna
        // suelta al costado, asi que "la de mas atras" puede ser una del borde
        // y no dice donde esta el eje.
        val primeras = losas.filter { it.z > lejos.z - 0.8f }
        val eje = primeras.map { it.x }.average().toFloat()
        assertTrue(
            "el camino no arranca en la boca (eje=${"%.2f".format(eje)}, boca=$bocaX)",
            kotlin.math.abs(eje - bocaX) < 1.0f
        )
    }

    @Test
    fun elPatioEntraEnElPresupuestoDeTriangulos() {
        // El patio es el unico lugar del juego con cientos de instancias de
        // mallas propias: matas de pasto, losas, cerros, arboles. Todo eso lo
        // dibuja un telefono, y es facil pasarse sin darse cuenta porque cada
        // pieza sola parece barata.
        //
        // Se cuenta lo que de verdad se manda a la GPU: por cada pieza, los
        // triangulos de SU malla. Es la cuenta que hace el renderer.
        val piezas = ArmadoDelPatio.armar(LADO, LADO * 0.5f, 7L)
        val porMalla = HashMap<ArmadoDelPatio.Malla, Int>()
        for (m in ArmadoDelPatio.Malla.entries) {
            porMalla[m] = ArmadoDelPatio.geometria(m).indices.size / 3
        }
        var total = 0L
        val resumen = HashMap<ArmadoDelPatio.Malla, Long>()
        for (p in piezas) {
            val t = porMalla[p.malla]!!.toLong()
            total += t
            resumen[p.malla] = (resumen[p.malla] ?: 0L) + t
        }
        println("TRIANGULOS DEL PATIO: $total")
        for ((m, t) in resumen.entries.sortedByDescending { it.value }.take(6)) {
            println("   $m: $t (${piezas.count { it.malla == m }} piezas)")
        }
        assertTrue("el patio dibuja $total triangulos: es demasiado para un telefono", total < 170_000)

        // Y ninguna malla sola puede comerse el presupuesto: si una lo hace,
        // es la que hay que simplificar y no las demas.
        for ((m, t) in resumen) {
            assertTrue("$m sola dibuja $t triangulos", t < 90_000)
        }
    }

    @Test
    fun ningunaMallaDelPatioSePasaDeSuCupo() {
        // El renderer reserva un cupo de instancias por malla
        // (CaveRenderer.buildShapes). Lo que pase del cupo NO se dibuja, en
        // silencio: aparecerian claros en el pasto sin ningun error.
        val piezas = ArmadoDelPatio.armar(LADO, LADO * 0.5f, 7L)
        for (m in ArmadoDelPatio.Malla.entries) {
            val n = piezas.count { it.malla == m }
            val cupo = ArmadoDelPatio.cupo(m)
            assertTrue("$m: $n piezas y el cupo es $cupo", n <= cupo)
        }
    }
}
