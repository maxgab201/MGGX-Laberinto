package com.mggx.laberinto.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Constructor de trazados en un lienzo virtual de 24x24 que se escala al tamano real.
 * Solo se usan lineTo/cubicTo/primitivas para maxima compatibilidad.
 */
class SP(@PublishedApi internal val u: Float) {
    val path = Path()
    fun m(x: Float, y: Float) { path.moveTo(x * u, y * u) }
    fun l(x: Float, y: Float) { path.lineTo(x * u, y * u) }
    fun c(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) {
        path.cubicTo(x1 * u, y1 * u, x2 * u, y2 * u, x * u, y * u)
    }
    fun z() { path.close() }
    fun oval(cx: Float, cy: Float, rx: Float, ry: Float) {
        path.addOval(Rect((cx - rx) * u, (cy - ry) * u, (cx + rx) * u, (cy + ry) * u))
    }
    fun rect(x: Float, y: Float, w: Float, h: Float) {
        path.addRect(Rect(x * u, y * u, (x + w) * u, (y + h) * u))
    }
    fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        path.addRoundRect(
            RoundRect(Rect(x * u, y * u, (x + w) * u, (y + h) * u), CornerRadius(r * u, r * u))
        )
    }
}

/** Contexto de dibujo de un icono. */
class Pen(val ds: DrawScope, val u: Float, val main: Color, val accent: Color)

inline fun Pen.fill(color: Color = main, build: SP.() -> Unit) {
    val sp = SP(u); sp.build(); ds.drawPath(sp.path, color)
}

inline fun Pen.line(color: Color = main, w: Float = 1.7f, build: SP.() -> Unit) {
    val sp = SP(u); sp.build()
    ds.drawPath(sp.path, color, style = Stroke(w * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

fun Pen.seg(x1: Float, y1: Float, x2: Float, y2: Float, color: Color = main, w: Float = 1.7f) {
    ds.drawLine(color, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), w * u, StrokeCap.Round)
}

fun Pen.dot(cx: Float, cy: Float, r: Float, color: Color = main) {
    ds.drawCircle(color, r * u, Offset(cx * u, cy * u))
}

fun Pen.ring(cx: Float, cy: Float, r: Float, color: Color = main, w: Float = 1.7f) {
    ds.drawCircle(color, r * u, Offset(cx * u, cy * u), style = Stroke(w * u))
}

/**
 * Icono vectorial del juego. [tint] es el trazo principal y [accent] el detalle
 * (llama, gema, brillo).
 */
@Composable
fun CaveIcon(
    id: IconId,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color(0xFFE6DCCB),
    accent: Color = Color(0xFFFFA24B)
) {
    Canvas(modifier = modifier.size(size)) {
        drawCaveIcon(id, this.size.minDimension, tint, accent)
    }
}

fun DrawScope.drawCaveIcon(id: IconId, box: Float, tint: Color, accent: Color) {
    val pen = Pen(this, box / 24f, tint, accent)
    if (!drawItemIcon(pen, id)) drawUiIcon(pen, id)
}
