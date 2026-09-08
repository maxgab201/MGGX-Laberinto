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
        val total: Float,
        val sourceId: String
    )

    private val map = LinkedHashMap<EffectType, Active>()

    val visible: List<Active> get() = map.values.filter { it.total > 0f }.toList()

    fun apply(type: EffectType, magnitude: Float, duration: Float, sourceId: String) {
        if (duration <= 0f) return
        val cur = map[type]
        if (cur == null) {
            map[type] = Active(type, magnitude, duration, duration, sourceId)
        } else {
            cur.magnitude = maxOf(cur.magnitude, magnitude)
            cur.remaining = maxOf(cur.remaining, duration)
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
