package com.mggx.laberinto.ui

import androidx.compose.ui.graphics.Color
import com.mggx.laberinto.game.EffectType
import com.mggx.laberinto.game.ItemKind
import com.mggx.laberinto.game.Rarity
import com.mggx.laberinto.game.ShopItem
import com.mggx.laberinto.ui.theme.Cave
import kotlin.math.roundToInt

/**
 * Traduce el efecto de un objeto a una frase que se entienda sin saber nada
 * de numeros internos. Nada de "magnitude 1.35": "un 35% mas de luz".
 */
object ItemText {

    private fun pct(v: Float): String = "${(v * 100f).roundToInt()}%"
    private fun pctMore(mult: Float): String = "${((mult - 1f) * 100f).roundToInt()}%"
    private fun secs(v: Float): String {
        val s = v.roundToInt()
        return if (s >= 60 && s % 60 == 0) "${s / 60} min" else "$s s"
    }

    /** Frase corta del efecto, para la tarjeta. */
    fun shortEffect(item: ShopItem): String {
        val e = item.effect
        return when (e.type) {
            EffectType.LUZ_RADIO -> "+${pctMore(e.magnitude)} de luz - ${secs(e.duration)}"
            EffectType.VELOCIDAD -> "+${pctMore(e.magnitude)} de velocidad - ${secs(e.duration)}"
            EffectType.RECARGA_AGUANTE -> "Aguante al maximo"
            EffectType.HILO_ARIADNA -> "Rastro visible - ${secs(e.duration)}"
            EffectType.BRUJULA -> "Flecha a la salida - ${secs(e.duration)}"
            EffectType.VISION_NOCTURNA -> "Ves sin luz - ${secs(e.duration)}"
            EffectType.REVELAR_MAPA ->
                if (e.magnitude >= 1f) "Revela todo el mapa" else "Revela ${pct(e.magnitude)} del mapa"
            EffectType.TIZA -> "${e.charges} marcas para dejar"
            EffectType.ROMPE_PARED -> "Rompe ${e.charges} pared"
            EffectType.ATRAVESAR_PARED -> "Cruza ${e.charges} pared"
            EffectType.IMAN_TOTAL -> "Atrae todos los ecos"
            EffectType.CONGELAR_RELOJ -> "Congela el reloj ${secs(e.duration)}"
            EffectType.BENGALA -> "Luz enorme - ${secs(e.duration)}"
            EffectType.CURAR -> "Cura ${e.magnitude.roundToInt()} de vida"
            EffectType.RESISTENCIA -> "-${pct(e.magnitude)} de dano - ${secs(e.duration)}"
            EffectType.GIRO_RAPIDO -> "+${pctMore(e.magnitude)} al girar - ${secs(e.duration)}"
            EffectType.VOLVER_CRUCE -> "Volves al ultimo cruce"
            EffectType.SONAR -> "Ves las paredes - ${secs(e.duration)}"
            EffectType.DESLIZAR -> "Arrancas mas rapido - ${secs(e.duration)}"
            EffectType.BONUS_ECOS -> "Ecos x${e.magnitude.roundToInt()} - ${secs(e.duration)}"
            EffectType.INMUNE_TRAMPAS -> "Trampas sin efecto - ${secs(e.duration)}"
            EffectType.DESHACER_TRAMPA -> "Anula ${e.charges} trampa"
            EffectType.COMIDA -> "+${e.magnitude.roundToInt()} vida y aguante"

            EffectType.UP_VELOCIDAD -> "+${pct(e.magnitude)} de velocidad por nivel"
            EffectType.UP_AGUANTE -> "+${pct(e.magnitude)} de aguante por nivel"
            EffectType.UP_VIDA -> "+${e.magnitude.roundToInt()} de vida por nivel"
            EffectType.UP_LUZ_BASE -> "+${pct(e.magnitude)} de luz base por nivel"
            EffectType.UP_RANURAS -> "+1 ranura rapida por nivel"
            EffectType.UP_RADIO_RECOGIDA -> "+${e.magnitude} m de alcance por nivel"
            EffectType.UP_MAPA_INICIAL -> "+${pct(e.magnitude)} de mapa inicial por nivel"
            EffectType.UP_SENTIDO_TRAMPAS -> "+${e.magnitude.roundToInt()} m para ver trampas"
            EffectType.UP_VELOCIDAD_USO -> "-${pct(e.magnitude)} al usar objetos"
            EffectType.UP_REDUCE_DANO -> "-${pct(e.magnitude)} de dano por nivel"
            EffectType.UP_GASTO_AGUANTE -> "-${pct(e.magnitude)} de gasto al correr"
            EffectType.UP_FOV -> "+${e.magnitude.roundToInt()} grados de vision"
            EffectType.UP_RENDIMIENTO_ECOS -> "+${pct(e.magnitude)} de ecos al terminar"
            EffectType.UP_RASTRO_PERMANENTE -> "+${secs(e.magnitude)} de rastro"
            EffectType.UP_VIDAS_EXTRA -> "+1 vida extra por nivel"

            EffectType.REL_ARMADURA_CRITICA -> "-${pct(e.magnitude)} de dano con poca vida"
            EffectType.REL_ECO_CRITICO -> "${pct(e.magnitude)} de que un eco valga x10"
            EffectType.REL_PING_SALIDA -> "La salida parpadea cada ${secs(e.magnitude)}"
            EffectType.REL_REGENERACION -> "+${e.magnitude.roundToInt()} de vida por segundo"
            EffectType.REL_HILO_DOBLE -> "El Hilo dura el doble"
            EffectType.REL_LUZ_MINIMA -> "Nunca hay oscuridad total"
            EffectType.REL_ARRANQUE_VELOZ -> "+${pct(e.magnitude)} al arrancar el nivel"
            EffectType.REL_ZUMBIDO_TRAMPA -> "Zumbido a ${e.magnitude.roundToInt()} m de una trampa"
            EffectType.REL_AHORRO_CONSUMIBLE -> "${pct(e.magnitude)} de no gastar el objeto"
            EffectType.REL_RENTA_VETAGRIS -> "1 Vetagris cada 5 niveles"
            EffectType.REL_INMUNE_CALOR -> "Inmune al calor de la roca"
            EffectType.REL_SONAR_GRATIS -> "Grito de Eco gratis cada ${secs(e.magnitude)}"

            EffectType.COS_GUANTES -> "Cambia como se ven tus manos"
            EffectType.COS_TINTE_LUZ -> "Cambia el color de tu luz"
        }
    }

    /** Como se usa el objeto, en una linea. */
    fun usage(item: ShopItem): String = when (item.kind) {
        ItemKind.CONSUMIBLE -> "Se usa desde las ranuras rapidas durante la partida."
        ItemKind.MEJORA -> "Se aplica sola, para siempre, apenas la compras."
        ItemKind.RELIQUIA -> "Equipala en una de las 3 ranuras de reliquia."
        ItemKind.COSMETICO -> "Se equipa al comprarla. No cambia como se juega."
    }

    fun rarityAccent(r: Rarity): Color = when (r) {
        Rarity.COMUN -> Cave.TextDim
        Rarity.RARO -> Cave.Ice
        Rarity.EPICO -> Color(0xFFB673E8)
        Rarity.LEGENDARIO -> Cave.Amber
    }
}
