package com.mggx.laberinto.game

import kotlin.math.hypot
import kotlin.math.min

/**
 * El rastro de pisadas que queda en el piso detras tuyo.
 *
 * Normalmente los puntos se apagan solos a los pocos segundos (es una ayuda
 * chica para no dar vueltas en circulos). Con el Hilo de Ariadna puesto, en
 * cambio, el rastro se queda quieto todo lo que dura el objeto: ese es el
 * sentido de comprarlo.
 *
 * Esta afuera de [GameSession] por una razon: aca adentro viven dos reglas que
 * se contradicen entre si (los puntos que no vencen y el tope de memoria) y que
 * ya se rompieron una vez. Separado se puede probar sin levantar una partida
 * entera.
 */
class Rastro(
    /**
     * Cuantos puntos se guardan como mucho.
     *
     * A un punto cada [separacion] metros, 900 son unos 675 metros de recorrido.
     * Pasado eso el rastro es una ventana que corre: entra el nuevo y sale el
     * mas viejo. Lo que NO puede hacer es dejar de anotar, que es lo que hacia
     * antes: con el hilo puesto no vence ningun punto, asi que al llegar al tope
     * el hilo dejaba de dibujarse detras tuyo sin ningun aviso — justo en un
     * nivel grande, que es cuando se compra.
     */
    val maxPuntos: Int = 900,
    /** Distancia minima entre dos puntos seguidos, en metros. */
    val separacion: Float = 0.75f,
    /**
     * Vida que se le da a un punto mientras el hilo esta puesto.
     *
     * Es "no vencer" escrito en segundos: mas largo que cualquier partida, pero
     * finito, para que al apagarse el hilo se lo pueda recortar.
     */
    val vidaConHilo: Float = 9999f,
) {

    class Punto(val x: Float, val z: Float, var life: Float)

    private val lista = ArrayList<Punto>()

    /** Los puntos, del mas viejo al mas nuevo. Solo de lectura para afuera. */
    val puntos: List<Punto> get() = lista

    /** Si el hilo estaba puesto el cuadro anterior. */
    private var hiloPuesto = false

    /**
     * Un cuadro de rastro.
     *
     * @param dt segundos desde el cuadro anterior.
     * @param x posicion del jugador.
     * @param z posicion del jugador.
     * @param hilo si el Hilo de Ariadna esta activo ahora.
     * @param vidaNormal cuanto dura un punto sin el hilo (`stats.trailSeconds`).
     */
    fun update(dt: Float, x: Float, z: Float, hilo: Boolean, vidaNormal: Float) {
        val ultimo = lista.lastOrNull()
        if (ultimo == null || hypot(ultimo.x - x, ultimo.z - z) > separacion) {
            // Cuando se llena se tira el MAS VIEJO; nunca se deja de anotar.
            if (lista.size >= maxPuntos) lista.removeAt(0)
            lista.add(Punto(x, z, if (hilo) vidaConHilo else vidaNormal))
        }

        if (hilo) {
            hiloPuesto = true
            return
        }

        // Se acaba de apagar el hilo. Los puntos que quedaron tienen la vida
        // larga, y dejarlos asi significaria un cuarto de hora de hilo de
        // regalo. El objeto dice "dura 90 segundos": cumplidos los 90, el
        // rastro se apaga como cualquier otro.
        if (hiloPuesto) {
            hiloPuesto = false
            for (p in lista) p.life = min(p.life, vidaNormal)
        }

        for (i in lista.indices.reversed()) {
            lista[i].life -= dt
            if (lista[i].life <= 0f) lista.removeAt(i)
        }
    }

    fun limpiar() {
        lista.clear()
        hiloPuesto = false
    }
}
