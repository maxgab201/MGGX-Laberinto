package com.mggx.laberinto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.ui.CaveBackdrop
import com.mggx.laberinto.ui.CaveButton
import com.mggx.laberinto.ui.CaveButtonStyle
import com.mggx.laberinto.ui.CaveSlider
import com.mggx.laberinto.ui.CaveToggle
import com.mggx.laberinto.ui.SectionTitle
import com.mggx.laberinto.ui.SettingRow
import com.mggx.laberinto.ui.StonePanel
import com.mggx.laberinto.ui.icons.CaveIcon
import com.mggx.laberinto.ui.icons.IconId
import com.mggx.laberinto.ui.theme.Cave
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    save: SaveData,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onResetProgress: () -> Unit
) {
    var section by remember { mutableIntStateOf(0) }
    var confirmReset by remember { mutableStateOf(false) }
    val s = save.settings

    // Cambiar un ajuste no necesita avisarle nada a esta pantalla: los campos
    // de Settings son estado de Compose, asi que el control que muestra cada
    // valor se entera solo. Aca solo queda guardar en disco y avisarle al
    // resto de la app (audio, renderer) que algo cambio.
    fun apply(block: () -> Unit) {
        block(); save.save(); onChanged()
    }

    val sections = listOf(
        Triple(IconId.MANDO, "Controles", "Como se maneja"),
        Triple(IconId.PANTALLA, "Imagen", "Como se ve"),
        Triple(IconId.ALTAVOZ, "Sonido", "Como se escucha"),
        Triple(IconId.DEDO, "En pantalla", "Botones y joystick"),
        Triple(IconId.INFO, "Partida", "Reglas y ayudas"),
        Triple(IconId.RESET, "Datos", "Progreso y reinicio")
    )

    Box(Modifier.fillMaxSize()) {
        CaveBackdrop(seed = 47)
        Column(
            Modifier
                .fillMaxSize()
                // El notch y la barra de gestos se comen los bordes reales:
                // sin esto, el menu lateral y el boton de volver quedan
                // pegados al borde fisico o directamente tapados.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CaveButton("Volver", onBack, icon = IconId.ATRAS, style = CaveButtonStyle.FANTASMA)
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Toca cada cosa y proba: se guarda solo.",
                        style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxSize()) {
                // ------------------------------------------------ menu lateral
                //
                // Con scroll propio: son seis secciones y en un telefono
                // apaisado las ultimas ("Partida" y "Datos") quedaban abajo
                // del borde de la pantalla, sin forma de llegar a ellas.
                Column(
                    Modifier
                        .width(196.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sections.forEachIndexed { i, (icon, title, sub) ->
                        val on = i == section
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) Cave.Amber.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { section = i }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CaveIcon(
                                icon, size = 21.dp,
                                tint = if (on) Cave.AmberSoft else Cave.TextDim,
                                accent = if (on) Cave.Amber else Cave.TextFaint
                            )
                            Spacer(Modifier.width(11.dp))
                            Column {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (on) Cave.AmberSoft else Cave.Text
                                )
                                Text(sub, fontSize = 10.sp, color = Cave.TextFaint)
                            }
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                // --------------------------------------------------- contenido
                StonePanel(Modifier.weight(1f).fillMaxHeight()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp)
                    ) {
                        when (section) {
                            0 -> ControlsSection(s, ::apply)
                            1 -> ImageSection(s, ::apply)
                            2 -> SoundSection(s, ::apply)
                            3 -> OnScreenSection(s, ::apply)
                            4 -> GameSection(s, ::apply)
                            else -> DataSection(
                                save = save,
                                confirmReset = confirmReset,
                                onAskReset = { confirmReset = true },
                                onCancelReset = { confirmReset = false },
                                onDoReset = {
                                    confirmReset = false
                                    onResetProgress()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------- secciones

@Composable
private fun ControlsSection(s: SaveData.Settings, apply: (() -> Unit) -> Unit) {
    SectionTitle("Camara", Modifier.padding(horizontal = 16.dp))
    SettingRow(
        IconId.OJO, "Sensibilidad al mirar",
        "Que tan rapido gira la camara cuando arrastras el dedo. Ahora: ${pctText(s.lookSensitivity, 0.2f, 3f)}"
    ) {
        CaveSlider(s.lookSensitivity, { v -> apply { s.lookSensitivity = v } },
            Modifier.width(210.dp), 0.2f..3f)
    }
    SettingRow(
        IconId.FLECHA_ABAJO, "Invertir el eje vertical",
        "Si lo activas, arrastrar para abajo mira para arriba."
    ) { CaveToggle(s.invertY, onCheckedChange = { v -> apply { s.invertY = v } }) }

    Spacer(Modifier.height(6.dp))
    SectionTitle("Mando", Modifier.padding(horizontal = 16.dp))
    SettingRow(
        IconId.MANDO, "Sensibilidad del mando",
        "Solo afecta al joystick derecho del control. Ahora: ${pctText(s.gamepadSensitivity, 0.2f, 3f)}"
    ) {
        CaveSlider(s.gamepadSensitivity, { v -> apply { s.gamepadSensitivity = v } },
            Modifier.width(210.dp), 0.2f..3f)
    }
    SettingRow(
        IconId.INFO, "Zona muerta de los sticks",
        "Cuanto hay que mover el stick para que empiece a contar. Subila si el personaje se mueve solo."
    ) {
        CaveSlider(s.stickDeadzone, { v -> apply { s.stickDeadzone = v } },
            Modifier.width(210.dp), 0.02f..0.45f)
    }
    Spacer(Modifier.height(6.dp))
    GamepadMapHelp()
}

@Composable
private fun ImageSection(s: SaveData.Settings, apply: (() -> Unit) -> Unit) {
    SectionTitle("Calidad", Modifier.padding(horizontal = 16.dp))
    SettingRow(
        IconId.DIFICULTAD, "Nivel de detalle",
        "Bajalo si el celular se calienta o va lento. Subilo si te sobra potencia."
    ) {
        StepSelector(
            listOf("Bajo", "Medio", "Alto", "Ultra"), s.quality
        ) { v -> apply { s.quality = v } }
    }
    SettingRow(
        IconId.PANTALLA, "Resolucion interna",
        "Dibuja el 3D a menos pixeles y lo estira. Es lo que mas cuadros gana en telefonos justos."
    ) {
        StepSelector(listOf("60%", "75%", "90%", "100%"), resIndex(s.renderScale)) { v ->
            apply { s.renderScale = floatArrayOf(0.6f, 0.75f, 0.9f, 1.0f)[v] }
        }
    }
    SettingRow(
        IconId.CRONOMETRO, "Suavidad objetivo",
        "Cuadros por segundo a los que apunta el juego."
    ) {
        StepSelector(listOf("30", "45", "60", "90"), fpsIndex(s.targetFps)) { v ->
            apply { s.targetFps = intArrayOf(30, 45, 60, 90)[v] }
        }
    }
    Spacer(Modifier.height(6.dp))
    SectionTitle("Ambiente", Modifier.padding(horizontal = 16.dp))
    SettingRow(
        IconId.FAROL, "Brillo",
        "Si no llegas a ver nada, subilo. Ahora: ${pctText(s.brightness, 0.5f, 2f)}"
    ) { CaveSlider(s.brightness, { v -> apply { s.brightness = v } }, Modifier.width(210.dp), 0.5f..2f) }
    SettingRow(
        IconId.SOMBRA, "Densidad de la niebla",
        "Cuanto se cierra la vista a lo lejos. Menos niebla = ves mas."
    ) { CaveSlider(s.fogIntensity, { v -> apply { s.fogIntensity = v } }, Modifier.width(210.dp), 0.3f..1.6f) }
    SettingRow(
        IconId.ANTORCHA, "Titileo de la antorcha",
        "La luz late como una llama de verdad. Apagalo si te molesta."
    ) { CaveToggle(s.torchFlicker, onCheckedChange = { v -> apply { s.torchFlicker = v } }) }
    SettingRow(
        IconId.CATALEJO, "Campo de vision extra",
        "Ves mas de costado, pero todo se ve un poco mas chico."
    ) { CaveSlider(s.fovExtra, { v -> apply { s.fovExtra = v } }, Modifier.width(210.dp), -8f..22f) }
    SettingRow(
        IconId.MANO, "Mostrar los brazos",
        "Tus manos en primera persona. Apagalas si preferis la vista limpia."
    ) { CaveToggle(s.showArms, onCheckedChange = { v -> apply { s.showArms = v } }) }
    SettingRow(
        IconId.CORRER, "Movimiento de la camara al caminar",
        "El balanceo de los pasos. Bajalo a cero si te marea."
    ) { CaveSlider(s.headBob, { v -> apply { s.headBob = v } }, Modifier.width(210.dp), 0f..1.6f) }
    SettingRow(
        IconId.ESTADISTICA, "Mostrar los cuadros por segundo",
        "Un numerito arriba a la derecha para ver como rinde."
    ) { CaveToggle(s.showFps, onCheckedChange = { v -> apply { s.showFps = v } }) }
}

@Composable
private fun SoundSection(s: SaveData.Settings, apply: (() -> Unit) -> Unit) {
    SectionTitle("Volumen", Modifier.padding(horizontal = 16.dp))
    SettingRow(IconId.ALTAVOZ, "Volumen general", "Baja todo de una.") {
        CaveSlider(s.masterVolume, { v -> apply { s.masterVolume = v } }, Modifier.width(210.dp))
    }
    SettingRow(IconId.NOTA_MUSICAL, "Musica", "El ambiente de la cueva. Todo generado en vivo.") {
        CaveSlider(s.musicVolume, { v -> apply { s.musicVolume = v } }, Modifier.width(210.dp))
    }
    SettingRow(IconId.ONDA, "Efectos", "Pasos, ecos, trampas y monedas.") {
        CaveSlider(s.sfxVolume, { v -> apply { s.sfxVolume = v } }, Modifier.width(210.dp))
    }
    Spacer(Modifier.height(6.dp))
    SectionTitle("Vibracion", Modifier.padding(horizontal = 16.dp))
    SettingRow(IconId.VIBRACION, "Vibracion", "El celular vibra en golpes y hallazgos importantes.") {
        CaveToggle(s.haptics, onCheckedChange = { v -> apply { s.haptics = v } })
    }
    SettingRow(IconId.MONEDA_ECO, "Vibrar al juntar ecos", "Un toque cortito cada vez que levantas algo.") {
        CaveToggle(s.vibrateOnPickup, onCheckedChange = { v -> apply { s.vibrateOnPickup = v } })
    }
}

@Composable
private fun OnScreenSection(s: SaveData.Settings, apply: (() -> Unit) -> Unit) {
    SectionTitle("Controles tactiles", Modifier.padding(horizontal = 16.dp))
    SettingRow(IconId.IDIOMA, "Zurdo", "Pone el joystick a la derecha y la camara a la izquierda.") {
        CaveToggle(s.leftHanded, onCheckedChange = { v -> apply { s.leftHanded = v } })
    }
    SettingRow(IconId.DEDO, "Tamano del joystick", "Mas grande es mas facil de encontrar sin mirar.") {
        CaveSlider(s.joystickSize, { v -> apply { s.joystickSize = v } }, Modifier.width(210.dp), 0.7f..1.6f)
    }
    SettingRow(IconId.PANTALLA, "Transparencia del joystick", "Que tanto se ve el joystick sobre el juego.") {
        CaveSlider(s.joystickOpacity, { v -> apply { s.joystickOpacity = v } }, Modifier.width(210.dp), 0.15f..1f)
    }
    SettingRow(IconId.MAS, "Tamano de los botones", "Los botones de accion de la derecha.") {
        CaveSlider(s.buttonScale, { v -> apply { s.buttonScale = v } }, Modifier.width(210.dp), 0.75f..1.5f)
    }
    Spacer(Modifier.height(6.dp))
    SectionTitle("Informacion en pantalla", Modifier.padding(horizontal = 16.dp))
    SettingRow(IconId.MAPA, "Mostrar el minimapa", "El plano de lo que ya recorriste.") {
        CaveToggle(s.showMinimap, onCheckedChange = { v -> apply { s.showMinimap = v } })
    }
    SettingRow(IconId.CUADRICULA, "Tamano del minimapa", "Mas grande se lee mejor, pero tapa mas.") {
        CaveSlider(s.minimapSize, { v -> apply { s.minimapSize = v } }, Modifier.width(210.dp), 0.7f..1.5f)
    }
    SettingRow(IconId.MAS, "Tamano de los textos", "Para que se lea comodo desde lejos.") {
        CaveSlider(s.uiScale, { v -> apply { s.uiScale = v } }, Modifier.width(210.dp), 0.85f..1.35f)
    }
    SettingRow(IconId.INFO, "Avisos escritos", "Los carteles que aparecen al usar objetos o pisar trampas.") {
        CaveToggle(s.subtitles, onCheckedChange = { v -> apply { s.subtitles = v } })
    }
}

@Composable
private fun GameSection(s: SaveData.Settings, apply: (() -> Unit) -> Unit) {
    SectionTitle("Comodidad", Modifier.padding(horizontal = 16.dp))
    SettingRow(IconId.CORRER, "Correr siempre", "Vas siempre a la maxima velocidad sin apretar nada.") {
        CaveToggle(s.autoRun, onCheckedChange = { v -> apply { s.autoRun = v } })
    }
    SettingRow(IconId.BRUJULA, "Brujula siempre encendida", "Una flecha te marca la salida todo el tiempo. Mas facil.") {
        CaveToggle(s.compassAlwaysOn, onCheckedChange = { v -> apply { s.compassAlwaysOn = v } })
    }
    Spacer(Modifier.height(6.dp))
    SectionTitle("Accesibilidad", Modifier.padding(horizontal = 16.dp))
    SettingRow(IconId.OJO, "Modo para daltonismo", "Ajusta los colores del mapa y los avisos.") {
        StepSelector(listOf("Ninguno", "Rojo", "Verde", "Azul"), s.colorBlindMode) { v ->
            apply { s.colorBlindMode = v }
        }
    }
    Spacer(Modifier.height(10.dp))
    Box(Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Consejo: si te perdes seguido, prende la brujula y comprate el Hilo de Ariadna. " +
                "Marca por donde ya pasaste y no volves a dar vueltas.",
            style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
        )
    }
}

@Composable
private fun DataSection(
    save: SaveData,
    confirmReset: Boolean,
    onAskReset: () -> Unit,
    onCancelReset: () -> Unit,
    onDoReset: () -> Unit
) {
    SectionTitle("Tu progreso", Modifier.padding(horizontal = 16.dp))
    Spacer(Modifier.height(10.dp))
    Column(Modifier.padding(horizontal = 16.dp)) {
        DataLine("Nivel mas hondo alcanzado", "${save.maxLevel}")
        DataLine("Niveles completados", "${save.totalWins}")
        DataLine("Ecos ganados en total", com.mggx.laberinto.ui.formatNumber(save.totalEcosGanados))
        DataLine("Partidas jugadas", "${save.totalRuns}")
        Spacer(Modifier.height(18.dp))
        SectionTitle("Empezar de cero")
        Spacer(Modifier.height(8.dp))
        Text(
            "Borra todo: monedas, objetos, mejoras y niveles. No se puede deshacer.",
            style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
        )
        Spacer(Modifier.height(12.dp))
        if (!confirmReset) {
            CaveButton("Borrar todo mi progreso", onAskReset,
                icon = IconId.RESET, style = CaveButtonStyle.PELIGRO)
        } else {
            Text("Seguro? Esto no tiene vuelta atras.",
                style = MaterialTheme.typography.titleMedium, color = Cave.Bad)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CaveButton("Si, borrar", onDoReset, icon = IconId.TILDE, style = CaveButtonStyle.PELIGRO)
                CaveButton("Mejor no", onCancelReset, icon = IconId.CERRAR)
            }
        }
    }
}

// ------------------------------------------------------------- auxiliares

@Composable
private fun DataLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Cave.TextDim,
            modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StepSelector(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(Cave.Void.copy(alpha = 0.7f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) Cave.Amber.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(
                    o, fontSize = 12.sp,
                    color = if (on) Cave.AmberSoft else Cave.TextDim,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GamepadMapHelp() {
    Column(Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Que hace cada boton del mando")
        Spacer(Modifier.height(10.dp))
        val rows = listOf(
            "Stick izquierdo" to "Caminar",
            "Stick derecho" to "Mirar alrededor",
            "Gatillo derecho / L3" to "Correr",
            "A" to "Saltar",
            "R3 (pulsar stick derecho)" to "Agacharte: normal, bien bajo y de nuevo de pie",
            "X" to "Usar el objeto elegido",
            "L2" to "Dejar una marca de tiza",
            "Y" to "Cambiar de objeto",
            "B" to "Volver atras",
            "R1" to "Golpear con lo que tengas en la mano",
            "L1" to "Objeto anterior",
            "Cruceta arriba" to "Prender y apagar la linterna",
            "Start" to "Pausa",
            "Cruceta" to "Moverte por los menus"
        )
        rows.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Box(
                    Modifier
                        .width(150.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Cave.Void.copy(alpha = 0.5f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) { Text(k, fontSize = 11.sp, color = Cave.AmberSoft) }
                Spacer(Modifier.width(12.dp))
                Text(v, style = MaterialTheme.typography.bodyMedium, color = Cave.TextDim)
            }
        }
    }
}

private fun pctText(v: Float, min: Float, max: Float): String {
    val f = ((v - min) / (max - min)).coerceIn(0f, 1f)
    return "${(f * 100).roundToInt()}%"
}

private fun fpsIndex(fps: Int): Int = when (fps) {
    30 -> 0; 45 -> 1; 90 -> 3; else -> 2
}

private fun resIndex(v: Float): Int = when {
    v < 0.68f -> 0
    v < 0.83f -> 1
    v < 0.95f -> 2
    else -> 3
}
