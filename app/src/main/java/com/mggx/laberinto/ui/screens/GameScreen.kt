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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mggx.laberinto.core.AhorroDeEnergia
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.EffectType
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.Tutorial
import com.mggx.laberinto.gl.CaveRenderer
import com.mggx.laberinto.ui.CaveBar
import com.mggx.laberinto.ui.CaveButton
import com.mggx.laberinto.ui.CaveButtonStyle
import com.mggx.laberinto.ui.MinimapMath
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
    onResume: () -> Unit,
    dispatch: (() -> Unit) -> Unit
) {
    val s = save.settings
    // Refresco del HUD a ~30 Hz jugando, y mucho mas lento en pausa.
    //
    // Con el menu de pausa abierto la partida esta congelada: no cambia ni una
    // barra de vida ni una casilla del mapa, asi que rehacer el dibujo 30 veces
    // por segundo es trabajo tirado. Y el minimapa no es barato: recorre 169
    // casillas y dibuja los bordes de cada una.
    var uiTick by remember { mutableIntStateOf(0) }
    val ritmoHud = AhorroDeEnergia.intervaloHudMs(
        if (paused) AhorroDeEnergia.Donde.PAUSA else AhorroDeEnergia.Donde.JUGANDO
    ) * 1_000_000L
    LaunchedEffect(ritmoHud) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                if (t - last > ritmoHud) { last = t; uiTick++ }
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

        // ----------------------------------------------------------- mira
        //
        // Va en el Box de afuera y no en el de los insets: tiene que caer en
        // el centro real de la imagen 3D, que es a donde apunta el golpe.
        if (!paused) {
            Mira(
                alAlcance = session.hayBichoAlAlcance(),
                cargada = session.puedeGolpear(),
                sem = sem,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Todo lo que sigue son paneles anclados a un borde de la pantalla
        // (HUD de arriba, minimapa, buffs, botones de accion): a proposito
        // en un Box aparte de las zonas tactiles de arriba, que si tienen
        // que llegar hasta el borde fisico real para no perder area de
        // gesto. Sin este padding, en un telefono con notch o con gestos de
        // navegacion, estos paneles pueden quedar tapados o pegados al
        // borde real en vez del borde util de la pantalla.
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {

        // ------------------------------------------------------ HUD arriba
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            StonePanel(Modifier.width((150 * s.buttonScale).dp), corner = 12.dp) {
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
                IconId.PAUSA, onPause, diameter = (36 * s.buttonScale).dp, opacity = 0.55f
            )
        }

        // ------------------------------------------------------- minimapa
        if (s.showMinimap) {
            Minimap(
                session, sem,
                // El minimapa se dibuja desde `session`, que es una clase
                // mutable comun (var posX, revealed[i], ...) y no un State de
                // Compose: nada avisa cuando cambia. Este tick es lo que lo
                // mantiene vivo; adentro de Minimap se explica por que tiene
                // que leerse dentro del propio Canvas.
                tick = uiTick,
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

        // ------------------------------------------------------ tutorial
        // Solo existe en el nivel 1 (GameSession.tutorial queda en null en el
        // resto). Va arriba al medio, debajo de la barra de estado: es el
        // unico lugar del HUD que no pelea ni con el minimapa ni con los
        // botones, y el jugador no tiene que taparlo con el pulgar para leerlo.
        session.tutorial?.pasoActual?.let { paso ->
            PanelTutorial(
                paso = paso,
                // El progreso se lee ACA, en el cuerpo del composable que
                // depende de uiTick, y no adentro de un hijo que reciba
                // `session`: si no, se congela igual que se congelaba el
                // minimapa.
                progreso = session.tutorial?.progreso ?: 0f,
                tick = uiTick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 58.dp)
                    .widthIn(max = 340.dp)
                    .padding(horizontal = 12.dp)
            )
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
                dispatch = dispatch,
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

        // ------------------------------------------------- caido (cooperativo)
        //
        // Sin este cartel el jugador no entiende nada: la pantalla sigue
        // andando pero el personaje no camina, y parece que se colgo el juego.
        if (session.caido && !paused) {
            val espera = session.esperaParaLevantarte()
            val companieros = session.red?.match?.otros()?.count { !it.caido } ?: 0
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Cave.Bad.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                StonePanel(Modifier.width(420.dp), glow = Cave.Bad) {
                    Column(
                        Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Estás en el piso", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when {
                                espera > 0.1f -> "Aguantá ${espera.toInt() + 1}..."
                                companieros > 0 -> "Que un compañero se te acerque para levantarte."
                                else -> "No queda nadie de pie que te pueda levantar."
                            },
                            style = MaterialTheme.typography.titleMedium, color = Cave.TextDim
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Podés seguir mirando alrededor para guiarlo.",
                            style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                        )
                    }
                }
            }
        }

        } // fin del Box con padding de insets seguros

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
    dispatch: (() -> Unit) -> Unit,
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
                        .size((42 * scale).dp)
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

        // Los botones van en DOS filas y no en una sola larga. En una fila
        // unica, los de mas a la izquierda quedaban lejos del pulgar y encima
        // se metian sobre la zona de mirar. Arriba queda lo que se toca de vez
        // en cuando (acciones del inventario, linterna, agacharse); abajo,
        // contra la esquina y mas grandes, lo que se toca todo el tiempo.
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            // Acciones contextuales
            Row(verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (session.pickCharges > 0) {
                    RoundActionButton(
                        IconId.PICO, { dispatch { session.breakWall() } },
                        diameter = (44 * scale).dp,
                        enabled = session.canBreakWall(), badge = "${session.pickCharges}"
                    )
                }
                if (session.phaseCharges > 0) {
                    RoundActionButton(
                        IconId.FANTASMA, { dispatch { session.phaseThrough() } },
                        diameter = (44 * scale).dp,
                        enabled = session.canPhase(), badge = "${session.phaseCharges}"
                    )
                }
                if (session.stats.freeSonarSeconds > 0f) {
                    RoundActionButton(
                        IconId.DIAPASON, { dispatch { session.useFreeSonar() } },
                        diameter = (44 * scale).dp,
                        enabled = session.freeSonarTimer <= 0f,
                        badge = if (session.freeSonarTimer > 0f) "${session.freeSonarTimer.roundToInt()}" else null
                    )
                }
            }

            // Linterna: solo aparece si compraste el poder.
            if (session.stats.tieneLinterna) {
                RoundActionButton(
                    IconId.LINTERNA,
                    onClick = { dispatch { session.toggleLinterna() } },
                    diameter = (44 * scale).dp,
                    tint = if (session.linternaEncendida) Cave.Amber else Cave.Text,
                    enabled = session.carburo > 0f || session.linternaEncendida,
                    badge = "${(session.carburo * 100).roundToInt()}%"
                )
            }

            // Agacharse: cicla de pie -> agachado -> arrastrandose -> de pie,
            // asi es un solo boton y no dos.
            RoundActionButton(
                when (input.crouchLevel) {
                    0 -> IconId.AGACHARSE
                    1 -> IconId.ARRASTRARSE
                    else -> IconId.DE_PIE
                },
                onClick = { input.crouchLevel = (input.crouchLevel + 1) % 3 },
                diameter = (48 * scale).dp,
                tint = if (input.crouchLevel == 0) Cave.Text else Cave.Amber,
                badge = when (session.postura) {
                    Postura.DE_PIE -> null
                    Postura.AGACHADO -> "bajo"
                    Postura.ARRASTRANDOSE -> "raso"
                }
            )
        }

        Spacer(Modifier.height(9.dp))

        // Fila de abajo: lo que se usa a cada rato, ordenado de menos a mas
        // usado hacia la esquina, que es donde el pulgar llega sin estirarse.
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            // Usar objeto
            RoundActionButton(
                currentItem?.icon ?: IconId.MOCHILA,
                onClick = { currentId?.let { id -> dispatch { session.useItem(id) } } },
                diameter = (50 * scale).dp,
                enabled = currentItem != null,
                badge = currentId?.let { "${save.stockOf(it)}" }
            )

            RoundActionButton(
                IconId.SALTAR,
                onClick = { input.jumpPending = true },
                diameter = (52 * scale).dp,
                enabled = session.enSuelo && session.postura.puedeSaltar
            )

            // Correr: no es un boton de toque sino de mantener apretado, asi que
            // el trabajo lo hace el pointerInput de abajo y no el onClick.
            RoundActionButton(
                IconId.CORRER,
                onClick = { },
                diameter = (52 * scale).dp,
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
            // limpia, para que nunca quedes sin forma de defenderte. Va ultimo
            // y es el mas grande a proposito: es la accion que mas se toca en
            // combate y la que menos puede fallarse por llegar mal.
            RoundActionButton(
                ItemCatalog.get(save.armaEquipada)?.icon ?: IconId.PUNO,
                onClick = { dispatch { session.golpear() } },
                diameter = (74 * scale).dp,
                enabled = session.puedeGolpear(),
                tint = if (session.puedeGolpear()) Cave.Text else Cave.TextFaint,
                accent = Cave.Bad
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

// ------------------------------------------------------------------ mira

/**
 * La mira del centro de la pantalla.
 *
 * Es chiquita y apagada mientras no haya nada que pegar, y se abre en cruz
 * roja cuando hay un bicho REALMENTE al alcance (la cuenta es la misma que
 * usa el golpe, ver GameSession.hayBichoAlAlcance). Sin esto no habia forma
 * de saber si estabas en rango salvo pegar y ver si pasaba algo, que es
 * justo lo que hacia sentir que habia que estar encima del bicho.
 *
 * [cargada] en false es el golpe todavia recargando: la mira se apaga para
 * que se entienda que el problema no es la punteria sino el tiempo.
 */
@Composable
private fun Mira(alAlcance: Boolean, cargada: Boolean, sem: Semantics, modifier: Modifier = Modifier) {
    Canvas(modifier.size(46.dp)) {
        // La lambda captura los dos booleanos, asi que se vuelve a dibujar
        // cuando cambian y no antes.
        val c = Offset(size.width / 2f, size.height / 2f)
        val color = when {
            alAlcance && cargada -> sem.bad
            alAlcance -> sem.bad.copy(alpha = 0.45f)
            else -> Cave.Text.copy(alpha = 0.35f)
        }
        val hueco = if (alAlcance) 7f * density else 4f * density
        val brazo = if (alAlcance) 9f * density else 4.5f * density
        val grosor = if (alAlcance) 2.4f * density else 1.6f * density
        drawCircle(color.copy(alpha = 0.9f), 1.5f * density, c)
        for (a in 0 until 4) {
            val dx = if (a == 0) -1f else if (a == 1) 1f else 0f
            val dy = if (a == 2) -1f else if (a == 3) 1f else 0f
            drawLine(
                color,
                Offset(c.x + dx * hueco, c.y + dy * hueco),
                Offset(c.x + dx * (hueco + brazo), c.y + dy * (hueco + brazo)),
                grosor, cap = StrokeCap.Round
            )
        }
    }
}

// -------------------------------------------------------------- tutorial

/**
 * El cartel del guion del nivel 1: que hay que hacer ahora y cuanto falta.
 *
 * Recibe el [paso] y el [progreso] ya resueltos en vez de la sesion entera, y
 * un [tick] que cambia todos los cuadros. Es el mismo cuidado que hubo que
 * tener con el minimapa: un composable que recibe un objeto mutable siempre
 * igual (la sesion) se saltea la recomposicion para siempre y queda clavado.
 */
@Composable
private fun PanelTutorial(
    paso: Tutorial.Paso,
    progreso: Float,
    tick: Int,
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_EXPRESSION") tick
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Cave.Void.copy(alpha = 0.78f))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            paso.titulo,
            style = MaterialTheme.typography.titleMedium,
            color = Cave.Amber
        )
        Spacer(Modifier.height(3.dp))
        Text(
            paso.ayuda,
            style = MaterialTheme.typography.bodySmall,
            color = Cave.TextDim
        )
        Spacer(Modifier.height(7.dp))
        // La barrita no es decoracion: es la unica senal de que lo que estas
        // haciendo cuenta para el paso. Sin ella, girar despacio parece que no
        // hace nada.
        Canvas(Modifier.fillMaxWidth().height(4.dp)) {
            drawRoundRect(
                Cave.StoneHi,
                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
            )
            if (progreso > 0.001f) {
                drawRoundRect(
                    Cave.Amber,
                    size = Size(size.width * progreso.coerceIn(0f, 1f), size.height),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
            }
        }
    }
}

