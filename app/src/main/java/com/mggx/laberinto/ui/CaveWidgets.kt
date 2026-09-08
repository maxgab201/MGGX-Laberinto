package com.mggx.laberinto.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mggx.laberinto.ui.icons.CaveIcon
import com.mggx.laberinto.ui.icons.IconId
import com.mggx.laberinto.ui.theme.Cave
import kotlin.math.roundToInt

/** Panel de piedra: fondo degradado, borde de mineral y brillo interno. */
@Composable
fun StonePanel(
    modifier: Modifier = Modifier,
    glow: Color = Cave.StoneEdge,
    corner: Dp = 16.dp,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(
                Brush.verticalGradient(
                    listOf(Cave.StoneHi, Cave.Stone, Cave.Deep)
                )
            )
    ) {
        // matchParentSize y no fillMaxSize: el fondo NO tiene que decidir el
        // tamano del panel, si no el panel se estira y empuja todo lo de abajo.
        Canvas(Modifier.matchParentSize()) {
            val r = corner.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(glow.copy(alpha = 0.55f), glow.copy(alpha = 0.10f))
                ),
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = 1.4f * density)
            )
            // Brillo superior, como si la antorcha pegara desde arriba
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.045f),
                    0.35f to Color.Transparent
                ),
                cornerRadius = CornerRadius(r, r)
            )
        }
        content()
    }
}

enum class CaveButtonStyle { PRIMARIO, SECUNDARIO, PELIGRO, FANTASMA }

@Composable
fun CaveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: IconId? = null,
    style: CaveButtonStyle = CaveButtonStyle.SECUNDARIO,
    enabled: Boolean = true,
    subtitle: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val accent = when (style) {
        CaveButtonStyle.PRIMARIO -> Cave.Amber
        CaveButtonStyle.SECUNDARIO -> Cave.StoneEdge
        CaveButtonStyle.PELIGRO -> Cave.Bad
        CaveButtonStyle.FANTASMA -> Color.Transparent
    }
    val bg = when (style) {
        CaveButtonStyle.PRIMARIO -> Brush.horizontalGradient(
            listOf(Cave.AmberDeep.copy(alpha = 0.55f), Cave.Amber.copy(alpha = 0.30f))
        )
        CaveButtonStyle.PELIGRO -> Brush.horizontalGradient(
            listOf(Cave.Bad.copy(alpha = 0.30f), Cave.Bad.copy(alpha = 0.14f))
        )
        CaveButtonStyle.FANTASMA -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
        else -> Brush.verticalGradient(listOf(Cave.StoneHi, Cave.Stone))
    }
    val contentColor = when {
        !enabled -> Cave.TextFaint
        style == CaveButtonStyle.PRIMARIO -> Cave.AmberSoft
        style == CaveButtonStyle.PELIGRO -> Cave.Bad
        else -> Cave.Text
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .padding(horizontal = 18.dp, vertical = if (subtitle == null) 13.dp else 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (style != CaveButtonStyle.FANTASMA) {
            Canvas(Modifier.matchParentSize()) {
                drawRoundRect(
                    color = accent.copy(alpha = if (pressed) 0.85f else 0.45f),
                    cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx()),
                    style = Stroke(width = 1.3f * density)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                CaveIcon(
                    icon, size = 20.dp,
                    tint = contentColor,
                    accent = if (style == CaveButtonStyle.PRIMARIO) Cave.Amber else Cave.AmberSoft
                )
                Spacer(Modifier.width(11.dp))
            }
            Column {
                Text(
                    text.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = Cave.TextFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Boton redondo grande de los controles del juego. */
@Composable
fun RoundActionButton(
    icon: IconId,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 62.dp,
    tint: Color = Cave.Text,
    accent: Color = Cave.Amber,
    enabled: Boolean = true,
    badge: String? = null,
    opacity: Float = 0.72f
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(diameter)
                .clip(RoundedCornerShape(diameter / 2))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Cave.StoneHi.copy(alpha = opacity),
                            Cave.Deep.copy(alpha = opacity * 0.95f)
                        )
                    )
                )
                .alpha(if (enabled) 1f else 0.35f)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    color = if (pressed) accent.copy(alpha = 0.95f) else accent.copy(alpha = 0.42f),
                    style = Stroke(width = 1.8f * density)
                )
            }
            CaveIcon(icon, size = diameter * 0.46f, tint = tint, accent = accent)
        }
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Cave.AmberDeep)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Cave.Void)
            }
        }
    }
}

/** Barra de progreso fina con relleno degradado (vida, aguante, buffs). */
@Composable
fun CaveBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    background: Color = Cave.Void.copy(alpha = 0.75f)
) {
    Canvas(modifier.height(height)) {
        val r = size.height / 2f
        drawRoundRect(background, cornerRadius = CornerRadius(r, r))
        val w = size.width * fraction.coerceIn(0f, 1f)
        if (w > 1f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color)),
                size = Size(w, size.height),
                cornerRadius = CornerRadius(r, r)
            )
        }
        drawRoundRect(
            color = color.copy(alpha = 0.30f),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = 1f * density)
        )
    }
}

