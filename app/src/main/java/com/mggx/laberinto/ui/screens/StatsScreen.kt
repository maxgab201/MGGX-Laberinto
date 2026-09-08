package com.mggx.laberinto.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.Currency
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import com.mggx.laberinto.maze.CaveTheme
import com.mggx.laberinto.ui.CaveBackdrop
import com.mggx.laberinto.ui.CaveBar
import com.mggx.laberinto.ui.CaveButton
import com.mggx.laberinto.ui.CaveButtonStyle
import com.mggx.laberinto.ui.SectionTitle
import com.mggx.laberinto.ui.StonePanel
import com.mggx.laberinto.ui.formatLongTime
import com.mggx.laberinto.ui.formatNumber
import com.mggx.laberinto.ui.formatTime
import com.mggx.laberinto.ui.icons.CaveIcon
import com.mggx.laberinto.ui.icons.IconId
import com.mggx.laberinto.ui.theme.Cave

@Composable
fun StatsScreen(save: SaveData, onBack: () -> Unit) {
    val owned = ItemCatalog.all.count { save.isOwned(it.id) }
    val total = ItemCatalog.all.size
    val winRate = if (save.totalRuns > 0) save.totalWins.toFloat() / save.totalRuns else 0f

    Box(Modifier.fillMaxSize()) {
        CaveBackdrop(seed = 61)
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CaveButton("Volver", onBack, icon = IconId.ATRAS, style = CaveButtonStyle.FANTASMA)
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("Tu registro", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Todo lo que llevas hecho en la cueva.",
                        style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StonePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("Descenso")
                            Spacer(Modifier.height(12.dp))
                            BigStat(IconId.TROFEO, "${save.maxLevel - 1}", "niveles superados")
                            Spacer(Modifier.height(10.dp))
                            StatLine("Nivel mas hondo alcanzado", "${save.maxLevel}")
                            StatLine("Zona actual", CaveTheme.forLevel(save.currentLevel).displayName)
                            StatLine("Mejor tiempo",
                                if (save.bestTimeMs > 0) formatTime(save.bestTimeMs) else "todavia ninguno")
                            StatLine("Tiempo total jugado", formatLongTime(save.totalPlayMs))
                            StatLine("Pasos dados", formatNumber(save.totalSteps.toInt()))
                        }
                    }
                    StonePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("Partidas")
                            Spacer(Modifier.height(12.dp))
                            StatLine("Jugadas", "${save.totalRuns}")
                            StatLine("Ganadas", "${save.totalWins}")
                            StatLine("Perdidas", "${save.totalDeaths}")
                            Spacer(Modifier.height(8.dp))
                            Text("Porcentaje de exito", fontSize = 11.sp, color = Cave.TextFaint)
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CaveBar(winRate, Cave.Good, Modifier.weight(1f), height = 10.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("${(winRate * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StonePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("Riqueza")
                            Spacer(Modifier.height(12.dp))
                            BigStat(IconId.MONEDA_ECO, formatNumber(save.balance(Currency.ECOS)), "ecos en el bolsillo")
                            Spacer(Modifier.height(10.dp))
                            StatLine("Ecos ganados en total", formatNumber(save.totalEcosGanados))
                            StatLine("Vetagris", "${save.balance(Currency.VETAGRIS)}")
                        }
                    }
                    StonePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("Coleccion")
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CaveBar(owned.toFloat() / total, Cave.Amber, Modifier.weight(1f), height = 10.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("$owned / $total", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(12.dp))
                            ItemKind.entries.forEach { k ->
                                val list = ItemCatalog.ofKind(k)
                                val n = list.count { save.isOwned(it.id) }
                                StatLine(k.label, "$n de ${list.size}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BigStat(icon: IconId, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CaveIcon(icon, size = 38.dp, tint = Cave.AmberSoft, accent = Cave.Amber)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(value, style = MaterialTheme.typography.displayLarge)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Cave.TextDim,
            modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
