package com.mggx.laberinto.core

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mggx.laberinto.game.Currency
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import org.json.JSONArray
import org.json.JSONObject

/**
 * Perfil del jugador. Vive en SharedPreferences como un unico JSON,
 * asi que guardar es atomico y no puede quedar a medias.
 */
class SaveData private constructor(private val store: Store) {

    /**
     * Donde se guarda el perfil. En el telefono son SharedPreferences;
     * en los tests es un simple mapa en memoria.
     */
    interface Store {
        fun read(): String?
        fun write(json: String)
    }

    private class PrefsStore(private val prefs: SharedPreferences) : Store {
        override fun read(): String? = prefs.getString(KEY, null)
        override fun write(json: String) { prefs.edit().putString(KEY, json).apply() }
    }


    // --------------------------------------------------------------- estado
    var ecos: Int = 0; private set
    var vetagris: Int = 0; private set

    /** Nivel mas alto desbloqueado (siempre >= 1). */
    var maxLevel: Int = 1; private set
    /** Nivel que el jugador va a jugar al tocar "Continuar". */
    var currentLevel: Int = 1; private set

    /** id -> nivel comprado (1 para objetos simples, 1..maxLevel para mejoras). */
    private val owned = HashMap<String, Int>()
    /** id -> cantidad, solo consumibles. */
    private val stock = HashMap<String, Int>()
    /** Reliquias equipadas (maximo 3). */
    private val equippedRelics = ArrayList<String>()
    /** Consumibles cargados en las ranuras rapidas de la partida. */
    private val loadout = ArrayList<String>()

    var cosmeticGloves: String = "cos_guantes_cuero"; private set
    var cosmeticLight: String = "cos_luz_calida"; private set
    /** Skin del personaje: define la piel y el traje que se ven en primera persona. */
    var cosmeticSkin: String = "skin_minero"; private set
    /** Arma que llevas en la mano. Vacio = a mano limpia. */
    var armaEquipada: String = ""; private set

    /**
     * Como te ven los demas en una sala de multijugador. Vacio = todavia no
     * elegiste ninguno y se usa uno por defecto.
     */
    var nombreJugador: String = ""; private set

    /** Cambia el nombre con el que entras a las salas. */
    fun setNombreJugador(n: String) {
        // El mismo recorte que hace el protocolo: si no, el nombre se veria
        // de una forma en tu pantalla y de otra en la del de al lado.
        nombreJugador = com.mggx.laberinto.net.NetProtocol.limpiar(n)
        save()
    }

    // --- estadisticas
    var totalRuns: Int = 0; private set
    var totalWins: Int = 0; private set
    var totalDeaths: Int = 0; private set
    var totalEcosGanados: Int = 0; private set
    var bestTimeMs: Long = 0L; private set
    var totalPlayMs: Long = 0L; private set
    var totalSteps: Long = 0L; private set
    /** Contador para la reliquia "Ojo de Vetagris". */
    var levelsSinceVetagris: Int = 0; private set

    // --- ajustes
    val settings = Settings()

