package com.mggx.laberinto.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colores con significado (bien / mal / peligro / salida) adaptados al tipo de
 * daltonismo elegido en Ajustes. La idea es que el par "esto esta bien" contra
 * "esto es peligro" siempre se distinga, aunque el rojo y el verde se confundan.
 */
data class Semantics(
    val good: Color,
    val bad: Color,
    val warn: Color,
    val health: Color,
    val exit: Color,
    val trap: Color
) {
    companion object {
        private val NORMAL = Semantics(
            good = Cave.Good, bad = Cave.Bad, warn = Cave.Warn,
            health = Cave.Health, exit = Cave.Good, trap = Cave.Bad
        )

        /** Protanopia y deuteranopia: se cambia el par rojo/verde por naranja/azul. */
        private val ROJO_VERDE = Semantics(
            good = Color(0xFF4FC3F7),
            bad = Color(0xFFFF8A3D),
            warn = Color(0xFFF2D64B),
            health = Color(0xFFFF8A3D),
            exit = Color(0xFF4FC3F7),
            trap = Color(0xFFFF8A3D)
        )

        /** Tritanopia: se confunde el azul con el amarillo, se usa turquesa/rosa. */
        private val AZUL = Semantics(
            good = Color(0xFF00C2A8),
            bad = Color(0xFFFF5E8A),
            warn = Color(0xFFE0655A),
            health = Color(0xFFFF5E8A),
            exit = Color(0xFF00C2A8),
            trap = Color(0xFFFF5E8A)
        )

        fun of(mode: Int): Semantics = when (mode) {
            1, 2 -> ROJO_VERDE
            3 -> AZUL
            else -> NORMAL
        }
    }
}
