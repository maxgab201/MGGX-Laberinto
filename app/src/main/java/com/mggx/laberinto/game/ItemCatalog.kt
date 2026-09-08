package com.mggx.laberinto.game

import com.mggx.laberinto.ui.icons.IconId

/**
 * Catalogo completo de la tienda: 61 objetos, todos con un efecto propio.
 * No hay dos entradas que compartan (EffectType, magnitud, duracion).
 * La consistencia se verifica en los tests unitarios.
 */
object ItemCatalog {

    val all: List<ShopItem> = buildList {

        // ============================ CONSUMIBLES (25) ============================
        add(ShopItem("antorcha_sebo", "Antorcha de Sebo",
            "Grasa de cabra ardiendo lento. Agranda el circulo de luz un 35% durante un minuto.",
            ItemKind.CONSUMIBLE, Rarity.COMUN, Currency.ECOS, 40,
            ItemEffect(EffectType.LUZ_RADIO, 1.35f, 60f), IconId.ANTORCHA))

        add(ShopItem("antorcha_fosforo", "Antorcha de Fosforo",
            "Arde con llama azul. Casi el doble de alcance, pero se consume rapido.",
            ItemKind.CONSUMIBLE, Rarity.RARO, Currency.ECOS, 95,
            ItemEffect(EffectType.LUZ_RADIO, 1.75f, 32f), IconId.LLAMA_AZUL, unlockLevel = 4))

        add(ShopItem("pocion_zancada", "Pocion de Zancada",
            "Las piernas se te aflojan solas: +30% de velocidad por 25 segundos.",
            ItemKind.CONSUMIBLE, Rarity.COMUN, Currency.ECOS, 60,
            ItemEffect(EffectType.VELOCIDAD, 1.30f, 25f), IconId.FRASCO))

        add(ShopItem("elixir_aliento", "Elixir de Aliento",
            "Un trago de aire embotellado. Recupera todo el aguante al instante.",
            ItemKind.CONSUMIBLE, Rarity.COMUN, Currency.ECOS, 45,
            ItemEffect(EffectType.RECARGA_AGUANTE, 1.0f), IconId.BURBUJA))

        add(ShopItem("hilo_ariadna", "Hilo de Ariadna",
            "Deja un rastro brillante en el piso por donde ya pasaste. Dura 90 segundos.",
            ItemKind.CONSUMIBLE, Rarity.RARO, Currency.ECOS, 110,
            ItemEffect(EffectType.HILO_ARIADNA, 1f, 90f), IconId.HILO, unlockLevel = 3))

        add(ShopItem("brujula_hueso", "Brujula de Hueso",
            "Una aguja tallada que apunta a la salida. Aparece flotando 20 segundos.",
            ItemKind.CONSUMIBLE, Rarity.RARO, Currency.ECOS, 130,
            ItemEffect(EffectType.BRUJULA, 1f, 20f), IconId.BRUJULA, unlockLevel = 5))

        add(ShopItem("ojo_murcielago", "Ojo de Murcielago",
            "Ves en la oscuridad total: todo se ilumina apenas, sin fuente de luz.",
            ItemKind.CONSUMIBLE, Rarity.RARO, Currency.ECOS, 150,
            ItemEffect(EffectType.VISION_NOCTURNA, 0.28f, 40f), IconId.OJO, unlockLevel = 6))

        add(ShopItem("mapa_parcial", "Boceto del Minero",
            "Un pedazo de mapa arrancado: revela un 35% del nivel en el minimapa.",
            ItemKind.CONSUMIBLE, Rarity.COMUN, Currency.ECOS, 80,
            ItemEffect(EffectType.REVELAR_MAPA, 0.35f), IconId.MAPA, unlockLevel = 2))

        add(ShopItem("mapa_completo", "Carta de la Sima",
            "El plano entero del nivel, hasta el ultimo pasillo. Se revela todo.",
            ItemKind.CONSUMIBLE, Rarity.EPICO, Currency.ECOS, 220,
            ItemEffect(EffectType.REVELAR_MAPA, 1.0f), IconId.PERGAMINO, unlockLevel = 9))

        add(ShopItem("tiza_luminosa", "Tiza Luminosa",
            "Te da 5 marcas para dejar donde vos quieras. Se ven en 3D y en el mapa.",
            ItemKind.CONSUMIBLE, Rarity.COMUN, Currency.ECOS, 55,
            ItemEffect(EffectType.TIZA, 1f, 0f, charges = 5), IconId.TIZA, unlockLevel = 2))

        add(ShopItem("pico_mano", "Pico de Mano",
            "Rompe una pared de un golpe y te abre un atajo permanente en el nivel.",
            ItemKind.CONSUMIBLE, Rarity.EPICO, Currency.ECOS, 180,
            ItemEffect(EffectType.ROMPE_PARED, 1f, 0f, charges = 1), IconId.PICO, unlockLevel = 8))

        add(ShopItem("salto_fantasma", "Salto Fantasma",
            "Te desmaterializa lo justo para cruzar una pared entera. Un solo uso.",
            ItemKind.CONSUMIBLE, Rarity.EPICO, Currency.ECOS, 240,
            ItemEffect(EffectType.ATRAVESAR_PARED, 1f, 0f, charges = 1), IconId.FANTASMA, unlockLevel = 12))

        add(ShopItem("piedra_iman", "Piedra Iman",
            "Todos los ecos sueltos del nivel vuelan hacia vos de una.",
            ItemKind.CONSUMIBLE, Rarity.RARO, Currency.ECOS, 200,
            ItemEffect(EffectType.IMAN_TOTAL, 1f), IconId.IMAN, unlockLevel = 7))

        add(ShopItem("reloj_arena", "Reloj de Arena Negra",
            "Congela el cronometro del nivel 30 segundos. El tiempo no corre.",
            ItemKind.CONSUMIBLE, Rarity.RARO, Currency.ECOS, 120,
            ItemEffect(EffectType.CONGELAR_RELOJ, 1f, 30f), IconId.RELOJ, unlockLevel = 5))

        add(ShopItem("bengala", "Bengala de Mina",
            "Un fogonazo que ilumina toda la galeria alrededor durante 15 segundos.",
            ItemKind.CONSUMIBLE, Rarity.COMUN, Currency.ECOS, 70,
            ItemEffect(EffectType.BENGALA, 3.2f, 15f), IconId.BENGALA, unlockLevel = 3))

        add(ShopItem("vendaje_musgo", "Vendaje de Musgo",
            "Musgo cicatrizante bien apretado. Cura 40 puntos de vida.",
            ItemKind.CONSUMIBLE, Rarity.COMUN, Currency.ECOS, 50,
            ItemEffect(EffectType.CURAR, 40f), IconId.VENDA))

        add(ShopItem("tonico_hierro", "Tonico de Hierro",
            "Te endurece la piel: la mitad del dano recibido durante 45 segundos.",
            ItemKind.CONSUMIBLE, Rarity.RARO, Currency.ECOS, 140,
            ItemEffect(EffectType.RESISTENCIA, 0.50f, 45f), IconId.ESCUDO, unlockLevel = 6))

        add(ShopItem("alas_polilla", "Alas de Polilla",
            "Girar la cabeza cuesta nada: +60% de velocidad de camara por 30 segundos.",
            ItemKind.CONSUMIBLE, Rarity.COMUN, Currency.ECOS, 65,
            ItemEffect(EffectType.GIRO_RAPIDO, 1.60f, 30f), IconId.ALA, unlockLevel = 4))

        add(ShopItem("semilla_retorno", "Semilla de Retorno",
            "Te devuelve al ultimo cruce de caminos que pisaste. Ideal si te perdiste.",
            ItemKind.CONSUMIBLE, Rarity.RARO, Currency.ECOS, 160,
            ItemEffect(EffectType.VOLVER_CRUCE, 1f, 0f, charges = 1), IconId.SEMILLA, unlockLevel = 10))

        add(ShopItem("grito_eco", "Grito de Eco",
            "Un alarido que dibuja el contorno de las paredes cercanas por 10 segundos.",
            ItemKind.CONSUMIBLE, Rarity.EPICO, Currency.ECOS, 175,
            ItemEffect(EffectType.SONAR, 9f, 10f), IconId.ONDA, unlockLevel = 11))

        add(ShopItem("aceite_resbaladizo", "Aceite Resbaladizo",
            "Arrancas y frenas mucho mas rapido durante 20 segundos. Cuidado en las curvas.",
            ItemKind.CONSUMIBLE, Rarity.COMUN, Currency.ECOS, 75,
            ItemEffect(EffectType.DESLIZAR, 1.45f, 20f), IconId.GOTA, unlockLevel = 5))

        add(ShopItem("nectar_suerte", "Nectar de Suerte",
            "Cada eco que levantes vale el doble durante un minuto entero.",
            ItemKind.CONSUMIBLE, Rarity.EPICO, Currency.ECOS, 190,
            ItemEffect(EffectType.BONUS_ECOS, 2.0f, 60f), IconId.ESTRELLA_SUERTE, unlockLevel = 7))

        add(ShopItem("vial_sombra", "Vial de Sombra",
            "Las trampas no te registran durante 25 segundos. Pasas como si nada.",
            ItemKind.CONSUMIBLE, Rarity.EPICO, Currency.ECOS, 210,
            ItemEffect(EffectType.INMUNE_TRAMPAS, 1f, 25f), IconId.SOMBRA, unlockLevel = 13))

        add(ShopItem("cristal_retroceso", "Cristal de Retroceso",
            "Anula por completo el proximo golpe de trampa que recibas.",
            ItemKind.CONSUMIBLE, Rarity.RARO, Currency.ECOS, 155,
            ItemEffect(EffectType.DESHACER_TRAMPA, 1f, 0f, charges = 1), IconId.CRISTAL_TIEMPO, unlockLevel = 9))

        add(ShopItem("pan_cueva", "Pan de Cueva",
            "Duro como piedra pero llena. Devuelve 25 de vida y 25% de aguante.",
            ItemKind.CONSUMIBLE, Rarity.COMUN, Currency.ECOS, 35,
            ItemEffect(EffectType.COMIDA, 25f), IconId.PAN))

        // ============================= MEJORAS (15) ==============================
        add(ShopItem("up_botas", "Botas de Espeleologo",
            "Suela de caucho pegada a la roca. +4% de velocidad base por nivel.",
            ItemKind.MEJORA, Rarity.COMUN, Currency.ECOS, 150,
            ItemEffect(EffectType.UP_VELOCIDAD, 0.04f), IconId.BOTA, maxLevel = 5))

        add(ShopItem("up_pulmones", "Pulmones de Grieta",
            "Te acostumbras al aire viciado. +12% de aguante maximo por nivel.",
            ItemKind.MEJORA, Rarity.COMUN, Currency.ECOS, 140,
            ItemEffect(EffectType.UP_AGUANTE, 0.12f), IconId.PULMON, maxLevel = 5))

        add(ShopItem("up_corazon", "Corazon de Basalto",
            "Aguantas mucho mas castigo. +15 puntos de vida maxima por nivel.",
            ItemKind.MEJORA, Rarity.RARO, Currency.ECOS, 160,
            ItemEffect(EffectType.UP_VIDA, 15f), IconId.CORAZON, maxLevel = 5))

        add(ShopItem("up_farol", "Farol Perpetuo",
            "Tu luz de base nunca se apaga y crece +8% por nivel de mejora.",
            ItemKind.MEJORA, Rarity.RARO, Currency.ECOS, 175,
            ItemEffect(EffectType.UP_LUZ_BASE, 0.08f), IconId.FAROL, maxLevel = 5))

        add(ShopItem("up_bolsillos", "Bolsillos Profundos",
            "Una ranura extra de consumible por nivel. Entras mejor equipado.",
            ItemKind.MEJORA, Rarity.RARO, Currency.ECOS, 260,
            ItemEffect(EffectType.UP_RANURAS, 1f), IconId.MOCHILA, maxLevel = 4))

        add(ShopItem("up_iman", "Iman de Ecos",
            "Los ecos saltan solos a tu bolsillo desde mas lejos. +0.6 m por nivel.",
            ItemKind.MEJORA, Rarity.COMUN, Currency.ECOS, 190,
            ItemEffect(EffectType.UP_RADIO_RECOGIDA, 0.6f), IconId.ESPIRAL, maxLevel = 4))

        add(ShopItem("up_cartografo", "Cartografo Innato",
            "Arrancas cada nivel con un 10% del minimapa ya dibujado, por nivel.",
            ItemKind.MEJORA, Rarity.RARO, Currency.ECOS, 220,
            ItemEffect(EffectType.UP_MAPA_INICIAL, 0.10f), IconId.CUADRICULA, maxLevel = 3))

        add(ShopItem("up_sentido_peligro", "Sentido del Peligro",
            "Las trampas se te marcan en rojo desde 3 metros mas lejos por nivel.",
            ItemKind.MEJORA, Rarity.RARO, Currency.ECOS, 240,
            ItemEffect(EffectType.UP_SENTIDO_TRAMPAS, 3.0f), IconId.CALAVERA, maxLevel = 3, unlockLevel = 5))

        add(ShopItem("up_manos_firmes", "Manos Firmes",
            "Usar un objeto tarda un 15% menos por nivel. Sin temblores.",
            ItemKind.MEJORA, Rarity.COMUN, Currency.ECOS, 200,
            ItemEffect(EffectType.UP_VELOCIDAD_USO, 0.15f), IconId.MANO, maxLevel = 3))

        add(ShopItem("up_piel", "Piel de Estalactita",
            "Costra mineral sobre la piel: -8% de dano recibido por nivel.",
            ItemKind.MEJORA, Rarity.RARO, Currency.ECOS, 230,
            ItemEffect(EffectType.UP_REDUCE_DANO, 0.08f), IconId.ROCA, maxLevel = 4, unlockLevel = 4))

        add(ShopItem("up_zancada", "Zancada Silenciosa",
            "Correr te cuesta un 20% menos de aguante por nivel de mejora.",
            ItemKind.MEJORA, Rarity.COMUN, Currency.ECOS, 210,
            ItemEffect(EffectType.UP_GASTO_AGUANTE, 0.20f), IconId.PLUMA, maxLevel = 3))

        add(ShopItem("up_vista", "Vista de Halcon",
            "Abris el campo de vision 6 grados por nivel. Ves mucho mas de reojo.",
            ItemKind.MEJORA, Rarity.COMUN, Currency.ECOS, 185,
            ItemEffect(EffectType.UP_FOV, 6f), IconId.CATALEJO, maxLevel = 3))

        add(ShopItem("up_instinto", "Instinto Recolector",
            "+10% de ecos ganados al terminar cada nivel, por nivel de mejora.",
            ItemKind.MEJORA, Rarity.RARO, Currency.ECOS, 280,
            ItemEffect(EffectType.UP_RENDIMIENTO_ECOS, 0.10f), IconId.SACO_MONEDAS, maxLevel = 4))

        add(ShopItem("up_memoria", "Memoria Muscular",
            "Tus pasos quedan marcados en el piso mucho mas tiempo. +40 s por nivel.",
            ItemKind.MEJORA, Rarity.EPICO, Currency.ECOS, 300,
            ItemEffect(EffectType.UP_RASTRO_PERMANENTE, 40f), IconId.HUELLA, maxLevel = 3, unlockLevel = 7))

        add(ShopItem("up_cuerda", "Cuerda de Rescate",
            "Una vida extra por nivel de mejora: si caes, te levantas donde estabas.",
            ItemKind.MEJORA, Rarity.LEGENDARIO, Currency.VETAGRIS, 3,
            ItemEffect(EffectType.UP_VIDAS_EXTRA, 1f), IconId.CUERDA, maxLevel = 2, unlockLevel = 8))

        // ============================ RELIQUIAS (12) =============================
        add(ShopItem("rel_colmillo", "Colmillo del Guardian",
            "Con la vida por debajo del 30% evitas un 20% mas de dano. Se activa sola.",
            ItemKind.RELIQUIA, Rarity.EPICO, Currency.VETAGRIS, 4,
            ItemEffect(EffectType.REL_ARMADURA_CRITICA, 0.20f), IconId.COLMILLO, unlockLevel = 10))

        add(ShopItem("rel_ambar", "Ambar del Minero",
            "3% de posibilidad de que un eco cualquiera valga diez veces mas.",
            ItemKind.RELIQUIA, Rarity.RARO, Currency.VETAGRIS, 3,
            ItemEffect(EffectType.REL_ECO_CRITICO, 0.03f), IconId.AMBAR, unlockLevel = 8))

        add(ShopItem("rel_rosa", "Rosa de los Vientos",
            "Cada 45 segundos la salida parpadea un instante en el minimapa.",
            ItemKind.RELIQUIA, Rarity.EPICO, Currency.VETAGRIS, 5,
            ItemEffect(EffectType.REL_PING_SALIDA, 45f), IconId.ROSA_VIENTOS, unlockLevel = 12))

        add(ShopItem("rel_lagrima", "Lagrima de Estalagmita",
            "Regeneras 1 punto de vida por segundo mientras no te lastimen.",
            ItemKind.RELIQUIA, Rarity.EPICO, Currency.VETAGRIS, 4,
            ItemEffect(EffectType.REL_REGENERACION, 1.0f), IconId.LAGRIMA, unlockLevel = 11))

        add(ShopItem("rel_nudo", "Nudo del Espeleologo",
            "El Hilo de Ariadna te dura exactamente el doble de tiempo.",
            ItemKind.RELIQUIA, Rarity.RARO, Currency.VETAGRIS, 2,
            ItemEffect(EffectType.REL_HILO_DOBLE, 2.0f), IconId.NUDO, unlockLevel = 9))

        add(ShopItem("rel_craneo", "Craneo de Luciernaga",
            "Nunca hay negro absoluto: siempre queda una luz ambiental minima.",
            ItemKind.RELIQUIA, Rarity.EPICO, Currency.VETAGRIS, 5,
            ItemEffect(EffectType.REL_LUZ_MINIMA, 0.12f), IconId.LUCIERNAGA, unlockLevel = 14))

        add(ShopItem("rel_runa", "Runa de Prisa",
            "+15% de velocidad durante los primeros 30 segundos de cada nivel.",
            ItemKind.RELIQUIA, Rarity.RARO, Currency.VETAGRIS, 3,
            ItemEffect(EffectType.REL_ARRANQUE_VELOZ, 0.15f, 30f), IconId.RUNA, unlockLevel = 10))

        add(ShopItem("rel_vigia", "Piedra del Vigia",
            "Las trampas a menos de 5 metros emiten un zumbido que solo vos escuchas.",
            ItemKind.RELIQUIA, Rarity.RARO, Currency.VETAGRIS, 3,
            ItemEffect(EffectType.REL_ZUMBIDO_TRAMPA, 5.0f), IconId.TORRE_VIGIA, unlockLevel = 13))

        add(ShopItem("rel_gemelo", "Corazon Gemelo",
            "Uno de cada cuatro consumibles que uses no se gasta. Magia pura.",
            ItemKind.RELIQUIA, Rarity.LEGENDARIO, Currency.VETAGRIS, 6,
            ItemEffect(EffectType.REL_AHORRO_CONSUMIBLE, 0.25f), IconId.CORAZONES_GEMELOS, unlockLevel = 18))

        add(ShopItem("rel_ojo_vetagris", "Ojo de Vetagris",
            "Cada 5 niveles completados te llevas 1 Vetagris garantizado de regalo.",
            ItemKind.RELIQUIA, Rarity.LEGENDARIO, Currency.VETAGRIS, 8,
            ItemEffect(EffectType.REL_RENTA_VETAGRIS, 1f), IconId.VETA, unlockLevel = 20))

        add(ShopItem("rel_escama", "Escama de Salamandra",
            "El calor de las Venas de Magma deja de hacerte dano por completo.",
            ItemKind.RELIQUIA, Rarity.EPICO, Currency.VETAGRIS, 4,
            ItemEffect(EffectType.REL_INMUNE_CALOR, 1f), IconId.ESCAMA, unlockLevel = 26))

        add(ShopItem("rel_diapason", "Diapason de Cuarzo",
            "Cada 60 segundos podes lanzar un Grito de Eco sin gastar nada.",
            ItemKind.RELIQUIA, Rarity.LEGENDARIO, Currency.VETAGRIS, 7,
            ItemEffect(EffectType.REL_SONAR_GRATIS, 60f), IconId.DIAPASON, unlockLevel = 22))

        // =========================== COSMETICOS (9) ==============================
        add(ShopItem("cos_guantes_cuero", "Guantes de Cuero Curtido",
            "Los de toda la vida. Gastados, comodos y confiables.",
            ItemKind.COSMETICO, Rarity.COMUN, Currency.ECOS, 0,
            ItemEffect(EffectType.COS_GUANTES, 0f), IconId.GUANTE))

        add(ShopItem("cos_guantes_malla", "Guantes de Malla de Hierro",
            "Anillos de hierro remachados que brillan cuando les pega la antorcha.",
            ItemKind.COSMETICO, Rarity.RARO, Currency.ECOS, 350,
            ItemEffect(EffectType.COS_GUANTES, 1f), IconId.MALLA, unlockLevel = 6))

        add(ShopItem("cos_manos_ceniza", "Manos de Ceniza",
            "Piel agrietada con brasas vivas adentro. Nadie te pregunta que te paso.",
            ItemKind.COSMETICO, Rarity.EPICO, Currency.ECOS, 700,
            ItemEffect(EffectType.COS_GUANTES, 2f), IconId.CENIZA, unlockLevel = 15))

        add(ShopItem("cos_guantes_cazador", "Guantes de Cazador de Vetagris",
            "Cuero reforzado con una gema incrustada en el dorso de cada mano.",
            ItemKind.COSMETICO, Rarity.EPICO, Currency.VETAGRIS, 3,
            ItemEffect(EffectType.COS_GUANTES, 3f), IconId.CAZADOR, unlockLevel = 20))

        add(ShopItem("cos_brazaletes", "Brazaletes de Cristal Vivo",
            "El cuarzo creció sobre tus munecas y late al ritmo de tus pasos.",
            ItemKind.COSMETICO, Rarity.LEGENDARIO, Currency.VETAGRIS, 6,
            ItemEffect(EffectType.COS_GUANTES, 4f), IconId.BRAZALETE, unlockLevel = 30))

        add(ShopItem("cos_luz_calida", "Luz Calida",
            "El naranja de siempre de una antorcha honesta.",
            ItemKind.COSMETICO, Rarity.COMUN, Currency.ECOS, 0,
            ItemEffect(EffectType.COS_TINTE_LUZ, 0f, color = 0xFFFFC58AL), IconId.TINTE_CALIDO))

        add(ShopItem("cos_luz_ambar", "Luz de Ambar",
            "Un dorado espeso que hace ver la roca mas viva.",
            ItemKind.COSMETICO, Rarity.COMUN, Currency.ECOS, 200,
            ItemEffect(EffectType.COS_TINTE_LUZ, 1f, color = 0xFFFFB25EL), IconId.TINTE_AMBAR))

        add(ShopItem("cos_luz_espectral", "Luz Espectral",
            "Verde palido de fuego fatuo. Frio y raro, como la cueva profunda.",
            ItemKind.COSMETICO, Rarity.RARO, Currency.ECOS, 400,
            ItemEffect(EffectType.COS_TINTE_LUZ, 2f, color = 0xFF7BF0A8L), IconId.TINTE_VERDE, unlockLevel = 10))

        add(ShopItem("cos_luz_carmesi", "Luz Carmesi",
            "Rojo de forja. Todo parece a punto de prenderse fuego.",
            ItemKind.COSMETICO, Rarity.EPICO, Currency.ECOS, 550,
            ItemEffect(EffectType.COS_TINTE_LUZ, 3f, color = 0xFFFF6A5EL), IconId.TINTE_ROJO, unlockLevel = 16))
    }

    private val byId: Map<String, ShopItem> = all.associateBy { it.id }

    fun get(id: String): ShopItem? = byId[id]
    fun require(id: String): ShopItem = byId.getValue(id)

    fun ofKind(kind: ItemKind): List<ShopItem> = all.filter { it.kind == kind }

    /** Objetos que ya se pueden comprar segun el nivel maximo alcanzado. */
    fun available(maxLevelReached: Int, kind: ItemKind): List<ShopItem> =
        ofKind(kind).filter { it.unlockLevel <= maxLevelReached }

    /** Objetos que el jugador tiene de arranque, sin pagar. */
    val defaultsOwned = listOf("cos_guantes_cuero", "cos_luz_calida")

    const val RELIC_SLOTS = 3
}