    /**
     * Los ajustes, como estado observable de Compose.
     *
     * Cada campo es un `mutableStateOf` y no un `var` comun, y eso NO es
     * decoracion: es lo que hace que tocar un ajuste se vea en el acto.
     *
     * Con campos comunes, Compose no tiene forma de enterarse de que cambio
     * algo adentro de este objeto. Peor todavia: desde Kotlin 2.0 el
     * compilador de Compose usa "strong skipping", que compara los parametros
     * de un composable por identidad. Como este objeto es SIEMPRE el mismo,
     * toda pantalla que lo reciba se saltea el redibujado para siempre. El
     * sintoma era exactamente ese: cambiabas un ajuste, no pasaba nada, y
     * recien se veia al cambiar de pestana y volver (ahi la pantalla se arma
     * de cero y lee los valores nuevos).
     *
     * Siendo estado de verdad, cada pantalla se suscribe sola al ajuste que
     * lee y se redibuja sola cuando ese ajuste cambia. Leerlos desde el hilo
     * de OpenGL (que es donde los usa el renderer) sigue siendo valido: una
     * lectura fuera de una composicion devuelve el valor actual y no suscribe
     * a nada.
     */
    class Settings {
        var musicVolume: Float by mutableFloatStateOf(0.7f)
        var sfxVolume: Float by mutableFloatStateOf(0.85f)
        var masterVolume: Float by mutableFloatStateOf(1.0f)
        var haptics: Boolean by mutableStateOf(true)
        var invertY: Boolean by mutableStateOf(false)
        var lookSensitivity: Float by mutableFloatStateOf(1.0f)
        var gamepadSensitivity: Float by mutableFloatStateOf(1.0f)
        var stickDeadzone: Float by mutableFloatStateOf(0.16f)
        var leftHanded: Boolean by mutableStateOf(false)
        var showMinimap: Boolean by mutableStateOf(true)
        var minimapSize: Float by mutableFloatStateOf(1.0f)
        var showArms: Boolean by mutableStateOf(true)
        var headBob: Float by mutableFloatStateOf(1.0f)
        var fovExtra: Float by mutableFloatStateOf(0f)
        var quality: Int by mutableIntStateOf(2)        // 0 bajo, 1 medio, 2 alto, 3 ultra
        var renderScale: Float by mutableFloatStateOf(1.0f)
        var targetFps: Int by mutableIntStateOf(60)
        var showFps: Boolean by mutableStateOf(false)
        var brightness: Float by mutableFloatStateOf(1.0f)
        var fogIntensity: Float by mutableFloatStateOf(1.0f)
        var torchFlicker: Boolean by mutableStateOf(true)
        var autoRun: Boolean by mutableStateOf(false)
        var joystickSize: Float by mutableFloatStateOf(1.0f)
        var joystickOpacity: Float by mutableFloatStateOf(0.55f)
        var buttonScale: Float by mutableFloatStateOf(1.0f)
        var vibrateOnPickup: Boolean by mutableStateOf(true)
        var subtitles: Boolean by mutableStateOf(true)
        var tutorialDone: Boolean by mutableStateOf(false)
        var compassAlwaysOn: Boolean by mutableStateOf(false)
        var colorBlindMode: Int by mutableIntStateOf(0)  // 0 ninguno, 1 protan, 2 deutan, 3 tritan
        var uiScale: Float by mutableFloatStateOf(1.0f)
    }

    // ------------------------------------------------------------ consultas

    fun balance(c: Currency): Int = if (c == Currency.ECOS) ecos else vetagris

    fun ownedLevel(id: String): Int = owned[id] ?: 0
    fun isOwned(id: String): Boolean = ownedLevel(id) > 0
    fun stockOf(id: String): Int = stock[id] ?: 0
    fun relics(): List<String> = equippedRelics.toList()
    fun isRelicEquipped(id: String): Boolean = equippedRelics.contains(id)
    fun loadoutList(): List<String> = loadout.toList()

    /** Ranuras de consumible disponibles: 3 de base + mejora Bolsillos Profundos. */
    fun loadoutSlots(): Int = 3 + ownedLevel("up_bolsillos")

    /**
     * Todo lo que se puede usar durante la partida: primero las ranuras que
     * elegiste, y despues el resto de lo que tengas en el bolso.
     *
     * Antes la partida mostraba UNICAMENTE las ranuras. Con las ranuras llenas,
     * comprar algo lo sumaba al stock pero no aparecia por ningun lado: lo
     * pagabas y no lo podias usar nunca. Ahora las ranuras deciden el ORDEN en
     * que aparecen las cosas, no si existen.
     */
    fun bolsaDeMano(): List<String> {
        val out = ArrayList<String>(loadout.size + stock.size)
        for (id in loadout) if (stockOf(id) > 0 && !out.contains(id)) out.add(id)
        // El resto va ordenado por id para que la barra no cambie de orden sola
        // entre una partida y la siguiente.
        stock.keys.sorted().forEach { id ->
            if (out.contains(id) || stockOf(id) <= 0) return@forEach
            if (ItemCatalog.get(id)?.kind != ItemKind.CONSUMIBLE) return@forEach
            out.add(id)
        }
        return out
    }

    // ------------------------------------------------------------ mutaciones

    fun addEcos(n: Int) {
        if (n <= 0) return
        ecos += n
        totalEcosGanados += n
        save()
    }

