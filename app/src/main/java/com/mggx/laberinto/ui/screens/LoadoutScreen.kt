package com.mggx.laberinto.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.EffectType
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import com.mggx.laberinto.game.PlayerStats
import com.mggx.laberinto.game.ShopItem
import com.mggx.laberinto.ui.CaveBackdrop
import com.mggx.laberinto.ui.CaveButton
import com.mggx.laberinto.ui.CaveButtonStyle
import com.mggx.laberinto.ui.CaveTabs
import com.mggx.laberinto.ui.ItemText
import com.mggx.laberinto.ui.SectionTitle
import com.mggx.laberinto.ui.StonePanel
import com.mggx.laberinto.ui.icons.CaveIcon
import com.mggx.laberinto.ui.icons.IconId
import com.mggx.laberinto.ui.theme.Cave

@Composable
fun LoadoutScreen(
    save: SaveData,
    refreshKey: Int,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ranuras rapidas", "Arma", "Reliquias", "Aspecto")

    Box(Modifier.fillMaxSize()) {
        CaveBackdrop(seed = 31)
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CaveButton("Volver", onBack, icon = IconId.ATRAS, style = CaveButtonStyle.FANTASMA)
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tu equipo", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Lo que llevas puesto cuando bajas a la cueva.",
                        style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            CaveTabs(tabs, tab, { tab = it }, Modifier.fillMaxWidth(0.74f))
            Spacer(Modifier.height(14.dp))

            when (tab) {
                0 -> QuickSlots(save, refreshKey, onChanged, onMessage)
                1 -> Armas(save, refreshKey, onChanged, onMessage)
                2 -> Relics(save, refreshKey, onChanged, onMessage)
                else -> Cosmetics(save, refreshKey, onChanged, onMessage)
            }
        }
    }
}

// ----------------------------------------------------------------- arma

/**
 * Que llevas en la mano para pelear. "A mano limpia" es una opcion de verdad
 * y siempre esta: pega poco, pero nadie se queda sin poder defenderse.
 */