/** Deslizador con estilo de cueva. */
@Composable
fun CaveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    accent: Color = Cave.Amber
) {
    var trackWidth by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    val span = valueRange.endInclusive - valueRange.start
    val frac = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    fun emit(px: Float) {
        var f = (px / trackWidth).coerceIn(0f, 1f)
        if (steps > 0) f = (f * steps).roundToInt().toFloat() / steps
        onValueChange(valueRange.start + f * span)
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .onSizeChanged { trackWidth = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(steps, valueRange) {
                detectTapGestures { emit(it.x) }
            }
            .pointerInput(steps, valueRange) {
                detectHorizontalDragGestures { change, _ -> emit(change.position.x) }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(Modifier.fillMaxWidth().height(40.dp)) {
            val cy = size.height / 2f
            val h = with(density) { 6.dp.toPx() }
            drawRoundRect(
                Cave.Void.copy(alpha = 0.8f),
                topLeft = Offset(0f, cy - h / 2),
                size = Size(size.width, h),
                cornerRadius = CornerRadius(h / 2, h / 2)
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.5f), accent)),
                topLeft = Offset(0f, cy - h / 2),
                size = Size(size.width * frac, h),
                cornerRadius = CornerRadius(h / 2, h / 2)
            )
            val kx = size.width * frac
            val kr = with(density) { 10.dp.toPx() }
            drawCircle(Cave.Deep, kr, Offset(kx, cy))
            drawCircle(accent, kr, Offset(kx, cy), style = Stroke(2f * this.density))
            drawCircle(accent.copy(alpha = 0.85f), kr * 0.34f, Offset(kx, cy))
        }
    }
}

/** Interruptor de dos estados. */
@Composable
fun CaveToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Cave.Amber
) {
    Box(
        modifier = modifier
            .size(width = 58.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) accent.copy(alpha = 0.22f) else Cave.Void.copy(alpha = 0.8f))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = if (checked) accent.copy(alpha = 0.8f) else Cave.StoneEdge,
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(1.4f * density)
            )
        }
        Box(
            Modifier
                .padding(4.dp)
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) accent else Cave.StoneEdge),
            contentAlignment = Alignment.Center
        ) {
            if (checked) CaveIcon(IconId.TILDE, size = 14.dp, tint = Cave.Void, accent = Cave.Void)
        }
    }
}

/** Fila de una pantalla de ajustes: icono, titulo, explicacion y control. */
@Composable
fun SettingRow(
    icon: IconId,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    control: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Cave.Void.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            CaveIcon(icon, size = 22.dp, tint = Cave.AmberSoft, accent = Cave.Amber)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint)
        }
        Spacer(Modifier.width(14.dp))
        control()
    }
}

/** Chip de moneda con su icono propio. */
@Composable
fun CurrencyChip(
    icon: IconId,
    amount: Int,
    modifier: Modifier = Modifier,
    accent: Color = Cave.Amber
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Cave.Void.copy(alpha = 0.72f))
            .padding(start = 7.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CaveIcon(icon, size = 22.dp, tint = accent, accent = accent)
        Spacer(Modifier.width(7.dp))
        Text(
            formatNumber(amount),
            style = MaterialTheme.typography.titleMedium,
            color = Cave.Text
        )
    }
}

/** Pestañas horizontales. */
@Composable
fun CaveTabs(
    titles: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Cave.Void.copy(alpha = 0.55f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        titles.forEachIndexed { i, t ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) Cave.Amber.copy(alpha = 0.20f) else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                val baseFontSize = MaterialTheme.typography.labelMedium.fontSize
                var fontSize by remember(t, baseFontSize) { mutableStateOf(baseFontSize) }
                Text(
                    t.uppercase(),
                    fontSize = fontSize,
                    color = if (on) Cave.AmberSoft else Cave.TextDim,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    // Con cinco categorias las etiquetas ya no entran en un
                    // telefono angosto: se achican de verdad hasta entrar.
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    onTextLayout = { r ->
                        if (r.hasVisualOverflow && fontSize > 9.sp) fontSize *= 0.92f
                    }
                )
            }
        }
    }
}