// -------------------------------------------------------------- minimapa

/** Cuantas casillas se ven para cada lado del jugador. Es un radar, no un mapa entero. */
private const val MINIMAP_RADIO_CELDAS = 6

@Composable
private fun Minimap(session: GameSession, sem: Semantics, tick: Int, modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).background(Cave.Void.copy(alpha = 0.9f))) {
        Canvas(Modifier.fillMaxSize().padding(5.dp)) {
            // `tick` se lee ACA ADENTRO, no en el cuerpo del composable, y es
            // lo unico que mantiene vivo al minimapa.
            //
            // Compose memoriza la lambda de dibujo segun lo que captura. Si
            // solo capturara `session` —el mismo objeto de principio a fin del
            // nivel, con campos mutables comunes que no son State— la lambda
            // seria siempre la misma instancia, el modificador de dibujo nunca
            // cambiaria y este Canvas se dibujaria UNA sola vez: el minimapa
            // quedaba congelado en el primer cuadro, casi siempre antes de
            // revelar ninguna casilla (el famoso "cuadrado negro" que no se
            // desbloquea ni se mueve). Capturando el tick, que cambia ~30
            // veces por segundo, la lambda cambia y el dibujo se rehace.
            @Suppress("UNUSED_EXPRESSION") tick
            val m = session.maze
            val radio = min(size.width, size.height) / 2f
            val cell = radio / MINIMAP_RADIO_CELDAS
            val cx = size.width / 2f
            val cy = size.height / 2f
            val centro = Offset(cx, cy)
            val yaw = session.yawDeg
            val pgx = (session.posX / GameSession.CELL).toInt()
            val pgy = (session.posZ / GameSession.CELL).toInt()

            // Es un radar centrado en el jugador y rotado con su mirada, no
            // un mapa entero: mostrar el laberinto completo rotando lo deja
            // ilegible en niveles grandes, y un vistazo rapido necesita
            // "arriba = para donde voy", no "arriba = norte".
            fun aPantalla(worldX: Float, worldZ: Float): Offset {
                val (sx, sy) = MinimapMath.rotarHaciaArriba(
                    worldX - session.posX, worldZ - session.posZ, yaw
                )
                return Offset(cx + sx / GameSession.CELL * cell, cy + sy / GameSession.CELL * cell)
            }

            /** Cuanto se desvanece algo por estar cerca del borde del radar. */
            fun fundido(p: Offset): Float {
                val d = hypot(p.x - cx, p.y - cy) / radio
                return (1f - ((d - 0.72f) / 0.28f)).coerceIn(0f, 1f)
            }

            // Fondo: mas claro en el centro que en el borde, para que el
            // radar se lea como un pozo de luz y no como un cuadrado negro.
            drawCircle(
                Brush.radialGradient(
                    listOf(Cave.Deep, Cave.Void),
                    center = centro, radius = radio
                ),
                radio, centro
            )

            // --------------------------------------------------- el terreno
            // El piso se pinta y las paredes se dibujan como lineas en el
            // borde de la casilla, en vez de pintar cuadrados de roca. Es la
            // diferencia entre un mapa y una grilla de colores: asi se ve el
            // tunel, no un mosaico.
            val paso = GameSession.CELL
            for (gy in (pgy - MINIMAP_RADIO_CELDAS)..(pgy + MINIMAP_RADIO_CELDAS)) {
                for (gx in (pgx - MINIMAP_RADIO_CELDAS)..(pgx + MINIMAP_RADIO_CELDAS)) {
                    if (gx !in 0 until m.gw || gy !in 0 until m.gh) continue
                    val i = m.index(gx, gy)
                    if (!session.revealed[i] || m.isSolid(gx, gy)) continue
                    val p = aPantalla((gx + 0.5f) * paso, (gy + 0.5f) * paso)
                    val f = fundido(p)
                    if (f <= 0.01f) continue
                    // Por donde pasaste se ve calido; lo que te revelo algun
                    // poder pero no pisaste, apagado. El agua va en frio: es
                    // informacion que importa (chapotear hace ruido y te oyen),
                    // y de un vistazo tiene que distinguirse del piso seco.
                    val col = when {
                        m.hayAgua(gx, gy) -> Cave.Ice.copy(alpha = 0.42f * f)
                        session.walked[i] -> Cave.AmberDeep.copy(alpha = 0.55f * f)
                        else -> Cave.StoneHi.copy(alpha = 0.5f * f)
                    }
                    rotate(yaw, p) {
                        drawRect(
                            col,
                            Offset(p.x - cell * 0.55f, p.y - cell * 0.55f),
                            Size(cell * 1.1f, cell * 1.1f)
                        )
                    }
                }
            }

            // Las paredes del tramo descubierto: solo el borde entre una
            // casilla abierta que ya viste y la roca de al lado.
            for (gy in (pgy - MINIMAP_RADIO_CELDAS)..(pgy + MINIMAP_RADIO_CELDAS)) {
                for (gx in (pgx - MINIMAP_RADIO_CELDAS)..(pgx + MINIMAP_RADIO_CELDAS)) {
                    if (gx !in 0 until m.gw || gy !in 0 until m.gh) continue
                    if (!session.revealed[m.index(gx, gy)] || m.isSolid(gx, gy)) continue
                    val x0 = gx * paso; val x1 = (gx + 1) * paso
                    val z0 = gy * paso; val z1 = (gy + 1) * paso
                    // (vecino dx, vecino dy, esquina A, esquina B)
                    val lados = arrayOf(
                        intArrayOf(0, -1), intArrayOf(0, 1),
                        intArrayOf(-1, 0), intArrayOf(1, 0)
                    )
                    for ((n, d) in lados.withIndex()) {
                        val vx = gx + d[0]; val vy = gy + d[1]
                        val hayPared = !m.inBounds(vx, vy) || m.isSolid(vx, vy)
                        if (!hayPared) continue
                        val a: Offset; val b: Offset
                        when (n) {
                            0 -> { a = aPantalla(x0, z0); b = aPantalla(x1, z0) }
                            1 -> { a = aPantalla(x0, z1); b = aPantalla(x1, z1) }
                            2 -> { a = aPantalla(x0, z0); b = aPantalla(x0, z1) }
                            else -> { a = aPantalla(x1, z0); b = aPantalla(x1, z1) }
                        }
                        val f = fundido(Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f))
                        if (f <= 0.01f) continue
                        drawLine(
                            Cave.StoneEdge.copy(alpha = 0.95f * f), a, b,
                            1.7f * density, cap = StrokeCap.Round
                        )
                    }
                }
            }

            // ---------------------------------------------------- las marcas
            // Trampas que ya descubriste: se marcan con una cruz, no con un
            // punto, para que no se confundan con un companiero.
            for (t in session.traps) {
                if (!t.revealed) continue
                if (!session.revealed[m.index(t.gx, t.gy)]) continue
                val p = aPantalla((t.gx + 0.5f) * paso, (t.gy + 0.5f) * paso)
                val f = fundido(p)
                if (f <= 0.01f) continue
                val r = cell * 0.3f
                val col = sem.trap.copy(alpha = 0.95f * f)
                drawLine(col, Offset(p.x - r, p.y - r), Offset(p.x + r, p.y + r), 2f * density)
                drawLine(col, Offset(p.x - r, p.y + r), Offset(p.x + r, p.y - r), 2f * density)
            }

            /**
             * Marca que sobrevive al borde del radar: si el punto queda
             * afuera, se pega al borde apuntando para donde esta.
             */
            fun marcaLejana(worldX: Float, worldZ: Float, col: Color, r: Float) {
                val (sx, sy) = MinimapMath.rotarHaciaArriba(
                    worldX - session.posX, worldZ - session.posZ, yaw
                )
                val (px, py, pegada) = MinimapMath.pegarAlBorde(
                    sx / GameSession.CELL * cell, sy / GameSession.CELL * cell, radio * 0.86f
                )
                val p = Offset(cx + px, cy + py)
                if (pegada) {
                    // Pegada al borde: aro hueco, para que se lea como
                    // "para alla" y no como "esta justo aca".
                    drawCircle(col.copy(alpha = 0.75f), r * 0.85f, p, style = Stroke(2f * density))
                } else {
                    drawCircle(col.copy(alpha = 0.3f), r * 1.9f, p)
                    drawCircle(col, r, p)
                }
            }

            // La salida SOLO con la Rosa de los Vientos, y solo mientras dura
            // el parpadeo. Sin la reliquia el mapa no dice donde esta: es el
            // nivel el que hay que resolver, no el minimapa.
            if (session.stats.exitPingSeconds > 0f && session.exitPingFlash > 0f) {
                marcaLejana(
                    (m.exitGx + 0.5f) * paso, (m.exitGy + 0.5f) * paso,
                    sem.exit, cell * 0.5f
                )
            }

            // Vetagris y cofres SOLO con el Ojo de la Veta, que es justo lo
            // que ese poder promete en la tienda. Sin el poder, encontrarlos
            // es cosa de caminar y mirar.
            if (session.stats.verTesoros) {
                for (pk in session.pickups) {
                    if (pk.taken) continue
                    if (pk.kind != GameSession.PickupKind.VETAGRIS &&
                        pk.kind != GameSession.PickupKind.COFRE
                    ) continue
                    val col =
                        if (pk.kind == GameSession.PickupKind.VETAGRIS) Cave.Vetagris
                        else Cave.AmberSoft
                    marcaLejana((pk.gx + 0.5f) * paso, (pk.gy + 0.5f) * paso, col, cell * 0.32f)
                }
            }

            // Companeros de sala, en cooperativo (o cualquier modo de a
            // varios): la posicion suavizada, no la cruda, para que no
            // salten entre mensaje y mensaje de red.
            session.red?.match?.otros()?.forEach { j ->
                if (j.sinPose) return@forEach
                marcaLejana(j.dibX, j.dibZ, if (j.caido) sem.bad else Cave.Ice, cell * 0.34f)
            }

            // ------------------------------------------------- brujula y aro
            // Cuatro marcas cardinales que giran con el mundo. La del norte
            // es mas larga y clara: sin una referencia fija, un radar que
            // rota te deja sin saber para donde estas yendo en el nivel.
            for (k in 0 until 4) {
                val (ux, uy) = MinimapMath.rotarHaciaArriba(
                    if (k == 1) 1f else if (k == 3) -1f else 0f,
                    if (k == 0) 1f else if (k == 2) -1f else 0f,
                    yaw
                )
                val norte = k == 0
                val largo = if (norte) 7f * density else 4f * density
                val col =
                    if (norte) Cave.AmberSoft.copy(alpha = 0.9f)
                    else Cave.StoneEdge.copy(alpha = 0.9f)
                drawLine(
                    col,
                    Offset(cx + ux * radio, cy + uy * radio),
                    Offset(cx + ux * (radio - largo), cy + uy * (radio - largo)),
                    if (norte) 2.4f * density else 1.6f * density,
                    cap = StrokeCap.Round
                )
            }

            // ------------------------------------------------------ el flecha
            // Jugador: siempre fijo en el centro mirando hacia arriba, ya
            // que es el MUNDO el que rota alrededor de el. Es una punta de
            // flecha, no un punto: asi se ve de un vistazo para donde mira.
            val punta = Offset(cx, cy - cell * 0.85f)
            val ala = cell * 0.5f
            drawCircle(Cave.Amber.copy(alpha = 0.18f), cell * 1.15f, centro)
            val flecha = Path().apply {
                moveTo(punta.x, punta.y)
                lineTo(cx - ala, cy + cell * 0.55f)
                lineTo(cx, cy + cell * 0.2f)
                lineTo(cx + ala, cy + cell * 0.55f)
                close()
            }
            drawPath(flecha, Cave.AmberSoft)
            drawPath(flecha, Cave.Void.copy(alpha = 0.65f), style = Stroke(1.2f * density))
        }
        // El aro de afuera, encima de todo, para que el radar tenga un borde
        // limpio y no quede flotando sobre el juego.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Cave.StoneEdge.copy(alpha = 0.85f),
                min(size.width, size.height) / 2f - density,
                Offset(size.width / 2f, size.height / 2f),
                style = Stroke(1.6f * density)
            )
        }
    }
}