@Composable
private fun Armas(save: SaveData, refreshKey: Int, onChanged: () -> Unit, onMessage: (String) -> Unit) {
    val armas = remember(refreshKey) { ItemCatalog.ofKind(ItemKind.ARMA) }
    val stats = remember(refreshKey) { PlayerStats(save) }

    Column(Modifier.fillMaxSize()) {
        StonePanel(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                CaveIcon(
                    ItemCatalog.get(save.armaEquipada)?.icon ?: IconId.PUNO,
                    size = 34.dp, tint = Cave.Text, accent = Cave.Bad
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stats.nombreArma, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${stats.danoGolpe.toInt()} de dano · un golpe cada " +
                            "${(stats.cadenciaGolpe * 100).toInt() / 100f} s · " +
                            "alcance ${(stats.alcanceGolpe * 100).toInt() / 100f} m",
                        style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ArmaRow(
                icon = IconId.PUNO,
                nombre = "A mano limpia",
                detalle = "${PlayerStats.GOLPE_BASE_DANO.toInt()} de dano. Siempre disponible, " +
                    "no hay que comprarla.",
                tuya = true,
                activa = save.armaEquipada.isEmpty()
            ) {
                save.equipArma("")
                onMessage("Peleas a mano limpia")
                onChanged()
            }
            Spacer(Modifier.height(8.dp))
            armas.forEach { item ->
                val tuya = save.isOwned(item.id)
                ArmaRow(
                    icon = item.icon,
                    nombre = item.name,
                    detalle = if (tuya) ItemText.shortEffect(item)
                    else "Se compra en la Tienda por ${item.basePrice} ${item.currency.code}",
                    tuya = tuya,
                    activa = save.armaEquipada == item.id
                ) {
                    save.equipArma(item.id)
                    onMessage("Agarraste: ${item.name}")
                    onChanged()
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ArmaRow(
    icon: IconId,
    nombre: String,
    detalle: String,
    tuya: Boolean,
    activa: Boolean,
    onClick: () -> Unit
) {
    val accent = if (activa) Cave.Bad else Cave.StoneEdge
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (activa) Cave.Bad.copy(alpha = 0.13f) else Cave.Stone)
            .clickable(enabled = tuya) { onClick() }
            .padding(12.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = accent,
                cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx()),
                style = Stroke(1.3f * density)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CaveIcon(
                icon, size = 32.dp,
                tint = if (tuya) Cave.Text else Cave.TextFaint,
                accent = if (tuya) Cave.Bad else Cave.TextFaint
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    nombre, style = MaterialTheme.typography.titleMedium,
                    color = if (tuya) Cave.Text else Cave.TextFaint
                )
                Text(detalle, style = MaterialTheme.typography.bodyMedium, color = Cave.TextDim)
            }
            if (activa) Text("EN LA MANO", fontSize = 10.sp, color = Cave.Bad)
        }
    }
}

// --------------------------------------------------------------- ranuras

@Composable
private fun QuickSlots(save: SaveData, refreshKey: Int, onChanged: () -> Unit, onMessage: (String) -> Unit) {
    val slots = save.loadoutSlots()
    val equipped = remember(refreshKey) { save.loadoutList() }
    val stocked = remember(refreshKey) {
        ItemCatalog.ofKind(ItemKind.CONSUMIBLE).filter { save.stockOf(it.id) > 0 }
    }

    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            SectionTitle("Ranuras ($slots)")
            Spacer(Modifier.height(10.dp))
            Text(
                "Estas son las que vas a poder usar con un toque durante la partida. " +
                    "Comprando Bolsillos Profundos sumas mas ranuras.",
                style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0 until slots) {
                    val id = equipped.getOrNull(i)
                    val item = id?.let { ItemCatalog.get(it) }
                    Box(
                        Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (item != null) Cave.Amber.copy(alpha = 0.13f)
                                else Cave.Void.copy(alpha = 0.55f)
                            )
                            .clickable(enabled = item != null) {
                                if (id != null) { save.toggleLoadout(id); onChanged() }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.matchParentSize()) {
                            drawRoundRect(
                                color = if (item != null) Cave.Amber.copy(alpha = 0.7f) else Cave.StoneEdge,
                                cornerRadius = CornerRadius(15.dp.toPx(), 15.dp.toPx()),
                                style = Stroke(1.4f * density)
                            )
                        }
                        if (item != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CaveIcon(item.icon, size = 34.dp, tint = Cave.Text, accent = Cave.Amber)
                                Text("x${save.stockOf(item.id)}", fontSize = 10.sp, color = Cave.AmberSoft)
                            }
                        } else {
                            Text("vacia", fontSize = 10.sp, color = Cave.TextFaint)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Toca una ranura para sacar el objeto.",
                fontSize = 11.sp, color = Cave.TextFaint
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1.25f).fillMaxHeight()) {
            SectionTitle("Consumibles que tenes")
            Spacer(Modifier.height(10.dp))
            if (stocked.isEmpty()) {
                StonePanel(Modifier.fillMaxWidth().height(120.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Todavia no compraste ningun consumible.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(stocked, key = { it.id }) { item ->
                        val on = equipped.contains(item.id)
                        EquipRow(
                            item = item,
                            active = on,
                            trailing = "x${save.stockOf(item.id)}",
                            onClick = {
                                val nowOn = save.toggleLoadout(item.id)
                                onMessage(if (nowOn) "${item.name} listo para usar" else "${item.name} fuera de la ranura")
                                onChanged()
                            }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------- reliquias

@Composable
private fun Relics(save: SaveData, refreshKey: Int, onChanged: () -> Unit, onMessage: (String) -> Unit) {
    val ownedRelics = remember(refreshKey) {
        ItemCatalog.ofKind(ItemKind.RELIQUIA).filter { save.isOwned(it.id) }
    }
    val equipped = remember(refreshKey) { save.relics() }

    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            SectionTitle("Ranuras de reliquia (${ItemCatalog.RELIC_SLOTS})")
            Spacer(Modifier.height(10.dp))
            Text(
                "Las reliquias actuan solas todo el tiempo. Podes llevar tres.",
                style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
            )
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                for (i in 0 until ItemCatalog.RELIC_SLOTS) {
                    val id = equipped.getOrNull(i)
                    val item = id?.let { ItemCatalog.get(it) }
                    StonePanel(
                        Modifier.fillMaxWidth(),
                        glow = if (item != null) ItemText.rarityAccent(item.rarity) else Cave.StoneEdge
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp).clickable(enabled = item != null) {
                                if (id != null) { save.toggleRelic(id); onChanged() }
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(11.dp))
                                    .background(Cave.Void.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item != null)
                                    CaveIcon(item.icon, size = 28.dp, tint = Cave.Text,
                                        accent = ItemText.rarityAccent(item.rarity))
                                else
                                    CaveIcon(IconId.EQUIPAR, size = 22.dp, tint = Cave.TextFaint, accent = Cave.TextFaint)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item?.name ?: "Ranura libre", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (item != null) ItemText.shortEffect(item) else "Elegi una reliquia de la lista",
                                    style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1.25f).fillMaxHeight()) {
            SectionTitle("Reliquias que tenes")
            Spacer(Modifier.height(10.dp))
            if (ownedRelics.isEmpty()) {
                StonePanel(Modifier.fillMaxWidth().height(120.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Todavia no tenes reliquias. Se compran con Vetagris.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ownedRelics, key = { it.id }) { item ->
                        EquipRow(
                            item = item,
                            active = equipped.contains(item.id),
                            trailing = null,
                            onClick = {
                                val on = save.toggleRelic(item.id)
                                onMessage(if (on) "${item.name} equipada" else "${item.name} guardada")
                                onChanged()
                            }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------- cosmeticos

@Composable
private fun Cosmetics(save: SaveData, refreshKey: Int, onChanged: () -> Unit, onMessage: (String) -> Unit) {
    val gloves = remember(refreshKey) {
        ItemCatalog.ofKind(ItemKind.COSMETICO).filter { it.effect.type == EffectType.COS_GUANTES }
    }
    val lights = remember(refreshKey) {
        ItemCatalog.ofKind(ItemKind.COSMETICO).filter { it.effect.type == EffectType.COS_TINTE_LUZ }
    }
    val skins = remember(refreshKey) {
        ItemCatalog.ofKind(ItemKind.COSMETICO).filter { it.effect.type == EffectType.COS_PIEL }
    }

    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            SectionTitle("Quien sos")
            Spacer(Modifier.height(4.dp))
            Text(
                "La skin te cambia la piel y el traje que se te ven al bajar.",
                style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
            )
            Spacer(Modifier.height(10.dp))
            skins.forEach { item ->
                CosmeticRow(item, save, save.cosmeticSkin == item.id, onChanged, onMessage)
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            SectionTitle("Manos")
            Spacer(Modifier.height(10.dp))
            gloves.forEach { item ->
                CosmeticRow(item, save, save.cosmeticGloves == item.id, onChanged, onMessage)
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            SectionTitle("Color de tu luz")
            Spacer(Modifier.height(10.dp))
            lights.forEach { item ->
                CosmeticRow(item, save, save.cosmeticLight == item.id, onChanged, onMessage)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CosmeticRow(
    item: ShopItem, save: SaveData, active: Boolean,
    onChanged: () -> Unit, onMessage: (String) -> Unit
) {
    val owned = save.isOwned(item.id)
    val accent = when (item.effect.type) {
        EffectType.COS_TINTE_LUZ -> Color(item.effect.color)
        EffectType.COS_PIEL -> Color(item.effect.color2)
        else -> ItemText.rarityAccent(item.rarity)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (active) accent.copy(alpha = 0.15f) else Cave.Stone)
            .clickable(enabled = owned) {
                save.equipCosmetic(item.id)
                onMessage("${item.name} equipado")
                onChanged()
            }
            .padding(12.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = if (active) accent.copy(alpha = 0.9f) else Cave.StoneEdge,
                cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx()),
                style = Stroke(1.3f * density)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CaveIcon(item.icon, size = 34.dp, tint = if (owned) Cave.Text else Cave.TextFaint, accent = accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium,
                    color = if (owned) Cave.Text else Cave.TextFaint)
                Text(item.desc, style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            when {
                active -> CaveIcon(IconId.TILDE, size = 22.dp, tint = Cave.Good, accent = Cave.Good)
                owned -> Text("Equipar", fontSize = 11.sp, color = Cave.AmberSoft)
                else -> CaveIcon(IconId.CANDADO, size = 20.dp, tint = Cave.TextFaint, accent = Cave.TextFaint)
            }
        }
    }
}

@Composable
private fun EquipRow(item: ShopItem, active: Boolean, trailing: String?, onClick: () -> Unit) {
    val accent = ItemText.rarityAccent(item.rarity)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(if (active) accent.copy(alpha = 0.16f) else Cave.Stone, Cave.Deep)
                )
            )
            .clickable { onClick() }
            .padding(11.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = if (active) accent.copy(alpha = 0.9f) else Cave.StoneEdge,
                cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx()),
                style = Stroke(1.3f * density)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CaveIcon(item.icon, size = 30.dp, tint = Cave.Text, accent = accent)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(ItemText.shortEffect(item), style = MaterialTheme.typography.bodyMedium,
                    color = Cave.TextFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Text(trailing, fontSize = 12.sp, color = Cave.AmberSoft)
            }
            Spacer(Modifier.width(8.dp))
            if (active) CaveIcon(IconId.TILDE, size = 20.dp, tint = Cave.Good, accent = Cave.Good)
        }
    }
}
