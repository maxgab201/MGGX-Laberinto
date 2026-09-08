package com.mggx.laberinto.game

/**
 * Como va parado el jugador. Cambia la altura del ojo, el alto que ocupa y la
 * velocidad, y es lo que permite pasar por los tramos bajos de la cueva.
 *
 * Agacharse NO gasta aguante: es una forma de avanzar, no un esfuerzo.
 */
enum class Postura(
    /** Altura del ojo sobre el piso, en metros. */
    val alturaOjo: Float,
    /** Alto que ocupa el cuerpo: define por donde entra. */
    val alturaCuerpo: Float,
    /** Multiplicador de velocidad. */
    val velocidad: Float,
    val etiqueta: String
) {
    DE_PIE(1.62f, 1.80f, 1.00f, "De pie"),
    AGACHADO(1.05f, 1.20f, 0.62f, "Agachado"),
    ARRASTRANDOSE(0.55f, 0.70f, 0.34f, "Arrastrandose");

    /** La siguiente postura mas baja, o la misma si ya es la mas baja. */
    fun masBajo(): Postura = when (this) {
        DE_PIE -> AGACHADO
        AGACHADO -> ARRASTRANDOSE
        ARRASTRANDOSE -> ARRASTRANDOSE
    }

    /** La siguiente postura mas alta. */
    fun masAlto(): Postura = when (this) {
        ARRASTRANDOSE -> AGACHADO
        AGACHADO -> DE_PIE
        DE_PIE -> DE_PIE
    }

    /** Solo se puede correr y saltar estando de pie. */
    val puedeCorrer: Boolean get() = this == DE_PIE
    val puedeSaltar: Boolean get() = this == DE_PIE

    companion object {
        /** La postura mas alta que entra en un hueco de [altoLibre] metros. */
        fun paraAltura(altoLibre: Float): Postura? = when {
            altoLibre >= DE_PIE.alturaCuerpo -> DE_PIE
            altoLibre >= AGACHADO.alturaCuerpo -> AGACHADO
            altoLibre >= ARRASTRANDOSE.alturaCuerpo -> ARRASTRANDOSE
            else -> null
        }
    }
}
