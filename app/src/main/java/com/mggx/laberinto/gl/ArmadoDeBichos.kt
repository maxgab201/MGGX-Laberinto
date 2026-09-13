package com.mggx.laberinto.gl

import com.mggx.laberinto.maze.MazeGenerator.EnemyKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Como se arma cada bicho a partir de sus piezas.
 *
 * Esto vivia adentro de `CaveRenderer.drawProps`, y ese era el problema: las
 * MALLAS de los bichos se pueden fotografiar una por una (existe
 * `EnemyMeshes` y el visor les saca fotos), pero el ARMADO —que pieza va
 * donde, con que escala, con que giro, a que altura— no se podia mirar de
 * ninguna forma. Y el armado es justo donde estaban los errores: el guardian
 * era un busto flotando a diez centimetros del piso, sin nada abajo de la
 * cintura.
 *
 * Es el mismo agujero que dejo pasar al pico parado como un mastil en la
 * 1.9.3: la malla impecable y el problema en donde se la ponia.
 *
 * Aca no hay nada de OpenGL, asi que el bicho entero se puede componer y
 * medir en un test de JVM (ver BichosArmadosTest y RetratosDeBichosTest).
 *
 * ## La restriccion que manda
 *
 * El pipeline de instancias ([InstancedShape.add]) solo admite **escala
 * uniforme y giro en Y**. No hay escala por eje ni giro en X o Z. Todo lo que
 * necesite proporciones distintas va horneado en la malla o se resuelve
 * sumando piezas. La mitad de lo que se ve raro en un bicho sale de pelearse
 * con eso.
 */
object ArmadoDeBichos {

    /** Las mallas que puede usar un bicho. El renderer las mapea a su shape. */
    enum class Malla {
        MURCIELAGO_CUERPO, ALA,
        TOPO_CUERPO, PALA,
        ARANA_CUERPO, PATA,
        RASTRERO_SEGMENTO,
        GUARDIAN_TORSO, GUARDIAN_CABEZA, GUARDIAN_BRAZO, GUARDIAN_PIERNA,
        GEMA
    }

    /**
     * Una pieza puesta, en el espacio del bicho: el bicho parado en (0,0,0)
     * con los pies en y=0 y mirando hacia +Z.
     *
     * Solo geometria, a proposito. El COLOR no vive aca sino en el renderer
     * (`CaveRenderer.colorDePieza`): es lo unico de un bicho que depende del
     * bioma y del destello del golpe, y los tests que miran el armado miden
     * cajas, no colores. Separarlos deja este archivo entero fotografiable.
     *
     * @param tipo la animacion que le toca en el vertex shader (0 quieto,
     *   1 gema, 2 ala, 3 reptar). Ver Shaders.PROP_VS.
     */
    class Pieza(
        var malla: Malla,
        var x: Float, var y: Float, var z: Float,
        var escala: Float,
        var giro: Float,
        var fase: Float,
        var tipo: Float
    ) {
        constructor() : this(Malla.GEMA, 0f, 0f, 0f, 1f, 0f, 0f, 0f)

        fun set(
            malla: Malla, x: Float, y: Float, z: Float,
            escala: Float, giro: Float, fase: Float, tipo: Float
        ) {
            this.malla = malla; this.x = x; this.y = y; this.z = z
            this.escala = escala; this.giro = giro; this.fase = fase; this.tipo = tipo
        }
    }

    /**
     * Bolsa de piezas que se reusa cuadro a cuadro.
     *
     * El renderer arma cada bicho en cada cuadro, y son hasta dieciseis piezas
     * por bicho: crear objetos nuevos cada vez son unas diez mil asignaciones
     * por segundo en la ruta de dibujo. En un telefono eso se paga en
     * tironeos, que es justo lo que se estuvo sacando en las ultimas
     * versiones. La bolsa crece hasta lo que haga falta y ahi se queda.
     */
    class Bolsa {
        private val piezas = ArrayList<Pieza>(16)
        var cuantas = 0; private set

