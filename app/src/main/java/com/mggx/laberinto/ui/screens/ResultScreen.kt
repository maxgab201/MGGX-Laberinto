package com.mggx.laberinto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mggx.laberinto.game.GameSession
import com.mggx.laberinto.maze.CaveTheme
import com.mggx.laberinto.ui.CaveBackdrop
import com.mggx.laberinto.ui.CaveButton
import com.mggx.laberinto.ui.CaveButtonStyle
import com.mggx.laberinto.ui.SectionTitle
import com.mggx.laberinto.ui.StonePanel
import com.mggx.laberinto.ui.formatNumber
import com.mggx.laberinto.ui.formatTime
import com.mggx.laberinto.ui.icons.CaveIcon
import com.mggx.laberinto.ui.icons.IconId
import com.mggx.laberinto.ui.theme.Cave
import com.mggx.laberinto.ui.theme.Semantics

@Composable
fun ResultScreen(
    won: Boolean,
    level: Int,
    colorBlindMode: Int,
    reward: GameSession.Reward,
    timeMs: Long,
    steps: Int,
    nextLevel: Int,
    /** Si veniamos de una sala de a varios y sigue conectada. */
    salaActiva: Boolean,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    /** Volver a la sala, que sigue conectada, para bajar el proximo nivel juntos. */
    onContinuarSala: () -> Unit,
    onLobby: () -> Unit,
    onShop: () -> Unit
) {
    val sem = Semantics.of(colorBlindMode)
    Box(Modifier.fillMaxSize()) {
        CaveBackdrop(seed = if (won) 5 else 91)
        Box(Modifier.fillMaxSize().background(Cave.Void.copy(alpha = 0.45f)))

        Row(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 40.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                CaveIcon(
                    if (won) IconId.TROFEO else IconId.CALAVERA,
                    size = 62.dp,
                    tint = if (won) Cave.Amber else sem.bad,
                    accent = if (won) Cave.AmberSoft else sem.bad
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    if (won) "Saliste" else "Te quedaste abajo",
                    style = MaterialTheme.typography.displayLarge,
                    color = if (won) Cave.Text else sem.bad
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (won)
                        "Nivel $level superado: ${CaveTheme.forLevel(level).displayName}."
                    else
                        "La cueva te gano esta vez. Te llevas la mitad de lo que juntaste.",
                    style = MaterialTheme.typography.bodyLarge, color = Cave.TextDim
                )
                Spacer(Modifier.height(24.dp))
                // Con la sala viva son cuatro botones: en un telefono angosto
                // no entran de una y el ultimo quedaba fuera de pantalla.
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (salaActiva) {
                        // Con la sala viva, "seguir" es volver a ella: es lo
                        // que se espera despues de jugar acompanado, y evita
                        // tener que dictar el codigo de nuevo para el
                        // proximo nivel. "Jugar solo" sigue disponible para
                        // el que prefiera desconectarse.
                        CaveButton(
                            "Bajar con tu compañero", onContinuarSala, icon = IconId.MULTIJUGADOR,
                            style = CaveButtonStyle.PRIMARIO, subtitle = "La sala sigue conectada"
                        )
                        CaveButton(
                            "Jugar solo",
                            if (won) onNext else onRetry,
                            icon = if (won) IconId.JUGAR else IconId.REINICIAR
                        )
                    } else if (won) {
                        CaveButton(
                            "Seguir bajando", onNext, icon = IconId.JUGAR,
                            style = CaveButtonStyle.PRIMARIO, subtitle = "Nivel $nextLevel"
                        )
                    } else {
                        CaveButton(
                            "Reintentar", onRetry, icon = IconId.REINICIAR,
                            style = CaveButtonStyle.PRIMARIO, subtitle = "Nivel $level de nuevo"
                        )
                    }
                    CaveButton("Tienda", onShop, icon = IconId.TIENDA)
                    CaveButton("Al lobby", onLobby, icon = IconId.CASA)
                }
            }

            Spacer(Modifier.width(30.dp))

            StonePanel(Modifier.width(360.dp), glow = if (won) Cave.Amber else sem.bad) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                    SectionTitle("Resumen", accent = if (won) Cave.Amber else sem.bad)
                    Spacer(Modifier.height(14.dp))
                    ResultRow(IconId.CRONOMETRO, "Tiempo", formatTime(timeMs))
                    ResultRow(IconId.HUELLA, "Pasos dados", "$steps")
                    Spacer(Modifier.height(10.dp))
                    SectionTitle("Ecos", accent = Cave.AmberDeep)
                    Spacer(Modifier.height(10.dp))
                    ResultRow(IconId.MONEDA_ECO, "Juntados en la cueva", formatNumber(reward.baseEcos))
                    if (won) {
                        ResultRow(IconId.RELOJ, "Por rapidez", "+${formatNumber(reward.timeBonus)}")
                        ResultRow(IconId.MAPA, "Por explorar", "+${formatNumber(reward.exploreBonus)}")
                        if (reward.yieldMultiplier > 1.001f) {
                            ResultRow(
                                IconId.SACO_MONEDAS, "Instinto Recolector",
                                "x${String.format("%.2f", reward.yieldMultiplier)}"
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Cave.StoneEdge))
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CaveIcon(IconId.MONEDA_ECO, size = 26.dp, tint = Cave.AmberSoft, accent = Cave.AmberSoft)
                        Spacer(Modifier.width(9.dp))
                        Text("Total", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Text(
                            "+${formatNumber(reward.totalEcos)}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Cave.AmberSoft
                        )
                    }
                    if (reward.vetagris > 0) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CaveIcon(IconId.MONEDA_VETAGRIS, size = 24.dp,
                                tint = Cave.Vetagris, accent = Cave.Vetagris)
                            Spacer(Modifier.width(9.dp))
                            Text("Vetagris", style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f))
                            Text("+${reward.vetagris}",
                                style = MaterialTheme.typography.headlineMedium, color = Cave.Vetagris)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(icon: IconId, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CaveIcon(icon, size = 17.dp, tint = Cave.TextDim, accent = Cave.AmberDeep)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Cave.TextDim,
            modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
