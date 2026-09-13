package com.mggx.laberinto.game

/**
 * Buffs temporales activos durante una partida.
 * Cada EffectType puede estar activo una sola vez: volver a usarlo refresca
 * la duracion y se queda con la magnitud mas alta (nunca se apilan al infinito).
 */
class ActiveEffects {

    data class Active(
        val type: EffectType,
        var magnitude: Float,
        var remaining: Float,
        /**
         * Duracion completa de la aplicacion que manda ahora mismo. Es el
         * denominador de la barrita del HUD, asi que tiene que corresponderse
         * con [remaining] o la barra se pasa de largo.
         */
        var total: Float,
        /** De que objeto viene el efecto que manda ahora. Da el icono del HUD. */
        var sourceId: String
    )

    private val map = LinkedHashMap<EffectType, Active>()

    /**
     * Los efectos que se muestran en el HUD.
     *
     * `filter` ya devuelve una lista nueva: el `.toList()` que habia detras
     * hacia una SEGUNDA copia de la misma cosa. Esto se lee en cada
     * recomposicion del HUD, o sea unas treinta veces por segundo.
     */
    val visible: List<Active> get() = map.values.filter { it.total > 0f }

    /**
     * Prende un efecto, o refresca el que ya estaba.
     *
     * Al refrescar se queda con lo MEJOR de los dos (la magnitud mas alta y el
     * tiempo mas largo), que es lo que evita que usar dos pociones seguidas
     * apile efectos al infinito.
     *
     * Y si el que gana es el nuevo, se queda tambien con SU objeto y SU
     * duracion total. Antes no: usabas una pocion corta y despues una larga, y
     * el HUD te seguia mostrando el icono de la corta —el efecto era el de la
     * larga, pero el cartel mentia— y la barrita de tiempo se iba por arriba
     * del 100%, porque dividia el tiempo nuevo (mas largo) por el total viejo
     * (mas corto).
     */
    fun apply(type: EffectType, magnitude: Float, duration: Float, sourceId: String) {
        if (duration <= 0f) return
        val cur = map[type]
        if (cur == null) {
            map[type] = Active(type, magnitude, duration, duration, sourceId)
        } else {
            cur.magnitude = maxOf(cur.magnitude, magnitude)
            if (duration > cur.remaining) {
                cur.remaining = duration
                cur.total = duration
                cur.sourceId = sourceId
            }
        }
    }

    fun isActive(type: EffectType): Boolean = (map[type]?.remaining ?: 0f) > 0f

    fun remaining(type: EffectType): Float = map[type]?.remaining ?: 0f

    /** Magnitud del efecto o [fallback] si no esta activo. */
    fun magnitude(type: EffectType, fallback: Float = 0f): Float =
        map[type]?.takeIf { it.remaining > 0f }?.magnitude ?: fallback

    /** Multiplicador: devuelve 1 si el efecto no esta activo. */
    fun multiplier(type: EffectType): Float = magnitude(type, 1f)

    fun clear(type: EffectType) { map.remove(type) }

    fun clearAll() = map.clear()

    fun update(dt: Float) {
        if (map.isEmpty()) return
        val it = map.entries.iterator()
        while (it.hasNext()) {
            val e = it.next().value
            e.remaining -= dt
            if (e.remaining <= 0f) it.remove()
        }
    }
}