/** Titulo de seccion con linea de mineral. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, accent: Color = Cave.Amber) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = accent)
        Spacer(Modifier.width(12.dp))
        Canvas(Modifier.weight(1f).height(2.dp)) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.45f), Color.Transparent)
                ),
                cornerRadius = CornerRadius(1f, 1f)
            )
        }
    }
}

/** Fondo animado de cueva para los menus: siluetas de roca, polvo y antorcha. */
@Composable
fun CaveBackdrop(modifier: Modifier = Modifier, seed: Int = 7) {
    val trans = rememberInfiniteTransition(label = "cueva")
    val flicker by trans.animateFloat(
        initialValue = 0.82f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse),
        label = "antorcha"
    )
    val drift by trans.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24000), RepeatMode.Restart),
        label = "polvo"
    )

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(Brush.verticalGradient(listOf(Cave.Void, Cave.Deep, Color(0xFF120E0A))))

        // Halo de antorcha desde arriba a la izquierda
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Cave.Amber.copy(alpha = 0.11f * flicker),
                    Cave.AmberDeep.copy(alpha = 0.05f * flicker),
                    Color.Transparent
                ),
                center = Offset(w * 0.20f, h * 0.12f),
                radius = w * 0.62f
            ),
            radius = w * 0.62f,
            center = Offset(w * 0.20f, h * 0.12f)
        )

        // Capas de roca: cada una un poco mas clara y desplazada
        val layers = 4
        for (l in 0 until layers) {
            val depth = l / (layers - 1f)
            val baseY = h * (0.42f + depth * 0.20f)
            val amp = h * (0.07f + depth * 0.05f)
            val alpha = 0.20f + depth * 0.28f
            val col = Cave.Stone.copy(alpha = alpha)
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(0f, h)
            path.lineTo(0f, baseY)
            var x = 0f
            var i = 0
            val step = w / 13f
            while (x <= w + step) {
                val n = pseudo(seed + l * 31 + i)
                val y = baseY - amp * (0.35f + n * 0.65f)
                path.lineTo(x, y)
                x += step
                i++
            }
            path.lineTo(w, h)
            path.close()
            drawPath(path, col)
        }

        // Estalactitas colgando del techo
        for (i in 0 until 9) {
            val n = pseudo(seed * 3 + i * 17)
            val x = w * (0.05f + 0.10f * i + n * 0.03f)
            val len = h * (0.07f + n * 0.15f)
            val wd = w * (0.012f + n * 0.014f)
            val p = androidx.compose.ui.graphics.Path()
            p.moveTo(x - wd, 0f); p.lineTo(x + wd, 0f); p.lineTo(x, len); p.close()
            drawPath(p, Cave.Stone.copy(alpha = 0.55f))
            drawPath(p, Cave.StoneEdge.copy(alpha = 0.22f), style = Stroke(1.2f))
        }

        // Motas de polvo flotando
        for (i in 0 until 42) {
            val n = pseudo(seed * 5 + i * 7)
            val n2 = pseudo(seed * 11 + i * 13)
            val x = ((n + drift * (0.15f + n2 * 0.35f)) % 1f) * w
            val y = ((n2 + drift * 0.08f) % 1f) * h
            val r = 1f + n2 * 2.2f
            drawCircle(
                Cave.AmberSoft.copy(alpha = 0.05f + n * 0.10f * flicker),
                r, Offset(x, y)
            )
        }

        // Vineta
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Cave.Void.copy(alpha = 0.80f)),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = maxOf(w, h) * 0.72f
            )
        )
    }
}

private fun pseudo(i: Int): Float {
    var h = i * 374761393
    h = (h xor (h shr 13)) * 1274126177
    h = h xor (h shr 16)
    return (h and 0x7FFFFFFF) / 2147483647.0f
}

fun formatNumber(n: Int): String {
    if (n < 1000) return n.toString()
    val s = n.toString()
    val sb = StringBuilder()
    var c = 0
    for (i in s.indices.reversed()) {
        sb.append(s[i])
        c++
        if (c % 3 == 0 && i != 0) sb.append('.')
    }
    return sb.reverse().toString()
}

fun formatTime(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return String.format("%d:%02d", m, s)
}

fun formatLongTime(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    return if (h > 0) "${h} h ${m} min" else "${m} min"
}

/**
 * Campo de texto con la pinta del resto del juego.
 *
 * Es el unico del juego (se usa para el nombre y el codigo de sala), asi que
 * esta armado sobre BasicTextField en vez de traerse Material entero: lo
 * unico que hace falta es una linea de texto adentro de una placa de piedra.
 */
@Composable
fun CaveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxChars: Int = 24,
    centrado: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    mayusculas: Boolean = false
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Cave.Void.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        contentAlignment = if (centrado) Alignment.Center else Alignment.CenterStart
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                placeholder, fontSize = fontSize, color = Cave.TextFaint,
                maxLines = 1, textAlign = if (centrado) TextAlign.Center else TextAlign.Start
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = { nuevo ->
                // El recorte se hace aca y no en el que llama: asi ningun uso
                // se puede olvidar y terminar con un nombre de 300 letras.
                val limpio = nuevo.replace("\n", "").take(maxChars)
                onValueChange(if (mayusculas) limpio.uppercase() else limpio)
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Cave.Text,
                fontSize = fontSize,
                letterSpacing = if (centrado) 4.sp else 0.sp,
                textAlign = if (centrado) TextAlign.Center else TextAlign.Start
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Cave.Amber),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
