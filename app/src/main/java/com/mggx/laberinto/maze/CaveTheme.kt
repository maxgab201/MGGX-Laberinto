package com.mggx.laberinto.maze

/**
 * Ambientacion visual de cada tramo de la cueva.
 * Todos los colores son lineales (0..1) y los consume directamente el shader.
 */
enum class CaveTheme(
    val displayName: String,
    val description: String,
    /** Tinte de la roca. */
    val rockR: Float, val rockG: Float, val rockB: Float,
    /** Tinte del suelo. */
    val floorR: Float, val floorG: Float, val floorB: Float,
    /** Color de la niebla / bruma. */
    val fogR: Float, val fogG: Float, val fogB: Float,
    /** Luz ambiental minima. */
    val ambientR: Float, val ambientG: Float, val ambientB: Float,
    /** Color emisivo de las vetas de mineral. */
    val veinR: Float, val veinG: Float, val veinB: Float,
    /** Densidad de niebla (mas alto = se ve menos lejos). */
    val fogDensity: Float,
    /** Rugosidad de la piedra para el ruido de textura. */
    val roughness: Float,
    /** Semilla de textura: cambia el patron de roca. */
    val textureSeed: Int
) {
    ENTRADA(
        "Boca de la Cueva", "Piedra caliza humeda y raices colgantes.",
        0.42f, 0.38f, 0.33f, 0.30f, 0.27f, 0.23f,
        0.055f, 0.052f, 0.048f, 0.055f, 0.052f, 0.050f,
        1.00f, 0.72f, 0.30f, 0.030f, 0.62f, 1
    ),
    MUSGO(
        "Galerias de Musgo", "Verdin fosforescente que trepa por la roca.",
        0.34f, 0.40f, 0.32f, 0.24f, 0.28f, 0.22f,
        0.040f, 0.058f, 0.046f, 0.042f, 0.058f, 0.046f,
        0.45f, 1.00f, 0.55f, 0.038f, 0.70f, 2
    ),
    CRISTAL(
        "Cavernas de Cuarzo", "Cristales azules que laten con luz propia.",
        0.31f, 0.35f, 0.46f, 0.22f, 0.25f, 0.34f,
        0.038f, 0.048f, 0.075f, 0.038f, 0.048f, 0.078f,
        0.42f, 0.78f, 1.00f, 0.042f, 0.55f, 3
    ),
    HIELO(
        "Sima Helada", "Escarcha eterna y ecos que se congelan.",
        0.48f, 0.54f, 0.60f, 0.40f, 0.46f, 0.53f,
        0.062f, 0.072f, 0.086f, 0.060f, 0.070f, 0.086f,
        0.70f, 0.92f, 1.00f, 0.036f, 0.42f, 4
    ),
    AZUFRE(
        "Pozos de Azufre", "Vapor acido y roca amarillenta.",
        0.46f, 0.40f, 0.26f, 0.34f, 0.30f, 0.19f,
        0.068f, 0.058f, 0.034f, 0.062f, 0.054f, 0.032f,
        1.00f, 0.86f, 0.30f, 0.048f, 0.75f, 5
    ),
    MAGMA(
        "Venas de Magma", "El calor sube desde las grietas del suelo.",
        0.38f, 0.24f, 0.20f, 0.28f, 0.17f, 0.14f,
        0.078f, 0.036f, 0.026f, 0.070f, 0.032f, 0.024f,
        1.00f, 0.42f, 0.14f, 0.050f, 0.80f, 6
    ),
    OBSIDIANA(
        "Corredores de Obsidiana", "Vidrio volcanico negro que refleja tu antorcha.",
        0.17f, 0.16f, 0.20f, 0.13f, 0.12f, 0.16f,
        0.024f, 0.022f, 0.032f, 0.026f, 0.024f, 0.034f,
        0.72f, 0.40f, 1.00f, 0.055f, 0.28f, 7
    ),
    VETAGRIS(
        "Corazon de Vetagris", "El mineral mas raro de toda la sima.",
        0.30f, 0.31f, 0.34f, 0.22f, 0.23f, 0.26f,
        0.040f, 0.042f, 0.048f, 0.044f, 0.046f, 0.052f,
        0.86f, 0.92f, 0.98f, 0.046f, 0.50f, 8
    );

    companion object {
        /** Rotacion de temas por tramos de nivel. */
        fun forLevel(level: Int): CaveTheme = when {
            level <= 4 -> ENTRADA
            level <= 9 -> MUSGO
            level <= 15 -> CRISTAL
            level <= 21 -> HIELO
            level <= 28 -> AZUFRE
            level <= 36 -> MAGMA
            level <= 45 -> OBSIDIANA
            else -> {
                // A partir del 46 se rotan todos, con Vetagris cada 5.
                if (level % 5 == 0) VETAGRIS
                else entries[(level - 46) % (entries.size - 1)]
            }
        }
    }
}
