package com.mggx.laberinto.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.Currency
import com.mggx.laberinto.maze.CaveTheme
import com.mggx.laberinto.maze.MazeGenerator
import com.mggx.laberinto.ui.CaveBackdrop
import com.mggx.laberinto.ui.CaveButton
import com.mggx.laberinto.ui.CaveButtonStyle
import com.mggx.laberinto.ui.CurrencyChip
import com.mggx.laberinto.ui.StonePanel
import com.mggx.laberinto.ui.formatNumber
import com.mggx.laberinto.ui.icons.CaveIcon
import com.mggx.laberinto.ui.icons.IconId
import com.mggx.laberinto.ui.theme.Cave

@Composable
fun LobbyScreen(
    save: SaveData,
    refreshKey: Int,
    onPlay: (Int) -> Unit,
    onShop: () -> Unit,
    onLoadout: () -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit
) {
    val trans = rememberInfiniteTransition(label = "lobby")
    val pulse by trans.animateFloat(
        0.85f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "latido"
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // El lobby se achica solo en pantallas angostas para que nunca se
        // desborde ni se pisen los textos.
        val k = (maxWidth / 860.dp).coerceIn(0.58f, 1f)
        val compacto = maxWidth < 660.dp

        CaveBackdrop(seed = 11)

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = (26 * k).dp, vertical = (18 * k).dp)
        ) {
            // -------------------------------------------------- columna izquierda
            Column(
                Modifier.weight(1.15f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LabyrinthGlyph(pulse, (78 * k).dp)
                    Spacer(Modifier.width((16 * k).dp))
                    Column {
                        Text(
                            "MGGX",
                            fontSize = (42 * k).sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (8 * k).sp,
                            color = Cave.Text,
                            maxLines = 1
                        )
                        Text(
                            "LABERINTO",
                            fontSize = (26 * k).sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (9 * k).sp,
                            color = Cave.Amber,
                            maxLines = 1
                        )
                    }
                }
                if (!compacto) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Bajas, te perdes, encontras la salida. Y despues bajas mas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Cave.TextFaint,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height((26 * k).dp))

                val level = save.currentLevel
                val theme = CaveTheme.forLevel(level)
                StonePanel(
                    Modifier.fillMaxWidth(0.92f),
                    glow = Cave.Amber
                ) {
                    Column(Modifier.padding((18 * k).dp)) {
                        Text(
                            "PROXIMO DESCENSO",
                            style = MaterialTheme.typography.labelMedium,
                            color = Cave.AmberDeep
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "Nivel $level",
                                style = MaterialTheme.typography.headlineLarge,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                MazeGenerator.difficultyLabel(level),
                                style = MaterialTheme.typography.titleMedium,
                                color = Cave.TextDim,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Text(
                            theme.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Cave.Amber
                        )
                        if (!compacto) {
                            Text(
                                theme.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Cave.TextFaint,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height((16 * k).dp))
                        CaveButton(
                            "Descender",
                            onClick = { onPlay(level) },
                            icon = IconId.JUGAR,
                            style = CaveButtonStyle.PRIMARIO,
                            modifier = Modifier.fillMaxWidth(),
                            subtitle = if (compacto) null else "Empeza el nivel $level"
                        )
                    }
                }

                if (save.maxLevel > 1) {
                    Spacer(Modifier.height((14 * k).dp))
                    LevelPicker(save, refreshKey, onPlay)
                }
            }

            Spacer(Modifier.width(24.dp))

            // -------------------------------------------------- columna derecha
            Column(
                Modifier.weight(0.85f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CurrencyChip(IconId.MONEDA_ECO, save.balance(Currency.ECOS), accent = Cave.AmberSoft)
                    CurrencyChip(IconId.MONEDA_VETAGRIS, save.balance(Currency.VETAGRIS), accent = Cave.Vetagris)
                }
                Spacer(Modifier.height(20.dp))

                StonePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        CaveButton(
                            "Tienda", onShop, icon = IconId.TIENDA,
                            modifier = Modifier.fillMaxWidth(),
                            subtitle = if (compacto) null else "61 objetos para el descenso"
                        )
                        CaveButton(
                            "Equipo", onLoadout, icon = IconId.MOCHILA,
                            modifier = Modifier.fillMaxWidth(),
                            subtitle = if (compacto) null else "Reliquias y ranuras rapidas"
                        )
                        CaveButton(
                            "Ajustes", onSettings, icon = IconId.ENGRANAJE,
                            modifier = Modifier.fillMaxWidth(),
                            subtitle = if (compacto) null else "Controles, imagen y sonido"
                        )
                        CaveButton(
                            "Registro", onStats, icon = IconId.ESTADISTICA,
                            modifier = Modifier.fillMaxWidth(),
                            subtitle = if (compacto) null else "Todo lo que llevas hecho"
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (!compacto) StonePanel(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniStat(IconId.TROFEO, "${save.maxLevel - 1}", "niveles")
                        Spacer(Modifier.width(18.dp))
                        MiniStat(IconId.LOGROS, formatNumber(save.totalEcosGanados), "ecos")
                        Spacer(Modifier.width(18.dp))
                        MiniStat(
                            IconId.CRONOMETRO,
                            if (save.bestTimeMs > 0) com.mggx.laberinto.ui.formatTime(save.bestTimeMs) else "-",
                            "record"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(icon: IconId, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CaveIcon(icon, size = 20.dp, tint = Cave.TextDim, accent = Cave.AmberDeep)
        Spacer(Modifier.width(7.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, fontSize = 9.sp, color = Cave.TextFaint)
        }
    }
}

/** Glifo animado del laberinto: la misma espiral del icono, dibujada en vivo. */
@Composable
private fun LabyrinthGlyph(pulse: Float, side: androidx.compose.ui.unit.Dp = 78.dp) {
    Canvas(Modifier.size(side)) {
        val s = size.minDimension
        val pts = listOf(
            0.500f to 0.500f, 0.500f to 0.365f, 0.365f to 0.365f, 0.365f to 0.635f,
            0.635f to 0.635f, 0.635f to 0.250f, 0.250f to 0.250f, 0.250f to 0.750f,
            0.750f to 0.750f, 0.750f to 0.135f, 0.135f to 0.135f, 0.135f to 0.865f,
            0.560f to 0.865f
        )
        val path = androidx.compose.ui.graphics.Path()
        path.moveTo(pts[0].first * s, pts[0].second * s)
        for (i in 1 until pts.size) path.lineTo(pts[i].first * s, pts[i].second * s)
        drawPath(
            path, Cave.Amber.copy(alpha = 0.28f * pulse),
            style = Stroke(width = s * 0.115f, cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
        drawPath(
            path, Cave.Amber,
            style = Stroke(width = s * 0.048f, cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
        drawCircle(Cave.AmberSoft, s * 0.045f, Offset(s * 0.5f, s * 0.5f))
    }
}

/** Carrusel para volver a cualquier nivel ya desbloqueado. */
@Composable
private fun LevelPicker(save: SaveData, refreshKey: Int, onPlay: (Int) -> Unit) {
    val levels = (1..save.maxLevel).toList()
    val state = rememberLazyListState()
    LaunchedEffect(refreshKey, save.currentLevel) {
        val idx = (save.currentLevel - 1).coerceIn(0, (levels.size - 1).coerceAtLeast(0))
        if (levels.isNotEmpty()) state.scrollToItem(idx)
    }
    Column(Modifier.fillMaxWidth(0.92f)) {
        Text(
            "O volve a un nivel que ya pasaste",
            style = MaterialTheme.typography.labelMedium,
            color = Cave.TextFaint
        )
        Spacer(Modifier.height(7.dp))
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(levels) { lv ->
                val on = lv == save.currentLevel
                val theme = CaveTheme.forLevel(lv)
                Box(
                    Modifier
                        .size(width = 58.dp, height = 54.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (on) Cave.Amber.copy(alpha = 0.22f) else Cave.Stone,
                                    Cave.Deep
                                )
                            )
                        )
                        .clickable { save.setCurrentLevel(lv); onPlay(lv) },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawRoundRect(
                            color = if (on) Cave.Amber.copy(alpha = 0.8f) else Cave.StoneEdge,
                            cornerRadius = CornerRadius(11.dp.toPx(), 11.dp.toPx()),
                            style = Stroke(1.2f * density)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$lv",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (on) Cave.AmberSoft else Cave.Text
                        )
                        Canvas(Modifier.size(width = 26.dp, height = 3.dp)) {
                            drawRoundRect(
                                Color(
                                    (theme.veinR * 255).toInt().coerceIn(0, 255),
                                    (theme.veinG * 255).toInt().coerceIn(0, 255),
                                    (theme.veinB * 255).toInt().coerceIn(0, 255)
                                ).copy(alpha = 0.85f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
