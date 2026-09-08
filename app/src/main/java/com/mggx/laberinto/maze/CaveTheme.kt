package com.mggx.laberinto.maze

/**
 * Que clase de lugar es cada tramo. No todo es cueva de roca viva: hay mina
 * trabajada, ruinas inundadas, un bosque de hongos y un templo enterrado, y
 * cada uno pone otros objetos en el suelo y otra piedra en las paredes.
 */
enum class Biome { CUEVA, MINA, RUINAS, HONGOS, TEMPLO }

/**
 * Como esta labrada la pared. Cambia el mapa de alturas de la textura, que es
 * lo que hace que una galeria de mina no se vea igual que una cueva.
 */
enum class Patron { ROCA, SILLAR, MADERA, ORGANICO }

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
    val textureSeed: Int,
    /** Que clase de lugar es. */
    val biome: Biome = Biome.CUEVA,
    /** Como esta labrada la pared. */
    val patron: Patron = Patron.ROCA
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
    ),

    // ----------------------------------------------- lo que ya no es cueva
    MINA(
        "Mina Abandonada", "Galerias entibadas, rieles oxidados y polvo de carbon.",
        0.40f, 0.34f, 0.27f, 0.29f, 0.25f, 0.20f,
        0.052f, 0.045f, 0.036f, 0.050f, 0.044f, 0.036f,
        0.95f, 0.66f, 0.28f, 0.040f, 0.58f, 9,
        Biome.MINA, Patron.MADERA
    ),
    RUINAS(
        "Cisternas Anegadas", "Sillares tallados, agua quieta y columnas partidas.",
        0.36f, 0.38f, 0.39f, 0.26f, 0.29f, 0.31f,
        0.036f, 0.046f, 0.052f, 0.038f, 0.048f, 0.054f,
        0.55f, 0.86f, 0.92f, 0.044f, 0.36f, 10,
        Biome.RUINAS, Patron.SILLAR
    ),
    HONGOS(
        "Bosque de Esporas", "Sombreros gigantes que alumbran mas que tu antorcha.",
        0.33f, 0.36f, 0.28f, 0.25f, 0.29f, 0.22f,
        0.042f, 0.060f, 0.040f, 0.046f, 0.064f, 0.044f,
        0.58f, 1.00f, 0.62f, 0.034f, 0.66f, 11,
        Biome.HONGOS, Patron.ORGANICO
    ),
    TEMPLO(
        "Templo Sepultado", "Piedra labrada con oro en las juntas y braseros apagados.",
        0.44f, 0.39f, 0.30f, 0.34f, 0.30f, 0.23f,
        0.058f, 0.050f, 0.034f, 0.056f, 0.050f, 0.038f,
        1.00f, 0.80f, 0.34f, 0.042f, 0.30f, 12,
        Biome.TEMPLO, Patron.SILLAR
    );

    companion object {
        /** Todos menos Vetagris, que se reserva para uno de cada cinco niveles. */
        private val ROTATIVOS: List<CaveTheme> by lazy { entries.filter { it != VETAGRIS } }

        /** Rotacion de temas por tramos de nivel. */
        fun forLevel(level: Int): CaveTheme = when {
            level <= 4 -> ENTRADA
            level <= 8 -> MUSGO
            level <= 12 -> MINA
            level <= 17 -> CRISTAL
            level <= 22 -> RUINAS
            level <= 27 -> HIELO
            level <= 32 -> HONGOS
            level <= 37 -> AZUFRE
            level <= 42 -> TEMPLO
            level <= 47 -> MAGMA
            level <= 52 -> OBSIDIANA
            else -> {
                // A partir del 53 se rotan todos, con Vetagris cada 5.
                if (level % 5 == 0) VETAGRIS
                else ROTATIVOS[(level - 53).mod(ROTATIVOS.size)]
            }
        }
    }
}
