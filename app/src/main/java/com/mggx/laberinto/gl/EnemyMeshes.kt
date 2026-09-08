package com.mggx.laberinto.gl

import com.mggx.laberinto.gl.PropMeshes.Geometry
import com.mggx.laberinto.gl.PropMeshes.combinar
import com.mggx.laberinto.gl.PropMeshes.escalar
import com.mggx.laberinto.gl.PropMeshes.lathe
import com.mggx.laberinto.gl.PropMeshes.rotarX
import com.mggx.laberinto.gl.PropMeshes.rotarY
import com.mggx.laberinto.gl.PropMeshes.rotarZ
import com.mggx.laberinto.gl.PropMeshes.trasladar

/**
 * Un modelo propio para cada bicho.
 *
 * Antes los tres se armaban apilando las mismas primitivas genericas que usa
 * el resto de la cueva: el murcielago era un octaedro con dos cajas de ala, el
 * rastrero tres cajas y cuatro cilindros, y el guardian tres octaedros. Se
 * veian todos "hechos con lo que habia", y como el pipeline de instancias solo
 * permite una escala igual en los tres ejes, tampoco habia forma de darles
 * proporciones sin sumar mas y mas piezas sueltas.
 *
 * Aca cada bicho tiene su malla, con las proporciones horneadas adentro. Cada
 * modelo mide mas o menos 1 unidad en su lado largo, asi que el tamano real
 * sale del `scale` que le pasa CaveRenderer y se lee de una.
 *
 * Convencion, igual que la del resto de los props: el bicho mira hacia +Z y el
 * shader lo gira en Y segun su rumbo.
 */
object EnemyMeshes {

    // ------------------------------------------------------------ murcielago

    /**
     * Cuerpo peludo y compacto, con hocico, orejas grandes y cola.
     *
     * El perfil se hace de revolucion sobre Y y despues se acuesta hacia +Z:
     * es la unica forma de que un cuerpo alargado quede alargado hacia adelante
     * y no hacia arriba.
     */
    fun murcielagoCuerpo(): Geometry {
        val cuerpo = rotarX(
            lathe(
                arrayOf(
                    floatArrayOf(0.00f, -0.50f),   // punta de la cola
                    floatArrayOf(0.07f, -0.40f),
                    floatArrayOf(0.15f, -0.24f),
                    floatArrayOf(0.21f, -0.06f),   // panza, la parte mas ancha
                    floatArrayOf(0.20f, 0.08f),
                    floatArrayOf(0.13f, 0.22f),    // cuello
                    floatArrayOf(0.17f, 0.31f),    // cabeza
                    floatArrayOf(0.15f, 0.38f),
                    floatArrayOf(0.08f, 0.46f),    // hocico
                    floatArrayOf(0.00f, 0.50f)
                ),
                segmentos = 20
            ),
            90f
        )
        // Orejas: conos parados, un poco abiertos hacia los costados.
        val oreja = escalar(PropMeshes.cone(6, 1f, 0.30f, false), 0.30f, 0.34f, 0.30f)
        val izq = trasladar(rotarZ(oreja, 20f), -0.11f, 0.12f, 0.24f)
        val der = trasladar(rotarZ(oreja, -20f), 0.11f, 0.12f, 0.24f)
        return combinar(cuerpo, izq, der)
    }

    /**
     * Ala de membrana del lado derecho: se estira desde el hombro y el borde
     * de atras viene festoneado por los dedos, que es lo que la hace leer como
     * ala de murcielago y no como una tabla.
     *
     * Va en el plano XY (X hacia afuera, Y arriba) porque el shader la aletea
     * moviendo Y segun |x|: cuanto mas lejos del cuerpo, mas sube y baja.
     * Se dibuja con la punta hacia +X; la del otro lado es esta misma girada
     * 180 grados en Y.
     */
    fun murcielagoAla(): Geometry {
        // Contorno antihorario visto desde +Z, arrancando en la raiz (hombro).
        val contorno = arrayOf(
            floatArrayOf(0.00f, 0.00f),    // raiz, es el centro del abanico
            floatArrayOf(0.30f, 0.16f),    // borde de adelante
            floatArrayOf(0.66f, 0.24f),
            floatArrayOf(1.00f, 0.20f),    // punta del ala
            floatArrayOf(0.86f, 0.02f),    // y de aca para atras, los dedos
            floatArrayOf(0.74f, 0.10f),
            floatArrayOf(0.58f, -0.06f),
            floatArrayOf(0.46f, 0.04f),
            floatArrayOf(0.30f, -0.12f),
            floatArrayOf(0.18f, -0.02f),
            floatArrayOf(0.06f, -0.14f)
        )
        val piezas = ArrayList<Geometry>()
        piezas.add(PropMeshes.extruir(contorno, 0.020f))
        for ((x, y) in arrayOf(1f to 0.20f, 0.58f to -0.06f, 0.30f to -0.12f)) {
            val largo = kotlin.math.sqrt(x*x+y*y)
            val angulo = Math.toDegrees(kotlin.math.atan2(y.toDouble(),x.toDouble())).toFloat()
            piezas.add(trasladar(rotarZ(DetailMeshes.roundedBox(largo,0.018f,0.025f,0.006f),angulo),x/2f,y/2f,0f))
        }
        return combinar(*piezas.toTypedArray())
    }

