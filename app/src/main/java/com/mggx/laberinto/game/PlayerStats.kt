package com.mggx.laberinto.game

import com.mggx.laberinto.core.SaveData

/**
 * Estadisticas efectivas del jugador, ya con las mejoras permanentes
 * y las reliquias equipadas aplicadas. Se recalcula al entrar a un nivel.
 */
class PlayerStats(private val save: SaveData) {

    private fun up(id: String): Int = save.ownedLevel(id)
    private fun upMag(id: String): Float {
        val item = ItemCatalog.get(id) ?: return 0f
        return item.effect.magnitude * up(id)
    }

    /** Los poderes se compran una sola vez: o los tenes o no. */
    private fun poder(id: String): Boolean = save.ownedLevel(id) > 0
    private fun poderMag(id: String): Float =
        if (poder(id)) ItemCatalog.get(id)?.effect?.magnitude ?: 0f else 0f

    private val relicSet: Set<String> = save.relics().toSet()
    fun hasRelic(id: String): Boolean = relicSet.contains(id)
    private fun relicMag(id: String): Float =
        if (hasRelic(id)) ItemCatalog.get(id)?.effect?.magnitude ?: 0f else 0f

    // ------------------------------------------------------------- valores

    /** Velocidad de caminata en metros/segundo. */
    val walkSpeed: Float = BASE_WALK * (1f + upMag("up_botas"))
    /** Multiplicador de velocidad al correr. */
    val runMultiplier: Float = 1.65f

    val maxHealth: Float = BASE_HEALTH + upMag("up_corazon")
    val maxStamina: Float = BASE_STAMINA * (1f + upMag("up_pulmones"))

    /** Radio de la antorcha en metros. */
    val lightRadius: Float = BASE_LIGHT * (1f + upMag("up_farol"))

    /** Radio de recogida automatica de ecos. */
    val pickupRadius: Float = BASE_PICKUP + upMag("up_iman")

    /** Fraccion del minimapa ya revelada al empezar (0..1). */
    val startMapReveal: Float = upMag("up_cartografo").coerceIn(0f, 1f)

    /** Distancia a la que se ven las trampas marcadas (0 = solo al pisarlas). */
    val trapSenseRange: Float = upMag("up_sentido_peligro") + relicMag("rel_vigia")

    /** Multiplicador de tiempo de uso de objetos (menor = mas rapido). */
    val useTimeScale: Float = (1f - upMag("up_manos_firmes")).coerceAtLeast(0.25f)

    /** Fraccion de dano que se recibe (menor = mejor). */
    val damageTaken: Float = (1f - upMag("up_piel")).coerceAtLeast(0.2f)

    /** Consumo de aguante al correr, por segundo. */
    val staminaDrain: Float = BASE_STAMINA_DRAIN * (1f - upMag("up_zancada")).coerceAtLeast(0.3f)

    /** Grados extra de campo de vision. */
    val fovBonus: Float = upMag("up_vista")

    /** Multiplicador de ecos ganados al terminar el nivel. */
    val ecoYield: Float = 1f + upMag("up_instinto")

    /** Segundos que dura el rastro de pisadas en el suelo. */
    val trailSeconds: Float = 12f + upMag("up_memoria")

    /** Vidas extra por partida. */
    val extraLives: Int = up("up_cuerda")

    // --- reliquias
    val criticalArmor: Float = relicMag("rel_colmillo")           // dano evitado con vida baja
    val ecoCritChance: Float = relicMag("rel_ambar")              // prob. de eco x10
    val exitPingSeconds: Float = relicMag("rel_rosa")             // 0 = sin ping
    /** Regeneracion por segundo: la Lagrima suma con el Corazon de la Cueva. */
    val regenPerSecond: Float = relicMag("rel_lagrima") + poderMag("pod_corazon_cueva")
    val threadMultiplier: Float = if (hasRelic("rel_nudo")) 2f else 1f
    val minAmbient: Float = relicMag("rel_craneo")
    val startBurstSpeed: Float = relicMag("rel_runa")
    val startBurstSeconds: Float = if (hasRelic("rel_runa")) 30f else 0f
    val consumableSaveChance: Float = relicMag("rel_gemelo")
    val vetagrisIncome: Boolean = hasRelic("rel_ojo_vetagris")
    val heatImmune: Boolean = hasRelic("rel_escama")
    val freeSonarSeconds: Float = relicMag("rel_diapason")

    // ------------------------------------------------------------- poderes

    /** Linterna de carburo: haz dirigido con bateria. */
    val tieneLinterna: Boolean = poder("pod_linterna")
    /** Reptador: agachado y arrastrandose no perdes velocidad. */
    val posturaLibre: Boolean = poder("pod_reptador")
    /** Pies de Cabra: saltas mas alto y no te lastima ninguna caida. */
    val saltoExtra: Float = if (poder("pod_cabra")) poderMag("pod_cabra") else 1f
    val sinDanoDeCaida: Boolean = poder("pod_cabra")
    /** Ojo de la Veta: cristales y cofres marcados desde el arranque. */
    val verTesoros: Boolean = poder("pod_veta")
    /** Pico Eterno: golpes de pico gratis al empezar cada nivel. */
    val picosGratis: Int = if (poder("pod_pico_eterno")) 1 else 0
    /** Memoria de la Sima: el mapa explorado sobrevive al reintento. */
    val mapaPersistente: Boolean = poder("pod_memoria_sima")
    /**
     * Paso de Sombra: los bichos te notan a la mitad de distancia, y
     * arrastrandote directamente no te ven.
     */
    val sigilo: Float = if (poder("pod_sombra")) poderMag("pod_sombra") else 1f
    val invisibleArrastrandose: Boolean = poder("pod_sombra")

    /** Indice de guantes cosmeticos (0..4). */
    val gloveStyle: Int = (ItemCatalog.get(save.cosmeticGloves)?.effect?.magnitude ?: 0f).toInt()
    /** Color de la luz de la antorcha en ARGB. */
    val lightTint: Long = ItemCatalog.get(save.cosmeticLight)?.effect?.color ?: 0xFFFFC58AL

    // --- skin del personaje
    private val skinItem = ItemCatalog.get(save.cosmeticSkin)
    /** Color de la piel de las manos, en ARGB. */
    val skinTint: Long = skinItem?.effect?.color ?: 0xFF95664FL
    /** Color del traje / manga, en ARGB. */
    val suitTint: Long = skinItem?.effect?.color2 ?: 0xFF60422AL
    /** Indice de skin (0..5): el shader lo usa para los detalles propios. */
    val skinStyle: Int = (skinItem?.effect?.magnitude ?: 0f).toInt()

    companion object {
        const val BASE_WALK = 3.2f
        const val BASE_HEALTH = 100f
        const val BASE_STAMINA = 100f
        const val BASE_LIGHT = 7.8f
        const val BASE_PICKUP = 1.1f
        const val BASE_STAMINA_DRAIN = 22f
        const val STAMINA_REGEN = 16f
    }
}
