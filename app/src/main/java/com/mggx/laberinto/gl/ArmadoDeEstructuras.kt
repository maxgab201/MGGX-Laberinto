package com.mggx.laberinto.gl

import com.mggx.laberinto.maze.MazeGenerator.TrapKind
import kotlin.math.cos
import kotlin.math.sin

/**
 * Como se arman las trampas y la salida.
 *
 * Es el mismo tratamiento que ya recibieron los bichos, y por el mismo motivo:
 * las MALLAS de una trampa se podian fotografiar sueltas, pero el armado —
 * cuantos pinchos, en que corona, a que altura, con que giro— vivia adentro de
 * `CaveRenderer.drawProps` y no habia forma de mirarlo.
 *
 * Ese punto ciego ya dejo pasar dos cosas en este proyecto: el pico parado como
 * un mastil (1.9.3) y el guardian que era un busto flotando (1.9.4). Las dos
 * veces la malla estaba impecable y el error estaba en donde se la ponia.
 *
 * Todo lo de aca es aritmetica: se puede componer y medir en un test de JVM.
 */
object ArmadoDeEstructuras {

    /** Las piezas con las que se arman las trampas y la salida. */
    enum class Malla {
        PINCHO, LOSA, TABLA, BOCA_POZO, BROCAL, COSTRA, BOCA_FISURA, VAPOR,
        OBELISCO, GEMA
    }

    /**
     * Una pieza puesta, relativa al centro de la casilla y a su piso.
     *
     * Solo geometria: el color y el aviso que late los pone el renderer.
     */
    class Pieza(
        val malla: Malla,
        val x: Float, val y: Float, val z: Float,
        val escala: Float,
        val giro: Float,
        /** Desfase de animacion, para lo que se mueve. */
        val fase: Float = 0f,
        /** Cuanto la enciende su propio brillo, aparte del aviso. */
        val brillo: Float = 0f,
        /** Transparencia: la usa el vapor, que se va desvaneciendo. */
        val alfa: Float = 1f
    )

    fun geometria(m: Malla): PropMeshes.Geometry = when (m) {
        Malla.PINCHO -> StructureMeshes.pincho()
        Malla.LOSA -> StructureMeshes.losaRajada()
        Malla.TABLA -> DetailMeshes.roundedBox(1f, 0.13f, 0.13f, 0.018f)
        Malla.BOCA_POZO -> StructureMeshes.bocaDePozo()
        Malla.BROCAL -> StructureMeshes.brocalDePozo()
        Malla.COSTRA -> StructureMeshes.costraDeFisura()
        Malla.BOCA_FISURA -> StructureMeshes.bocaDeFisura()
        Malla.VAPOR -> StructureMeshes.nubeDeVapor()
        Malla.OBELISCO -> StructureMeshes.obeliscoSalida()
        Malla.GEMA -> DetailMeshes.gem()
    }

    /**
     * Lo ancho que puede ser una trampa sin meterse en la roca.
     *
     * La casilla mide 3 m, pero la pared dibujada se abulta hacia adentro
     * (ver `WorldMesh`), asi que una trampa de 3 m de ancho quedaria con las
     * puntas adentro de la piedra.
     */
    const val ANCHO_UTIL = 2.2f

    /**
     * Cuanto se levanta la boca del pozo sobre la altura de apoyo de la
     * casilla.
     *
     * La roca del piso NO es plana: esta abollada por ruido hasta
     * `WorldMesh.BULTO_PISO` (13 cm), con el maximo justo en el centro de la
     * casilla, que es donde se mide la altura de apoyo. O sea que en el resto
     * de la casilla la piedra dibujada puede estar hasta 13 cm MAS ARRIBA que
     * el punto donde se apoya la trampa.
     *
     * Un disco de 2,2 m puesto a ras quedaria medio enterrado y se veria como
     * media luna negra. Levantarlo 20 cm lo deja entero por encima de la roca
     * en toda su superficie; el escalon lo tapa el brocal de piedras, que es
     * mas alto que eso.
     */
    const val LEVANTE_POZO = 0.20f

    /**
     * Lo mismo para la raja de la fisura, que es mas chica.
     *
     * La abolladura de la roca se apaga hacia los bordes de la casilla, asi
     * que una pieza de 1,3 m centrada en la casilla no ve los 13 cm enteros;
     * con 12 cm sale entera por encima de la piedra.
     */
    const val LEVANTE_FISURA = 0.12f

