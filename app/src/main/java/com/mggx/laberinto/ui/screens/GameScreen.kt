package com.mggx.laberinto.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.EffectType
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.gl.CaveRenderer
import com.mggx.laberinto.ui.CaveBar
import com.mggx.laberinto.ui.CaveButton
import com.mggx.laberinto.ui.CaveButtonStyle
import com.mggx.laberinto.ui.RoundActionButton
import com.mggx.laberinto.ui.StonePanel
import com.mggx.laberinto.ui.formatNumber
import com.mggx.laberinto.ui.formatTime
import com.mggx.laberinto.game.Postura
import com.mggx.laberinto.ui.icons.CaveIcon
import com.mggx.laberinto.ui.icons.IconId
import com.mggx.laberinto.ui.theme.Cave
import com.mggx.laberinto.ui.theme.Semantics
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun GameHud(
    save: SaveData,
    session: GameSession,
    input: CaveRenderer.InputState,
    fps: Float,
    onPause: () -> Unit,
    onQuit: () -> Unit,
    paused: Boolean,
    onResume: () -> Unit
) {
    val s = save.settings
    // Refresco del HUD a ~30 Hz: suficiente para las barras y baratisimo.
    var uiTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                if (t - last > 33_000_000L) { last = t; uiTick++ }
            }
        }
    }
    @Suppress("UNUSED_EXPRESSION") uiTick

    val sem = remember(s.colorBlindMode) { Semantics.of(s.colorBlindMode) }
    var selectedSlot by remember { mutableIntStateOf(0) }
    // La barra de la partida muestra TODO lo que se puede usar, no solo las
    // ranuras elegidas: si no, algo comprado con las ranuras llenas quedaba
    // pago y sin forma de usarse.
    val loadout = save.bolsaDeMano()
    if (selectedSlot >= max(1, loadout.size)) selectedSlot = 0

    Box(Modifier.fillMaxSize()) {

        // ------------------------------------------------- zonas tactiles
        if (!paused) {
            Row(Modifier.fillMaxSize()) {
                val joystickFirst = !s.leftHanded
                if (joystickFirst) {
                    JoystickArea(Modifier.weight(1f).fillMaxHeight(), s, input)
                    LookArea(Modifier.weight(1.15f).fillMaxHeight(), s, input)
                } else {
                    LookArea(Modifier.weight(1.15f).fillMaxHeight(), s, input)
                    JoystickArea(Modifier.weight(1f).fillMaxHeight(), s, input)
                }
            }
        }

        // ------------------------------------------------------ HUD arriba
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            StonePanel(Modifier.width(178.dp), corner = 12.dp) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CaveIcon(IconId.VIDA, size = 14.dp, tint = sem.health, accent = sem.health)
                        Spacer(Modifier.width(6.dp))
                        CaveBar(session.healthFraction(), sem.health, Modifier.weight(1f))
                        Spacer(Modifier.width(6.dp))
                        Text("${session.health.roundToInt()}", fontSize = 10.sp, color = Cave.TextDim)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CaveIcon(IconId.AGUANTE, size = 14.dp, tint = Cave.Stamina, accent = Cave.Stamina)
                        Spacer(Modifier.width(6.dp))
                        CaveBar(session.staminaFraction(), Cave.Stamina, Modifier.weight(1f), height = 6.dp)
                    }
                    if (session.stats.tieneLinterna) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val enc = session.linternaEncendida
                            CaveIcon(
                                IconId.LINTERNA, size = 14.dp,
                                tint = if (enc) Cave.Amber else Cave.TextFaint,
                                accent = Cave.AmberSoft
                            )
                            Spacer(Modifier.width(6.dp))
                            CaveBar(
                                session.carburo,
                                if (session.carburo > 0.2f) Cave.AmberSoft else sem.bad,
                                Modifier.weight(1f), height = 6.dp
                            )
                        }
                    }
                    if (session.livesLeft > 0) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CaveIcon(IconId.CUERDA, size = 13.dp, tint = sem.good, accent = sem.good)
                            Spacer(Modifier.width(6.dp))
                            Text("${session.livesLeft} vida(s) extra", fontSize = 10.sp, color = sem.good)
                        }
                    }
                    val vivos = session.bichosVivos()
                    if (vivos > 0 || session.bichosVolteados > 0) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CaveIcon(
                                ItemCatalog.get(save.armaEquipada)?.icon ?: IconId.PUNO,
                                size = 13.dp, tint = Cave.TextDim, accent = Cave.Bad
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "$vivos en la cueva · ${session.bichosVolteados} volteados",
                                fontSize = 10.sp, color = Cave.TextDim
                            )
                        }
                    }
                    val acechan = session.enemigosAlerta()
                    if (acechan > 0) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CaveIcon(IconId.CALAVERA, size = 13.dp, tint = sem.bad, accent = sem.bad)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (acechan == 1) "Algo te sigue" else "$acechan te siguen",
                                fontSize = 10.sp, color = sem.bad
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            StonePanel(corner = 12.dp) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("N.${session.level}", style = MaterialTheme.typography.titleMedium, color = Cave.AmberSoft)
                    Spacer(Modifier.width(12.dp))
                    CaveIcon(IconId.CRONOMETRO, size = 15.dp, tint = Cave.TextDim, accent = Cave.AmberDeep)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        formatTime(session.elapsedMs),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (session.effects.isActive(EffectType.CONGELAR_RELOJ)) Cave.Ice else Cave.Text
                    )
                    if (session.isCompassOn()) {
                        Spacer(Modifier.width(12.dp))
                        CaveIcon(IconId.BRUJULA, size = 15.dp, tint = Cave.Ice, accent = Cave.Ice)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${session.distanceToExit().roundToInt()} m",
                            style = MaterialTheme.typography.titleMedium, color = Cave.Ice
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    CaveIcon(IconId.MONEDA_ECO, size = 16.dp, tint = Cave.AmberSoft, accent = Cave.AmberSoft)
                    Spacer(Modifier.width(5.dp))
                    Text(formatNumber(session.ecosCollected), style = MaterialTheme.typography.titleMedium)
                    if (session.vetagrisCollected > 0) {
                        Spacer(Modifier.width(10.dp))
                        CaveIcon(IconId.MONEDA_VETAGRIS, size = 16.dp, tint = Cave.Vetagris, accent = Cave.Vetagris)
                        Spacer(Modifier.width(4.dp))
                        Text("${session.vetagrisCollected}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (s.showFps) {
                Text(
                    "${fps.roundToInt()} fps",
                    fontSize = 11.sp, color = Cave.TextFaint,
                    modifier = Modifier.padding(top = 8.dp, end = 10.dp)
                )
            }
            RoundActionButton(
                IconId.PAUSA, onPause, diameter = 42.dp, opacity = 0.55f
            )
        }

        // ------------------------------------------------------- minimapa
        if (s.showMinimap) {
            Minimap(
                session, sem,
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 64.dp, end = 14.dp)
                    .size((116 * s.minimapSize).dp)
            )
        }

        // --------------------------------------------------- buffs activos
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            session.effects.visible.take(6).forEach { a ->
                val item = ItemCatalog.get(a.sourceId)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(Cave.Void.copy(alpha = 0.62f))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CaveIcon(
                        item?.icon ?: IconId.INFO, size = 17.dp,
                        tint = Cave.AmberSoft, accent = Cave.Amber
                    )
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("${a.remaining.roundToInt()}s", fontSize = 10.sp, color = Cave.Text)
                        CaveBar(
                            (a.remaining / a.total).coerceIn(0f, 1f), Cave.Amber,
                            Modifier.width(44.dp), height = 3.dp
                        )
                    }
                }
            }
        }

        // ------------------------------------------------------- avisos
        if (s.subtitles) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 128.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                session.currentToasts().forEach { msg ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Cave.Void.copy(alpha = 0.74f))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(msg, style = MaterialTheme.typography.titleMedium, color = Cave.AmberSoft)
                    }
                }
            }
        }

        // ------------------------------------------------- botones de accion
        if (!paused) {
            ActionButtons(
                save = save,
                session = session,
                input = input,
                selectedSlot = selectedSlot,
                onSelectSlot = { selectedSlot = it },
                modifier = Modifier
                    .align(if (s.leftHanded) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(20.dp)
            )
        }

        // ---------------------------------------------------------- pausa
        if (paused) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Cave.Void.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                StonePanel(Modifier.width(360.dp), glow = Cave.Amber) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("En pausa", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Nivel ${session.level} - ${session.theme.displayName}",
                            style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                        )
                        Spacer(Modifier.height(18.dp))
                        CaveButton(
                            "Seguir", onResume, icon = IconId.JUGAR,
                            style = CaveButtonStyle.PRIMARIO, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(9.dp))
                        CaveButton(
                            "Abandonar el nivel", onQuit, icon = IconId.SALIDA,
                            style = CaveButtonStyle.PELIGRO, modifier = Modifier.fillMaxWidth(),
                            subtitle = "Te llevas la mitad de los ecos juntados"
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------- controles

@Composable
private fun JoystickArea(
    modifier: Modifier,
    s: SaveData.Settings,
    input: CaveRenderer.InputState
) {
    var center by remember { mutableStateOf<Offset?>(null) }
    var knob by remember { mutableStateOf(Offset.Zero) }
    val radiusDp = 74f * s.joystickSize

    Box(
        modifier.pointerInput(s.joystickSize) {
            val radiusPx = radiusDp * density
            awaitPointerEventScope {
                while (true) {
                    val down = awaitPointerEvent().changes.firstOrNull { it.pressed && it.previousPressed.not() }
                        ?: continue
                    center = down.position
                    knob = down.position
                    down.consume()
                    var active = true
                    while (active) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id }
                        if (ch == null || !ch.pressed) { active = false } else {
                            knob = ch.position
                            ch.consume()
                            val c = center!!
                            var dx = (knob.x - c.x) / radiusPx
                            var dy = (knob.y - c.y) / radiusPx
                            val m = hypot(dx, dy)
                            if (m > 1f) { dx /= m; dy /= m }
                            // Zona muerta chica para que no se mueva solo
                            val dead = 0.11f
                            val mag = hypot(dx, dy)
                            if (mag < dead) { dx = 0f; dy = 0f }
                            input.moveX = dx
                            input.moveY = -dy
                            input.runStick = mag > 0.82f
                        }
                    }
                    center = null
                    input.moveX = 0f; input.moveY = 0f; input.runStick = false
                }
            }
        }
    ) {
        val c = center
        Canvas(Modifier.fillMaxSize()) {
            val alpha = s.joystickOpacity
            val base = c ?: Offset(size.width * 0.42f, size.height * 0.68f)
            val r = radiusDp * density
            // Aro exterior
            drawCircle(Cave.Void.copy(alpha = alpha * 0.42f), r, base)
            drawCircle(Cave.StoneEdge.copy(alpha = alpha * 0.9f), r, base, style = Stroke(2f * density))
            drawCircle(Cave.Amber.copy(alpha = alpha * 0.20f), r * 0.62f, base, style = Stroke(1.2f * density))
            // Cruz de referencia
            drawLine(Cave.StoneEdge.copy(alpha = alpha * 0.5f),
                Offset(base.x - r * 0.24f, base.y), Offset(base.x + r * 0.24f, base.y), 1.4f * density)
            drawLine(Cave.StoneEdge.copy(alpha = alpha * 0.5f),
                Offset(base.x, base.y - r * 0.24f), Offset(base.x, base.y + r * 0.24f), 1.4f * density)
            // Perilla
            if (c != null) {
                var kx = knob.x - c.x
                var ky = knob.y - c.y
                val m = hypot(kx, ky)
                if (m > r) { kx = kx / m * r; ky = ky / m * r }
                val kp = Offset(c.x + kx, c.y + ky)
                drawCircle(Cave.Amber.copy(alpha = alpha * 0.28f), r * 0.42f, kp)
                drawCircle(Cave.AmberSoft.copy(alpha = alpha), r * 0.30f, kp)
                drawCircle(Cave.Void.copy(alpha = alpha * 0.9f), r * 0.30f, kp, style = Stroke(2f * density))
            }
        }
    }
}