        fun vaciar() { cuantas = 0 }

        fun tomar(): Pieza {
            if (cuantas == piezas.size) piezas.add(Pieza())
            return piezas[cuantas++]
        }

        operator fun get(i: Int): Pieza = piezas[i]
    }

    /**
     * La malla de cada pieza.
     *
     * Una sola lista, usada por el renderer para armar sus formas
     * instanciadas y por el visor para componer el bicho entero. Si hubiera
     * dos, se desincronizarian y el bicho de la foto dejaria de ser el bicho
     * del juego — que es justo el problema que este archivo viene a cerrar.
     */
    fun geometria(m: Malla): PropMeshes.Geometry = when (m) {
        Malla.MURCIELAGO_CUERPO -> EnemyMeshes.murcielagoCuerpo()
        Malla.ALA -> EnemyMeshes.murcielagoAla()
        Malla.TOPO_CUERPO -> EnemyMeshes.topoCuerpo()
        Malla.PALA -> EnemyMeshes.topoPala()
        Malla.ARANA_CUERPO -> EnemyMeshes.aranaCuerpo()
        Malla.PATA -> EnemyMeshes.rastreroPata()
        Malla.RASTRERO_SEGMENTO -> EnemyMeshes.rastreroSegmento()
        Malla.GUARDIAN_TORSO -> EnemyMeshes.guardianTorso()
        Malla.GUARDIAN_CABEZA -> EnemyMeshes.guardianCabeza()
        Malla.GUARDIAN_BRAZO -> EnemyMeshes.guardianBrazo()
        Malla.GUARDIAN_PIERNA -> EnemyMeshes.guardianPierna()
        Malla.GEMA -> DetailMeshes.gem()
    }

    /** Las piezas que son ojos: el renderer les pone su brillo propio. */
    fun esOjo(m: Malla): Boolean = m == Malla.GEMA

    /**
     * Arma un bicho en una [Bolsa], sin asignar memoria. Es la via que usa el
     * renderer, que llama a esto una vez por bicho y por cuadro.
     *
     * @param paso metros caminados, que es el reloj de las animaciones de
     *   patas (van atadas al avance y no al tiempo, asi las patas se mueven
     *   cuando el bicho avanza y se quedan quietas cuando esta parado).
     */
    fun armarEn(kind: EnemyKind, paso: Float, fase: Float, bolsa: Bolsa) {
        bolsa.vaciar()
        when (kind) {
            EnemyKind.MURCIELAGO -> murcielago(bolsa, fase)
            EnemyKind.TOPO -> topo(bolsa, paso, fase)
            EnemyKind.ARANA -> arana(bolsa, paso, fase)
            EnemyKind.RASTRERO -> rastrero(bolsa, fase)
            EnemyKind.GUARDIAN -> guardian(bolsa)
        }
    }

    /** Lo mismo, en una lista nueva. Comodo para los tests. */
    fun armar(kind: EnemyKind, paso: Float = 0f, fase: Float = 0f): List<Pieza> {
        val bolsa = Bolsa()
        armarEn(kind, paso, fase, bolsa)
        return (0 until bolsa.cuantas).map { i ->
            val p = bolsa[i]
            Pieza(p.malla, p.x, p.y, p.z, p.escala, p.giro, p.fase, p.tipo)
        }
    }

    private const val MEDIA = PI.toFloat()

    // --------------------------------------------------------- murcielago