    fun addVetagris(n: Int) {
        if (n <= 0) return
        vetagris += n
        save()
    }

    fun spend(c: Currency, n: Int): Boolean {
        if (n <= 0) return true
        if (balance(c) < n) return false
        if (c == Currency.ECOS) ecos -= n else vetagris -= n
        save()
        return true
    }

    /** Compra un objeto. Devuelve el resultado para que la UI muestre el motivo. */
    fun buy(id: String): BuyResult {
        val item = ItemCatalog.get(id) ?: return BuyResult.NO_EXISTE
        if (item.unlockLevel > maxLevel) return BuyResult.BLOQUEADO
        val lvl = ownedLevel(id)

        if (item.kind == ItemKind.CONSUMIBLE) {
            if (stockOf(id) >= MAX_STACK) return BuyResult.STACK_LLENO
            if (!spend(item.currency, item.priceAt(0))) return BuyResult.SIN_FONDOS
            stock[id] = stockOf(id) + 1
            owned[id] = 1
            // Va solo a una ranura rapida si hay lugar. Sin esto, el jugador
            // compraba algo y no lo podia usar nunca: solo se puede usar lo que
            // esta en una ranura, y eso habia que hacerlo a mano en otra
            // pantalla que nadie encontraba.
            autoEquipar(id)
            save()
            return BuyResult.OK
        }

        if (item.isLeveled) {
            if (lvl >= item.maxLevel) return BuyResult.AL_MAXIMO
            if (!spend(item.currency, item.priceAt(lvl))) return BuyResult.SIN_FONDOS
            owned[id] = lvl + 1
            save()
            return BuyResult.OK
        }

        if (lvl > 0) return BuyResult.YA_COMPRADO
        if (!spend(item.currency, item.basePrice)) return BuyResult.SIN_FONDOS
        owned[id] = 1
        // Los cosmeticos se equipan solos al comprarlos.
        when (item.kind) {
            ItemKind.COSMETICO -> equipCosmetic(id)
            ItemKind.RELIQUIA -> if (equippedRelics.size < ItemCatalog.RELIC_SLOTS) equippedRelics.add(id)
            // Un arma nueva se agarra sola: nadie compra un hacha para dejarla
            // en el bolso.
            ItemKind.ARMA -> armaEquipada = id
            else -> {}
        }
        save()
        return BuyResult.OK
    }

    enum class BuyResult(val message: String) {
        OK("Comprado"),
        SIN_FONDOS("No te alcanza"),
        YA_COMPRADO("Ya lo tenes"),
        AL_MAXIMO("Ya esta al maximo"),
        BLOQUEADO("Todavia no se desbloqueo"),
        STACK_LLENO("No podes llevar mas"),
        NO_EXISTE("Objeto desconocido")
    }

    fun equipCosmetic(id: String) {
        val item = ItemCatalog.get(id) ?: return
        if (!isOwned(id)) return
        when (item.effect.type) {
            com.mggx.laberinto.game.EffectType.COS_GUANTES -> cosmeticGloves = id
            com.mggx.laberinto.game.EffectType.COS_TINTE_LUZ -> cosmeticLight = id
            com.mggx.laberinto.game.EffectType.COS_PIEL -> cosmeticSkin = id
            else -> return
        }
        save()
    }

    /**
     * Agarra un arma. Con [id] vacio se pelea a mano limpia, que siempre es
     * una opcion valida.
     */
    fun equipArma(id: String) {
        if (id.isEmpty()) { armaEquipada = ""; save(); return }
        val item = ItemCatalog.get(id) ?: return
        if (item.kind != ItemKind.ARMA || !isOwned(id)) return
        armaEquipada = id
        save()
    }

    /** Alterna una reliquia equipada. Devuelve true si quedo equipada. */
    fun toggleRelic(id: String): Boolean {
        if (!isOwned(id)) return false
        if (equippedRelics.contains(id)) {
            equippedRelics.remove(id); save(); return false
        }
        if (equippedRelics.size >= ItemCatalog.RELIC_SLOTS) {
            equippedRelics.removeAt(0)
        }
        equippedRelics.add(id)
        save()
        return true
    }