@Composable
private fun LookArea(
    modifier: Modifier,
    s: SaveData.Settings,
    input: CaveRenderer.InputState
) {
    Box(
        modifier.pointerInput(s.lookSensitivity, s.invertY) {
            val scale = 0.16f * s.lookSensitivity / density
            awaitPointerEventScope {
                while (true) {
                    val down = awaitPointerEvent().changes.firstOrNull { it.pressed && !it.previousPressed }
                        ?: continue
                    var last = down.position
                    down.consume()
                    var active = true
                    while (active) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id }
                        if (ch == null || !ch.pressed) { active = false } else {
                            val d = ch.position - last
                            last = ch.position
                            ch.consume()
                            input.lookDX += d.x * scale * density
                            input.lookDY += (if (s.invertY) d.y else -d.y) * scale * density
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ActionButtons(
    save: SaveData,
    session: GameSession,
    input: CaveRenderer.InputState,
    selectedSlot: Int,
    onSelectSlot: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = save.settings
    val scale = s.buttonScale
    val loadout = save.bolsaDeMano()
    val currentId = loadout.getOrNull(selectedSlot)
    val currentItem = currentId?.let { ItemCatalog.get(it) }
    val favoritas = save.loadoutSlots()

    Column(modifier, horizontalAlignment = Alignment.End) {
        // Bolso: las ranuras elegidas primero y despues el resto. Con muchos
        // objetos la fila se corre para el costado en vez de desbordarse.
        Row(
            Modifier
                .widthIn(max = 320.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            loadout.forEachIndexed { i, id ->
                val item = ItemCatalog.get(id) ?: return@forEachIndexed
                val on = i == selectedSlot
                Box(
                    Modifier
                        .size((46 * scale).dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (on) Cave.Amber.copy(alpha = 0.24f) else Cave.Void.copy(alpha = 0.55f)
                        )
                        .pointerInput(id) { detectTapSimple { onSelectSlot(i) } },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawRoundRect(
                            color = if (on) Cave.Amber else Cave.StoneEdge,
                            cornerRadius = CornerRadius(11.dp.toPx(), 11.dp.toPx()),
                            style = Stroke(1.4f * density)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CaveIcon(
                            item.icon, size = (24 * scale).dp, tint = Cave.Text,
                            // Las que elegiste en Equipo van con el acento
                            // fuerte; el resto del bolso, mas apagado.
                            accent = if (i < favoritas) Cave.Amber else Cave.TextDim
                        )
                        Text("${save.stockOf(id)}", fontSize = (9 * scale).sp, color = Cave.AmberSoft)
                    }
                }
            }
        }

        Spacer(Modifier.height(11.dp))

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            // Acciones contextuales
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (session.chalkCharges > 0) {
                    RoundActionButton(
                        IconId.TIZA, { session.dropChalk() },
                        diameter = (50 * scale).dp, badge = "${session.chalkCharges}"
                    )
                }
                if (session.pickCharges > 0) {
                    RoundActionButton(
                        IconId.PICO, { session.breakWall() },
                        diameter = (50 * scale).dp,
                        enabled = session.canBreakWall(), badge = "${session.pickCharges}"
                    )
                }
                if (session.phaseCharges > 0) {
                    RoundActionButton(
                        IconId.FANTASMA, { session.phaseThrough() },
                        diameter = (50 * scale).dp,
                        enabled = session.canPhase(), badge = "${session.phaseCharges}"
                    )
                }
                if (session.stats.freeSonarSeconds > 0f) {
                    RoundActionButton(
                        IconId.DIAPASON, { session.useFreeSonar() },
                        diameter = (50 * scale).dp,
                        enabled = session.freeSonarTimer <= 0f,
                        badge = if (session.freeSonarTimer > 0f) "${session.freeSonarTimer.roundToInt()}" else null
                    )
                }
            }

            // Linterna: solo aparece si compraste el poder.
            if (session.stats.tieneLinterna) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    RoundActionButton(
                        IconId.LINTERNA,
                        onClick = { session.toggleLinterna() },
                        diameter = (50 * scale).dp,
                        tint = if (session.linternaEncendida) Cave.Amber else Cave.Text,
                        enabled = session.carburo > 0f || session.linternaEncendida,
                        badge = "${(session.carburo * 100).roundToInt()}%"
                    )
                }
            }

            // Saltar y agacharse. El agacharse cicla de pie -> agachado ->
            // arrastrandose -> de pie, asi es un solo boton y no dos.
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                RoundActionButton(
                    IconId.SALTAR,
                    onClick = { input.jumpPending = true },
                    diameter = (54 * scale).dp,
                    enabled = session.enSuelo && session.postura.puedeSaltar
                )
                RoundActionButton(
                    when (input.crouchLevel) {
                        0 -> IconId.AGACHARSE
                        1 -> IconId.ARRASTRARSE
                        else -> IconId.DE_PIE
                    },
                    onClick = { input.crouchLevel = (input.crouchLevel + 1) % 3 },
                    diameter = (54 * scale).dp,
                    tint = if (input.crouchLevel == 0) Cave.Text else Cave.Amber,
                    badge = when (session.postura) {
                        Postura.DE_PIE -> null
                        Postura.AGACHADO -> "bajo"
                        Postura.ARRASTRANDOSE -> "raso"
                    }
                )
            }

            // Correr: no es un boton de toque sino de mantener apretado, asi que
            // el trabajo lo hace el pointerInput de abajo y no el onClick.
            RoundActionButton(
                IconId.CORRER,
                onClick = { },
                diameter = (58 * scale).dp,
                modifier = Modifier.pointerInput(Unit) {
                    // Boton sostenido: corre mientras el dedo este apoyado.
                    awaitPointerEventScope {
                        while (true) {
                            val ev = awaitPointerEvent()
                            input.runButton = ev.changes.any { it.pressed }
                            ev.changes.forEach { it.consume() }
                        }
                    }
                },
                tint = if (session.staminaFraction() > 0.05f) Cave.Text else Cave.TextFaint
            )

            // Golpear. Siempre esta: aunque no tengas arma se pega a mano
            // limpia, para que nunca quedes sin forma de defenderte.
            RoundActionButton(
                ItemCatalog.get(save.armaEquipada)?.icon ?: IconId.PUNO,
                onClick = { session.golpear() },
                diameter = (62 * scale).dp,
                enabled = session.puedeGolpear(),
                tint = if (session.puedeGolpear()) Cave.Text else Cave.TextFaint,
                accent = Cave.Bad
            )

            // Usar objeto
            RoundActionButton(
                currentItem?.icon ?: IconId.MOCHILA,
                onClick = { currentId?.let { session.useItem(it) } },
                diameter = (74 * scale).dp,
                enabled = currentItem != null,
                badge = currentId?.let { "${save.stockOf(it)}" }
            )
        }
        if (currentItem == null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "No te queda ningun objeto. Compra en la Tienda antes de bajar",
                fontSize = 10.sp, color = Cave.TextFaint
            )
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapSimple(onTap: () -> Unit) {
    awaitPointerEventScope {
        while (true) {
            val ev = awaitPointerEvent()
            val c = ev.changes.firstOrNull { it.pressed && !it.previousPressed }
            if (c != null) { c.consume(); onTap() }
        }
    }
}

// -------------------------------------------------------------- minimapa

@Composable
private fun Minimap(session: GameSession, sem: Semantics, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Cave.Void.copy(alpha = 0.72f))
    ) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val m = session.maze
            val cell = min(size.width / m.gw, size.height / m.gh)
            val ox = (size.width - cell * m.gw) / 2f
            val oy = (size.height - cell * m.gh) / 2f

            for (gy in 0 until m.gh) {
                for (gx in 0 until m.gw) {
                    val i = m.index(gx, gy)
                    if (!session.revealed[i]) continue
                    val solid = m.isSolid(gx, gy)
                    val col = when {
                        solid -> Cave.Stone.copy(alpha = 0.85f)
                        session.walked[i] -> Cave.AmberDeep.copy(alpha = 0.55f)
                        else -> Cave.StoneHi.copy(alpha = 0.75f)
                    }
                    drawRect(col, Offset(ox + gx * cell, oy + gy * cell), Size(cell, cell))
                }
            }

            // Trampas descubiertas
            for (t in session.traps) {
                if (!t.revealed) continue
                if (!session.revealed[m.index(t.gx, t.gy)]) continue
                drawCircle(
                    sem.trap.copy(alpha = 0.9f), cell * 0.42f,
                    Offset(ox + (t.gx + 0.5f) * cell, oy + (t.gy + 0.5f) * cell)
                )
            }

            // Marcas de tiza
            for (mk in session.marks) {
                val gx = (mk.x / GameSession.CELL)
                val gy = (mk.z / GameSession.CELL)
                drawCircle(
                    Cave.Ice, cell * 0.4f,
                    Offset(ox + gx * cell, oy + gy * cell)
                )
            }

            // Salida: se ve si esta revelada o si la reliquia hace ping
            val exitRevealed = session.revealed[m.index(m.exitGx, m.exitGy)] || session.exitPingFlash > 0f
            if (exitRevealed) {
                val ex = ox + (m.exitGx + 0.5f) * cell
                val ey = oy + (m.exitGy + 0.5f) * cell
                drawCircle(sem.exit.copy(alpha = 0.35f), cell * 1.5f, Offset(ex, ey))
                drawCircle(sem.exit, cell * 0.7f, Offset(ex, ey))
            }

            // Jugador
            val px = ox + (session.posX / GameSession.CELL) * cell
            val py = oy + (session.posZ / GameSession.CELL) * cell
            drawCircle(Cave.AmberSoft, cell * 0.8f, Offset(px, py))
            // Direccion de la mirada
            val yawRad = Math.toRadians(session.yawDeg.toDouble())
            val dx = Math.sin(yawRad).toFloat() * cell * 2.2f
            val dy = Math.cos(yawRad).toFloat() * cell * 2.2f
            drawLine(Cave.Amber, Offset(px, py), Offset(px + dx, py + dy), 2f * density)
        }
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                Cave.StoneEdge.copy(alpha = 0.8f),
                cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx()),
                style = Stroke(1.3f * density)
            )
        }
    }
}