    /**
     * Cuerpo de una sola pieza (con hocico y orejas) mirando al frente, y las
     * dos alas colgadas de los hombros. La del lado izquierdo es la misma
     * malla girada media vuelta, con el aleteo desfasado para que no baten
     * como un solo panel.
     *
     * Vuela: su y sale de `EnemyKind.vuelaA`, asi que aca el cuerpo va en 0 y
     * el renderer lo sube.
     */
    private fun murcielago(out: Bolsa, fase: Float) {
        out.tomar().set(Malla.MURCIELAGO_CUERPO, 0f, 0f, 0f, 0.78f, 0f, fase, 0f)
        out.tomar().set(Malla.ALA, 0.13f, 0.09f, 0f, 0.68f, 0f, fase, 2f)
        out.tomar().set(Malla.ALA, -0.13f, 0.09f, 0f, 0.68f, MEDIA, fase + 3.14f, 2f)
        out.tomar().set(Malla.GEMA, -0.09f, 0.11f, 0.29f, 0.042f, 0f, 0f, 0f)
        out.tomar().set(Malla.GEMA, 0.09f, 0.11f, 0.29f, 0.042f, 0f, 0f, 0f)
    }

    // --------------------------------------------------------------- topo

    /**
     * Va pegado al piso, cabeceando como el que viene cavando, con las dos
     * palas moviendose alternadas. No tiene ojos utiles: lo que se le ve es la
     * nariz humeda.
     *
     * Apoyado en el piso y al tamano que el juego le da (0,50 m de alto).
     * Antes se dibujaba de 28 cm y flotando 5: un chorizo aplastado.
     */
    private fun topo(out: Bolsa, paso: Float, fase: Float) {
        val cabeceo = sin((paso * 5.5f + fase).toDouble()).toFloat()
        out.tomar().set(Malla.TOPO_CUERPO, 0f, 0.28f + cabeceo * 0.03f, 0f, 0.90f, 0f, fase, 0f)
        for (lado in 0 until 2) {
            val s = if (lado == 0) 1f else -1f
            val bat = sin((paso * 5.5f + fase + lado * 3.14f).toDouble()).toFloat()
            out.tomar().set(
                Malla.PALA, 0.30f * s, 0.25f + bat * 0.05f, 0.24f,
                0.44f, if (lado == 0) 0f else MEDIA, fase, 0f
            )
        }
        out.tomar().set(Malla.GEMA, 0f, 0.28f, 0.46f, 0.042f, 0f, 0f, 0f)
    }

    // -------------------------------------------------------------- arana

    /**
     * Cuelga a media altura. Ocho patas quebradas repartidas alrededor del
     * cefalotorax, las de cada lado giradas media vuelta para que la rodilla
     * apunte siempre para afuera. Los ojos en fila son lo unico que se le ve
     * de lejos.
     */
    private fun arana(out: Bolsa, paso: Float, fase: Float) {
        out.tomar().set(Malla.ARANA_CUERPO, 0f, 0f, 0f, 1.10f, 0f, fase, 0f)
        for (k in 0 until 8) {
            val lado = if (k % 2 == 0) -1f else 1f
            val a = 0.38f - (k / 2) * 0.28f
            val mueve = sin((paso * 4.2f + fase + k * 0.9f).toDouble()).toFloat()
            out.tomar().set(
                Malla.PATA, 0.26f * lado, -0.24f + mueve * 0.04f, a,
                0.58f, if (lado > 0f) 0f else MEDIA, fase + k * 0.5f, 0f
            )
        }
        for (k in 0 until 3) {
            out.tomar().set(Malla.GEMA, (k - 1) * 0.10f, 0.09f, 0.40f, 0.036f, 0f, 0f, 0f)
        }
    }

    // ----------------------------------------------------------- rastrero

