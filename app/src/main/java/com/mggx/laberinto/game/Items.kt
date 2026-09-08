package com.mggx.laberinto.game

import com.mggx.laberinto.ui.icons.IconId
import kotlin.math.pow
import kotlin.math.roundToInt

/** Las dos monedas del juego. */
enum class Currency(val code: String, val label: String, val plural: String) {
    ECOS("ECO", "Eco", "Ecos"),
    VETAGRIS("VTG", "Vetagris", "Vetagris")
}

enum class ItemKind(val label: String, val blurb: String) {
    CONSUMIBLE("Consumibles", "Se usan durante la partida y se gastan."),
    MEJORA("Mejoras", "Suben de nivel y son para siempre."),
    RELIQUIA("Reliquias", "Se equipan y actuan solas. Tenes 3 ranuras."),
    PODER("Poderes", "Carisimos y para siempre. Cambian como se juega."),
    COSMETICO("Aspecto", "Cambian como se ven tus manos y tu luz.")
}

enum class Rarity(val label: String, val tint: Long) {
    COMUN("Comun", 0xFF9AA3AD),
    RARO("Raro", 0xFF4FA8E8),
    EPICO("Epico", 0xFFB673E8),
    LEGENDARIO("Legendario", 0xFFE8A33D)
}

/**
 * Cada tipo de efecto se implementa de verdad en el juego.
 * No hay dos objetos con el mismo par (tipo, magnitud, duracion).
 */
enum class EffectType {
    // --- consumibles temporales / instantaneos
    LUZ_RADIO,          // multiplica el radio de la antorcha
    VELOCIDAD,          // multiplica la velocidad de avance
    RECARGA_AGUANTE,    // recarga aguante (magnitud = fraccion)
    HILO_ARIADNA,       // deja rastro luminoso en el suelo
    BRUJULA,            // flecha 3D apuntando a la salida
    VISION_NOCTURNA,    // brillo minimo global
    REVELAR_MAPA,       // revela fraccion del minimapa
    TIZA,               // entrega marcas manuales
    ROMPE_PARED,        // cargas para destruir una pared
    ATRAVESAR_PARED,    // cargas para cruzar una pared
    IMAN_TOTAL,         // atrae todos los ecos del nivel
    CONGELAR_RELOJ,     // pausa el cronometro
    BENGALA,            // estallido de luz muy amplio
    CURAR,              // cura vida
    RESISTENCIA,        // reduce dano recibido
    GIRO_RAPIDO,        // multiplica velocidad de camara
    VOLVER_CRUCE,       // teleport al ultimo cruce visitado
    SONAR,              // dibuja contornos de paredes cercanas
    DESLIZAR,           // mayor aceleracion y menor friccion
    BONUS_ECOS,         // multiplica ecos recogidos
    INMUNE_TRAMPAS,     // ignora trampas
    DESHACER_TRAMPA,    // cancela el proximo golpe de trampa
    COMIDA,             // cura vida y aguante a la vez

    // --- mejoras permanentes (magnitud = por nivel)
    UP_VELOCIDAD,
    UP_AGUANTE,
    UP_VIDA,
    UP_LUZ_BASE,
    UP_RANURAS,
    UP_RADIO_RECOGIDA,
    UP_MAPA_INICIAL,
    UP_SENTIDO_TRAMPAS,
    UP_VELOCIDAD_USO,
    UP_REDUCE_DANO,
    UP_GASTO_AGUANTE,
    UP_FOV,
    UP_RENDIMIENTO_ECOS,
    UP_RASTRO_PERMANENTE,
    UP_VIDAS_EXTRA,

    // --- reliquias (pasivas)
    REL_ARMADURA_CRITICA,
    REL_ECO_CRITICO,
    REL_PING_SALIDA,
    REL_REGENERACION,
    REL_HILO_DOBLE,
    REL_LUZ_MINIMA,
    REL_ARRANQUE_VELOZ,
    REL_ZUMBIDO_TRAMPA,
    REL_AHORRO_CONSUMIBLE,
    REL_RENTA_VETAGRIS,
    REL_INMUNE_CALOR,
    REL_SONAR_GRATIS,

    // --- poderes permanentes (carisimos, se compran una sola vez)
    POD_LINTERNA,           // linterna de carburo con bateria y estaciones de carga
    POD_POSTURA_LIBRE,      // agachado y arrastrandose vas a velocidad normal
    POD_SALTO_ALTO,         // saltas mucho mas y no te lastimas al caer
    POD_VER_TESOROS,        // cristales y cofres marcados desde el arranque
    POD_PICO_ETERNO,        // un golpe de pico gratis en cada nivel
    POD_MAPA_PERSISTENTE,   // el mapa explorado no se borra al reintentar
    POD_REGENERACION,       // la vida se recupera sola de a poco
    POD_SIGILO,             // los bichos te ven de mucho mas cerca

    // --- cosmeticos
    COS_GUANTES,
    COS_TINTE_LUZ,
    COS_PIEL            // skin del personaje: piel + traje
}

data class ItemEffect(
    val type: EffectType,
    /** Magnitud del efecto. En mejoras es POR NIVEL. */
    val magnitude: Float = 0f,
    /** Duracion en segundos (0 = instantaneo o permanente). */
    val duration: Float = 0f,
    /** Cargas otorgadas (para objetos de uso contado). */
    val charges: Int = 0,
    /** Color asociado (cosmeticos de luz y piel de la skin), formato ARGB. */
    val color: Long = 0L,
    /** Segundo color (traje de la skin), formato ARGB. */
    val color2: Long = 0L
)

data class ShopItem(
    val id: String,
    val name: String,
    val desc: String,
    val kind: ItemKind,
    val rarity: Rarity,
    val currency: Currency,
    val basePrice: Int,
    val effect: ItemEffect,
    val icon: IconId,
    /** Niveles maximos (1 = objeto simple). */
    val maxLevel: Int = 1,
    /** Nivel de juego minimo para que aparezca en la tienda. */
    val unlockLevel: Int = 1
) {
    /** Precio del siguiente nivel de una mejora (o precio unico si maxLevel = 1). */
    fun priceAt(ownedLevel: Int): Int {
        if (maxLevel <= 1) return basePrice
        val f = (1.0 + ownedLevel).pow(1.55)
        return (basePrice * f).roundToInt()
    }

    val isLeveled: Boolean get() = maxLevel > 1
}