    /** Alterna un consumible en las ranuras rapidas. */
    fun toggleLoadout(id: String): Boolean {
        if (stockOf(id) <= 0) return false
        if (loadout.contains(id)) { loadout.remove(id); save(); return false }
        if (loadout.size >= loadoutSlots()) loadout.removeAt(0)
        loadout.add(id)
        save()
        return true
    }

    /** Consume una unidad. Devuelve false si no habia. */
    fun consume(id: String): Boolean {
        val n = stockOf(id)
        if (n <= 0) return false
        if (n == 1) { stock.remove(id); loadout.remove(id) } else stock[id] = n - 1
        save()
        return true
    }

    fun grantConsumable(id: String, amount: Int = 1) {
        if (ItemCatalog.get(id) == null || amount <= 0) return
        stock[id] = (stockOf(id) + amount).coerceAtMost(MAX_STACK)
        owned[id] = 1
        autoEquipar(id)
        save()
    }

    /**
     * Mete el consumible en una ranura rapida si queda alguna libre.
     * Si estan todas ocupadas no pisa nada: el jugador ya eligio que llevar.
     */
    private fun autoEquipar(id: String) {
        if (loadout.contains(id)) return
        if (loadout.size >= loadoutSlots()) return
        loadout.add(id)
    }

    /** true si al comprar [id] entro solo en una ranura rapida. */
    fun estaEnRanuraRapida(id: String): Boolean = loadout.contains(id)

    /** true si el objeto se puede usar en la partida (esta en el bolso). */
    fun sePuedeUsarEnPartida(id: String): Boolean =
        stockOf(id) > 0 && ItemCatalog.get(id)?.kind == ItemKind.CONSUMIBLE

    /** true si no queda ninguna ranura rapida libre. */
    fun ranurasLlenas(): Boolean = loadout.size >= loadoutSlots()

    fun setCurrentLevel(level: Int) {
        currentLevel = level.coerceIn(1, maxLevel)
        save()
    }

    fun onLevelCompleted(level: Int, timeMs: Long, steps: Int) {
        totalWins++
        totalRuns++
        totalPlayMs += timeMs
        totalSteps += steps
        if (bestTimeMs == 0L || timeMs < bestTimeMs) bestTimeMs = timeMs
        if (level >= maxLevel) maxLevel = level + 1
        currentLevel = (level + 1).coerceAtMost(maxLevel)
        levelsSinceVetagris++
        save()
    }

    fun onRunFailed(timeMs: Long) {
        totalRuns++
        totalDeaths++
        totalPlayMs += timeMs
        save()
    }

    // ------------------------------------------ mapa explorado (Memoria de la Sima)

    /** Nivel del que se guardo el mapa explorado, y el mapa en si. */
    private var exploredLevel: Int = 0
    private var exploredMask: String = ""

    /**
     * Guarda que casillas viste de un nivel. Se comprime a hexadecimal, un bit
     * por casilla: un nivel grande entra en unos pocos cientos de caracteres.
     */
    fun rememberExplored(level: Int, revealed: BooleanArray) {
        val sb = StringBuilder((revealed.size + 3) / 4)
        var i = 0
        while (i < revealed.size) {
            var nib = 0
            for (b in 0 until 4) {
                if (i + b < revealed.size && revealed[i + b]) nib = nib or (1 shl b)
            }
            sb.append("0123456789abcdef"[nib])
            i += 4
        }
        exploredLevel = level
        exploredMask = sb.toString()
        save()
    }

    /** Vuelca el mapa guardado sobre [revealed] si es de este mismo nivel. */
    fun restoreExplored(level: Int, revealed: BooleanArray): Boolean {
        if (level != exploredLevel || exploredMask.isEmpty()) return false
        if (exploredMask.length != (revealed.size + 3) / 4) return false
        for (k in exploredMask.indices) {
            val nib = Character.digit(exploredMask[k], 16)
            if (nib < 0) return false
            for (b in 0 until 4) {
                val i = k * 4 + b
                if (i < revealed.size && (nib shr b) and 1 == 1) revealed[i] = true
            }
        }
        return true
    }

    fun consumeVetagrisCounter(): Boolean {
        if (levelsSinceVetagris >= 5) { levelsSinceVetagris = 0; save(); return true }
        return false
    }

