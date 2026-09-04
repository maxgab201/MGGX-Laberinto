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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.Currency
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import com.mggx.laberinto.game.PlayerStats
import com.mggx.laberinto.game.ShopItem
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
    // Se compra desde aca mismo, asi que hace falta un tick para redibujar.
    var compras by remember { mutableIntStateOf(0) }
    @Suppress("UNUSED_EXPRESSION") compras
    val stats = remember(compras) { PlayerStats(save) }
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
                            SectionTitle("Tu cuerpo")
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Subilo desde aca. Cada nivel cuesta un poco mas y no se pierde nunca.",
                                style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                            )
                            Spacer(Modifier.height(12.dp))
                            MejoraCuerpo(save, "up_corazon", IconId.VIDA, Cave.Bad,
                                "Vida maxima", "${stats.maxHealth.toInt()}") { compras++ }
                            MejoraCuerpo(save, "up_pulmones", IconId.AGUANTE, Cave.Stamina,
                                "Aguante maximo", "${stats.maxStamina.toInt()}") { compras++ }
                            MejoraCuerpo(save, "up_botas", IconId.BOTA, Cave.Amber,
                                "Velocidad", String.format("%.1f m/s", stats.walkSpeed)) { compras++ }
                            MejoraCuerpo(save, "up_farol", IconId.FAROL, Cave.AmberSoft,
                                "Alcance de la luz", String.format("%.1f m", stats.lightRadius)) { compras++ }
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
                    StonePanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("Poderes activos")
                            Spacer(Modifier.height(10.dp))
                            val poderes = ItemCatalog.ofKind(ItemKind.PODER)
                            val tuyos = poderes.filter { save.isOwned(it.id) }
                            if (tuyos.isEmpty()) {
                                Text(
                                    "Todavia ninguno. Los poderes se compran con Vetagris en la tienda y no se gastan nunca.",
                                    style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                                )
                            } else {
                                tuyos.forEach { p ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CaveIcon(p.icon, size = 20.dp, tint = Cave.Amber, accent = Cave.AmberSoft)
                                        Spacer(Modifier.width(10.dp))
                                        Text(p.name, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            StatLine("Comprados", "${tuyos.size} de ${poderes.size}")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Una fila de mejora del cuerpo: cuanto tenes, cuanto sube y el boton de pagar.
 * Compra la misma mejora que la tienda, asi que no hay dos sistemas paralelos.
 */
@Composable
private fun MejoraCuerpo(
    save: SaveData,
    id: String,
    icon: IconId,
    color: androidx.compose.ui.graphics.Color,
    label: String,
    valorActual: String,
    onCambio: () -> Unit
) {
    val item: ShopItem = ItemCatalog.require(id)
    val nivel = save.ownedLevel(id)
    val alMaximo = nivel >= item.maxLevel
    val precio = item.priceAt(nivel)
    val alcanza = save.balance(item.currency) >= precio
    val bloqueado = item.unlockLevel > save.maxLevel

    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CaveIcon(icon, size = 20.dp, tint = color, accent = color)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (alMaximo) "Al maximo - $valorActual"
                    else "$valorActual   ·   nivel $nivel de ${item.maxLevel}",
                    fontSize = 11.sp, color = Cave.TextFaint
                )
            }
            Spacer(Modifier.width(10.dp))
            if (alMaximo) {
                Text("Completo", fontSize = 11.sp, color = Cave.Good)
            } else {
                CaveButton(
                    if (bloqueado) "Bloqueado" else "$precio ${item.currency.code}",
                    onClick = {
                        if (save.buy(id) == SaveData.BuyResult.OK) onCambio()
                    },
                    icon = IconId.MAS,
                    style = if (alcanza && !bloqueado) CaveButtonStyle.PRIMARIO else CaveButtonStyle.FANTASMA,
                    enabled = alcanza && !bloqueado
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        CaveBar(
            if (item.maxLevel <= 0) 0f else nivel.toFloat() / item.maxLevel,
            color, Modifier.fillMaxWidth(), height = 7.dp
        )
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
