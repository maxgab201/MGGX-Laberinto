package com.mggx.laberinto.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mggx.laberinto.core.SaveData
import com.mggx.laberinto.game.Currency
import com.mggx.laberinto.game.ItemCatalog
import com.mggx.laberinto.game.ItemKind
import com.mggx.laberinto.game.ShopItem
import com.mggx.laberinto.ui.CaveBackdrop
import com.mggx.laberinto.ui.CaveButton
import com.mggx.laberinto.ui.CaveButtonStyle
import com.mggx.laberinto.ui.CaveTabs
import com.mggx.laberinto.ui.CurrencyChip
import com.mggx.laberinto.ui.ItemText
import com.mggx.laberinto.ui.SectionTitle
import com.mggx.laberinto.ui.StonePanel
import com.mggx.laberinto.ui.formatNumber
import com.mggx.laberinto.ui.icons.CaveIcon
import com.mggx.laberinto.ui.icons.IconId
import com.mggx.laberinto.ui.theme.Cave

@Composable
fun ShopScreen(
    save: SaveData,
    refreshKey: Int,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val kinds = ItemKind.entries
    val kind = kinds[tab]

    val items = remember(tab, refreshKey, save.maxLevel) {
        ItemCatalog.ofKind(kind).sortedWith(
            compareBy({ it.unlockLevel > save.maxLevel }, { it.unlockLevel }, { it.basePrice })
        )
    }
    val selected = items.firstOrNull { it.id == selectedId } ?: items.firstOrNull()

    Box(Modifier.fillMaxSize()) {
        CaveBackdrop(seed = 23)
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {

            // ------------------------------------------------------- cabecera
            Row(verticalAlignment = Alignment.CenterVertically) {
                CaveButton("Volver", onBack, icon = IconId.ATRAS, style = CaveButtonStyle.FANTASMA)
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tienda de la Boca de Cueva", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${ItemCatalog.all.size} objetos. Ninguno hace lo mismo que otro.",
                        style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint
                    )
                }
                CurrencyChip(IconId.MONEDA_ECO, save.balance(Currency.ECOS), accent = Cave.AmberSoft)
                Spacer(Modifier.width(9.dp))
                CurrencyChip(IconId.MONEDA_VETAGRIS, save.balance(Currency.VETAGRIS), accent = Cave.Vetagris)
            }

            Spacer(Modifier.height(12.dp))
            CaveTabs(
                kinds.map { it.label }, tab,
                { tab = it; selectedId = null },
                Modifier.fillMaxWidth(0.80f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                kind.blurb,
                style = MaterialTheme.typography.bodyMedium,
                color = Cave.TextFaint,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxSize()) {
                // ---------------------------------------------------- catalogo
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(180.dp),
                    modifier = Modifier.weight(1.55f).fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            save = save,
                            selected = item.id == selected?.id,
                            onClick = { selectedId = item.id }
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                // ------------------------------------------------------ detalle
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (selected == null) {
                        StonePanel(Modifier.fillMaxSize()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Todavia no hay nada de esta clase.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        ItemDetail(selected, save, onChanged, onMessage)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(item: ShopItem, save: SaveData, selected: Boolean, onClick: () -> Unit) {
    val locked = item.unlockLevel > save.maxLevel
    val lvl = save.ownedLevel(item.id)
    val stock = save.stockOf(item.id)
    val accent = ItemText.rarityAccent(item.rarity)
    val maxed = item.isLeveled && lvl >= item.maxLevel
    val ownedSimple = !item.isLeveled && item.kind != ItemKind.CONSUMIBLE && lvl > 0

    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (selected) accent.copy(alpha = 0.16f) else Cave.StoneHi,
                        Cave.Deep
                    )
                )
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = if (selected) accent.copy(alpha = 0.95f) else Cave.StoneEdge.copy(alpha = 0.75f),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(if (selected) 1.9f * density else 1.1f * density)
            )
        }
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Cave.Void.copy(alpha = 0.62f)),
                    contentAlignment = Alignment.Center
                ) {
                    CaveIcon(
                        item.icon, size = 30.dp,
                        tint = if (locked) Cave.TextFaint else Cave.Text,
                        accent = if (locked) Cave.TextFaint else accent
                    )
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (locked) Cave.TextFaint else Cave.Text
                    )
                    Text(
                        item.rarity.label,
                        fontSize = 10.sp,
                        color = accent
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                ItemText.shortEffect(item),
                style = MaterialTheme.typography.bodyMedium,
                color = Cave.TextDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    locked -> {
                        CaveIcon(IconId.CANDADO, size = 15.dp, tint = Cave.TextFaint, accent = Cave.TextFaint)
                        Spacer(Modifier.width(5.dp))
                        Text("Nivel ${item.unlockLevel}", fontSize = 11.sp, color = Cave.TextFaint)
                    }
                    maxed -> {
                        CaveIcon(IconId.TILDE, size = 15.dp, tint = Cave.Good, accent = Cave.Good)
                        Spacer(Modifier.width(5.dp))
                        Text("Al maximo", fontSize = 11.sp, color = Cave.Good)
                    }
                    ownedSimple -> {
                        CaveIcon(IconId.TILDE, size = 15.dp, tint = Cave.Good, accent = Cave.Good)
                        Spacer(Modifier.width(5.dp))
                        Text("Comprado", fontSize = 11.sp, color = Cave.Good)
                    }
                    else -> {
                        CaveIcon(
                            if (item.currency == Currency.ECOS) IconId.MONEDA_ECO else IconId.MONEDA_VETAGRIS,
                            size = 17.dp,
                            tint = if (item.currency == Currency.ECOS) Cave.AmberSoft else Cave.Vetagris,
                            accent = if (item.currency == Currency.ECOS) Cave.AmberSoft else Cave.Vetagris
                        )
                        Spacer(Modifier.width(5.dp))
                        val price = item.priceAt(lvl)
                        Text(
                            formatNumber(price),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (save.balance(item.currency) >= price) Cave.Text else Cave.Bad
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (item.isLeveled && !locked) {
                    Text("$lvl/${item.maxLevel}", fontSize = 11.sp, color = Cave.TextDim)
                } else if (item.kind == ItemKind.CONSUMIBLE && stock > 0) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Cave.Amber.copy(alpha = 0.20f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text("x$stock", fontSize = 11.sp, color = Cave.AmberSoft)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemDetail(
    item: ShopItem,
    save: SaveData,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit
) {
    val accent = ItemText.rarityAccent(item.rarity)
    val lvl = save.ownedLevel(item.id)
    val locked = item.unlockLevel > save.maxLevel
    val price = item.priceAt(lvl)
    val canAfford = save.balance(item.currency) >= price
    val maxed = item.isLeveled && lvl >= item.maxLevel
    val ownedSimple = !item.isLeveled && item.kind != ItemKind.CONSUMIBLE && lvl > 0
    val scroll = rememberScrollState()

    StonePanel(Modifier.fillMaxSize(), glow = accent) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Column(Modifier.weight(1f).verticalScroll(scroll)) {
                Box(
                    Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(accent.copy(alpha = 0.18f), Cave.Void.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CaveIcon(item.icon, size = 50.dp, tint = Cave.Text, accent = accent)
                }
                Spacer(Modifier.height(12.dp))
                Text(item.name, style = MaterialTheme.typography.headlineMedium)
                Text(item.rarity.label.uppercase(), fontSize = 11.sp, color = accent)
                Spacer(Modifier.height(12.dp))
                Text(item.desc, style = MaterialTheme.typography.bodyLarge, color = Cave.TextDim)

                Spacer(Modifier.height(16.dp))
                SectionTitle("Que hace", accent = accent)
                Spacer(Modifier.height(8.dp))
                Text(ItemText.shortEffect(item), style = MaterialTheme.typography.titleMedium, color = Cave.Text)
                Spacer(Modifier.height(6.dp))
                Text(ItemText.usage(item), style = MaterialTheme.typography.bodyMedium, color = Cave.TextFaint)

                if (item.isLeveled) {
                    Spacer(Modifier.height(16.dp))
                    SectionTitle("Progreso", accent = accent)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        for (i in 1..item.maxLevel) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (i <= lvl) accent else Cave.Void.copy(alpha = 0.8f))
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Nivel $lvl de ${item.maxLevel}", fontSize = 12.sp, color = Cave.TextDim)
                }
            }

            Spacer(Modifier.height(14.dp))

            when {
                locked -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CaveIcon(IconId.CANDADO, size = 20.dp, tint = Cave.TextFaint, accent = Cave.TextFaint)
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "Se desbloquea cuando llegues al nivel ${item.unlockLevel}.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                maxed -> Text("Ya lo tenes al maximo. Nada mas que comprar aca.", color = Cave.Good,
                    style = MaterialTheme.typography.titleMedium)
                ownedSimple -> Text("Ya es tuyo. Equipalo desde Equipo.", color = Cave.Good,
                    style = MaterialTheme.typography.titleMedium)
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CaveIcon(
                            if (item.currency == Currency.ECOS) IconId.MONEDA_ECO else IconId.MONEDA_VETAGRIS,
                            size = 26.dp,
                            tint = if (item.currency == Currency.ECOS) Cave.AmberSoft else Cave.Vetagris,
                            accent = if (item.currency == Currency.ECOS) Cave.AmberSoft else Cave.Vetagris
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                formatNumber(price),
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (canAfford) Cave.Text else Cave.Bad
                            )
                            if (!canAfford) {
                                Text(
                                    "Te faltan ${formatNumber(price - save.balance(item.currency))}",
                                    fontSize = 11.sp, color = Cave.Bad
                                )
                            }
                        }
                        CaveButton(
                            if (item.kind == ItemKind.CONSUMIBLE) "Comprar" else if (item.isLeveled) "Mejorar" else "Comprar",
                            onClick = {
                                val r = save.buy(item.id)
                                onMessage(
                                    when {
                                        r != SaveData.BuyResult.OK -> r.message
                                        item.kind != ItemKind.CONSUMIBLE -> "Listo: ${item.name}"
                                        save.estaEnRanuraRapida(item.id) ->
                                            "${item.name} listo para usar en la partida"
                                        else ->
                                            "${item.name} comprado. Va al final del bolso: " +
                                                "en la partida corre la fila de objetos"
                                    }
                                )
                                onChanged()
                            },
                            style = CaveButtonStyle.PRIMARIO,
                            enabled = canAfford,
                            icon = IconId.TIENDA
                        )
                    }
                }
            }
        }
    }
}