    fun resetProgress() {
        ecos = 0; vetagris = 0; maxLevel = 1; currentLevel = 1
        owned.clear(); stock.clear(); equippedRelics.clear(); loadout.clear()
        cosmeticGloves = "cos_guantes_cuero"; cosmeticLight = "cos_luz_calida"
        cosmeticSkin = "skin_minero"
        armaEquipada = ""
        nombreJugador = ""
        totalRuns = 0; totalWins = 0; totalDeaths = 0; totalEcosGanados = 0
        bestTimeMs = 0; totalPlayMs = 0; totalSteps = 0; levelsSinceVetagris = 0
        exploredLevel = 0; exploredMask = ""
        ItemCatalog.defaultsOwned.forEach { owned[it] = 1 }
        save()
    }

    // ------------------------------------------------------- persistencia

    fun save() {
        val root = JSONObject()
        root.put("v", SCHEMA)
        root.put("ecos", ecos)
        root.put("vtg", vetagris)
        root.put("maxLevel", maxLevel)
        root.put("curLevel", currentLevel)
        root.put("gloves", cosmeticGloves)
        root.put("light", cosmeticLight)
        root.put("skin", cosmeticSkin)
        root.put("arma", armaEquipada)
        root.put("nombre", nombreJugador)
        root.put("owned", JSONObject(owned as Map<*, *>))
        root.put("stock", JSONObject(stock as Map<*, *>))
        root.put("relics", JSONArray(equippedRelics))
        root.put("loadout", JSONArray(loadout))
        root.put("runs", totalRuns)
        root.put("wins", totalWins)
        root.put("deaths", totalDeaths)
        root.put("earned", totalEcosGanados)
        root.put("best", bestTimeMs)
        root.put("playMs", totalPlayMs)
        root.put("steps", totalSteps)
        root.put("sinceVtg", levelsSinceVetagris)
        root.put("expLevel", exploredLevel)
        root.put("expMask", exploredMask)

        val s = JSONObject()
        with(settings) {
            s.put("music", musicVolume); s.put("sfx", sfxVolume); s.put("master", masterVolume)
            s.put("haptics", haptics); s.put("invertY", invertY); s.put("look", lookSensitivity)
            s.put("pad", gamepadSensitivity); s.put("dead", stickDeadzone); s.put("lefty", leftHanded)
            s.put("map", showMinimap); s.put("mapSize", minimapSize); s.put("arms", showArms)
            s.put("bob", headBob); s.put("fov", fovExtra); s.put("q", quality)
            s.put("rs", renderScale); s.put("fps", targetFps); s.put("showFps", showFps)
            s.put("bright", brightness); s.put("fog", fogIntensity); s.put("flicker", torchFlicker)
            s.put("autorun", autoRun); s.put("jsize", joystickSize); s.put("jop", joystickOpacity)
            s.put("btn", buttonScale); s.put("vibPick", vibrateOnPickup); s.put("subs", subtitles)
            s.put("tuto", tutorialDone); s.put("compass", compassAlwaysOn)
            s.put("cb", colorBlindMode); s.put("ui", uiScale)
        }
        root.put("settings", s)

        store.write(root.toString())
    }