    // -------------------------------------------------------------- rastrero

    /**
     * Un segmento del cuerpo: una placa de caparazon, mas ancha que alta y con
     * la cresta al medio. Se dibujan tres, cada vez mas chicos hacia la cola.
     *
     * Achatar el perfil es justamente lo que no se podia hacer desde el
     * renderer, porque la escala de instancia es una sola para los tres ejes.
     */
    fun rastreroSegmento(): Geometry {
        val placa = escalar(
            lathe(
                arrayOf(
                    floatArrayOf(0.00f, -0.50f),
                    floatArrayOf(0.30f, -0.44f),
                    floatArrayOf(0.46f, -0.26f),
                    floatArrayOf(0.50f, 0.00f),    // el borde que sobresale
                    floatArrayOf(0.42f, 0.22f),
                    floatArrayOf(0.26f, 0.40f),
                    floatArrayOf(0.00f, 0.50f)
                ),
                segmentos = 18
            ),
            1f, 0.62f, 1.15f    // achatado de arriba y estirado hacia adelante
        )
        // Cresta: una quilla finita a lo largo del lomo.
        val cresta = escalar(
            rotarY(PropMeshes.cone(4, 1f, 0.5f, false), 45f),
            0.16f, 0.20f, 0.86f
        )
        return combinar(placa, trasladar(cresta, 0f, 0.16f, 0f))
    }

    /**
     * Pata quebrada en dos tramos: un femur que sale para afuera y arriba, y
     * una tibia que baja al piso. Un cilindro derecho no leia como pata.
     */
    fun rastreroPata(): Geometry {
        val femur = escalar(PropMeshes.cylinder(5, 1f, 0.1f), 0.5f, 0.42f, 0.5f)
        val tibia = escalar(PropMeshes.cylinder(5, 1f, 0.1f), 0.42f, 0.46f, 0.42f)
        return combinar(
            trasladar(rotarZ(femur, -52f), 0f, 0.34f, 0f),
            trasladar(rotarZ(tibia, -8f), 0.30f, 0.00f, 0f)
        )
    }

    // -------------------------------------------------------------- guardian

    /**
     * Torso de piedra: ancho de hombros, angosto de cintura. Pocos segmentos a
     * proposito, para que quede facetado como un bloque tallado y no como un
     * globo.
     */
    fun guardianTorso(): Geometry {
        val torso = escalar(
            lathe(
                arrayOf(
                    floatArrayOf(0.00f, -0.50f),
                    floatArrayOf(0.30f, -0.44f),
                    floatArrayOf(0.34f, -0.20f),   // cintura
                    floatArrayOf(0.44f, 0.10f),
                    floatArrayOf(0.50f, 0.34f),    // hombros
                    floatArrayOf(0.38f, 0.48f),
                    floatArrayOf(0.00f, 0.54f)
                ),
                segmentos = 7
            ),
            1f, 1f, 0.72f    // mas ancho de lado que de frente
        )
        // Dos lajas salidas en la espalda, como placas de roca.
        val laja = escalar(rotarY(PropMeshes.cone(4, 1f, 0.5f, false), 45f), 0.22f, 0.30f, 0.14f)
        return combinar(
            torso,
            trasladar(rotarX(laja, -28f), -0.20f, 0.26f, -0.22f),
            trasladar(rotarX(laja, -34f), 0.18f, 0.32f, -0.20f)
        )
    }

    /**
     * Cabeza sin cuello, hundida entre los hombros, con la ceja saliente que
     * le da la sombra sobre los ojos.
     */
    fun guardianCabeza(): Geometry {
        val craneo = escalar(
            lathe(
                arrayOf(
                    floatArrayOf(0.00f, -0.46f),
                    floatArrayOf(0.32f, -0.36f),
                    floatArrayOf(0.44f, -0.10f),
                    floatArrayOf(0.46f, 0.16f),
                    floatArrayOf(0.34f, 0.38f),
                    floatArrayOf(0.00f, 0.48f)
                ),
                segmentos = 7
            ),
            1f, 0.92f, 0.88f
        )
        val ceja = escalar(DetailMeshes.roundedBox(), 0.62f, 0.14f, 0.26f)
        return combinar(craneo, trasladar(rotarX(ceja, 12f), 0f, 0.14f, 0.34f))
    }

    /**
     * Brazo del guardian: un bloque de roca colgando, mas grueso abajo, que es
     * de donde sale el golpe.
     */
    fun guardianBrazo(): Geometry = escalar(
        lathe(
            arrayOf(
                floatArrayOf(0.00f, -0.50f),
                floatArrayOf(0.34f, -0.40f),   // el puno, la parte gruesa
                floatArrayOf(0.30f, -0.16f),
                floatArrayOf(0.22f, 0.14f),
                floatArrayOf(0.26f, 0.40f),    // hombro
                floatArrayOf(0.00f, 0.50f)
            ),
            segmentos = 6
        ),
        1f, 1f, 0.86f
    )
}