    /**
     * Arma una trampa.
     *
     * @param semillaX,semillaY la casilla, que es lo que le da a cada trampa su
     *   propio desorden sin necesitar guardar nada.
     * @param tiempo el reloj, para el vapor que sale a chorros.
     */
    fun trampa(kind: TrapKind, semillaX: Int, semillaY: Int, tiempo: Float): List<Pieza> {
        val out = ArrayList<Pieza>(12)
        when (kind) {
            TrapKind.SPIKES -> {
                // Corona de pinches de hierro asomando del piso: uno en el
                // medio y seis alrededor, cada uno de su alto.
                for (k in 0 until 7) {
                    val a = k * 0.8976f + semillaX
                    val rr = if (k == 0) 0f else 0.52f
                    out.add(
                        Pieza(
                            Malla.PINCHO,
                            cos(a.toDouble()).toFloat() * rr, 0f, sin(a.toDouble()).toFloat() * rr,
                            0.40f + (k % 3) * 0.09f, a
                        )
                    )
                }
                // La losa partida al medio, girada distinto en cada trampa.
                //
                // Antes eran dos TABLAs cruzadas en equis. En la foto se leia
                // un durmiente de vias, no una tapa que cede: hacia falta ver
                // la trampa armada para darse cuenta.
                out.add(
                    Pieza(
                        Malla.LOSA, 0f, 0.055f, 0f, ANCHO_UTIL * 0.86f,
                        (semillaX * 0.7f + semillaY * 1.3f) % 3.1416f
                    )
                )
            }
            TrapKind.PITFALL -> {
                // El agujero de verdad: un embudo que se pierde para abajo.
                //
                // Antes esto era una CAJA negra puesta en y=-0.28 con escala
                // 1.35. Como la caja mide lo mismo en los tres ejes (el
                // pipeline de instancias solo admite escala UNIFORME), al
                // agrandarla para tapar la casilla tambien crecia para
                // arriba: quedaban 39 cm de cubo negro apoyados en el piso.
                // Se veia un baul, no un pozo. Aguanto meses porque el armado
                // de las trampas no se podia mirar.
                val giro = (semillaX * 1.1f + semillaY * 0.7f) % 6.2832f
                out.add(
                    Pieza(
                        Malla.BOCA_POZO, 0f, LEVANTE_POZO, 0f, ANCHO_UTIL, giro,
                        brillo = -1f
                    )
                )
                out.add(Pieza(Malla.BROCAL, 0f, LEVANTE_POZO, 0f, ANCHO_UTIL, giro))
                // Las tablas podridas que quedaron cruzando el hueco: son lo
                // que dice que esto lo tapo alguien y no aguanto, y ademas lo
                // unico que le da al ojo una referencia de que hay vacio
                // abajo. Van por encima del disco negro.
                out.add(Pieza(Malla.TABLA, -0.26f, LEVANTE_POZO + 0.07f, 0.10f, 1.85f, 0.22f))
                out.add(Pieza(Malla.TABLA, 0.30f, LEVANTE_POZO + 0.04f, -0.18f, 1.55f, -0.34f))
                out.add(Pieza(Malla.TABLA, 0.05f, LEVANTE_POZO + 0.05f, 0.62f, 1.40f, 1.42f))
            }
            TrapKind.STEAM -> {
                // Una RAJA en la roca con costra mineral a los costados.
                //
                // Antes era un CILINDRO con una CAJA encima: en la foto se
                // veia un cubo blanco de tres cuartos de metro apoyado en el
                // piso. El vapor no sale de una caja, sale de una grieta.
                val giro = (semillaX * 0.9f + semillaY * 1.7f) % 3.1416f
                out.add(Pieza(Malla.COSTRA, 0f, 0.075f, 0f, 1.30f, giro))
                out.add(
                    Pieza(
                        Malla.BOCA_FISURA, 0f, LEVANTE_FISURA, 0f, 1.30f, giro,
                        brillo = -1f
                    )
                )
                for (k in 0 until 4) {
                    val t = ((tiempo * 0.8f + k * 0.25f) % 1f)
                    // Cada bocanada sube, se abre y se desvanece. La malla es
                    // un bollo blando: antes se usaba la misma gema facetada
                    // que los cristales y se veia una torre de diamantes
                    // saliendo del piso.
                    out.add(
                        Pieza(
                            Malla.VAPOR, 0f, 0.26f + t * 1.6f, 0f,
                            0.20f + t * 0.42f, t * 5f, k.toFloat(),
                            brillo = (1f - t) * 0.55f, alfa = (1f - t) * 0.7f
                        )
                    )
                }
            }
        }
        return out
    }

    /**
     * La salida: la columna de cristal y las cinco gemas que le giran
     * alrededor.
     *
     * Se ve de lejos a proposito — es el objetivo del nivel, y una salida que
     * hay que encontrar por casualidad no es un objetivo, es una loteria.
     */
    fun salida(tiempo: Float): List<Pieza> {
        val out = ArrayList<Pieza>(6)
        out.add(Pieza(Malla.OBELISCO, 0f, 0f, 0f, 2.55f, 0f, brillo = 0.85f))
        for (i in 0 until 5) {
            val a = tiempo * 0.55f + i * (2f * Math.PI.toFloat() / 5f)
            val rr = 0.85f
            out.add(
                Pieza(
                    Malla.GEMA,
                    cos(a.toDouble()).toFloat() * rr,
                    0.85f + sin((tiempo * 1.4f + i).toDouble()).toFloat() * 0.16f,
                    sin(a.toDouble()).toFloat() * rr,
                    0.24f, a, i.toFloat(), brillo = 1.15f
                )
            )
        }
        return out
    }
}
