package com.mggx.laberinto.game

import kotlin.math.abs

/**
 * El guion del nivel 1: ensena los controles de a un paso por vez, mirando lo
 * que el jugador HACE, no lo que lee.
 *
 * Esta separado de [GameSession] a proposito, igual que BrazoPose o
 * MinimapMath: es una maquina de estados de aritmetica pura, sin nada de
 * Android ni de OpenGL, asi que se puede verificar paso por paso en un test
 * de JVM. GameSession solo le cuenta que paso en el cuadro ([observar]) y el
 * HUD lee [pasoActual] y [progreso].
 *
 * Dos decisiones de diseno que vale la pena dejar escritas:
 *
 *  - Cada paso se aprueba HACIENDO la cosa (girar tantos grados, caminar
 *    tantos metros, agacharse tanto tiempo). Un tutorial que se pasa tocando
 *    "siguiente" no ensena nada.
 *  - Y cada paso tiene un [Paso.limite] de segundos: si el jugador no lo saca
 *    en ese rato, el tutorial sigue igual con un mensaje mas suave. Un
 *    tutorial que te deja trabado porque no encontras el boton es peor que no
 *    tener tutorial.
 */
class Tutorial(conLinterna: Boolean) {

    enum class Clave { MIRAR, CAMINAR, CORRER, AGACHARSE, SALTAR, GOLPEAR, JUNTAR, LINTERNA, MAPA, SALIR }

    /**
     * [objetivo] esta en la unidad que junta cada paso: grados para MIRAR,
     * metros para CAMINAR, segundos para los que se miden por tiempo y veces
     * para los que se miden por accion.
     */
    class Paso(
        val clave: Clave,
        val titulo: String,
        val ayuda: String,
        val logro: String,
        val objetivo: Float,
        val limite: Float
    )

    val pasos: List<Paso> = buildList {
        add(
            Paso(
                Clave.MIRAR, "Mira alrededor",
                "Arrastra el dedo por la mitad derecha de la pantalla.",
                "Eso es. Con eso mirás para donde quieras.",
                objetivo = 240f, limite = 25f
            )
        )
        add(
            Paso(
                Clave.CAMINAR, "Camina",
                "El joystick de la izquierda te mueve.",
                "Ahí va. Ya sabés andar por la cueva.",
                objetivo = 8f, limite = 30f
            )
        )
        add(
            Paso(
                Clave.CORRER, "Corre un poco",
                "Con el boton de correr vas mas rapido, pero gastas aguante.",
                "Bien. Ojo con el aguante, que no es infinito.",
                objetivo = 1.6f, limite = 25f
            )
        )
        add(
            Paso(
                Clave.AGACHARSE, "Agachate",
                "Tocá el boton de agacharse. Hay tramos bajos donde es la unica forma de pasar.",
                "Perfecto. Agachado pasás por donde no entrás de pie.",
                objetivo = 1.2f, limite = 25f
            )
        )
        add(
            Paso(
                Clave.SALTAR, "Salta",
                "El salto te sube escalones y te saca de arriba de las trampas.",
                "Listo. Saltar tambien salva de mas de una trampa.",
                objetivo = 1f, limite = 22f
            )
        )
        add(
            Paso(
                Clave.GOLPEAR, "Pega un golpe",
                "El boton grande de la derecha. La mira del medio se abre sola cuando tenes algo al alcance.",
                "Ese es el golpe. Mirá la mira antes de pegar.",
                objetivo = 1f, limite = 22f
            )
        )
        add(
            Paso(
                Clave.JUNTAR, "Junta un eco",
                "Los ecos que brillan en el piso son la plata de la tienda. Pasales por encima.",
                "Con ecos comprás cosas entre nivel y nivel.",
                objetivo = 1f, limite = 45f
            )
        )
        if (conLinterna) {
            add(
                Paso(
                    Clave.LINTERNA, "Prendé la linterna",
                    "Ilumina mucho mas lejos, pero se come el carburo.",
                    "Acordate de apagarla cuando no la necesites.",
                    objetivo = 0.8f, limite = 25f
                )
            )
        }
        add(
            Paso(
                Clave.MAPA, "El mapa, arriba a la derecha",
                "Solo se dibuja lo que vas caminando. La marca clara del borde es el norte.",
                "Si te perdes, mira el mapa: lo que esta en blanco es por donde ya pasaste.",
                objetivo = 6f, limite = 6f
            )
        )
        add(
            Paso(
                Clave.SALIR, "Ahora busca la salida",
                "Es el obelisco con luz. Ahi termina el nivel.",
                "Eso es todo. De aca en adelante la cueva se pone seria.",
                objetivo = 1f, limite = 0f
            )
        )
    }

    var indice = 0
        private set

    /** Cuanto lleva juntado del paso actual, en la unidad del paso. */
    private var acumulado = 0f

    /** Segundos en el paso actual, para el limite. */
    private var enPaso = 0f

    var terminado = false
        private set

    /** El paso que hay que mostrar, o null si ya se termino todo. */
    val pasoActual: Paso?
        get() = if (terminado) null else pasos[indice]

    /** De 0 a 1, para la barrita del HUD. */
    val progreso: Float
        get() {
            val p = pasoActual ?: return 1f
            return (acumulado / p.objetivo).coerceIn(0f, 1f)
        }

    /**
     * Le cuenta al tutorial que paso en este cuadro. Devuelve el mensaje para
     * el HUD si se acaba de completar un paso, o null si no.
     */
    fun observar(
        dt: Float,
        giroGrados: Float,
        metros: Float,
        corriendo: Boolean,
        agachado: Boolean,
        salto: Boolean,
        golpe: Boolean,
        junto: Boolean,
        linterna: Boolean,
        gano: Boolean
    ): String? {
        if (terminado) return null
        val p = pasos[indice]
        enPaso += dt
        acumulado += when (p.clave) {
            Clave.MIRAR -> abs(giroGrados)
            Clave.CAMINAR -> metros
            Clave.CORRER -> if (corriendo && metros > 0f) dt else 0f
            Clave.AGACHARSE -> if (agachado) dt else 0f
            Clave.SALTAR -> if (salto) 1f else 0f
            Clave.GOLPEAR -> if (golpe) 1f else 0f
            Clave.JUNTAR -> if (junto) 1f else 0f
            Clave.LINTERNA -> if (linterna) dt else 0f
            Clave.MAPA -> dt
            Clave.SALIR -> if (gano) 1f else 0f
        }

        val hecho = acumulado >= p.objetivo
        // El ultimo paso (buscar la salida) no vence: no hay nada despues.
        val vencido = p.limite > 0f && enPaso >= p.limite && !hecho
        if (!hecho && !vencido) return null

        val msg = if (hecho) p.logro else "Lo dejamos para despues: ${p.ayuda}"
        acumulado = 0f
        enPaso = 0f
        if (indice + 1 >= pasos.size) terminado = true else indice++
        return msg
    }
}