    private fun load() {
        val raw = store.read()
        ItemCatalog.defaultsOwned.forEach { owned[it] = 1 }
        if (raw.isNullOrBlank()) { save(); return }
        try {
            val root = JSONObject(raw)
            ecos = root.optInt("ecos", 0)
            vetagris = root.optInt("vtg", 0)
            maxLevel = root.optInt("maxLevel", 1).coerceAtLeast(1)
            currentLevel = root.optInt("curLevel", 1).coerceIn(1, maxLevel)
            cosmeticGloves = root.optString("gloves", "cos_guantes_cuero")
            cosmeticLight = root.optString("light", "cos_luz_calida")
            cosmeticSkin = root.optString("skin", "skin_minero")
            armaEquipada = root.optString("arma", "")
            nombreJugador = root.optString("nombre", "")
            exploredLevel = root.optInt("expLevel", 0)
            exploredMask = root.optString("expMask", "")

            root.optJSONObject("owned")?.let { o ->
                o.keys().forEach { k -> if (ItemCatalog.get(k) != null) owned[k] = o.optInt(k, 0) }
            }
            root.optJSONObject("stock")?.let { o ->
                o.keys().forEach { k -> if (ItemCatalog.get(k) != null) stock[k] = o.optInt(k, 0) }
            }
            root.optJSONArray("relics")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val id = arr.optString(i)
                    if (ItemCatalog.get(id) != null && equippedRelics.size < ItemCatalog.RELIC_SLOTS)
                        equippedRelics.add(id)
                }
            }
            root.optJSONArray("loadout")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val id = arr.optString(i)
                    if (ItemCatalog.get(id) != null) loadout.add(id)
                }
            }
            totalRuns = root.optInt("runs", 0)
            totalWins = root.optInt("wins", 0)
            totalDeaths = root.optInt("deaths", 0)
            totalEcosGanados = root.optInt("earned", 0)
            bestTimeMs = root.optLong("best", 0L)
            totalPlayMs = root.optLong("playMs", 0L)
            totalSteps = root.optLong("steps", 0L)
            levelsSinceVetagris = root.optInt("sinceVtg", 0)

            root.optJSONObject("settings")?.let { s ->
                with(settings) {
                    musicVolume = s.optDouble("music", 0.7).toFloat()
                    sfxVolume = s.optDouble("sfx", 0.85).toFloat()
                    masterVolume = s.optDouble("master", 1.0).toFloat()
                    haptics = s.optBoolean("haptics", true)
                    invertY = s.optBoolean("invertY", false)
                    lookSensitivity = s.optDouble("look", 1.0).toFloat()
                    gamepadSensitivity = s.optDouble("pad", 1.0).toFloat()
                    stickDeadzone = s.optDouble("dead", 0.16).toFloat()
                    leftHanded = s.optBoolean("lefty", false)
                    showMinimap = s.optBoolean("map", true)
                    minimapSize = s.optDouble("mapSize", 1.0).toFloat()
                    showArms = s.optBoolean("arms", true)
                    headBob = s.optDouble("bob", 1.0).toFloat()
                    fovExtra = s.optDouble("fov", 0.0).toFloat()
                    quality = s.optInt("q", 2)
                    renderScale = s.optDouble("rs", 1.0).toFloat()
                    targetFps = s.optInt("fps", 60)
                    showFps = s.optBoolean("showFps", false)
                    brightness = s.optDouble("bright", 1.0).toFloat()
                    fogIntensity = s.optDouble("fog", 1.0).toFloat()
                    torchFlicker = s.optBoolean("flicker", true)
                    autoRun = s.optBoolean("autorun", false)
                    joystickSize = s.optDouble("jsize", 1.0).toFloat()
                    joystickOpacity = s.optDouble("jop", 0.55).toFloat()
                    buttonScale = s.optDouble("btn", 1.0).toFloat()
                    vibrateOnPickup = s.optBoolean("vibPick", true)
                    subtitles = s.optBoolean("subs", true)
                    tutorialDone = s.optBoolean("tuto", false)
                    compassAlwaysOn = s.optBoolean("compass", false)
                    colorBlindMode = s.optInt("cb", 0)
                    uiScale = s.optDouble("ui", 1.0).toFloat()
                }
            }
        } catch (t: Throwable) {
            // Partida corrupta: se arranca de cero en vez de crashear.
            resetProgress()
        }
        ItemCatalog.defaultsOwned.forEach { if (ownedLevel(it) <= 0) owned[it] = 1 }
    }

    companion object {
        private const val KEY = "perfil"
        private const val SCHEMA = 1
        const val MAX_STACK = 99

        @Volatile private var instance: SaveData? = null

        fun get(context: Context): SaveData =
            instance ?: synchronized(this) {
                instance ?: SaveData(
                    PrefsStore(
                        context.applicationContext
                            .getSharedPreferences("mggx_laberinto", Context.MODE_PRIVATE)
                    )
                ).also { it.load(); instance = it }
            }

        /** Perfil aislado sobre un almacen propio. Lo usan los tests. */
        fun fromStore(store: Store): SaveData = SaveData(store).also { it.load() }

        /** Almacen en memoria, sin nada de Android. */
        fun memoryStore(): Store = object : Store {
            private var data: String? = null
            override fun read(): String? = data
            override fun write(json: String) { data = json }
        }
    }
}