    /**
     * Cuerpo largo de tres placas de caparazon que ondulan al avanzar, cada
     * vez mas chicas hacia la cola, y cuatro patas quebradas. Es ciego: en vez
     * de ojos tiene dos antenas que tantean.
     *
     * Tres placas apoyadas, de la mas grande adelante a la mas chica atras,
     * hasta el alto que el juego le da (0,75 m).
     */
    private fun rastrero(out: Bolsa, fase: Float) {
        val escalas = floatArrayOf(0.78f, 0.64f, 0.48f)
        val zetas = floatArrayOf(0.40f, 0f, -0.38f)
        val yes = floatArrayOf(0.36f, 0.30f, 0.24f)
        for (k in 0 until 3) {
            out.tomar().set(
                Malla.RASTRERO_SEGMENTO, 0f, yes[k], zetas[k],
                escalas[k], 0f, fase + k * 0.8f, 3f
            )
        }
        for (k in 0 until 4) {
            val a = if (k < 2) 0.34f else -0.24f
            val lado = if (k % 2 == 0) -1f else 1f
            out.tomar().set(
                Malla.PATA, 0.26f * lado, 0f, a,
                0.62f, if (lado > 0f) 0f else MEDIA, fase, 0f
            )
        }
        out.tomar().set(Malla.GEMA, -0.10f, 0.58f, 0.78f, 0.055f, 0f, 0f, 0f)
        out.tomar().set(Malla.GEMA, 0.10f, 0.58f, 0.78f, 0.055f, 0f, 0f, 0f)
    }

    // ----------------------------------------------------------- guardian

    /**
     * Torso tallado, cabeza hundida entre los hombros, dos brazos de roca
     * colgando y dos piernas macizas.
     *
     * Las medidas salen de su `alto` declarado (1,85 m), que es el numero con
     * el que el juego decide por donde entra. Antes se dibujaba de 1,31 m y
     * flotando 9,5 cm, y sin NADA abajo de la cintura: un busto serruchado,
     * mas bajo que lo que uno esquivaba.
     *
     * Piernas 0 -> 0,62. Torso 0,55 -> 1,52. Cabeza 1,42 -> 1,85.
     */
    private fun guardian(out: Bolsa) {
        out.tomar().set(Malla.GUARDIAN_PIERNA, -0.22f, 0.31f, 0.02f, 0.62f, 0f, 0f, 0f)
        out.tomar().set(Malla.GUARDIAN_PIERNA, 0.22f, 0.31f, 0.02f, 0.62f, 0f, 0f, 0f)
        out.tomar().set(Malla.GUARDIAN_TORSO, 0f, 1.004f, 0f, 0.908f, 0f, 0f, 0f)
        out.tomar().set(Malla.GUARDIAN_CABEZA, 0f, 1.630f, 0f, 0.496f, 0f, 0f, 0f)
        out.tomar().set(Malla.GUARDIAN_BRAZO, -0.42f, 1.085f, 0f, 0.73f, 0f, 0f, 0f)
        out.tomar().set(Malla.GUARDIAN_BRAZO, 0.42f, 1.085f, 0f, 0.73f, 0f, 0f, 0f)
        out.tomar().set(Malla.GEMA, -0.10f, 1.655f, 0.20f, 0.055f, 0f, 0f, 0f)
        out.tomar().set(Malla.GEMA, 0.10f, 1.655f, 0.20f, 0.055f, 0f, 0f, 0f)
    }

    /**
     * Lleva una pieza del espacio del bicho al del mundo.
     *
     * El bicho mira hacia +Z en su propio espacio; en el mundo mira hacia su
     * rumbo. Es el mismo par (adelante, derecha) que armaba el renderer a
     * mano para cada pieza.
     */
    fun aMundo(p: Pieza, ex: Float, ey: Float, ez: Float, rumbo: Float, out: FloatArray) {
        val fwdX = sin(rumbo.toDouble()).toFloat(); val fwdZ = cos(rumbo.toDouble()).toFloat()
        val rgtX = cos(rumbo.toDouble()).toFloat(); val rgtZ = -sin(rumbo.toDouble()).toFloat()
        out[0] = ex + rgtX * p.x + fwdX * p.z
        out[1] = ey + p.y
        out[2] = ez + rgtZ * p.x + fwdZ * p.z
        out[3] = rumbo + p.giro
    }
}
